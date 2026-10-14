package salve.core.mind;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Personas de la familia que Bryan menciona («mi hermana se llama Ana»).
 *
 * <p>Se guardan como datos de perfil con categoría {@code family_<relación>_<nombre>}, de modo que
 * dos hermanas no se pisan y borrar una no borra la otra. «¿Quién es mi familia?» se responde con
 * lo guardado, sin que el modelo invente ni conteste «como modelo de lenguaje…».</p>
 */
public final class FamilyFacts {
    private static final String RELATIONS =
            "madre|mama|padre|papa|hermana|hermano|hijo|hija|esposa|esposo|mujer|marido|novia|novio|pareja|"
                    + "abuela|abuelo|tia|tio|prima|primo|sobrina|sobrino|suegra|suegro|cunada|cunado";
    /** «se llama» admite minúsculas; «es» exige nombre en mayúscula para no confundir «mi madre es enfermera». */
    private static final Pattern CALLED = Pattern.compile(
            "\\bmi (" + RELATIONS + ") se llama ([\\p{L}]+(?: [\\p{L}]+)?)");
    private static final Pattern IS_NAME = Pattern.compile(
            "\\b(?i:mi) ((?i:" + RELATIONS + ")) es (\\p{Lu}[\\p{L}]+(?: \\p{Lu}[\\p{L}]+)?)");
    private static final Pattern QUESTION = Pattern.compile(
            "\\b(quien es mi familia|quienes son mi familia|quienes son mis familiares|que sabes de mi familia|"
                    + "conoces a mi familia|sabes quien es mi familia|como se llama mi (" + RELATIONS + ")|"
                    + "quien es mi (" + RELATIONS + ")|recuerdas a mi (" + RELATIONS + "))\\b");

    public static final class Member {
        public final String relation;
        public final String name;
        Member(String relation, String name) { this.relation = relation; this.name = name; }
        public String category() { return "family_" + relation + "_" + normalize(name).replace(' ', '_'); }
        public String statement() { return "Mi " + display(relation) + " se llama " + name + "."; }
    }

    private FamilyFacts() {}

    /** Miembros declarados en un mensaje (no en una pregunta). */
    public static List<Member> parse(String input) {
        List<Member> out = new ArrayList<>();
        if (input == null || input.trim().endsWith("?")) return out;
        String folded = fold(input);
        Matcher called = CALLED.matcher(folded);
        while (called.find()) {
            String original = input.substring(called.start(2), called.end(2));
            String[] words = original.split(" ");
            // Un segundo nombre solo si va en mayúscula («Ana María»), no «ana vive…».
            if (words.length == 2 && !Character.isUpperCase(words[1].charAt(0))) original = words[0];
            String name = capitalize(original);
            String first = normalize(name.split(" ")[0]);
            if (!first.matches("como|igual|asi|parecido")) out.add(new Member(called.group(1), stopAtConnector(name)));
        }
        Matcher is = IS_NAME.matcher(stripAccents(input));
        while (is.find()) out.add(new Member(is.group(1).toLowerCase(Locale.ROOT), stopAtConnector(is.group(2))));
        return out;
    }

    public static boolean asksAboutFamily(String input) {
        String t = normalize(input);
        return !t.isEmpty() && t.split(" ").length <= 14 && QUESTION.matcher(t).find();
    }

    /** Respuesta con lo guardado. {@code stored} son frases de perfil «Mi hermana se llama Ana.». */
    public static String answer(String input, List<String> stored) {
        String t = normalize(input);
        Matcher specific = Pattern.compile("\\bmi (" + RELATIONS + ")\\b").matcher(t);
        String wanted = specific.find() && !t.contains("familia") ? specific.group(1) : null;
        List<String> facts = new ArrayList<>();
        for (String s : stored) {
            String n = normalize(s);
            if (wanted == null || n.startsWith("mi " + wanted + " ")) facts.add(s.replaceAll("[.]+$", "").replaceFirst("^Mi ", "tu "));
        }
        if (facts.isEmpty()) {
            return wanted == null
                    ? "Aún no me has hablado de tu familia en esta instalación. Si me dices, por ejemplo, «mi hermana se llama …», lo recordaré."
                    : "No tengo guardado quién es tu " + display(wanted) + ". Si me lo dices («mi " + display(wanted)
                    + " se llama …»), lo recordaré.";
        }
        return "Por lo que me has contado: " + String.join("; ", facts) + ".";
    }

    private static String stopAtConnector(String name) {
        return name.replaceAll("(?i)\\s+(y|e|pero|que)$", "").trim();
    }

    private static String display(String relation) {
        switch (relation) {
            case "mama": return "mamá";
            case "papa": return "papá";
            case "tia": return "tía";
            case "tio": return "tío";
            case "cunada": return "cuñada";
            case "cunado": return "cuñado";
            default: return relation;
        }
    }

    private static String capitalize(String raw) {
        StringBuilder out = new StringBuilder();
        for (String part : raw.trim().split("\\s+")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /** Minúsculas y sin tildes, conservando longitud para mapear posiciones al texto original. */
    private static String fold(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            out.append(base.isEmpty() ? c : Character.toLowerCase(base.charAt(0)));
        }
        return out.toString();
    }

    private static String stripAccents(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            String base = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            out.append(base.isEmpty() ? c : base.charAt(0));
        }
        return out.toString();
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
