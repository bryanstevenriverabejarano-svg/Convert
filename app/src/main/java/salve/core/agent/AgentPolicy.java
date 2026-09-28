package salve.core.agent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.NetworkResourcePolicy;

/** Authority comes from the user's persisted request, never from a model or a web page. */
public final class AgentPolicy {
    public static final Set<String> TOOLS = java.util.Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "memory.lookup", "web.search", "web.read", "api.get", "browser.render", "code.python")));
    private static final Pattern URL = Pattern.compile("https://[^\\s<>\\[\\]{}]+", Pattern.CASE_INSENSITIVE);
    private AgentPolicy() { }
    /** Null means allowed; a denial is a recoverable observation, never executed. */
    public static String denial(AgentRun run, String tool, String input) {
        if (!TOOLS.contains(tool)) return "Herramienta no registrada; no puedo crear permisos desde un plan.";
        if (input == null || input.trim().isEmpty() || input.length() > 6000) return "Argumento fuera de presupuesto.";
        if (tool.equals("memory.lookup")) return run.codeAllowed
                ? "Los planes de código usan el objetivo explícito y fuentes públicas, no la memoria personal." : null;
        if (tool.equals("web.search")) return run.goal.equals(input) ? null
                : "La consulta pública debe ser el objetivo original; no se envían recuerdos privados como búsquedas.";
        if (tool.equals("code.python")) return !run.codeAllowed ? "Este plan no tiene autorización para código remoto."
                : run.bridge.isEmpty() ? "Falta configurar el puente de herramientas." : null;
        if (!NetworkResourcePolicy.validateKnowledgeUrl(input).allowed) return "URL pública HTTPS no válida.";
        if (!allowedUrls(run).contains(input)) return "La URL debe proceder del objetivo o de enlaces públicos ya observados.";
        if (tool.equals("browser.render") && run.bridge.isEmpty()) return "Falta configurar el navegador externo.";
        return null;
    }
    public static Set<String> allowedUrls(AgentRun run) {
        Set<String> urls = new HashSet<>();
        Matcher matcher = URL.matcher(run.goal);
        while (matcher.find() && urls.size() < 12) urls.add(matcher.group().replaceFirst("[.,;!?)]*$", ""));
        for (AgentRun.Observation observation : run.observations) if (observation.useful() && !observation.tool.equals("memory.lookup")) {
            urls.addAll(observation.sources); urls.addAll(observation.links);
        }
        return urls;
    }
}
