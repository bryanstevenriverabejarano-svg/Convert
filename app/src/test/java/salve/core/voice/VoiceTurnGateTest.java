package salve.core.voice;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoiceTurnGateTest {
    @Test public void cancelledMicrophoneDoesNotResumeTheInterruptedInference() {
        VoiceTurnGate gate = new VoiceTurnGate();
        long inference = gate.beginTurn();
        gate.listening(true);
        gate.listening(false);
        assertFalse(gate.maySpeak(inference));
        assertTrue(gate.maySpeak(gate.beginTurn()));
    }

    @Test public void aNewTurnRevokesThePreviousAudioEvenWithoutMicrophoneUse() {
        VoiceTurnGate gate = new VoiceTurnGate();
        long first = gate.beginTurn();
        long second = gate.beginTurn();
        assertFalse(gate.maySpeak(first));
        assertTrue(gate.maySpeak(second));
    }

    @Test public void aStandaloneAnswerKeepsItsPermissionAfterAnOlderResultArrives() {
        VoiceTurnGate gate = new VoiceTurnGate();
        long inference = gate.beginTurn();
        long standalone = gate.beginTurn();
        assertFalse(gate.maySpeak(inference));
        // Inspecting a late inference cannot revoke the newer utterance.
        assertTrue(gate.maySpeak(standalone));
    }

    @Test public void aQueuedTurnStartedWhileListeningStaysSilentAfterCancellation() {
        VoiceTurnGate gate = new VoiceTurnGate();
        gate.listening(true);
        long queued = gate.beginTurn();
        assertEquals(0L, queued);
        gate.listening(false);
        assertFalse(gate.maySpeak(queued));
    }

    @Test public void endingAnInactiveListenDoesNotInvalidateCurrentSpeech() {
        VoiceTurnGate gate = new VoiceTurnGate();
        long turn = gate.beginTurn();
        gate.listening(false);
        assertTrue(gate.maySpeak(turn));
    }

    @Test public void closingCannotBeReversedByLateEvents() {
        VoiceTurnGate gate = new VoiceTurnGate();
        long turn = gate.beginTurn();
        gate.close();
        gate.listening(false);
        assertFalse(gate.maySpeak(turn));
        assertFalse(gate.maySpeak(gate.beginTurn()));
    }
}
