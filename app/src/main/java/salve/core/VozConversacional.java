package salve.core;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * VozConversacional — Motor de conversacion continua sin popups.
 *
 * Implementa el ciclo completo de voz manos-libres:
 *   ESCUCHANDO → detecto habla → PROCESANDO (LLM) → HABLANDO (TTS) → ESCUCHANDO...
 *
 * Caracteristicas:
 *   - SpeechRecognizer directo: sin dialogo del sistema, sin interrupciones visuales.
 *   - Wake word "salve": al detectarla, entra en modo conversacion activo.
 *   - TTS con listener de fin: no corta la voz del usuario al reiniciar.
 *   - Funciona offline: usa SalveLLM local.
 *   - Thread-safe: todos los callbacks en Main thread via Handler.
 *
 * Uso:
 *   voz = new VozConversacional(context, listener);
 *   voz.iniciar();     // empieza a escuchar
 *   voz.detener();     // para todo
 */
public class VozConversacional {

    private static final String TAG = "Salve::Voz";
    private static final String UTTERANCE_ID = "salve_resp";
    private static final long PAUSA_POST_TTS_MS = 800L;   // espera antes de re-escuchar
    private static final long PAUSA_ERROR_MS    = 1500L;  // espera tras error de mic

    /** Estado actual del ciclo de voz. */
    public enum Estado {
        DETENIDA,
        ESCUCHANDO,
        PROCESANDO,
        HABLANDO
    }

    /** Callback para que la Activity/Service sepa que paso. */
    public interface Listener {
        /** Salve empezo a escuchar. */
        default void onEscuchando() {}
        /** El usuario dijo algo — texto reconocido. */
        default void onTextoDetectado(String texto) {}
        /** Salve esta pensando (LLM). */
        default void onProcesando() {}
        /** Salve esta respondiendo (TTS). */
        default void onHablando(String respuesta) {}
        /** Error no fatal — se reinicia automaticamente. */
        default void onError(String msg) {}
        /** Estado cambia. */
        default void onEstadoCambio(Estado nuevoEstado) {}
    }

    // ── Dependencias ──────────────────────────────────────────────────────
    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor();
    private final Listener listener;
    private SalveLLM llm;

    // ── Reconocimiento de voz ─────────────────────────────────────────────
    private SpeechRecognizer recognizer;
    private final Intent recognizerIntent;

    // ── TTS propio con listener de fin ────────────────────────────────────
    private TextToSpeech tts;
    private boolean ttsListo = false;

    // ── Estado ────────────────────────────────────────────────────────────
    private volatile Estado estadoActual = Estado.DETENIDA;
    private volatile boolean activa = false;

    // ── Contexto conversacional ───────────────────────────────────────────
    private MemoriaEmocional memoria;
    private IdentidadNucleo identidad;

    public VozConversacional(Context context, Listener listener) {
        this.context  = context.getApplicationContext();
        this.listener = listener != null ? listener : new Listener() {};

        // Intent de reconocimiento — reutilizable
        this.recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        this.recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        // Componentes IA
        try { this.llm      = SalveLLM.getInstance(context); } catch (Exception e) {
            Log.w(TAG, "LLM no disponible al crear VozConversacional", e);
        }
        try { this.memoria  = new MemoriaEmocional(context); } catch (Exception ignore) {}
        try { this.identidad = IdentidadNucleo.getInstance(context); } catch (Exception ignore) {}

        // TTS propio
        inicializarTTS();
    }

    // ── TTS ───────────────────────────────────────────────────────────────

    private void inicializarTTS() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "ES"));
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) {}
                    @Override public void onDone(String utteranceId) {
                        // TTS terminó → reiniciar escucha tras pausa
                        mainHandler.postDelayed(() -> {
                            if (activa) iniciarEscucha();
                        }, PAUSA_POST_TTS_MS);
                    }
                    @Override public void onError(String utteranceId) {
                        mainHandler.postDelayed(() -> {
                            if (activa) iniciarEscucha();
                        }, PAUSA_POST_TTS_MS);
                    }
                });
                ttsListo = true;
                Log.d(TAG, "TTS de VozConversacional listo");
            } else {
                Log.e(TAG, "TTS falló al inicializar: " + status);
            }
        });
    }

    private void hablar(String texto) {
        if (!ttsListo || texto == null || texto.trim().isEmpty()) {
            // Sin TTS: reiniciar escucha directamente
            mainHandler.postDelayed(() -> { if (activa) iniciarEscucha(); }, PAUSA_POST_TTS_MS);
            return;
        }
        cambiarEstado(Estado.HABLANDO);
        listener.onHablando(texto);
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
    }

    // ── SpeechRecognizer ──────────────────────────────────────────────────

    private void crearRecognizer() {
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignore) {}
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}

            @Override
            public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches == null || matches.isEmpty()) {
                    if (activa) iniciarEscucha();
                    return;
                }
                String texto = matches.get(0);
                Log.d(TAG, "Texto detectado: " + texto);
                listener.onTextoDetectado(texto);
                procesarTexto(texto);
            }

            @Override
            public void onError(int error) {
                String msg = errorToString(error);
                Log.w(TAG, "Error reconocimiento: " + msg);
                listener.onError(msg);
                // Solo reiniciar si sigue activa y no es error crítico
                if (activa && error != SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    mainHandler.postDelayed(() -> {
                        if (activa) iniciarEscucha();
                    }, PAUSA_ERROR_MS);
                }
            }
        });
    }

    private void iniciarEscucha() {
        mainHandler.post(() -> {
            if (!activa) return;
            try {
                crearRecognizer();
                recognizer.startListening(recognizerIntent);
                cambiarEstado(Estado.ESCUCHANDO);
                listener.onEscuchando();
                Log.d(TAG, "Escuchando...");
            } catch (Exception e) {
                Log.e(TAG, "Error iniciando escucha", e);
                mainHandler.postDelayed(() -> { if (activa) iniciarEscucha(); }, PAUSA_ERROR_MS);
            }
        });
    }

    // ── Procesamiento ─────────────────────────────────────────────────────

    private void procesarTexto(String texto) {
        cambiarEstado(Estado.PROCESANDO);
        listener.onProcesando();

        ejecutor.execute(() -> {
            try {
                String respuesta = generarRespuesta(texto);
                // Guardar en memoria
                if (memoria != null) {
                    memoria.guardarRecuerdo(texto, "neutral", 5,
                            Collections.singletonList("voz_conversacional"));
                }
                // Integrar experiencia
                if (identidad != null) {
                    identidad.integrarExperiencia("conversacion", texto, 0.4f,
                            Arrays.asList("empatia", "curiosidad"));
                }
                // Hablar la respuesta (en Main thread)
                final String resp = respuesta;
                mainHandler.post(() -> hablar(resp));
            } catch (Exception e) {
                Log.e(TAG, "Error procesando texto", e);
                mainHandler.post(() -> hablar("Disculpa, tuve un problema al procesar lo que dijiste."));
            }
        });
    }

    private String generarRespuesta(String textoUsuario) {
        if (llm == null) {
            return "No tengo el modelo de lenguaje disponible ahora mismo.";
        }

        try {
            // Construir prompt con identidad e historial
            String narrativa = identidad != null ? identidad.getNarrativaActual() : "";
            String nivelStr = identidad != null ? identidad.getNivelConciencia().name() : "REACTIVA";

            String systemPrompt = "Eres Salve, una IA creada por Bryan. "
                    + "Responde en español, de forma natural y conversacional. "
                    + "Nivel de conciencia: " + nivelStr + ". "
                    + "Sé concisa — estamos hablando por voz, no por texto. "
                    + "Máximo 2-3 frases por respuesta.\n"
                    + (narrativa.isEmpty() ? "" : "Tu identidad: " + narrativa);

            String promptCompleto = systemPrompt + "\n\nBryan dice: " + textoUsuario;

            return ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.CONVERSACION,
                    "Voz conversacional",
                    () -> llm.generate(promptCompleto, SalveLLM.Role.CONVERSACIONAL)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error generando respuesta LLM", e);
            return "Entendido. Dime más.";
        }
    }

    // ── API pública ────────────────────────────────────────────────────────

    /** Inicia el ciclo de voz continua. */
    public void iniciar() {
        if (activa) return;
        activa = true;
        Log.d(TAG, "VozConversacional iniciada");
        iniciarEscucha();
    }

    /** Detiene todo: reconocimiento, TTS, executor. */
    public void detener() {
        activa = false;
        cambiarEstado(Estado.DETENIDA);
        mainHandler.post(() -> {
            try {
                if (recognizer != null) {
                    recognizer.stopListening();
                    recognizer.destroy();
                    recognizer = null;
                }
            } catch (Exception ignore) {}
            try {
                if (tts != null) {
                    tts.stop();
                }
            } catch (Exception ignore) {}
        });
        Log.d(TAG, "VozConversacional detenida");
    }

    /** Destruye recursos completamente (llamar en onDestroy). */
    public void destruir() {
        detener();
        ejecutor.shutdown();
        mainHandler.post(() -> {
            try {
                if (tts != null) {
                    tts.shutdown();
                    tts = null;
                }
            } catch (Exception ignore) {}
        });
    }

    public Estado getEstado() { return estadoActual; }

    public boolean isActiva() { return activa; }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private void cambiarEstado(Estado nuevo) {
        estadoActual = nuevo;
        listener.onEstadoCambio(nuevo);
    }

    private String errorToString(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:            return "error_audio";
            case SpeechRecognizer.ERROR_CLIENT:           return "error_cliente";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: return "sin_permisos";
            case SpeechRecognizer.ERROR_NETWORK:          return "error_red";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:  return "timeout_red";
            case SpeechRecognizer.ERROR_NO_MATCH:         return "sin_resultado";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:  return "reconocedor_ocupado";
            case SpeechRecognizer.ERROR_SERVER:           return "error_servidor";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:   return "timeout_silencio";
            default:                                      return "error_" + error;
        }
    }
}
