package salve.core.cognition;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Controller-owned capability discovery for the small model's bounded catalogue.
 * Only ranks tools already offered/authorized by CognitiveToolCatalog. No LLM
 * text, retrieved page, memory or self-improvement result can grant a capability.
 *
 * This is a cheap preliminary relevance ranking; Gemma still selects which of
 * the offered tools to use, CognitiveTurn limits work and the executor checks
 * permissions again.
 */
public final class CapabilityPlanner {
    private CapabilityPlanner() { }

    public static Map<String, CognitiveTurn.Tool> rank(
            Map<String, CognitiveTurn.Tool> available, String input) {
        if (available == null || input == null || available.isEmpty()) return available;
        String s = normalize(input);
        Set<String> first = new LinkedHashSet<>();
        boolean draw = has(s, "dibuj", "pint", "ilustr", "traz", "bosquej");
        boolean wardrobe = has(s, "ropa", "prenda", "vestid", "pijama", "traje",
                "camiseta", "falda", "abrigo", "armario", "outfit", "vistete", "ponte",
                "disen", "conjunto", "vestuario");
        boolean newDesign = has(s, "disen", "crea", "inventa", "nuevo", "nueva",
                "gener", "personaliz", "fabric");
        boolean wear = has(s, "vistete", "ponte", "equip", "lleva", "cambia");
        boolean list = has(s, "muestra", "lista", "ense", "cuales", "opciones", "armario");
        boolean research = has(s, "busca", "investig", "internet", "google", "noticia",
                "actual", "hoy", "precio", "fuente", "verifica", "consulta la web");
        boolean recall = has(s, "recuerd", "memoria", "memorias", "dijiste", "te dije",
                "hablamos", "conversamos", "primera vez", "lo anterior", "antes", "historial");
        boolean status = has(s, "estado", "modelo", "rendimiento", "procesador",
                "ram", "memoria libre", "temperatura", "bateria", "funcionando");
        boolean sync = has(s, "pcloud", "la nube", "sincroniz", "sube mis recuerdos",
                "restaura mi memoria", "copia de seguridad");
        boolean tap = has(s, "toca", "pulsa", "haz clic", "tap");
        boolean type = has(s, "escribe", "teclea", "rellena");
        boolean solver = has(s, "resuelve", "calcula", "optimiza", "ruta mas corta",
                "organiza tareas", "algoritmo");

        // Prioritize the user's explicit action before optional observability.
        // A tool being offered is NOT evidence that it answers this turn:
        // broad/combined catalogs must not turn "diseña ropa" into a log read.
        if (draw) prefer(available, first, "sketch.draw");

        if (wardrobe && !draw) {
            if (newDesign) prefer(available, first, "AVATAR_CREATE");
            else if (wear) prefer(available, first, "AVATAR_WEAR");
            else if (list) prefer(available, first, "AVATAR_LIST");
            else prefer(available, first, "AVATAR_CREATE");
            prefer(available, first, "AVATAR_LIST");
            prefer(available, first, "AVATAR_WEAR");
            prefer(available, first, "AVATAR_CREATE");
        }

        // Grounded self-report or a contextual follow-up ("¿y el registro?",
        // "muéstrame ambas") needs the already-authorized evidence reader.
        // Those elliptical follow-ups are offered by the conversation
        // controller; do not rank it for unrelated greetings or device state.
        boolean evidenceQuestion = ConversationEvidenceIntent.classify(input, null)
                != ConversationEvidenceIntent.Scope.NONE
                || has(s, "registro de ejecucion", "registro de tareas",
                       "ambas", "ambos", "informe nocturno", "resultados del sueno");
        if (evidenceQuestion) prefer(available, first, "evidence.inspect");

        if (sync) prefer(available, first, "cloud.sync");
        // Natural combined tasks can expose memory and web together; the model
        // decides whether one or both are necessary within its existing budget.
        if (recall && !research) prefer(available, first, "memory.lookup");
        if (research) prefer(available, first, "web.search");
        if (recall && research) prefer(available, first, "memory.lookup");
        if (status && !research && !recall) prefer(available, first, "runtime.status");
        if (solver) prefer(available, first, "SOLVE_CHALLENGE");
        if (type) prefer(available, first, "ESCRIBIR");
        if (tap) prefer(available, first, "TAP");
        if (has(s, "publica", "despliega") && has(s, "pagina", "web", "sitio"))
            prefer(available, first, "DEPLOY_WEB");

        // Preserve the original order when nothing is relevant; this is not a
        // universal lexical classification of arbitrary human conversation.
        if (first.isEmpty()) return available;
        Map<String, CognitiveTurn.Tool> result = new LinkedHashMap<>();
        for (String name : first) result.put(name, available.get(name));
        for (Map.Entry<String, CognitiveTurn.Tool> entry : available.entrySet())
            result.putIfAbsent(entry.getKey(), entry.getValue());
        return result;
    }

    private static void prefer(Map<String, CognitiveTurn.Tool> offered, Set<String> first, String name) {
        if (offered.containsKey(name)) first.add(name);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private static boolean has(String text, String... terms) {
        for (String term : terms) {
            String token = term.replace(" ", "\\s+");
            if (text.matches("(?s).*\\b" + token + "[a-z0-9]*\\b.*")) return true;
        }
        return false;
    }
}
