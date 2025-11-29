package salve.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.salve.app.R;
import salve.core.MemoriaEmocional;
import salve.core.TTSManager;
import salve.services.SalveApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio que despliega una “burbuja flotante” y permite:
 *  - Escucha pasiva de palabras clave via SpeechRecognizer.
 *  - Leer y guardar fragmentos de voz como recuerdos.
 *  - Responder por TTS, solicitando foco de audio.
 *  - Desplazar y cerrar la burbuja.
 */
public class BurbujaFlotanteService extends Service {

    // ── Configuración de escucha y voz ─────────────────────────────────────
    private boolean escuchaPasivaActiva = false;
    private boolean vozActiva;

    // ── Vista flotante ──────────────────────────────────────────────────────
    private View burbujaView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;

    // ── Reconocimiento de voz ───────────────────────────────────────────────
    private SpeechRecognizer recognizer;
    private Intent recognizerIntent;
    private final List<String> palabrasClave = Arrays.asList(
            "salve", "bryan", "steven", "guerra", "atentado", "explosión", "acabar"
    );

    // ── Persistencia y lógica IA ────────────────────────────────────────────
    private SharedPreferences preferencias;
    private MemoriaEmocional memoria;

    // ── Gestión de audio (foco + TTS) ───────────────────────────────────────
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private TTSManager ttsManager;

    @Override
    public IBinder onBind(Intent intent) {
        // No soporta binding
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // 1) Carga preferencias
        preferencias = getSharedPreferences("config_salve", MODE_PRIVATE);
        vozActiva    = preferencias.getBoolean("voz_activada", true);

        // 2) Inicializa IA y TTS
        memoria     = new MemoriaEmocional(getApplicationContext());
        ttsManager  = SalveApplication.getTTS();

        // 3) Configura AudioManager y foco compatible
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();

            focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(focusChange -> {
                        // No hacemos nada especial
                    })
                    .build();
        }

        // 4) Infla la burbuja flotante
        burbujaView = LayoutInflater.from(this)
                .inflate(R.layout.burbuja_flotante, null);

        int tipoOverlay = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipoOverlay,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        windowParams.gravity = Gravity.TOP | Gravity.START;
        windowParams.x = 100;
        windowParams.y = 200;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(burbujaView, windowParams);

        // 5) Íconos y listeners
        ImageView micIcon = burbujaView.findViewById(R.id.icono_mic);
        ImageView vozIcon = burbujaView.findViewById(R.id.icono_voz);

        // Tocar micrófono: toggle escucha pasiva
        micIcon.setOnClickListener(v -> {
            escuchaPasivaActiva = !escuchaPasivaActiva;
            micIcon.setAlpha(escuchaPasivaActiva ? 1f : 0.4f);
            if (escuchaPasivaActiva) {
                hablar("Escucha pasiva activada");
                iniciarEscuchaPasiva();
            } else {
                hablar("Escucha pasiva desactivada");
                detenerEscuchaPasiva();
            }
        });

        // Tocar altavoz: toggle respuestas por voz
        vozIcon.setOnClickListener(v -> {
            vozActiva = !vozActiva;
            preferencias.edit().putBoolean("voz_activada", vozActiva).apply();
            vozIcon.setAlpha(vozActiva ? 1f : 0.4f);
            String msg = vozActiva
                    ? "Voz activada. Responderé en voz alta."
                    : "Voz desactivada. Solo texto.";
            if (vozActiva) hablar(msg);
            else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        // Pulsación larga en micrófono: detiene servicio
        micIcon.setOnLongClickListener(v -> {
            stopSelf();
            return true;
        });

        // Arrastrar burbuja
        burbujaView.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX     = windowParams.x;
                        startY     = windowParams.y;
                        touchX     = event.getRawX();
                        touchY     = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        windowParams.x = startX + (int) (event.getRawX() - touchX);
                        windowParams.y = startY + (int) (event.getRawY() - touchY);
                        windowManager.updateViewLayout(burbujaView, windowParams);
                        return true;
                }
                return false;
            }
        });
    }

    // ── ESCUCHA PASIVA ────────────────────────────────────────────────────────

    private void iniciarEscuchaPasiva() {
        if (recognizer != null) {
            recognizer.destroy();
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        );
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                if (escuchaPasivaActiva) reiniciarEscuchaPasiva();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION
                );
                if (matches != null) {
                    for (String frase : matches) {
                        for (String clave : palabrasClave) {
                            if (frase.toLowerCase().contains(clave)) {
                                hablar("Escuché algo importante.");
                                guardarRecuerdo(frase);
                                break;
                            }
                        }
                    }
                }
                if (escuchaPasivaActiva) reiniciarEscuchaPasiva();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(recognizerIntent);
        Toast.makeText(this, "Escucha pasiva ON", Toast.LENGTH_SHORT).show();
    }

    private void reiniciarEscuchaPasiva() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (recognizer != null) {
                recognizer.startListening(recognizerIntent);
            }
        }, 1000);
    }

    private void detenerEscuchaPasiva() {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
        Toast.makeText(this, "Escucha pasiva OFF", Toast.LENGTH_SHORT).show();
    }

    private void guardarRecuerdo(String texto) {
        Toast.makeText(this, "Recordado: " + texto, Toast.LENGTH_LONG).show();
        List<String> etiquetas = new ArrayList<>();
        etiquetas.add("escucha_pasiva");
        memoria.guardarRecuerdo(texto, "curiosidad", 6, etiquetas);
    }

    // ── TTS Y AUDIOFOCUS ─────────────────────────────────────────────────────

    /**
     * Solicita foco de audio y reproduce por TTS si está activado.
     */
    private void hablar(String texto) {
        if (!vozActiva || texto == null || texto.isEmpty()) {
            return;
        }

        // 1) Solicitar foco
        int focusResult;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusResult = audioManager.requestAudioFocus(focusRequest);
        } else {
            focusResult = audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            );
        }

        // 2) Reproducir con TTS
        ttsManager.speak(texto);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Quitar burbuja
        if (windowManager != null && burbujaView != null) {
            windowManager.removeView(burbujaView);
        }
        // Detener reconocimiento
        detenerEscuchaPasiva();
    }
}