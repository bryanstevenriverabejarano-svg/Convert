package salve.avatar;

/**
 * Convierte el giro del teléfono en el punto de vista del relieve 3D de Salve.
 *
 * Integra la velocidad angular del giroscopio y la deja volver sola al frente (integrador con fuga):
 * inclinar el teléfono la gira un poco y, al dejarlo quieto en cualquier postura, vuelve a mirar de frente.
 * Sin Android: se puede probar en la JVM.
 */
public final class AstralParallax {
    /** Grados de relieve por grado de giro del teléfono. */
    static final float GAIN = .9f;
    /** Segundos que tarda en volver al frente (constante de tiempo). */
    static final float RETURN_SECONDS = 2.5f;
    private float yaw, pitch;

    /**
     * @param aroundX velocidad angular alrededor del eje X del teléfono (rad/s): cabecear
     * @param aroundY velocidad angular alrededor del eje Y del teléfono (rad/s): girarlo a los lados
     * @param dt segundos desde la muestra anterior
     */
    public void rotate(float aroundX, float aroundY, float dt) {
        if (!Float.isFinite(aroundX) || !Float.isFinite(aroundY) || !Float.isFinite(dt) || dt <= 0) return;
        dt = Math.min(dt, .1f);
        yaw += (float) Math.toDegrees(aroundY) * dt * GAIN;
        pitch += (float) Math.toDegrees(aroundX) * dt * GAIN;
        float keep = (float) Math.exp(-dt / RETURN_SECONDS);
        yaw = clamp(yaw * keep, AstralBody.MAX_VIEW_YAW);
        pitch = clamp(pitch * keep, AstralBody.MAX_VIEW_PITCH);
    }

    public void reset() { yaw = pitch = 0; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }

    private static float clamp(float value, float limit) { return Math.max(-limit, Math.min(limit, value)); }
}
