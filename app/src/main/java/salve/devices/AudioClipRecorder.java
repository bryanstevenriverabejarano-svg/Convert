package salve.devices;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.ByteArrayOutputStream;
import java.util.function.BooleanSupplier;

/**
 * Graba unos segundos del micrófono (16 kHz, mono, 16 bits: lo que esperan los oídos de Gemma 4) y
 * los devuelve como WAV en memoria. No guarda nada en disco.
 */
public final class AudioClipRecorder {
    public static final int SAMPLE_RATE = 16_000;

    private AudioClipRecorder() { }

    /** @throws SecurityException sin permiso de micrófono; IllegalStateException si el micro está ocupado. */
    public static byte[] record(int seconds, BooleanSupplier cancelled) {
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) throw new IllegalStateException("Este móvil no deja grabar a 16 kHz");
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, SAMPLE_RATE));
        try {
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("El micrófono está ocupado");
            short[] pcm = new short[SAMPLE_RATE * seconds];
            short[] chunk = new short[SAMPLE_RATE / 10];
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                throw new IllegalStateException("El micrófono está ocupado");
            int filled = 0;
            while (filled < pcm.length) {
                if (cancelled.getAsBoolean()) break;
                int read = recorder.read(chunk, 0, Math.min(chunk.length, pcm.length - filled));
                if (read <= 0) break;
                System.arraycopy(chunk, 0, pcm, filled, read);
                filled += read;
            }
            if (filled < SAMPLE_RATE / 2) throw new IllegalStateException("No llegó sonido del micrófono");
            return wav(pcm, filled, SAMPLE_RATE);
        } finally {
            try { recorder.stop(); } catch (IllegalStateException notStarted) { }
            recorder.release();
        }
    }

    /** WAV PCM 16 bits mono. Puro: probado en la JVM. */
    public static byte[] wav(short[] pcm, int samples, int rate) {
        int data = samples * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream(44 + data);
        writeAscii(out, "RIFF"); writeInt(out, 36 + data); writeAscii(out, "WAVE");
        writeAscii(out, "fmt "); writeInt(out, 16); writeShort(out, 1); writeShort(out, 1);
        writeInt(out, rate); writeInt(out, rate * 2); writeShort(out, 2); writeShort(out, 16);
        writeAscii(out, "data"); writeInt(out, data);
        for (int i = 0; i < samples; i++) writeShort(out, pcm[i]);
        return out.toByteArray();
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) { for (char c : s.toCharArray()) out.write(c); }
    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v); out.write(v >>> 8); out.write(v >>> 16); out.write(v >>> 24);
    }
    private static void writeShort(ByteArrayOutputStream out, int v) { out.write(v); out.write(v >>> 8); }
}
