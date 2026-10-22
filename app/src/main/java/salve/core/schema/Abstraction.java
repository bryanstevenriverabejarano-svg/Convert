package salve.core.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cuándo y cómo razona con esquemas: la herramienta cognition.abstract se ofrece solo cuando el
 * mensaje lo pide (un porqué, una metáfora, un concepto abstracto), y las frases directas
 * («descompón el concepto de libertad», «¿qué conceptos has abstraído?») se responden sin modelo.
 */
public final class Abstraction {
    private Abstraction() { }

    public static final String TOOL = "cognition.abstract";

    /** Corta: el catálogo de herramientas entero cabe en 500 caracteres. */
    public static String toolDescription() {
        return "{concepto:string}: lo descompones en esquemas (soporte, camino, frontera…) y sus consecuencias, para razonar por analogía";
    }

    private static final Pattern WHY = Pattern.compile(
            "\\b(por que|que significa|que quiere decir|como se entiende|metafora|analogia|en el fondo|como funciona|que es (?:la|el|una|un) )");

    /** Se ofrece con un porqué, una metáfora o cuando el texto ya delata un esquema. */
    public static boolean offered(String input) {
        String t = ImageSchemas.normalize(input);
        return t.length() >= 8 && (WHY.matcher(t + " ").find() || !ImageSchemas.detect(t).isEmpty());
    }

    private static final Pattern DECOMPOSE = Pattern.compile(
            "^(?:salve )?(?:descompon|descomponme|abstrae|abstraeme|desmonta|analiza)(?: el concepto de| la idea de| el concepto| la idea)? (.{3,50})$");

    /** «Descompón el concepto de libertad» → «libertad»; null si no es eso. */
    public static String decomposeRequest(String input) {
        Matcher m = DECOMPOSE.matcher(ImageSchemas.normalize(input));
        if (!m.find()) return null;
        String c = m.group(1).replaceFirst("^(?:el |la |los |las |un |una )", "").trim();
        return c.isEmpty() || c.matches(".*\\b(foto|escena|imagen|documento|texto)\\b.*") ? null : c;
    }

    /** «¿Qué conceptos has abstraído?», «tus esquemas», «¿qué esquemas conoces?» */
    public static boolean asksForList(String input) {
        String t = ImageSchemas.normalize(input);
        return t.matches(".*\\b(que|cuales) (conceptos|esquemas) (has abstraido|has anclado|conoces|tienes|entiendes)\\b.*")
                || t.matches("(tus|mis) (conceptos|esquemas)( abstraidos| anclados)?");
    }

    /** Cómo lo entiende, en palabras normales, sin nombres técnicos. */
    public static String explain(ConceptStore.Concept c) {
        StringBuilder b = new StringBuilder("Así entiendo «").append(c.name).append("» por dentro:");
        for (SceneSchemas.Instance i : c.instances()) {
            List<String> inf = i.schema.infer(i.a, i.b);
            b.append("\n• Como ").append(i.schema.gloss).append(": ").append(inf.get(0)).append('.');
            for (int k = 1; k < inf.size(); k++) b.append(' ').append(capital(inf.get(k))).append('.');
        }
        if (!c.words.isEmpty()) b.append("\nLo mismo vale para: ").append(String.join(", ", c.words)).append('.');
        return b.toString();
    }

    /** Lista para Bryan. */
    public static String list(List<ConceptStore.Concept> all, List<String> pending) {
        if (all.isEmpty()) return "Todavía no he anclado ningún concepto." + (pending.isEmpty() ? "" : " Tengo pendientes para mi sueño: "
                + String.join(", ", pending) + ".") + " Pídeme «descompón el concepto de …» y lo hago ahora.";
        StringBuilder b = new StringBuilder("He anclado ").append(all.size()).append(all.size() == 1 ? " concepto: " : " conceptos: ");
        List<String> items = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0 && items.size() < 15; i--) {
            ConceptStore.Concept c = all.get(i);
            List<String> labels = new ArrayList<>();
            for (SceneSchemas.Instance x : c.instances()) labels.add(x.schema.label);
            items.add(c.name + " (" + String.join(", ", labels) + ")");
        }
        b.append(String.join("; ", items)).append('.');
        if (!pending.isEmpty()) b.append(" Pendientes para mi sueño: ").append(String.join(", ", pending)).append('.');
        return b.toString();
    }

    private static String capital(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
}
