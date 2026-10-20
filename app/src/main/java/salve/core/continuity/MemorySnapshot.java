package salve.core.continuity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Instantánea de la memoria de Salve: su base de datos de recuerdos y los archivos de su mente
 * (modelo de sí misma, acciones, lecciones, habilidades, objetivos, dibujos aprendidos), en un zip
 * con manifiesto y sumas SHA-256. Sin Android.
 *
 * Al leer una instantánea (que llega de fuera del teléfono) se valida todo antes de tocar nada:
 * nombres en lista blanca y sin rutas trampa, límites de tamaño contra bombas zip, cabecera SQLite,
 * versión de esquema que la app sabe abrir, JSON válido y sumas que coinciden con el manifiesto.
 */
public final class MemorySnapshot {
    public static final String DATABASE = "db/memoria.db", MANIFEST = "manifest.json";
    public static final int FORMAT = 1;
    public static final long MAX_ENTRY = 96L * 1024 * 1024, MAX_TOTAL = 160L * 1024 * 1024;
    public static final int MAX_ENTRIES = 200;
    /**
     * db/memoria.db, nobackup/mind/x.json, nobackup/autonomy/x.json, files/sketch/x.json. Los nombres
     * admiten guion: las habilidades verificadas viven en autonomy/inductive-skills.json.
     */
    private static final Pattern NAME = Pattern.compile(
            "db/memoria\\.db|nobackup/(?:mind|autonomy)/[a-z0-9][a-z0-9_-]{0,47}\\.json|files/sketch/[a-z0-9][a-z0-9_-]{0,47}\\.json");
    private static final byte[] SQLITE = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);

    private MemorySnapshot() { }

    public static boolean allowed(String name) { return name != null && NAME.matcher(name).matches(); }

    /** Empaqueta. {@code entries}: nombre permitido → contenido. Debe incluir la base de datos. */
    public static byte[] pack(Map<String, byte[]> entries, long createdAt, int schemaVersion) throws IOException {
        if (!entries.containsKey(DATABASE)) throw new IllegalArgumentException("Falta la base de datos de recuerdos");
        JsonObject manifest = new JsonObject();
        manifest.addProperty("salve", "memoria"); manifest.addProperty("format", FORMAT);
        manifest.addProperty("created_at", createdAt); manifest.addProperty("schema", schemaVersion);
        JsonArray list = new JsonArray();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.setLevel(6);
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                if (!allowed(e.getKey())) throw new IllegalArgumentException("Archivo no permitido: " + e.getKey());
                zip.putNextEntry(new ZipEntry(e.getKey())); zip.write(e.getValue()); zip.closeEntry();
                JsonObject item = new JsonObject();
                item.addProperty("name", e.getKey()); item.addProperty("size", e.getValue().length); item.addProperty("sha256", sha256(e.getValue()));
                list.add(item);
            }
            manifest.add("entries", list);
            zip.putNextEntry(new ZipEntry(MANIFEST)); zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8)); zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    /** Resultado de una instantánea ya validada. */
    public static final class Contents {
        public final Map<String, byte[]> entries; public final long createdAt; public final int schema;
        Contents(Map<String, byte[]> entries, long createdAt, int schema) { this.entries = entries; this.createdAt = createdAt; this.schema = schema; }
        public int memoriesFiles() { return entries.size() - 1; }
    }

    /**
     * Desempaqueta y valida. {@code minSchema..maxSchema}: versiones de la base de datos que esta
     * app sabe abrir (con sus migraciones). Lanza IllegalArgumentException si algo no encaja.
     */
    public static Contents unpack(byte[] zipBytes, int minSchema, int maxSchema) {
        return unpack(zipBytes, minSchema, maxSchema, MAX_ENTRY, MAX_TOTAL);
    }

    static Contents unpack(byte[] zipBytes, int minSchema, int maxSchema, long maxEntry, long maxTotal) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        byte[] manifestBytes = null;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory() || (!allowed(name) && !MANIFEST.equals(name))) throw new IllegalArgumentException("Archivo no permitido en la copia: " + name);
                if (entries.containsKey(name) || (MANIFEST.equals(name) && manifestBytes != null)) throw new IllegalArgumentException("Archivo repetido en la copia");
                if (entries.size() >= MAX_ENTRIES) throw new IllegalArgumentException("Demasiados archivos en la copia");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[65536]; int n;
                while ((n = zip.read(buffer)) > 0) {
                    total += n;
                    if (out.size() + n > maxEntry || total > maxTotal) throw new IllegalArgumentException("La copia es demasiado grande");
                    out.write(buffer, 0, n);
                }
                if (MANIFEST.equals(name)) manifestBytes = out.toByteArray(); else entries.put(name, out.toByteArray());
            }
        } catch (IOException damaged) { throw new IllegalArgumentException("La copia está dañada"); }
        if (manifestBytes == null) throw new IllegalArgumentException("A la copia le falta el manifiesto");
        JsonObject manifest;
        long createdAt; int schema;
        try {
            manifest = JsonParser.parseString(new String(manifestBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!"memoria".equals(manifest.get("salve").getAsString()) || manifest.get("format").getAsInt() != FORMAT)
                throw new IllegalArgumentException("Copia de otro formato");
            createdAt = manifest.get("created_at").getAsLong(); schema = manifest.get("schema").getAsInt();
            JsonArray list = manifest.getAsJsonArray("entries");
            if (list.size() != entries.size()) throw new IllegalArgumentException("El manifiesto no coincide con la copia");
            for (int i = 0; i < list.size(); i++) {
                JsonObject item = list.get(i).getAsJsonObject();
                byte[] data = entries.get(item.get("name").getAsString());
                if (data == null || data.length != item.get("size").getAsLong() || !sha256(data).equals(item.get("sha256").getAsString()))
                    throw new IllegalArgumentException("Un archivo de la copia no coincide con su suma");
            }
        } catch (IllegalArgumentException invalid) { throw invalid; }
        catch (RuntimeException malformed) { throw new IllegalArgumentException("Manifiesto ilegible"); }

        byte[] db = entries.get(DATABASE);
        if (db == null || db.length < 100 || !Arrays.equals(Arrays.copyOf(db, 16), SQLITE))
            throw new IllegalArgumentException("La base de datos de la copia no es válida");
        int userVersion = userVersion(db);
        if (userVersion != schema || userVersion < minSchema || userVersion > maxSchema)
            throw new IllegalArgumentException("La copia es de una versión de memoria que esta app no sabe abrir (" + userVersion + ")");
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            if (!e.getKey().endsWith(".json")) continue;
            try { JsonParser.parseString(new String(e.getValue(), StandardCharsets.UTF_8)); }
            catch (RuntimeException bad) { throw new IllegalArgumentException("Un archivo de la mente está dañado: " + e.getKey()); }
        }
        return new Contents(entries, createdAt, schema);
    }

    /** PRAGMA user_version: 4 bytes big-endian en el desplazamiento 60 de la cabecera SQLite (Room guarda ahí su versión). */
    public static int userVersion(byte[] db) {
        return ((db[60] & 0xff) << 24) | ((db[61] & 0xff) << 16) | ((db[62] & 0xff) << 8) | (db[63] & 0xff);
    }

    static String sha256(byte[] data) {
        try {
            StringBuilder out = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) out.append(String.format("%02x", b));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
