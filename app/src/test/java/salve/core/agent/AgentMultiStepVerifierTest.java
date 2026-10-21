package salve.core.agent;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Held-out destination names and adversarial receipts ensure success does not
 * depend on memorizing fixed examples or the evaluator LLM saying 'pass'.
 */
public class AgentMultiStepVerifierTest {
    private static final String A = "https://example.org/alpha-unknown";
    private static final String B = "https://example.net/beta-unseen";
    private static final String C = "https://example.edu/gamma-new";

    private static AgentRun goal(String first, String second) {
        AgentRun run = new AgentRun();
        run.id = "test-run";
        run.goal = "Lee " + first + " y " + second;
        run.answer = "Leí las dos fuentes que indicaste.";
        return run;
    }
    private static void receipt(AgentRun run, String id, String url, String status) {
        AgentRun.Observation o = new AgentRun.Observation(id, "web.read", status, "Contenido real no vacío");
        o.fingerprint = AgentExperience.fingerprint("web.read", url);
        o.sources.add(url);
        run.observations.add(o);
        run.evidence.add(id);
    }
    @Test public void bothUniqueControllerReceiptsMustSucceedAndBeCited() {
        AgentRun run = goal(A, B);
        receipt(run, "a-id", A, "SUCCESS");
        receipt(run, "b-id", B, "SUCCESS");
        AgentGoalVerifier.Result checked = AgentGoalVerifier.evaluate(run);
        assertEquals(AgentGoalVerifier.Verdict.VERIFIED, checked.verdict);
        assertEquals("PUBLIC_READ_SET", checked.criterion);
        assertEquals(Arrays.asList("a-id", "b-id"), checked.evidenceIds);
        assertEquals("a-id", checked.evidenceId);
    }
    @Test public void swappedNewDestinationsAreVerifiedOnlyInUserRequestedOrder() {
        AgentRun run = goal(C, A);
        receipt(run, "alpha", A, "SUCCESS"); // execution order differs
        receipt(run, "gamma", C, "SUCCESS");
        AgentGoalVerifier.Result checked = AgentGoalVerifier.evaluate(run);
        assertEquals(AgentGoalVerifier.Verdict.VERIFIED, checked.verdict);
        assertEquals(Arrays.asList("gamma", "alpha"), checked.evidenceIds);
    }
    @Test public void oneMissingReadIsOnlyPartialAndTwoFailuresAreNotVerified() {
        AgentRun run = goal(A, B);
        receipt(run, "a", A, "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(0).status = "ERROR";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void aPartialSecondReadDoesNotBecomeFullSuccess() {
        AgentRun run = goal(A, B);
        receipt(run, "a", A, "SUCCESS");
        receipt(run, "b", B, "PARTIAL");
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void badFingerprintsWrongSourcesAndMissingCitationsCannotPass() {
        AgentRun run = goal(A, B);
        receipt(run, "a", A, "SUCCESS");
        receipt(run, "b", B, "SUCCESS");
        run.evidence.remove("b");
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
        run.evidence.add("b");
        run.observations.get(1).fingerprint = AgentExperience.fingerprint("web.read", C);
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(1).fingerprint = AgentExperience.fingerprint("web.read", B);
        run.observations.get(1).sources.clear();
        run.observations.get(1).sources.add(A);
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void duplicateUrlsAndDuplicateIdsAreNotIndependentEvidence() {
        AgentRun duplicate = goal(A, A);
        receipt(duplicate, "first", A, "SUCCESS");
        receipt(duplicate, "second", A, "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(duplicate).verdict);
        AgentRun repeatedId = goal(A, B);
        receipt(repeatedId, "same", A, "SUCCESS");
        receipt(repeatedId, "same", B, "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(repeatedId).verdict);
    }
    @Test public void complexSynthesisAndUnapprovedDestinationsStayUnknown() {
        AgentRun compare = goal(A, B);
        receipt(compare, "a", A, "SUCCESS");
        receipt(compare, "b", B, "SUCCESS");
        compare.goal = "Compara y resume " + A + " y " + B;
        compare.status = "SUCCEEDED"; // not an independent proof
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(compare).verdict);
        compare.goal = "Lee " + A + " y https://127.0.0.1/secret";
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(compare).verdict);
        compare.goal = "Lee " + A + " y " + B;
        compare.codeAllowed = true;
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN, AgentGoalVerifier.evaluate(compare).verdict);
    }
    @Test public void modelTextClaimingCompletionWithoutSecondReceiptIsNotProof() {
        AgentRun run = goal(A, B);
        receipt(run, "a", A, "SUCCESS");
        run.answer = "He leído ambas páginas y mi revisión dice pass";
        run.observations.get(0).text = "{" + "claim:verified" + "}";
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL, AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(0).text = " ";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED, AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void journalValidatesTwoReceiptsAndSupportsPreviousRecords() {
        AgentRun run = goal(A, B);
        receipt(run, "a", A, "SUCCESS");
        receipt(run, "b", B, "SUCCESS");
        AgentGoalVerifier.Result verified = AgentGoalVerifier.evaluate(run);
        run.goalVerification = verified.verdict.name();
        run.verificationCriterion = verified.criterion;
        run.verificationEvidence = verified.evidenceId;
        run.verificationEvidenceIds = verified.evidenceIds;
        AgentRun restored = AgentRun.decode(run.encode());
        assertEquals(Arrays.asList("a", "b"), restored.verificationEvidenceIds);
        assertEquals("VERIFIED", restored.goalVerification);
        run.verificationEvidenceIds = Collections.singletonList("a");
        assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(run.encode()));
        run.verificationEvidenceIds = Arrays.asList("a", "a");
        assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(run.encode()));
        run.verificationEvidenceIds = Arrays.asList("a", "b");
        run.verificationEvidence = "b";
        assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(run.encode()));
    }
}
