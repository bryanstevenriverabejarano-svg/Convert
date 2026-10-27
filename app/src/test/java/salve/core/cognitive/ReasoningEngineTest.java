package salve.core.cognitive;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class ReasoningEngineTest {
    @Test public void predictionsAreUniqueAndKeepTheStrongestPath() {
        ReasoningEngine e = new ReasoningEngine();
        e.registerCausalLink("a", "b", 0.9f, ReasoningEngine.LinkType.LOGICAL);
        e.registerCausalLink("a", "c", 0.5f, ReasoningEngine.LinkType.LOGICAL);
        e.registerCausalLink("b", "c", 0.9f, ReasoningEngine.LinkType.LOGICAL);
        List<ReasoningEngine.Prediction> p = e.predict("a", 3);
        assertEquals(2, p.size());
        assertEquals("b", p.get(0).concept);
        assertEquals("c", p.get(1).concept);
        assertEquals(0.81f, p.get(1).probability, 1e-4);
    }

    @Test public void hypothesesCombineCausesWithNoisyOrAndNeverExceedOne() {
        ReasoningEngine e = new ReasoningEngine();
        e.registerCausalLink("estres", "insomnio", 0.8f, ReasoningEngine.LinkType.LOGICAL);
        e.registerCausalLink("cafe", "insomnio", 0.8f, ReasoningEngine.LinkType.LOGICAL);
        ReasoningEngine.Hypothesis h = e.generateHypothesis(Arrays.asList("estres", "cafe"));
        assertEquals("insomnio", h.conclusion);
        assertEquals(0.96f, h.confidence, 1e-4);
    }

    @Test public void explainWalksBackwardsThroughChains() {
        ReasoningEngine e = new ReasoningEngine();
        e.registerCausalLink("estres", "cafe", 0.8f, ReasoningEngine.LinkType.LOGICAL);
        e.registerCausalLink("cafe", "insomnio", 0.8f, ReasoningEngine.LinkType.LOGICAL);
        List<ReasoningEngine.Prediction> why = e.explain("insomnio", 3);
        assertEquals("cafe", why.get(0).concept);
        assertEquals(1, why.get(0).chainLength);
        assertEquals("estres", why.get(1).concept);
        assertEquals(2, why.get(1).chainLength);
        assertTrue(e.explain("nada", 3).isEmpty());
    }
}
