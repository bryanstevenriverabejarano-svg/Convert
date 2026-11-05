package salve.core.voice;

/**
 * Detecta cuándo empieza y cuándo termina de hablar Bryan a partir del volumen de cada trocito de
 * audio (20 ms). Primero mide el ruido de fondo (coche, calle) y exige hablar claramente por encima.
 * Lógica pura para pruebas JVM; el «modo oídos» la usa en lugar del reconocimiento de Android.
 */
public final class SpeechEndpointer {
    public enum Event { NONE, SPEECH_STARTED, SPEECH_ENDED, NO_SPEECH, TOO_LONG }

    static final int CALIBRATION_MS = 300, START_MS = 160, END_SILENCE_MS = 900;
    static final int NO_SPEECH_MS = 8000, MAX_SPEECH_MS = 15000;
    /** Por debajo de esto ni el ruido más bajo cuenta como voz (RMS de PCM 16 bits). */
    static final double MIN_SPEECH_RMS = 500;

    private double noise = 0;
    private int calibrated = 0, elapsed = 0, loud = 0, quiet = 0, speech = 0;
    private boolean speaking, finished;

    /** Un trozo de audio: su volumen (RMS) y cuánto dura. Devuelve lo que acaba de pasar. */
    public Event feed(double rms, int frameMs) {
        if (finished) return Event.NONE;
        elapsed += frameMs;
        if (calibrated < CALIBRATION_MS) {
            noise = calibrated == 0 ? rms : (noise * calibrated + rms * frameMs) / (calibrated + frameMs);
            calibrated += frameMs;
            return Event.NONE;
        }
        double threshold = Math.max(MIN_SPEECH_RMS, noise * 3.0);
        boolean voice = rms >= threshold;
        if (!speaking) {
            if (!voice) noise = noise * 0.97 + rms * 0.03; // el ruido de fondo cambia (túnel, ventanilla)
            loud = voice ? loud + frameMs : 0;
            if (loud >= START_MS) { speaking = true; speech = loud; quiet = 0; return Event.SPEECH_STARTED; }
            if (elapsed >= NO_SPEECH_MS) { finished = true; return Event.NO_SPEECH; }
            return Event.NONE;
        }
        speech += frameMs;
        quiet = voice ? 0 : quiet + frameMs;
        if (quiet >= END_SILENCE_MS) { finished = true; return Event.SPEECH_ENDED; }
        if (speech >= MAX_SPEECH_MS) { finished = true; return Event.TOO_LONG; }
        return Event.NONE;
    }

    public boolean speaking() { return speaking; }

    /** RMS de un trozo de PCM 16 bits. */
    public static double rms(short[] pcm, int length) {
        if (length <= 0) return 0;
        double sum = 0;
        for (int i = 0; i < length; i++) sum += (double) pcm[i] * pcm[i];
        return Math.sqrt(sum / length);
    }
}
