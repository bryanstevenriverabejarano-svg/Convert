package salve.core.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Visión que abstrae: de una foto saca los objetos, dónde está cada uno y cómo se relacionan.
 *
 * <p>El modelo de visión solo dice qué ve y la caja de cada cosa (en % de la imagen). Las relaciones
 * —encima de, dentro de, a la izquierda de, delante de, cerca de— las calcula esta clase con
 * geometría, no se le piden al modelo: un modelo pequeño inventa relaciones; las cajas se pueden
 * medir. Cada objeto queda como un vector espacial [centro x, centro y, ancho, alto] entre 0 y 1.</p>
 */
public final class SceneGraph {
    public static final int MAX_OBJECTS = 12;
    private static final int MAX_RELATIONS_PER_OBJECT = 3;

    public static final class Item {
        public final String name, color;
        /** Caja en % de la imagen: x1, y1 (arriba izquierda), x2, y2 (abajo derecha). */
        public final double x1, y1, x2, y2;
        Item(String name, String color, double x1, double y1, double x2, double y2) {
            this.name = name; this.color = color == null ? "" : color;
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        }
        public double w() { return x2 - x1; }
        public double h() { return y2 - y1; }
        public double cx() { return (x1 + x2) / 2; }
        public double cy() { return (y1 + y2) / 2; }
        public double area() { return w() * h(); }
        /** Vector espacial normalizado: centro x, centro y, ancho, alto (0..1). */
        public double[] vector() { return new double[]{cx() / 100, cy() / 100, w() / 100, h() / 100}; }

        /** «arriba a la izquierda», «abajo», «en el centro»… */
        public String where() {
            String hz = cx() < 33 ? "a la izquierda" : cx() > 67 ? "a la derecha" : "";
            String vt = cy() < 33 ? "arriba" : cy() > 67 ? "abajo" : "";
            if (hz.isEmpty() && vt.isEmpty()) return "en el centro";
            if (vt.isEmpty()) return hz;
            return hz.isEmpty() ? vt : vt + " " + hz;
        }

        /** Tamaño por la parte de la foto que ocupa. */
        public String size() {
            double a = area() / 10_000.0;
            return a > 0.25 ? "grande" : a > 0.05 ? "mediano" : "pequeño";
        }
    }

    public static final class Relation {
        public final Item a, b;
        public final String kind;
        Relation(Item a, String kind, Item b) { this.a = a; this.kind = kind; this.b = b; }
        @Override public String toString() { return a.name + " " + kind + " " + b.name; }
    }

    public final List<Item> items;
    public final List<Relation> relations;

    private SceneGraph(List<Item> items) {
        this.items = Collections.unmodifiableList(items);
        this.relations = Collections.unmodifiableList(relate(items));
    }

    /** Lo que se le pide al modelo de visión: objetos y cajas, nada más. */
    public static String prompt() {
        return "Mira la foto y enumera los objetos que se ven (como máximo " + MAX_OBJECTS + "), con nombres cortos en español. "
                + "Para cada uno da su caja en porcentaje de la imagen: x1,y1 es la esquina de arriba a la izquierda y x2,y2 la de "
                + "abajo a la derecha, de 0 a 100. Si sabes el color, ponlo. No describas nada más. Responde SOLO con JSON así: "
                + "{\"objetos\":[{\"nombre\":\"taza\",\"color\":\"blanca\",\"caja\":[60,55,75,70]},"
                + "{\"nombre\":\"mesa\",\"color\":\"marrón\",\"caja\":[10,62,95,100]}]}";
    }

    /**
     * Lee la respuesta del modelo con tolerancia (texto alrededor, ```json, cajas en 0..1 o 0..1000,
     * esquinas al revés). Devuelve null si no hay ni un objeto utilizable.
     */
    public static SceneGraph parse(String modelText) {
        if (modelText == null) return null;
        int open = modelText.indexOf('{'), close = modelText.lastIndexOf('}');
        int ao = modelText.indexOf('['), ac = modelText.lastIndexOf(']');
        JsonArray list = null;
        // Un objeto {"objetos":[…]} o directamente la lista […]; se prueba lo que empiece antes.
        boolean arrayFirst = ao >= 0 && (open < 0 || ao < open);
        for (int attempt = 0; attempt < 2 && list == null; attempt++) {
            boolean tryArray = (attempt == 0) == arrayFirst;
            try {
                if (tryArray && ao >= 0 && ac > ao) {
                    JsonElement e = JsonParser.parseString(modelText.substring(ao, ac + 1));
                    if (e.isJsonArray()) list = e.getAsJsonArray();
                } else if (!tryArray && open >= 0 && close > open) {
                    JsonObject o = JsonParser.parseString(modelText.substring(open, close + 1)).getAsJsonObject();
                    for (String k : new String[]{"objetos", "objects", "items", "elementos"})
                        if (o.has(k) && o.get(k).isJsonArray()) { list = o.getAsJsonArray(k); break; }
                }
            } catch (RuntimeException unreadable) { /* se prueba la otra forma */ }
        }
        if (list == null) return null;
        List<Item> items = new ArrayList<>();
        Map<String, Integer> seen = new HashMap<>();
        for (JsonElement e : list) {
            if (items.size() >= MAX_OBJECTS || !e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String name = cleanName(first(o, "nombre", "name", "objeto", "object", "label"));
            if (name.isEmpty()) continue;
            double[] box = box(o);
            if (box == null) continue;
            int n = seen.merge(name, 1, Integer::sum);
            items.add(new Item(n == 1 ? name : name + " " + n, cleanName(first(o, "color", "colour")), box[0], box[1], box[2], box[3]));
        }
        return items.isEmpty() ? null : new SceneGraph(items);
    }

    /** Para leer una escena guardada. */
    public static SceneGraph fromJson(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            List<Item> items = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("objetos")) {
                JsonObject i = e.getAsJsonObject();
                JsonArray c = i.getAsJsonArray("caja");
                items.add(new Item(i.get("nombre").getAsString(), i.has("color") ? i.get("color").getAsString() : "",
                        c.get(0).getAsDouble(), c.get(1).getAsDouble(), c.get(2).getAsDouble(), c.get(3).getAsDouble()));
            }
            return items.isEmpty() ? null : new SceneGraph(items);
        } catch (RuntimeException damaged) { return null; }
    }

    public JsonObject json() {
        JsonObject out = new JsonObject();
        out.addProperty("schema", 1);
        JsonArray objs = new JsonArray();
        for (Item i : items) {
            JsonObject o = new JsonObject();
            o.addProperty("nombre", i.name);
            if (!i.color.isEmpty()) o.addProperty("color", i.color);
            JsonArray c = new JsonArray();
            for (double v : new double[]{i.x1, i.y1, i.x2, i.y2}) c.add(Math.round(v * 10) / 10.0);
            o.add("caja", c);
            JsonArray v = new JsonArray();
            for (double d : i.vector()) v.add(Math.round(d * 1000) / 1000.0);
            o.add("vector", v);
            o.addProperty("donde", i.where());
            o.addProperty("tamano", i.size());
            objs.add(o);
        }
        out.add("objetos", objs);
        JsonArray rels = new JsonArray();
        for (Relation r : relations) {
            JsonObject o = new JsonObject();
            o.addProperty("a", r.a.name); o.addProperty("relacion", r.kind); o.addProperty("b", r.b.name);
            rels.add(o);
        }
        out.add("relaciones", rels);
        return out;
    }

    /**
     * Texto para su memoria (lo que se busca y lo que lee el modelo): «Escena de la foto …: hay taza
     * blanca (pequeña, abajo a la derecha); mesa (grande, abajo). taza: encima de mesa.»
     */
    public String summary(String photoId) {
        StringBuilder b = new StringBuilder("Escena de la foto ").append(photoId == null ? "" : photoId).append(": hay ");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            b.append(i == 0 ? "" : "; ").append(it.name).append(it.color.isEmpty() ? "" : " " + it.color)
                    .append(" (").append(it.size()).append(", ").append(it.where()).append(')');
        }
        b.append('.');
        if (!relations.isEmpty()) {
            b.append(" Dónde está cada cosa:");
            for (Relation r : relations) b.append(' ').append(r.a.name).append(": ").append(r.kind).append(' ').append(r.b.name).append('.');
        }
        return b.toString();
    }

    /** Para contárselo a Bryan: qué hay y cómo está colocado. */
    public String spoken() {
        StringBuilder b = new StringBuilder("En la foto veo ");
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            b.append(i == 0 ? "" : i == items.size() - 1 ? " y " : ", ").append(it.name).append(it.color.isEmpty() ? "" : " " + it.color)
                    .append(" (").append(it.where()).append(')');
        }
        b.append('.');
        if (!relations.isEmpty()) {
            b.append(" Cómo está colocado:");
            for (int i = 0; i < relations.size(); i++) {
                Relation r = relations.get(i);
                b.append(i == 0 ? " " : "; ").append(r.a.name).append(' ').append(r.kind).append(' ').append(r.b.name);
            }
            b.append('.');
        }
        return b.toString();
    }

    /** «¿Qué objetos hay?», «analiza la escena», «¿dónde está cada cosa?», «mira la foto con calma»… */
    public static boolean isCommand(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
        return t.matches(".*\\b(que objetos hay|que cosas hay|analiza la escena|analiza los objetos|donde esta cada cosa|"
                + "como esta colocad[oa]|mira la foto con calma|que hay en la foto y donde|separa los objetos|"
                + "abstrae la (foto|imagen|escena)|grafo de la escena|escena de la foto)\\b.*");
    }

    /** Nombres de los objetos (sin el número de repetición), para etiquetar el recuerdo. */
    public List<String> names() {
        List<String> out = new ArrayList<>();
        for (Item i : items) {
            String base = i.name.replaceAll(" \\d+$", "");
            if (!out.contains(base)) out.add(base);
        }
        return out;
    }

    // ── Geometría ────────────────────────────────────────────────────────────────

    static List<Relation> relate(List<Item> items) {
        List<Relation> out = new ArrayList<>();
        for (Item a : items) {
            List<Object[]> mine = new ArrayList<>(); // {Relation, prioridad, distancia}
            for (Item b : items) {
                if (a == b) continue;
                String kind = relation(a, b);
                if (kind == null) continue;
                int priority = kind.equals("dentro de") || kind.equals("encima de") ? 0 : kind.equals("delante de") ? 1
                        : kind.startsWith("a la") ? 2 : 3;
                mine.add(new Object[]{new Relation(a, kind, b), priority, Math.hypot(a.cx() - b.cx(), a.cy() - b.cy())});
            }
            mine.sort(Comparator.<Object[]>comparingInt(x -> (Integer) x[1]).thenComparingDouble(x -> (Double) x[2]));
            for (int i = 0; i < Math.min(MAX_RELATIONS_PER_OBJECT, mine.size()); i++) {
                Relation r = (Relation) mine.get(i)[0];
                // «cerca de» es simétrica: una sola vez.
                if (r.kind.equals("cerca de") && contains(out, r.b, "cerca de", r.a)) continue;
                out.add(r);
            }
        }
        return out;
    }

    /** La relación de a respecto de b, o null. Coordenadas de imagen: y crece hacia abajo. */
    static String relation(Item a, Item b) {
        double overlapX = Math.max(0, Math.min(a.x2, b.x2) - Math.max(a.x1, b.x1));
        double overlapY = Math.max(0, Math.min(a.y2, b.y2) - Math.max(a.y1, b.y1));
        double inter = overlapX * overlapY;
        double minW = Math.max(0.01, Math.min(a.w(), b.w())), minH = Math.max(0.01, Math.min(a.h(), b.h()));
        // Casi entero dentro de la caja de otro más grande: o apoyado encima (en su parte alta) o dentro.
        if (a.area() > 0 && inter / a.area() >= 0.85 && a.area() < 0.6 * b.area())
            return a.cy() < b.y1 + 0.4 * b.h() ? "encima de" : "dentro de";
        // Apoyado encima: se solapan en horizontal y su base cae en la parte alta del otro.
        if (overlapX / minW >= 0.4 && a.cy() < b.cy() && a.y2 >= b.y1 - 0.1 * b.h() && a.y2 <= b.y1 + 0.3 * b.h())
            return "encima de";
        // Uno tapa a otro: el que llega más abajo en la imagen está más cerca de la cámara.
        if (inter >= 0.1 * Math.min(a.area(), b.area()) && a.y2 > b.y2 + 3) return "delante de";
        // Al lado: a la misma altura y casi tocándose.
        double gap = b.x1 - a.x2;
        if (overlapY / minH >= 0.4 && gap >= -5 && gap <= 15) return "a la izquierda de";
        if (Math.hypot(a.cx() - b.cx(), a.cy() - b.cy()) < 25) return "cerca de";
        return null;
    }

    private static boolean contains(List<Relation> list, Item a, String kind, Item b) {
        for (Relation r : list) if (r.a == a && r.b == b && r.kind.equals(kind)) return true;
        return false;
    }

    // ── Lectura tolerante ───────────────────────────────────────────────────────

    private static String first(JsonObject o, String... keys) {
        for (String k : keys) if (o.has(k) && o.get(k).isJsonPrimitive()) return o.get(k).getAsString();
        return "";
    }

    private static double[] box(JsonObject o) {
        JsonArray a = null;
        for (String k : new String[]{"caja", "box", "bbox", "box_2d", "coordenadas"})
            if (o.has(k) && o.get(k).isJsonArray()) { a = o.getAsJsonArray(k); break; }
        if (a == null || a.size() != 4) return null;
        double[] v = new double[4];
        try { for (int i = 0; i < 4; i++) v[i] = a.get(i).getAsDouble(); } catch (RuntimeException bad) { return null; }
        double max = Math.max(Math.max(v[0], v[1]), Math.max(v[2], v[3]));
        double scale = max <= 1.0 ? 100 : max > 100 && max <= 1000 ? 0.1 : 1;
        for (int i = 0; i < 4; i++) v[i] = Math.max(0, Math.min(100, v[i] * scale));
        if (v[2] < v[0]) { double t = v[0]; v[0] = v[2]; v[2] = t; }
        if (v[3] < v[1]) { double t = v[1]; v[1] = v[3]; v[3] = t; }
        if (v[2] - v[0] < 1 || v[3] - v[1] < 1) return null;
        return v;
    }

    static String cleanName(String raw) {
        String t = raw == null ? "" : Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
        t = t.replaceAll("[^\\p{L}\\p{N} ]+", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(un|una|unos|unas|el|la|los|las) ", "");
        return t.length() > 30 ? t.substring(0, 30).trim() : t;
    }
}
