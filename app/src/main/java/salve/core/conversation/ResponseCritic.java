package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Se lee antes de hablar: comprueba su borrador contra lo que Bryan acaba de pedir (según
 * {@link DialogueFocus}) y nombra los fallos concretos. Si hay alguno, el motor lo reescribe una vez
 * con esa autocrítica. Reglas deterministas: no cuesta otra inferencia salvo cuando hace falta.
 */
public final class ResponseCritic {
    public enum Problem {
        REASKED("volviste a preguntar qué prefiere aunque ya eligió o aceptó: empieza directamente con el contenido"),
        NARRATIVE("escribiste un relato poético en vez de ayuda práctica: responde de forma directa, con explicación y ejemplos"),
        NO_CONTENT("solo anuncias lo que vas a hacer, sin hacerlo: escribe ya el contenido"),
        THIRD_PERSON("hablaste de ti en tercera persona («Salve sabe…»): habla en primera persona");
        public final String fix;
        Problem(String fix) { this.fix = fix; }
    }

    private ResponseCritic() { }

    private static final Pattern ASKS_PREFERENCE = Pattern.compile(
            "\\b(te gustaria|prefieres|quieres que|que te parece mejor|cuentame que|por cual|que opcion|o prefieres|o si prefieres|"
            + "que tipo de cosas|que te gustaria)\\b");
    private static final Pattern LYRICAL = Pattern.compile(
            "\\b(imagine|senti|el aire|vibraba|danza|susurro|como si|melodia|alma|brillaba|resonancia|tejer|arcilla|"
            + "rompecabezas|escalera|lienzo|manto|majestuoso|cantaba|esencia|mi conciencia|pinte la escena)\\b");
    private static final Pattern ANNOUNCES = Pattern.compile(
            "\\b(voy a|vamos a|estoy preparando|estoy terminando|preparo|dame un momento|enseguida|a continuacion te|ahora te)\\b");
    private static final Pattern THIRD = Pattern.compile("(?:^|[.!?¿¡]\\s*|,\\s*)(?:si,?\\s+)?salve (sabe|puede|es|tiene|esta|quiere)\\b");

    /** Fallos del borrador respecto a lo pedido (vacío si está bien). */
    public static List<Problem> check(String draft, DialogueFocus.Reading reading) {
        List<Problem> out = new ArrayList<>();
        if (draft == null || draft.trim().isEmpty()) return out;
        String t = normalize(draft);
        boolean committed = reading != null && (reading.kind == DialogueFocus.Kind.CHOOSE || reading.kind == DialogueFocus.Kind.ACCEPT);
        boolean practical = reading != null && (!reading.task.isEmpty() || committed);
        if (committed && draft.contains("?")) {
            String lastQuestion = lastQuestion(draft);
            int offered = 0;
            for (String o : reading.options) if (lastQuestion.contains(firstWord(o))) offered++;
            if (ASKS_PREFERENCE.matcher(lastQuestion).find() && (offered >= 1 || lastQuestion.contains(" o ")) && t.length() < 700)
                out.add(Problem.REASKED);
        }
        if (practical) {
            java.util.regex.Matcher m = LYRICAL.matcher(t);
            int lyrical = 0;
            while (m.find()) lyrical++;
            if (lyrical >= 3) out.add(Problem.NARRATIVE);
        }
        if (committed && ANNOUNCES.matcher(t).find() && t.length() < 260 && !t.contains(":")) out.add(Problem.NO_CONTENT);
        if (THIRD.matcher(t).find()) out.add(Problem.THIRD_PERSON);
        return out;
    }

    /** Instrucción de reescritura con los fallos concretos. */
    public static String critique(List<Problem> problems) {
        StringBuilder b = new StringBuilder(" | AUTOCRITICA (leíste tu borrador antes de enviarlo y no servía): ");
        for (Problem p : problems) b.append(p.fix).append(". ");
        return b.append("Reescribe la respuesta corrigiéndolo.").toString();
    }

    /** Arreglo mínimo sin reescribir: «Sí, Salve sabe inglés» → «Sí, sé inglés». */
    public static String firstPerson(String text) {
        if (text == null) return null;
        return text.replaceAll("\\bSalve sabe\\b", "sé").replaceAll("\\bSalve puede\\b", "puedo")
                .replaceAll("\\bSalve tiene\\b", "tengo").replaceAll("\\bSalve quiere\\b", "quiero")
                .replaceAll("(^|[.!?]\\s+)sé\\b", "$1Sé").replaceAll("(^|[.!?]\\s+)puedo\\b", "$1Puedo")
                .replaceAll("(^|[.!?]\\s+)tengo\\b", "$1Tengo").replaceAll("(^|[.!?]\\s+)quiero\\b", "$1Quiero");
    }

    private static String lastQuestion(String draft) {
        String[] parts = draft.split("(?<=[.!?])\\s+");
        for (int i = parts.length - 1; i >= 0; i--) if (parts[i].contains("?")) return normalize(parts[i]);
        return "";
    }

    private static String firstWord(String option) {
        for (String w : normalize(option).split(" ")) if (w.length() >= 5) return w;
        return normalize(option);
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ ,.!?¿¡:]", " ").replaceAll("\\s+", " ").trim();
    }
}
