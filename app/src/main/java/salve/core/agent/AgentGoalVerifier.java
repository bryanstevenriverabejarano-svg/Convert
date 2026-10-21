package salve.core.agent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Verifies only a narrow, user-specified, observable outcome without model judgement. */
public final class AgentGoalVerifier {
    public enum Verdict { VERIFIED, PARTIAL, NOT_VERIFIED, UNKNOWN }
    public static final class Result {
        public final Verdict verdict;
        public final String criterion, evidenceId;
        Result(Verdict verdict, String criterion, String evidenceId) {
            this.verdict = verdict; this.criterion = criterion; this.evidenceId = evidenceId;
        }
    }
    private AgentGoalVerifier() { }

    /** Unsupported language tasks must remain UNKNOWN, even after an LLM review. */
    public static Result evaluate(AgentRun run) {
        if (run == null || run.goal == null || run.codeAllowed)
            return new Result(Verdict.UNKNOWN, "NONE", "");
        String phrase = run.goal.trim();
        String lower = phrase.toLowerCase(Locale.ROOT);
        String address = null;
        for (String prefix : new String[]{
                "lee ", "abre ", "consulta ", "lee la página ", "consulta la url ",
                "abre la página ", "lee la url "}) {
            if (lower.startsWith(prefix)) {
                address = phrase.substring(prefix.length()).trim(); break;
            }
        }
        if (address == null || !address.startsWith("https://") || address.contains(" ")
                || AgentPolicy.denial(run, "web.read", address) != null)
            return new Result(Verdict.UNKNOWN, "NONE", "");
        if (run.observations == null || run.evidence == null)
            return new Result(Verdict.NOT_VERIFIED, "PUBLIC_READ", "");
        Set<String> seen = new HashSet<>();
        boolean partial = false;
        String expected = AgentExperience.fingerprint("web.read", address);
        for (AgentRun.Observation observed : run.observations) {
            if (observed == null || !seen.add(observed.id)
                    || !"web.read".equals(observed.tool)
                    || !expected.equals(observed.fingerprint)
                    || observed.sources == null || !observed.sources.contains(address)
                    || !run.evidence.contains(observed.id)) continue;
            if ("SUCCESS".equals(observed.status) && run.answer != null
                    && !run.answer.trim().isEmpty())
                return new Result(Verdict.VERIFIED, "PUBLIC_READ", observed.id);
            if ("PARTIAL".equals(observed.status)) partial = true;
        }
        return new Result(partial ? Verdict.PARTIAL : Verdict.NOT_VERIFIED, "PUBLIC_READ", "");
    }
}
