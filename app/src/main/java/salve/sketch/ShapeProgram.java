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
            "^\\s*[-*•]?\\s*(rect|rectangulo|circle|circulo|ellipse|elipse|line|linea|poly|poligono|path|camino|arc|arco|curve|curva|blob|mancha)\\b\\s*[:(]?\\s*([-0-9.,\\s]+)\\)?\\s*$");

    private ShapeProgram() { }

    /** Instrucción para el modelo (ejemplos completos: un modelo pequeño copia mejor que interpreta). */
    public static String prompt(String subject) { return prompt(subject, null, null); }

    /**
     * Con su idea del objeto: le pide dibujar cada parte que ella misma describió, y lo que Bryan
     * quiere distinto («con más detalle, otra pose»).
     */
    public static String prompt(String subject, VisualConcept concept, String wish) {
        String base = basePrompt(subject);
        String parts = concept == null ? "" : concept.forShapes();
        String w = wish == null || wish.trim().isEmpty() ? "" : "Bryan quiere: " + VisualConcept.clip(wish, 80) + ".\n";
        if (parts.isEmpty() && w.isEmpty()) return base;
        int at = base.lastIndexOf("Ahora «");
        return base.substring(0, at) + parts + w + "Usa entre 8 y 25 formas, una o más por parte.\n" + base.substring(at);
    }

    private static String basePrompt(String subject) {
        String what = subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " ").trim();
        if (what.length() > 60) what = what.substring(0, 60);
        return "Eres Salve y dibujas con formas geométricas. Lienzo de 100x100: x crece a la derecha, y crece hacia abajo.\n"
                + "Escribe SOLO las formas, una por línea, sin explicaciones ni texto extra. Formas permitidas:\n"
                + "rect x y ancho alto\ncircle x y radio\nellipse x y radioX radioY\nline x1 y1 x2 y2\n"
                + "poly x1 y1 x2 y2 x3 y3 ... (cerrado)\npath x1 y1 x2 y2 x3 y3 ... (abierto)\narc x y radio gradoInicio gradoFin\n"
                + "curve x1 y1 cx1 cy1 cx2 cy2 x2 y2 (curva Bézier: inicio, dos puntos de control, fin)\n"
                + "blob x1 y1 x2 y2 x3 y3 ... (contorno curvo y cerrado que pasa por esos puntos: cuerpos, cabezas, hojas)\n"
                + "Para seres vivos y ropa usa blob y curve, no rectángulos.\n"
                + "Piensa en las partes reales del objeto, su forma y su posición.\n"
                + "Ejemplo «casa»:\nrect 20 45 60 45\npoly 14 47 50 15 86 47\nrect 44 65 12 25\nrect 27 55 11 10\nrect 62 55 11 10\n"
                + "Antes de cada parte escribe una línea «# parte: lo que trazas».\n"
                + "Ejemplo «gato sentado»:\n# cuerpo: un óvalo sentado\nblob 40 55 52 48 64 56 66 78 58 92 40 92 32 78\n"
                + "# cabeza: redonda sobre el cuerpo\nblob 44 30 52 26 62 30 62 42 53 48 44 42\n"
                + "# orejas: dos triángulos arriba\npoly 46 30 47 20 52 27\npoly 56 27 61 20 61 31\n"
                + "# cola: curva que sube por detrás\ncurve 62 88 80 88 86 70 78 58\n# ojos\nline 50 37 52 37\nline 56 37 58 37\n"
                + "Ejemplo «gafas»:\ncircle 32 50 14\ncircle 68 50 14\narc 50 52 6 200 340\nline 18 46 6 40\nline 82 46 94 40\n"
                + "Ejemplo «mesa»:\nrect 10 40 80 6\nline 16 46 16 85\nline 84 46 84 85\nline 26 46 24 80\nline 74 46 76 80\n"
                + "Ahora «" + what + "»:\n";
    }

    public static final class Result {
        public final List<float[]> strokes;
        public final int shapes, rejected;
        public final float aspect;
        /** Partes con nombre en orden de trazado (vacío si no las escribió). */
        public List<SketchLibrary.Part> parts = new ArrayList<>();
        /** Lo que pensó antes de dibujar («thoughtSummary»), si lo dio. */
        public String summary = "";
        Result(List<float[]> strokes, int shapes, int rejected, float aspect) {
            this.strokes = strokes; this.shapes = shapes; this.rejected = rejected; this.aspect = aspect;
        }
        /** Al menos tres formas válidas (con menos sale un garabato abstracto) y casi todo lo escrito entendido. */
        public boolean isUsable() { return shapes >= 3 && rejected <= shapes; }
    }

    /** «# cabeza: redonda sobre el cuerpo» abre una parte; lo que sigue le pertenece. */
    private static final Pattern HEADER = Pattern.compile(
            "^\\s*(?:#+|parte|part)\\s*[:.-]?\\s*([\\p{L}][\\p{L} ]{1,38}?)\\s*(?:[:\\-–]\\s*(.{0,140}))?\\s*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static Result parse(String text) {
        List<float[]> strokes = new ArrayList<>();
        int shapes = 0, rejected = 0;
        if (text == null) return new Result(strokes, 0, 0, 1);
        List<String[]> headers = new ArrayList<>(); // {nombre, descripción, desdeTrazo}
        for (String raw : text.split("\\r?\\n")) {
            Matcher h = HEADER.matcher(raw.replace("**", ""));
            if (h.matches() && !LINE.matcher(raw.trim().toLowerCase(Locale.ROOT)).matches()) {
                headers.add(new String[]{h.group(1).trim().toLowerCase(Locale.ROOT), h.group(2) == null ? "" : h.group(2).trim(),
                        String.valueOf(strokes.size())});
                continue;
            }
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
        Result result = normalize(strokes, shapes, rejected);
        for (int i = 0; i < headers.size(); i++) {
            int from = Integer.parseInt(headers.get(i)[2]);
            int to = i + 1 < headers.size() ? Integer.parseInt(headers.get(i + 1)[2]) : strokes.size();
            if (to > from) result.parts.add(new SketchLibrary.Part(headers.get(i)[0], headers.get(i)[1], from, to));
        }
        // Formas antes del primer encabezado: parte sin nombre al principio.
        if (!result.parts.isEmpty() && result.parts.get(0).from > 0)
            result.parts.add(0, new SketchLibrary.Part("base", "", 0, result.parts.get(0).from));
        return result;
    }

    /** Centra las líneas en el lienzo 0..1000 conservando la proporción. */
    static Result normalize(List<float[]> strokes, int shapes, int rejected) {
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
            case "curve": case "curva": {
                if (v.length != 8) return null;
                float[] p = new float[2 * 25];
                for (int i = 0; i < 25; i++) {
                    float[] q = BezierSketch.point(v, i / 24f);
                    p[2 * i] = q[0]; p[2 * i + 1] = q[1];
                }
                return p;
            }
            case "blob": case "mancha":
                if (v.length < 6 || v.length % 2 != 0 || v.length > 60) return null;
                return catmullRom(v);
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

    /** Contorno suave y cerrado que pasa por los puntos (Catmull-Rom). */
    static float[] catmullRom(float[] v) {
        int n = v.length / 2, steps = 8;
        float[] out = new float[2 * (n * steps + 1)];
        int k = 0;
        for (int i = 0; i < n; i++) {
            int i0 = (i - 1 + n) % n, i2 = (i + 1) % n, i3 = (i + 2) % n;
            for (int s = 0; s < steps; s++) {
                float t = s / (float) steps, t2 = t * t, t3 = t2 * t;
                for (int c = 0; c < 2; c++) {
                    float p0 = v[2 * i0 + c], p1 = v[2 * i + c], p2 = v[2 * i2 + c], p3 = v[2 * i3 + c];
                    out[k++] = .5f * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
                }
            }
        }
        out[k++] = v[0]; out[k++] = v[1];
        return out;
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
