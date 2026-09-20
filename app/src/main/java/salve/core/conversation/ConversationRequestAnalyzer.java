package salve.core.conversation;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Clasificador pragmático pequeño y determinista.
 *
 * No intenta sustituir al modelo ni al reconocedor de acciones: aporta una
 * señal estable para adaptar la respuesta y detectar referencias sin contexto.
 */
public final class ConversationRequestAnalyzer {
    private static final Pattern CORRECTION = Pattern.compile(
            "^(eso no|no es asi|te equivocas|estas equivocado|incorrecto|corrige|correccion)\\b.*");
    private static final Pattern EXPLANATION = Pattern.compile(
            "^(explica|explicame|por que|como funciona|como se hace|ayudame a entender)\\b.*");
    private static final Pattern OPINION = Pattern.compile(
            ".*\\b(creo que|pienso que|me parece|opino que|en mi opinion)\\b.*");
    private static final Pattern COMMAND = Pattern.compile(
            "^(haz|hazlo|crea|abre|busca|recuerda|dime|pon|analiza|muestra|guarda|elimina|envia|inicia|deten)\\b.*");
    private static final Pattern CASUAL = Pattern.compile(
            "^(hola|buenos dias|buenas tardes|buenas noches|como estas|gracias|muchas gracias|adios|hasta luego)[.! ]*$");
    private static final Pattern QUESTION_START = Pattern.compile(
            "^(que|como|cuando|donde|quien|quienes|cual|cuales|puedes|podrias|sabes|hay)\\b.*");
    private static final Pattern ORPHAN_REFERENCE = Pattern.compile(
            "^(hazlo|eso|aquello|lo anterior|continua con eso|sigue con eso|repitelo|corrigelo)[.! ]*$");

    private ConversationRequestAnalyzer() {}

    public static ConversationAnalysis analyze(String input, boolean hasPriorContext) {
        String normalized = normalize(input);
        boolean needsClarification = !hasPriorContext && ORPHAN_REFERENCE.matcher(normalized).matches();

        ConversationAct act;
        if (CORRECTION.matcher(normalized).matches()) {
            act = ConversationAct.CORRECTION;
        } else if (EXPLANATION.matcher(normalized).matches()) {
            act = ConversationAct.EXPLANATION_REQUEST;
        } else if (OPINION.matcher(normalized).matches()) {
            act = ConversationAct.OPINION;
        } else if (COMMAND.matcher(normalized).matches()) {
            act = ConversationAct.COMMAND;
        } else if (CASUAL.matcher(normalized).matches()) {
            act = ConversationAct.CASUAL;
        } else if (normalized.contains("?") || QUESTION_START.matcher(normalized).matches()) {
            act = ConversationAct.QUESTION;
        } else {
            act = ConversationAct.STATEMENT;
        }

        return new ConversationAnalysis(act, needsClarification);
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String lower = input.trim().toLowerCase(Locale.ROOT);
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('¿', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
