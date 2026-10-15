package salve.sketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cuando Salve no tiene un motivo, lo compone ella: su modelo describe el objeto con formas
 * geométricas simples (rectángulos, círculos, elipses, líneas, polígonos, arcos) en un lienzo de
 * 100×100, y aquí se validan y se convierten en líneas para los epiciclos. El texto del modelo nunca
 * se ejecuta: sólo se aceptan líneas con esta gramática y números acotados.
 */
public final class ShapeProgram {
    public static final int MAX_SHAPES = 40;
    private static final Pattern LINE = Pattern.compile(
            "^\\s*[-*•]?\\s*(rect|rectangulo|circle|circulo|ellipse|elipse|line|linea|poly|poligono|path|camino|arc|arco)\\b\\s*[:(]?\\s*([-0-9.,\\s]+)\\)?\\s*$");

    private ShapeProgram() { }

    /** Instrucción para el modelo (ejemplos completos: un modelo pequeño copia mejor que interpreta). */
    public static String prompt(String subject) {
        String what = subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " ").trim();
        if (what.length() > 60) what = what.substring(0, 60);
        return "Eres Salve y dibujas con formas geométricas. Lienzo de 100x100: x crece a la derecha, y crece hacia abajo.\n"
                + "Escribe SOLO las formas, una por línea, sin explicaciones ni texto extra. Formas permitidas:\n"
                + "rect x y ancho alto\ncircle x y radio\nellipse x y radioX radioY\nline x1 y1 x2 y2\n"
                + "poly x1 y1 x2 y2 x3 y3 ... (cerrado)\npath x1 y1 x2 y2 x3 y3 ... (abierto)\narc x y radio gradoInicio gradoFin\n"
                + "Piensa en las partes reales del objeto, su forma y su posición.\n"
                + "Ejemplo «casa»:\nrect 20 45 60 45\npoly 14 47 50 15 86 47\nrect 44 65 12 25\nrect 27 55 11 10\nrect 62 55 11 10\n"
                + "Ejemplo «gafas»:\ncircle 32 50 14\ncircle 68 50 14\narc 50 52 6 200 340\nline 18 46 6 40\nline 82 46 94 40\n"
                + "Ejemplo «mesa»:\nrect 10 40 80 6\nline 16 46 16 85\nline 84 46 84 85\nline 26 46 24 80\nline 74 46 76 80\n"
                + "Ahora «" + what + "»:\n";
    }

    public static final class Result {
        public final List<float[]> strokes;
        public final int shapes, rejected;
        public final float aspect;
        Result(List<float[]> strokes, int shapes, int rejected, float aspect) {
            this.strokes = strokes; this.shapes = shapes; this.rejected = rejected; this.aspect = aspect;
        }
        /** Al menos dos formas válidas y casi todo lo escrito entendido. */
        public boolean isUsable() { return shapes >= 2 && rejected <= shapes; }
    }

    public static Result parse(String text) {
        List<float[]> strokes = new ArrayList<>();
        int shapes = 0, rejected = 0;
        if (text == null) return new Result(strokes, 0, 0, 1);
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim().toLowerCase(Locale.ROOT).replace("rectángulo", "rectangulo").replace("círculo", "circulo")
                    .replace("línea", "linea").replace("polígono", "poligono");
            if (line.isEmpty() || line.startsWith("```") || line.startsWith("ahora") || line.startsWith("ejemplo")) continue;
            if (shapes >= MAX_SHAPES) break;
            Matcher m = LINE.matcher(line);
            if (!m.matches()) { if (line.matches(".*\\d.*")) rejected++; continue; }
            float[] v = numbers(m.group(2));
            float[] stroke = shape(m.group(1), v);
            if (stroke == null) { rejected++; continue; }
            strokes.add(stroke);
            shapes++;
        }
        float aspect = 1;
        if (!strokes.isEmpty()) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (float[] s : strokes) for (int i = 0; i < s.length; i += 2) {
                minX = Math.min(minX, s[i]); maxX = Math.max(maxX, s[i]); minY = Math.min(minY, s[i + 1]); maxY = Math.max(maxY, s[i + 1]);
            }
            float w = Math.max(1, maxX - minX), h = Math.max(1, maxY - minY), scale = 1000 / Math.max(w, h);
            float ox = (1000 - w * scale) / 2, oy = (1000 - h * scale) / 2;
            for (float[] s : strokes) for (int i = 0; i < s.length; i += 2) {
                s[i] = (s[i] - minX) * scale + ox; s[i + 1] = (s[i + 1] - minY) * scale + oy;
            }
            aspect = w / h;
        }
        return new Result(strokes, shapes, rejected, aspect);
    }

    private static float[] numbers(String text) {
        String[] parts = text.trim().split("[\\s,]+");
        List<Float> out = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            try { out.add(Float.parseFloat(p)); } catch (NumberFormatException ignored) { return null; }
        }
        float[] v = new float[out.size()];
        for (int i = 0; i < v.length; i++) v[i] = out.get(i);
        return v;
    }

    private static boolean ok(float v) { return Float.isFinite(v) && v >= -20 && v <= 120; }

    static float[] shape(String kind, float[] v) {
        if (v == null) return null;
        for (float x : v) if (!ok(x) && !kind.startsWith("arc")) return null;
        switch (kind) {
            case "rect": case "rectangulo":
                if (v.length != 4 || v[2] <= 0 || v[3] <= 0) return null;
                return new float[]{v[0], v[1], v[0] + v[2], v[1], v[0] + v[2], v[1] + v[3], v[0], v[1] + v[3], v[0], v[1]};
            case "circle": case "circulo":
                if (v.length != 3 || v[2] <= 0 || v[2] > 70) return null;
                return ellipse(v[0], v[1], v[2], v[2], 0, 360);
            case "ellipse": case "elipse":
                if (v.length != 4 || v[2] <= 0 || v[3] <= 0 || v[2] > 70 || v[3] > 70) return null;
                return ellipse(v[0], v[1], v[2], v[3], 0, 360);
            case "line": case "linea":
                if (v.length != 4 || (v[0] == v[2] && v[1] == v[3])) return null;
                return v.clone();
            case "poly": case "poligono": case "path": case "camino": {
                if (v.length < 4 || v.length % 2 != 0 || v.length > 80) return null;
                boolean closed = kind.startsWith("pol");
                float[] p = java.util.Arrays.copyOf(v, closed ? v.length + 2 : v.length);
                if (closed) { p[v.length] = v[0]; p[v.length + 1] = v[1]; }
                return p;
            }
            case "arc": case "arco":
                if (v.length != 5 || !ok(v[0]) || !ok(v[1]) || v[2] <= 0 || v[2] > 70 || !Float.isFinite(v[3]) || !Float.isFinite(v[4]))
                    return null;
                float sweep = v[4] - v[3];
                if (Math.abs(sweep) < 5 || Math.abs(sweep) > 360) return null;
                return ellipse(v[0], v[1], v[2], v[2], v[3], v[4]);
            default:
                return null;
        }
    }

    private static float[] ellipse(float cx, float cy, float rx, float ry, float from, float to) {
        int n = Math.max(12, Math.round(Math.abs(to - from) / 6f));
        float[] p = new float[2 * (n + 1)];
        for (int i = 0; i <= n; i++) {
            double a = Math.toRadians(from + (to - from) * i / n);
            p[2 * i] = cx + rx * (float) Math.cos(a);
            p[2 * i + 1] = cy + ry * (float) Math.sin(a);
        }
        return p;
    }
}
