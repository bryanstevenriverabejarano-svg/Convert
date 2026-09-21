package salve.avatar;

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One bounded atomic file. State changes only after a successful disk replacement. Single IO owner. */
public final class AvatarWardrobeRepository {
    public static final int MAX_DESIGNS = 24;
    private static final long MAX_BYTES = 32_768;
    private final File file;
    private List<AvatarDesignSpec> designs = new ArrayList<>();
    private String selectedId = AvatarDesignSpec.ORIGINAL_ID;
    public AvatarWardrobeRepository(File file) throws IOException {
        this.file = file;
        if (file.exists()) read();
    }
    public List<AvatarDesignSpec> list() {
        List<AvatarDesignSpec> result = new ArrayList<>(); result.add(AvatarDesignSpec.original()); result.addAll(designs);
        return Collections.unmodifiableList(result);
    }
    public AvatarDesignSpec selected() { return find(selectedId); }
    public AvatarDesignSpec find(String id) {
        if (AvatarDesignSpec.ORIGINAL_ID.equals(id)) return AvatarDesignSpec.original();
        for (AvatarDesignSpec design : designs) if (design.id.equals(id)) return design;
        throw new IllegalArgumentException("No existe ese diseño guardado.");
    }
    public AvatarDesignSpec create(AvatarDesignRequest request) throws IOException {
        if (request.kind != AvatarDesignRequest.Kind.CREATE) throw new IllegalArgumentException("No es una creación de diseño.");
        if (designs.size() >= MAX_DESIGNS) throw new IllegalArgumentException("El armario admite 24 diseños. Elimina uno antes de crear otro.");
        for (AvatarDesignSpec existing : list())
            if (AvatarDesignSpec.normalizedName(existing.name).equals(AvatarDesignSpec.normalizedName(request.name)))
                throw new IllegalArgumentException("Ya hay un diseño con ese nombre. Usa otro para conservar ambos.");
        AvatarDesignSpec design = new AvatarDesignSpec(UUID.randomUUID().toString(), request.name, request.template, request.palette, request.pattern);
        List<AvatarDesignSpec> next = new ArrayList<>(designs); next.add(design);
        persist(next, request.wear ? design.id : selectedId);
        return design;
    }
    public AvatarDesignSpec select(String id) throws IOException {
        AvatarDesignSpec next = find(id); persist(designs, id); return next;
    }
    public void delete(String id) throws IOException {
        if (AvatarDesignSpec.ORIGINAL_ID.equals(id)) throw new IllegalArgumentException("El vestido original siempre se conserva.");
        find(id);
        List<AvatarDesignSpec> next = new ArrayList<>(designs); next.removeIf(d -> d.id.equals(id));
        persist(next, selectedId.equals(id) ? AvatarDesignSpec.ORIGINAL_ID : selectedId);
    }
    private void persist(List<AvatarDesignSpec> next, String selected) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>(); root.put("version", 1); root.put("selected", selected);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AvatarDesignSpec design : next) {
            Map<String, Object> row = new LinkedHashMap<>(); row.put("id", design.id); row.put("name", design.name);
            row.put("template", design.template); row.put("color", design.palette.name()); row.put("pattern", design.pattern.name()); rows.add(row);
        }
        root.put("designs", rows);
        byte[] bytes = new Gson().toJson(root).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("El archivo del armario supera su límite.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("No pude abrir la carpeta del armario.");
        File temporary = File.createTempFile("wardrobe-", ".tmp", parent);
        try {
            try (FileOutputStream stream = new FileOutputStream(temporary)) { stream.write(bytes); stream.getFD().sync(); }
            // App-private storage supports atomic rename. If it fails, keep both the old disk and memory state.
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            designs = new ArrayList<>(next); selectedId = selected;
        } finally { if (temporary.exists()) temporary.delete(); }
    }
    private void read() throws IOException {
        if (file.length() > MAX_BYTES) throw new IOException("El archivo del armario supera su límite.");
        List<AvatarDesignSpec> loaded = new ArrayList<>();
        String selected = null;
        try (JsonReader reader = new JsonReader(new StringReader(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)))) {
            reader.setStrictness(Strictness.STRICT); reader.beginObject();
            java.util.Set<String> keys = new java.util.HashSet<>();
            while (reader.hasNext()) {
                String key = reader.nextName();
                if (!keys.add(key)) throw new IOException("Campo duplicado en el armario.");
                switch (key) {
                    case "version":
                        if (reader.peek() != JsonToken.NUMBER || reader.nextInt() != 1) throw new IOException("Versión de armario no compatible.");
                        break;
                    case "selected":
                        if (reader.peek() != JsonToken.STRING) throw new IOException("Selección de armario no válida.");
                        selected = reader.nextString(); break;
                    case "designs":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (loaded.size() >= MAX_DESIGNS) throw new IOException("Demasiados diseños guardados.");
                            Map<String, String> row = new LinkedHashMap<>(); reader.beginObject();
                            while (reader.hasNext()) {
                                String field = reader.nextName();
                                if (row.containsKey(field) || reader.peek() != JsonToken.STRING) throw new IOException("Diseño guardado no válido.");
                                row.put(field, reader.nextString());
                            }
                            reader.endObject();
                            if (row.size() != 5) throw new IOException("Campos de diseño no válidos.");
                            AvatarDesignSpec design = new AvatarDesignSpec(row.get("id"), row.get("name"), row.get("template"),
                                    AvatarDesignSpec.Palette.valueOf(row.get("color")), AvatarDesignSpec.Pattern.valueOf(row.get("pattern")));
                            if (design.id.equals(AvatarDesignSpec.ORIGINAL_ID)) throw new IOException("El original no se sustituye.");
                            if (AvatarDesignSpec.normalizedName(design.name).equals(AvatarDesignSpec.normalizedName(AvatarDesignSpec.original().name)))
                                throw new IOException("El nombre del diseño original se conserva.");
                            for (AvatarDesignSpec old : loaded)
                                if (old.id.equals(design.id) || AvatarDesignSpec.normalizedName(old.name).equals(AvatarDesignSpec.normalizedName(design.name)))
                                    throw new IOException("Diseño duplicado en el armario.");
                            loaded.add(design);
                        }
                        reader.endArray(); break;
                    default: throw new IOException("Campo desconocido en el armario.");
                }
            }
            reader.endObject();
            if (keys.size() != 3 || selected == null || reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("Armario incompleto.");
            designs = loaded; selectedId = selected; find(selected);
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException invalid) {
            throw new IOException("El archivo del armario no es válido; se ha conservado para recuperarlo.", invalid);
        }
    }
}
