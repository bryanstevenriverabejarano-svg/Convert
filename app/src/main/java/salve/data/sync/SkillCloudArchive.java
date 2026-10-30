package salve.data.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import salve.core.autonomy.AutonomousToolLab;
import salve.core.autonomy.InductiveSkillLab;

/**
 * A bounded, data-only pCloud snapshot of an EXISTING verified symbolic skill
 * registry. It never compiles, installs or runs downloaded content.
 *
 * SHA-256 detects corruption and binds an immutable remote filename to its
 * payload; it is NOT a signature or proof of the cloud account's integrity.
 */
public final class SkillCloudArchive {
    public static final String ROOT = "/Salve/autonomy/verified";
    public static final String TOOLS = "tools", INDUCTIVE = "inductive";
    public static final int MAX_ARCHIVE_BYTES = 1024 * 1024;
    private static final int MAX_PAYLOAD_BYTES = 512 * 1024;

    private SkillCloudArchive() { }

    public static final class Snapshot {
        public final String kind, payload, digest;
        public final long createdAt;
        private Snapshot(String kind, String payload, String digest, long createdAt) {
            this.kind = kind; this.payload = payload; this.digest = digest;
            this.createdAt = createdAt;
        }
        public String remotePath() {
            return ROOT + "/" + kind + "-" + createdAt + "-" + digest + ".json";
        }
    }

    private static void kind(String value) {
        if (!TOOLS.equals(value) && !INDUCTIVE.equals(value))
            throw new IllegalArgumentException("Familia de habilidades no reconocida");
    }

    public static String sha256(String data) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(
                    data.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void validatePayload(String type, String payload) {
        kind(type);
        if (payload == null || payload.isEmpty()
                || payload.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES)
            throw new IllegalArgumentException("Registro de habilidades fuera de presupuesto");
        try {
            if (TOOLS.equals(type)) {
                // Same existing on-device registry decoder, not another rule engine.
                new AutonomousToolLab(new AutonomousToolLab.StateStore() {
                    public String read() { return payload; }
                    public void write(String ignored) { throw new IllegalStateException("Solo lectura"); }
                });
            } else {
                new InductiveSkillLab(new InductiveSkillLab.Store() {
                    public String read() { return payload; }
                    public void write(String ignored) { throw new IllegalStateException("Solo lectura"); }
                });
            }
        } catch (Exception invalid) {
            throw new IllegalArgumentException("Registro de habilidades incompatible", invalid);
        }
    }

    public static String pack(String type, String payload, long createdAt) {
        if (createdAt <= 0) throw new IllegalArgumentException("Fecha inválida");
        validatePayload(type, payload);
        JsonObject json = new JsonObject();
        json.addProperty("schema", 1);
        json.addProperty("kind", type);
        json.addProperty("created_at", createdAt);
        json.addProperty("sha256", sha256(payload));
        json.addProperty("payload", payload); // inert JSON string, never executed
        String encoded = json.toString();
        if (encoded.getBytes(StandardCharsets.UTF_8).length > MAX_ARCHIVE_BYTES)
            throw new IllegalArgumentException("Copia demasiado grande");
        return encoded;
    }

    public static Snapshot unpack(String envelope, String requestedRemotePath) {
        try {
            if (envelope == null || envelope.getBytes(StandardCharsets.UTF_8).length > MAX_ARCHIVE_BYTES)
                throw new IllegalArgumentException("Copia ausente o demasiado grande");
            JsonObject json = JsonParser.parseString(envelope).getAsJsonObject();
            if (json.size() != 5 || !json.has("schema") || json.get("schema").getAsInt() != 1)
                throw new IllegalArgumentException("Esquema desconocido");
            String type = json.get("kind").getAsString();
            kind(type);
            String payload = json.get("payload").getAsString();
            String digest = json.get("sha256").getAsString();
            long createdAt = json.get("created_at").getAsLong();
            if (createdAt <= 0 || !digest.matches("[a-f0-9]{64}")
                    || !MessageDigest.isEqual(digest.getBytes(StandardCharsets.US_ASCII),
                        sha256(payload).getBytes(StandardCharsets.US_ASCII)))
                throw new IllegalArgumentException("Huella de la copia incorrecta");
            Snapshot restored = new Snapshot(type, payload, digest, createdAt);
            if (requestedRemotePath != null && !restored.remotePath().equals(requestedRemotePath))
                throw new IllegalArgumentException("Ruta no coincide con el contenido");
            validatePayload(type, payload);
            return restored;
        } catch (IllegalArgumentException invalid) { throw invalid; }
        catch (RuntimeException malformed) {
            throw new IllegalArgumentException("Copia de habilidades no válida", malformed);
        }
    }

    /**
     * A remote catalog is never activated by synchronization. Stage it in a
     * separate no-backup inbox for explicit owner review in a later workflow.
     * Neither an existing skill file nor an earlier staged snapshot is replaced.
     */
    public static boolean stageIfMissing(java.io.File active, java.io.File inbox,
            String envelope, String path) throws java.io.IOException {
        if (active == null || inbox == null || active.exists() || inbox.exists())
            return false;
        if (!recognizedPath(path)) return false;
        Snapshot snapshot = unpack(envelope, path);
        String expected = TOOLS.equals(snapshot.kind) ? "tools.json" : "inductive-skills.json";
        if (!expected.equals(active.getName()) || !expected.equals(inbox.getName()))
            return false;
        new salve.core.autonomy.FileToolStateStore(inbox).write(envelope);
        return true;
    }

    /** A bounded chooser; names are timestamped, but content still needs validation. */
    public static java.util.List<String> candidates(String type, java.util.List<String> paths) {
        kind(type);
        java.util.List<String> matches = new java.util.ArrayList<>();
        if (paths == null || paths.size() > 256) return matches;
        for (String path : paths) if (recognizedPath(path)
                && path.startsWith(ROOT + "/" + type + "-")) matches.add(path);
        matches.sort((a,b) -> Long.compare(timestamp(b), timestamp(a)));
        return matches;
    }

    private static long timestamp(String path) {
        try {
            int start = path.lastIndexOf('/') + 1;
            int first = path.indexOf('-', start);
            int second = path.indexOf('-', first + 1);
            return Long.parseLong(path.substring(first + 1, second));
        } catch (RuntimeException invalid) { return 0; }
    }

    /** Strict known filename, no arbitrary remote paths, folders or executable extensions. */
    public static boolean recognizedPath(String path) {
        return path != null && path.matches(
                "/Salve/autonomy/verified/(tools|inductive)-[1-9][0-9]{0,15}-[a-f0-9]{64}\\.json");
    }
}
