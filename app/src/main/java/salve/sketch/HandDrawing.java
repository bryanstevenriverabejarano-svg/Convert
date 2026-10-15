package salve.sketch;

import java.util.ArrayList;
import java.util.List;

/** Convierte lo que Bryan dibuja con el dedo en líneas del lienzo de Salve (suavizadas y normalizadas). */
public final class HandDrawing {
    private HandDrawing() { }

    public static final class Result {
        public final List<float[]> strokes;
        public final float aspect;
        Result(List<float[]> strokes, float aspect) { this.strokes = strokes; this.aspect = aspect; }
    }

    /** @param raw trazos en píxeles de pantalla (x0,y0,x1,y1…), en el orden en que se dibujaron */
    public static Result normalize(List<float[]> raw) {
        List<float[]> clean = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (float[] s : raw) {
            if (s == null || s.length < 2) continue;
            if (s.length < 4) s = new float[]{s[0], s[1], s[0] + 1.5f, s[1] + 1.5f}; // un toque es un punto
            float[] smooth = smooth(s);
            clean.add(smooth);
            for (int i = 0; i < smooth.length; i += 2) {
                minX = Math.min(minX, smooth[i]); maxX = Math.max(maxX, smooth[i]);
                minY = Math.min(minY, smooth[i + 1]); maxY = Math.max(maxY, smooth[i + 1]);
            }
        }
        List<float[]> out = new ArrayList<>();
        if (clean.isEmpty()) return new Result(out, 1f);
        float w = Math.max(1f, maxX - minX), h = Math.max(1f, maxY - minY);
        float scale = 1000f / Math.max(w, h), ox = (1000 - w * scale) / 2, oy = (1000 - h * scale) / 2;
        float total = 0;
        for (float[] s : clean) for (int i = 2; i < s.length; i += 2) total += Math.hypot(s[i] - s[i - 2], s[i + 1] - s[i - 1]);
        float spacing = Math.max(.5f, total / 1600f);
        for (float[] s : clean) {
            float[] r = ImageTracer.resample(s, spacing);
            if (r.length < 4) {
                // Un toque suelto (un punto, un ojo) se convierte en un trazo mínimo para no perderlo.
                r = new float[]{r[0], r[1], r[0] + 1, r[1] + 1};
            }
            for (int i = 0; i < r.length; i += 2) { r[i] = (r[i] - minX) * scale + ox; r[i + 1] = (r[i + 1] - minY) * scale + oy; }
            out.add(r);
        }
        return new Result(out, w / h);
    }

    /** Media móvil de 3 puntos: quita el temblor del dedo sin redondear las esquinas en exceso. */
    static float[] smooth(float[] s) {
        int n = s.length / 2;
        if (n < 3) return s.clone();
        float[] r = s.clone();
        for (int i = 1; i < n - 1; i++) {
            r[2 * i] = (s[2 * i - 2] + 2 * s[2 * i] + s[2 * i + 2]) / 4f;
            r[2 * i + 1] = (s[2 * i - 1] + 2 * s[2 * i + 1] + s[2 * i + 3]) / 4f;
        }
        return r;
    }
}
