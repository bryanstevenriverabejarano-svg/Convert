package salve.core.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Métricas no textuales de calidad para un turno conversacional. */
public final class TurnQualityAssessment {
    private final int score;
    private final int responseLength;
    private final long latencyMillis;
    private final List<String> issues;

    TurnQualityAssessment(int score, int responseLength, long latencyMillis, List<String> issues) {
        this.score = score;
        this.responseLength = responseLength;
        this.latencyMillis = latencyMillis;
        this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
    }

    public int getScore() { return score; }
    public int getResponseLength() { return responseLength; }
    public long getLatencyMillis() { return latencyMillis; }
    public List<String> getIssues() { return issues; }
    public boolean passed() { return score >= 70; }

    public String toMetricsLog() {
        return "turn_quality score=" + score
                + " length=" + responseLength
                + " latency_ms=" + latencyMillis
                + " issues=" + issuesLabel();
    }

    private String issuesLabel() {
        if (issues.isEmpty()) return "none";
        StringBuilder out = new StringBuilder();
        for (String issue : issues) {
            if (out.length() > 0) out.append(',');
            out.append(issue);
        }
        return out.toString();
    }
}
