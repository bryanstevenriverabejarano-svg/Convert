package salve.core.agent;

import org.junit.Test;
import static org.junit.Assert.*;

/** Recovery is a limited experiment within an explicitly authorized goal. */
public class AgentExperimentOpportunityTest {
    private static final String URL = "https://example.org/study";
    private static AgentRun failedTwice(String goal) {
        AgentRun run = new AgentRun();
        run.id = "exp"; run.goal = goal;
        for (int i = 0; i < 2; i++) {
            AgentRun.Observation failure =
                    new AgentRun.Observation("failure-" + i, "web.search", "ERROR", "Sin red");
            failure.fingerprint = AgentExperience.fingerprint("web.search", goal);
            run.observations.add(failure);
        }
        return run;
    }
    private static AgentRun.Pending repeat(AgentRun run) {
        return new AgentRun.Pending("repeat", "web.search", run.goal);
    }

    @Test public void discoversObstacleAndCanProposeOneAuthorizedRead() {
        AgentRun r = failedTwice("Investiga " + URL);
        AgentExperimentOpportunity option = AgentExperimentOpportunity.afterRepeat(r, repeat(r));
        assertNotNull(option);
        assertEquals("web.read", option.tool);
        assertEquals(URL, option.input);
        assertNull(AgentPolicy.denial(r, option.tool, option.input));
        assertTrue(option.reason.contains("dos búsquedas"));
        assertFalse(option.reason.contains("Éxito del objetivo"));
    }

    @Test public void requiresTwoActualFailuresWithMatchingFingerprints() {
        AgentRun r = failedTwice("Consulta " + URL);
        r.observations.remove(1);
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
        r.observations.add(new AgentRun.Observation("forged", "web.search", "ERROR", "falso"));
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
    }

    @Test public void neverExperimentsWithAmbiguousPrivateOrAbsentDestinations() {
        for (String goal : new String[]{
                "Búsqueda de ciencia",
                "Busca http://example.org/unencrypted",
                "Busca https://127.0.0.1/admin",
                "Busca https://localhost/admin",
                "Investiga " + URL + " y https://example.com/second"
        }) {
            AgentRun r = failedTwice(goal);
            assertNull(goal, AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
        }
    }

    @Test public void doesNotWidenRemoteCodeAuthorityOrRepeatAnyRead() {
        AgentRun r = failedTwice("Investiga " + URL);
        r.codeAllowed = true;
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
        r.codeAllowed = false;
        AgentRun.Observation tried = new AgentRun.Observation("read", "web.read", "ERROR", "Sin datos");
        tried.fingerprint = AgentExperience.fingerprint("web.read", URL);
        r.observations.add(tried);
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
    }

    @Test public void neverRetriesBlockedOrPreviouslySuccessfulSearch() {
        AgentRun r = failedTwice("Busca " + URL);
        AgentRun.Observation previous = new AgentRun.Observation("prior", "web.search", "SUCCESS", "Un resultado");
        previous.fingerprint = AgentExperience.fingerprint("web.search", r.goal);
        r.observations.add(previous);
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
        r.observations.remove(previous);
        previous.status = "BLOCKED";
        r.observations.add(previous);
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
    }

    @Test public void respectsRemainingBudgets() {
        AgentRun r = failedTwice("Consulta " + URL);
        r.modelCalls = AgentEngine.MAX_MODEL_CALLS;
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
        r.modelCalls = 0;
        while (r.observations.size() < AgentEngine.MAX_STEPS)
            r.observations.add(new AgentRun.Observation("x", "web.read", "ERROR", "Sin datos"));
        assertNull(AgentExperimentOpportunity.afterRepeat(r, repeat(r)));
    }
}
