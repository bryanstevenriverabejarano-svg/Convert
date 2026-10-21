package salve.core.agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class AgentGoalVerifierTest {
    static final String URL = "https://example.org/manual";
    private AgentRun base(String goal, String url, String status) {
        AgentRun run = new AgentRun(); run.id = "plan"; run.goal = goal;
        run.answer = "He podido leer la página indicada.";
        AgentRun.Observation o = new AgentRun.Observation("read-1", "web.read", status, "Contenido externo");
        o.fingerprint = AgentExperience.fingerprint("web.read", URL);
        o.sources.add(url); run.observations.add(o);
        run.evidence.add(o.id);
        return run;
    }
    @Test public void explicitReadOnlyGoalIsVerifiableFromIndependentReceipt() {
        AgentRun r = base("Lee " + URL, URL, "SUCCESS");
        AgentGoalVerifier.Result result = AgentGoalVerifier.evaluate(r);
        assertEquals(AgentGoalVerifier.Verdict.VERIFIED, result.verdict);
        assertEquals("PUBLIC_READ", result.criterion);
        assertEquals("read-1", result.evidenceId);
    }
    @Test public void longerUserFormulationsAreRecognizedBeforeGenericPrefixes() {
        for (String goal : new String[]{
                "Lee la página " + URL,
                "Abre la página " + URL,
                "Consulta la URL " + URL,
                "Lee la URL " + URL
        }) {
            assertEquals(goal, AgentGoalVerifier.Verdict.VERIFIED,
                    AgentGoalVerifier.evaluate(base(goal, URL, "SUCCESS")).verdict);
        }
    }

    @Test public void modelReviewedResearchIsNotIndependentlyProven() {
        AgentRun r = base("Investiga y compara los cambios en " + URL, URL, "SUCCESS");
        r.status = "SUCCEEDED";
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(r).verdict);
        r.goal = "Resume " + URL;
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(r).verdict);
    }
    @Test public void missingOrForgedEvidenceNeverBecomesVerified() {
        AgentRun r = base("Consulta " + URL, URL, "SUCCESS");
        r.evidence.clear();
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(r).verdict);
        r.evidence.add("read-1"); r.observations.get(0).sources.clear();
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(r).verdict);
        r.observations.get(0).sources.add(URL);
        r.observations.get(0).fingerprint = AgentExperience.fingerprint("web.read", URL + "/other");
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(r).verdict);
        r.observations.get(0).fingerprint = AgentExperience.fingerprint("web.read", URL);
        r.observations.get(0).text = "   ";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(r).verdict);
        r.observations.get(0).text = "Contenido externo";
        r.answer = "";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(r).verdict);
    }
    @Test public void partialOrFailedReadNeverBecomesComplete() {
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL,
                AgentGoalVerifier.evaluate(base("Abre " + URL, URL, "PARTIAL")).verdict);
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(base("Abre " + URL, URL, "ERROR")).verdict);
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(base("Abre " + URL, URL, "BLOCKED")).verdict);
    }
    @Test public void privateAddressAndCodeRunsCannotSelfVerify() {
        AgentRun r = base("Consulta https://127.0.0.1/private", URL, "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(r).verdict);
        r.goal = "Lee " + URL; r.codeAllowed = true;
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(r).verdict);
    }
    @Test public void independentMetadataValidatesAndSurvivesJournal() {
        AgentRun r = base("Consulta " + URL, URL, "SUCCESS");
        AgentGoalVerifier.Result checked = AgentGoalVerifier.evaluate(r);
        r.goalVerification = checked.verdict.name();
        r.verificationCriterion = checked.criterion;
        r.verificationEvidence = checked.evidenceId;
        assertEquals("VERIFIED", AgentRun.decode(r.encode()).goalVerification);
        assertEquals("read-1", AgentRun.decode(r.encode()).verificationEvidence);
        r.goalVerification = "VERIFIED"; r.verificationEvidence = "";
        assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(r.encode()));
        r.goalVerification = "UNKNOWN"; r.verificationCriterion = "NONE";
        assertEquals("UNKNOWN", AgentRun.decode(r.encode()).goalVerification);
    }
}