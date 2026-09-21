package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AvatarMotionTest {
    private AvatarMotion running() {
        AvatarMotion motion = new AvatarMotion(); motion.activate(1); motion.beginTurn(1, 2, "Hola");
        return motion;
    }
    private void speak(AvatarMotion motion, long turn, String id) {
        motion.speechPending(1, turn, id); motion.speechStart(1, id); motion.advance(.1f);
    }
    private AvatarMotionProtocol.Result wave() { return AvatarMotionProtocol.parse("Hola [[salve_motion:WAVE:WARM]]"); }

    @Test public void onlyActiveOwnerAndTurnCanAnimate() {
        AvatarMotion m = running(); m.response(9, 2, wave());
        assertEquals(AvatarMotion.Gesture.THINK, m.snapshot().gesture);
        m.response(1, 1, wave()); assertEquals(AvatarMotion.Gesture.THINK, m.snapshot().gesture);
        m.response(1, 2, wave()); assertEquals(AvatarMotion.Gesture.WAVE, m.snapshot().gesture);
        m.activate(7); m.response(1, 2, wave()); assertEquals(AvatarMotion.Gesture.NONE, m.snapshot().gesture);
        m.activate(1); m.beginTurn(1, 10, "hola"); assertFalse(m.snapshot().thinking);
    }
    @Test public void newTurnInterruptsOldSpeechAndRejectsItsCallbacks() {
        AvatarMotion m = running(); speak(m, 2, "old"); assertTrue(m.snapshot().speaking);
        m.beginTurn(1, 3, "otra pregunta");
        assertEquals(0, m.snapshot().mouthOpen, 0); assertFalse(m.snapshot().speaking);
        m.speechStart(1, "old"); m.speechRange(1, "old", 0, 5);
        assertFalse(m.snapshot().speaking);
        speak(m, 3, "new"); m.speechEnd(1, "old"); assertTrue(m.snapshot().speaking);
    }
    @Test public void listeningClosesMouthAndInvalidatesPreviousTurnEvenAfterListeningEnds() {
        AvatarMotion m = running(); speak(m, 2, "voice");
        m.listening(1, true); assertTrue(m.snapshot().listening); assertEquals(0, m.snapshot().mouthOpen, 0);
        m.response(1, 2, wave()); m.speechPending(1, 2, "late"); m.speechStart(1, "late");
        assertTrue(m.snapshot().listening); assertFalse(m.snapshot().speaking);
        m.listening(1, false); m.speechStart(1, "voice"); assertFalse(m.snapshot().speaking);
        m.beginTurn(1, 3, "hola"); speak(m, 3, "fresh"); assertTrue(m.snapshot().speaking);
    }
    @Test public void redundantListeningFalseDoesNotStopSpeech() {
        AvatarMotion m = running(); speak(m, 2, "voice"); m.listening(1, false);
        assertTrue(m.snapshot().speaking);
    }
    @Test public void finishingGenerationPreservesResponseGestureAndSpeech() {
        AvatarMotion m = running(); m.response(1, 2, wave()); speak(m, 2, "voice"); m.endTurn(1, 2);
        assertFalse(m.snapshot().thinking); assertTrue(m.snapshot().speaking);
        assertEquals(AvatarMotion.Gesture.WAVE, m.snapshot().gesture);
    }
    @Test public void pendingSpeechHasNoOpenMouthBeforeActualStart() {
        AvatarMotion m = running(); m.speechPending(1, 2, "voice"); m.advance(.1f);
        m.speechRange(1, "voice", 0, 5);
        assertFalse(m.snapshot().speaking); assertEquals(0, m.snapshot().mouthOpen, 0);
        m.speechStart(1, "voice"); m.advance(.1f); assertTrue(m.snapshot().mouthOpen > 0);
        m.speechEnd(1, "voice"); assertEquals(0, m.snapshot().mouthOpen, 0);
    }
    @Test public void pendingTimeoutRejectsLateStart() {
        AvatarMotion m = running(); m.speechPending(1, 2, "never-started");
        for (int i = 0; i < 110; i++) m.advance(.1f);
        m.speechStart(1, "never-started"); assertFalse(m.snapshot().speaking);
    }
    @Test public void pauseAndCloseRejectQueuedCallbacks() {
        AvatarMotion m = running(); speak(m, 2, "voice"); m.pause(1);
        m.response(1, 2, wave()); m.speechStart(1, "voice");
        assertFalse(m.snapshot().speaking); assertEquals(AvatarMotion.Gesture.NONE, m.snapshot().gesture);
        m.beginTurn(1, 2, "old"); assertFalse(m.snapshot().thinking);
        m.beginTurn(1, 3, "new"); assertTrue(m.snapshot().thinking);
        m.close(1); m.beginTurn(1, 4, "closed"); assertFalse(m.snapshot().thinking); m.speechPending(1, 3, "closed"); m.speechStart(1, "closed"); assertFalse(m.snapshot().speaking);
    }
    @Test public void fallbackGestureUsesActWithoutRequiringAnotherModelCall() {
        AvatarMotion m = running(); m.response(1, 2, AvatarMotionProtocol.parse("Hola."));
        assertEquals(AvatarMotion.Gesture.WAVE, m.snapshot().gesture);
        m.beginTurn(1, 3, "Estoy triste"); m.response(1, 3, AvatarMotionProtocol.parse("Te escucho."));
        assertEquals(AvatarMotion.Expression.NEUTRAL, m.snapshot().expression);
    }
    @Test public void clarificationAndErrorSurvivePlainTextResponse() {
        AvatarMotion m = running(); m.clarification(1, 2); m.response(1, 2, AvatarMotionProtocol.parse("¿Cuál?"));
        assertEquals(AvatarMotion.Expression.CURIOUS, m.snapshot().expression);
        m.error(1, 2); m.response(1, 2, AvatarMotionProtocol.parse("Ha fallado."));
        assertEquals(AvatarMotion.Expression.CONCERNED, m.snapshot().expression);
    }
    @Test public void roomPreviewDoesNotInvalidateCurrentAudio() {
        AvatarMotion m = running(); speak(m, 2, "voice");
        m.previewGesture(AvatarMotion.Gesture.NOD, AvatarMotion.Expression.WARM);
        assertTrue(m.snapshot().speaking); assertEquals(AvatarMotion.Gesture.NOD, m.snapshot().gesture);
        m.speechEnd(1, "voice"); assertFalse(m.snapshot().speaking);
    }
    @Test public void everyGestureRemainsFiniteAndWithinRigBoundsThenReturnsToRest() {
        for (AvatarMotion.Gesture gesture : AvatarMotion.Gesture.values()) {
            AvatarMotion m = running();
            m.response(1, 2, AvatarMotionProtocol.parse("Ok [[salve_motion:" + gesture + ":WARM]]"));
            speak(m, 2, "voice");
            for (int i = 0; i < 100; i++) {
                m.advance(.1f); AvatarMotion.Snapshot p = m.snapshot();
                inRange(p.leftArm, -25, 25); inRange(p.rightArm, -25, 25);
                inRange(p.headTilt, -12, 12); inRange(p.headYaw, -14, 14); inRange(p.headPitch, -10, 10);
                inRange(p.bodyTilt, -5, 5); inRange(p.gazeX, -1, 1); inRange(p.gazeY, -1, 1);
                inRange(p.blink, 0, 1); inRange(p.mouthOpen, 0, 1); inRange(p.breath, 0, 1);
                inRange(p.leftKnee, 0, 1); inRange(p.rightKnee, 0, 1);
            }
            assertEquals(AvatarMotion.Gesture.NONE, m.snapshot().gesture);
            assertEquals(0, m.snapshot().leftArm, .01f); assertEquals(0, m.snapshot().rightArm, .01f);
        }
    }
    @Test public void invalidAndLongFrameDurationsDoNotJumpOrPoisonTheRig() {
        AvatarMotion a = running(), b = running(); a.response(1, 2, wave()); b.response(1, 2, wave());
        a.advance(Float.NaN); a.advance(Float.POSITIVE_INFINITY); a.advance(-10); a.advance(0);
        assertEquals(0, a.snapshot().rightArm, 0);
        a.advance(.1f); b.advance(.1f);
        assertEquals(b.snapshot().rightArm, a.snapshot().rightArm, 0);
    }
    @Test public void hiddenViewResumeExpiresOldGestureExpressionAndAudio() {
        AvatarMotion m = running(); m.response(1, 2, wave()); speak(m, 2, "voice");
        assertTrue(m.snapshot().speaking);
        m.advance(600f);
        assertFalse(m.snapshot().speaking); assertEquals(0, m.snapshot().mouthOpen, 0);
        assertEquals(AvatarMotion.Gesture.NONE, m.snapshot().gesture);
        assertEquals(AvatarMotion.Expression.NEUTRAL, m.snapshot().expression);
        m.speechStart(1, "voice"); assertFalse(m.snapshot().speaking);
    }
    private void inRange(float value, float min, float max) {
        assertTrue("finite", Float.isFinite(value)); assertTrue(value >= min); assertTrue(value <= max);
    }
}
