package salve.avatar;

import static org.junit.Assert.*;
import org.junit.Test;

/** Salve se gira con sus vistas ilustradas: hacia donde va, mirando a un lado y dando la vuelta. */
public class AvatarTurnTest {
    @Test public void turnsThroughEveryIntermediateViewTowardsWhereSheGoes() {
        AvatarTurn turn = new AvatarTurn();
        assertEquals(-1, turn.view(true, false, false, -1, 0f));
        java.util.List<Integer> seen = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            int v = turn.view(false, true, false, -1, 1f + i * .02f);
            if (v >= 0 && (seen.isEmpty() || seen.get(seen.size() - 1) != v)) seen.add(v);
        }
        // G3 y G4 son los giros originales hacia el lado de la 08: 0 → G3 → 7 → G4 → 6 y vuelta.
        assertEquals("ida y vuelta por el lado de la 08",
                java.util.Arrays.asList(10, 7, 11, 6, 11, 7, 10), seen);
        assertEquals(-1, turn.view(false, true, false, -1, 3f));
        // Hacia el lado de la 02: 0 → G1 → 1 → G2 → 2.
        assertEquals("hacia el otro lado empieza por G1", 8, turn.view(true, true, false, -1, 4f));
        assertEquals("tres cuartos", 1, turn.view(true, true, false, -1, 4.15f));
        assertEquals("G2", 9, turn.view(true, true, false, -1, 4.25f));
        assertEquals("llega al perfil del lado 03", 2, turn.view(true, true, false, -1, 4.35f));
    }

    @Test public void glancesAsideNowAndThenOnlyWhenIdle() {
        AvatarTurn turn = new AvatarTurn();
        turn.view(true, false, true, -1, 0f);
        assertEquals(-1, turn.view(true, false, true, -1, 10f));
        assertEquals("empieza con el giro más leve", 8, turn.view(true, false, true, -1, 23f));
        assertEquals("se queda en tres cuartos", 1, turn.view(true, false, true, -1, 23.6f));
        assertEquals(-1, turn.view(true, false, true, -1, 26f));
        AvatarTurn busy = new AvatarTurn();
        busy.view(true, false, false, -1, 0f);
        assertEquals("hablando no se distrae", -1, busy.view(true, false, false, -1, 30f));
    }

    @Test public void spinGoesThroughTheEightViewsAndTheFourOriginalTurnsAndEndsFacingFront() {
        AvatarTurn turn = new AvatarTurn();
        java.util.Set<Integer> views = new java.util.HashSet<>();
        for (int i = 0; i < 160; i++) views.add(turn.view(true, false, false, i / 160f, i * .02f));
        assertEquals("8 vistas base + G1–G4", 12, views.size());
        assertEquals(0, turn.view(true, false, false, 1f, 4f));
    }

    @Test public void bodiesWithoutTurnSheetUseTheNearestView() {
        assertEquals("G1 → frente", 0, AvatarTurn.fallback(8));
        assertEquals("G2 → tres cuartos 02", 1, AvatarTurn.fallback(9));
        assertEquals("G3 → frente", 0, AvatarTurn.fallback(10));
        assertEquals("G4 → tres cuartos 08", 7, AvatarTurn.fallback(11));
        assertEquals(3, AvatarTurn.fallback(3));
    }

    @Test public void restAndSpinAreBodyCommands() {
        assertEquals(AvatarMotion.Gesture.REST, AvatarMotionProtocol.parseCommand("Cierra los ojos").gesture);
        assertEquals(AvatarMotion.Gesture.REST, AvatarMotionProtocol.parseCommand("medita").gesture);
        assertEquals(AvatarMotion.Gesture.SPIN, AvatarMotionProtocol.parseCommand("Date la vuelta").gesture);
        assertEquals(AvatarMotion.Gesture.SPIN, AvatarMotionProtocol.parse("Mira [[salve_motion:SPIN:WARM]]").gesture);
        AvatarMotion m = new AvatarMotion();
        m.previewGesture(AvatarMotion.Gesture.REST, AvatarMotion.Expression.NEUTRAL);
        RealisticSpriteAnimation.Frame f = RealisticSpriteAnimation.select(new AvatarState(), m.snapshot(), 0, 0, false);
        assertEquals("poses", f.sheet);
        assertEquals("cierra los ojos con su ilustración 32", 7, f.index);
        m.advance(2.5f);
        assertTrue(m.snapshot().gestureProgress > .4f && m.snapshot().gestureProgress < .6f);
        m.advance(3f);
        assertEquals(AvatarMotion.Gesture.NONE, m.snapshot().gesture);
    }
}
