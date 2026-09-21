package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;
import static salve.avatar.MotionPreferenceProfile.Feedback.*;

/** Verifies the user preference affects actual animation without altering voice or session ownership. */
public final class MotionPreferenceIntegrationTest {
    private AvatarMotion gesture(AvatarMotion.Gesture gesture) {
        AvatarMotion motion = new AvatarMotion();
        motion.activate(1); motion.beginTurn(1, 2, "Hola");
        motion.previewGesture(gesture, AvatarMotion.Expression.WARM);
        return motion;
    }

    private MotionPreferenceProfile gentle() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults();
        for (int i = 0; i < 10; i++) profile = profile.withFeedback(TOO_STRONG).withFeedback(TOO_FAST);
        return profile;
    }

    @Test public void softerFeedbackReducesActualGestureAmplitude() {
        AvatarMotion normal = gesture(AvatarMotion.Gesture.THINK), softer = gesture(AvatarMotion.Gesture.THINK);
        softer.setPreferences(MotionPreferenceProfile.defaults().withFeedback(TOO_STRONG));
        for (int i = 0; i < 20; i++) { normal.advance(.02f); softer.advance(.02f); }
        assertEquals(normal.snapshot().rightArm * .9f, softer.snapshot().rightArm, .0001f);
        assertEquals(normal.snapshot().headTilt * .9f, softer.snapshot().headTilt, .0001f);
    }

    @Test public void slowerFeedbackReducesOscillationCountWithoutExtendingGestureLifetime() {
        AvatarMotion normal = gesture(AvatarMotion.Gesture.SHAKE), slower = gesture(AvatarMotion.Gesture.SHAKE);
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults();
        for (int i = 0; i < 10; i++) profile = profile.withFeedback(TOO_FAST);
        slower.setPreferences(profile);
        int normalCrossings = 0, slowerCrossings = 0;
        float previousNormal = 0, previousSlower = 0;
        for (int i = 0; i < 145; i++) {
            normal.advance(.01f); slower.advance(.01f);
            float normalYaw = normal.snapshot().headYaw, slowerYaw = slower.snapshot().headYaw;
            if (normalYaw * previousNormal < 0) normalCrossings++;
            if (slowerYaw * previousSlower < 0) slowerCrossings++;
            previousNormal = normalYaw; previousSlower = slowerYaw;
            assertEquals(normal.snapshot().gesture, slower.snapshot().gesture);
        }
        assertTrue("slower oscillation", slowerCrossings < normalCrossings);
        normal.advance(.1f); slower.advance(.1f);
        assertEquals(AvatarMotion.Gesture.NONE, normal.snapshot().gesture);
        assertEquals(normal.snapshot().gesture, slower.snapshot().gesture);
    }

    @Test public void comfortPreferencesDoNotAlterMouthBlinkGazeBreathingOrAudioExpiry() {
        AvatarMotion normal = gesture(AvatarMotion.Gesture.EXPLAIN), adapted = gesture(AvatarMotion.Gesture.EXPLAIN);
        adapted.setPreferences(gentle());
        normal.speechPending(1, 2, "voice"); adapted.speechPending(1, 2, "voice");
        normal.speechStart(1, "voice"); adapted.speechStart(1, "voice");
        for (int i = 0; i < 200; i++) {
            if (i == 10) { normal.speechRange(1, "voice", 0, 3); adapted.speechRange(1, "voice", 0, 3); }
            normal.advance(.02f); adapted.advance(.02f);
            AvatarMotion.Snapshot a = normal.snapshot(), b = adapted.snapshot();
            assertEquals(a.mouthOpen, b.mouthOpen, 0); assertEquals(a.blink, b.blink, 0);
            assertEquals(a.breath, b.breath, 0); assertEquals(a.gazeX, b.gazeX, 0); assertEquals(a.gazeY, b.gazeY, 0);
            assertEquals(a.speaking, b.speaking); assertEquals(a.expression, b.expression);
        }
        normal.advance(90); adapted.advance(90);
        assertFalse(normal.snapshot().speaking); assertFalse(adapted.snapshot().speaking);
        assertEquals(0, adapted.snapshot().mouthOpen, 0);
    }

    @Test public void changingPreferencesDuringSpeechDoesNotRestartOrStopAudio() {
        AvatarMotion motion = gesture(AvatarMotion.Gesture.WAVE);
        motion.speechPending(1, 2, "voice"); motion.speechStart(1, "voice"); motion.advance(.1f);
        float mouth = motion.snapshot().mouthOpen;
        motion.setPreferences(gentle());
        assertTrue(motion.snapshot().speaking); assertEquals(mouth, motion.snapshot().mouthOpen, 0);
        motion.speechEnd(1, "voice"); assertEquals(0, motion.snapshot().mouthOpen, 0);
    }

    @Test public void adaptedPendingSpeechStillExpiresOnRealTime() {
        AvatarMotion motion = gesture(AvatarMotion.Gesture.WAVE); motion.setPreferences(gentle());
        motion.speechPending(1, 2, "late"); motion.advance(10.1f); motion.speechStart(1, "late");
        assertFalse(motion.snapshot().speaking); assertEquals(0, motion.snapshot().mouthOpen, 0);
    }

    @Test public void feedbackCannotReviveClosedOrInterruptedTurn() {
        AvatarMotion motion = gesture(AvatarMotion.Gesture.WAVE);
        motion.speechPending(1, 2, "old"); motion.listening(1, true);
        motion.setPreferences(gentle()); motion.listening(1, false); motion.speechStart(1, "old");
        assertFalse(motion.snapshot().speaking);
        motion.close(1); motion.setPreferences(MotionPreferenceProfile.defaults());
        motion.beginTurn(1, 3, "closed"); motion.speechPending(1, 3, "closed"); motion.speechStart(1, "closed");
        assertFalse(motion.snapshot().thinking); assertFalse(motion.snapshot().speaking);
    }

    @Test public void preferencesSurviveSessionActivationWhileOldVisualStateDoesNot() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults().withFeedback(TOO_STRONG);
        AvatarMotion persisted = gesture(AvatarMotion.Gesture.THINK); persisted.setPreferences(profile);
        persisted.advance(.4f); persisted.close(1); persisted.activate(3);
        assertEquals(0, persisted.snapshot().rightArm, 0);
        persisted.previewGesture(AvatarMotion.Gesture.THINK, AvatarMotion.Expression.CURIOUS);
        AvatarMotion fresh = gesture(AvatarMotion.Gesture.THINK); fresh.setPreferences(profile);
        for (int i = 0; i < 10; i++) { persisted.advance(.02f); fresh.advance(.02f); }
        assertEquals(fresh.snapshot().rightArm, persisted.snapshot().rightArm, .0001f);
    }

    @Test public void allAdaptedGesturesRemainFiniteBoundedAndReturnToRestAfterHiddenInterval() {
        for (AvatarMotion.Gesture gesture : AvatarMotion.Gesture.values()) {
            AvatarMotion motion = gesture(gesture); motion.setPreferences(gentle());
            for (int i = 0; i < 200; i++) {
                motion.advance(.016f); AvatarMotion.Snapshot pose = motion.snapshot();
                assertTrue(Float.isFinite(pose.leftArm)); assertTrue(Float.isFinite(pose.rightArm));
                assertTrue(Math.abs(pose.leftArm) <= 25); assertTrue(Math.abs(pose.rightArm) <= 25);
                assertTrue(Math.abs(pose.headYaw) <= 14); assertTrue(Math.abs(pose.headPitch) <= 10);
                assertTrue(pose.leftKnee >= 0 && pose.leftKnee <= 1);
            }
            motion.advance(600); assertEquals(AvatarMotion.Gesture.NONE, motion.snapshot().gesture);
            for (int i = 0; i < 30; i++) motion.advance(.1f);
            assertEquals(0, motion.snapshot().leftArm, .001f); assertEquals(0, motion.snapshot().rightArm, .001f);
        }
    }
}
