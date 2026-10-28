package salve.sketch;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * ¿Le piden dibujarse a sí misma, a Bryan o a los dos? Entonces no busca «salve» en internet (salen
 * marcas y cosas que no son ella): usa sus propias imágenes, su cuerpo del Vestuario, y la foto de
 * Bryan que tiene en su memoria visual.
 */
public final class SelfPortraitRequest {
    public final boolean salve, bryan;

    private SelfPortraitRequest(boolean salve, boolean bryan) { this.salve = salve; this.bryan = bryan; }

    public boolean any() { return salve || bryan; }

    public boolean together() { return salve && bryan; }

    private static final Pattern SALVE = Pattern.compile(
            "\\b(a ti|dibujate|dibujarte|retratate|tu misma|ti misma|autorretrato|autoretrato|tu cuerpo|como eres tu|salve)\\b");
    private static final Pattern BRYAN = Pattern.compile(
            "\\b(bryan|mi retrato|como soy yo)\\b|\\ba mi( y\\b| con\\b| junto| tambien| solo| mismo| misma|$)");
    private static final Pattern BOTH = Pattern.compile("\\b(juntos|juntas|los dos|las dos|nosotros|nosotras|tu y yo|ti y yo|yo y tu)\\b");
    private static final Pattern SUBJECT_SALVE = Pattern.compile("^(salve|ti|tu|ti misma|tu misma|autorretrato|ti y yo|tu y yo)$");

    /** @param input lo que escribió Bryan; @param subject lo que el analizador cree que hay que dibujar */
    public static SelfPortraitRequest of(String input, String subject) {
        String t = normalize(input).trim(), s = normalize(subject).trim();
        boolean both = BOTH.matcher(t).find();
        boolean salve = SALVE.matcher(t).find() || SUBJECT_SALVE.matcher(s).matches();
        boolean bryan = BRYAN.matcher(t).find() || s.equals("bryan") || s.equals("mi");
        // «juntos» une a los dos sólo si uno de ellos ya aparece (no «dibuja dos gatos juntos»).
        if (both && (salve || bryan)) { salve = true; bryan = true; }
        return new SelfPortraitRequest(salve, bryan);
    }

    /** El nombre con el que lo guarda en su repertorio. */
    public String title() {
        return together() ? "Bryan y yo" : salve ? "yo, Salve" : "Bryan";
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ ]+", " ").replaceAll("\\s+", " ");
    }
}
