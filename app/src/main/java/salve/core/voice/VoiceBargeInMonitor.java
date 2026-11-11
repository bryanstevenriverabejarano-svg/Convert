package salve.core.voice;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Experimental, foreground-only barge-in detector. Uses one short-lived AudioRecord
 * while Android TTS is speaking; hands control back to the EXISTING VoiceConversationLoop
 * and SpeechRecognizer. Does not record, transcribe, store or transmit audio.
 *
 * Acoustic echo cancellation is mandatory. An enabled AEC is not a guarantee that a
 * given speaker/volume/route is safe: false triggers remain possible until phone testing.
 */
public final class VoiceBargeInMonitor {
    public interface Listener {
        void onSpeechDetected(long voiceTurnToken);
        default void onStatus(long token, VoiceDiagnosticLog.Event event, long code) { }
    }

    private static final String TAG = "Salve/BargeIn";
    private static final int SAMPLE_RATE = 16_000;
    private static final int FRAME = 320;
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private volatile AudioRecord activeRecord;
    private volatile boolean running;

    public static boolean isSupported() {
        return AcousticEchoCanceler.isAvailable();
    }

    /** Call on the UI thread when actual voice output begins. */
    public synchronized boolean start(long token, Listener listener) {
        if (token <= 0 || listener == null) return false;
        if (!isSupported()) {
            listener.onStatus(token, VoiceDiagnosticLog.Event.MIC_AEC_UNAVAILABLE, 0);
            return false;
        }
        stop();
        final int owner = ++generation;
        running = true;
        Thread capture = new Thread(() -> captureLoop(owner, token, listener), "salve-barge-in");
        capture.setDaemon(true);
        capture.start();
        return true;
    }

    /** Idempotent. Stops capture before allowing the regular Android ASR to open. */
    public synchronized void stop() {
        running = false;
        generation++;
        AudioRecord rec = activeRecord;
        if (rec != null) {
            try { rec.stop(); } catch (RuntimeException ignored) { }
        }
    }

    private void captureLoop(int owner, long voiceToken, Listener listener) {
        AudioRecord rec = null;
        AcousticEchoCanceler echo = null;
        NoiseSuppressor noise = null;
        try {
            int minimum = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minimum <= 0) {
                listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_BUFFER_INVALID, minimum);
                return;
            }
            rec = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum * 2, 4096));
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_INIT_FAILED, rec.getState());
                return;
            }
            echo = AcousticEchoCanceler.create(rec.getAudioSessionId());
            if (echo == null) {
                listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_AEC_CREATE_FAILED, 0);
                return;
            }
            echo.setEnabled(true);
            if (!echo.getEnabled()) {
                listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_AEC_ENABLE_FAILED, 0);
                return;
            }
            if (NoiseSuppressor.isAvailable()) {
                noise = NoiseSuppressor.create(rec.getAudioSessionId());
                if (noise != null) noise.setEnabled(true);
            }
            if (!owns(owner)) return;
            activeRecord = rec;
            rec.startRecording();
            if (rec.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_CAPTURE_FAILED, rec.getRecordingState());
                return;
            }
            listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_CAPTURE_ACTIVE,
                    1L | (noise != null && noise.getEnabled() ? 2L : 0L));
            VoiceBargeInDetector detector = new VoiceBargeInDetector();
            short[] pcm = new short[FRAME];
            while (owns(owner)) {
                int count = rec.read(pcm, 0, pcm.length, AudioRecord.READ_NON_BLOCKING);
                if (count < 0) {
                    listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_READ_FAILED, count);
                    break;
                }
                if (count == 0) {
                    try { Thread.sleep(10); } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt(); break;
                    }
                    continue;
                }
                if (detector.accept(pcm, count)) {
                    listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_DETECTOR_TRIGGERED, 0);
                    // Never dispatch raw audio or commands. The current voice session owns
                    // the actual cancellation and microphone transfer.
                    main.post(() -> {
                        if (owns(owner)) listener.onSpeechDetected(voiceToken);
                    });
                    break;
                }
            }
        } catch (RuntimeException error) {
            // No raw audio or exception messages are persisted; only a numeric event code.
            listener.onStatus(voiceToken, VoiceDiagnosticLog.Event.MIC_CAPTURE_EXCEPTION, 0);
            // No fallback to raw microphone without AEC; the Interrupt button still works.
            Log.w(TAG, "Barge-in audio capture unavailable; keeping manual interruption.", error);
        } finally {
            if (rec != null) {
                try { rec.stop(); } catch (RuntimeException ignored) { }
                try { rec.release(); } catch (RuntimeException ignored) { }
            }
            if (echo != null) {
                try { echo.release(); } catch (RuntimeException ignored) { }
            }
            if (noise != null) {
                try { noise.release(); } catch (RuntimeException ignored) { }
            }
            synchronized (this) {
                if (activeRecord == rec) activeRecord = null;
                if (generation == owner) running = false;
            }
        }
    }

    private boolean owns(int owner) {
        return running && generation == owner;
    }
}
