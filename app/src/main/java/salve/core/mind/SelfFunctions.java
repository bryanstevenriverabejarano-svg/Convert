package salve.core.mind;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sus funciones propias (examinarse, mirar su memoria, enseñar el mapa de su mente, proponer
 * mejoras) como una herramienta del modelo. Las frases exactas siguen funcionando al momento; esto
 * es para cuando Bryan lo dice con sus palabras («¿qué tal tu memoria?», «ponte a prueba»,
 * «enséñame cómo piensas»): el modelo entiende la petición y elige la función.
 */
public final class SelfFunctions {
    private SelfFunctions() { }

    public static final String TOOL = "self.function";
    public static final String[] ACTIONS = {"estado_memoria", "examen", "mapa_mente", "propuestas_mejora"};

    /** Solo se ofrece cuando el mensaje toca alguno de estos temas (el catálogo no se llena de herramientas). */
    private static final Pattern TOPIC = Pattern.compile(
            "\\b(tu memoria|la memoria|vectores|examen|examinate|examiname|evaluate|evaluarte|ponte a prueba|"
            + "mapa|tu mente|como piensas|tu cabeza|tu cerebro|grafo|mejorar|mejorarte|te falla|fallas|evoluciona\\w*)\\b");

    /**
     * Cuando el mensaje toca alguna de sus funciones. No cuando pide un recuerdo («busca en tu
     * memoria lo de navegar»): eso es buscar, no informar de su memoria.
     */
    public static boolean offered(String input) {
        String t = normalize(input);
        return TOPIC.matcher(t).find() && !t.matches(".*\\b(busca|buscalo|encuentra|recuerda|acuerdas|recuerdas|hablamos|"
                + "hablaste|dijiste|dijistes|comentaste|contaste|ese recuerdo)\\b.*");
    }

    /** Corta: el catálogo entero de herramientas cabe en 500 caracteres. */
    public static String description() {
        return "{accion:estado_memoria|examen|mapa_mente|propuestas_mejora}: lo haces si Bryan lo pide ahora (tu memoria, examinarte, tu mente, qué mejorar)";
    }

    /** La acción pedida, o null si no es una de las suyas. */
    public static String action(String raw) {
        String a = normalize(raw).replace(' ', '_');
        for (String known : ACTIONS) if (known.equals(a)) return known;
        if (a.contains("memoria")) return "estado_memoria";
        if (a.contains("exam")) return "examen";
        if (a.contains("mapa") || a.contains("mente")) return "mapa_mente";
        if (a.contains("propuesta") || a.contains("mejora")) return "propuestas_mejora";
        return null;
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim();
    }
}
