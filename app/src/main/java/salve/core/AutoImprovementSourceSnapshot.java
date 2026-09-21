package salve.core;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Exact, operator-exported source context. Hashes check integrity, not authorship or APK equivalence. */
public final class AutoImprovementSourceSnapshot {
    public static final String RELATIVE_PATH = "auto-improvement/source-snapshot.json";
    public static final int MAX_SOURCE_CHARS = 8_000;
    public static final int MAX_FILES = 8;
    public static final int MAX_BYTES = 256_000;
    public final String revision;
    private final Map<String, Source> sources;

    private AutoImprovementSourceSnapshot(String revision, Map<String, Source> sources) {
        this.revision = revision;
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
    }

    public List<String> classNames() { return new ArrayList<>(sources.keySet()); }
    public Source source(String className) { return sources.get(className); }

    public static AutoImprovementSourceSnapshot read(File file) throws IOException {
        if (!file.isFile() || file.length() > MAX_BYTES) throw new IOException("Falta una instantánea de fuente válida.");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int size;
            while ((size = input.read(buffer)) != -1) {
                if (bytes.size() + size > MAX_BYTES) throw new IOException("La instantánea supera el límite.");
                bytes.write(buffer, 0, size);
            }
        }
        String json = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        try { return parse(json); }
        catch (IllegalArgumentException error) { throw new IOException("La instantánea de fuente es inválida.", error); }
    }

    public static AutoImprovementSourceSnapshot parse(String json) {
        if (json == null || json.length() > MAX_BYTES) throw new IllegalArgumentException("Instantánea demasiado grande.");
        String revision = null;
        boolean version = false;
        Map<String, Source> sources = null;
        List<String> seen = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            reader.beginObject();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (seen.contains(field)) throw new IllegalArgumentException("Campo duplicado.");
                seen.add(field);
                if (field.equals("schemaVersion")) {
                    if (reader.peek() != JsonToken.NUMBER || !reader.nextString().equals("1")) throw new IllegalArgumentException();
                    version = true;
                } else if (field.equals("revision")) {
                    revision = string(reader);
                } else if (field.equals("files")) {
                    sources = new LinkedHashMap<>();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        if (sources.size() == MAX_FILES) throw new IllegalArgumentException("Demasiados archivos.");
                        Map<String, String> values = new LinkedHashMap<>();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String key = reader.nextName();
                            if (values.put(key, string(reader)) != null) throw new IllegalArgumentException("Campo duplicado.");
                        }
                        reader.endObject();
                        if (values.size() != 3) throw new IllegalArgumentException("Campos de archivo inválidos.");
                        Source source = new Source(values.get("path"), values.get("source"), values.get("sha256"));
                        if (sources.put(source.className, source) != null) throw new IllegalArgumentException("Archivo duplicado.");
                    }
                    reader.endArray();
                } else throw new IllegalArgumentException("Campo desconocido.");
            }
            reader.endObject();
            if (reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Texto sobrante.");
        } catch (IOException | IllegalStateException error) { throw new IllegalArgumentException("JSON inválido.", error); }
        if (!version || revision == null || !revision.matches("[a-f0-9]{40}") || sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("Instantánea incompleta.");
        }
        return new AutoImprovementSourceSnapshot(revision, sources);
    }

    private static String string(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.STRING) throw new IllegalArgumentException("Se esperaba texto.");
        return reader.nextString();
    }

    public static final class Source {
        public final String path, className, code, sha256;
        private Source(String path, String code, String sha256) {
            if (path == null || !path.matches("app/src/main/java/salve/core/[A-Za-z_][A-Za-z0-9_]*\\.java")) {
                throw new IllegalArgumentException("Ruta fuera de los archivos permitidos.");
            }
            if (code == null || code.trim().isEmpty() || code.length() > MAX_SOURCE_CHARS || code.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("Fuente ausente o fuera del presupuesto; no se trunca.");
            }
            if (sha256 == null || !sha256.matches("[a-f0-9]{64}") || !sha256.equals(hash(code))) {
                throw new IllegalArgumentException("El SHA-256 no coincide con la fuente.");
            }
            this.path = path; this.code = code; this.sha256 = sha256;
            this.className = path.substring(path.lastIndexOf('/') + 1, path.length() - 5);
        }
    }

    static String hash(String code) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
