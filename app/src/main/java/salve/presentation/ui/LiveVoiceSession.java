package salve.presentation.ui;

import android.Manifest;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import android.view.View;
import com.salve.app.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import salve.core.voice.VoiceLabReport;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.text.Normalizer;
import java.util.Locale;

import salve.core.MotorConversacional;
import salve.core.voice.VoiceConversationLoop;
import salve.core.voice.VoiceBargeInMonitor;
import salve.core.voice.VoiceTurnMetrics;
import salve.core.voice.VoiceDiagnosticLog;

/** Foreground voice turns, with guarded echo-cancelled interruption during TTS. */
public final class LiveVoiceSession implements MotorConversacional.VoiceConversationListener {
    private final AppCompatActivity activity;
    private final SalveStageController stage;
    private final View controls;
    private final TextView caption;
    private String backend = "";
    private final OnBackPressedCallback back = new OnBackPressedCallback(true) {
        @Override public void handleOnBackPressed() {
            if (stage.hasOpenPanel()) stage.closePanels(); else dismiss();
        }
    };
    private final MotorConversacional motor;
    /** Manos libres: tras cada respuesta vuelve a escuchar sola hasta «adiós, Salve» o 3 minutos de silencio. */
    private final VoiceConversationLoop loop = new VoiceConversationLoop(true);
    private final VoiceTurnMetrics metrics = new VoiceTurnMetrics();
    private VoiceDiagnosticLog diagnostics;
    private final VoiceLabReport voiceLab = new VoiceLabReport();
    /** AEC-only one-shot detector; the existing SpeechRecognizer still owns transcribed input. */
    private final VoiceBargeInMonitor bargeIn = new VoiceBargeInMonitor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable watchdog = () -> apply(loop.tick(now()));
    private final AudioManager audioManager;
    private final AudioManager.OnAudioFocusChangeListener audioFocusListener = change -> {
        diagnostics.event(metrics.token(), VoiceDiagnosticLog.Event.AUDIO_FOCUS_CHANGE, change);
        if (change == AudioManager.AUDIOFOCUS_LOSS
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_FOCUS));
        }
    };

    private SpeechRecognizer recognizer;
    /** Modo oídos: micrófono propio + oído del modelo, en lugar del dictado de Android. */
    private salve.devices.EarCapture ear;
    /** Los oídos fallaron en esta sesión: se sigue con el dictado hasta abrir otra. */
    private boolean earFailed;
    private AudioFocusRequest audioFocusRequest;
    private boolean focusHeld;
    private boolean automaticInterruptDispatch;
    private boolean started;
    private boolean dismissed;
    private boolean resourcesReleased;
    private TextView status;
    private Button interrupt;
    private String recognitionErrorText;
    /**
     * Tras «faltan datos del idioma» en el reconocimiento sin red, esta sesión usa el reconocimiento
     * normal de Android (puede usar Internet) y se pide la descarga del paquete para la próxima vez.
     */
    private boolean standardRecognizer;
    /** Botón de los auriculares (play/pausa o el botón del cable): corta a Salve o vuelve a escuchar. */
    private android.media.session.MediaSession headsetButton;
    private long displayedReplyToken = -1L;
    /** Primera petición dicha junto al nombre («Salve, ¿qué hora es?»): se envía sin volver a escuchar. */
    private String initialRequest;
    private static volatile int showing;
    /** Último estado enviado a la notificación (para no repetirlo en cada refresco). */
    private String notified;

    /** ¿Hay alguna conversación por voz abierta? (La palabra de activación cede el micrófono mientras tanto.) */
    public static boolean isAnyShowing() { return showing > 0; }

    public void setInitialRequest(String request) { initialRequest = request; }

    public LiveVoiceSession(AppCompatActivity activity, MotorConversacional motor, SalveStageController stage) {
        this.activity = activity;
        this.motor = motor;
        this.stage = stage;
        this.audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        this.diagnostics = new VoiceDiagnosticLog(activity);
        controls = activity.findViewById(R.id.voiceStageControls);
        caption = activity.findViewById(R.id.salveCaption);
        status = activity.findViewById(R.id.voiceStageStatus);
        interrupt = activity.findViewById(R.id.btnVoiceInterrupt);
        status.setOnClickListener(view -> stage.showVoiceDetails());
        status.setOnLongClickListener(view -> {
            if (!voiceLab.isEnabled()) {
                voiceLab.start();
                voiceLab.event(loop.state() == VoiceConversationLoop.State.STOPPED ? 0 : metrics.token(),
                        "exam", "started");
                Toast.makeText(activity, "VoiceLab iniciado: mantén pulsado el estado para ver el informe", Toast.LENGTH_LONG).show();
            } else {
                voiceLab.event(metrics.token(), "exam", "finished");
                voiceLab.stop();
                presentVoiceLab();
            }
            return true;
        });
        interrupt.setOnClickListener(view -> {
            if (!loop.isActive()) {
                dismiss();
                activity.findViewById(R.id.btnHablar).performClick();
                return;
            }
            bargeIn.stop();
            if (!automaticInterruptDispatch) {
                voiceLab.event(metrics.token(), "interrupt", "manual_button");
                diagnostics.event(metrics.token(), VoiceDiagnosticLog.Event.INTERRUPT_MANUAL, 0);
            }
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
        activity.findViewById(R.id.btnVoiceEnd).setOnClickListener(view -> {
            apply(loop.stop(VoiceConversationLoop.StopReason.USER)); dismiss();
        });
    }

    /** Generate privately; the user decides whether to download from the chat. */
    private boolean presentVoiceLab() {
        File reports = new File(activity.getFilesDir(), "voicelab");
        if (!reports.exists() && !reports.mkdirs()) {
            Toast.makeText(activity, "No se pudo preparar VoiceLab", Toast.LENGTH_LONG).show();
            return false;
        }
        File pdf = new File(reports, "informe-" + System.currentTimeMillis() + ".pdf");
        try {
            voiceLab.writePdf(pdf);
            stage.showPdfAttachment(pdf);
            return true;
        } catch (Exception error) {
            Log.e("Salve/VoiceLab", "PDF generation failed", error);
            if (pdf.exists()) pdf.delete();
            Toast.makeText(activity, "No se pudo generar el informe", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    /** Local voice diagnostic intent; reuses the current Android recognizer. */
    private boolean handleVoiceLabCommand(String utterance) {
        if (utterance == null) return false;
        String normalized = Normalizer.normalize(utterance.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9 ]", " ").replaceAll(" +", " ").trim();
        boolean exam = normalized.contains("examen de voz")
                || normalized.contains("prueba de voz")
                || normalized.contains("voicelab") || normalized.contains("voice lab");
        boolean report = normalized.contains("informe")
                && (normalized.contains("voz") || normalized.contains("examen")
                    || normalized.contains("diagnostico") || normalized.contains("voicelab"));
        boolean start = normalized.contains("inicia") || normalized.contains("iniciar")
                || normalized.contains("empieza") || normalized.contains("comienza")
                || normalized.contains("haz un");
        boolean finish = normalized.contains("termina") || normalized.contains("finaliza")
                || normalized.contains("deten") || normalized.contains("acaba")
                || normalized.contains("dame") || normalized.contains("genera")
                || normalized.contains("descarga") || normalized.contains("guarda")
                || normalized.contains("exporta");
        if (exam && start && !finish) {
            if (!voiceLab.isEnabled()) {
                voiceLab.start();
                voiceLab.event(metrics.token(), "exam", "started_by_voice");
                Toast.makeText(activity, "VoiceLab iniciado", Toast.LENGTH_LONG).show();
                caption.setText("Salve: He iniciado el examen de voz.");
            } else caption.setText("Salve: El examen ya está en marcha.");
            return true;
        }
        if ((report || exam) && finish) {
            if (!voiceLab.isEnabled()) {
                caption.setText("Salve: Primero inicia un examen de voz.");
                return true;
            }
            voiceLab.event(metrics.token(), "exam", "finished_by_voice");
            voiceLab.stop();
            boolean ready = presentVoiceLab();
            caption.setText(ready
                    ? "Salve: He puesto el informe en el chat para que lo revises."
                    : "Salve: No pude generar el informe de voz.");
            return true;
        }
        return false;
    }

    public boolean isShowing() { return started && !dismissed; }

    public void show() {
        if (started || dismissed) return;
        started = true;
        diagnostics.start();
        stage.closePanels();
        controls.setVisibility(View.VISIBLE);
        activity.findViewById(R.id.salveGestureHint).setVisibility(View.GONE);
        activity.getOnBackPressedDispatcher().addCallback(activity, back);
        status.setText("Preparando la voz…");
        backend = motor.getVoiceStatus();
        showing++;
        salve.services.WakeWordService.pauseForConversation();
        VoiceConversationLoop.Step first = loop.start(now());
        if (initialRequest != null && first.action == VoiceConversationLoop.Action.LISTEN) {
            VoiceConversationLoop.Step submit = loop.recognized(first.token, initialRequest, now());
            if (submit.action == VoiceConversationLoop.Action.SUBMIT) {
                long at = now();
                if (metrics.begin(first.token, at)) metrics.recognized(first.token, at);
            }
            if (submit.action == VoiceConversationLoop.Action.SUBMIT
                    || submit.action == VoiceConversationLoop.Action.CONTINUE) first = submit;
            initialRequest = null;
        }
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            apply(loop.stop(VoiceConversationLoop.StopReason.MICROPHONE_PERMISSION));
        } else if (!motor.isVoiceReady()) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_UNAVAILABLE));
        } else if (!requestAudioFocus()) {
            apply(loop.stop(VoiceConversationLoop.StopReason.AUDIO_FOCUS));
        } else {
            controls.setKeepScreenOn(true);
            listenToHeadsetButton();
            // Sigue contigo si sales de la app o apagas la pantalla, con notificación y «Terminar».
            salve.services.VoiceForegroundService.start(activity, "Te escucho…", this::endFromNotification);
            // Conversation brings her out of the sleeping pose so speech gestures stay visible.
            salve.avatar.AvatarStore.get(activity).change(salve.avatar.AvatarState::wake);
            apply(first);
        }
    }

    /** «Terminar» en la notificación: igual que el botón Fin de la pantalla. */
    private void endFromNotification() {
        if (dismissed) { salve.services.VoiceForegroundService.stop(activity); return; }
        apply(loop.stop(VoiceConversationLoop.StopReason.USER));
        dismiss();
    }

    /**
     * La conversación puede seguir con la app en segundo plano: está abierta, el micrófono sigue en
     * uso y el servicio con notificación visible está activo. Si no, al salir se cierra como siempre.
     */
    public boolean runsInBackground() {
        return started && !dismissed && loop.isActive() && salve.services.VoiceForegroundService.isRunning();
    }

    /** Called by End, Back, onPause (unless it continues in background) and onDestroy. Never a hidden microphone. */
    public void dismiss() {
        if (dismissed) return;
        apply(loop.stop(VoiceConversationLoop.StopReason.LIFECYCLE));
        dismissed = true;
        if (showing > 0) showing--;
        salve.services.WakeWordService.resumeAfterConversation();
        stage.setVoicePresence(null);
        releaseResources();
        diagnostics.finish(metrics.token());
        controls.setVisibility(View.GONE);
        activity.findViewById(R.id.salveGestureHint).setVisibility(View.VISIBLE);
        back.remove();
    }

    private void apply(VoiceConversationLoop.Step step) {
        if (dismissed || step.action == VoiceConversationLoop.Action.NONE) return;
        voiceLab.event(step.token, "loop", step.action.name() + " state=" + loop.state().name());
        switch (step.action) {
            case LISTEN:
                bargeIn.stop();
                diagnostics.event(step.token, VoiceDiagnosticLog.Event.LISTEN_STARTED, 0);
                beginRecognition(step.token);
                break;
            case CONTINUE:
                bargeIn.stop();
                // Android finalized an unfinished clause. Keep it only in VoiceConversationLoop,
                // close this recognizer and rearm the SAME session after a brief delay.
                destroyRecognizer();
                if (metrics.complete(step.token, now())) reportMetrics();
                caption.setText("Tú: " + step.text + " …");
                break;
            case SUBMIT:
                bargeIn.stop();
                destroyRecognizer();
                metrics.submitted(step.token, now());
                stage.closePanels();
                stage.appendMessage(false, step.text);
                caption.setText("Tú: " + step.text);
                if (handleVoiceLabCommand(step.text)) {
                    long at = now();
                    metrics.replied(step.token, at);
                    apply(loop.replied(step.token, false, at));
                    break;
                }
                try {
                    motor.procesarEntradaVoz(step.text, step.token, this);
                } catch (RuntimeException unavailable) {
                    apply(loop.stop(VoiceConversationLoop.StopReason.RESPONSE_ERROR));
                }
                break;
            case WAIT:
                if (loop.state() != VoiceConversationLoop.State.SPEAKING) bargeIn.stop();
                destroyRecognizer();
                if (loop.state() == VoiceConversationLoop.State.COOLDOWN && metrics.isActive()
                        && !metrics.hasSubmitted()) {
                    // The application watchdog or an empty final transcript ended this capture.
                    metrics.stop(metrics.token(), now(), VoiceConversationLoop.StopReason.NO_SPEECH);
                    reportMetrics();
                }
                break;
            case STOP:
                diagnostics.event(metrics.token(), VoiceDiagnosticLog.Event.LOOP_STOP,
                        loop.stopReason() == null ? -1 : loop.stopReason().ordinal());
                bargeIn.stop();
                metrics.stop(metrics.token(), now(), loop.stopReason());
                reportMetrics();
                releaseResources();
                diagnostics.finish(metrics.token());
                break;
            default: break;
        }
        renderState();
        scheduleWatchdog();
    }

    private void beginRecognition(long token) {
        bargeIn.stop();
        destroyRecognizer();
        if (!loop.owns(token)) return;
        if (!metrics.begin(token, now())) return;
        voiceLab.event(token, "asr", "capture_started");
        recognitionErrorText = null;
        if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            apply(loop.stop(VoiceConversationLoop.StopReason.MICROPHONE_PERMISSION));
            return;
        }
        if (!earFailed && motor.earModeUsable()) {
            beginEarCapture(token);
            return;
        }
        try {
            boolean onDevice = !standardRecognizer && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && SpeechRecognizer.isOnDeviceRecognitionAvailable(activity);
            final String language = recognitionLanguage();
            if (!onDevice && !SpeechRecognizer.isRecognitionAvailable(activity)) {
                recognitionErrorText = "No hay un servicio de reconocimiento disponible en Android.";
                apply(loop.stop(VoiceConversationLoop.StopReason.RECOGNIZER_UNAVAILABLE));
                return;
            }
            final SpeechRecognizer capture = onDevice
                    ? SpeechRecognizer.createOnDeviceSpeechRecognizer(activity)
                    : SpeechRecognizer.createSpeechRecognizer(activity);
            recognizer = capture;
            backend = (onDevice ? "Reconocimiento: servicio local de Android (" + language + "). "
                    : "Reconocimiento: servicio de Android (" + language + "); puede enviar audio por Internet. ")
                    + "\n" + motor.getVoiceStatus();
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
                    voiceLab.event(token, "asr", "error=" + error);
                    diagnostics.event(token, VoiceDiagnosticLog.Event.RECOGNITION_ERROR, error);
                    VoiceConversationLoop.RecognitionFailure failure = recognitionFailure(error);
                    // When a previous clause is held, a no-speech result finishes that same
                    // user turn. Record it as successful ASR, never as a failed/double turn.
                    boolean flushHeld = failure == VoiceConversationLoop.RecognitionFailure.SILENCE
                            && loop.hasHeldSpeech();
                    if (flushHeld) {
                        if (!metrics.recognized(token, timestamp)) return;
                        recognitionErrorText = null;
                        destroyRecognizer();
                        apply(loop.recognitionFailed(token, failure, timestamp));
                        return;
                    }
                    if (!metrics.recognitionFailed(token, timestamp, error)) return;
                    recognitionErrorText = recognitionErrorMessage(error);
                    reportMetrics();
                    if (onDevice && languageMissing(error) && SpeechRecognizer.isRecognitionAvailable(activity)) {
                        // El paquete sin red de ese idioma no está instalado: se pide su descarga para la
                        // próxima vez y, mientras, se reintenta con el reconocimiento normal de Android.
                        requestLanguageDownload(capture, language);
                        standardRecognizer = true;
                        recognitionErrorText = "Falta el paquete de voz sin Internet (" + language
                                + "); lo pido a Android y mientras uso su reconocimiento normal.";
                        failure = VoiceConversationLoop.RecognitionFailure.TRANSIENT;
                    }
                    destroyRecognizer();
                    apply(loop.recognitionFailed(token, failure, timestamp));
                }
                @Override public void onResults(Bundle results) {
                    if (!current()) return;
                    long timestamp = now();
                    if (!metrics.recognized(token, timestamp)) return;
                    ArrayList<String> matches = results == null ? null
                            : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String text = matches == null || matches.isEmpty() ? "" : matches.get(0);
                    voiceLab.event(token, "asr", "final_chars=" + text.length());
                    destroyRecognizer();
                    apply(loop.recognized(token, text, timestamp));
                }
                @Override public void onPartialResults(Bundle partialResults) {
                    if (!current() || partialResults == null) return;
                    ArrayList<String> partials = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (partials != null && !partials.isEmpty()) caption.setText("Tú: " + partials.get(0));
                }
                @Override public void onEvent(int eventType, Bundle params) { }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
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

    /**
     * Lo que importa para reconocer es cómo habla Bryan, no el acento de la voz de Salve: si el móvil
     * está en español se usa su idioma (es-ES en un Galaxy de España, cuyo paquete sin red ya está
     * instalado); antes se pedía es-MX por la voz y Android respondía «faltan datos».
     */
    private String recognitionLanguage() {
        return salve.core.voice.VoiceSelectionPolicy.speakerLanguageTag(
                java.util.Locale.getDefault(), motor.getRecognitionLanguageTag());
    }

    private static boolean languageMissing(int error) {
        return error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE || error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED;
    }

    private void requestLanguageDownload(SpeechRecognizer recognizer, String language) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        try {
            Intent download = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language);
            recognizer.triggerModelDownload(download);
        } catch (RuntimeException unavailable) {
            Log.w("Salve/Voz", "Android no aceptó descargar el paquete de " + language);
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

    /**
     * Modo oídos: Salve graba la frase con su propio micrófono y la oye con su modelo (Gemma 4 sin
     * censura): entiende lo que el dictado escribe mal y nota el tono. Si algo falla, el dictado.
     */
    private void beginEarCapture(long token) {
        final salve.devices.EarCapture capture = new salve.devices.EarCapture();
        ear = capture;
        backend = "Escucha: oídos de Salve (su propio modelo, sin Internet).\n" + motor.getVoiceStatus();
        motor.setListening(true);
        if (metrics.ready(token, now())) renderState();
        java.util.function.BooleanSupplier current = () -> !dismissed && ear == capture && loop.owns(token)
                && loop.state() == VoiceConversationLoop.State.LISTENING;
        capture.start(new salve.devices.EarCapture.Listener() {
            @Override public void onSpeechStarted() {
                handler.post(() -> { if (current.getAsBoolean() && metrics.speechBegan(token, now())) renderState(); });
            }
            @Override public void onPhrase(byte[] wav) {
                handler.post(() -> {
                    if (!current.getAsBoolean()) return;
                    if (metrics.speechEnded(token, now())) renderState();
                    caption.setText("Te estoy oyendo…");
                });
                salve.core.voice.EarMode.Heard heard;
                try { heard = motor.oirFrase(wav, () -> !current.getAsBoolean()); }
                catch (RuntimeException failed) {
                    handler.post(() -> earFallback(token, capture, failed.getMessage()));
                    return;
                }
                handler.post(() -> {
                    if (!current.getAsBoolean()) return;
                    long timestamp = now();
                    if (!metrics.recognized(token, timestamp)) return;
                    voiceLab.event(token, "oidos", "final_chars=" + heard.text.length() + " tono=" + heard.tone);
                    destroyRecognizer();
                    apply(loop.recognized(token, heard.text, timestamp));
                });
            }
            @Override public void onNoSpeech() {
                handler.post(() -> {
                    if (!current.getAsBoolean()) return;
                    long timestamp = now();
                    if (loop.hasHeldSpeech()) {
                        if (!metrics.recognized(token, timestamp)) return;
                    } else if (!metrics.recognitionFailed(token, timestamp, SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) return;
                    destroyRecognizer();
                    apply(loop.recognitionFailed(token, VoiceConversationLoop.RecognitionFailure.SILENCE, timestamp));
                });
            }
            @Override public void onError(String reason) { handler.post(() -> earFallback(token, capture, reason)); }
        });
    }

    /** Los oídos no pudieron: el resto de la sesión se usa el dictado de Android. */
    private void earFallback(long token, salve.devices.EarCapture capture, String reason) {
        if (dismissed || ear != capture) return;
        earFailed = true;
        long timestamp = now();
        recognitionErrorText = "No pude usar mis oídos (" + (reason == null ? "error" : reason) + "); sigo con el dictado del móvil.";
        if (!loop.owns(token) || loop.state() != VoiceConversationLoop.State.LISTENING) { destroyRecognizer(); return; }
        if (!metrics.recognitionFailed(token, timestamp, SpeechRecognizer.ERROR_CLIENT)) { destroyRecognizer(); return; }
        destroyRecognizer();
        apply(loop.recognitionFailed(token, VoiceConversationLoop.RecognitionFailure.TRANSIENT, timestamp));
    }

    private void destroyRecognizer() {
        salve.devices.EarCapture oldEar = ear;
        ear = null;
        if (oldEar != null) oldEar.cancel();
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
            // En Android Auto el foco va con el mismo uso que la voz, para que suene por el coche.
            boolean driving = salve.core.driving.DrivingModeManager.get(activity).isActive();
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(salve.core.voice.SpeechAudioPolicy.focusUsage(driving)).build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(audioFocusListener, handler).build();
            focusHeld = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            return focusHeld;
        } catch (RuntimeException unavailable) { return false; }
    }

    /**
     * Mientras dura el modo voz, el botón de los auriculares hace lo mismo que «Interrumpir»: si Salve
     * habla, la corta y te escucha; si ya terminó, vuelve a abrir la escucha. Android envía el botón a
     * la última sesión multimedia activa, así que puede depender de los auriculares.
     */
    private void listenToHeadsetButton() {
        try {
            headsetButton = new android.media.session.MediaSession(activity, "Salve-voz");
            headsetButton.setCallback(new android.media.session.MediaSession.Callback() {
                @Override public boolean onMediaButtonEvent(Intent event) {
                    android.view.KeyEvent key = event == null ? null : event.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (key == null) return super.onMediaButtonEvent(event);
                    switch (key.getKeyCode()) {
                        case android.view.KeyEvent.KEYCODE_HEADSETHOOK:
                        case android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case android.view.KeyEvent.KEYCODE_MEDIA_PLAY:
                        case android.view.KeyEvent.KEYCODE_MEDIA_PAUSE:
                            if (key.getAction() == android.view.KeyEvent.ACTION_UP)
                                handler.post(() -> { if (!dismissed) interrupt.performClick(); });
                            return true;
                        default: return super.onMediaButtonEvent(event);
                    }
                }
            });
            headsetButton.setPlaybackState(new android.media.session.PlaybackState.Builder()
                    .setActions(android.media.session.PlaybackState.ACTION_PLAY_PAUSE)
                    .setState(android.media.session.PlaybackState.STATE_PLAYING, 0, 1f).build());
            headsetButton.setActive(true);
        } catch (RuntimeException unavailable) {
            Log.w("Salve/Voz", "Sin control por el botón de los auriculares");
            headsetButton = null;
        }
    }

    private void releaseResources() {
        bargeIn.stop();
        handler.removeCallbacks(watchdog);
        if (headsetButton != null) {
            try { headsetButton.setActive(false); headsetButton.release(); } catch (RuntimeException ignored) { }
            headsetButton = null;
        }
        controls.setKeepScreenOn(false);
        salve.services.VoiceForegroundService.stop(activity);
        notified = null;
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
        interrupt.setText(loop.isActive() ? "Interrumpir" : "Reintentar");
        interrupt.setContentDescription(loop.isActive() ? "Interrumpir a Salve y hablar" : "Reintentar la conversación de voz");
        interrupt.setEnabled(loop.state() != VoiceConversationLoop.State.LISTENING);
        switch (loop.state()) {
            case LISTENING:
                stage.setVoicePresence(SalveStageController.PresenceMode.LISTENING);
                switch (metrics.captureStage()) {
                    case STARTING: status.setText("Preparando el micrófono…"); break;
                    case FINALIZING:
                    case FINISHED: status.setText("Terminando la transcripción…"); break;
                    default: status.setText("Te escucho… Di «adiós, Salve» para terminar."); break;
                }
                break;
            case THINKING:
                stage.setVoicePresence(SalveStageController.PresenceMode.THINKING);
                status.setText("Preparando respuesta…"); break;
            case SPEAKING:
                stage.setVoicePresence(SalveStageController.PresenceMode.SPEAKING);
                status.setText(VoiceBargeInMonitor.isSupported()
                        ? "Salve está hablando… Puedes interrumpirla hablando."
                        : "Salve está hablando… Usa Interrumpir para tomar tu turno."); break;
            case COOLDOWN:
                stage.setVoicePresence(loop.hasHeldSpeech()
                        ? SalveStageController.PresenceMode.LISTENING : null);
                status.setText(loop.hasHeldSpeech() ? "Te sigo escuchando…"
                        : recognitionErrorText == null ? "Enseguida te escucho…"
                        : recognitionErrorText + " Volveré a intentarlo…");
                break;
            case STOPPED:
                stage.setVoicePresence(null);
                boolean recognitionFailure = loop.stopReason() == VoiceConversationLoop.StopReason.RECOGNIZER_ERROR
                        || loop.stopReason() == VoiceConversationLoop.StopReason.RECOGNIZER_UNAVAILABLE;
                status.setText(recognitionFailure && recognitionErrorText != null
                        ? recognitionErrorText + " He cerrado el micrófono." : stopMessage(loop.stopReason()));
                break;
            default: break;
        }
        String state = status.getText().toString();
        if (loop.isActive() && !state.equals(notified)) {
            notified = state;
            salve.services.VoiceForegroundService.update(activity, state);
        }
        stage.setVoiceDetails(status.getText() + "\n\n" + backend
                + "\n\nLos fragmentos de una frase incompleta se mantienen temporalmente en memoria"
                + " y se reúnen antes de enviarlos al motor."
                + " Durante el TTS puede funcionar la detección experimental de interrupción:"
                + " únicamente si Android permite cancelación de eco."
                + " Esta detección no transcribe ni guarda audio; puede fallar o confundir eco con habla."
                + " El reconocimiento normal vuelve tras cortar la voz de Salve."
                + " Puedes usar el botón Interrumpir en cualquier caso."
                + " La sesión termina al salir de la aplicación o por su límite de seguridad.");
        status.setContentDescription(status.getText() + ". Toca para ver los detalles de voz.");
    }

    private static String stopMessage(VoiceConversationLoop.StopReason reason) {
        switch (reason) {
            case SESSION_LIMIT: return "Sesión terminada: han pasado 10 minutos. Puedes abrir otra cuando quieras.";
            case NO_SPEECH: return "Llevo un rato sin oírte, así que he cerrado el micrófono. Vuelve a abrir el modo voz cuando quieras.";
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
        voiceLab.event(token, "llm", "reply_chars=" + (text == null ? 0 : text.length()) + " audio=" + audioQueued);
        diagnostics.event(token, audioQueued
                ? VoiceDiagnosticLog.Event.REPLY_READY : VoiceDiagnosticLog.Event.REPLY_WITHOUT_AUDIO,
                text == null ? 0 : text.length());
        // The engine already publishes the final reply to the shared chat listener.
        // Keep the on-stage subtitle current without adding a duplicate chat message.
        if (text != null && !text.isEmpty()) caption.setText(text);
        apply(loop.replied(token, audioQueued, timestamp));
    }

    @Override public void onSpeechStarted(long token) {
        long timestamp = now();
        if (!dismissed && loop.owns(token) && metrics.speechStarted(token, timestamp)) {
            voiceLab.event(token, "tts", "started");
            apply(loop.speechStarted(token, timestamp));
            if (!dismissed && loop.owns(token)
                    && loop.state() == VoiceConversationLoop.State.SPEAKING) {
                bargeIn.start(token, new VoiceBargeInMonitor.Listener() {
                    @Override public void onSpeechDetected(long activeToken) { onBargeInDetected(activeToken); }
                    @Override public void onStatus(long activeToken, VoiceDiagnosticLog.Event event, long code) {
                        diagnostics.event(activeToken, event, code);
                    }
                });
            }
        }
    }

    /** Runs on the main thread. Uses the SAME revocable token and interrupt path as the button. */
    private void onBargeInDetected(long token) {
        if (dismissed || !loop.owns(token)
                || loop.state() != VoiceConversationLoop.State.SPEAKING) return;
        bargeIn.stop();
        Log.i("Salve/Voice", "voice_barge_in detected=true");
        voiceLab.event(token, "interrupt", "automatic_detector");
        diagnostics.event(token, VoiceDiagnosticLog.Event.INTERRUPT_AUTOMATIC, 0);
        automaticInterruptDispatch = true;
        try { interrupt.performClick(); } finally { automaticInterruptDispatch = false; }
    }

    @Override public void onSpeechFinished(long token, boolean completed) {
        if (dismissed || !loop.owns(token)) return;
        bargeIn.stop();
        long timestamp = now();
        if (!metrics.speechFinished(token, timestamp)) return;
        voiceLab.event(token, "tts", "finished completed=" + completed);
        diagnostics.event(token, completed ? VoiceDiagnosticLog.Event.TTS_FINISHED_OK
                : VoiceDiagnosticLog.Event.TTS_FINISHED_FAILED, 0);
        VoiceConversationLoop.Step step = loop.speechFinished(token, completed, timestamp);
        if (step.action == VoiceConversationLoop.Action.WAIT && completed) {
            metrics.complete(token, timestamp);
            reportMetrics();
        }
        apply(step);
    }

    @Override public void onDiagnostic(long token, VoiceDiagnosticLog.Event event, long code) {
        if (!dismissed && loop.owns(token)) diagnostics.event(token, event, code);
    }

    private void reportMetrics() {
        String line = metrics.takeLogLine();
        if (line != null) {
            Log.i("Salve/Voice", line);
            voiceLab.event(metrics.token(), "metrics", line);
            diagnostics.metrics(line);
        }
    }

    private static long now() { return SystemClock.elapsedRealtime(); }
}
