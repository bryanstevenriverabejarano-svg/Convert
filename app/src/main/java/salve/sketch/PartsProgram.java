package salve.sketch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dibujo por partes con sentido (formato JSON): un resumen de lo que piensa y, por cada parte, su
 * nombre, qué hace al trazarla, sus puntos normalizados (0..1), si es abierta y cuántos armónicos usar.
 *
 * <pre>{"thoughtSummary": "…", "parts": [{"name": "cabeza", "description": "…", "isOpenPath": false,
 *   "maxHarmonics": 30, "points": [{"x": .4, "y": .2}, …]}]}</pre>
 *
 * Cada parte se suaviza con un filtro de Fourier (sólo sus {@code maxHarmonics} círculos mayores): de
 * pocos puntos sale una curva fluida, no un polígono. Lo usa el modelo grande (Gemini); el texto se
 * valida, nunca se ejecuta.
 */
public final class PartsProgram {
    public static final int MAX_PARTS = 24, MAX_POINTS = 200;

    private PartsProgram() { }

    public static String prompt(String subject, VisualConcept concept, String wish) {
        String what = VisualConcept.clip(subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " "), 60);
        StringBuilder b = new StringBuilder("Eres Salve y dibujas con líneas de luz. Dibuja «").append(what)
                .append("» reconocible, de perfil o en su pose típica, como un dibujo de líneas limpio.\n");
        if (concept != null && concept.isUsable()) b.append(concept.forShapes());
        if (wish != null && !wish.trim().isEmpty()) b.append("Bryan quiere: ").append(VisualConcept.clip(wish, 80)).append(".\n");
        return b.append("Responde SOLO con JSON (sin texto ni ```): {\"thoughtSummary\": \"qué vas a dibujar y cómo\", ")
                .append("\"parts\": [{\"name\": \"parte\", \"description\": \"lo que trazas, en primera persona\", ")
                .append("\"isOpenPath\": false, \"maxHarmonics\": 30, \"points\": [{\"x\": 0.15, \"y\": 0.20}, …]}]}.\n")
                .append("Coordenadas de 0.0 a 1.0 (y crece hacia abajo). Entre 4 y 12 partes, en el orden en que las dibujarías; ")
                .append("cada parte con 6 a 40 puntos siguiendo su contorno real. Contornos cerrados con isOpenPath false; ")
                .append("líneas sueltas (patas, pelos, pliegues) con true. maxHarmonics 15-25 para formas simples, 30-60 para detalladas.\n")
                .toString();
    }

    /** null si el texto no trae un dibujo por partes válido. */
    public static ShapeProgram.Result parse(String text) {
        if (text == null) return null;
        int a = text.indexOf('{'), z = text.lastIndexOf('}');
        if (a < 0 || z <= a) return null;
        JsonObject root;
        try { root = JsonParser.parseString(text.substring(a, z + 1)).getAsJsonObject(); }
        catch (RuntimeException notJson) { return null; }
        JsonArray raw = root.has("parts") && root.get("parts").isJsonArray() ? root.getAsJsonArray("parts") : null;
        if (raw == null || raw.size() == 0) return null;
        List<float[]> strokes = new ArrayList<>();
        List<String[]> names = new ArrayList<>();
        int rejected = 0;
        float max = 0;
        List<float[]> rawPoints = new ArrayList<>();
        List<boolean[]> open = new ArrayList<>();
        List<Integer> harmonics = new ArrayList<>();
        for (JsonElement e : raw) {
            if (rawPoints.size() >= MAX_PARTS) break;
            try {
                JsonObject p = e.getAsJsonObject();
                float[] pts = points(p.getAsJsonArray("points"));
                if (pts == null) { rejected++; continue; }
                for (float v : pts) max = Math.max(max, Math.abs(v));
                rawPoints.add(pts);
                open.add(new boolean[]{p.has("isOpenPath") && p.get("isOpenPath").getAsBoolean()});
                harmonics.add(p.has("maxHarmonics") ? Math.max(6, Math.min(80, p.get("maxHarmonics").getAsInt())) : 35);
                names.add(new String[]{str(p, "name", "parte " + (rawPoints.size())), str(p, "description", "")});
            } catch (RuntimeException bad) { rejected++; }
        }
        if (rawPoints.isEmpty()) return null;
        // Acepta 0..1, 0..100 o 0..1000.
        float unit = max > 101 ? 1000 : max > 1.5f ? 100 : 1;
        for (int i = 0; i < rawPoints.size(); i++) {
            float[] p = rawPoints.get(i);
            int n = p.length / 2;
            boolean closed = !open.get(i)[0];
            float[] scaled = new float[closed ? p.length + 2 : p.length];
            for (int k = 0; k < p.length; k++) scaled[k] = p[k] / unit * 100f;
            if (closed) { scaled[2 * n] = scaled[0]; scaled[2 * n + 1] = scaled[1]; }
            // Suavizado armónico: de pocos puntos, una curva fluida.
            float[] smooth = n >= 4 ? FourierStroke.lowPass(FourierStroke.uniform(scaled, 1.2f, 260), harmonics.get(i)) : scaled;
            strokes.add(smooth);
        }
        ShapeProgram.Result normalized = ShapeProgram.normalize(strokes, rawPoints.size(), rejected);
        for (int i = 0; i < names.size(); i++)
            normalized.parts.add(new SketchLibrary.Part(names.get(i)[0].toLowerCase(Locale.ROOT), names.get(i)[1], i, i + 1));
        normalized.summary = VisualConcept.clip(str(root, "thoughtSummary", ""), 200);
        return normalized;
    }

    private static float[] points(JsonArray arr) {
        if (arr == null || arr.size() < 2 || arr.size() > MAX_POINTS) return null;
        float[] out = new float[2 * arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            float x, y;
            if (e.isJsonObject()) { x = e.getAsJsonObject().get("x").getAsFloat(); y = e.getAsJsonObject().get("y").getAsFloat(); }
            else { JsonArray xy = e.getAsJsonArray(); x = xy.get(0).getAsFloat(); y = xy.get(1).getAsFloat(); }
            if (!Float.isFinite(x) || !Float.isFinite(y) || Math.abs(x) > 1000 || Math.abs(y) > 1000) return null;
            out[2 * i] = x; out[2 * i + 1] = y;
        }
        return out;
    }

    private static String str(JsonObject o, String key, String fallback) {
        try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback; }
        catch (RuntimeException notText) { return fallback; }
    }
}
