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

    @Test public void supervisorNeverCallsModelOrExternalTools() {
        AgentRun r = run("RUNNING", "UNKNOWN");
        r.pending = new AgentRun.Pending("call", "web.search", "pending");
        AgentProgressSupervisor.inspect(r);
        assertEquals("web.search", r.pending.tool);
        assertEquals("pending", r.pending.input);
    }
}
