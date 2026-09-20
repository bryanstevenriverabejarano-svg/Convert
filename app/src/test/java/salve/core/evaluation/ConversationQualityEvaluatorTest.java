package salve.core.evaluation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ConversationQualityEvaluatorTest {
    @Test
    public void cleanTurnPassesWithoutIssues() {
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                "Una respuesta clara.", false, false, false, false, 350L);

        assertEquals(100, assessment.getScore());
        assertTrue(assessment.passed());
        assertTrue(assessment.getIssues().isEmpty());
    }

    @Test
    public void recordsFailuresWithoutConversationText() {
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                "respuesta privada", true, true, true, true, 9_000L);

        assertFalse(assessment.passed());
        assertTrue(assessment.getIssues().contains("fallback"));
        assertTrue(assessment.getIssues().contains("role_leak"));
        assertTrue(assessment.getIssues().contains("repetition"));
        assertFalse(assessment.toMetricsLog().contains("respuesta privada"));
    }

    @Test
    public void emptyResponseAlwaysFails() {
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                "  ", false, false, false, false, 10L);

        assertEquals(0, assessment.getScore());
        assertFalse(assessment.passed());
    }
}
