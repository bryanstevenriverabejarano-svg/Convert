package salve.avatar;

import java.util.Random;

/**
 * El cuerpo astral de Salve: la figura digital que se ve en pantalla, tratada como su propio cuerpo.
 *
 * Es estado físico simulado y determinista (sin Android ni modelo): parpadeo natural con intervalos
 * irregulares, pelo con inercia de muelle amortiguado, respiración, balanceo de apoyo y la energía
 * de los circuitos de luz. Las vistas lo avanzan con el reloj; {@link AstralBodySense} lo convierte
 * en propiocepción (texto) para que Salve sepa qué está haciendo su cuerpo en cada momento.
 *
 * Las acciones voluntarias (parpadear, mover el pelo, encender los circuitos) entran por
 * {@link #requestBlink()}, {@link #flickHair(float)} y {@link #pulse()}.
 */
public final class AstralBody {
    /** Instantánea inmutable: se puede leer desde cualquier hilo. */
    public static final class Snapshot {
        /** 0 abierto, 1 cerrado. */
        public final float blink;
        /** Desplazamiento horizontal de las puntas del pelo, en píxeles de la ilustración base. */
        public final float hairSwing;
        /** Ondulación lenta propia de los mechones (radianes). */
        public final float hairWave;
        /** Intensidad de los circuitos 0..1 y anillo de energía 0..1 (negativo: sin anillo). */
        public final float glow, glowRing;
        /** Grados de inclinación de cabeza y de balanceo del cuerpo; desplazamientos de cabeza en px. */
        public final float headTilt, headShiftX, headShiftY, sway;
        public final boolean drawing, speaking, thinking, listening, moving;
        public final String drawingSubject;
        public final long blinks;
        public final float hairEnergy;
        /** Postura y posición (0 izquierda .. 1 derecha) vistas en el último fotograma; null si no hay vista. */
        public final AvatarState.Pose pose;
        public final float stageX;
        /** Milisegundos (reloj de la vista) del último fotograma y del último parpadeo. */
        public final long seenAtMillis, lastBlinkMillis;
        Snapshot(AstralBody b) {
            pose = b.pose; stageX = b.stageX; seenAtMillis = b.lastMillis; lastBlinkMillis = b.lastBlinkMillis;
            blink = b.blink; hairSwing = b.hairAngle; hairWave = b.wavePhase; glow = b.glow; glowRing = b.ring;
            headTilt = b.headTilt; headShiftX = b.headX; headShiftY = b.headY; sway = b.sway;
            drawing = b.drawingSubject != null; drawingSubject = b.drawingSubject;
            speaking = b.speaking; thinking = b.thinking; listening = b.listening; moving = b.moving;
            blinks = b.blinkCount; hairEnergy = Math.min(1f, Math.abs(b.hairAngle) / 24f + Math.abs(b.hairVelocity) / 120f);
        }
    }

    private static final AstralBody INSTANCE = new AstralBody(new Random());
    public static AstralBody get() { return INSTANCE; }

    private final Random random;
    private long lastMillis = -1;
    private float time;
    private float blink, blinkClock, nextBlink, blinkAge = -1;
    private boolean doubleBlink, voluntaryBlink;
    private long blinkCount;
    private float hairAngle, hairVelocity, wavePhase, pendingFlick;
    private float glow = .25f, ring = -1, ringAge, pulseBoost;
    private float headTilt, headX, headY, sway, lastBodyX = Float.NaN, bodyVelocity;
    private boolean speaking, thinking, listening, moving;
    private String drawingSubject;
    private float drawingAge;
    private long drawingUntilNanos;
    private AvatarState.Pose pose;
    private float stageX = .5f;
    private long lastBlinkMillis = -1;
    private volatile Snapshot snapshot;

    AstralBody(Random random) {
        this.random = random;
        nextBlink = nextInterval();
        snapshot = new Snapshot(this);
    }

    public Snapshot snapshot() { return snapshot; }

    /** Parpadeo voluntario (se ejecuta en el siguiente fotograma). */
    public synchronized void requestBlink() { voluntaryBlink = true; }

    /** Agita el pelo hacia un lado (-1 izquierda, 1 derecha, 0 ambos lados alternando). */
    public synchronized void flickHair(float direction) {
        float d = Float.isFinite(direction) ? Math.max(-1f, Math.min(1f, direction)) : 0f;
        pendingFlick += (d == 0 ? (random.nextBoolean() ? 1 : -1) : d) * 150f;
    }

    /** Enciende los circuitos un instante. */
    public synchronized void pulse() { pulseBoost = 1f; ring = 0; ringAge = 0; }

    /** El cuerpo está trazando un dibujo (null al terminar). */
    public synchronized void setDrawing(String subject) { setDrawing(subject, 30f); }

    /** Igual, con caducidad en segundos de reloj real (aunque ninguna vista esté en pantalla). */
    public synchronized void setDrawing(String subject, float seconds) {
        drawingSubject = subject == null || subject.trim().isEmpty() ? null : subject.trim();
        drawingAge = 0;
        drawingUntilNanos = drawingSubject == null ? 0
                : System.nanoTime() + (long) (Math.max(.5f, Math.min(120f, seconds)) * 1e9);
        if (drawingSubject != null) pulse();
        snapshot = new Snapshot(this);
    }

    /** Qué está dibujando ahora mismo (null si nada). Vale desde cualquier hilo. */
    public synchronized String drawingNow() {
        if (drawingSubject != null && System.nanoTime() > drawingUntilNanos) {
            drawingSubject = null;
            snapshot = new Snapshot(this);
        }
        return drawingSubject;
    }

    /**
     * Avanza la física. Varias vistas pueden llamar en el mismo fotograma: sólo cuenta el tiempo nuevo.
     * @param bodyX posición horizontal del cuerpo en el escenario (0..1)
     */
    public synchronized Snapshot advance(long nowMillis, AvatarMotion.Snapshot motion, float bodyX, boolean walking) {
        return advance(nowMillis, motion, bodyX, walking ? AvatarState.Pose.WALKING : AvatarState.Pose.IDLE);
    }

    public synchronized Snapshot advance(long nowMillis, AvatarMotion.Snapshot motion, float bodyX, AvatarState.Pose bodyPose) {
        boolean walking = bodyPose == AvatarState.Pose.WALKING;
        pose = bodyPose;
        if (Float.isFinite(bodyX)) stageX = Math.max(0, Math.min(1, bodyX));
        if (lastMillis < 0 || nowMillis < lastMillis) { lastMillis = nowMillis; return snapshot = new Snapshot(this); }
        float seconds = (nowMillis - lastMillis) / 1000f;
        if (seconds <= 0) return snapshot;
        lastMillis = nowMillis;
        step(Math.min(seconds, .25f), motion, bodyX, walking);
        return snapshot = new Snapshot(this);
    }

    /** Paso puro de simulación (visible para pruebas). */
    void step(float dt, AvatarMotion.Snapshot motion, float bodyX, boolean walking) {
        if (!Float.isFinite(dt) || dt <= 0) return;
        time = (time + dt) % 3600f;
        speaking = motion != null && motion.speaking;
        thinking = motion != null && motion.thinking;
        listening = motion != null && motion.listening;
        stepBlink(dt, motion);
        // Velocidad del cuerpo en "anchos de escenario" por segundo → inercia del pelo.
        float velocity = 0;
        if (Float.isFinite(bodyX)) {
            if (Float.isFinite(lastBodyX)) velocity = (bodyX - lastBodyX) / dt;
            lastBodyX = bodyX;
        }
        float acceleration = (velocity - bodyVelocity) / dt;
        bodyVelocity = velocity;
        moving = walking || Math.abs(velocity) > .02f;
        float wind = (float) (Math.sin(time * .63) * 3.2 + Math.sin(time * 1.37 + 1.3) * 1.6 + Math.sin(time * .21) * 2.4);
        float drive = -Math.max(-60f, Math.min(60f, acceleration)) * 420f + (wind - hairAngle * .15f) * 9f;
        if (walking) drive += (float) Math.sin(time * 9.0) * 520f;
        if (motion != null) drive -= (motion.headTilt - headTilt * 3f) * 6f;
        hairVelocity += pendingFlick; pendingFlick = 0;
        // Muelle amortiguado (frecuencia propia ~0,9 Hz): oscila y se asienta como pelo largo.
        float stiffness = 32f, damping = 3.4f;
        hairVelocity += (drive - stiffness * hairAngle - damping * hairVelocity) * dt;
        hairAngle = Math.max(-42f, Math.min(42f, hairAngle + hairVelocity * dt));
        if (Math.abs(hairAngle) >= 42f) hairVelocity *= -.3f;
        wavePhase = (wavePhase + dt * (1.1f + Math.abs(hairVelocity) * .01f)) % (float) (2 * Math.PI * 1000);

        float targetTilt = motion == null ? 0 : motion.headTilt * .32f;
        float targetX = motion == null ? 0 : motion.headYaw * .55f;
        float targetY = motion == null ? 0 : motion.headPitch * .9f - motion.breath * 1.2f;
        float blend = 1f - (float) Math.exp(-dt * 10f);
        headTilt += (targetTilt - headTilt) * blend;
        headX += (targetX - headX) * blend;
        headY += (targetY - headY) * blend;
        float bodyTilt = motion == null ? 0 : motion.bodyTilt;
        sway = (float) Math.sin(time * .47) * .32f + bodyTilt * .5f;

        // Energía de los circuitos: respiración tranquila, pensar = ondas, hablar = pulso con la voz.
        float target = .2f + .08f * (float) Math.sin(time * 1.1);
        if (listening) target = .34f;
        if (thinking) target = .4f;
        if (speaking) target = .3f + (motion == null ? 0 : motion.mouthOpen) * .55f;
        if (drawingSubject != null && System.nanoTime() > drawingUntilNanos) drawingSubject = null;
        if (drawingSubject != null) {
            drawingAge += dt;
            target = Math.max(target, .5f + .12f * (float) Math.sin(drawingAge * 6.0));
        }
        pulseBoost = Math.max(0, pulseBoost - dt * .9f);
        target = Math.min(1f, target + pulseBoost * .5f);
        glow += (target - glow) * (1f - (float) Math.exp(-dt * 6f));
        boolean waves = thinking || drawingSubject != null || pulseBoost > 0;
        if (waves) {
            ringAge += dt;
            ring = (ringAge % 1.8f) / 1.8f;
        } else { ring = -1; ringAge = 0; }
    }

    private void stepBlink(float dt, AvatarMotion.Snapshot motion) {
        blinkClock += dt;
        boolean startle = motion != null && motion.gesture == AvatarMotion.Gesture.STARTLE;
        if (blinkAge < 0 && !startle && (voluntaryBlink || blinkClock >= nextBlink)) {
            blinkAge = 0; blinkClock = 0; blinkCount++; lastBlinkMillis = lastMillis;
            nextBlink = nextInterval() * (thinking ? .7f : speaking ? 1.15f : 1f);
            doubleBlink = !voluntaryBlink && random.nextFloat() < .18f;
            voluntaryBlink = false;
        }
        if (blinkAge >= 0) {
            blinkAge += dt;
            // 70 ms cerrando, 50 ms cerrado, 110 ms abriendo.
            float close = .07f, hold = .05f, open = .11f;
            if (blinkAge < close) blink = blinkAge / close;
            else if (blinkAge < close + hold) blink = 1;
            else if (blinkAge < close + hold + open) blink = 1 - (blinkAge - close - hold) / open;
            else {
                blink = 0;
                if (doubleBlink) { doubleBlink = false; blinkAge = -1; voluntaryBlink = true; }
                else blinkAge = -1;
            }
        } else blink = 0;
        // La risa entorna los ojos; el parpadeo propio sigue por encima.
        if (motion != null && motion.gesture == AvatarMotion.Gesture.LAUGH) blink = Math.max(blink, motion.blink);
        if (motion != null && motion.gesture == AvatarMotion.Gesture.CRY) blink = Math.max(blink, .45f);
    }

    /** Intervalos humanos: 2,2–6,5 s, más frecuentes los cortos. */
    private float nextInterval() {
        float u = random.nextFloat();
        return 2.2f + u * u * 4.3f;
    }
}
