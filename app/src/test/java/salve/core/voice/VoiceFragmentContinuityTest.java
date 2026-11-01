package salve.core.voice;

import org.junit.Test;

import static org.junit.Assert.*;
import static salve.core.voice.VoiceConversationLoop.Action;
import static salve.core.voice.VoiceConversationLoop.RecognitionFailure;
import static salve.core.voice.VoiceConversationLoop.State;
import static salve.core.voice.VoiceConversationLoop.StopReason;

/** Partial-turn continuity uses the original loop; no new ASR, TTS or LLM. */
public class VoiceFragmentContinuityTest {

    @Test public void reassemblesIncompleteQuestionBeforeSingleSubmission() {
        VoiceConversationLoop loop = new VoiceConversationLoop(true);
        long first = loop.start(0).token;
        VoiceConversationLoop.Step incomplete = loop.recognized(first, "Me gustaría saber cómo", 200);
        assertEquals(Action.CONTINUE, incomplete.action);
        assertEquals(State.COOLDOWN, loop.state());
        assertEquals("Me gustaría saber cómo", incomplete.text);
        assertTrue(loop.hasHeldSpeech());
        assertEquals(Action.NONE, loop.recognized(first, "duplicado", 210).action);
        assertEquals(Action.NONE, loop.tick(549).action);

        long next = loop.tick(550).token;
        assertNotEquals(first, next);
        assertEquals(State.LISTENING, loop.state());
        VoiceConversationLoop.Step done = loop.recognized(next, "funciona tu memoria", 600);
        assertEquals(Action.SUBMIT, done.action);
        assertEquals("Me gustaría saber cómo funciona tu memoria", done.text);
        assertFalse(loop.hasHeldSpeech());
        assertEquals(Action.NONE, loop.recognized(next, "duplicado", 601).action);
    }

    @Test public void silenceAfterFragmentSubmitsHeldSpeechInsteadOfLosingIt() {
        VoiceConversationLoop loop = new VoiceConversationLoop(true);
        long token = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(token, "Quiero saber cómo", 100).action);
        token = loop.tick(450).token;
        VoiceConversationLoop.Step done = loop.recognitionFailed(token, RecognitionFailure.SILENCE, 1_000);
        assertEquals(Action.SUBMIT, done.action);
        assertEquals("Quiero saber cómo", done.text);
        assertEquals(State.THINKING, loop.state());
        assertFalse(loop.hasHeldSpeech());
    }

    @Test public void emptyFinalAfterFragmentFlushesTheSameTurn() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long first = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(first, "Me pregunto si", 50).action);
        long second = loop.tick(400).token;
        VoiceConversationLoop.Step done = loop.recognized(second, "", 450);
        assertEquals(Action.SUBMIT, done.action);
        assertEquals("Me pregunto si", done.text);
    }

    @Test public void sentenceWithTerminalPunctuationIsImmediate() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0).token;
        assertEquals(Action.SUBMIT, loop.recognized(token, "¿Cómo funciona tu memoria?", 50).action);
        assertFalse(loop.hasHeldSpeech());
    }

    @Test public void boundedFragmentsCannotLoopForeverOrGrowWithoutLimit() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(token, "Me gustaría, ", 100).action);
        token = loop.tick(450).token;
        assertEquals(Action.CONTINUE, loop.recognized(token, "pero", 500).action);
        token = loop.tick(850).token;
        VoiceConversationLoop.Step done = loop.recognized(token, "con", 900);
        assertEquals(Action.SUBMIT, done.action);
        assertEquals("Me gustaría, pero con", done.text);
        assertFalse(loop.hasHeldSpeech());
    }

    @Test public void goodbyeStopsAndDiscardsUnsubmittedFragments() {
        VoiceConversationLoop loop = new VoiceConversationLoop(true);
        long token = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(token, "Me pregunto si", 80).action);
        token = loop.tick(430).token;
        VoiceConversationLoop.Step stopped = loop.recognized(token, "Adiós, Salve", 500);
        assertEquals(Action.STOP, stopped.action);
        assertEquals(StopReason.USER, loop.stopReason());
        assertFalse(loop.hasHeldSpeech());
    }

    @Test public void lifecycleAndPermissionStopClearHeldSpeech() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(token, "Me gustaría saber cómo", 80).action);
        loop.stop(StopReason.LIFECYCLE);
        assertFalse(loop.hasHeldSpeech());
        token = loop.start(2_000).token;
        assertEquals(Action.SUBMIT, loop.recognized(token, "Hola", 2_050).action);
    }

    @Test public void staleCallbackFromPreviousFragmentCannotSubmitAgain() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long old = loop.start(0).token;
        assertEquals(Action.CONTINUE, loop.recognized(old, "Dime cómo", 100).action);
        long current = loop.tick(450).token;
        assertEquals(Action.NONE, loop.recognized(old, "antiguo", 500).action);
        assertEquals(Action.SUBMIT, loop.recognized(current, "estás", 550).action);
        assertEquals(Action.NONE, loop.recognized(old, "antiguo", 600).action);
    }

    @Test public void classifierDoesNotPretendToBeGeneralSemanticUnderstanding() {
        assertTrue(VoiceConversationLoop.looksIncomplete("Me pregunto cómo"));
        assertTrue(VoiceConversationLoop.looksIncomplete("Y,"));
        assertTrue(VoiceConversationLoop.looksIncomplete("Quiero"));
        assertFalse(VoiceConversationLoop.looksIncomplete("¿Qué hora es?"));
        assertFalse(VoiceConversationLoop.looksIncomplete("Buenas tardes."));
        assertFalse(VoiceConversationLoop.looksIncomplete("Gracias"));
    }
}
