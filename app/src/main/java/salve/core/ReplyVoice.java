package salve.core;

import java.util.regex.Pattern;

/**
 * Retoques de la voz de Salve en lo que dice a Bryan. El modelo pequeño a veces confunde los
 * papeles y le llama por el nombre de ella («Con gusto, Salve.»): Salve nunca se habla a sí misma.
 */
public final class ReplyVoice {
    private ReplyVoice() { }

    /** «Con gusto, Salve.» · «Claro Salve,» · «Hola, Salve!» al empezar (tras saludo o asentimiento). */
    private static final Pattern OPENING = Pattern.compile(
            "^(\\s*(?:\\[\\[[^\\]]*\\]\\]\\s*)*)"                              // marcas de dibujo delante
            + "((?:con gusto|claro|vale|de acuerdo|perfecto|genial|gracias|muchas gracias|hola|buenas|"
            + "buenos d[ií]as|buenas tardes|buenas noches|por supuesto|entendido|listo|venga|s[ií]))"
            + "\\s*,?\\s+salve\\s*([.,;:!?¡]?)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** «…, Salve.» al final de una frase dirigida a Bryan. */
    private static final Pattern CLOSING = Pattern.compile(",\\s*salve\\s*([.!?]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Quita el vocativo «Salve» dirigido a Bryan; no toca «Salve» como sujeto («Salve dibujó…»). */
    public static String withoutSelfVocative(String text) {
        if (text == null || text.isEmpty()) return text;
        String out = OPENING.matcher(text).replaceFirst("$1$2$3");
        out = CLOSING.matcher(out).replaceAll("$1");
        return out;
    }

    /** ¿La marca de dibujo ([[salve_draw:…]]) es de este motivo? Sin motivo conocido, se da por bueno. */
    public static boolean sketchAbout(String tagged, String subject) {
        if (subject == null || subject.trim().isEmpty()) return true;
        String want = salve.sketch.SketchLibrary.normalize(subject).replaceAll("[^a-z0-9ñ]", "");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\[salve_draw:([^\\]]*)\\]\\]").matcher(tagged);
        boolean any = false;
        while (m.find()) {
            any = true;
            String key = salve.sketch.SketchLibrary.normalize(m.group(1)).replaceAll("[^a-z0-9ñ]", "");
            if (!want.isEmpty() && (key.contains(want) || (key.length() >= 3 && want.contains(key)))) return true;
        }
        return !any;
    }

}
