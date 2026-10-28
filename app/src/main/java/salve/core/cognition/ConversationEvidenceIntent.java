package salve.core.cognition;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import salve.core.conversation.ChatMessage;
import salve.core.evaluation.SleepImprovementCycle;

/**
 * Cheap, read-only retrieval hint. The LLM may choose a scope among authorized tools;
 * this guard only guarantees grounded fallback for *clearly* requested private records.
 * A hint never grants permission and does not run any model or action.
 */
public final class ConversationEvidenceIntent {
    public enum Scope { NONE, SLEEP, EXECUTION, BOTH }
    private ConversationEvidenceIntent() { }

    public static Scope classify(String input, List<ChatMessage> history) {
        String s = normalize(input);
        if (s.isEmpty() || s.length() > 240 || s.matches("^(?:inventa|imagina|escribe|crea|dibuja|simula|finge|no me)\\b.*"))
            return Scope.NONE;
        if (s.matches(".*\\b(?:mi|mis) (?:suenos|sueno|ubicacion|localizacion|posicion)\\b.*")
                || s.matches(".*\\b(?:donde estoy|donde estuve|mi gps)\\b.*"))
            return Scope.NONE; // A geolocation history is not the execution journal.
        boolean execution = s.matches(".*\\b(?:registro de ejecucion|registro de tareas|registro de acciones|historial de ejecucion|historial de tareas|log de ejecucion|logs de ejecucion|notas de tarea|tus registros de ejecucion)\\b.*")
                || s.matches("^(?:muestrame|ensename|abre|ver|quiero ver|dame|lee) (?:el |tu |tus )?(?:registro|registros|logs|log)$");
        if (!execution && recentSleepContext(history)
                && s.matches("^(?:y |pues |entonces )?(?:el |tu |un )?(?:registro|logs|log|historial)(?: de (?:tareas|ejecucion|acciones))?$"))
            execution = true;
        boolean sleep = SleepImprovementCycle.asksStatus(input)
                || s.matches(".*\\b(?:informe de sueno|informe de sueño|informe nocturno|actividad nocturna|aprendizaje nocturno)\\b.*");
        boolean learn = s.matches(".*\\b(?:aprend\\w*|mejor\\w*|progres\\w*|practic\\w*|consolid\\w*|investig\\w*|pens\\w*|hic\\w*|hiz\\w*|hech\\w*|trabaj\\w*|avanc\\w*|descubr\\w*|realiz\\w*)\\b.*");
        boolean refer = s.matches(".*\\b(?:duerm\\w*|dorm\\w*|durm\\w*|descans\\w*|sueno\\w*|noche|anoche|nocturn\\w*|reposo)\\b.*");
        if (!sleep && learn && refer && likelyQuestion(s)) sleep = true;
        // Multi-source question: "¿Qué aprendiste y dónde puedo ver el registro?"
        if (execution && (sleep || (learn && recentSleepContext(history)))) sleep = true;
        // Ellipsis "¿Y el registro?" adds a second source to the prior sleep question.
        if (execution && !sleep && recentSleepContext(history)
                && s.matches("^(?:y |pues |entonces )?(?:el |tu |un )?(?:registro|logs|log|historial)(?: de (?:tareas|ejecucion|acciones))?$"))
            sleep = true;
        if (recentSleepContext(history)
                && s.matches("^(?:y )?(?:(?:muestrame|ensename|quiero|dame|prefiero) )?(?:los dos|las dos|ambos|ambas|todo|los dos informes)$"))
            return Scope.BOTH;
        if (!sleep && !execution && recentSleepContext(history) && likelyQuestion(s)
                && s.matches("^(?:y |pues |entonces )?(?:que|como|y|el|la|muest|ense|dime|cuentame|resum)\\w*\\b.*")
                && s.matches(".*\\b(?:resultad\\w*|conclus\\w*|prueba\\w*|logr\\w*|hic\\w*|aprend\\w*|progres\\w*|informe\\w*|resumen\\w*)\\b.*"))
            sleep = true;
        return sleep ? (execution ? Scope.BOTH : Scope.SLEEP) : execution ? Scope.EXECUTION : Scope.NONE;
    }

    public static String normalize(String input) {
        return Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
    }

    private static boolean likelyQuestion(String s) {
        return s.matches("^(?:que|como|cual|cuando|cuanto|en que|dime|muestrame|ensename|cuentame|quiero saber|explicame|resumeme|y|el|tu)\\b.*");
    }

    private static boolean recentSleepContext(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return false;
        int seen = 0;
        for (int i = history.size() - 1; i >= 0 && seen < 4; i--) {
            ChatMessage message = history.get(i);
            if (message == null || message.getContent() == null) continue;
            String content = normalize(message.getContent());
            if (content.length() > 600) content = content.substring(0, 600);
            if (content.matches(".*\\b(?:dorm\\w*|durm\\w*|sueno\\w*|nocturn\\w*|sleep.improvement|informe de mejora nocturna)\\b.*"))
                return true;
            seen++;
        }
        return false;
    }
}
