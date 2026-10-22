package salve.core.schema;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import salve.core.mind.SelfModelJournal;

/**
 * Sus conceptos anclados: cada concepto apunta a 1-3 esquemas del catálogo con sus papeles
 * rellenos («economía»: SOPORTE, sostenido = la economía, soporte = la confianza y el trabajo).
 * Las palabras de cualquier idioma («caer», «fall», «derrumbe») apuntan al mismo concepto.
 * Lo que no ha anclado todavía queda pendiente para su sueño.
 */
public final class ConceptStore {
    public static final class Mapping {
        public String schema, a, b;
    }

    public static final class Concept {
        public String name;
        public List<String> words = new ArrayList<>();
        public List<Mapping> schemas = new ArrayList<>();
        public String source;
        public long at;

        public List<SceneSchemas.Instance> instances() {
            List<SceneSchemas.Instance> out = new ArrayList<>();
            for (Mapping m : schemas) {
                ImageSchemas.Schema s = ImageSchemas.byName(m.schema);
                if (s != null) out.add(new SceneSchemas.Instance(s, m.a, m.b));
            }
            return out;
        }
    }

    static final class State {
        Map<String, Concept> concepts = new LinkedHashMap<>();
        List<String> pending = new ArrayList<>();
    }

    private static final Gson GSON = new Gson();
    private static final int MAX = 600, MAX_PENDING = 40;
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private State state;

    public ConceptStore(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            state = raw == null ? null : GSON.fromJson(raw, new TypeToken<State>() { }.getType());
        } catch (Exception damaged) { state = null; }
        if (state == null) state = new State();
        if (state.concepts == null) state.concepts = new LinkedHashMap<>();
        if (state.pending == null) state.pending = new ArrayList<>();
    }

    /** El concepto por su nombre o por cualquiera de sus palabras; null si no lo ha anclado. */
    public synchronized Concept find(String word) {
        String k = key(word);
        if (k.isEmpty()) return null;
        Concept c = state.concepts.get(k);
        if (c != null) return c;
        for (Concept x : state.concepts.values()) for (String w : x.words) if (key(w).equals(k)) return x;
        return null;
    }

    public synchronized List<Concept> all() { return Collections.unmodifiableList(new ArrayList<>(state.concepts.values())); }

    public synchronized void put(Concept c) {
        if (c == null || key(c.name).isEmpty() || c.schemas.isEmpty()) return;
        c.at = clock.getAsLong();
        state.concepts.put(key(c.name), c);
        state.pending.remove(key(c.name));
        while (state.concepts.size() > MAX) state.concepts.remove(state.concepts.keySet().iterator().next());
        save();
    }

    /** Para anclarlo al soñar (si no lo tiene ya). */
    public synchronized void queue(String concept) {
        String k = key(concept);
        if (k.length() < 3 || k.length() > 40 || state.concepts.containsKey(k) || state.pending.contains(k)) return;
        state.pending.add(k);
        while (state.pending.size() > MAX_PENDING) state.pending.remove(0);
        save();
    }

    public synchronized List<String> pending(int max) {
        return new ArrayList<>(state.pending.subList(0, Math.min(max, state.pending.size())));
    }

    public synchronized void drop(String concept) { if (state.pending.remove(key(concept))) save(); }

    // ── El modelo elige; esto valida ─────────────────────────────────────────

    /** Lo que se le pide al modelo: elegir de la lista, no inventar. */
    public static String prompt(String concept, String context) {
        return "Concepto: «" + clip(concept, 60) + "»" + (context == null || context.isEmpty() ? "" : " (contexto: " + clip(context, 300) + ")")
                + ".\nEsquemas posibles (elige SOLO de esta lista, de 1 a 3, los que mejor explican el concepto): " + ImageSchemas.catalog()
                + ".\nPara cada uno di qué cosa hace cada papel, con palabras cortas. Añade sinónimos y la palabra en inglés. Responde SOLO con JSON: "
                + "{\"palabras\":[\"…\"],\"esquemas\":[{\"esquema\":\"soporte\",\"sostenido\":\"…\",\"soporte\":\"…\"}]}";
    }

    /** Lee la elección del modelo; descarta esquemas que no son del catálogo. null si no queda ninguno. */
    public static Concept parse(String concept, String modelText, String source) {
        if (modelText == null) return null;
        int open = modelText.indexOf('{'), close = modelText.lastIndexOf('}');
        if (open < 0 || close <= open) return null;
        JsonObject o;
        try { o = JsonParser.parseString(modelText.substring(open, close + 1)).getAsJsonObject(); }
        catch (RuntimeException bad) { return null; }
        Concept c = new Concept();
        c.name = clip(concept, 40).trim(); c.source = source;
        if (o.has("palabras") && o.get("palabras").isJsonArray())
            for (JsonElement e : o.getAsJsonArray("palabras"))
                if (e.isJsonPrimitive() && c.words.size() < 8) {
                    String w = clip(e.getAsString(), 30).trim();
                    if (!w.isEmpty() && !key(w).equals(key(c.name)) && !c.words.contains(w)) c.words.add(w);
                }
        JsonArray list = o.has("esquemas") && o.get("esquemas").isJsonArray() ? o.getAsJsonArray("esquemas") : new JsonArray();
        for (JsonElement e : list) {
            if (c.schemas.size() >= 3 || !e.isJsonObject()) continue;
            JsonObject m = e.getAsJsonObject();
            ImageSchemas.Schema s = ImageSchemas.byName(m.has("esquema") ? m.get("esquema").getAsString() : "");
            if (s == null) continue;
            boolean dup = false;
            for (Mapping x : c.schemas) if (x.schema.equals(s.id())) dup = true;
            if (dup) continue;
            Mapping mp = new Mapping();
            mp.schema = s.id();
            mp.a = role(m, s.roleA); mp.b = role(m, s.roleB);
            c.schemas.add(mp);
        }
        return c.schemas.isEmpty() ? null : c;
    }

    /**
     * Lo que recibe el modelo al razonar: los esquemas del concepto y sus consecuencias, para que
     * explique por analogía con sus palabras.
     */
    public static String reasoning(Concept c) {
        StringBuilder b = new StringBuilder("ESQUEMAS de «").append(c.name).append("»");
        if (!c.words.isEmpty()) b.append(" (también: ").append(String.join(", ", c.words)).append(')');
        b.append(':');
        for (SceneSchemas.Instance i : c.instances()) {
            b.append("\n- ").append(i.schema.label.toUpperCase(java.util.Locale.ROOT)).append(" (").append(i.schema.roleA.replace('_', ' '))
                    .append(" = ").append(i.a == null || i.a.isEmpty() ? "?" : i.a).append("; ").append(i.schema.roleB.replace('_', ' '))
                    .append(" = ").append(i.b == null || i.b.isEmpty() ? "?" : i.b).append("): ").append(String.join("; ", i.schema.infer(i.a, i.b))).append('.');
        }
        return b.append("\nRazona con estas consecuencias por analogía y explícalo con tus palabras, sin nombres técnicos de esquemas.").toString();
    }

    private static String role(JsonObject m, String role) {
        for (String k : new String[]{role, role.replace('_', ' '), role.replace("_", "")})
            if (m.has(k) && m.get(k).isJsonPrimitive()) return clip(m.get(k).getAsString(), 50).trim();
        return "";
    }

    static String key(String s) { return ImageSchemas.normalize(s); }

    private static String clip(String s, int max) {
        String t = s == null ? "" : s.replaceAll("[\\r\\n«»]+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private void save() {
        try { store.write(GSON.toJson(state)); } catch (Exception ignored) { /* best effort */ }
    }
}
