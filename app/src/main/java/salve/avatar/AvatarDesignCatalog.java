package salve.avatar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Bundled, reviewed illustration templates. Model output can choose an id, never a file or URL. */
public final class AvatarDesignCatalog {
    public static final class Template {
        public final String id, name, assetPath;
        private Template(String id, String name, String assetPath) {
            this.id = id; this.name = name; this.assetPath = assetPath;
        }
    }
    private static final List<Template> TEMPLATES = Collections.unmodifiableList(Arrays.asList(
            new Template("original_dress", "Vestido original", null),
            new Template("pajamas", "Pijama", "avatar/wardrobe/pajamas.png"),
            new Template("explorer", "Exploradora", "avatar/wardrobe/explorer.png")));
    private AvatarDesignCatalog() { }
    public static List<Template> templates() { return TEMPLATES; }
    public static Template template(String id) {
        for (Template candidate : TEMPLATES) if (candidate.id.equals(id)) return candidate;
        throw new IllegalArgumentException("Esa plantilla de ropa no existe.");
    }
}
