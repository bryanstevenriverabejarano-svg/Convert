package salve.core.voice;

import org.junit.Test;

import static org.junit.Assert.*;
import static salve.core.voice.VoiceConversationLoop.Action;
import static salve.core.voice.VoiceConversationLoop.RecognitionFailure;
import static salve.core.voice.VoiceConversationLoop.State;
import static salve.core.voice.VoiceConversationLoop.StopReason;

/** Session behavior with a monotonic fake clock; no Android microphone or TTS required. */
public class VoiceConversationLoopTest {
    @Test
    public void startsOneCaptureWithoutSubmittingText() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        assertFalse(loop.isActive());
        assertEquals(Long.MAX_VALUE, loop.nextDeadlineMillis());

        VoiceConversationLoop.Step capture = loop.start(100L);
        assertEquals(Action.LISTEN, capture.action);
        assertEquals(State.LISTENING, loop.state());
        assertTrue(loop.owns(capture.token));
        assertEquals("", capture.text);
        assertEquals(60_100L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.start(200L).action);
        assertEquals(capture.token, loop.token());
        assertEquals(Action.NONE, loop.tick(5_000L).action);
        assertEquals(State.LISTENING, loop.state());
    }

    @Test
    public void finalRecognitionSubmitsTrimmedTextOnce() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        VoiceConversationLoop.Step submitted = loop.recognized(token, "  Hola Salve  ", 100L);
        assertEquals(Action.SUBMIT, submitted.action);
        assertEquals("Hola Salve", submitted.text);
        assertEquals(State.THINKING, loop.state());
        assertEquals(120_100L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.recognized(token, "duplicado", 101L).action);
    }

    @Test
    public void successfulSpeechReopensMicrophoneOnlyAfterEchoGuard() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertEquals(Action.WAIT, loop.replied(token, true, 200L).action);
        assertEquals(State.SPEAKING, loop.state());
        assertEquals(Action.WAIT, loop.speechStarted(token, 210L).action);
        assertEquals(Action.WAIT, loop.speechFinished(token, true, 1_000L).action);
        assertEquals(State.COOLDOWN, loop.state());
        assertEquals(1_300L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(1_299L).action);
        VoiceConversationLoop.Step capture = loop.tick(1_300L);
        assertEquals(Action.LISTEN, capture.action);
        assertEquals(State.LISTENING, loop.state());
        assertNotEquals(token, capture.token);
        assertFalse(loop.owns(token));
    }

    @Test
    public void ttsStartCanArriveBeforeReplyWithoutExtendingSpeechDeadline() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertEquals(Action.WAIT, loop.speechStarted(token, 200L).action);
        assertEquals(State.SPEAKING, loop.state());
        assertEquals(120_200L, loop.nextDeadlineMillis());
        assertEquals(Action.WAIT, loop.replied(token, true, 300L).action);
        assertEquals(Action.WAIT, loop.speechStarted(token, 400L).action);
        assertEquals(120_200L, loop.nextDeadlineMillis());
    }

    @Test
    public void fastTtsCompletionBeforeReplyDoesNotRestartFinishedSpeech() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertEquals(Action.WAIT, loop.speechFinished(token, true, 200L).action);
        assertEquals(State.COOLDOWN, loop.state());
        assertEquals(Action.NONE, loop.replied(token, true, 210L).action);
        assertEquals(Action.NONE, loop.speechStarted(token, 220L).action);
        assertEquals(Action.NONE, loop.speechFinished(token, true, 230L).action);
        assertEquals(500L, loop.nextDeadlineMillis());
        assertEquals(Action.LISTEN, loop.tick(500L).action);
    }

    @Test
    public void responseCallbacksDuringCaptureAreIgnored() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.NONE, loop.replied(token, true, 10L).action);
        assertEquals(Action.NONE, loop.speechStarted(token, 20L).action);
        assertEquals(Action.NONE, loop.speechFinished(token, false, 30L).action);
        assertEquals(State.LISTENING, loop.state());
        assertEquals(60_000L, loop.nextDeadlineMillis());
    }

    @Test
    public void interruptionDiscardsOldModelAndTtsCallbacks() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        loop.replied(oldToken, true, 200L);
        VoiceConversationLoop.Step interrupted = loop.interrupt(300L);
        assertEquals(Action.WAIT, interrupted.action);
        assertNotEquals(oldToken, interrupted.token);
        assertFalse(loop.owns(oldToken));
        assertEquals(Action.NONE, loop.replied(oldToken, true, 310L).action);
        assertEquals(Action.NONE, loop.speechStarted(oldToken, 320L).action);
        assertEquals(Action.NONE, loop.speechFinished(oldToken, true, 330L).action);
        assertEquals(Action.NONE, loop.recognized(oldToken, "viejo", 340L).action);
        assertEquals(Action.NONE, loop.recognitionFailed(oldToken, RecognitionFailure.PERMISSION, 350L).action);
        assertEquals(State.COOLDOWN, loop.state());
        assertEquals(600L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.recognized(interrupted.token, "eco", 360L).action);
        assertEquals(Action.NONE, loop.tick(599L).action);
        VoiceConversationLoop.Step capture = loop.tick(600L);
        assertEquals(Action.LISTEN, capture.action);
        assertNotEquals(interrupted.token, capture.token);
        assertEquals(Action.NONE, loop.speechFinished(oldToken, true, 610L).action);
        assertEquals(Action.SUBMIT, loop.recognized(capture.token, "nuevo", 620L).action);
    }

    @Test
    public void interruptionDuringThinkingRevokesPendingResponseAndWaitsForEchoGuard() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        VoiceConversationLoop.Step interrupted = loop.interrupt(200L);
        assertEquals(Action.WAIT, interrupted.action);
        assertEquals(State.COOLDOWN, loop.state());
        assertNotEquals(oldToken, interrupted.token);
        assertFalse(loop.owns(oldToken));
        assertEquals(Action.NONE, loop.replied(oldToken, false, 300L).action);
        assertEquals(Action.NONE, loop.tick(499L).action);
        VoiceConversationLoop.Step capture = loop.tick(500L);
        assertEquals(Action.LISTEN, capture.action);
        assertNotEquals(interrupted.token, capture.token);
        assertTrue(loop.owns(capture.token));
        assertEquals(Action.NONE, loop.replied(oldToken, true, 510L).action);
    }

    @Test
    public void interruptionDuringCooldownRestartsEchoGuardBeforeOneNewCapture() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        loop.speechFinished(oldToken, true, 200L);
        VoiceConversationLoop.Step interrupted = loop.interrupt(250L);
        assertEquals(Action.WAIT, interrupted.action);
        assertEquals(State.COOLDOWN, loop.state());
        assertNotEquals(oldToken, interrupted.token);
        assertFalse(loop.owns(oldToken));
        assertEquals(Action.NONE, loop.tick(500L).action);
        assertEquals(550L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(549L).action);
        VoiceConversationLoop.Step capture = loop.tick(550L);
        assertEquals(Action.LISTEN, capture.action);
        assertNotEquals(interrupted.token, capture.token);
        assertEquals(capture.token, loop.token());
        assertEquals(60_550L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(551L).action);
    }

    @Test
    public void interruptionWhileAlreadyListeningDoesNotCreateAnotherRecognizer() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        assertEquals(Action.NONE, loop.interrupt(0L).action);
        long token = loop.start(10L).token;
        assertEquals(Action.NONE, loop.interrupt(100L).action);
        assertEquals(token, loop.token());
        assertEquals(60_010L, loop.nextDeadlineMillis());
    }

    @Test
    public void lifecycleStopInvalidatesEveryPendingCallbackAndTimer() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        assertStopped(loop, loop.stop(StopReason.LIFECYCLE), StopReason.LIFECYCLE);
        assertFalse(loop.owns(oldToken));
        assertEquals(Long.MAX_VALUE, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(999_999L).action);
        assertEquals(Action.NONE, loop.recognized(oldToken, "hola", 300L).action);
        assertEquals(Action.NONE, loop.replied(oldToken, true, 300L).action);
        assertEquals(Action.NONE, loop.speechStarted(oldToken, 300L).action);
        assertEquals(Action.NONE, loop.speechFinished(oldToken, true, 300L).action);
        assertEquals(Action.NONE, loop.recognitionFailed(oldToken, RecognitionFailure.TRANSIENT, 300L).action);
        assertEquals(Action.NONE, loop.stop(StopReason.USER).action);
        assertEquals(StopReason.LIFECYCLE, loop.stopReason());
    }

    @Test
    public void restartCreatesFreshSessionAndCannotAcceptPreviousSessionResponse() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        loop.stop(StopReason.USER);
        long newToken = loop.start(700_000L).token;
        assertNotEquals(oldToken, newToken);
        assertEquals(Action.NONE, loop.replied(oldToken, true, 700_010L).action);
        assertEquals(Action.SUBMIT, loop.recognized(newToken, "hola", 700_100L).action);
        assertEquals(State.THINKING, loop.state());
    }

    @Test
    public void silenceAllowsTwoRetriesThenStops() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.WAIT, loop.recognitionFailed(token, RecognitionFailure.SILENCE, 100L).action);
        assertEquals(600L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(599L).action);
        token = loop.tick(600L).token;
        assertEquals(Action.WAIT, loop.recognitionFailed(token, RecognitionFailure.SILENCE, 700L).action);
        token = loop.tick(1_200L).token;
        assertStopped(loop, loop.recognitionFailed(token, RecognitionFailure.SILENCE, 1_300L), StopReason.NO_SPEECH);
    }

    @Test
    public void nullAndBlankFinalTranscriptsUseSilenceBudgetInsteadOfSubmitting() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.WAIT, loop.recognized(token, null, 100L).action);
        token = loop.tick(600L).token;
        assertEquals(Action.WAIT, loop.recognized(token, " \n\t ", 700L).action);
        token = loop.tick(1_200L).token;
        assertStopped(loop, loop.recognized(token, "", 1_300L), StopReason.NO_SPEECH);
    }

    @Test
    public void transientFailureAllowsOneDelayedRetryThenStops() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.WAIT, loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 100L).action);
        assertEquals(1_600L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(1_599L).action);
        VoiceConversationLoop.Step retry = loop.tick(1_600L);
        assertEquals(Action.LISTEN, retry.action);
        assertStopped(loop, loop.recognitionFailed(retry.token, RecognitionFailure.TRANSIENT, 1_700L), StopReason.RECOGNIZER_ERROR);
    }

    @Test
    public void duplicateFailureCallbackCannotConsumeAnotherRetry() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 100L);
        assertEquals(Action.NONE, loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 200L).action);
        assertEquals(1_600L, loop.nextDeadlineMillis());
        VoiceConversationLoop.Step retry = loop.tick(1_600L);
        assertEquals(Action.LISTEN, retry.action);
        assertEquals(Action.NONE, loop.recognitionFailed(token, RecognitionFailure.PERMISSION, 1_650L).action);
        assertTrue(loop.owns(retry.token));
    }

    @Test
    public void successfulTurnResetsRecognitionRetryBudgets() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        loop.recognitionFailed(token, RecognitionFailure.SILENCE, 100L);
        token = loop.tick(600L).token;
        loop.recognitionFailed(token, RecognitionFailure.SILENCE, 700L);
        token = loop.tick(1_200L).token;
        loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 1_300L);
        token = loop.tick(2_800L).token;
        assertEquals(Action.SUBMIT, loop.recognized(token, "hola", 2_900L).action);
        loop.speechFinished(token, true, 3_000L);
        token = loop.tick(3_300L).token;
        assertEquals(Action.WAIT, loop.recognitionFailed(token, RecognitionFailure.SILENCE, 3_400L).action);
        token = loop.tick(3_900L).token;
        assertEquals(Action.WAIT, loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 4_000L).action);
        assertTrue(loop.isActive());
    }

    @Test
    public void permissionFailureStopsWithoutRetrying() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertStopped(loop, loop.recognitionFailed(token, RecognitionFailure.PERMISSION, 100L), StopReason.MICROPHONE_PERMISSION);
    }

    @Test
    public void unavailableRecognizerStopsWithoutRetrying() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertStopped(loop, loop.recognitionFailed(token, RecognitionFailure.UNAVAILABLE, 100L), StopReason.RECOGNIZER_UNAVAILABLE);
    }

    @Test
    public void replyWithoutPlayableAudioStopsAndDoesNotReopenMicrophone() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertStopped(loop, loop.replied(token, false, 200L), StopReason.AUDIO_UNAVAILABLE);
        assertEquals(Action.NONE, loop.tick(500L).action);
    }

    @Test
    public void failedTtsStopsEvenWhenStartCallbackHasNotArrived() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertStopped(loop, loop.speechFinished(token, false, 200L), StopReason.AUDIO_ERROR);
    }

    @Test
    public void failedTtsAfterStartStopsInsteadOfStartingAnotherCapture() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.speechStarted(token, 200L);
        assertStopped(loop, loop.speechFinished(token, false, 300L), StopReason.AUDIO_ERROR);
    }

    @Test
    public void lossOfAudioFocusStopsSessionAndRejectsLateCompletion() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.speechStarted(token, 200L);
        assertStopped(loop, loop.stop(StopReason.AUDIO_FOCUS), StopReason.AUDIO_FOCUS);
        assertEquals(Action.NONE, loop.speechFinished(token, true, 300L).action);
    }

    @Test
    public void captureWatchdogUsesSilenceRetriesAndCannotListenForever() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long firstToken = loop.start(0L).token;
        assertEquals(Action.NONE, loop.tick(59_999L).action);
        assertEquals(Action.WAIT, loop.tick(60_000L).action);
        assertEquals(State.COOLDOWN, loop.state());
        VoiceConversationLoop.Step second = loop.tick(60_500L);
        assertEquals(Action.LISTEN, second.action);
        assertNotEquals(firstToken, second.token);
        assertEquals(Action.WAIT, loop.tick(120_500L).action);
        assertEquals(Action.LISTEN, loop.tick(121_000L).action);
        assertStopped(loop, loop.tick(181_000L), StopReason.NO_SPEECH);
    }

    @Test
    public void unansweredModelRequestStopsAtResponseWatchdog() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertEquals(Action.NONE, loop.tick(120_099L).action);
        assertStopped(loop, loop.tick(120_100L), StopReason.RESPONSE_TIMEOUT);
        assertEquals(Action.NONE, loop.replied(token, true, 120_101L).action);
    }

    @Test
    public void missingTtsCompletionStopsAtSpeechWatchdog() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.replied(token, true, 200L);
        assertEquals(Action.NONE, loop.tick(120_199L).action);
        assertStopped(loop, loop.tick(120_200L), StopReason.SPEECH_TIMEOUT);
        assertEquals(Action.NONE, loop.speechFinished(token, true, 120_201L).action);
    }

    @Test
    public void finalRecognitionAtCaptureDeadlineCannotBeatPendingWatchdog() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.WAIT, loop.recognized(token, "demasiado tarde", 60_000L).action);
        assertEquals(State.COOLDOWN, loop.state());
        assertEquals(60_500L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.recognized(token, "duplicado", 60_001L).action);
        assertEquals(Action.LISTEN, loop.tick(60_500L).action);
    }

    @Test
    public void finalRecognitionImmediatelyBeforeCaptureDeadlineIsAccepted() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.SUBMIT, loop.recognized(token, "a tiempo", 59_999L).action);
        assertEquals(Action.NONE, loop.tick(60_000L).action);
        assertEquals(State.THINKING, loop.state());
    }

    @Test
    public void longUtteranceCanCompleteAfterOldTwentySecondLimit() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertEquals(Action.NONE, loop.tick(20_000L).action);
        assertEquals(Action.NONE, loop.tick(45_000L).action);
        assertEquals(Action.SUBMIT, loop.recognized(token, "explicación larga", 45_100L).action);
        assertEquals(State.THINKING, loop.state());
    }

    @Test
    public void stoppingDuringInterruptionGuardPreventsMicrophoneRestart() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.speechStarted(token, 200L);
        assertEquals(Action.WAIT, loop.interrupt(300L).action);
        assertStopped(loop, loop.stop(StopReason.LIFECYCLE), StopReason.LIFECYCLE);
        assertEquals(Action.NONE, loop.tick(600L).action);
        assertEquals(Action.NONE, loop.speechFinished(token, true, 700L).action);
    }

    @Test
    public void repeatedInterruptionsCannotOpenMicrophoneBeforeLatestEchoGuard() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long originalToken = thinking(loop);
        VoiceConversationLoop.Step first = loop.interrupt(200L);
        VoiceConversationLoop.Step second = loop.interrupt(300L);
        assertEquals(Action.WAIT, first.action);
        assertEquals(Action.WAIT, second.action);
        assertNotEquals(first.token, second.token);
        assertFalse(loop.owns(originalToken));
        assertFalse(loop.owns(first.token));
        assertEquals(Action.NONE, loop.tick(500L).action);
        assertEquals(Action.NONE, loop.tick(599L).action);
        assertEquals(Action.LISTEN, loop.tick(600L).action);
    }

    @Test
    public void lateReplyCannotBeatPendingResponseWatchdog() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertStopped(loop, loop.replied(token, true, 120_100L), StopReason.RESPONSE_TIMEOUT);
    }

    @Test
    public void lateTtsStartCannotReviveExpiredResponse() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertStopped(loop, loop.speechStarted(token, 120_100L), StopReason.RESPONSE_TIMEOUT);
    }

    @Test
    public void lateTtsCompletionCannotBeatPendingSpeechWatchdog() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.speechStarted(token, 200L);
        assertStopped(loop, loop.speechFinished(token, true, 120_200L), StopReason.SPEECH_TIMEOUT);
    }

    @Test
    public void lateCompletionWithoutStartCannotReviveExpiredResponse() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        assertStopped(loop, loop.speechFinished(token, true, 120_100L), StopReason.RESPONSE_TIMEOUT);
    }

    @Test
    public void duplicateLateReplyCannotExtendAnExpiredSpeechPhase() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = thinking(loop);
        loop.replied(token, true, 200L);
        assertStopped(loop, loop.replied(token, true, 120_200L), StopReason.SPEECH_TIMEOUT);
    }

    @Test
    public void staleCallbackCannotStopNewSessionEvenAfterOldDeadline() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long oldToken = thinking(loop);
        loop.stop(StopReason.USER);
        long newToken = loop.start(120_000L).token;
        assertEquals(Action.NONE, loop.replied(oldToken, true, 120_100L).action);
        assertEquals(Action.NONE, loop.speechFinished(oldToken, true, 120_100L).action);
        assertEquals(Action.SUBMIT, loop.recognized(newToken, "sesión actual", 120_200L).action);
    }

    @Test
    public void sessionLimitRemainsTenMinutesAcrossManySuccessfulTurns() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        for (int turn = 1; turn <= 59; turn++) {
            long now = turn * 10_000L;
            assertEquals(Action.SUBMIT, loop.recognized(token, "siguiente", now).action);
            assertEquals(Action.WAIT, loop.replied(token, true, now + 1L).action);
            loop.speechFinished(token, true, now + 2L);
            VoiceConversationLoop.Step capture = loop.tick(now + 302L);
            assertEquals(Action.LISTEN, capture.action);
            token = capture.token;
        }
        assertEquals(600_000L, loop.nextDeadlineMillis());
        assertEquals(Action.NONE, loop.tick(599_999L).action);
        assertStopped(loop, loop.tick(600_000L), StopReason.SESSION_LIMIT);
    }

    @Test
    public void callbackAtSessionLimitCannotSubmitNewUserTurn() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertStopped(loop, loop.recognized(token, "hola", 600_000L), StopReason.SESSION_LIMIT);
    }

    @Test
    public void sessionLimitTakesPriorityOverRecognizerRetry() {
        VoiceConversationLoop loop = new VoiceConversationLoop();
        long token = loop.start(0L).token;
        assertStopped(loop, loop.recognitionFailed(token, RecognitionFailure.TRANSIENT, 600_000L), StopReason.SESSION_LIMIT);
    }

    @Test
    public void sessionLimitTakesPriorityOverAudioAndInterruption() {
        VoiceConversationLoop replyLoop = new VoiceConversationLoop();
        long token = thinking(replyLoop);
        assertStopped(replyLoop, replyLoop.replied(token, true, 600_000L), StopReason.SESSION_LIMIT);

        VoiceConversationLoop speechLoop = new VoiceConversationLoop();
        token = thinking(speechLoop);
        assertStopped(speechLoop, speechLoop.speechFinished(token, true, 600_000L), StopReason.SESSION_LIMIT);

        VoiceConversationLoop interruptLoop = new VoiceConversationLoop();
        thinking(interruptLoop);
        assertStopped(interruptLoop, interruptLoop.interrupt(600_000L), StopReason.SESSION_LIMIT);
    }

    @Test
    public void exactVoiceStopCommandEndsSessionWithoutSubmittingToModel() {
        String[] commands = {
                "termina modo voz", "termina el modo voz", "termina el modo de voz",
                "salir del modo voz", "salir del modo de voz", "cierra el modo voz",
                "cierra el modo de voz", "sal del modo voz", "sal del modo de voz",
                " ¡TERMINA   EL MODO DE VOZ! "
        };
        for (String command : commands) {
            VoiceConversationLoop loop = new VoiceConversationLoop();
            long token = loop.start(0L).token;
            assertTrue(command, VoiceConversationLoop.isStopCommand(command));
            assertStopped(loop, loop.recognized(token, command, 100L), StopReason.USER);
        }
    }

    @Test
    public void mentioningOrNegatingStopCommandDoesNotEndConversation() {
        String[] ordinaryUtterances = {
                "No termina el modo de voz", "no cierres el modo de voz",
                "¿Cómo salir del modo voz?", "explica qué significa termina modo voz",
                "dije termina el modo voz ayer", "\"termina el modo voz\"",
                "termina", "sal", "quiero cambiar el modo voz", "adiós"
        };
        for (String text : ordinaryUtterances) {
            assertFalse(text, VoiceConversationLoop.isStopCommand(text));
            VoiceConversationLoop loop = new VoiceConversationLoop();
            long token = loop.start(0L).token;
            assertEquals(text, Action.SUBMIT, loop.recognized(token, text, 100L).action);
            assertEquals(State.THINKING, loop.state());
        }
        assertFalse(VoiceConversationLoop.isStopCommand(null));
        assertFalse(VoiceConversationLoop.isStopCommand(""));
    }

    private static long thinking(VoiceConversationLoop loop) {
        long token = loop.start(0L).token;
        assertEquals(Action.SUBMIT, loop.recognized(token, "hola", 100L).action);
        return token;
    }

    private static void assertStopped(VoiceConversationLoop loop, VoiceConversationLoop.Step step,
                                      StopReason expectedReason) {
        assertEquals(Action.STOP, step.action);
        assertEquals(expectedReason, step.reason);
        assertEquals(expectedReason, loop.stopReason());
        assertEquals(State.STOPPED, loop.state());
        assertFalse(loop.isActive());
    }
}
