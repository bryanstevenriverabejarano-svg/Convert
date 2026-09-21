package salve.avatar;

import java.text.Normalizer;
import java.util.Locale;

/** Small persistent design recipe; it does not contain generated images or executable code. */
public final class AvatarDesignSpec {
    public enum Palette {
        ORIGINAL(0), TURQUOISE(0xFF15CCC8), LAVENDER(0xFF9A86E8), ROSE(0xFFE19CAD), BLUE(0xFF609EE8), AMBER(0xFFE1B36C);
        public final int color;
        Palette(int color) { this.color = color; }
    }
    public enum Pattern { NONE, STARS, STRIPES }
    public static final String ORIGINAL_ID = "original";
    public final String id, name, template;
    /** Zero preserves the template's original colors. Other colors apply only to validated clothing masks. */
    public final int color;
    public final Palette palette;
    public final Pattern pattern;

    public AvatarDesignSpec(String id, String name, String template, Palette palette, Pattern pattern) {
        if (id == null || !(id.equals(ORIGINAL_ID) || id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
            throw new IllegalArgumentException("Identificador de diseño no válido.");
        this.name = validateName(name);
        AvatarDesignCatalog.template(template);
        if (palette == null || pattern == null) throw new IllegalArgumentException("Falta el color o el patrón.");
        if (id.equals(ORIGINAL_ID) && (!template.equals("original_dress") || palette != Palette.ORIGINAL || pattern != Pattern.NONE))
            throw new IllegalArgumentException("El diseño original se conserva intacto.");
        this.id = id; this.template = template; this.palette = palette; this.pattern = pattern; color = palette.color;
    }
    public static AvatarDesignSpec original() {
        return new AvatarDesignSpec(ORIGINAL_ID, "Vestido original", "original_dress", Palette.ORIGINAL, Pattern.NONE);
    }
    public static String validateName(String name) {
        if (name == null) throw new IllegalArgumentException("Pon un nombre al diseño.");
        String value = Normalizer.normalize(name.trim(), Normalizer.Form.NFC);
        if (!value.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _'’-]{0,47}"))
            throw new IllegalArgumentException("El nombre debe tener de 1 a 48 letras, números o espacios.");
        return value;
    }
    static String normalizedName(String name) { return validateName(name).toLowerCase(Locale.ROOT); }
}
