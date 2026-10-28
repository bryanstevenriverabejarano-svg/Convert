package salve.sketch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Lo que Salve ha aprendido a dibujar: de una foto ("esto es un mueble"), de un dibujo hecho con el
 * dedo, o compuesto por ella misma. Se guarda en files/sketch/learned.json y se suma a su repertorio.
 * Sin rutas ni código: sólo nombres y líneas en el lienzo 0..1000.
 */
public final class LearnedSketches {
    public static final int MAX_MOTIFS = 80, MAX_POINTS = 2600;
    private final File file;
    private List<SketchLibrary.Motif> cache;

    public LearnedSketches(File directory) {
        this.file = new File(directory, "learned.json");
    }

    public synchronized List<SketchLibrary.Motif> all() {
        if (cache != null) return cache;
        List<SketchLibrary.Motif> out = new ArrayList<>();
        if (file.isFile()) {
            try {
                JsonArray list = JsonParser.parseString(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonArray("motifs");
                for (JsonElement e : list) {
                    try { out.add(SketchLibrary.parseMotif(e.getAsJsonObject(), "photo")); }
                    catch (RuntimeException skip) { /* un motivo dañado no borra los demás */ }
                }
            } catch (IOException | RuntimeException damaged) { /* se empieza de cero sin perder el archivo */ }
        }
        return cache = Collections.unmodifiableList(out);
    }

    /**
     * Aprende (o vuelve a aprender) un motivo. Las líneas vienen normalizadas a 0..1000.
     * @param spokenName como lo dijo Bryan ("un mueble", "mi taza")
     * @param source "photo", "hand" o "model"
     */
    public synchronized SketchLibrary.Motif learn(String spokenName, List<float[]> strokes, float aspect, String source)
            throws IOException {
        return learn(spokenName, strokes, aspect, source, null);
    }

    /** @param parts partes con sentido sobre los índices de {@code strokes} (o null) */
    public synchronized SketchLibrary.Motif learn(String spokenName, List<float[]> strokes, float aspect, String source,
                                                  List<SketchLibrary.Part> parts) throws IOException {
        if (strokes != null && parts != null && !parts.isEmpty()) {
            // Quita trazos vacíos sin descolocar las partes.
            List<float[]> clean = new ArrayList<>();
            int[] newIndex = new int[strokes.size() + 1];
            for (int i = 0; i < strokes.size(); i++) {
                newIndex[i] = clean.size();
                float[] st = strokes.get(i);
                if (st != null && st.length >= 4) clean.add(st);
            }
            newIndex[strokes.size()] = clean.size();
            List<SketchLibrary.Part> moved = new ArrayList<>();
            for (SketchLibrary.Part p : parts) {
                if (p.from < 0 || p.to > strokes.size() || p.to <= p.from) continue;
                int from = newIndex[p.from], to = newIndex[p.to];
                if (to > from) moved.add(new SketchLibrary.Part(p.name, p.description, from, to));
            }
            strokes = clean;
            parts = moved;
        }
        String name = displayName(spokenName);
        String key = SketchLibrary.normalize(stripArticle(name));
        if (key.length() < 2 || key.length() > 40) throw new IllegalArgumentException("Nombre de dibujo no válido");
        if (strokes == null || strokes.isEmpty()) throw new IllegalArgumentException("No hay líneas que aprender");
        List<float[]> kept = budget(strokes);
        String id = slug(key);
        List<SketchLibrary.Motif> current = new ArrayList<>(all());
        current.removeIf(m -> m.id.equals(id));
        while (current.size() >= MAX_MOTIFS) current.remove(current.size() - 1); // olvida lo más antiguo
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("place", "any");
        json.addProperty("aspect", Float.isFinite(aspect) && aspect > 0 ? Math.max(.2f, Math.min(5f, aspect)) : 1f);
        json.addProperty("source", "hand".equals(source) || "model".equals(source) || "web".equals(source)
                || "icon".equals(source) ? source : "photo");
        json.addProperty("learnedAt", System.currentTimeMillis());
        JsonArray words = new JsonArray();
        words.add(key);
        // «sillón rojo» también responde a «sillón».
        if (key.contains(" ") && key.indexOf(' ') > 2) words.add(key.substring(0, key.indexOf(' ')));
        json.add("words", words);
        JsonArray lines = new JsonArray();
        for (float[] s : kept) {
            JsonArray a = new JsonArray();
            for (float v : s) a.add(Math.round(Math.max(0, Math.min(1000, v)) * 10f) / 10f);
            lines.add(a);
        }
        json.add("strokes", lines);
        if (parts != null && !parts.isEmpty() && kept.size() == strokes.size()) json.add("parts", partsJson(parts));
        SketchLibrary.Motif motif = SketchLibrary.parseMotif(json, "photo");
        current.add(0, motif);
        write(current, json);
        cache = Collections.unmodifiableList(current);
        return motif;
    }

    public synchronized boolean forget(String spokenName) throws IOException {
        String id = slug(SketchLibrary.normalize(stripArticle(displayName(spokenName))));
        List<SketchLibrary.Motif> current = new ArrayList<>(all());
        if (!current.removeIf(m -> m.id.equals(id))) return false;
        write(current, null);
        cache = Collections.unmodifiableList(current);
        return true;
    }

    private void write(List<SketchLibrary.Motif> motifs, JsonObject fresh) throws IOException {
        JsonArray list = new JsonArray();
        for (SketchLibrary.Motif m : motifs) {
            if (fresh != null && m.id.equals(fresh.get("id").getAsString())) { list.add(fresh); continue; }
            list.add(toJson(m));
        }
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        root.add("motifs", list);
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IOException("No se pudo crear " + dir);
        File tmp = new File(dir, file.getName() + ".tmp");
        Files.write(tmp.toPath(), root.toString().getBytes(StandardCharsets.UTF_8));
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private static JsonObject toJson(SketchLibrary.Motif m) {
        JsonObject json = new JsonObject();
        json.addProperty("id", m.id);
        json.addProperty("name", m.name);
        json.addProperty("place", m.place == SketchLibrary.Place.SKY ? "sky" : m.place == SketchLibrary.Place.GROUND ? "ground" : "any");
        json.addProperty("aspect", m.aspect);
        json.addProperty("source", m.source);
        JsonArray words = new JsonArray();
        for (String w : m.words) words.add(w);
        json.add("words", words);
        JsonArray lines = new JsonArray();
        for (float[] s : m.strokes) { JsonArray a = new JsonArray(); for (float v : s) a.add(v); lines.add(a); }
        json.add("strokes", lines);
        if (!m.parts.isEmpty()) json.add("parts", partsJson(m.parts));
        return json;
    }

    static JsonArray partsJson(List<SketchLibrary.Part> parts) {
        JsonArray out = new JsonArray();
        for (SketchLibrary.Part p : parts) {
            JsonObject o = new JsonObject();
            o.addProperty("name", p.name);
            o.addProperty("description", p.description);
            o.addProperty("from", p.from);
            o.addProperty("to", p.to);
            out.add(o);
        }
        return out;
    }

    /** Recorta puntos (quitando los trazos más cortos primero) para que la DFT siga siendo ligera. */
    static List<float[]> budget(List<float[]> strokes) {
        List<float[]> out = new ArrayList<>();
        int total = 0;
        for (float[] s : strokes) if (s != null && s.length >= 4) { out.add(s); total += s.length / 2; }
        if (total <= MAX_POINTS) return out;
        float step = total / (float) MAX_POINTS;
        List<float[]> thinned = new ArrayList<>();
        for (float[] s : out) {
            int n = s.length / 2, m = Math.max(2, Math.round(n / step));
            float[] r = new float[2 * m];
            for (int i = 0; i < m; i++) {
                int k = Math.min(n - 1, Math.round(i * (n - 1) / (float) (m - 1)));
                r[2 * i] = s[2 * k]; r[2 * i + 1] = s[2 * k + 1];
            }
            thinned.add(r);
        }
        return thinned;
    }

    static String displayName(String spoken) {
        String n = spoken == null ? "" : spoken.trim().replaceAll("[.,;:!?¡¿\"«»]+", " ").replaceAll("\\s+", " ").trim();
        if (n.length() > 48) n = n.substring(0, 48).trim();
        return n.toLowerCase(Locale.ROOT);
    }

    static String stripArticle(String name) {
        return name.replaceFirst("^(un|una|unos|unas|el|la|los|las|mi|mis|tu|tus|este|esta|ese|esa)\\s+", "");
    }

    static String slug(String key) {
        String s = key.replaceAll("[^a-z0-9]", "");
        if (s.length() < 2) s = "dibujo" + Math.abs(key.hashCode() % 1000);
        return s.length() > 32 ? s.substring(0, 32) : s;
    }
}
