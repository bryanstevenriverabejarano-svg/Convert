package salve.core.voice;

import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic tests: no Android microphone, no stored PCM, no real AEC simulation. */
public class VoiceBargeInDetectorTest {
    private static final int FRAME = 320;

    private static short[] silence() { return new short[FRAME]; }
    private static short[] sine(double hz, int amplitude) {
        short[] pcm = new short[FRAME];
        for (int i = 0; i < FRAME; i++) {
            pcm[i] = (short) Math.round(amplitude * Math.sin(2 * Math.PI * hz * i / 16000.0));
        }
        return pcm;
    }

    @Test public void sustainedSpeechLikeSignalTriggersExactlyOnceAfterWarmup() {
        VoiceBargeInDetector detector = new VoiceBargeInDetector();
        for (int i = 0; i < VoiceBargeInDetector.WARMUP_FRAMES; i++)
            assertFalse(detector.accept(silence(), FRAME));

        short[] speech = sine(220, 8500);
        for (int i = 0; i < VoiceBargeInDetector.REQUIRED_VOICED_FRAMES - 1; i++)
            assertFalse(detector.accept(speech, FRAME));
        assertTrue(detector.accept(speech, FRAME));
        assertFalse(detector.accept(speech, FRAME));
    }

    @Test public void isolatedTransientDoesNotInterrupt() {
        VoiceBargeInDetector detector = new VoiceBargeInDetector();
        for (int i = 0; i < 20; i++) detector.accept(silence(), FRAME);
        assertFalse(detector.accept(sine(220, 14000), FRAME));
        for (int i = 0; i < 50; i++) assertFalse(detector.accept(silence(), FRAME));
    }

    @Test public void loudDcLikeBackgroundDoesNotCountAsSpeech() {
        VoiceBargeInDetector detector = new VoiceBargeInDetector();
        for (int i = 0; i < 20; i++) detector.accept(silence(), FRAME);
        short[] constant = new short[FRAME];
        java.util.Arrays.fill(constant, (short) 14000);
        for (int i = 0; i < 50; i++) assertFalse(detector.accept(constant, FRAME));
    }

    @Test public void invalidAndShortBuffersAreIgnored() {
        VoiceBargeInDetector detector = new VoiceBargeInDetector();
        assertFalse(detector.accept(null, 320));
        assertFalse(detector.accept(new short[100], 100));
        assertFalse(detector.accept(new short[320], 321));
        assertFalse(detector.accept(new short[320], -1));
    }

    @Test public void interruptedTurnUsesExistingOwnerAndEchoCooldown() {
        VoiceConversationLoop loop = new VoiceConversationLoop(true);
        long token = loop.start(0).token;
        assertEquals(VoiceConversationLoop.Action.SUBMIT,
                loop.recognized(token, "Salve dime una cosa", 100).action);
        loop.speechStarted(token, 200);
        VoiceConversationLoop.Step interrupted = loop.interrupt(700);
        assertEquals(VoiceConversationLoop.Action.WAIT, interrupted.action);
        assertFalse(loop.owns(token));
        assertEquals(VoiceConversationLoop.Action.NONE, loop.speechFinished(token, true, 750).action);
        assertEquals(VoiceConversationLoop.Action.NONE, loop.tick(999).action);
        assertEquals(VoiceConversationLoop.Action.LISTEN, loop.tick(1000).action);
    }
}
