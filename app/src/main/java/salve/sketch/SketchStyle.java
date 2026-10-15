package salve.sketch;

import java.util.ArrayList;
import java.util.List;

/**
 * Su propia versión de un calco: de la imagen de referencia se queda con los contornos que definen la
 * forma, los redondea como haría una mano (Chaikin) y quita el detalle que sobra (Douglas-Peucker).
 * El resultado ya no es la referencia: es cómo lo dibuja ella. Lienzo 0..1000.
 */
public final class SketchStyle {
    private SketchStyle() { }

    public static final class Result {
        public final List<float[]> strokes;
        public final int fromStrokes, kept;
        Result(List<float[]> strokes, int fromStrokes) { this.strokes = strokes; this.fromStrokes = fromStrokes; this.kept = strokes.size(); }
        public String describe() {
            return "de " + fromStrokes + " líneas del calco me quedé con " + kept + " y las redondeé a mano alzada";
        }
    }

    /** @param maxStrokes contornos que conserva (los más largos) */
    public static Result ownVersion(List<float[]> traced, int maxStrokes) {
        List<float[]> source = new ArrayList<>();
        for (float[] s : traced) if (s != null && s.length >= 4 && length(s) >= 18) source.add(s);
        if (source.size() > maxStrokes) {
            List<float[]> sorted = new ArrayList<>(source);
            sorted.sort((a, b) -> Float.compare(length(b), length(a)));
            java.util.Set<float[]> keep = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            keep.addAll(sorted.subList(0, maxStrokes));
            source.removeIf(s -> !keep.contains(s));
        }
        List<float[]> out = new ArrayList<>();
        for (float[] s : source) {
            float[] smooth = simplify(chaikin(chaikin(s)), 3.5f);
            if (smooth.length >= 4) out.add(smooth);
        }
        return new Result(out, traced.size());
    }

    static float[] chaikin(float[] s) {
        int n = s.length / 2;
        if (n < 3) return s.clone();
        boolean closed = Math.hypot(s[0] - s[2 * n - 2], s[1] - s[2 * n - 1]) < 2;
        List<Float> p = new ArrayList<>();
        if (!closed) { p.add(s[0]); p.add(s[1]); }
        for (int i = 0; i < n - 1; i++) {
            float x0 = s[2 * i], y0 = s[2 * i + 1], x1 = s[2 * i + 2], y1 = s[2 * i + 3];
            p.add(.75f * x0 + .25f * x1); p.add(.75f * y0 + .25f * y1);
            p.add(.25f * x0 + .75f * x1); p.add(.25f * y0 + .75f * y1);
        }
        if (closed) { p.add(p.get(0)); p.add(p.get(1)); }
        else { p.add(s[2 * n - 2]); p.add(s[2 * n - 1]); }
        float[] r = new float[p.size()];
        for (int i = 0; i < r.length; i++) r[i] = Math.max(0, Math.min(1000, p.get(i)));
        return r;
    }

    static float[] simplify(float[] s, float epsilon) {
        int n = s.length / 2;
        if (n < 3) return s.clone();
        boolean[] keep = new boolean[n];
        keep[0] = keep[n - 1] = true;
        java.util.ArrayDeque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[]{0, n - 1});
        while (!stack.isEmpty()) {
            int[] r = stack.pop();
            float max = 0; int at = -1;
            for (int i = r[0] + 1; i < r[1]; i++) {
                float d = distance(s, i, r[0], r[1]);
                if (d > max) { max = d; at = i; }
            }
            if (at >= 0 && max > epsilon) { keep[at] = true; stack.push(new int[]{r[0], at}); stack.push(new int[]{at, r[1]}); }
        }
        int count = 0;
        for (boolean k : keep) if (k) count++;
        float[] out = new float[2 * count];
        for (int i = 0, j = 0; i < n; i++) if (keep[i]) { out[j++] = s[2 * i]; out[j++] = s[2 * i + 1]; }
        return out;
    }

    private static float distance(float[] s, int p, int a, int b) {
        float ax = s[2 * a], ay = s[2 * a + 1], bx = s[2 * b], by = s[2 * b + 1], px = s[2 * p], py = s[2 * p + 1];
        float dx = bx - ax, dy = by - ay, len = (float) Math.hypot(dx, dy);
        if (len < 1e-3f) return (float) Math.hypot(px - ax, py - ay);
        return Math.abs(dy * px - dx * py + bx * ay - by * ax) / len;
    }

    static float length(float[] s) {
        float d = 0;
        for (int i = 2; i + 1 < s.length; i += 2) d += (float) Math.hypot(s[i] - s[i - 2], s[i + 1] - s[i - 1]);
        return d;
    }
}
