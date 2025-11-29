package salve.services;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
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

    private MemoriaEmocional memoria;
    private MotorConversacional motor;
    private DecisionEngine decisionEngine;
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private boolean modoEscuchaActivo = true;
    private boolean modoPrivado = false;
    private static final int GLOBAL_ACTION_ANSWER_CALL = 26;
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

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
        // Aquí puedes seguir guardando recuerdos o reaccionando a la UI,
        // y además disparar tu “ciclo de decisión” según convenga:
        if (modoEscuchaActivo && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // Cada vez que cambia pantalla, por ejemplo:
            decisionEngine.runCycle();        // 1) genera planes, 2) puntúa, 3) escoge, 4) ejecuta
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
}