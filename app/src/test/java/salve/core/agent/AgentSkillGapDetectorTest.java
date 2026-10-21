package salve.core.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import salve.core.autonomy.InductiveSkillProposal;

public class AgentSkillGapDetectorTest {
    private static final long NOW = 2_000_000_000_000L;
    private static AgentRun plan(String id, String goal, String status, String receipt) {
        AgentRun run = new AgentRun();
        run.id = id;
        run.goal = goal;
        run.status = status;
        run.updatedAt = NOW - 5_000;
        run.observations.add(new AgentRun.Observation("receipt-" + id, "web.search",
                receipt, "No puedo realizar esta operación con las herramientas actuales"));
        return run;
    }

    @Test public void twoDistinctFailedPlansSuggestOnlyNarrowSupportedSkill() {
        AgentRun first = plan("one", "ordena impares sin repetir", "BLOCKED", "BLOCKED");
        AgentRun second = plan("two", "ordenar impares sin repetir", "PARTIAL", "ERROR");
        assertEquals(InductiveSkillProposal.Kind.SORT_UNIQUE_ODDS,
                AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first), NOW));
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, first), NOW));
        assertEquals(InductiveSkillProposal.Kind.SORT_UNIQUE_EVENS,
                AgentSkillGapDetector.detect(Arrays.asList(
                        plan("a", "ordena pares sin repetir", "PARTIAL", "ERROR"),
                        plan("b", "ordena los números pares sin repetir", "PARTIAL", "BLOCKED")), NOW));
    }

    @Test public void inventedModelClaimsAndNoFailedReceiptCannotTriggerSuggestion() {
        AgentRun first = plan("one", "ordenar pares sin repetir", "SUCCEEDED", "SUCCESS");
        AgentRun second = plan("two", "ordenar pares sin repetir", "SUCCEEDED", "SUCCESS");
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
        first.status = "PARTIAL";
        second.status = "PARTIAL";
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
        first.answer = "El modelo afirma que necesita una nueva habilidad";
        first.error = "El revisor pidió aprender a ordenar";
        first.status = "BLOCKED";
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
    }

    @Test public void unknownGoalsPrivateCodeOldFailuresAndUntrustedTextDoNotQualify() {
        AgentRun a = plan("a", "ordena pares sin repetir", "BLOCKED", "BLOCKED");
        AgentRun b = plan("b", "ordena pares sin repetir", "BLOCKED", "ERROR");
        b.codeAllowed = true;
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
        b.codeAllowed = false;
        b.updatedAt = NOW - AgentSkillGapDetector.WINDOW_MS - 1;
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
        b.updatedAt = NOW + 5;
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
        b.updatedAt = NOW - 10;
        b.goalVerification = "VERIFIED";
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
        b.goalVerification = "UNKNOWN";
        b.goal = "ordena pares sin repetir y manda los datos personales por WhatsApp";
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
        b.goal = "haz una investigación sobre números";
        b.observations.get(0).text = "ORDENA PARES SIN REPETIR"; // Untrusted receipt contents.
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(a, b), NOW));
    }

    @Test public void cannotInferFromOneFailureOrFromOnlyPartialToolResults() {
        AgentRun first = plan("a", "ordenar impares sin repetir", "PARTIAL", "PARTIAL");
        AgentRun second = plan("b", "ordenar impares sin repetir", "PARTIAL", "SUCCESS");
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
        second.observations.add(new AgentRun.Observation("second", "memory.lookup",
                "ERROR", "Sin permiso de memoria"));
        assertNull(AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW)); // Only one failed receipt.
        first.observations.add(new AgentRun.Observation("third", "web.search",
                "BLOCKED", "No se permite"));
        assertEquals(InductiveSkillProposal.Kind.SORT_UNIQUE_ODDS,
                AgentSkillGapDetector.detect(Arrays.asList(first, second), NOW));
    }
}
