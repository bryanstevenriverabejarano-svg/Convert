package salve.core.voice;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static salve.core.voice.VoiceConversationLoop.StopReason;
import static salve.core.voice.VoiceTurnMetrics.CaptureStage;

/** Latencies are software callback timings, with missing observations kept explicit. */
public class VoiceTurnMetricsTest {
    @Test
    public void completeTurnTracksCapturePhasesAndMeasuredLatencies() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        assertTrue(metrics.begin(1L, 1_000L));
        assertEquals(CaptureStage.STARTING, metrics.captureStage());
        assertTrue(metrics.ready(1L, 1_050L));
        assertEquals(CaptureStage.READY, metrics.captureStage());
        assertTrue(metrics.speechBegan(1L, 1_100L));
        assertEquals(CaptureStage.SPEAKING, metrics.captureStage());
        assertTrue(metrics.speechEnded(1L, 1_900L));
        assertEquals(CaptureStage.FINALIZING, metrics.captureStage());
        assertTrue(metrics.recognized(1L, 2_000L));
        assertEquals(CaptureStage.FINISHED, metrics.captureStage());
        assertTrue(metrics.isActive());
        assertTrue(metrics.submitted(1L, 2_010L));
        assertTrue(metrics.replied(1L, 2_500L));
        assertTrue(metrics.speechStarted(1L, 2_550L));
        assertTrue(metrics.speechFinished(1L, 3_050L));
        assertTrue(metrics.complete(1L, 3_060L));
        assertFalse(metrics.isActive());

        assertEquals("voice_turn token=1 result=COMPLETED stop_reason=none asr_error=none"
                + " asr_ready_ms=50 asr_finalize_ms=100 response_wait_ms=490"
                + " reply_to_tts_start_ms=50 submit_to_tts_start_ms=540"
                + " tts_duration_ms=500 turn_total_ms=2060", metrics.takeLogLine());
    }

    @Test
    public void oneLogIsAvailableOnlyAfterTerminalEvent() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        assertNull(metrics.takeLogLine());
        assertTrue(metrics.begin(1L, 0L));
        assertNull(metrics.takeLogLine());
        metrics.recognized(1L, 100L);
        metrics.submitted(1L, 110L);
        assertNull(metrics.takeLogLine());
        assertTrue(metrics.stop(1L, 200L, StopReason.AUDIO_UNAVAILABLE));
        assertNotNull(metrics.takeLogLine());
        assertNull(metrics.takeLogLine());
        assertNull(metrics.takeLogLine());
    }

    @Test
    public void tokensAndSessionClockMustAdvanceWithoutReplacingActiveAttempt() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        assertFalse(metrics.begin(0L, 0L));
        assertFalse(metrics.begin(-1L, 0L));
        assertFalse(metrics.begin(1L, -1L));
        assertTrue(metrics.begin(2L, 100L));
        assertFalse(metrics.begin(3L, 110L));
        assertEquals(2L, metrics.token());
        assertTrue(metrics.interrupt(2L, 200L));
        metrics.takeLogLine();
        assertFalse(metrics.begin(2L, 201L));
        assertFalse(metrics.begin(1L, 201L));
        assertFalse(metrics.begin(3L, 199L));
        assertTrue(metrics.begin(3L, 200L));
    }

    @Test
    public void regressingTimestampDoesNotOverwriteMeasurementsOrPoisonLaterEvents() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 100L);
        assertFalse(metrics.ready(1L, 99L));
        assertTrue(metrics.ready(1L, 150L));
        assertFalse(metrics.speechBegan(1L, 149L));
        assertTrue(metrics.speechBegan(1L, 160L));
        assertFalse(metrics.speechEnded(1L, 159L));
        assertTrue(metrics.speechEnded(1L, 200L));
        assertFalse(metrics.recognized(1L, 199L));
        assertTrue(metrics.recognized(1L, 220L));
        assertFalse(metrics.submitted(1L, 219L));
        assertTrue(metrics.submitted(1L, 230L));
        assertFalse(metrics.replied(1L, 229L));
        assertTrue(metrics.replied(1L, 300L));
        assertFalse(metrics.speechStarted(1L, 299L));
        assertTrue(metrics.speechStarted(1L, 320L));
        assertFalse(metrics.speechFinished(1L, 319L));
        assertTrue(metrics.speechFinished(1L, 400L));
        assertFalse(metrics.complete(1L, 399L));
        assertTrue(metrics.complete(1L, 410L));
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("50", log.get("asr_ready_ms"));
        assertEquals("20", log.get("asr_finalize_ms"));
        assertEquals("70", log.get("response_wait_ms"));
        assertEquals("80", log.get("tts_duration_ms"));
        assertEquals("310", log.get("turn_total_ms"));
    }

    @Test
    public void callbacksInSameClockMillisecondReportZeroRatherThanMissing() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        assertTrue(metrics.begin(1L, 0L));
        assertTrue(metrics.ready(1L, 0L));
        assertTrue(metrics.speechBegan(1L, 0L));
        assertTrue(metrics.speechEnded(1L, 0L));
        assertTrue(metrics.recognized(1L, 0L));
        assertTrue(metrics.submitted(1L, 0L));
        assertTrue(metrics.replied(1L, 0L));
        assertTrue(metrics.speechStarted(1L, 0L));
        assertTrue(metrics.speechFinished(1L, 0L));
        assertTrue(metrics.complete(1L, 0L));
        Map<String, String> log = fields(metrics.takeLogLine());
        for (Map.Entry<String, String> entry : log.entrySet()) {
            if (entry.getKey().endsWith("_ms")) assertEquals(entry.getKey(), "0", entry.getValue());
        }
    }

    @Test
    public void oldAndUnknownTokensCannotChangeCurrentAttempt() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        metrics.interrupt(1L, 100L);
        metrics.takeLogLine();
        metrics.begin(3L, 200L);
        assertFalse(metrics.ready(1L, 999L));
        assertFalse(metrics.speechBegan(1L, 999L));
        assertFalse(metrics.speechEnded(1L, 999L));
        assertFalse(metrics.recognized(1L, 999L));
        assertFalse(metrics.submitted(1L, 999L));
        assertFalse(metrics.replied(1L, 999L));
        assertFalse(metrics.speechStarted(1L, 999L));
        assertFalse(metrics.speechFinished(1L, 999L));
        assertFalse(metrics.recognitionFailed(1L, 999L, 9));
        assertFalse(metrics.stop(1L, 999L, StopReason.LIFECYCLE));
        assertFalse(metrics.complete(1L, 999L));
        assertFalse(metrics.interrupt(1L, 999L));
        assertFalse(metrics.ready(4L, 999L));
        assertEquals(CaptureStage.STARTING, metrics.captureStage());
        assertTrue(metrics.ready(3L, 210L));
        assertTrue(metrics.stop(3L, 220L, StopReason.USER));
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("3", log.get("token"));
        assertEquals("10", log.get("asr_ready_ms"));
        assertEquals("20", log.get("turn_total_ms"));
    }

    @Test
    public void duplicateCallbacksCannotOverwriteFirstObservations() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        assertTrue(metrics.ready(1L, 10L));
        assertFalse(metrics.ready(1L, 11L));
        assertTrue(metrics.speechBegan(1L, 20L));
        assertFalse(metrics.speechBegan(1L, 21L));
        assertTrue(metrics.speechEnded(1L, 30L));
        assertFalse(metrics.speechEnded(1L, 31L));
        assertTrue(metrics.recognized(1L, 40L));
        assertFalse(metrics.recognized(1L, 41L));
        assertTrue(metrics.submitted(1L, 50L));
        assertFalse(metrics.submitted(1L, 51L));
        assertTrue(metrics.replied(1L, 60L));
        assertFalse(metrics.replied(1L, 61L));
        assertTrue(metrics.speechStarted(1L, 70L));
        assertFalse(metrics.speechStarted(1L, 71L));
        assertTrue(metrics.speechFinished(1L, 80L));
        assertFalse(metrics.speechFinished(1L, 81L));
        assertTrue(metrics.complete(1L, 90L));
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("10", log.get("asr_ready_ms"));
        assertEquals("10", log.get("asr_finalize_ms"));
        assertEquals("10", log.get("response_wait_ms"));
        assertEquals("10", log.get("reply_to_tts_start_ms"));
        assertEquals("10", log.get("tts_duration_ms"));
    }

    @Test
    public void missingRecognizerEventsRemainMissingInsteadOfInventingZeroLatency() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        assertTrue(metrics.recognized(1L, 100L));
        assertTrue(metrics.submitted(1L, 110L));
        assertTrue(metrics.replied(1L, 150L));
        assertTrue(metrics.speechFinished(1L, 180L));
        metrics.complete(1L, 190L);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("missing", log.get("asr_ready_ms"));
        assertEquals("missing", log.get("asr_finalize_ms"));
        assertEquals("40", log.get("response_wait_ms"));
        assertEquals("missing", log.get("reply_to_tts_start_ms"));
        assertEquals("missing", log.get("submit_to_tts_start_ms"));
        assertEquals("missing", log.get("tts_duration_ms"));
    }

    @Test
    public void ttsStartBeforeReplyKeepsSubmitLatencyAndMarksInvertedPairMissing() {
        VoiceTurnMetrics metrics = submittedTurn();
        assertTrue(metrics.speechStarted(1L, 200L));
        assertTrue(metrics.replied(1L, 250L));
        assertTrue(metrics.speechFinished(1L, 400L));
        metrics.complete(1L, 410L);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("140", log.get("response_wait_ms"));
        assertEquals("missing", log.get("reply_to_tts_start_ms"));
        assertEquals("90", log.get("submit_to_tts_start_ms"));
        assertEquals("200", log.get("tts_duration_ms"));
    }

    @Test
    public void missingReplyCallbackDoesNotHideObservedTimeUntilTtsStarts() {
        VoiceTurnMetrics metrics = submittedTurn();
        metrics.speechStarted(1L, 200L);
        metrics.speechFinished(1L, 400L);
        metrics.complete(1L, 410L);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("missing", log.get("response_wait_ms"));
        assertEquals("missing", log.get("reply_to_tts_start_ms"));
        assertEquals("90", log.get("submit_to_tts_start_ms"));
        assertEquals("200", log.get("tts_duration_ms"));
    }

    @Test
    public void responseEventsBeforeSubmissionAreRejectedWithoutSettingTimestamps() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        assertFalse(metrics.submitted(1L, 10L));
        assertFalse(metrics.replied(1L, 20L));
        assertFalse(metrics.speechStarted(1L, 30L));
        assertFalse(metrics.speechFinished(1L, 40L));
        assertTrue(metrics.recognized(1L, 50L));
        assertFalse(metrics.replied(1L, 60L));
        assertFalse(metrics.speechStarted(1L, 70L));
        assertTrue(metrics.submitted(1L, 80L));
        assertTrue(metrics.speechStarted(1L, 90L));
        metrics.complete(1L, 100L);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("missing", log.get("response_wait_ms"));
        assertEquals("10", log.get("submit_to_tts_start_ms"));
        assertEquals("missing", log.get("tts_duration_ms"));
    }

    @Test
    public void lateReadyDoesNotRegressSpeakingOrFinalizedCapture() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        assertTrue(metrics.speechBegan(1L, 10L));
        assertTrue(metrics.ready(1L, 20L));
        assertEquals(CaptureStage.SPEAKING, metrics.captureStage());
        metrics.speechEnded(1L, 30L);
        assertFalse(metrics.speechBegan(1L, 40L));
        assertFalse(metrics.ready(1L, 40L));
        assertEquals(CaptureStage.FINALIZING, metrics.captureStage());
        metrics.recognized(1L, 50L);
        assertFalse(metrics.speechEnded(1L, 60L));
        assertFalse(metrics.speechBegan(1L, 60L));
        assertEquals(CaptureStage.FINISHED, metrics.captureStage());
    }

    @Test
    public void recognitionErrorKeepsNumericCodeAndRejectsCompetingTerminalEvents() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        metrics.ready(1L, 10L);
        assertTrue(metrics.recognitionFailed(1L, 50L, 7));
        assertFalse(metrics.recognitionFailed(1L, 60L, 9));
        assertFalse(metrics.stop(1L, 60L, StopReason.MICROPHONE_PERMISSION));
        assertFalse(metrics.interrupt(1L, 60L));
        assertFalse(metrics.complete(1L, 60L));
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("RECOGNITION_ERROR", log.get("result"));
        assertEquals("7", log.get("asr_error"));
        assertEquals("none", log.get("stop_reason"));
        assertEquals("50", log.get("turn_total_ms"));
        assertEquals("missing", log.get("asr_finalize_ms"));
        assertEquals("missing", log.get("response_wait_ms"));
    }

    @Test
    public void watchdogStopKeepsReasonWithoutPretendingRecognitionCompleted() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        metrics.ready(1L, 100L);
        metrics.speechBegan(1L, 200L);
        metrics.speechEnded(1L, 1_000L);
        assertTrue(metrics.stop(1L, 60_000L, StopReason.NO_SPEECH));
        assertEquals(CaptureStage.FINISHED, metrics.captureStage());
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("STOPPED", log.get("result"));
        assertEquals("NO_SPEECH", log.get("stop_reason"));
        assertEquals("none", log.get("asr_error"));
        assertEquals("missing", log.get("asr_finalize_ms"));
        assertEquals("60000", log.get("turn_total_ms"));
    }

    @Test
    public void interruptionAndNewCaptureDoNotReusePreviousTurnObservations() {
        VoiceTurnMetrics metrics = submittedTurn();
        metrics.replied(1L, 200L);
        metrics.speechStarted(1L, 220L);
        assertTrue(metrics.interrupt(1L, 250L));
        Map<String, String> first = fields(metrics.takeLogLine());
        assertEquals("INTERRUPTED", first.get("result"));
        assertEquals("missing", first.get("tts_duration_ms"));

        assertTrue(metrics.begin(3L, 550L));
        assertEquals(CaptureStage.STARTING, metrics.captureStage());
        assertNull(metrics.takeLogLine());
        assertTrue(metrics.stop(3L, 600L, StopReason.LIFECYCLE));
        Map<String, String> second = fields(metrics.takeLogLine());
        assertEquals("3", second.get("token"));
        assertEquals("STOPPED", second.get("result"));
        assertEquals("LIFECYCLE", second.get("stop_reason"));
        assertEquals("none", second.get("asr_error"));
        assertEquals("50", second.get("turn_total_ms"));
        for (Map.Entry<String, String> entry : second.entrySet()) {
            if (entry.getKey().endsWith("_ms") && !entry.getKey().equals("turn_total_ms")) {
                assertEquals(entry.getKey(), "missing", entry.getValue());
            }
        }
    }

    @Test
    public void retryAfterRecognitionErrorClearsFailureMetadataAndReportsAgain() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        metrics.recognitionFailed(1L, 100L, 2);
        assertEquals("2", fields(metrics.takeLogLine()).get("asr_error"));
        assertTrue(metrics.begin(2L, 1_600L));
        metrics.recognized(2L, 1_700L);
        metrics.submitted(2L, 1_710L);
        metrics.complete(2L, 1_800L);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("COMPLETED", log.get("result"));
        assertEquals("none", log.get("asr_error"));
        assertEquals("none", log.get("stop_reason"));
        assertEquals("200", log.get("turn_total_ms"));
        assertNull(metrics.takeLogLine());
    }

    @Test
    public void terminalAttemptRejectsCallbacksEvenBeforeLogIsTaken() {
        VoiceTurnMetrics metrics = submittedTurn();
        assertTrue(metrics.stop(1L, 200L, StopReason.RESPONSE_TIMEOUT));
        assertFalse(metrics.ready(1L, 210L));
        assertFalse(metrics.speechBegan(1L, 210L));
        assertFalse(metrics.speechEnded(1L, 210L));
        assertFalse(metrics.recognized(1L, 210L));
        assertFalse(metrics.submitted(1L, 210L));
        assertFalse(metrics.replied(1L, 210L));
        assertFalse(metrics.speechStarted(1L, 210L));
        assertFalse(metrics.speechFinished(1L, 210L));
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals("RESPONSE_TIMEOUT", log.get("stop_reason"));
        assertEquals("missing", log.get("response_wait_ms"));
        assertEquals("missing", log.get("submit_to_tts_start_ms"));
    }

    @Test
    public void privacyBoundaryAcceptsOnlyNumbersAndFixedEnumsForTelemetry() {
        // Protect the boundary: transcripts, replies, and arbitrary labels must never enter it.
        for (Method method : VoiceTurnMetrics.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            for (Class<?> parameter : method.getParameterTypes()) {
                assertTrue(method.getName(), parameter == long.class || parameter == int.class
                        || parameter == StopReason.class);
            }
        }
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        metrics.begin(1L, 0L);
        metrics.stop(1L, 100L, StopReason.USER);
        Map<String, String> log = fields(metrics.takeLogLine());
        assertEquals(11, log.size());
        for (Map.Entry<String, String> entry : log.entrySet()) {
            assertTrue(entry.getKey(), entry.getValue().matches("[A-Z_]+|[0-9]+|none|missing"));
        }
    }

    private static VoiceTurnMetrics submittedTurn() {
        VoiceTurnMetrics metrics = new VoiceTurnMetrics();
        assertTrue(metrics.begin(1L, 0L));
        assertTrue(metrics.recognized(1L, 100L));
        assertTrue(metrics.submitted(1L, 110L));
        return metrics;
    }

    private static Map<String, String> fields(String line) {
        assertNotNull(line);
        assertTrue(line, line.startsWith("voice_turn "));
        assertFalse(line.contains("\n"));
        Map<String, String> fields = new LinkedHashMap<>();
        for (String field : line.substring("voice_turn ".length()).split(" ")) {
            String[] pair = field.split("=", -1);
            assertEquals(field, 2, pair.length);
            assertNull(field, fields.put(pair[0], pair[1]));
        }
        return fields;
    }
}
