package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * «Usa el modelo Gemma 4 E2B», «cámbiate a Gemma 4 E4B», «usa el modelo rápido / grande». El dictado
 * casi nunca escribe «E2B» tal cual («gemma 42b», «gemma cuatro e dos b», «gema 4 2 b»…), así que se
 * compacta el nombre antes de reconocerlo. Pide siempre un verbo de cambio al principio para no
 * confundirlo con una pregunta («¿qué tal funciona gemma 4 e2b?»). Lógica pura para pruebas JVM.
 */
public final class ModelSwitchCommand {
    /** QWEN35 = el de 4B (cabe entero en RAM); QWEN35_9B solo si se pide expresamente. */
    public enum Target { E2B, E4B, QWEN35, QWEN35_9B }

    private static final Pattern VERB = Pattern.compile(
            "^(?:quiero que |a partir de ahora |de ahora en adelante )?(?:usa|uses|utiliza|utilices|cambia|cambiate|"
                    + "cambies|pasa|pasate|pases|activa|actives|vuelve|vuelvas|ponte|pon|carga|cargues|"
                    + "descarga|descargate|descargues|instala|instalate|baja|bajate)\\b.*");
    private static final Pattern NAME_E2B = Pattern.compile("(?:gem+a|jema|yema|modelo)4?e?2b");
    private static final Pattern NAME_E4B = Pattern.compile("(?:gem+a|jema|yema|modelo)4?e?4b");
    private static final Pattern LIGHT = Pattern.compile(
            "\\bmodelo (?:mas )?(?:rapido|ligero|pequeno|chico|liviano)\\b");
    private static final Pattern LARGE = Pattern.compile(
            "\\bmodelo (?:mas )?(?:grande|potente|listo|inteligente|completo)\\b");
    /** «Qwen» casi nunca sale escrito así en el dictado: «cuen», «kuen», «quen», «güen»… */
    private static final Pattern QWEN = Pattern.compile("\\b(?:qwen|quen|kwen|kuen|cuen|guen|qwen3|qwen35)\\b");
    private static final Pattern QWEN_BIG = Pattern.compile("\\b(?:9 ?b|nueve(?: b)?|grande)\\b");
    private static final Pattern UNCENSORED = Pattern.compile("\\bmodelo (?:sin censura|sin censurar|sin filtros?)\\b");
    private static final int MAX_WORDS = 14;

    private ModelSwitchCommand() { }

    public static Target parse(String input) {
        String text = normalize(input);
        if (text.isEmpty() || text.split(" ").length > MAX_WORDS || !VERB.matcher(text).matches()) return null;
        if (QWEN.matcher(text).find() || UNCENSORED.matcher(text).find())
            return QWEN_BIG.matcher(text).find() ? Target.QWEN35_9B : Target.QWEN35;
        if (LIGHT.matcher(text).find()) return Target.E2B;
        if (LARGE.matcher(text).find()) return Target.E4B;
        String compact = text.replace("cuatro", "4").replace(" dos ", " 2 ").replaceAll(" dos$", " 2")
                .replace(" de ", " ").replace(" ", "");
        // «gemma 4 e4b» compacta a «gemma4e4b»; «gemma 42b» a «gemma42b» (E2B).
        boolean e4 = NAME_E4B.matcher(compact).find(), e2 = NAME_E2B.matcher(compact).find();
        if (e2 == e4) return null; // ninguno, o los dos («cambia de e2b a e4b»: que lo diga claro)
        return e2 ? Target.E2B : Target.E4B;
    }

    public static String label(Target target) {
        switch (target) {
            case E2B: return "Gemma 4 E2B";
            case E4B: return "Gemma 4 E4B";
            case QWEN35_9B: return "Qwen 3.5 9B sin censura";
            default: return "Qwen 3.5 4B sin censura";
        }
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "").replaceFirst("^(?:puedes |podrias )", "")
                .replaceFirst(" (?:por favor|porfa|ahora|ya)$", "").trim();
    }
}
