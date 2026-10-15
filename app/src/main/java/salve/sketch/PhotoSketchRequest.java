package salve.sketch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que Bryan dice junto a una foto (o sobre la foto que ya está en el chat):
 * «esto es un mueble, dibújame uno» → aprender «un mueble» y dibujarlo;
 * «dibújalo» / «dibuja esta foto» → dibujarla sin guardarle nombre propio.
 */
public final class PhotoSketchRequest {
    public enum Kind { NONE, DRAW, LEARN }

    public final Kind kind;
    /** Nombre tal como lo dijo, con artículo («un mueble»); vacío si no lo nombró. */
    public final String name;

    private PhotoSketchRequest(Kind kind, String name) { this.kind = kind; this.name = name == null ? "" : name; }

    private static final Pattern NAME = Pattern.compile(
            "(?:^|\\b)(?:esto|esta|este|eso|esa|ese|aqui|aquí)\\s+(?:(?:es|son)\\s+(?=(?:un|una|unos|unas|el|la|los|las|mi|mis)\\s)|se llama\\s+)"
            + "((?:(?:un|una|unos|unas|el|la|los|las|mi|mis)\\s+)?[\\p{L}][\\p{L} -]{1,40}?)"
            + "(?=\\s*(?:[,.;:!?¿¡]|\\by\\b|\\bque\\b|\\bpara\\b|\\bdibuj|\\bapr[eé]nd|$))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern LEARN_NAME = Pattern.compile(
            "\\b(?:aprende|apr[eé]ndete|recuerda)\\s+(?:a\\s+dibujar\\s+)?(?:(?:que|como)\\s+(?:esto|esta|este)\\s+es\\s+)?"
            + "((?:(?:un|una|el|la|mi)\\s+)?[\\p{L}][\\p{L} -]{1,40}?)(?=\\s*(?:[,.;:!?¿¡]|\\by\\b|$))",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DRAW = Pattern.compile(
            "\\b(dib[uú]j|p[ií]nt|tr[aá]z|c[oó]pi|ilustr|bosquej)\\p{L}*|\\bapr[eé]nde\\p{L}*\\s+a\\s+dibujar",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    /** Sin foto nueva, sólo cuenta si se refiere a la foto («dibuja la foto», «dibújala», «esto»). */
    private static final Pattern REFERS_TO_PHOTO = Pattern.compile(
            "\\b(foto|imagen|esto|esta|este|eso|dib[uú]jal[oa]s?|tr[aá]zal[oa]|c[oó]pial[oa])\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final java.util.Set<String> NOT_NAMES = new java.util.HashSet<>(java.util.Arrays.asList(
            "lo que", "lo mismo", "algo", "nada", "esto", "eso", "muy", "precioso", "bonito", "bonita", "genial",
            "una foto", "mi foto", "la foto", "una imagen", "la imagen"));

    /** @param withNewPhoto true si el mensaje llega con una foto adjunta */
    public static PhotoSketchRequest parse(String text, boolean withNewPhoto) {
        if (text == null || text.trim().isEmpty() || text.length() > 300) return new PhotoSketchRequest(Kind.NONE, null);
        String t = text.trim();
        if (t.matches("(?is).*\\b(qu[eé]|cu[aá]l)\\b.*\\?\\s*$") && !DRAW.matcher(t).find())
            return new PhotoSketchRequest(Kind.NONE, null); // una pregunta sobre la foto no es una lección
        String name = clean(match(NAME, t));
        if (name.isEmpty()) name = clean(match(LEARN_NAME, t));
        boolean draw = DRAW.matcher(t).find();
        if (!withNewPhoto && !REFERS_TO_PHOTO.matcher(t).find()) return new PhotoSketchRequest(Kind.NONE, null);
        if (!name.isEmpty()) return new PhotoSketchRequest(Kind.LEARN, name);
        return new PhotoSketchRequest(draw ? Kind.DRAW : Kind.NONE, null);
    }

    private static String match(Pattern p, String t) {
        Matcher m = p.matcher(t);
        return m.find() ? m.group(1) : "";
    }

    private static String clean(String raw) {
        if (raw == null) return "";
        String n = raw.trim().replaceAll("\\s+", " ").replaceAll("(?i)\\s+(dibuj|aprend|ap[eé]nd)\\p{L}*.*$", "").trim();
        String[] words = n.split(" ");
        if (words.length > 5) n = String.join(" ", java.util.Arrays.copyOf(words, 5));
        String lower = n.toLowerCase(java.util.Locale.ROOT);
        if (lower.length() < 2 || NOT_NAMES.contains(lower) || lower.startsWith("lo ") || lower.matches("(un|una|el|la)")) return "";
        return lower;
    }
}
