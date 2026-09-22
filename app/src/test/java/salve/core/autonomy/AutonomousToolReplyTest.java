package salve.core.autonomy;

import com.google.gson.JsonObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class AutonomousToolReplyTest {
    @Test public void verifiedReportIncludesResultAndEvidenceCounts() {
        JsonObject report = AutonomousToolCommand.parseObject("{\"status\":\"verified\",\"verified\":true,"
                + "\"promoted\":true,\"adapted\":true,\"result\":{\"value\":9},"
                + "\"attempts\":[{\"strategy\":\"greedy\",\"passed\":false},{\"strategy\":\"exact\",\"passed\":true}]}");
        String text = AutonomousToolReply.summarize(report);
        assertTrue(text.contains("Resultado verificado"));
        assertTrue(text.contains("\"value\":9"));
        assertTrue(text.contains("Estrategia: exact"));
        assertTrue(text.contains("Intentos: 2"));
        assertTrue(text.contains("quedó registrada"));
    }

    @Test public void unverifiedCandidateNeverLeaksAsAnswer() {
        for (String status : new String[] {"unverified", "cancelled", "budget_exhausted", "storage_error"}) {
            JsonObject report = AutonomousToolCommand.parseObject("{\"status\":\"" + status
                    + "\",\"verified\":false,\"result\":{\"candidate\":\"DO_NOT_PUBLISH\"}}");
            String text = AutonomousToolReply.summarize(report);
            assertFalse(text.contains("DO_NOT_PUBLISH"));
            assertFalse(text.startsWith("Resultado verificado"));
        }
    }

    @Test public void booleanAndStatusMustBothConfirmVerification() {
        for (String json : new String[] {
                "{\"status\":\"verified\",\"verified\":\"true\",\"result\":{\"value\":8}}",
                "{\"status\":\"cancelled\",\"verified\":true,\"result\":{\"value\":8}}",
                "{\"status\":\"verified\",\"verified\":true,\"result\":null}"}) {
            assertFalse(AutonomousToolReply.summarize(AutonomousToolCommand.parseObject(json)).contains("\"value\":8"));
        }
        assertFalse(AutonomousToolReply.summarize(null).startsWith("Resultado verificado"));
    }

    @Test public void storageFailureDoesNotDiscardAnIndependentlyVerifiedCalculation() {
        JsonObject report = AutonomousToolCommand.parseObject("{\"status\":\"storage_error\",\"verified\":true,"
                + "\"promoted\":false,\"result\":{\"value\":9},\"attempts\":[{\"strategy\":\"exact\",\"passed\":true}]}");
        String text = AutonomousToolReply.summarize(report);
        assertTrue(text, text.contains("Resultado verificado"));
        assertTrue(text, text.contains("\"value\":9"));
        assertTrue(text, text.contains("No pude guardar la herramienta"));
        assertFalse(text, text.contains("quedó registrada"));
    }
}
