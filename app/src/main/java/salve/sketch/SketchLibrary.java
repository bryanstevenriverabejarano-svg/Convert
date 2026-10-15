package salve.sketch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repertorio de motivos que Salve sabe trazar (assets/sketch/library.json, generado por
 * scripts/build_sketch_library.py). Cada motivo son líneas en un lienzo de 0..1000.
 */
public final class SketchLibrary {
    public enum Place { SKY, GROUND, ANY }

    public static final class Motif {
        public final String id, name;
        public final Place place;
        public final float aspect;
        public final boolean self;
        /** De dónde salió: "builtin", "photo" (foto), "hand" (dibujado por Bryan) o "model" (lo compuso ella). */
        public final String source;
        final List<String> words;
        final float[][] strokes;
        Motif(String id, String name, Place place, float aspect, boolean self, List<String> words, float[][] strokes) {
            this(id, name, place, aspect, self, words, strokes, "builtin");
        }
        Motif(String id, String name, Place place, float aspect, boolean self, List<String> words, float[][] strokes,
              String source) {
            this.id = id; this.name = name; this.place = place; this.aspect = aspect; this.self = self;
            this.words = words; this.strokes = strokes; this.source = source == null ? "builtin" : source;
        }
        public int strokeCount() { return strokes.length; }
        public float[] stroke(int i) { return strokes[i].clone(); }
        public List<String> words() { return words; }
        public boolean learned() { return !"builtin".equals(source); }
    }

    private final Map<String, Motif> byId = new LinkedHashMap<>();
    private final Map<String, Motif> byWord = new LinkedHashMap<>();
    private int longestWord = 1;

    private SketchLibrary(List<Motif> motifs) {
        if (motifs.isEmpty()) throw new IllegalArgumentException("Biblioteca de trazos vacía");
        for (Motif motif : motifs) {
            if (byId.containsKey(motif.id)) continue;
            byId.put(motif.id, motif);
            byWord.putIfAbsent(motif.id, motif);
            for (String w : motif.words) {
                byWord.putIfAbsent(w, motif);
                longestWord = Math.max(longestWord, w.split(" ").length);
            }
        }
    }

    public static SketchLibrary parse(String json) {
        JsonArray motifs = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("motifs");
        if (motifs == null || motifs.size() == 0) throw new IllegalArgumentException("Biblioteca de trazos vacía");
        List<Motif> list = new ArrayList<>();
        for (JsonElement element : motifs) list.add(parseMotif(element.getAsJsonObject(), "builtin"));
        return new SketchLibrary(list);
    }

    /** Lo aprendido va delante: si Bryan le enseña «casa», su casa sustituye a la de serie. */
    public SketchLibrary withLearned(List<Motif> learned) {
        if (learned == null || learned.isEmpty()) return this;
        List<Motif> all = new ArrayList<>(learned);
        for (Motif m : byId.values()) if (!m.learned()) all.add(m);
        return new SketchLibrary(all);
    }

    static Motif parseMotif(JsonObject m, String defaultSource) {
        String id = m.get("id").getAsString();
        if (!id.matches("[a-z0-9]{2,32}")) throw new IllegalArgumentException("Identificador de motivo inválido");
        List<String> words = new ArrayList<>();
        for (JsonElement w : m.getAsJsonArray("words")) {
            String n = normalize(w.getAsString());
            if (!n.isEmpty()) words.add(n);
        }
        JsonArray raw = m.getAsJsonArray("strokes");
        if (raw == null || raw.size() == 0 || raw.size() > 400) throw new IllegalArgumentException("Motivo sin trazos: " + id);
        float[][] strokes = new float[raw.size()][];
        for (int i = 0; i < raw.size(); i++) {
            JsonArray s = raw.get(i).getAsJsonArray();
            if (s.size() < 4 || s.size() % 2 != 0 || s.size() > 4000) throw new IllegalArgumentException("Trazo inválido en " + id);
            strokes[i] = new float[s.size()];
            for (int k = 0; k < s.size(); k++) {
                float v = s.get(k).getAsFloat();
                if (!Float.isFinite(v) || v < -1 || v > 1001) throw new IllegalArgumentException("Coordenada fuera del lienzo en " + id);
                strokes[i][k] = v;
            }
        }
        String place = m.has("place") ? m.get("place").getAsString() : "any";
        return new Motif(id, m.get("name").getAsString(),
                "sky".equals(place) ? Place.SKY : "ground".equals(place) ? Place.GROUND : Place.ANY,
                m.has("aspect") ? m.get("aspect").getAsFloat() : 1f,
                m.has("self") && m.get("self").getAsBoolean(), Collections.unmodifiableList(words), strokes,
                m.has("source") ? m.get("source").getAsString() : defaultSource);
    }

    public Motif byId(String id) { return id == null ? null : byId.get(id); }
    public List<Motif> motifs() { return new ArrayList<>(byId.values()); }

    /** Busca una palabra o expresión ("arbol", "árboles", "arco iris"); null si no la conoce. */
    public Motif find(String words) {
        String key = normalize(words);
        if (key.isEmpty()) return null;
        Motif m = byWord.get(key);
        if (m == null && key.endsWith("es")) m = byWord.get(key.substring(0, key.length() - 2));
        if (m == null && key.endsWith("s")) m = byWord.get(key.substring(0, key.length() - 1));
        return m;
    }

    /**
     * Motivos mencionados en un texto, en orden de aparición y sin repetir.
     * @param storyWords si es true ignora las palabras demasiado comunes para ilustrar un relato.
     */
    public List<Mention> mentions(String text, boolean storyWords) {
        List<Mention> found = new ArrayList<>();
        if (text == null) return found;
        String[] tokens = normalize(text).split(" ");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < tokens.length; i++) {
            for (int size = Math.min(longestWord, tokens.length - i); size >= 1; size--) {
                StringBuilder phrase = new StringBuilder(tokens[i]);
                for (int k = 1; k < size; k++) phrase.append(' ').append(tokens[i + k]);
                String p = phrase.toString();
                if (p.length() < 3) continue;
                Motif m = find(p);
                if (m == null || (storyWords && (m.self || STORY_STOP.contains(p)))) continue;
                if (seen.add(m.id)) found.add(new Mention(m, i, p));
                i += size - 1;
                break;
            }
        }
        return found;
    }

    public static final class Mention {
        public final Motif motif;
        public final int token;
        public final String word;
        Mention(Motif motif, int token, String word) { this.motif = motif; this.token = token; this.word = word; }
    }

    /** Palabras que en un cuento casi nunca significan "dibuja esto". */
    private static final java.util.Set<String> STORY_STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "tu", "ti", "mundo", "tierra", "agua", "luz", "historia", "cuento", "viaje", "aventura", "gente",
            "persona", "amigo", "amiga", "amor", "carino", "magia", "espacio", "costa", "planta", "entrada",
            "llama", "fruta", "nave", "libros", "otono", "ave", "llover", "oceano"));

    public static String normalize(String value) {
        if (value == null) return "";
        String n = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9ñ ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
