package salve.core.voice;

/**
 * Conservative, bounded activity gate for an echo-reduced PCM stream.
 * It is NOT speech recognition and MUST NOT be presented as semantic VAD.
 * No audio is retained: this object stores only levels and counters.
 */
public final class VoiceBargeInDetector {
    static final int WARMUP_FRAMES = 18;          // approximately 360 ms at 16 kHz / 320 samples
    static final int REQUIRED_VOICED_FRAMES = 17; // approximately 340 ms
    private static final double MIN_RMS = 0.026;
    private static final double FLOOR_MULTIPLIER = 2.8;

    private int frames;
    private int voiced;
    private double floor = 0.008;
    private boolean fired;

    /** @return true only once after enough sustained, speech-like activity. */
    public boolean accept(short[] samples, int size) {
        if (fired || samples == null || size < 120 || size > samples.length) return false;
        double energy = 0;
        int crossings = 0;
        int last = samples[0];
        for (int i = 0; i < size; i++) {
            int current = samples[i];
            double level = current / 32768.0;
            energy += level * level;
            if (i > 0 && ((current < 0 && last >= 0) || (current >= 0 && last < 0))) crossings++;
            last = current;
        }
        double rms = Math.sqrt(energy / size);
        double zcr = crossings / (double) size;
        frames++;
        if (frames <= WARMUP_FRAMES) {
            // Short calibration during TTS/AEC startup; conservative if speaker echo persists.
            floor = 0.86 * floor + 0.14 * rms;
            return false;
        }
        boolean speechLike = zcr >= 0.012 && zcr <= 0.30;
        boolean loudEnough = rms >= Math.max(MIN_RMS, floor * FLOOR_MULTIPLIER);
        if (speechLike && loudEnough) {
            voiced++;
        } else {
            voiced = Math.max(0, voiced - 2);
            if (rms < Math.max(MIN_RMS, floor * FLOOR_MULTIPLIER)) {
                floor = floor * 0.98 + rms * 0.02;
            }
        }
        if (voiced >= REQUIRED_VOICED_FRAMES) {
            fired = true;
            return true;
        }
        return false;
    }
}
