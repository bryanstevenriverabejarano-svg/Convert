package salve.sketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Su propia representación matemática de un dibujo: cada línea pasa a ser una cadena de curvas de
 * Bézier cúbicas (dos extremos y dos puntos de control por tramo), ajustadas por mínimos cuadrados al
 * contorno (algoritmo de Schneider). Ya no son los píxeles de una referencia: son sus vectores, con
 * los que puede redibujar, deformar o variar la figura. Lienzo 0..1000.
 */
public final class BezierSketch {
    /** Tramos de una línea; cada tramo: x0 y0 c1x c1y c2x c2y x1 y1. */
    public final List<List<float[]>> curves;

    private BezierSketch(List<List<float[]>> curves) { this.curves = curves; }

    public int segments() { int n = 0; for (List<float[]> c : curves) n += c.size(); return n; }
    public int controlPoints() { return segments() * 2; }

    public String describe() {
        return "lo convertí en mis propios vectores: " + curves.size() + " líneas hechas de " + segments()
                + " curvas de Bézier con " + controlPoints() + " puntos de control";
    }

    /** Ajusta curvas a cada trazo. @param tolerance error máximo en unidades del lienzo (≈ 4–10) */
    public static BezierSketch fit(List<float[]> strokes, float tolerance) {
        List<List<float[]>> out = new ArrayList<>();
        for (float[] s : strokes) {
            float[] p = dedupe(s);
            if (p.length < 4) continue;
            List<float[]> segs = new ArrayList<>();
            int n = p.length / 2;
            if (n == 2) segs.add(line(p[0], p[1], p[2], p[3]));
            else {
                float[] t0 = unit(p[2] - p[0], p[3] - p[1]);
                float[] t1 = unit(p[2 * n - 4] - p[2 * n - 2], p[2 * n - 3] - p[2 * n - 1]);
                fitRange(p, 0, n - 1, t0, t1, tolerance, segs, 0);
            }
            if (!segs.isEmpty()) out.add(segs);
        }
        return new BezierSketch(out);
    }

    /** Vuelve a trazos (puntos) para los epiciclos. */
    public List<float[]> sample(int perSegment) {
        List<float[]> out = new ArrayList<>();
        for (List<float[]> line : curves) {
            int count = line.size() * perSegment + 1;
            float[] r = new float[2 * count];
            int k = 0;
            for (int si = 0; si < line.size(); si++) {
                float[] b = line.get(si);
                for (int i = si == 0 ? 0 : 1; i <= perSegment; i++) {
                    float[] q = point(b, i / (float) perSegment);
                    r[k++] = clamp(q[0]); r[k++] = clamp(q[1]);
                }
            }
            out.add(java.util.Arrays.copyOf(r, k));
        }
        return out;
    }

    /**
     * Variación experimental: deforma todos los puntos con un campo suave (sinusoidal) — los tramos
     * siguen unidos porque el mismo punto se desplaza igual. Sirve para «otra pose» o «otra versión».
     * @param amount desplazamiento máximo en unidades del lienzo (≈ 20–60)
     */
    public BezierSketch vary(long seed, float amount) {
        Random r = new Random(seed);
        float fx = .004f + r.nextFloat() * .006f, fy = .004f + r.nextFloat() * .006f;
        float px = r.nextFloat() * 6.3f, py = r.nextFloat() * 6.3f, lean = (r.nextFloat() - .5f) * .25f;
        List<List<float[]>> out = new ArrayList<>();
        for (List<float[]> line : curves) {
            List<float[]> segs = new ArrayList<>();
            for (float[] b : line) {
                float[] m = new float[8];
                for (int i = 0; i < 8; i += 2) {
                    float x = b[i], y = b[i + 1];
                    float height = 1 - y / 1000f; // más movimiento arriba (cabeza, brazos) que en la base
                    m[i] = clamp(x + amount * (float) Math.sin(y * fy + px) * height + lean * (500 - y) * .2f);
                    m[i + 1] = clamp(y + amount * .6f * (float) Math.sin(x * fx + py) * height);
                }
                segs.add(m);
            }
            out.add(segs);
        }
        return new BezierSketch(out);
    }

    // ───── ajuste (Schneider) ─────

    private static void fitRange(float[] p, int first, int last, float[] t0, float[] t1, float tol, List<float[]> out, int depth) {
        int count = last - first + 1;
        if (count == 2) {
            float d = dist(p, first, last) / 3;
            out.add(new float[]{p[2 * first], p[2 * first + 1], p[2 * first] + t0[0] * d, p[2 * first + 1] + t0[1] * d,
                    p[2 * last] + t1[0] * d, p[2 * last + 1] + t1[1] * d, p[2 * last], p[2 * last + 1]});
            return;
        }
        float[] u = chordParams(p, first, last);
        float[] bez = generate(p, first, last, u, t0, t1);
        int[] split = new int[1];
        float err = maxError(p, first, last, bez, u, split);
        if (err <= tol || depth > 10) { out.add(bez); return; }
        int s = Math.max(first + 1, Math.min(last - 1, split[0]));
        float[] tc = unit(p[2 * (s - 1)] - p[2 * (s + 1)], p[2 * (s - 1) + 1] - p[2 * (s + 1) + 1]);
        fitRange(p, first, s, t0, tc, tol, out, depth + 1);
        fitRange(p, s, last, new float[]{-tc[0], -tc[1]}, t1, tol, out, depth + 1);
    }

    private static float[] generate(float[] p, int first, int last, float[] u, float[] t0, float[] t1) {
        float c00 = 0, c01 = 0, c11 = 0, x0 = 0, x1 = 0;
        float ax = p[2 * first], ay = p[2 * first + 1], bx = p[2 * last], by = p[2 * last + 1];
        for (int i = 0; i < u.length; i++) {
            float t = u[i], mt = 1 - t;
            float b0 = mt * mt * mt, b1 = 3 * t * mt * mt, b2 = 3 * t * t * mt, b3 = t * t * t;
            float a1x = t0[0] * b1, a1y = t0[1] * b1, a2x = t1[0] * b2, a2y = t1[1] * b2;
            c00 += a1x * a1x + a1y * a1y; c01 += a1x * a2x + a1y * a2y; c11 += a2x * a2x + a2y * a2y;
            int k = first + i;
            float tx = p[2 * k] - (ax * (b0 + b1) + bx * (b2 + b3)), ty = p[2 * k + 1] - (ay * (b0 + b1) + by * (b2 + b3));
            x0 += a1x * tx + a1y * ty; x1 += a2x * tx + a2y * ty;
        }
        float det = c00 * c11 - c01 * c01;
        float alpha0 = 0, alpha1 = 0;
        if (Math.abs(det) > 1e-6f) { alpha0 = (x0 * c11 - c01 * x1) / det; alpha1 = (c00 * x1 - x0 * c01) / det; }
        float seg = (float) Math.hypot(bx - ax, by - ay);
        if (alpha0 < 1e-3f * seg || alpha1 < 1e-3f * seg || !Float.isFinite(alpha0) || !Float.isFinite(alpha1)) alpha0 = alpha1 = seg / 3;
        return new float[]{ax, ay, ax + t0[0] * alpha0, ay + t0[1] * alpha0, bx + t1[0] * alpha1, by + t1[1] * alpha1, bx, by};
    }

    private static float maxError(float[] p, int first, int last, float[] bez, float[] u, int[] split) {
        float max = 0;
        split[0] = (first + last) / 2;
        for (int i = 1; i < u.length - 1; i++) {
            float[] q = point(bez, u[i]);
            int k = first + i;
            float d = (float) Math.hypot(q[0] - p[2 * k], q[1] - p[2 * k + 1]);
            if (d > max) { max = d; split[0] = k; }
        }
        return max;
    }

    private static float[] chordParams(float[] p, int first, int last) {
        float[] u = new float[last - first + 1];
        for (int i = first + 1; i <= last; i++) u[i - first] = u[i - first - 1] + dist(p, i - 1, i);
        float total = u[u.length - 1];
        for (int i = 1; i < u.length; i++) u[i] = total <= 0 ? i / (float) (u.length - 1) : u[i] / total;
        return u;
    }

    static float[] point(float[] b, float t) {
        float mt = 1 - t, b0 = mt * mt * mt, b1 = 3 * t * mt * mt, b2 = 3 * t * t * mt, b3 = t * t * t;
        return new float[]{b0 * b[0] + b1 * b[2] + b2 * b[4] + b3 * b[6], b0 * b[1] + b1 * b[3] + b2 * b[5] + b3 * b[7]};
    }

    private static float[] line(float x0, float y0, float x1, float y1) {
        return new float[]{x0, y0, x0 + (x1 - x0) / 3, y0 + (y1 - y0) / 3, x0 + 2 * (x1 - x0) / 3, y0 + 2 * (y1 - y0) / 3, x1, y1};
    }

    private static float[] dedupe(float[] s) {
        if (s == null) return new float[0];
        List<Float> out = new ArrayList<>();
        for (int i = 0; i + 1 < s.length; i += 2) {
            int n = out.size();
            if (n >= 2 && Math.abs(out.get(n - 2) - s[i]) < .01f && Math.abs(out.get(n - 1) - s[i + 1]) < .01f) continue;
            out.add(s[i]); out.add(s[i + 1]);
        }
        float[] r = new float[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    private static float dist(float[] p, int a, int b) { return (float) Math.hypot(p[2 * b] - p[2 * a], p[2 * b + 1] - p[2 * a + 1]); }

    private static float[] unit(float x, float y) {
        float l = (float) Math.hypot(x, y);
        return l < 1e-6f ? new float[]{1, 0} : new float[]{x / l, y / l};
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1000, v)); }
}
