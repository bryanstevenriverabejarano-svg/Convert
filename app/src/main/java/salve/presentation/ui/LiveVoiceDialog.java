package salve.presentation.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import salve.core.MotorConversacional;
import salve.core.voice.VoiceConversationLoop;
import salve.core.voice.VoiceTurnMetrics;

/** Foreground, automatic voice turns. The microphone stays closed during synthesis. */
public final class LiveVoiceDialog extends Dialog implements MotorConversacional.VoiceConversationListener {
    private final Activity activity;
    private final MotorConversacional motor;
    private final VoiceConversationLoop loop = new VoiceConversationLoop();
    private final VoiceTurnMetrics metrics = new VoiceTurnMetrics();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable watchdog = () -> apply(loop.tick(now()));
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = change -> {
        if (change == AudioManager.AUDIOFOCUS_LOSS
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_FOCUS));
        }
    };

    private SpeechRecognizer recognizer;
    private AudioFocusRequest audioFocusRequest;
    private boolean focusHeld;
    private boolean started;
    private boolean dismissed;
    private boolean resourcesReleased;
    private TextView status;
    private TextView backend;
    private TextView transcript;
    private TextView reply;
    private Button interrupt;
    private String recognitionErrorText;
    private long displayedReplyToken = -1L;

    public LiveVoiceDialog(Activity activity, MotorConversacional motor) {
        super(activity);
        this.activity = activity;
        this.motor = motor;
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        setCanceledOnTouchOutside(false);
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Conversar con Salve");
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        content.setPadding(padding, padding, padding, padding);

        status = label("Preparando la voz…", 20);
        status.setTypeface(null, Typeface.BOLD);
        content.addView(status);
        content.addView(label("Te escucho cuando termino de hablar. Para adelantar tu turno, toca Interrumpir. "
                + "La sesión termina al salir o a los 10 minutos.", 14));
        backend = label(motor.getVoiceStatus(), 13);
        content.addView(backend);
        transcript = label("Tú: …", 17);
        reply = label("Salve: …", 17);
        content.addView(transcript);
        content.addView(reply);

        interrupt = new Button(activity);
        interrupt.setText("Interrumpir y hablar");
        interrupt.setOnClickListener(view -> {
            long timestamp = now();
            VoiceConversationLoop.Step step = loop.interrupt(timestamp);
            if (step.action != VoiceConversationLoop.Action.NONE) {
                if (step.action != VoiceConversationLoop.Action.STOP) {
                    metrics.interrupt(metrics.token(), timestamp);
                    reportMetrics();
                }
                motor.cancelarTurnoVoz();
            }
            apply(step);
        });
        content.addView(interrupt);
        Button exit = new Button(activity);
        exit.setText("Salir del modo voz");
        exit.setOnClickListener(view -> {
            apply(loop.stop(VoiceConversationLoop.StopReason.USER));
            dismiss();
        });
        content.addView(exit);
        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        setContentView(scroll);
    }

    @Override protected void onStart() {
        super.onStart();
        Window window = getWindow();
        if (window != null) window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (started || dismissed) return;
        started = true;
        VoiceConversationLoop.Step first = loop.start(now());
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            apply(loop.stop(VoiceConversationLoop.StopReason.MICROPHONE_PERMISSION));
        } else if (!motor.isVoiceReady()) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_UNAVAILABLE));
        } else if (!requestAudioFocus()) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_FOCUS));
        } else {
            if (window != null) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            apply(first);
        }
    }

    @Override public void dismiss() {
        apply(loop.stop(VoiceConversationLoop.StopReason.LIFECYCLE));
        dismissed = true;
        releaseResources();
        super.dismiss();
    }

    @Override public void cancel() {
        apply(loop.stop(VoiceConversationLoop.StopReason.USER));
        super.cancel();
    }

    @Override protected void onStop() {
        apply(loop.stop(VoiceConversationLoop.StopReason.LIFECYCLE));
        releaseResources();
        super.onStop();
    }

    private void apply(VoiceConversationLoop.Step step) {
        if (dismissed || step.action == VoiceConversationLoop.Action.NONE) return;
        switch (step.action) {
            case LISTEN:
                beginRecognition(step.token);
                break;
            case SUBMIT:
                destroyRecognizer();
                metrics.submitted(step.token, now());
                transcript.setText("Tú: " + step.text);
                reply.setText("Salve: preparando respuesta…");
                try {
                    motor.procesarEntradaVoz(step.text, step.token, this);
                } catch (RuntimeException unavailable) {
                    apply(loop.stop(VoiceConversationLoop.StopReason.RESPONSE_ERROR));
                }
                break;
            case WAIT:
                destroyRecognizer();
                if (loop.state() == VoiceConversationLoop.State.COOLDOWN && metrics.isActive()
                        && !metrics.hasSubmitted()) {
                    // The application watchdog or an empty final transcript ended this capture.
                    metrics.stop(metrics.token(), now(), VoiceConversationLoop.StopReason.NO_SPEECH);
                    reportMetrics();
                }
                break;
            case STOP:
                metrics.stop(metrics.token(), now(), loop.stopReason());
                reportMetrics();
                releaseResources();
                break;
            default: break;
        }
        renderState();
        scheduleWatchdog();
    }

    private void beginRecognition(long token) {
        destroyRecognizer();
        if (!loop.owns(token)) return;
        if (!metrics.begin(token, now())) return;
        recognitionErrorText = null;
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            apply(loop.stop(VoiceConversationLoop.StopReason.MICROPHONE_PERMISSION));
            return;
        }
        try {
            boolean onDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && SpeechRecognizer.isOnDeviceRecognitionAvailable(activity);
            if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(activity)) {
                recognitionErrorText = "No hay un servicio de reconocimiento disponible en Android.";
                apply(loop.stop(VoiceConversationLoop.StopReason.RECOGNIZER_UNAVAILABLE));
                return;
            }
            final SpeechRecognizer capture = onDevice
                    ? SpeechRecognizer.createOnDeviceSpeechRecognizer(activity)
                    : SpeechRecognizer.createSpeechRecognizer(activity);
            recognizer = capture;
            backend.setText((onDevice ? "Reconocimiento: servicio local de Android. "
                    : "Reconocimiento: servicio de Android; puede enviar audio por Internet. ")
                    + "\n" + motor.getVoiceStatus());
            motor.setListening(true);
            capture.setRecognitionListener(new RecognitionListener() {
                private boolean current() {
                    return !dismissed && recognizer == capture && loop.owns(token)
                            && loop.state() == VoiceConversationLoop.State.LISTENING;
                }

                @Override public void onReadyForSpeech(Bundle params) {
                    if (current() && metrics.ready(token, now())) renderState();
                }
                @Override public void onBeginningOfSpeech() {
                    if (current() && metrics.speechBegan(token, now())) renderState();
                }
                @Override public void onRmsChanged(float rmsdB) { }
                @Override public void onBufferReceived(byte[] buffer) { }
                @Override public void onEndOfSpeech() {
                    if (current() && metrics.speechEnded(token, now())) renderState();
                }
                @Override public void onError(int error) {
                    if (!current()) return;
                    long timestamp = now();
                    if (!metrics.recognitionFailed(token, timestamp, error)) return;
                    recognitionErrorText = recognitionErrorMessage(error);
                    reportMetrics();
                    destroyRecognizer();
                    apply(loop.recognitionFailed(token, recognitionFailure(error), timestamp));
                }
                @Override public void onResults(Bundle results) {
                    if (!current()) return;
                    long timestamp = now();
                    if (!metrics.recognized(token, timestamp)) return;
                    ArrayList<String> matches = results == null ? null
                            : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                    destroyRecognizer();
                    apply(loop.recognized(token, text, timestamp));
                }
                @Override public void onPartialResults(Bundle partialResults) {
                    if (!current() || partialResults == null) return;
                    ArrayList<String> partials = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partials != null && !partials.isEmpty()) transcript.setText("Tú: " + partials.get(0));
                }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, motor.getRecognitionLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            if (onDevice) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            capture.startListening(intent);
        } catch (SecurityException denied) {
            apply(loop.stop(VoiceConversationLoop.StopReason.MICROPHONE_PERMISSION));
        } catch (RuntimeException unavailable) {
            // Do not silently switch a failed local recognizer to a remote provider.
            recognitionErrorText = "No se pudo iniciar el servicio de reconocimiento de Android.";
            apply(loop.stop(VoiceConversationLoop.StopReason.RECOGNIZER_UNAVAILABLE));
        }
    }

    private static VoiceConversationLoop.RecognitionFailure recognitionFailure(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            case SpeechRecognizer.ERROR_NO_MATCH:
                return VoiceConversationLoop.RecognitionFailure.SILENCE;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_SERVER:
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return VoiceConversationLoop.RecognitionFailure.TRANSIENT;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return VoiceConversationLoop.RecognitionFailure.PERMISSION;
            default:
                return VoiceConversationLoop.RecognitionFailure.UNAVAILABLE;
        }
    }

    private static String recognitionErrorMessage(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_NETWORK:
                return "Falló la conexión con el servicio de reconocimiento.";
            case SpeechRecognizer.ERROR_SERVER:
            case SpeechRecognizer.ERROR_SERVER_DISCONNECTED:
                return "El servicio de reconocimiento dejó de responder.";
            case SpeechRecognizer.ERROR_AUDIO:
                return "Android no pudo capturar audio del micrófono.";
            case SpeechRecognizer.ERROR_CLIENT:
                return "El servicio rechazó la solicitud de reconocimiento.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "El servicio de reconocimiento está ocupado.";
            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                return "El servicio ha limitado las solicitudes de reconocimiento. Espera antes de volver a abrir el modo voz.";
            case SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED:
                return "El servicio no admite el idioma de la voz elegida. Revisa los idiomas de reconocimiento de Android.";
            case SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE:
                return "Faltan datos de reconocimiento para el idioma elegido. Revisa su instalación en Android.";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "El servicio no tiene permiso para usar el micrófono.";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No reconocí una frase.";
            default:
                return "El reconocimiento falló (código " + error + ").";
        }
    }

    private void destroyRecognizer() {
        SpeechRecognizer old = recognizer;
        recognizer = null;
        if (old != null) {
            try { old.cancel(); } catch (RuntimeException ignored) { }
            try { old.destroy(); } catch (RuntimeException ignored) { }
        }
        motor.setListening(false);
    }

    private boolean requestAudioFocus() {
        if (audioManager == null) return false;
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANT).build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusListener, handler).build();
            focusHeld = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            return focusHeld;
        } catch (RuntimeException unavailable) { return false; }
    }

    private void releaseResources() {
        handler.removeCallbacks(watchdog);
        Window window = getWindow();
        if (window != null) window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        destroyRecognizer();
        if (!resourcesReleased) {
            resourcesReleased = true;
            motor.cancelarTurnoVoz();
        }
        if (focusHeld && audioManager != null && audioFocusRequest != null) {
            focusHeld = false;
            try { audioManager.abandonAudioFocusRequest(audioFocusRequest); } catch (RuntimeException ignored) { }
        }
    }

    private void scheduleWatchdog() {
        handler.removeCallbacks(watchdog);
        if (loop.isActive() && !dismissed) {
            handler.postDelayed(watchdog, Math.max(1L, loop.nextDeadlineMillis() - now()));
        }
    }

    private void renderState() {
        if (status == null) return;
        interrupt.setEnabled(loop.isActive() && loop.state() != VoiceConversationLoop.State.LISTENING);
        switch (loop.state()) {
            case LISTENING:
                switch (metrics.captureStage()) {
                    case STARTING: status.setText("Preparando el micrófono…"); break;
                    case FINALIZING:
                    case FINISHED: status.setText("Terminando la transcripción…"); break;
                    default: status.setText("Te escucho…"); break;
                }
                break;
            case THINKING: status.setText("Preparando respuesta…"); break;
            case SPEAKING: status.setText("Salve está hablando…"); break;
            case COOLDOWN:
                status.setText(recognitionErrorText == null ? "Enseguida te escucho…"
                        : recognitionErrorText + " Volveré a intentarlo…");
                break;
            case STOPPED:
                boolean recognitionFailure = loop.stopReason() == VoiceConversationLoop.StopReason.RECOGNIZER_ERROR
                        || loop.stopReason() == VoiceConversationLoop.StopReason.RECOGNIZER_UNAVAILABLE;
                status.setText(recognitionFailure && recognitionErrorText != null
                        ? recognitionErrorText + " He cerrado el micrófono." : stopMessage(loop.stopReason()));
                break;
            default: break;
        }
    }

    private static String stopMessage(VoiceConversationLoop.StopReason reason) {
        switch (reason) {
            case SESSION_LIMIT: return "Sesión terminada: han pasado 10 minutos. Puedes abrir otra cuando quieras.";
            case NO_SPEECH: return "No detecté una frase. He cerrado el micrófono; puedes volver a abrir el modo voz.";
            case MICROPHONE_PERMISSION: return "Hace falta permitir el micrófono para conversar por voz.";
            case RECOGNIZER_UNAVAILABLE: return "El reconocimiento no está disponible. Revisa el servicio de voz de Android.";
            case RECOGNIZER_ERROR: return "No pude recuperar el reconocimiento. He cerrado el micrófono.";
            case RESPONSE_TIMEOUT: return "La respuesta está tardando demasiado. He detenido esta sesión de voz.";
            case RESPONSE_ERROR: return "No pude preparar la respuesta. He detenido esta sesión de voz.";
            case SPEECH_TIMEOUT: return "La voz no terminó a tiempo. He detenido esta sesión.";
            case AUDIO_UNAVAILABLE: return "No hay una voz utilizable ahora. Revisa Voz de Salve; la respuesta escrita sigue visible.";
            case AUDIO_ERROR: return "La síntesis de voz se interrumpió. La respuesta escrita sigue visible.";
            case AUDIO_FOCUS: return "Modo voz detenido: otra aplicación o una llamada necesita el audio.";
            default: return "Modo voz terminado. Micrófono cerrado.";
        }
    }

    @Override public void onReply(long token, String text, boolean audioQueued) {
        if (dismissed || !loop.owns(token) || displayedReplyToken == token) return;
        displayedReplyToken = token;
        long timestamp = now();
        // A very short utterance can finish before its reply callback; still show its text once.
        metrics.replied(token, timestamp);
        reply.setText("Salve: " + (text == null ? "" : text));
        apply(loop.replied(token, audioQueued, timestamp));
    }

    @Override public void onSpeechStarted(long token) {
        long timestamp = now();
        if (!dismissed && loop.owns(token) && metrics.speechStarted(token, timestamp))
            apply(loop.speechStarted(token, timestamp));
    }

    @Override public void onSpeechFinished(long token, boolean completed) {
        if (dismissed || !loop.owns(token)) return;
        long timestamp = now();
        if (!metrics.speechFinished(token, timestamp)) return;
        VoiceConversationLoop.Step step = loop.speechFinished(token, completed, timestamp);
        if (step.action == VoiceConversationLoop.Action.WAIT && completed) {
            metrics.complete(token, timestamp);
            reportMetrics();
        }
        apply(step);
    }

    private void reportMetrics() {
        String line = metrics.takeLogLine();
        if (line != null) Log.i("Salve/Voice", line);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(activity);
        view.setText(text);
        view.setTextSize(size);
        view.setPadding(0, dp(6), 0, dp(10));
        return view;
    }

    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private static long now() { return SystemClock.elapsedRealtime(); }
}
