package salve.avatar;

import static org.junit.Assert.*;
import org.junit.Test;

/** Salve se gira con sus vistas ilustradas: hacia donde va, mirando a un lado y dando la vuelta. */
public class AvatarTurnTest {
    @Test public void looksTowardsWhereSheGoesThenFacesFront() {
        AvatarTurn turn = new AvatarTurn();
        assertEquals(-1, turn.view(true, false, false, -1, 0f));
        assertEquals("tres cuartos a la izquierda", 7, turn.view(false, true, false, -1, 1f));
        assertEquals("perfil izquierda", 6, turn.view(false, true, false, -1, 1.2f));
        assertEquals(-1, turn.view(false, true, false, -1, 2f));
        assertEquals("tres cuartos derecha", 1, turn.view(true, true, false, -1, 3f));
        assertEquals(2, turn.view(true, true, false, -1, 3.2f));
    }

    @Test public void glancesAsideNowAndThenOnlyWhenIdle() {
        AvatarTurn turn = new AvatarTurn();
        turn.view(true, false, true, -1, 0f);
        assertEquals(-1, turn.view(true, false, true, -1, 10f));
        int seen = turn.view(true, false, true, -1, 23f);
        assertTrue("mira a un lado", seen == 1 || seen == 7);
        assertEquals(-1, turn.view(true, false, true, -1, 26f));
        AvatarTurn busy = new AvatarTurn();
        busy.view(true, false, false, -1, 0f);
        assertEquals("hablando no se distrae", -1, busy.view(true, false, false, -1, 30f));
    }

    @Test public void spinGoesThroughAllEightViewsAndEndsFacingFront() {
        AvatarTurn turn = new AvatarTurn();
        java.util.Set<Integer> views = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) views.add(turn.view(true, false, false, i / 100f, i * .03f));
        assertEquals(8, views.size());
        assertEquals(0, turn.view(true, false, false, 1f, 3f));
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
