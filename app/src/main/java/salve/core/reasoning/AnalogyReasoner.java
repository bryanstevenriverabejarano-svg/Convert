package salve.core.reasoning;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Razonamiento por analogía «A es a B como C es a ?».
 *
 * <p>Primero con lo que Bryan le ha contado (analogía estructural sobre su memoria causal: misma
 * relación, otro concepto). Si no tiene base propia, guía al modelo a la manera de la «structure-mapping»
 * de Gentner: nombrar primero la relación que une A con B y después aplicarla a C, en vez de adivinar
 * por parecido superficial.</p>
 *
 * <p>No usa los vectores de ConceptSpace: empiezan al azar y nada los entrena, así que sus analogías
 * serían ruido.</p>
 */
public final class AnalogyReasoner {
    private AnalogyReasoner() { }

    /** Los tres términos de la pregunta. */
    public static final class Question {
        public final String a, b, c;
        Question(String a, String b, String c) { this.a = a; this.b = b; this.c = c; }
    }

    private static final Pattern ANALOGY = Pattern.compile(
            "^(?:salve,? )?(?:(?:y )?(?:si )?)?(.{1,60}?) es (?:a|al|a la|a los|a las) (.{1,60}?),? (?:lo )?(?:que|como|igual que) "
                    + "(.{1,60}?) es (?:a|al|a la|a los|a las)(?: (?:que|cual|quien|que cosa|\\.\\.\\.|…))?$");

    public static Question parse(String input) {
        if (input == null || input.length() > 220 || input.indexOf('\n') >= 0) return null;
        String t = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!]+", " ").replaceAll("\\s+", " ").replaceFirst("[.…\\s]+$", "").trim();
        Matcher m = ANALOGY.matcher(t);
        if (!m.matches()) return null;
        String a = clean(m.group(1)), b = clean(m.group(2)), c = clean(m.group(3));
        if (a.isEmpty() || b.isEmpty() || c.isEmpty() || a.equals(c)) return null;
        return new Question(a, b, c);
    }

    private static String clean(String term) {
        return term.trim().replaceFirst("^(?:el|la|los|las|un|una) ", "").trim();
    }

    /** Contexto para el modelo; vacío si no es una pregunta de analogía. */
    public static String promptBlock(String input, CausalMemory memory) {
        Question q = parse(input);
        if (q == null) return "";
        String[] own = memory == null ? null : memory.analogy(q.a, q.b, q.c);
        if (own != null)
            return " | ANALOGIA (con lo que Bryan te contó): la relación entre «" + q.a + "» y «" + q.b + "» es causal, y aplicada a «"
                    + q.c + "» da «" + own[0] + "» (" + own[1] + "). Responde eso, di que sale de lo que él te contó y explica la relación en una frase.";
        return " | ANALOGIA: antes de responder, nombra en una frase la relación exacta que une «" + q.a + "» con «" + q.b
                + "» (función, causa, parte-todo, opuesto, categoría…). Después aplica esa MISMA relación a «" + q.c
                + "» y da la respuesta. Si hay varias relaciones posibles, elige la más específica y dilo; si no hay ninguna clara, reconócelo.";
    }
}
