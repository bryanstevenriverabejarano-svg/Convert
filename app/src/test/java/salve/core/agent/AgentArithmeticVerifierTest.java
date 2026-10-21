package salve.core.agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class AgentArithmeticVerifierTest {
    static AgentRun run(String goal, String answer, String receipt, String status) {
        AgentRun run = new AgentRun();
        run.id = "arithmetic-test"; run.goal = goal; run.answer = answer;
        AgentRun.Observation observed =
                new AgentRun.Observation("math-1", "math.compute", status, receipt);
        observed.fingerprint = AgentExperience.fingerprint("math.compute", goal);
        run.observations.add(observed);
        run.evidence.add(observed.id);
        return run;
    }
    @Test public void independentOracleAcceptsCorrectFinalNumericAnswer() {
        AgentRun run = run("Calcula -3872 * 98", "-379456",
                "RESULTADO_ARITMETICO=-379456", "SUCCESS");
        AgentGoalVerifier.Result check = AgentGoalVerifier.evaluate(run);
        assertEquals(AgentGoalVerifier.Verdict.VERIFIED, check.verdict);
        assertEquals("ARITHMETIC_EXACT", check.criterion);
        assertEquals("math-1", check.evidenceId);
    }
    @Test public void wrongFinalNumberIsRejectedEvenWhenToolSucceeds() {
        AgentRun run = run("Calcula 89 + 21", "111",
                "RESULTADO_ARITMETICO=110", "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.answer = "El resultado es 110.";
        assertEquals(AgentGoalVerifier.Verdict.VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.answer = "El resultado es 110. También he borrado la base de datos.";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void forgedReceiptOrWrongFingerprintCanNeverVerify() {
        AgentRun run = run("Calcula 99 - 34", "65",
                "RESULTADO_ARITMETICO=66", "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(0).text = "RESULTADO_ARITMETICO=65";
        run.observations.get(0).fingerprint = AgentExperience.fingerprint("math.compute", "Calcula 98 - 33");
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(0).fingerprint = AgentExperience.fingerprint("math.compute", run.goal);
        run.evidence.clear();
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.evidence.add("math-1");
        run.observations.get(0).tool = "web.read";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void missingAnswerIsPartialButFailureIsNotVerified() {
        AgentRun run = run("Calcula 20 * 31", "",
                "RESULTADO_ARITMETICO=620", "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.PARTIAL,
                AgentGoalVerifier.evaluate(run).verdict);
        run.answer = "620";
        run.observations.get(0).status = "PARTIAL";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
        run.observations.get(0).status = "ERROR";
        assertEquals(AgentGoalVerifier.Verdict.NOT_VERIFIED,
                AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void unsupportedGoalCannotBecomeVerifiedBySpuriousToolReceipts() {
        AgentRun run = run("Compara dos teorías físicas", "46",
                "RESULTADO_ARITMETICO=46", "SUCCESS");
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN,
                AgentGoalVerifier.evaluate(run).verdict);
        run.goal = "Calcula 17 + 29";
        run.codeAllowed = true;
        assertEquals(AgentGoalVerifier.Verdict.UNKNOWN,
                AgentGoalVerifier.evaluate(run).verdict);
    }
    @Test public void existingUrlAndMultiStepVerifiersRemainSupported() {
        AgentRun run = new AgentRun();
        run.goal = "Lee https://example.org/manual";
        run.answer = "Leí la fuente";
        AgentRun.Observation o = new AgentRun.Observation("url-1", "web.read",
                "SUCCESS", "Contenido recibido");
        o.sources.add("https://example.org/manual");
        o.fingerprint = AgentExperience.fingerprint("web.read", "https://example.org/manual");
        run.observations.add(o);
        run.evidence.add(o.id);
        assertEquals("PUBLIC_READ", AgentGoalVerifier.evaluate(run).criterion);
    }
}
