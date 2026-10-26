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

    /** Rig de una versión del cuerpo (avatar/realistic = actual, avatar/realistic_v1 = registro). */
    private static AstralBodyRig rig(String folder) throws Exception {
        Path p = Paths.get("src/main/assets/avatar/" + folder + "/astral.json");
        if (!Files.exists(p)) p = Paths.get("app/src/main/assets/avatar/" + folder + "/astral.json");
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
        AstralBodyRig rig = rig(AvatarBodyVersions.V1);
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
        String text = AstralBodySense.promptContext(body.snapshot(), "la luna", 1020, true);
        assertTrue(text.contains("ESQUEMA_CORPORAL"));
        assertTrue(text.contains("sentada"));
        assertTrue(text.contains("a la derecha"));
        assertTrue(text.contains("dibujando la luna"));
        String brief = AstralBodySense.promptContext(body.snapshot(), null, 1020, false);
        assertFalse("sin el tema, ni esquema ni epiciclos", brief.contains("Fourier") || brief.contains("ESQUEMA"));
        assertTrue(brief.length() < 120);
        assertTrue(AstralBodySense.promptContext(null, null, 0, false).contains("no se muestra"));
        assertTrue(AstralBodySense.relevant("Entonces la casa que hiciste antes, ¿cómo la hiciste?"));
        assertTrue(AstralBodySense.relevant("¿Quién eres?"));
        assertFalse(AstralBodySense.relevant("¿Qué tiempo hace mañana en Vitoria?"));
        assertNotNull(AstralBodySense.command("Parpadea"));
        assertNotNull(AstralBodySense.command("mueve tu pelo"));
        assertNotNull(AstralBodySense.command("¡Brilla!"));
        assertNull(AstralBodySense.command("¿por qué parpadeas tanto?"));
    }

    @Test public void theNewBaseMovesHairAndHeadButNotItsLimbs() throws Exception {
        AstralBodyRig rig = rig(AvatarBodyVersions.CURRENT);
        assertEquals(1024, rig.width);
        assertEquals(1536, rig.height);
        assertEquals(230f, rig.hairTop, 1e-6f);
        assertEquals(720f, rig.hairSpan, 1e-6f);
        int row = Math.round(800f / rig.height * rig.rows);
        // Melena exterior a los dos lados: ondea.
        assertTrue(rig.hairWeight(Math.round(90f / rig.width * rig.cols), row) > .6f);
        assertTrue(rig.hairWeight(Math.round(935f / rig.width * rig.cols), row) > .6f);
        // Las colas blancas del traje también ondean.
        assertTrue(rig.hairWeight(Math.round(230f / rig.width * rig.cols), Math.round(1100f / rig.height * rig.rows)) > .5f);
        // Codo, muñeca y rodilla: fijos.
        assertEquals(0f, rig.hairWeight(Math.round(345f / rig.width * rig.cols), Math.round(560f / rig.height * rig.rows)), .05f);
        assertEquals(0f, rig.hairWeight(Math.round(745f / rig.width * rig.cols), Math.round(700f / rig.height * rig.rows)), .05f);
        assertEquals(0f, rig.hairWeight(Math.round(470f / rig.width * rig.cols), Math.round(1100f / rig.height * rig.rows)), 1e-6f);
        // Ojos y auriculares, en la cabeza.
        assertEquals(1f, rig.headWeight(Math.round(510f / rig.width * rig.cols), Math.round(145f / rig.height * rig.rows)), 1e-6f);
        assertTrue("la luz llega hasta los pies", rig.reach > rig.feetY - rig.heartY);
        assertEquals("bocas «a» y «o» para hablar", 2, rig.mouthFrames);
        assertTrue("la boca está bajo los ojos", (rig.mouthTop + rig.mouthBottom) / 2 > rig.eyeBottom);
        assertEquals("y en la cabeza", 1f, rig.headWeight(Math.round((rig.mouthLeft + rig.mouthRight) / 2f / rig.width * rig.cols),
                Math.round(rig.mouthTop / (float) rig.height * rig.rows)), 1e-6f);
        assertEquals("la v1 no tiene bocas", 0, rig(AvatarBodyVersions.V1).mouthFrames);
        float[] rest = rig.deform(null, null);
        AstralBody body = new AstralBody(new Random(9));
        body.flickHair(-1);
        body.advance(0, idle(), .5f, AvatarState.Pose.IDLE);
        for (int i = 1; i < 12; i++) body.advance(i * 16L, idle(), .5f, AvatarState.Pose.IDLE);
        float[] moved = rig.deform(body.snapshot(), null);
        int hair = row * (rig.cols + 1) + Math.round(90f / rig.width * rig.cols);
        int knee = Math.round(1100f / rig.height * rig.rows) * (rig.cols + 1) + Math.round(470f / rig.width * rig.cols);
        assertNotEquals(rest[2 * hair], moved[2 * hair], .5f);
        assertEquals(rest[2 * knee], moved[2 * knee], 1e-4f);
    }

    @Test public void lightKeepsRunningAlongTheLinesAndQuickensWhenThinking() {
        AstralBody calm = new AstralBody(new Random(10));
        long t = 0;
        calm.advance(t, idle(), .5f, AvatarState.Pose.IDLE);
        java.util.Set<Integer> visited = new java.util.HashSet<>();
        for (int i = 0; i < 60 * 5; i++) {
            t += 16;
            AstralBody.Snapshot s = calm.advance(t, idle(), .5f, AvatarState.Pose.IDLE);
            assertTrue(s.flow >= 0 && s.flow < 1);
            visited.add((int) (s.flow * 10));
        }
        assertEquals("en reposo el frente recorre todo el cuerpo", 10, visited.size());
        float calmStrength = calm.snapshot().flowStrength;
        assertTrue(calmStrength > .5f && calmStrength < .9f);

        AvatarMotion thinking = new AvatarMotion();
        thinking.activate(1);
        thinking.beginTurn(1, 1, "¿qué piensas?");
        AstralBody busy = new AstralBody(new Random(10));
        AstralBody quiet = new AstralBody(new Random(10));
        long u = 0;
        busy.advance(u, thinking.snapshot(), .5f, AvatarState.Pose.IDLE);
        quiet.advance(u, idle(), .5f, AvatarState.Pose.IDLE);
        float busyTravel = 0, quietTravel = 0, lastBusy = 0, lastQuiet = 0;
        for (int i = 0; i < 60; i++) {
            u += 16;
            float b = busy.advance(u, thinking.snapshot(), .5f, AvatarState.Pose.IDLE).flow;
            float q = quiet.advance(u, idle(), .5f, AvatarState.Pose.IDLE).flow;
            busyTravel += (b - lastBusy + 1) % 1; quietTravel += (q - lastQuiet + 1) % 1;
            lastBusy = b; lastQuiet = q;
        }
        assertTrue("pensando, la luz corre más deprisa", busyTravel > quietTravel * 1.5f);
        assertTrue(busy.snapshot().flowStrength > calmStrength);
    }

    @Test public void theFirstBodyStaysInTheWardrobe() {
        assertEquals(AvatarBodyVersions.CURRENT, AvatarBodyVersions.all().get(0).id);
        AvatarBodyVersions.Version v1 = AvatarBodyVersions.find(AvatarBodyVersions.V1);
        assertNotNull(v1);
        assertEquals("12 oct 2026", v1.approved);
        assertNull(AvatarBodyVersions.find("../core"));
    }
}
