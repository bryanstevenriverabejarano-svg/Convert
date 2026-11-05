package salve.devices;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;

import salve.core.voice.SpeechEndpointer;

/**
 * Micrófono del «modo oídos»: escucha hasta que Bryan termina una frase (SpeechEndpointer) y entrega
 * esa frase como WAV en memoria (16 kHz mono, lo que oye Gemma 4). Corre en su propio hilo.
 */
public final class EarCapture {
    public interface Listener {
        void onSpeechStarted();
        void onPhrase(byte[] wav);
        void onNoSpeech();
        void onError(String reason);
    }

    private static final int RATE = AudioClipRecorder.SAMPLE_RATE, FRAME = RATE / 50; // 20 ms
    private volatile boolean cancelled;
    private Thread thread;

    public void start(Listener listener) {
        thread = new Thread(() -> run(listener), "salve-oidos-frase");
        thread.start();
    }

    public void cancel() {
        cancelled = true;
        if (thread != null) thread.interrupt();
    }

    private void run(Listener listener) {
        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord recorder;
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, RATE));
        } catch (SecurityException | IllegalArgumentException e) {
            if (!cancelled) listener.onError("sin acceso al micrófono");
            return;
        }
        AcousticEchoCanceler echo = null;
        NoiseSuppressor noise = null;
        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) { if (!cancelled) listener.onError("micrófono ocupado"); return; }
            int session = recorder.getAudioSessionId();
            try { if (AcousticEchoCanceler.isAvailable()) { echo = AcousticEchoCanceler.create(session); if (echo != null) echo.setEnabled(true); } }
            catch (RuntimeException ignored) { }
            try { if (NoiseSuppressor.isAvailable()) { noise = NoiseSuppressor.create(session); if (noise != null) noise.setEnabled(true); } }
            catch (RuntimeException ignored) { }
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) { if (!cancelled) listener.onError("micrófono ocupado"); return; }
            SpeechEndpointer endpointer = new SpeechEndpointer();
            // Se guarda un poco de antes de empezar a hablar para no cortar la primera sílaba.
            short[] all = new short[RATE * 17];
            int filled = 0, startAt = -1;
            short[] frame = new short[FRAME];
            while (!cancelled) {
                int read = recorder.read(frame, 0, FRAME);
                if (read <= 0) { if (!cancelled) listener.onError("el micrófono dejó de enviar audio"); return; }
                if (startAt < 0 && filled > RATE) { // sin hablar todavía: solo se guarda el último medio segundo
                    int keep = RATE / 2;
                    System.arraycopy(all, filled - keep, all, 0, keep);
                    filled = keep;
                }
                if (filled + read > all.length) read = all.length - filled; // nunca pasa: TOO_LONG corta antes
                System.arraycopy(frame, 0, all, filled, read);
                filled += read;
                SpeechEndpointer.Event event = endpointer.feed(SpeechEndpointer.rms(frame, read), read * 1000 / RATE);
                switch (event) {
                    case SPEECH_STARTED:
                        startAt = Math.max(0, filled - RATE * 4 / 10); // 0,4 s antes: la primera sílaba
                        listener.onSpeechStarted();
                        break;
                    case SPEECH_ENDED:
                    case TOO_LONG: {
                        int from = Math.max(0, startAt);
                        short[] phrase = new short[filled - from];
                        System.arraycopy(all, from, phrase, 0, phrase.length);
                        if (!cancelled) listener.onPhrase(AudioClipRecorder.wav(phrase, phrase.length, RATE));
                        return;
                    }
                    case NO_SPEECH:
                        if (!cancelled) listener.onNoSpeech();
                        return;
                    default:
                        break;
                }
            }
        } catch (RuntimeException failure) {
            if (!cancelled) listener.onError("fallo del micrófono");
        } finally {
            try { recorder.stop(); } catch (IllegalStateException ignored) { }
            recorder.release();
            if (echo != null) echo.release();
            if (noise != null) noise.release();
        }
    }
}
