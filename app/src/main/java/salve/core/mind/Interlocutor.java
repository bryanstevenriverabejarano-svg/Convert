package salve.core.mind;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Con quién habla Salve.
 *
 * <p>Un modelo pequeño confunde «¿sabes quién soy?» con «¿quién eres?» y responde hablando de sí
 * mismo. Esta clase detecta las preguntas sobre la identidad del usuario y responde con lo que
 * consta en memoria, distinguiendo lo declarado de lo verificado: decir «soy tu creador» no lo
 * demuestra, así que se registra como presentación del usuario.</p>
 */
public final class Interlocutor {
    private static final Pattern IDENTITY_QUESTION = Pattern.compile(
            "\\b(sabes quien soy|quien soy yo|me conoces|me reconoces|sabes con quien (estas )?hablando|"
                    + "con quien (estas )?hablando|con quien hablas|sabes quien te habla|quien te habla|"
                    + "sabes como me llamo|como me llamo|recuerdas mi nombre|te acuerdas de mi|sabes mi nombre|"
                    + "y yo quien soy|quien soy)\\b");
    private static final Pattern NAME = Pattern.compile(
            "\\b(?:me llamo|mi nombre es)\\s+([\\p{L}][\\p{L}'\\-]*(?:\\s+[\\p{L}][\\p{L}'\\-]*){0,3})");
    /** «Soy X» solo cuenta como nombre si X va en mayúscula («soy Bryan», no «soy programador»). */
    private static final Pattern SOY_NAME = Pattern.compile(
            "\\b[Ss]oy\\s+(\\p{Lu}[\\p{L}'\\-]*(?:\\s+\\p{Lu}[\\p{L}'\\-]*){0,3})");
    private static final Pattern STOP_WORDS = Pattern.compile(
            "\\s+(?:y|e|pero|que|porque|aunque|tu|tú|te|me|el|la|de|del|a|en|con|para|por|como|soy|estoy|vivo|tengo|quiero|"
                    + "trabajo|ya|si|sí|no|tu creador|creador)\\b.*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CREATOR = Pattern.compile(
            "\\b(soy tu creador|soy quien te creo|soy quien te ha creado|yo te cree|te cree yo|soy tu programador|soy tu desarrollador)\\b");

    /** Lo que consta del usuario en esta instalación. */
    public static final class Known {
        public final String name;
        public final String nameSource;
        public final boolean declaredCreator;

        public Known(String name, String nameSource, boolean declaredCreator) {
            this.name = name;
            this.nameSource = nameSource;
            this.declaredCreator = declaredCreator;
        }
    }

    private Interlocutor() {}

    /** Pregunta breve sobre quién es el usuario (no sobre quién es Salve). */
    public static boolean asksWhoUserIs(String input) {
        String t = normalize(input);
        if (t.isEmpty() || t.split(" ").length > 16) return false;
        if (t.matches(".*\\b(quien eres|quien eres tu|como te llamas)\\b.*") && !t.matches(".*\\b(y yo|me conoces|quien soy)\\b.*"))
            return false;
        return IDENTITY_QUESTION.matcher(t).find();
    }

    /** Extrae el nombre de «me llamo Bryan Steven y soy tu creador…» → «Bryan Steven». */
    public static String extractName(String statement) {
        if (statement == null) return null;
        String text = statement.replaceAll("[.,;:!¡¿?\\n]", " ").replaceAll("\\s+", " ").trim();
        String raw;
        Matcher m = NAME.matcher(text.toLowerCase(Locale.ROOT));
        if (m.find()) raw = text.substring(m.start(1), m.end(1));
        else {
            Matcher soy = SOY_NAME.matcher(text);
            if (!soy.find()) return null;
            raw = soy.group(1);
        }
        raw = STOP_WORDS.matcher(raw).replaceFirst("").trim();
        if (raw.isEmpty()) return null;
        String first = normalize(raw.split(" ")[0]);
        if (first.matches("tu|el|la|un|una|de|muy|bien|yo|salve|tu creador|creador|alguien")) return null;
        StringBuilder out = new StringBuilder();
        for (String part : raw.split(" ")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.length() == 0 ? null : out.toString();
    }

    public static boolean declaresCreator(String statement) {
        return CREATOR.matcher(normalize(statement)).find();
    }

    /** Respuesta directa, sin pasar por el modelo, a «¿sabes quién soy?». */
    public static String answer(Known known, String configuredCreator, List<SelfModel.Belief> aboutUser) {
        if (known == null || known.name == null) {
            return "Todavía no tengo tu nombre guardado en esta instalación, así que no puedo confirmar quién eres. "
                    + (configuredCreator == null ? "" : "Me creó " + configuredCreator + "; si eres tú, ")
                    + "dime «me llamo …» y lo recordaré.";
        }
        StringBuilder out = new StringBuilder("Eres ").append(known.name);
        if (known.declaredCreator) out.append(", mi creador: así te presentaste");
        out.append('.');
        int shown = 0;
        if (aboutUser != null) for (SelfModel.Belief b : aboutUser) {
            out.append(shown == 0 ? " También recuerdo: " : "; ");
            out.append(b.statement.replaceAll("[.]+$", ""));
            if (++shown == 2) break;
        }
        if (shown > 0) out.append('.');
        return out.toString();
    }

    /** Línea para el prompt de cada turno: el modelo sabe con quién habla antes de responder. */
    public static String promptLine(Known known) {
        if (known == null || known.name == null)
            return "Interlocutor: no identificado en esta instalación; si surge con naturalidad, pregúntale su nombre. Si pregunta quién es él, no hables de ti.";
        return "Interlocutor: " + known.name + (known.declaredCreator ? " (se presentó como tu creador)" : "")
                + ". USER en HISTORY es esta persona; si pregunta quién es, responde sobre él, no sobre ti.";
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}0-9 ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
