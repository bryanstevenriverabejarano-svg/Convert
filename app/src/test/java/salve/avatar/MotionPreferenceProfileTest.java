package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;
import static salve.avatar.MotionPreferenceProfile.Feedback.*;

public final class MotionPreferenceProfileTest {
    @Test public void defaultsPreserveExistingGestureSpeedAndStrength() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults();
        assertEquals(1f, profile.amplitude(), 0);
        assertEquals(1f, profile.tempo(), 0);
        assertEquals(0, profile.feedbackCount());
        assertFalse(profile.isConfirmed());
    }

    @Test public void feedbackChangesOnlyTheRequestedParameterAndKeepsOriginalImmutable() {
        MotionPreferenceProfile original = MotionPreferenceProfile.defaults();
        MotionPreferenceProfile softer = original.withFeedback(TOO_STRONG);
        MotionPreferenceProfile slower = original.withFeedback(TOO_FAST);
        assertEquals(.9f, softer.amplitude(), .0001f); assertEquals(1f, softer.tempo(), 0);
        assertEquals(1f, slower.amplitude(), 0); assertEquals(.9f, slower.tempo(), .0001f);
        assertEquals(1f, original.amplitude(), 0); assertEquals(1f, original.tempo(), 0);
        assertEquals(1, softer.feedbackCount()); assertEquals(1, slower.feedbackCount());
    }

    @Test public void repeatedFeedbackCannotDisableOrAccelerateMotionBeyondRigLimits() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults();
        for (int i = 0; i < 100; i++) profile = profile.withFeedback(TOO_FAST).withFeedback(TOO_STRONG);
        assertEquals(MotionPreferenceProfile.MIN_AMPLITUDE, profile.amplitude(), 0);
        assertEquals(MotionPreferenceProfile.MIN_TEMPO, profile.tempo(), 0);
        assertEquals(200, profile.feedbackCount());
    }

    @Test public void comfortableConfirmsCurrentValuesWithoutMakingMovementMoreExtreme() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.defaults().withFeedback(TOO_STRONG).withFeedback(TOO_FAST);
        MotionPreferenceProfile confirmed = profile.withFeedback(COMFORTABLE);
        assertEquals(profile.amplitude(), confirmed.amplitude(), 0);
        assertEquals(profile.tempo(), confirmed.tempo(), 0);
        assertTrue(confirmed.isConfirmed());
        assertFalse(confirmed.withFeedback(TOO_FAST).isConfirmed());
        assertTrue(confirmed.description().contains("A tu gusto"));
    }

    @Test public void preferencesSurviveSerializationAcrossSessions() {
        MotionPreferenceProfile saved = MotionPreferenceProfile.defaults().withFeedback(TOO_FAST)
                .withFeedback(TOO_STRONG).withFeedback(COMFORTABLE);
        MotionPreferenceProfile loaded = MotionPreferenceProfile.decode(saved.encode());
        assertEquals(saved.encode(), loaded.encode());
        assertEquals(saved.description(), loaded.description());
    }

    @Test public void resetRestoresDefaultsAndForgetsFeedbackCount() {
        MotionPreferenceProfile reset = MotionPreferenceProfile.defaults().withFeedback(TOO_FAST)
                .withFeedback(TOO_STRONG).withFeedback(COMFORTABLE).withFeedback(RESET);
        assertEquals(MotionPreferenceProfile.defaults().encode(), reset.encode());
    }

    @Test public void malformedUnsupportedAndNonFinitePreferencesUseDefaults() {
        String[] invalid = { null, "", "1", "2|.9|.9|true|1", "1|NaN|.9|true|1",
                "1|.9|Infinity|false|1", "1|1.01|1|false|1", "1|.44|.9|false|1",
                "1|.9|.64|false|1", "1|.9|1.01|false|1", "1|.9|.9|yes|1",
                "1|.9|.9|false|-1", "1|.9|.9|false|1000001", "1|.9|.9|false|1|extra" };
        for (String value : invalid) assertEquals(String.valueOf(value), MotionPreferenceProfile.defaults().encode(),
                MotionPreferenceProfile.decode(value).encode());
    }

    @Test public void feedbackCounterSaturatesWithoutOverflow() {
        MotionPreferenceProfile profile = MotionPreferenceProfile.decode("1|.9|.9|true|1000000").withFeedback(COMFORTABLE);
        assertEquals(1_000_000, profile.feedbackCount());
    }

    @Test(expected = IllegalArgumentException.class) public void missingFeedbackDoesNotInventAUserPreference() {
        MotionPreferenceProfile.defaults().withFeedback(null);
    }
}
