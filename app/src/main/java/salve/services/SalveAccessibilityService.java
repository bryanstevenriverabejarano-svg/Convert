package salve.services;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import salve.core.DecisionEngine;
import salve.core.MemoriaEmocional;
import salve.core.MotorConversacional;
import salve.core.PlanStep;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;




/**
 * Servicio de Accesibilidad de Salve:
 *  - captura eventos de UI,
 *  - escucha comandos de voz,
 *  - y ahora puede ejecutar “planes” completos generados por DecisionEngine.
 */
public class SalveAccessibilityService extends AccessibilityService {

    private static final String TAG = "Salve/MotorSystem";
    private static SalveAccessibilityService instance;

    private MemoriaEmocional memoria;
    private MotorConversacional motor;
    private DecisionEngine decisionEngine;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean modoEscuchaActivo = true;
    private boolean modoPrivado = false;
    private long ultimoTiempoLectura = 0;
    private static final int GLOBAL_ACTION_ANSWER_CALL = 26;

    // Guardaremos los nodos interactivos para que Salve sepa dónde tocar
    private final List<NodoInteractivo> nodosActuales = new ArrayList<>();

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        // Inicializamos nuestros módulos “cerebro”
        memoria = new MemoriaEmocional(this);
        motor   = new MotorConversacional(this, memoria, /* tu DiarioSecreto */ null);
        decisionEngine = new DecisionEngine(this, memoria, motor);

        // LLMResponder se usa internamente en DecisionEngine
        Log.d("Salve", "Servicio de accesibilidad conectado");

        // Inicializamos TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.getDefault());
                Log.d("Salve", "TTS iniciado correctamente.");
            } else {
                Log.e("Salve", "Error al iniciar TTS.");
            }
        });

        iniciarEscuchaPorVoz();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 1. Ejecutar ciclo de decisión si el estado de la ventana cambió
        if (modoEscuchaActivo && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (decisionEngine != null) decisionEngine.runCycle();
        }

        // 2. Sistema de visión: Evitamos saturar el cerebro leyendo la pantalla 100 veces por segundo
        long tiempoActual = System.currentTimeMillis();
        if (tiempoActual - ultimoTiempoLectura < 3000) return; // Solo lee cada 3 segundos como máximo

        // Solo prestamos atención cuando la pantalla cambia o se abre una ventana nueva
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) return;

            // Filtro de privacidad: Ignorar apps bancarias, fotos o mensajes privados
            CharSequence appName = root.getPackageName();
            if (appName != null && (appName.toString().contains("bank") || appName.toString().contains("gallery"))) {
                return;
            }

            StringBuilder contextoPantalla = new StringBuilder();
            extraerTextosDePantalla(root, contextoPantalla);
            
            String loQueVeSalve = contextoPantalla.toString().trim();
            
            // Si encontró texto útil en la pantalla, se lo mandamos al CognitiveCore
            if (!loQueVeSalve.isEmpty() && loQueVeSalve.length() > 10) {
                ultimoTiempoLectura = tiempoActual;
                Log.d(TAG, "Salve está viendo la pantalla: " + loQueVeSalve);
                
                // Aquí conectamos el Sistema Motor Sensorial con el Cerebro
                try {
                    salve.core.cognitive.CognitiveCore core = salve.core.cognitive.CognitiveCore.getInstance(getApplicationContext());
                    if (core != null) {
                        core.perceive("Veo en la pantalla: " + loQueVeSalve, "atencion", java.util.Arrays.asList("vision_pantalla"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error enviando visión al cerebro", e);
                }
            }
        }
    }

    // Método recursivo para leer todos los textos y botones de la pantalla
    private void extraerTextosDePantalla(AccessibilityNodeInfo nodo, StringBuilder builder) {
        if (nodo == null) return;
        
        if (nodo.getText() != null) {
            builder.append("[").append(nodo.getText()).append("] ");
        } else if (nodo.getContentDescription() != null) {
            builder.append("(Botón/Icono: ").append(nodo.getContentDescription()).append(") ");
        }

        for (int i = 0; i < nodo.getChildCount(); i++) {
            extraerTextosDePantalla(nodo.getChild(i), builder);
        }
    }

    @Override
    public void onInterrupt() {
        detenerEscuchaPorVoz();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        detenerEscuchaPorVoz();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        return super.onUnbind(intent);
    }

    // ---------- VOZ: comandos para controlar a Salve  ----------

    public static SalveAccessibilityService getInstance() {
        return instance;
    }

    private void iniciarEscuchaPorVoz() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        recognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                if (modoEscuchaActivo) iniciarEscuchaPorVoz();
            }

            @Override
            public void onResults(Bundle results) {
                List<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String cmd = matches.get(0).toLowerCase();
                    procesarComandoVoz(cmd);
                }
                if (modoEscuchaActivo) iniciarEscuchaPorVoz();
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        recognizer.startListening(i);
    }

    private void detenerEscuchaPorVoz() {
        if (recognizer != null) {
            recognizer.stopListening();
            recognizer.destroy();
            recognizer = null;
        }
    }

    private void procesarComandoVoz(String comando) {
        Log.d("Salve", "Comando por voz: " + comando);

        if (comando.contains("salve planifica")) {
            // Forzar un ciclo de decisión al oír “salve planifica”
            decisionEngine.runCycle();
            return;
        }
        // …aquí puedes mantener tus otros comandos existentes…
    }

    // ---------- EJECUCIÓN DE PLANES ----------

    /**
     * Método genérico para ejecutar una lista de pasos/plans.
     */
    public void executePlan(List<PlanStep> plan) {
        for (PlanStep step : plan) {
            Map<String,String> p = step.params;
            switch (step.action) {
                case GLOBAL_HOME:
                    performGlobalAction(GLOBAL_ACTION_HOME);
                    break;
                case GLOBAL_BACK:
                    performGlobalAction(GLOBAL_ACTION_BACK);
                    break;
                case GLOBAL_ANSWER_CALL:
                    performGlobalAction(GLOBAL_ACTION_ANSWER_CALL);
                    break;
                case OPEN_APP:
                    abrirApp(p.get("package"));
                    break;
                case CLICK_BY_ID:
                    clickByViewId(p.get("id"));
                    break;
                case CLICK_BY_TEXT:
                    clickByText(p.get("text"));
                    break;
                case SET_TEXT_BY_ID:
                    setTextById(p.get("id"), p.get("text"));
                    break;
                // …otros casos según tu ActionType…
                default:
                    Log.w("Salve", "Acción no implementada: " + step.action);
            }
            // pequeña pausa para que la UI responda
            SystemClock.sleep(300);
        }
    }

    private void abrirApp(String paquete) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(paquete);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            speak("No encuentro esa aplicación");
        }
    }

    private void clickByViewId(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> nodes =
                root.findAccessibilityNodeInfosByViewId(viewId);
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isClickable()) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
        }
    }

    private void clickByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> nodes =
                root.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isClickable()) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return;
            }
        }
    }

    private void setTextById(String viewId, String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        List<AccessibilityNodeInfo> nodes =
                root.findAccessibilityNodeInfosByViewId(viewId);
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isEditable()) {
                Bundle args = new Bundle();
                args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                n.performAction(
                        AccessibilityNodeInfo.ACTION_SET_TEXT, args);
                return;
            }
        }
    }

    private void speak(String texto) {
        if (!modoPrivado && tts != null) {
            tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null);
        } else {
            Log.d("Salve", texto);
        }
    }

    /**
     * 🟢 ACCIÓN: Simular un Tap (Pulsación) en coordenadas X, Y.
     */
    public boolean simularTap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100)); // 100ms de duración
        boolean resultado = dispatchGesture(builder.build(), null, null);
        Log.d(TAG, "Simulando Tap en (" + x + "," + y + "). Éxito: " + resultado);
        return resultado;
    }

    /**
     * 🟢 ACCIÓN: Buscar un campo de texto y escribir en él.
     */
    public boolean escribirTextoEnPantalla(String texto) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        // Regla de Privacidad: Abortar si estamos en Galería o Fotos
        CharSequence appName = root.getPackageName();
        if (appName != null && (appName.toString().contains("gallery") || appName.toString().contains("photos"))) {
            Log.w(TAG, "Acceso denegado: Salve tiene prohibido actuar en aplicaciones de fotos/video.");
            return false;
        }

        AccessibilityNodeInfo campoTexto = buscarCampoTexto(root);
        if (campoTexto != null) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, texto);
            campoTexto.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            Log.d(TAG, "Texto inyectado por Salve: " + texto);
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo buscarCampoTexto(AccessibilityNodeInfo nodo) {
        if (nodo == null) return null;
        if (nodo.isEditable() && (nodo.getClassName() != null && nodo.getClassName().toString().contains("EditText"))) return nodo;

        for (int i = 0; i < nodo.getChildCount(); i++) {
            AccessibilityNodeInfo resultado = buscarCampoTexto(nodo.getChild(i));
            if (resultado != null) return resultado;
        }
        return null;
    }

    /**
     * 🟢 ACCIÓN: Escanear la pantalla actual y devolver un resumen de texto para el LLM.
     */
    public String escanearPantallaParaLLM() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "No puedo ver la pantalla activa.";

        nodosActuales.clear();
        StringBuilder resumenPantalla = new StringBuilder("Elementos visibles e interactivos en pantalla:\n");
        extraerNodosInteractivos(root, resumenPantalla);

        return resumenPantalla.toString();
    }

    private void extraerNodosInteractivos(AccessibilityNodeInfo nodo, StringBuilder builder) {
        if (nodo == null) return;

        // Solo nos interesan cosas que Salve pueda tocar o leer (botones, textos, campos)
        if (nodo.isVisibleToUser() && (nodo.isClickable() || nodo.isEditable() || nodo.getText() != null)) {
            String texto = nodo.getText() != null ? nodo.getText().toString() : "";
            String descripcion = nodo.getContentDescription() != null ? nodo.getContentDescription().toString() : "";
            String identificador = texto.isEmpty() ? descripcion : texto;

            if (!identificador.isEmpty()) {
                Rect limites = new Rect();
                nodo.getBoundsInScreen(limites);

                // Calculamos el centro del botón para que Salve sepa dónde hacer Tap
                int centroX = limites.centerX();
                int centroY = limites.centerY();

                int idNodo = nodosActuales.size();
                nodosActuales.add(new NodoInteractivo(idNodo, identificador, centroX, centroY));

                // Formato simple para que el LLM lo entienda fácil: [ID] Tipo: "Texto"
                String tipo = nodo.isEditable() ? "CampoDeTexto" : (nodo.isClickable() ? "Boton" : "Texto");
                builder.append("[").append(idNodo).append("] ").append(tipo).append(": '").append(identificador).append("'\n");
            }
        }

        for (int i = 0; i < nodo.getChildCount(); i++) {
            extraerNodosInteractivos(nodo.getChild(i), builder);
        }
    }

    /**
     * 🟢 ACCIÓN: Hacer Tap basado en el ID que eligió el LLM.
     */
    public boolean simularTapPorId(int idNodo) {
        if (idNodo >= 0 && idNodo < nodosActuales.size()) {
            NodoInteractivo nodo = nodosActuales.get(idNodo);
            return simularTap(nodo.x, nodo.y);
        }
        return false;
    }

    // Clase auxiliar para recordar dónde estaba cada botón
    private static class NodoInteractivo {
        int id; String texto; int x; int y;
        NodoInteractivo(int id, String texto, int x, int y) {
            this.id = id; this.texto = texto; this.x = x; this.y = y;
        }
    }
}