package salve.sketch;

import java.util.List;

/**
 * Grosor de línea con intención, como dibuja una persona: el contorno exterior y las líneas largas
 * que dan la forma, más gruesos; los detalles cortos del interior, más finos. Devuelve un factor por
 * trazo (1 = grosor normal). Puro: se prueba sin el móvil.
 */
public final class StrokeWeight {
    private StrokeWeight() { }

    public static final float CONTOUR = 1.5f, NORMAL = 1f, DETAIL = .72f;

    /** @param uniform todos iguales (p. ej. su autorretrato, que ya está diseñado así) */
    public static float[] weights(List<float[]> strokes, boolean uniform) {
        int n = strokes == null ? 0 : strokes.size();
        float[] out = new float[n];
        java.util.Arrays.fill(out, NORMAL);
        if (uniform || n < 3) return out;
        float[] all = PartPlan.bounds(strokes);
        if (all == null) return out;
        float w = Math.max(1, all[2] - all[0]), h = Math.max(1, all[3] - all[1]);
        float edge = .035f * Math.max(w, h);
        float[] length = new float[n];
        float longest = 0;
        for (int i = 0; i < n; i++) { length[i] = length(strokes.get(i)); longest = Math.max(longest, length[i]); }
        for (int i = 0; i < n; i++) {
            float[] s = strokes.get(i);
            if (s == null || s.length < 4) continue;
            float[] b = PartPlan.bounds(java.util.Collections.singletonList(s));
            // Toca el borde del dibujo y es apreciable: es parte del contorno.
            boolean touches = b[0] - all[0] < edge || all[2] - b[2] < edge || b[1] - all[1] < edge || all[3] - b[3] < edge;
            float size = Math.max(w, h);
            boolean big = length[i] >= .45f * longest;
            if ((touches && length[i] >= .12f * size) || big) out[i] = CONTOUR;
            else if (length[i] < .25f * size) out[i] = DETAIL; // corto respecto al dibujo: un ojo, una arruga
        }
        return out;
    }

    static float length(float[] s) {
        float d = 0;
        if (s != null) for (int i = 2; i + 1 < s.length; i += 2) d += (float) Math.hypot(s[i] - s[i - 2], s[i + 1] - s[i - 1]);
        return d;
    }
}
