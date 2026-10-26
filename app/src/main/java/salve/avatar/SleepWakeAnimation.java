package salve.avatar;

/**
 * Dormir, despertarse, bostezar, estirarse y guiñar con las ilustraciones de las hojas «sleep» y «wake».
 * Sin Android: dice qué fotograma toca. Hoja «sleep» (cabeza a la izquierda, en la cama): 0 dormida de lado,
 * 1 acurrucada, 2 hecha un ovillo, 3 abre los ojos. Hoja «wake» (de rodillas): 0 se frota el ojo bostezando,
 * 1 se estira, 2 se estira del todo, 3 bosteza tapándose, 4 se frota el ojo despierta, 5 mano en la mejilla,
 * 6 guiño y saludo.
 */
public final class SleepWakeAnimation {
    private SleepWakeAnimation() { }

    /** Cambia de postura en sueños cada {@link #SLEEP_HOLD} s, con un fundido de {@link #SLEEP_FADE} s. */
    static final int[] SLEEP_CYCLE = {0, 1, 2};
    static final float SLEEP_HOLD = 40f, SLEEP_FADE = 1.5f;
    /** Escala de dibujo de las ilustraciones de rodillas (la 26 usa 0,67; estas vienen más cerca). */
    static final float[] WAKE_SCALE = {.60f, .65f, .65f, .60f, .60f, .60f, .62f};
    private static final float[] BEND_SCALE = {.95f, .78f, .70f, .72f, .70f};

    public static final class Lying {
        public final int from, to;
        /** 0 = solo «from», 1 = solo «to». */
        public final float mix;
        Lying(int from, int to, float mix) { this.from = from; this.to = to; this.mix = mix; }
    }

    public static Lying lying(float sleptSeconds) {
        float t = Float.isFinite(sleptSeconds) && sleptSeconds > 0 ? sleptSeconds : 0;
        int step = (int) (t / SLEEP_HOLD);
        float into = t - step * SLEEP_HOLD;
        int from = SLEEP_CYCLE[step % SLEEP_CYCLE.length];
        if (step == 0 || into >= SLEEP_FADE) return new Lying(from, from, 0);
        int previous = SLEEP_CYCLE[(step - 1) % SLEEP_CYCLE.length];
        return new Lying(previous, from, into / SLEEP_FADE);
    }

    /** Al despertar: abre los ojos en la cama, se frota el ojo, se estira, bosteza, sonríe con un guiño y se levanta. */
    static final String[] WAKE_SHEETS = {"sleep", "wake", "wake", "wake", "wake", "wake", "wake", "wake", "bend", "bend", "bend"};
    static final int[] WAKE_FRAMES = {3, 0, 1, 2, 3, 4, 5, 6, 4, 3, 0};
    static final float[] WAKE_TIMES = {1.6f, 1f, .7f, 1f, 1f, .9f, .8f, 1.4f, .14f, .14f, .14f};

    public static float wakeLength() { float sum = 0; for (float t : WAKE_TIMES) sum += t; return sum; }

    /** Sigue en la cama (abriendo los ojos) durante el primer paso del despertar. */
    public static boolean inBed(float elapsed) {
        return Float.isFinite(elapsed) && elapsed >= 0 && elapsed < WAKE_TIMES[0];
    }

    /** Fotograma del despertar o null cuando ya ha terminado. */
    public static RealisticSpriteAnimation.Frame wake(float elapsed) {
        if (!Float.isFinite(elapsed) || elapsed < 0) return null;
        float at = 0;
        for (int i = 0; i < WAKE_TIMES.length; i++) {
            at += WAKE_TIMES[i];
            if (elapsed < at) return frame(WAKE_SHEETS[i], WAKE_FRAMES[i]);
        }
        return null;
    }

    /** Gestos con las ilustraciones de rodillas: núcleo (fotograma de «wake», peso de tiempo). */
    private static final int[][] YAWN = {{3, 12}, {0, 10}}, STRETCH = {{1, 7}, {2, 12}, {1, 6}}, WINK = {{6, 16}};
    /** De pie, se arrodilla antes (bend 0, 3, 4) y se levanta después (pasos cortos de peso 2). */
    private static final int[] KNEEL_DOWN = {0, 3, 4};
    private static final int BEND_WEIGHT = 2;

    public static boolean handles(AvatarMotion.Gesture gesture) {
        return gesture == AvatarMotion.Gesture.YAWN || gesture == AvatarMotion.Gesture.STRETCH
                || gesture == AvatarMotion.Gesture.WINK;
    }

    /** Fotograma del gesto según su avance (0..1); null si no es uno de estos gestos. */
    public static RealisticSpriteAnimation.Frame gesture(AvatarMotion.Gesture gesture, float progress, boolean kneeling) {
        int[][] core;
        if (gesture == AvatarMotion.Gesture.YAWN) core = YAWN;
        else if (gesture == AvatarMotion.Gesture.STRETCH) core = STRETCH;
        else if (gesture == AvatarMotion.Gesture.WINK) core = WINK;
        else return null;
        int total = 0;
        for (int[] step : core) total += step[1];
        if (!kneeling) total += 2 * KNEEL_DOWN.length * BEND_WEIGHT;
        float p = Float.isFinite(progress) ? Math.max(0, Math.min(.9999f, progress)) : 0;
        float target = p * total, at = 0;
        if (!kneeling) for (int bend : KNEEL_DOWN) {
            at += BEND_WEIGHT;
            if (target < at) return frame("bend", bend);
        }
        for (int[] step : core) {
            at += step[1];
            if (target < at) return frame("wake", step[0]);
        }
        for (int i = KNEEL_DOWN.length - 1; i >= 0; i--) {
            at += BEND_WEIGHT;
            if (target < at) return frame("bend", KNEEL_DOWN[i]);
        }
        return frame("bend", KNEEL_DOWN[0]);
    }

    static RealisticSpriteAnimation.Frame frame(String sheet, int index) {
        float scale = "wake".equals(sheet) ? WAKE_SCALE[index] : "bend".equals(sheet) ? BEND_SCALE[index] : 1;
        return new RealisticSpriteAnimation.Frame(sheet, index, false, scale);
    }
}
