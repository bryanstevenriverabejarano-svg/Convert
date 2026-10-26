package salve.avatar;

/**
 * Giros de Salve con sus vistas ilustradas (hoja views: 0 frente, 1 tres cuartos derecha, 2 perfil
 * derecha, 3 tres cuartos espalda, 4 espalda, 5 tres cuartos espalda, 6 perfil izquierda, 7 tres cuartos
 * izquierda). Sin Android: decide qué vista toca o -1 si ninguna.
 *  - Al cambiar de dirección mira hacia donde va: tres cuartos → perfil → tres cuartos (≈ 0,5 s).
 *  - En reposo, de vez en cuando mira a un lado un momento (cada 20–35 s, ≈ 1,6 s).
 *  - El gesto SPIN da una vuelta completa por las ocho vistas.
 */
public final class AvatarTurn {
    private static final int[] TO_RIGHT = {1, 2, 2, 1}, TO_LEFT = {7, 6, 6, 7};
    private static final int[] LOOK_RIGHT = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, LOOK_LEFT = {7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
    private static final float STEP = .16f;
    private Boolean lastRight;
    private int[] sequence;
    private float start, nextLook = -1;
    private int looks;

    /**
     * @param idle de pie sin gesto, sin hablar ni escuchar (puede mirar a un lado)
     * @param spin progreso del gesto SPIN 0..1, o negativo si no gira
     */
    public int view(boolean facingRight, boolean walking, boolean idle, float spin, float time) {
        if (!Float.isFinite(time)) return -1;
        if (spin >= 0) { sequence = null; return Math.min(7, (int) (spin * 8.999f) % 9 % 8); }
        if (lastRight == null) lastRight = facingRight;
        if (nextLook < 0 || time < start) { nextLook = time + 22f; start = time; }
        if (facingRight != lastRight) {
            lastRight = facingRight;
            sequence = facingRight ? TO_RIGHT : TO_LEFT;
            start = time;
            nextLook = time + 25f;
        } else if (sequence == null && idle && !walking && time >= nextLook) {
            sequence = (looks++ % 2 == 0) ? LOOK_RIGHT : LOOK_LEFT;
            start = time;
            nextLook = time + 20f + (looks * 7.3f) % 15f;
        }
        if (sequence == null) return -1;
        int i = (int) ((time - start) / STEP);
        if (i >= sequence.length || (!idle && sequence.length > 4)) { sequence = null; return -1; }
        return sequence[i];
    }
}
