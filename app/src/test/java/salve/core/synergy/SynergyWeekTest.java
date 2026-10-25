package salve.core.synergy;

import org.junit.Test;
import static org.junit.Assert.*;

/** Una semana de uso real: hablar mucho con Salve no puede apagar su aprendizaje nocturno. */
public class SynergyWeekTest {
    private static final long HOUR = 3_600_000L;

    @Test public void heavyChatNeverBlocksNightlyReflectionOrCuriosity() {
        SynergyCore.State s = new SynergyCore.State();
        long t = 1_000L;
        s = SynergyCore.advance(s, SynergyCore.Event.NONE, false, t);
        SynergyCore.Device night = new SynergyCore.Device(80, true, false, false, false);
        for (int day = 1; day <= 7; day++) {
            // 16 h despierto: 60 respuestas, algunas suspendidas por el revisor.
            for (int i = 0; i < 60; i++) {
                t += 16 * HOUR / 60;
                s = SynergyCore.advance(s, i % 10 == 0 ? SynergyCore.Event.FAILED : SynergyCore.Event.ANSWER, false, t);
            }
            // 8 h cargando: cada hora se despiertan los trabajos en segundo plano.
            boolean reflected = false, explored = false;
            for (int h = 0; h < 8; h++) {
                t += HOUR;
                s = SynergyCore.advance(s, SynergyCore.Event.NONE, true, t);
                reflected |= SynergyCore.mayReflect(s, night, false, false);
                if (SynergyCore.mayExplore(s, night, false, false)) {
                    explored = true;
                    s = SynergyCore.advance(s, h % 2 == 0 ? SynergyCore.Event.FOUND : SynergyCore.Event.NO_EVIDENCE, true, t);
                }
            }
            assertTrue("día " + day + ": la reflexión nocturna debe poder ejecutarse", reflected);
            assertTrue("día " + day + ": la curiosidad debe poder investigar alguna vez", explored);
        }
    }

    @Test public void realPhoneLimitsStillWin() {
        SynergyCore.State rested = new SynergyCore.State();
        rested.energy = 1; rested.curiosity = 1;
        assertFalse("sin cargar no reflexiona", SynergyCore.mayReflect(rested, new SynergyCore.Device(90, false, false, false, false), false, false));
        assertFalse("caliente no", SynergyCore.mayReflect(rested, new SynergyCore.Device(90, true, false, false, true), false, false));
        assertFalse("poca RAM no", SynergyCore.mayReflect(rested, new SynergyCore.Device(90, true, false, true, false), false, false));
        assertFalse("ahorro de batería no", SynergyCore.mayReflect(rested, new SynergyCore.Device(90, true, true, false, false), false, false));
        assertFalse("batería baja no", SynergyCore.mayReflect(rested, new SynergyCore.Device(15, true, false, false, false), false, false));
    }
}
