package salve.core.conversation;

import static org.junit.Assert.*;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import salve.core.cognition.CognitiveTurn;

public class TurnObjectiveTest {
    @Test public void understandsAndReusesExistingAnalysisAndPlanner() {
        TurnObjective frame = TurnObjective.understand("Explica como funciona mi memoria", true, false);
        assertEquals(ConversationAct.EXPLANATION_REQUEST, frame.act());
        assertTrue(frame.plan().shouldRetrieveLongTermMemory());
        assertTrue(frame.plan().isVerificationRequired());
        assertEquals("ACTO_CONVERSACIONAL: EXPLANATION_REQUEST | " + frame.plan().toPromptContext(),
                frame.toPromptContext());
    }

    @Test public void anOrphanTurnCannotInventThePreviousObjective() {
        assertTrue(TurnObjective.understand("hazlo", false, false).needsClarification());
        assertFalse(TurnObjective.understand("hazlo", true, false).needsClarification());
    }

    @Test public void prioritizationNeverGrantsAnUnauthorizedCapability() {
        TurnObjective frame = TurnObjective.understand(
                "Revisa tu codigo sobre memoria y modelo", true, false);
        Map<String, CognitiveTurn.Tool> authorized = new LinkedHashMap<>();
        authorized.put("web.search", new CognitiveTurn.Tool("buscar", args -> new CognitiveTurn.Receipt("ok")));
        authorized.put("memory.lookup", new CognitiveTurn.Tool("memoria", args -> new CognitiveTurn.Receipt("ok")));
        authorized.put("self.code", new CognitiveTurn.Tool("indice", args -> new CognitiveTurn.Receipt("ok")));
        Map<String, CognitiveTurn.Tool> ordered = frame.rankAuthorized(authorized);
        assertEquals("self.code", ordered.keySet().iterator().next());
        assertEquals(new java.util.HashSet<>(authorized.keySet()), new java.util.HashSet<>(ordered.keySet()));
        assertSame(authorized.get("self.code"), ordered.get("self.code"));

        authorized.remove("self.code");
        assertFalse(frame.rankAuthorized(authorized).containsKey("self.code"));
    }

    @Test public void externalKnowledgeIsOnlyRequestedWhenTheControllerSaysSo() {
        assertFalse(TurnObjective.understand("Hazlo", true, false).plan().isExternalKnowledgeRequested());
        assertTrue(TurnObjective.understand("Busca eso", true, true).plan().isExternalKnowledgeRequested());
    }
}
