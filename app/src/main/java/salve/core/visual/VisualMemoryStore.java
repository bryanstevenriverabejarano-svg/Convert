package salve.core.visual;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Private, bounded archive. Files are never chosen from model-generated paths. */
public final class VisualMemoryStore {
    public static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_RECORDS = 200;
    private static final long MAX_ARCHIVE_BYTES = 100L * 1024 * 1024;
    private static final Object LOCK = new Object();
    private final File root;
    private final Gson gson = new Gson();

    public VisualMemoryStore(File root) { this.root = root; }

    public File imageFile(String id) {
        if (!VisualMemoryRecord.validId(id)) throw new IllegalArgumentException("Identificador de foto inválido");
        return new File(root, id + ".jpg");
    }

    public VisualMemoryRecord save(VisualMemoryRecord record, byte[] jpeg) throws IOException {
        synchronized (LOCK) {
            VisualMemoryRecord previous = read(record.id);
            if (previous != null && previous.revision > record.revision) record = previous;
            if (!root.isDirectory() && !root.mkdirs()) throw new IOException("No se pudo crear el archivo visual");
            File image = imageFile(record.id);
            if (jpeg != null && (jpeg.length == 0 || jpeg.length > MAX_IMAGE_BYTES))
                throw new IOException("Foto demasiado grande");
            if (!image.isFile()) {
                if (jpeg == null) throw new IOException("Falta la imagen original");
                File[] files = root.listFiles();
                long size = 0; int count = 0;
                if (files != null) for (File file : files) {
                    size += file.length();
                    if (file.getName().endsWith(".json")) count++;
                }
                if ((previous == null && count >= MAX_RECORDS) || size + jpeg.length > MAX_ARCHIVE_BYTES)
                    throw new IOException("El archivo visual ha alcanzado su límite de espacio");
                atomicWrite(image, jpeg);
            }
            atomicWrite(new File(root, record.id + ".json"), gson.toJson(record).getBytes(StandardCharsets.UTF_8));
            return record;
        }
    }

    public VisualMemoryRecord read(String id) throws IOException {
        synchronized (LOCK) {
            imageFile(id); // Validate before constructing any path.
            File metadata = new File(root, id + ".json");
            if (!metadata.isFile()) return null;
            if (metadata.length() > 65536) throw new IOException("Metadatos demasiado grandes");
            try {
                VisualMemoryRecord raw = gson.fromJson(new String(Files.readAllBytes(metadata.toPath()),
                        StandardCharsets.UTF_8), VisualMemoryRecord.class);
                VisualMemoryRecord record = validated(raw);
                if (!id.equals(record.id)) throw new IOException("Foto inconsistente");
                return record;
            } catch (RuntimeException invalid) { throw new IOException("Metadatos de foto inválidos", invalid); }
        }
    }

    public static VisualMemoryRecord validated(VisualMemoryRecord raw) {
        if (raw == null) throw new IllegalArgumentException("Falta la foto");
        return new VisualMemoryRecord(raw.id, raw.createdAt, raw.revision, raw.question, raw.analysis,
                raw.personName, raw.relationship, raw.position, raw.localOnly);
    }

    public List<VisualMemoryRecord> list() {
        synchronized (LOCK) {
            List<VisualMemoryRecord> records = new ArrayList<>();
            File[] files = root.listFiles();
            if (files != null) for (File file : files) {
                String name = file.getName();
                if (!name.endsWith(".json")) continue;
                String id = name.substring(0, name.length() - 5);
                if (!VisualMemoryRecord.validId(id)) continue;
                try { VisualMemoryRecord r = read(id); if (r != null) records.add(r); }
                catch (IOException ignored) { /* One corrupt entry must not hide the rest. */ }
            }
            records.sort(Comparator.comparingLong((VisualMemoryRecord r) -> r.createdAt).reversed()
                    .thenComparing(r -> r.id));
            return records;
        }
    }

    /** Only explicit, unambiguous recall selects pixels; other questions use retrieved summaries. */
    public VisualMemoryRecord recall(String query) {
        String text = VisualMemoryRecord.normalize(query);
        if (!text.matches(".*\\b(foto|imagen|fotografia|selfie)\\b.*")) return null;
        List<VisualMemoryRecord> records = list();
        if (records.isEmpty()) return null;
        List<VisualMemoryRecord> matches = new ArrayList<>();
        for (VisualMemoryRecord record : records) {
            String name = VisualMemoryRecord.normalize(record.personName);
            if (text.contains(record.id) || (!name.isEmpty() && java.util.regex.Pattern.compile(
                    "(?<![\\p{L}\\p{N}])" + java.util.regex.Pattern.quote(name) + "(?![\\p{L}\\p{N}])").matcher(text).find()))
                matches.add(record);
        }
        boolean latest = text.matches(".*\\b(ultima foto|ultima imagen|ultima fotografia)\\b.*");
        if (latest && !matches.isEmpty()) return matches.get(0);
        if (latest && text.matches(".*\\bultima (foto|imagen|fotografia)[?!. ]*$")) return records.get(0);
        if (matches.size() == 1) return matches.get(0);
        if (matches.isEmpty() && records.size() == 1
                && text.matches(".*\\b(recuerdas? (mi|la|esa) (foto|imagen|fotografia)|esa foto|esa imagen|mi foto)[?!. ]*$")) return records.get(0);
        return null;
    }

    private static void atomicWrite(File file, byte[] bytes) throws IOException {
        File temporary = File.createTempFile("visual-", ".tmp", file.getParentFile());
        try {
            Files.write(temporary.toPath(), bytes);
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { temporary.delete(); }
    }
}
