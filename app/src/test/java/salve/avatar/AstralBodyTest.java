package salve.avatar;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import org.junit.Test;

public class AstralBodyTest {
    private static AvatarMotion.Snapshot idle() { return new AvatarMotion().snapshot(); }

    private static AstralBodyRig rig() throws Exception {
        Path p = Paths.get("src/main/assets/avatar/realistic/astral.json");
        if (!Files.exists(p)) p = Paths.get("app/src/main/assets/avatar/realistic/astral.json");
        return AstralBodyRig.parse(new String(Files.readAllBytes(p), StandardCharsets.UTF_8));
    }

    @Test public void blinksNaturallyAndReopens() {
        AstralBody body = new AstralBody(new Random(7));
        int closedFrames = 0;
        boolean reopened = false;
        long t = 0;
        body.advance(t, idle(), .5f, AvatarState.Pose.IDLE);
        for (int i = 0; i < 60 * 14; i++) {
            t += 16;
            AstralBody.Snapshot s = body.advance(t, idle(), .5f, AvatarState.Pose.IDLE);
            assertTrue(s.blink >= 0 && s.blink <= 1);
            if (s.blink >= .8f) closedFrames++;
            else if (closedFrames > 0 && s.blink == 0) reopened = true;
        }
        assertTrue("en 14 s debe parpadear varias veces", body.snapshot().blinks >= 2);
        assertTrue(reopened);
        assertTrue("un parpadeo dura décimas, no segundos", closedFrames < 60);
    }

    @Test public void voluntaryBlinkHappensNow() {
        AstralBody body = new AstralBody(new Random(1));
        body.advance(0, idle(), .5f, AvatarState.Pose.IDLE);
        body.advance(16, idle(), .5f, AvatarState.Pose.IDLE);
        long before = body.snapshot().blinks;
        body.requestBlink();
        float max = 0;
        for (int i = 2; i < 20; i++) max = Math.max(max, body.advance(i * 16L, idle(), .5f, AvatarState.Pose.IDLE).blink);
        assertEquals(before + 1, body.snapshot().blinks);
        assertTrue(max >= .8f);
    }

    @Test public void hairFollowsMovementThenSettles() {
        AstralBody body = new AstralBody(new Random(2));
        long t = 0;
        body.advance(t, idle(), .2f, AvatarState.Pose.IDLE);
        float peak = 0;
        for (int i = 1; i <= 30; i++) {
            t += 16;
            peak = Math.max(peak, Math.abs(body.advance(t, idle(), .2f + i * .01f, AvatarState.Pose.WALKING).hairSwing));
        }
        assertTrue("al caminar el pelo se balancea", peak > 3);
        for (int i = 0; i < 60 * 8; i++) { t += 16; body.advance(t, idle(), .5f, AvatarState.Pose.IDLE); }
        assertTrue("en reposo vuelve a una brisa suave", Math.abs(body.snapshot().hairSwing) < 6);
        body.flickHair(1);
        float flick = 0;
        for (int i = 0; i < 40; i++) { t += 16; flick = Math.max(flick, Math.abs(body.advance(t, idle(), .5f, AvatarState.Pose.IDLE).hairSwing)); }
        assertTrue(flick > 10);
        assertTrue(flick <= 42);
    }

    @Test public void drawingMakesCircuitsGlowAndExpires() throws Exception {
        AstralBody body = new AstralBody(new Random(3));
        long t = 0;
        body.advance(t, idle(), .5f, AvatarState.Pose.IDLE);
        for (int i = 0; i < 120; i++) { t += 16; body.advance(t, idle(), .5f, AvatarState.Pose.IDLE); }
        float calm = body.snapshot().glow;
        body.setDrawing("un árbol", .5f);
        assertEquals("un árbol", body.drawingNow());
        for (int i = 0; i < 40; i++) { t += 16; body.advance(t, idle(), .5f, AvatarState.Pose.IDLE); }
        assertTrue(body.snapshot().glow > calm);
        assertTrue(body.snapshot().glowRing >= 0);
        Thread.sleep(600);
        assertNull(body.drawingNow());
    }

    @Test public void rigKeepsArmsAndLegsStillWhileHairAndHeadMove() throws Exception {
        AstralBodyRig rig = rig();
        assertEquals(853, rig.width);
        assertEquals(1844, rig.height);
        // Columna cercana al borde izquierdo a media altura: melena exterior.
        int row = Math.round(750f / rig.height * rig.rows);
        assertTrue(rig.hairWeight(Math.round(60f / rig.width * rig.cols), row) > .6f);
        // Codo izquierdo (240, 600) y rodilla: fijos.
        assertEquals(0f, rig.hairWeight(Math.round(240f / rig.width * rig.cols), Math.round(600f / rig.height * rig.rows)), .05f);
        assertEquals(0f, rig.hairWeight(Math.round(360f / rig.width * rig.cols), Math.round(1300f / rig.height * rig.rows)), 1e-6f);
        // Los ojos están en la cabeza (peso completo).
        assertEquals(1f, rig.headWeight(rig.cols / 2, Math.round(170f / rig.height * rig.rows)), 1e-6f);
        float[] rest = rig.deform(null, null);
        AstralBody body = new AstralBody(new Random(4));
        body.flickHair(1);
        body.advance(0, idle(), .5f, AvatarState.Pose.IDLE);
        for (int i = 1; i < 12; i++) body.advance(i * 16L, idle(), .5f, AvatarState.Pose.IDLE);
        float[] moved = rig.deform(body.snapshot(), null);
        int hair = row * (rig.cols + 1) + Math.round(60f / rig.width * rig.cols);
        int knee = Math.round(1300f / rig.height * rig.rows) * (rig.cols + 1) + Math.round(360f / rig.width * rig.cols);
        assertNotEquals(rest[2 * hair], moved[2 * hair], .5f);
        assertEquals(rest[2 * knee], moved[2 * knee], 1e-4f);
        assertEquals(rest[2 * knee + 1], moved[2 * knee + 1], 1e-4f);
    }

    @Test public void senseDescribesTheBodyAndAcceptsBodyCommands() {
        AstralBody body = new AstralBody(new Random(5));
        body.advance(1000, idle(), .8f, AvatarState.Pose.SEATED);
        body.advance(1016, idle(), .8f, AvatarState.Pose.SEATED);
        String text = AstralBodySense.promptContext(body.snapshot(), "la luna", 1020);
        assertTrue(text.contains("cuerpo astral"));
        assertTrue(text.contains("sentada"));
        assertTrue(text.contains("a la derecha"));
        assertTrue(text.contains("estás dibujando la luna"));
        assertTrue(AstralBodySense.promptContext(null, null, 0).contains("no se está mostrando"));
        assertNotNull(AstralBodySense.command("Parpadea"));
        assertNotNull(AstralBodySense.command("mueve tu pelo"));
        assertNotNull(AstralBodySense.command("¡Brilla!"));
        assertNull(AstralBodySense.command("¿por qué parpadeas tanto?"));
    }
}
