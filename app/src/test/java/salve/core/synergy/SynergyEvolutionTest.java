package salve.core.synergy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class SynergyEvolutionTest {
    private JsonObject report(String... failures) {
        JsonObject exam=new JsonObject(); JsonArray cases=new JsonArray();
        for(String issue:failures) {
            JsonObject c=new JsonObject(); JsonArray list=new JsonArray();
            list.add(issue+": respuesta incorrecta"); c.add("failed",list);
            cases.add(c);
        }
        exam.add("cases",cases); return exam;
    }
    @Test public void repeatedExamFailuresYieldOnlyAnAuditableProposal() {
        JsonObject candidate=SynergyEvolution.candidate(report("no_repite","no_repite","usa"));
        assertNotNull(candidate);
        assertEquals("PROPOSAL_ONLY",candidate.get("stage").getAsString());
        assertEquals("no_repite",candidate.get("issue_id").getAsString());
        assertEquals(2,candidate.get("observed_failures").getAsInt());
        assertTrue(candidate.get("sandbox_and_user_review_required").getAsBoolean());
        assertFalse(candidate.get("signed_update_automatic").getAsBoolean());
        assertFalse(candidate.has("patch"));
        assertFalse(candidate.has("code"));
    }
    @Test public void noEvidenceOrUnknownKeyCannotTriggerCodeProposal() {
        assertNull(SynergyEvolution.candidate(null));
        assertNull(SynergyEvolution.candidate(report("usa")));
        assertNull(SynergyEvolution.candidate(report("delete_identity","delete_identity")));
    }
}