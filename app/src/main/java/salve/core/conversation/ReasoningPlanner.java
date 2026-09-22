package salve.core.conversation;

import salve.core.memory.ConversationMemoryGrounding;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Decide qué recursos necesita el turno sin invocar otro modelo. */
public final class ReasoningPlanner {
    private static final Pattern MEMORY_CUE = Pattern.compile(
            ".*\\b(recuerda|recordamos|hablamos|dije|mi preferencia|prefiero|me gusta|sobre mi|lo anterior)\\b.*");

    private ReasoningPlanner() {}

    public static ReasoningPlan plan(String input,
                                     ConversationAnalysis analysis,
                                     boolean explicitExternalSearch) {
        if (analysis == null) throw new IllegalArgumentException("analysis no puede ser null");

        String normalized = normalize(input);
        ConversationAct act = analysis.getAct();
        boolean retrieveMemory = MEMORY_CUE.matcher(normalized).matches()
                || ConversationMemoryGrounding.shouldRetrieve(input)
                || act == ConversationAct.QUESTION
                || act == ConversationAct.EXPLANATION_REQUEST
                || act == ConversationAct.CORRECTION;
        boolean verify = explicitExternalSearch
                || act == ConversationAct.QUESTION
                || act == ConversationAct.EXPLANATION_REQUEST
                || act == ConversationAct.CORRECTION;

        return new ReasoningPlan(
                retrieveMemory,
                explicitExternalSearch,
                verify,
                directiveFor(act, verify)
        );
    }

    private static String directiveFor(ConversationAct act, boolean verify) {
        String base;
        switch (act) {
            case CORRECTION:
                base = "Contrasta con el turno anterior y corrige solo lo comprobable.";
                break;
            case EXPLANATION_REQUEST:
                base = "Explica por pasos, con precisión y sin alargar innecesariamente.";
                break;
            case QUESTION:
                base = "Responde directamente y declara cualquier incertidumbre relevante.";
                break;
            case COMMAND:
                base = "Indica el resultado real; no afirmes haber ejecutado lo que no se ejecutó.";
                break;
            case OPINION:
                base = "Distingue hechos, inferencias y opiniones.";
                break;
            case CASUAL:
                base = "Responde de forma natural y breve.";
                break;
            default:
                base = "Responde de forma coherente con el contexto disponible.";
        }
        return verify ? base + " Verifica la conclusión con el contexto disponible." : base;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
