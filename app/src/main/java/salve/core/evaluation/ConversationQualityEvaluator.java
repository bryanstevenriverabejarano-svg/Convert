package salve.core.evaluation;

import java.util.ArrayList;
import java.util.List;

/** Evaluación determinista; nunca recibe ni genera cadenas de pensamiento. */
public final class ConversationQualityEvaluator {
    private ConversationQualityEvaluator() {}

    public static TurnQualityAssessment evaluate(String response,
                                                 boolean fallbackUsed,
                                                 boolean truncated,
                                                 boolean roleLeakDetected,
                                                 boolean repetitionDetected,
                                                 long latencyMillis) {
        List<String> issues = new ArrayList<>();
        int score = 100;
        int length = response == null ? 0 : response.trim().length();

        if (length == 0) {
            issues.add("empty");
            score = 0;
        }
        if (fallbackUsed) {
            issues.add("fallback");
            score -= 20;
        }
        if (truncated) {
            issues.add("truncated");
            score -= 10;
        }
        if (roleLeakDetected) {
            issues.add("role_leak");
            score -= 35;
        }
        if (repetitionDetected) {
            issues.add("repetition");
            score -= 40;
        }
        if (latencyMillis > 8_000L) {
            issues.add("high_latency");
            score -= 10;
        }

        return new TurnQualityAssessment(Math.max(0, score), length,
                Math.max(0L, latencyMillis), issues);
    }
}
