package salve.avatar;

/**
 * Giros de Salve con sus vistas ilustradas. Índices de vista:
 *   0–7  hoja views: 0 frente, 1 tres cuartos (lado 02), 2 perfil (lado 03), 3 tres cuartos espalda,
 *        4 espalda, 5 tres cuartos espalda, 6 perfil (lado 07), 7 tres cuartos (lado 08);
 *   8–9: G1 y G2 originales, giros reales hacia el lado de la vista 02/03;
 *   10–11: G3 y G4 originales, giros reales hacia el lado de la vista 08/07.
 * Ordenados por ángulo de frente al perfil:
 *   lado 02: 0 → G1(8) → 1 → G2(9) → 2
 *   lado 08: 0 → G3(10) → 7 → G4(11) → 6
 * Sin Android: decide qué vista toca o -1 si ninguna.
 *  - Al cambiar de dirección mira hacia donde va y vuelve al frente (≈ 0,8 s).
 *  - En reposo, de vez en cuando mira a un lado un momento (cada 20–35 s, ≈ 1,5 s).
 *  - El gesto SPIN recorre las ocho vistas base y los cuatro giros originales.
 */
public final class AvatarTurn {
    static final int[] RIGHT_PATH = {0, 8, 1, 9, 2}, LEFT_PATH = {0, 10, 7, 11, 6};
    private static final int[] SPIN = {0, 8, 1, 9, 2, 3, 4, 5, 6, 11, 7, 10};
    private static final int[] TO_RIGHT = there(RIGHT_PATH, 4, 2), TO_LEFT = there(LEFT_PATH, 4, 2);
    private static final int[] LOOK_RIGHT = there(RIGHT_PATH, 2, 12), LOOK_LEFT = there(LEFT_PATH, 2, 12);
    private static final float TURN_STEP = .10f, LOOK_STEP = .08f;
    private Boolean lastRight;
    private int[] sequence;
    private float start, step, nextLook = -1;
    private int looks;

    /** Ida por el camino hasta la posición {@code reach}, se queda {@code hold} pasos y vuelve al frente. */
    private static int[] there(int[] path, int reach, int hold) {
        int[] out = new int[(reach - 1) * 2 + hold];
        int k = 0;
        for (int i = 1; i < reach; i++) out[k++] = path[i];
        for (int i = 0; i < hold; i++) out[k++] = path[reach];
        for (int i = reach - 1; i >= 1; i--) out[k++] = path[i];
        return out;
    }

    /**
     * @param idle de pie sin gesto, sin hablar ni escuchar (puede mirar a un lado)
     * @param spin progreso del gesto SPIN 0..1, o negativo si no gira
     */
    public int view(boolean facingRight, boolean walking, boolean idle, float spin, float time) {
        if (!Float.isFinite(time)) return -1;
        if (spin >= 0) { sequence = null; return spin >= 1 ? 0 : SPIN[Math.min(SPIN.length - 1, (int) (spin * SPIN.length))]; }
        if (lastRight == null) lastRight = facingRight;
        if (nextLook < 0 || time < start) { nextLook = time + 22f; start = time; }
        if (facingRight != lastRight) {
            lastRight = facingRight;
            sequence = facingRight ? TO_RIGHT : TO_LEFT;
            step = TURN_STEP;
            start = time;
            nextLook = time + 25f;
        } else if (sequence == null && idle && !walking && time >= nextLook) {
            sequence = (looks++ % 2 == 0) ? LOOK_RIGHT : LOOK_LEFT;
            step = LOOK_STEP;
            start = time;
            nextLook = time + 20f + (looks * 7.3f) % 15f;
        }
        if (sequence == null) return -1;
        int i = (int) ((time - start) / step);
        if (i >= sequence.length || (!idle && step == LOOK_STEP)) { sequence = null; return -1; }
        return sequence[i];
    }

    /** Vista más parecida de la hoja views (0–7) para cuerpos sin hoja turn. */
    public static int fallback(int view) {
        switch (view) {
            case 8: case 10: return 0;
            case 9: return 1;
            case 11: return 7;
            default: return view;
        }
    }
}
