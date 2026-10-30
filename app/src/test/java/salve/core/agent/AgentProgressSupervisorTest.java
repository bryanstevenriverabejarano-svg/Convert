package salve.core.agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class AgentProgressSupervisorTest {
    private static AgentRun run(String status, String verdict) {
        AgentRun value = new AgentRun();
        value.id = "sample-goal";
        value.goal = "Objetivo original autorizado";
        value.status = status;
        value.goalVerification = verdict;
        return value;
    }

    @Test public void modelSelfReviewIsNotIndependentProof() {
        assertEquals(AgentProgressSupervisor.Phase.UNVERIFIED,
                AgentProgressSupervisor.inspect(run("SUCCEEDED", "UNKNOWN")).phase);
        assertEquals(AgentProgressSupervisor.Phase.VERIFIED,
                AgentProgressSupervisor.inspect(run("SUCCEEDED", "VERIFIED")).phase);
        assertEquals(AgentProgressSupervisor.Phase.UNVERIFIED,
                AgentProgressSupervisor.inspect(run("SUCCEEDED", "PARTIAL")).phase);
    }

    @Test public void detectedStallNeverSilentlyRestartsPlan() {
        AgentRun a = run("RUNNING", "UNKNOWN");
        a.skippedRepeats = 3;
        a.observations.add(new AgentRun.Observation("1", "web.search", "ERROR", "timeout"));
        a.observations.add(new AgentRun.Observation("2", "web.search", "SUCCESS", "read"));
        AgentProgressSupervisor.Finding actual = AgentProgressSupervisor.inspect(a);
        assertEquals(AgentProgressSupervisor.Phase.STALLED, actual.phase);
        assertEquals(1, actual.completedObservations);
        assertEquals(1, actual.errorObservations);
        assertEquals("RUNNING", a.status);
        assertEquals(3, a.skippedRepeats);
        assertEquals(2, a.observations.size());
    }

    @Test public void partialBlockedQueuedAndCancelledRemainHonest() {
        assertEquals(AgentProgressSupervisor.Phase.PARTIAL,
                AgentProgressSupervisor.inspect(run("PARTIAL", "UNKNOWN")).phase);
        assertEquals(AgentProgressSupervisor.Phase.NEEDS_RESOURCES,
                AgentProgressSupervisor.inspect(run("BLOCKED", "UNKNOWN")).phase);
        assertEquals(AgentProgressSupervisor.Phase.PENDING,
                AgentProgressSupervisor.inspect(run("QUEUED", "UNKNOWN")).phase);
        assertEquals(AgentProgressSupervisor.Phase.STOPPED,
                AgentProgressSupervisor.inspect(run("CANCELLED", "UNKNOWN")).phase);
        assertFalse(AgentProgressSupervisor.inspect(run("BLOCKED", "UNKNOWN"))
                .summary().contains("VERIFIED"));
    }

    private static final class GoalJournal implements salve.core.goals.GoalAutonomy.Store {
        String value;
        public String read() { return value; }
        public void write(String json) { value = json; }
    }
    private static salve.core.tasks.ResearchTask research(String status) {
        salve.core.tasks.ResearchTask task = new salve.core.tasks.ResearchTask();
        task.id = "synthetic-task";
        task.status = status;
        task.question = "Confidencial: consultar solo en las pruebas";
        task.owner = "";
        return task;
    }

    @Test public void unifiedSupervisorReusesExistingJournalsWithoutExecutingOrLeakingContent() {
        GoalJournal journal = new GoalJournal();
        salve.core.goals.GoalAutonomy goals = new salve.core.goals.GoalAutonomy(journal,
                prompt -> { throw new AssertionError("No debe llamar al modelo"); }, () -> 1_000L);
        assertNotNull(journal.value);
        assertTrue(goals.overview().configured > 0);
        goals.respond("pausa objetivo mejora");
        String unchanged = journal.value;

        AgentRun queued = run("QUEUED", "UNKNOWN");
        queued.goal = "Objetivo privado del dueño";
        AgentRun expired = run("RUNNING", "UNKNOWN");
        expired.owner = "lease-1";
        expired.leaseUntil = 90L;
        expired.observations.add(new AgentRun.Observation("obs-1", "web.search",
                "ERROR", "URL privada en recibo"));
        AgentRun verified = run("SUCCEEDED", "VERIFIED");
        AgentRun modelOnly = run("SUCCEEDED", "UNKNOWN");
        AgentRun blocked = run("BLOCKED", "UNKNOWN");

        salve.core.tasks.ResearchTask waiting = research("STAGED");
        salve.core.tasks.ResearchTask stalled = research("RUNNING");
        stalled.owner = "lease-2"; stalled.leaseUntil = 90L;
        salve.core.tasks.ResearchTask archived = research("SUCCEEDED");
        salve.core.tasks.ResearchTask partial = research("PARTIAL");

        AgentProgressSupervisor.Overview view = AgentProgressSupervisor.inspectAll(
                java.util.Arrays.asList(queued, expired, verified, modelOnly, blocked),
                java.util.Arrays.asList(waiting, stalled, archived, partial),
                goals.overview(), true, 100L);
        assertTrue(view.autonomyPaused);
        assertEquals(5, view.plansObserved);
        assertEquals(1, view.plansPending);
        assertEquals(0, view.plansExecuting);
        assertEquals(2, view.plansAttention);
        assertEquals(1, view.plansVerified);
        assertEquals(1, view.plansUnverified);
        assertEquals(4, view.researchObserved);
        assertEquals(1, view.researchPending);
        assertEquals(2, view.researchAttention);
        assertEquals(1, view.researchArchived);
        assertEquals(1, view.goalsPaused);
        assertTrue(view.summary().contains("NO verifica el objetivo"));
        assertFalse(view.summary().contains("Objetivo privado"));
        assertFalse(view.summary().contains("Confidencial"));
        assertFalse(view.summary().contains("URL privada"));
        assertEquals("RUNNING", expired.status);
        assertEquals("STAGED", waiting.status);
        assertEquals(unchanged, journal.value);
    }

    @Test public void expiredLeaseIsObservationNotPermissionToRequeue() {
        AgentRun live = run("RUNNING", "UNKNOWN");
        live.owner = "worker";
        live.leaseUntil = 500L;
        assertEquals(AgentProgressSupervisor.Phase.EXECUTING,
                AgentProgressSupervisor.inspectAt(live, 499L).phase);
        assertEquals(AgentProgressSupervisor.Phase.STALLED,
                AgentProgressSupervisor.inspectAt(live, 500L).phase);
        assertEquals("RUNNING", live.status);
        assertEquals("worker", live.owner);
    }

    @Test public void overviewDoesNotHideInaccessibleGoalStoreOrMalformedInput() {
        GoalJournal broken = new GoalJournal();
        broken.value = "{not-json";
        salve.core.goals.GoalAutonomy goals = new salve.core.goals.GoalAutonomy(broken,
                prompt -> { throw new AssertionError(); }, () -> 1_000L);
        AgentProgressSupervisor.Overview view = AgentProgressSupervisor.inspectAll(
                java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                goals.overview(), false, 1_000L);
        assertTrue(view.goalsUnavailable);
        assertTrue(view.summary().contains("diario no disponible"));
        try {
            AgentProgressSupervisor.inspectAll(null, java.util.Collections.emptyList(),
                    goals.overview(), false, 1_000L);
            fail("A missing journal must not be interpreted as zero work");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void supervisedReplanBlockedIsNotACompletedPlan() {
        AgentRun r = run("BLOCKED", "UNKNOWN");
        r.replanAttempts = AgentEngine.MAX_REPLANS;
        r.replanState = "NEEDS_REVIEW";
        AgentProgressSupervisor.Finding finding = AgentProgressSupervisor.inspect(r);
        assertEquals(AgentProgressSupervisor.Phase.NEEDS_RESOURCES, finding.phase);
        assertTrue(finding.summary().contains("Solo el dueño"));
        assertEquals("BLOCKED", r.status);
    }

    @Test public void supervisorNeverCallsModelOrExternalTools() {
        AgentRun r = run("RUNNING", "UNKNOWN");
        r.pending = new AgentRun.Pending("call", "web.search", "pending");
        AgentProgressSupervisor.inspect(r);
        assertEquals("web.search", r.pending.tool);
        assertEquals("pending", r.pending.input);
    }
}
