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
    /**
     * Solo hay dos modelos para elegir, los dos sin censura (GGUF): E2B (pequeño y rápido) y E4B (más
     * listo). «Gemma 4 E2B», «el modelo rápido» → E2B; «Gemma 4 E4B», «el grande», «sin censura» o
     * «Qwen» (al que sustituyen) → E4B.
     */
    public enum Target { E2B, E4B }

    /**
     * Verbo de cambio al principio, en cualquier forma («usa», «usar», «descárgate», «cámbialo»…) y
     * con las erratas típicas del teclado («descaegar», «descrgar»). Nunca «describe»: no lleva «ga».
     */
    private static final Pattern VERB = Pattern.compile(
            "^(?:quiero que |quiero |a partir de ahora |de ahora en adelante |puedes |podrias )?"
                    + "(?:us[ae]\\w*|utiliz\\w*|utilic\\w*|cambi\\w*|pas[ae]\\w*|activ\\w*|vuelv\\w*|volver|pon\\w*|"
                    + "carg\\w*|desc\\w{0,3}g[ae]\\w*|instal\\w*|baj[ae]\\w*)\\b.*");
    private static final Pattern NAME_E2B = Pattern.compile("(?:gem+a|jema|yema|modelo)4?e?2b");
    private static final Pattern NAME_E4B = Pattern.compile("(?:gem+a|jema|yema|modelo)4?e?4b");
    private static final Pattern LIGHT = Pattern.compile(
            "\\bmodelo (?:mas )?(?:rapido|ligero|pequeno|chico|liviano)\\b");
    private static final Pattern LARGE = Pattern.compile(
            "\\bmodelo (?:mas )?(?:grande|potente|listo|inteligente|completo)\\b");
    /** «Qwen» casi nunca sale escrito así en el dictado: «cuen», «kuen», «quen», «güen»… */
    private static final Pattern QWEN = Pattern.compile("\\b(?:qwen|quen|kwen|kuen|cuen|guen|qwen3|qwen35)\\b");
    private static final Pattern UNCENSORED = Pattern.compile("\\bsin (?:censura|censurar|filtros?)\\b");
    private static final int MAX_WORDS = 14;

    private ModelSwitchCommand() { }

    public static Target parse(String input) {
        String text = normalize(input);
        if (text.isEmpty() || text.split(" ").length > MAX_WORDS || !VERB.matcher(text).matches()) return null;
        boolean uncensored = QWEN.matcher(text).find() || UNCENSORED.matcher(text).find();
        if (LIGHT.matcher(text).find()) return Target.E2B;
        if (LARGE.matcher(text).find()) return Target.E4B;
        String compact = text.replace("cuatro", "4").replace(" dos ", " 2 ").replaceAll(" dos$", " 2")
                .replace(" de ", " ").replace(" ", "");
        // «gemma 4 e4b» compacta a «gemma4e4b»; «gemma 42b» a «gemma42b» (E2B).
        boolean e4 = NAME_E4B.matcher(compact).find(), e2 = NAME_E2B.matcher(compact).find();
        if (e2 == e4) return uncensored && !e2 ? Target.E4B : null; // ninguno, o los dos («cambia de e2b a e4b»: que lo diga claro)
        return e2 ? Target.E2B : Target.E4B;
    }

    private static final Pattern DOWNLOAD_STATUS = Pattern.compile(
            "^(?:(?:como|que tal|en que) va(?:n)?|(?:cual es el )?estado de|que pasa con|como esta|ha terminado|termino|se ha bajado)"
                    + " (?:la |las |el )?(?:descargas?|modelo nuevo|modelo sin censura)(?: del modelo| de los modelos| del gemma.*)?$");

    /** «¿Cómo va la descarga?», «estado de la descarga», «¿ha terminado la descarga?». */
    public static boolean asksDownloadStatus(String input) {
        return DOWNLOAD_STATUS.matcher(normalize(input)).matches();
    }

    public static String label(Target target) {
        switch (target) {
            case E2B: return "Gemma 4 E2B sin censura";
            default: return "Gemma 4 E4B sin censura";
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
