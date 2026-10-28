package salve.sketch;

/**
 * Prepara una foto para calcarla como lo haría una persona: sólo el sujeto, sin fondo, y sin la
 * textura que confunde al calco (escamas, pelo, sombras). A partir de la máscara de «qué píxeles son
 * el sujeto» (la da la segmentación del móvil), el fondo pasa a blanco y el interior se suaviza y se
 * aclara: así el contorno sale limpio y sólo sobreviven los rasgos marcados (ojos, boca, patas).
 *
 * <p>Geometría pura sobre píxeles ARGB, sin Android: se prueba sin el móvil.</p>
 */
public final class SubjectMask {
    private SubjectMask() { }

    /** Con menos o más que esto, la máscara no separa un sujeto (foto vacía o todo es «sujeto»). */
    public static final float MIN_COVERAGE = .03f, MAX_COVERAGE = .92f;
    private static final float THRESHOLD = .5f;

    /** Fracción de la imagen que es sujeto. */
    public static float coverage(float[] mask) {
        if (mask == null || mask.length == 0) return 0;
        int n = 0;
        for (float v : mask) if (v >= THRESHOLD) n++;
        return n / (float) mask.length;
    }

    /** ¿Sirve esta máscara para recortar? (ni vacía ni la imagen entera). */
    public static boolean usable(float[] mask, int width, int height) {
        if (mask == null || mask.length != width * height) return false;
        float c = coverage(mask);
        return c >= MIN_COVERAGE && c <= MAX_COVERAGE;
    }

    /**
     * La foto lista para calcar: fondo blanco; dentro del sujeto, gris suavizado y aclarado (pero
     * siempre bastante más oscuro que el fondo, para que el contorno no se pierda aunque el sujeto
     * sea blanco). Devuelve píxeles ARGB nuevos en escala de grises.
     */
    public static int[] cutout(int[] argb, float[] mask, int width, int height) {
        if (argb == null || mask == null || argb.length < width * height || mask.length < width * height)
            throw new IllegalArgumentException("Tamaños no coinciden");
        int n = width * height;
        float[] lum = new float[n];
        for (int i = 0; i < n; i++) {
            int c = argb[i];
            float a = ((c >>> 24) & 255) / 255f;
            float y = .299f * ((c >> 16) & 255) + .587f * ((c >> 8) & 255) + .114f * (c & 255);
            lum[i] = y * a + 255f * (1 - a);
        }
        // Suaviza la textura: dos pasadas de caja ≈ desenfoque gaussiano, radio según el tamaño.
        int r = Math.max(2, Math.round(Math.max(width, height) / 80f));
        float[] soft = boxBlur(boxBlur(lum, width, height, r), width, height, r);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            int g;
            if (mask[i] >= THRESHOLD) {
                // Aclarado (la textura se queda en matices suaves) pero nunca casi blanco: el contorno se ve.
                float v = 255f - (255f - soft[i]) * .6f;
                g = Math.round(Math.max(0, Math.min(205, v)));
            } else g = 255;
            out[i] = 0xFF000000 | (g << 16) | (g << 8) | g;
        }
        return out;
    }

    /** Caja de recorte del sujeto en fracciones {x1,y1,x2,y2} (con un margen), o null si no hay sujeto. */
    public static float[] bounds(float[] mask, int width, int height) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            if (mask[y * width + x] < THRESHOLD) continue;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        if (maxX < 0) return null;
        float mx = (maxX - minX) * .06f + 2, my = (maxY - minY) * .06f + 2;
        return new float[]{Math.max(0, (minX - mx) / width), Math.max(0, (minY - my) / height),
                Math.min(1, (maxX + mx + 1) / width), Math.min(1, (maxY + my + 1) / height)};
    }

    /**
     * La máscara recortada a una caja (fracciones) y remuestreada a otro tamaño (vecino más cercano),
     * para usarla sobre la foto ya recortada y reducida.
     */
    public static float[] cropResize(float[] mask, int width, int height, float[] box, int newWidth, int newHeight) {
        float x0 = box == null ? 0 : box[0] * width, y0 = box == null ? 0 : box[1] * height;
        float bw = box == null ? width : (box[2] - box[0]) * width, bh = box == null ? height : (box[3] - box[1]) * height;
        float[] out = new float[newWidth * newHeight];
        for (int y = 0; y < newHeight; y++) for (int x = 0; x < newWidth; x++) {
            int sx = Math.min(width - 1, Math.max(0, (int) (x0 + (x + .5f) * bw / newWidth)));
            int sy = Math.min(height - 1, Math.max(0, (int) (y0 + (y + .5f) * bh / newHeight)));
            out[y * newWidth + x] = mask[sy * width + sx];
        }
        return out;
    }

    static float[] boxBlur(float[] src, int w, int h, int r) {
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) {
            float sum = 0; int count = 0;
            for (int x = -r; x <= r; x++) if (x >= 0 && x < w) { sum += src[y * w + x]; count++; }
            for (int x = 0; x < w; x++) {
                tmp[y * w + x] = sum / count;
                int drop = x - r, add = x + r + 1;
                if (drop >= 0) { sum -= src[y * w + drop]; count--; }
                if (add < w) { sum += src[y * w + add]; count++; }
            }
        }
        for (int x = 0; x < w; x++) {
            float sum = 0; int count = 0;
            for (int y = -r; y <= r; y++) if (y >= 0 && y < h) { sum += tmp[y * w + x]; count++; }
            for (int y = 0; y < h; y++) {
                out[y * w + x] = sum / count;
                int drop = y - r, add = y + r + 1;
                if (drop >= 0) { sum -= tmp[drop * w + x]; count--; }
                if (add < h) { sum += tmp[add * w + x]; count++; }
            }
        }
        return out;
    }
}
