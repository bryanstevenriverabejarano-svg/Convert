package salve.avatar;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Strict allowlisted local wardrobe commands, separate from physical Android actions. */
public final class AvatarDesignRequest {
    public enum Kind { LIST, CREATE, WEAR, DELETE }
    public final Kind kind;
    public final String id, name, template;
    public final AvatarDesignSpec.Palette palette;
    public final AvatarDesignSpec.Pattern pattern;
    public final boolean wear;
    private AvatarDesignRequest(Kind kind, String id, String name, String template,
                                AvatarDesignSpec.Palette palette, AvatarDesignSpec.Pattern pattern, boolean wear) {
        this.kind = kind; this.id = id; this.name = name; this.template = template;
        this.palette = palette; this.pattern = pattern; this.wear = wear;
    }
    public static AvatarDesignRequest create(String name, String template, AvatarDesignSpec.Palette palette,
                                             AvatarDesignSpec.Pattern pattern, boolean wear) {
        String checkedName = AvatarDesignSpec.validateName(name);
        AvatarDesignCatalog.template(template);
        if (palette == null || pattern == null) throw new IllegalArgumentException("Falta el color o el patrón.");
        return new AvatarDesignRequest(Kind.CREATE, null, checkedName, template, palette, pattern, wear);
    }
    public static AvatarDesignRequest list() { return new AvatarDesignRequest(Kind.LIST, null, null, null, null, null, false); }
    public static AvatarDesignRequest select(String id) { return byId(Kind.WEAR, id); }
    public static AvatarDesignRequest delete(String id) { return byId(Kind.DELETE, id); }
    public static AvatarDesignRequest byName(Kind kind, String name) {
        if (kind != Kind.WEAR && kind != Kind.DELETE) throw new IllegalArgumentException("Acción de selección no válida.");
        return new AvatarDesignRequest(kind, null, AvatarDesignSpec.validateName(name), null, null, null, false);
    }
    private static AvatarDesignRequest byId(Kind kind, String id) {
        if (id == null || id.length() > 64 || !(id.equals(AvatarDesignSpec.ORIGINAL_ID)
                || id.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
            throw new IllegalArgumentException("Usa el identificador que aparece en la lista de diseños.");
        return new AvatarDesignRequest(kind, id, null, null, null, null, false);
    }
    public static AvatarDesignRequest parse(String json) {
        if (json == null || json.length() > 4096) throw new IllegalArgumentException("Comando de vestuario demasiado largo.");
        Map<String, Object> fields = new LinkedHashMap<>();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT); reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (fields.containsKey(key) || fields.size() >= 6) throw new IllegalArgumentException("Campo de vestuario duplicado o sobrante.");
                if (reader.peek() == JsonToken.STRING) fields.put(key, reader.nextString());
                else if (reader.peek() == JsonToken.BOOLEAN) fields.put(key, reader.nextBoolean());
                else throw new IllegalArgumentException("Tipo de campo de vestuario no válido.");
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Sólo se admite un comando de vestuario.");
        } catch (IOException | IllegalStateException invalid) {
            throw new IllegalArgumentException("El comando de vestuario no es JSON válido.", invalid);
        }
        String tool = string(fields, "tool");
        if (tool.equals("AVATAR_LIST") && fields.size() == 1) return list();
        if ((tool.equals("AVATAR_WEAR") || tool.equals("AVATAR_DELETE")) && fields.size() == 2) {
            Kind kind = tool.equals("AVATAR_WEAR") ? Kind.WEAR : Kind.DELETE;
            if (fields.containsKey("name")) return byName(kind, string(fields, "name"));
            return byId(kind, string(fields, "id"));
        }
        if (tool.equals("AVATAR_CREATE") && fields.size() == 6 && fields.get("wear") instanceof Boolean) {
            try {
                return create(string(fields, "name"), string(fields, "template"),
                        AvatarDesignSpec.Palette.valueOf(string(fields, "color")),
                        AvatarDesignSpec.Pattern.valueOf(string(fields, "pattern")), (Boolean) fields.get("wear"));
            } catch (IllegalArgumentException invalid) {
                throw new IllegalArgumentException("Revisa el nombre, plantilla, color y patrón permitidos.", invalid);
            }
        }
        throw new IllegalArgumentException("Herramienta de vestuario desconocida o campos incorrectos.");
    }
    private static String string(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (!(value instanceof String)) throw new IllegalArgumentException("Falta el campo de texto " + key + ".");
        return (String) value;
    }
}
