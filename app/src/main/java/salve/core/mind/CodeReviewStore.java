package salve.core.mind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Su memoria de su propio código: por cada archivo, la huella de la versión que ya revisó, su nota
 * («qué hace, qué problemas vi, qué mejoraría») y una copia de ese texto para comparar con el próximo
 * parche. Así no vuelve a leerse entera: solo lo que cambia. Solo java.io (se prueba en la JVM).
 */
public final class CodeReviewStore {
    static final int MAX_NOTE = 1800, MAX_FINDINGS = 60;

    private final File dir, stateFile;
    private JsonObject state;

    public CodeReviewStore(File dir) {
        this.dir = dir;
        this.stateFile = new File(dir, "state.json");
        load();
    }

    private void load() {
        try {
            state = stateFile.isFile()
                    ? JsonParser.parseString(new String(Files.readAllBytes(stateFile.toPath()), StandardCharsets.UTF_8)).getAsJsonObject()
                    : new JsonObject();
        } catch (IOException | RuntimeException broken) { state = new JsonObject(); }
        if (!state.has("files")) state.add("files", new JsonObject());
        if (!state.has("night")) state.add("night", new JsonObject());
    }

    /** Escritura atómica: un corte a mitad no deja el estado roto. */
    public synchronized boolean save() {
        try {
            if (!dir.isDirectory() && !dir.mkdirs()) return false;
            File tmp = new File(dir, "state.json.tmp");
            Files.write(tmp.toPath(), state.toString().getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), stateFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (IOException | RuntimeException failed) { return false; }
    }

    private JsonObject files() { return state.getAsJsonObject("files"); }

    private JsonObject entry(String path, boolean create) {
        JsonElement e = files().get(path);
        if (e != null && e.isJsonObject()) return e.getAsJsonObject();
        if (!create) return null;
        JsonObject o = new JsonObject();
        files().add(path, o);
        return o;
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    /** ¿Ha visto alguna vez su código? (si no, todo es pendiente, no «parche nuevo»). */
    public synchronized boolean hasBaseline() { return state.has("baseline") && state.get("baseline").getAsBoolean(); }
    public synchronized void markBaseline() { state.addProperty("baseline", true); }

    public synchronized String seen(String path) { return str(entry(path, false), "seen"); }
    public synchronized void setSeen(String path, String hash) { entry(path, true).addProperty("seen", hash); }
    public synchronized String reviewed(String path) { return str(entry(path, false), "reviewed"); }

    /** Su nota sobre el archivo, o null si aún no lo ha revisado. */
    public synchronized String note(String path) { return str(entry(path, false), "note"); }

    public synchronized int reviewedCount() {
        int n = 0;
        for (String key : files().keySet()) if (str(files().getAsJsonObject(key), "reviewed") != null) n++;
        return n;
    }

    /** Lectura a medias de un archivo largo: hasta qué línea llegó y lo que lleva apuntado. */
    public synchronized int partialLine(String path, String hash) {
        JsonObject e = entry(path, false);
        if (e == null || !e.has("partial")) return 1;
        JsonObject p = e.getAsJsonObject("partial");
        return hash.equals(str(p, "hash")) ? p.get("line").getAsInt() : 1;
    }

    public synchronized String partialNotes(String path, String hash) {
        JsonObject e = entry(path, false);
        if (e == null || !e.has("partial")) return "";
        JsonObject p = e.getAsJsonObject("partial");
        return hash.equals(str(p, "hash")) ? str(p, "notes") : "";
    }

    public synchronized void setPartial(String path, String hash, int nextLine, String notes) {
        JsonObject p = new JsonObject();
        p.addProperty("hash", hash);
        p.addProperty("line", nextLine);
        p.addProperty("notes", notes);
        entry(path, true).add("partial", p);
    }

    /** Archivo terminado: su nota y la copia exacta de lo que revisó (base del próximo parche). */
    public synchronized void complete(String path, String hash, String note, String source, long at) {
        JsonObject e = entry(path, true);
        e.addProperty("reviewed", hash);
        e.addProperty("note", cut(note, MAX_NOTE));
        e.addProperty("at", at);
        e.remove("partial");
        try {
            File copy = new File(new File(dir, "src"), path);
            File parent = copy.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();
            Files.write(copy.toPath(), source.getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException ignored) { /* sin copia: el próximo parche se relee entero */ }
    }

    /** La versión que revisó, para comparar con el parche; null si no la tiene. */
    public String reviewedSource(String path) {
        try {
            File copy = new File(new File(dir, "src"), path);
            return copy.isFile() ? new String(Files.readAllBytes(copy.toPath()), StandardCharsets.UTF_8) : null;
        } catch (IOException | RuntimeException e) { return null; }
    }

    // ───────── la noche en curso: tiempo gastado y lo que irá al informe ─────────

    private JsonObject night() { return state.getAsJsonObject("night"); }

    /** Cambia de noche si hace falta; lo que quedó sin informar de la anterior se conserva. */
    public synchronized void startNight(String key) {
        if (key.equals(str(night(), "key"))) return;
        JsonObject next = new JsonObject();
        next.addProperty("key", key);
        next.addProperty("spent", 0L);
        JsonArray pending = night().has("findings") && !reportedNight() ? night().getAsJsonArray("findings") : new JsonArray();
        next.add("findings", pending);
        next.addProperty("reported", pending.size() == 0);
        state.add("night", next);
    }

    public synchronized long spentMs() { return night().has("spent") ? night().get("spent").getAsLong() : 0L; }
    public synchronized void addSpent(long ms) { night().addProperty("spent", spentMs() + Math.max(0, ms)); }

    public synchronized void addFinding(String text) {
        if (!night().has("findings")) night().add("findings", new JsonArray());
        JsonArray f = night().getAsJsonArray("findings");
        if (f.size() < MAX_FINDINGS) f.add(cut(text, 2500));
        night().addProperty("reported", false);
    }

    public synchronized List<String> findings() {
        List<String> out = new ArrayList<>();
        if (night().has("findings")) for (JsonElement e : night().getAsJsonArray("findings")) out.add(e.getAsString());
        return out;
    }

    public synchronized boolean reportedNight() {
        return !night().has("reported") || night().get("reported").getAsBoolean();
    }

    public synchronized void markReported() {
        night().addProperty("reported", true);
        night().add("findings", new JsonArray());
    }

    public static String hash(String text) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 12; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception impossible) { return Integer.toHexString(text.hashCode()); }
    }

    static String cut(String s, int n) {
        if (s == null) return "";
        return s.length() > n ? s.substring(0, n - 1) + "…" : s;
    }
}
