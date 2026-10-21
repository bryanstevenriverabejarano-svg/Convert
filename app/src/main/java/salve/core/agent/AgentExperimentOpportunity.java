package salve.core.agent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One controller-selected, read-only recovery experiment within the user's
 * existing objective. It cannot expand scope or grant capabilities.
 *
 * If public discovery failed twice and the user supplied one public HTTPS URL,
 * try reading exactly that URL once. No invented queries, accounts, writes,
 * private memories or model-created destinations.
 */
public final class AgentExperimentOpportunity {
    private static final Pattern PUBLIC_URL =
            Pattern.compile("https://[^\\s<>\\[\\]{}]+", Pattern.CASE_INSENSITIVE);

    public final String tool;
    public final String input;
    public final String reason;

    private AgentExperimentOpportunity(String tool, String input, String reason) {
        this.tool = tool;
        this.input = input;
        this.reason = reason;
    }

    /**
     * Called only when the controller already identified a repeated action.
     * The normal AgentPolicy check still runs at actual execution time.
     */
    public static AgentExperimentOpportunity afterRepeat(AgentRun run, AgentRun.Pending repeated) {
        if (run == null || repeated == null || run.goal == null || run.observations == null
                || run.codeAllowed || !"web.search".equals(repeated.tool)
                || run.observations.size() >= AgentEngine.MAX_STEPS
                || run.modelCalls >= AgentEngine.MAX_MODEL_CALLS) return null;

        String fingerprint = AgentExperience.fingerprint(repeated.tool, repeated.input);
        int errors = 0;
        for (AgentRun.Observation prior : run.observations) {
            if (prior == null) continue;
            if (fingerprint.equals(prior.fingerprint)) {
                if (prior.useful() || "BLOCKED".equals(prior.status)) return null;
                if ("ERROR".equals(prior.status)) errors++;
            }
        }
        if (errors < 2) return null;

        Matcher matches = PUBLIC_URL.matcher(run.goal);
        String url = null;
        while (matches.find()) {
            String candidate = matches.group().replaceFirst("[.,;!?)]*$", "");
            if (url != null) return null; // Ambiguous destination: never choose privately.
            url = candidate;
        }
        if (url == null || AgentPolicy.denial(run, "web.read", url) != null) return null;

        String key = AgentExperience.fingerprint("web.read", url);
        for (AgentRun.Observation prior : run.observations) {
            if (prior != null && key.equals(prior.fingerprint)) return null;
        }
        return new AgentExperimentOpportunity("web.read", url,
                "Experimento de lectura pública: dos búsquedas fallaron; "
                + "intento una vez la URL exacta del objetivo. Éxito de lectura no prueba éxito del objetivo.");
    }
}
