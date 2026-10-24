package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Salve analiza la geometría de una imagen y la convierte en líneas que puede trazar con epiciclos.
 *
 * Proceso (Java puro, sin red ni modelo): luminancia → suavizado gaussiano → gradiente de Sobel →
 * supresión de no máximos → histéresis (bordes finos y continuos) → encadenado de píxeles en
 * polilíneas → simplificación de Douglas–Peucker → se conservan las líneas más largas y nítidas →
 * orden de pluma por cercanía. Además resume la geometría (rectas horizontales, verticales,
 * diagonales, curvas, contornos cerrados, simetría) para que ella pueda hablar de lo que ve.
 */
public final class ImageTracer {
    public static final int MAX_SIDE = 320;

    public static final class Result {
        /** Trazos normalizados al lienzo 0..1000 (proporción conservada). */
        public final List<float[]> strokes;
        public final float aspect;
        public final Geometry geometry;
        Result(List<float[]> strokes, float aspect, Geometry geometry) {
            this.strokes = Collections.unmodifiableList(strokes); this.aspect = aspect; this.geometry = geometry;
        }
        public boolean isUsable() { return strokes.size() >= 1 && geometry.lines + geometry.curves >= 2; }
    }

    /** Lo que se ve, en números: sirve para describir y para decidir si merece la pena dibujarlo. */
    public static final class Geometry {
        public final int lines, horizontal, vertical, diagonal, curves, closed, strokes;
        public final float symmetry, aspect;
        Geometry(int lines, int horizontal, int vertical, int diagonal, int curves, int closed, int strokes,
                 float symmetry, float aspect) {
            this.lines = lines; this.horizontal = horizontal; this.vertical = vertical; this.diagonal = diagonal;
            this.curves = curves; this.closed = closed; this.strokes = strokes; this.symmetry = symmetry; this.aspect = aspect;
        }

        /** Resumen en español para el contexto de Salve. */
        public String describe() {
            StringBuilder b = new StringBuilder();
            b.append(strokes).append(" trazos: ").append(lines).append(" tramos rectos (")
                    .append(horizontal).append(" horizontales, ").append(vertical).append(" verticales, ")
                    .append(diagonal).append(" diagonales) y ").append(curves).append(" curvas");
            if (closed > 0) b.append("; ").append(closed).append(closed == 1 ? " contorno cerrado" : " contornos cerrados");
            int straight = horizontal + vertical;
            if (lines > 4 && straight >= lines * .7f) b.append("; predominan ángulos rectos, formas rectangulares");
            else if (curves > lines) b.append("; predominan las curvas, formas orgánicas o redondeadas");
            if (symmetry > .62f) b.append("; bastante simétrica de izquierda a derecha");
            b.append(String.format(Locale.ROOT, "; proporción %.2f (ancho/alto)", aspect));
            return b.append('.').toString();
        }
    }

    private ImageTracer() { }

    /**
     * @param argb píxeles ARGB (como Bitmap.getPixels), lado mayor ≤ {@link #MAX_SIDE} recomendado
     * @param pointBudget puntos totales tras el remuestreo (≈ duración y coste de la DFT)
     */
    public static Result trace(int[] argb, int width, int height, int pointBudget) {
        return trace(argb, width, height, pointBudget, false);
    }

    /**
     * @param subjectOnly true cuando le enseñan un objeto ("esto es un mueble"): se queda con el grupo
     *                    de líneas principal, cerca del centro, y descarta horizontes y marcos del fondo.
     */
    public static Result trace(int[] argb, int width, int height, int pointBudget, boolean subjectOnly) {
        if (argb == null || width < 8 || height < 8 || argb.length < width * height)
            throw new IllegalArgumentException("Imagen demasiado pequeña");
        float[] lum = luminance(argb, width, height);
        float[] gray = blur(lum, width, height);
        float[] mag = new float[width * height];
        byte[] dir = new byte[width * height];
        gradient(gray, width, height, mag, dir);
        // Un dibujo de líneas (papel, pizarra, trazos de luz) se sigue por el centro de cada línea;
        // una foto, por sus bordes.
        boolean[] lineArt = lineArtSkeleton(lum, width, height);
        boolean[] edges = lineArt != null ? lineArt : hysteresis(suppress(mag, dir, width, height), width, height);
        if (lineArt != null) for (int i = 0; i < mag.length; i++) if (lineArt[i]) mag[i] = Math.max(mag[i], 120f);
        float diagonal = (float) Math.hypot(width, height);
        List<int[]> chains = join(link(edges, width, height), joinGap(diagonal));
        List<Chain> kept = select(chains, mag, width, diagonal);
        if (subjectOnly) kept = subject(kept, width, height);
        order(kept);
        Geometry geometry = analyze(kept, edges, width, height);
        return new Result(normalize(kept, width, height, Math.max(200, pointBudget)), width / (float) height, geometry);
    }

    // ───── 1. luminancia y suavizado ─────
    static float[] luminance(int[] argb, int w, int h) {
        float[] out = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            int c = argb[i];
            float a = ((c >>> 24) & 255) / 255f;
            float y = .299f * ((c >> 16) & 255) + .587f * ((c >> 8) & 255) + .114f * (c & 255);
            out[i] = y * a + 255f * (1 - a); // lo transparente cuenta como papel blanco
        }
        return out;
    }

    private static float[] blur(float[] src, int w, int h) {
        float[] k = {.0702f, .1311f, .1907f, .2161f, .1907f, .1311f, .0702f}; // σ≈1,4
        float[] tmp = new float[w * h], out = new float[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float s = 0;
            for (int j = -3; j <= 3; j++) s += k[j + 3] * src[y * w + clamp(x + j, 0, w - 1)];
            tmp[y * w + x] = s;
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float s = 0;
            for (int j = -3; j <= 3; j++) s += k[j + 3] * tmp[clamp(y + j, 0, h - 1) * w + x];
            out[y * w + x] = s;
        }
        return out;
    }

    // ───── 2. gradiente y bordes finos ─────
    private static void gradient(float[] g, int w, int h, float[] mag, byte[] dir) {
        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
            int i = y * w + x;
            float gx = -g[i - w - 1] - 2 * g[i - 1] - g[i + w - 1] + g[i - w + 1] + 2 * g[i + 1] + g[i + w + 1];
            float gy = -g[i - w - 1] - 2 * g[i - w] - g[i - w + 1] + g[i + w - 1] + 2 * g[i + w] + g[i + w + 1];
            mag[i] = (float) Math.hypot(gx, gy);
            double a = Math.toDegrees(Math.atan2(gy, gx));
            if (a < 0) a += 180;
            dir[i] = (byte) (a < 22.5 || a >= 157.5 ? 0 : a < 67.5 ? 1 : a < 112.5 ? 2 : 3);
        }
    }

    private static float[] suppress(float[] mag, byte[] dir, int w, int h) {
        float[] out = new float[w * h];
        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
            int i = y * w + x;
            float m = mag[i], a, b;
            switch (dir[i]) {
                case 0: a = mag[i - 1]; b = mag[i + 1]; break;
                case 1: a = mag[i - w - 1]; b = mag[i + w + 1]; break;
                case 2: a = mag[i - w]; b = mag[i + w]; break;
                default: a = mag[i - w + 1]; b = mag[i + w - 1]; break;
            }
            if (m >= a && m > b) out[i] = m;
        }
        return out;
    }

    private static boolean[] hysteresis(float[] nms, int w, int h) {
        int count = 0;
        for (float v : nms) if (v > 0) count++;
        boolean[] edge = new boolean[w * h];
        if (count == 0) return edge;
        float[] values = new float[count];
        int k = 0;
        for (float v : nms) if (v > 0) values[k++] = v;
        java.util.Arrays.sort(values);
        // Umbral adaptativo: el 18% de los bordes más nítidos inicia línea; un mínimo evita trazar ruido.
        float high = Math.max(40f, values[(int) (count * .82f)]);
        float low = high * .42f;
        int[] stack = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            if (nms[i] < high || edge[i]) continue;
            int top = 0;
            stack[top++] = i;
            edge[i] = true;
            while (top > 0) {
                int p = stack[--top], px = p % w, py = p / w;
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                    int qx = px + dx, qy = py + dy;
                    if (qx < 1 || qy < 1 || qx >= w - 1 || qy >= h - 1) continue;
                    int q = qy * w + qx;
                    if (!edge[q] && nms[q] >= low) { edge[q] = true; stack[top++] = q; }
                }
            }
        }
        return edge;
    }

    // ───── 3. encadenar píxeles en líneas ─────
    private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1}, DY = {0, 1, 1, 1, 0, -1, -1, -1};

    private static List<int[]> link(boolean[] edge, int w, int h) {
        boolean[] used = new boolean[w * h];
        List<int[]> chains = new ArrayList<>();
        // Primero desde extremos (un solo vecino); luego lo que quede (lazos cerrados).
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < w * h; i++) {
                if (!edge[i] || used[i]) continue;
                if (pass == 0 && neighbours(edge, i, w, h) != 1) continue;
                chains.add(walk(edge, used, i, w, h));
            }
        }
        return chains;
    }

    /** Separación máxima, en píxeles, entre extremos que se consideran la misma línea. */
    static float joinGap(float diagonal) { return Math.max(2.5f, diagonal * .008f); }

    /**
     * El seguidor corta cada línea al llegar a un cruce (donde el ala toca el cuerpo, por ejemplo) y deja
     * trozos sueltos con huecos de uno o dos píxeles. Aquí se vuelven a empalmar por sus extremos más
     * cercanos, para que el dibujo tenga contornos continuos en vez de rayas partidas.
     */
    static List<int[]> join(List<int[]> chains, float gap) {
        int n = chains.size();
        if (n < 2 || n > 6000) return chains;
        int cell = Math.max(1, (int) Math.ceil(gap));
        java.util.Map<Long, List<Integer>> grid = new java.util.HashMap<>();
        boolean[] loop = new boolean[n];
        for (int i = 0; i < n; i++) {
            int[] c = chains.get(i);
            loop[i] = isLoop(c);
            if (loop[i] || c.length < 4) continue;
            for (int end = 0; end < 2; end++) {
                int x = c[end == 0 ? 0 : c.length - 2], y = c[end == 0 ? 1 : c.length - 1];
                grid.computeIfAbsent(key(x / cell, y / cell), k -> new ArrayList<>()).add(2 * i + end);
            }
        }
        boolean[] used = new boolean[n];
        List<int[]> out = new ArrayList<>();
        float gap2 = gap * gap;
        for (int i = 0; i < n; i++) {
            if (used[i]) continue;
            used[i] = true;
            int[] cur = chains.get(i);
            if (loop[i] || cur.length < 4) { out.add(cur); continue; }
            for (int side = 0; side < 2; side++) {
                while (true) {
                    int tx = cur[cur.length - 2], ty = cur[cur.length - 1];
                    int best = -1; float bestD = Float.MAX_VALUE;
                    for (int gy = ty / cell - 1; gy <= ty / cell + 1; gy++) for (int gx = tx / cell - 1; gx <= tx / cell + 1; gx++) {
                        List<Integer> ends = grid.get(key(gx, gy));
                        if (ends == null) continue;
                        for (int e : ends) {
                            if (used[e / 2]) continue;
                            int[] o = chains.get(e / 2);
                            int ox = o[e % 2 == 0 ? 0 : o.length - 2], oy = o[e % 2 == 0 ? 1 : o.length - 1];
                            float d = (ox - tx) * (ox - tx) + (oy - ty) * (oy - ty);
                            if (d <= gap2 && d < bestD) { bestD = d; best = e; }
                        }
                    }
                    if (best < 0) break;
                    used[best / 2] = true;
                    int[] next = chains.get(best / 2);
                    cur = concat(cur, best % 2 == 0 ? next : reverse(next));
                }
                cur = reverse(cur); // ahora alarga por el otro extremo
            }
            int m = cur.length / 2;
            if (m > 8 && !isLoop(cur) && Math.abs(cur[0] - cur[2 * m - 2]) <= 1 && Math.abs(cur[1] - cur[2 * m - 1]) <= 1) {
                cur = java.util.Arrays.copyOf(cur, cur.length + 2);
                cur[cur.length - 2] = cur[0]; cur[cur.length - 1] = cur[1];
            }
            out.add(cur);
        }
        return out;
    }

    private static boolean isLoop(int[] c) {
        return c.length / 2 > 8 && c[0] == c[c.length - 2] && c[1] == c[c.length - 1];
    }

    private static long key(int x, int y) { return ((long) x << 32) ^ (y & 0xffffffffL); }

    private static int[] reverse(int[] c) {
        int[] r = new int[c.length];
        for (int i = 0, n = c.length / 2; i < n; i++) { r[2 * i] = c[2 * (n - 1 - i)]; r[2 * i + 1] = c[2 * (n - 1 - i) + 1]; }
        return r;
    }

    private static int[] concat(int[] a, int[] b) {
        boolean same = a[a.length - 2] == b[0] && a[a.length - 1] == b[1];
        int skip = same ? 2 : 0;
        int[] r = java.util.Arrays.copyOf(a, a.length + b.length - skip);
        System.arraycopy(b, skip, r, a.length, b.length - skip);
        return r;
    }

    private static int neighbours(boolean[] edge, int i, int w, int h) {
        int x = i % w, y = i / w, n = 0;
        for (int d = 0; d < 8; d++) {
            int qx = x + DX[d], qy = y + DY[d];
            if (qx >= 0 && qy >= 0 && qx < w && qy < h && edge[qy * w + qx]) n++;
        }
        return n;
    }

    private static int[] walk(boolean[] edge, boolean[] used, int start, int w, int h) {
        int[] buf = new int[64];
        int n = 0, p = start, last = -1;
        while (true) {
            if (n * 2 + 2 > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
            buf[2 * n] = p % w; buf[2 * n + 1] = p / w; n++;
            used[p] = true;
            int x = p % w, y = p / w, best = -1, bestScore = Integer.MIN_VALUE;
            for (int d = 0; d < 8; d++) {
                int qx = x + DX[d], qy = y + DY[d];
                if (qx < 0 || qy < 0 || qx >= w || qy >= h) continue;
                int q = qy * w + qx;
                if (!edge[q] || used[q]) continue;
                // Prefiere seguir recto: menos giro respecto al paso anterior; los ortogonales antes que diagonales.
                int score = (d % 2 == 0 ? 2 : 1) - (last < 0 ? 0 : Math.min(Math.abs(d - last), 8 - Math.abs(d - last)) * 2);
                if (score > bestScore) { bestScore = score; best = d; }
            }
            if (best < 0) break;
            last = best;
            p = (y + DY[best]) * w + (x + DX[best]);
        }
        // Cierra el lazo si acaba junto a su inicio.
        int[] chain = java.util.Arrays.copyOf(buf, 2 * n);
        if (n > 8 && Math.abs(chain[0] - chain[2 * n - 2]) <= 1 && Math.abs(chain[1] - chain[2 * n - 1]) <= 1) {
            chain = java.util.Arrays.copyOf(chain, 2 * n + 2);
            chain[2 * n] = chain[0]; chain[2 * n + 1] = chain[1];
        }
        return chain;
    }


    // ───── dibujos de líneas: esqueleto en vez de bordes ─────
    /** Si la imagen es un dibujo de trazos finos sobre fondo liso, devuelve su esqueleto; si no, null. */
    static boolean[] lineArtSkeleton(float[] lum, int w, int h) {
        float[] sorted = lum.clone();
        java.util.Arrays.sort(sorted);
        float background = sorted[sorted.length / 2];
        float spread = sorted[(int) (sorted.length * .75f)] - sorted[(int) (sorted.length * .25f)];
        if (spread > 28) return null; // fondo con textura o foto: no es un dibujo de líneas
        boolean[] ink = new boolean[w * h];
        int count = 0;
        for (int i = 0; i < ink.length; i++) if (Math.abs(lum[i] - background) > 45) { ink[i] = true; count++; }
        if (count < 40 || count > ink.length * .16f) return null;
        boolean[] skeleton = thin(ink, w, h);
        int length = 0;
        for (boolean b : skeleton) if (b) length++;
        // Trazos finos: la tinta apenas es unas veces más ancha que su esqueleto.
        return length > 0 && count / (float) length < 7f ? skeleton : null;
    }

    /** Adelgazamiento de Zhang–Suen. */
    static boolean[] thin(boolean[] src, int w, int h) {
        boolean[] img = src.clone();
        List<Integer> remove = new ArrayList<>();
        boolean changed = true;
        for (int iter = 0; changed && iter < 60; iter++) {
            changed = false;
            for (int step = 0; step < 2; step++) {
                remove.clear();
                for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
                    int i = y * w + x;
                    if (!img[i]) continue;
                    boolean p2 = img[i - w], p3 = img[i - w + 1], p4 = img[i + 1], p5 = img[i + w + 1],
                            p6 = img[i + w], p7 = img[i + w - 1], p8 = img[i - 1], p9 = img[i - w - 1];
                    boolean[] ring = {p2, p3, p4, p5, p6, p7, p8, p9, p2};
                    int b = 0, a = 0;
                    for (int k = 0; k < 8; k++) { if (ring[k]) b++; if (!ring[k] && ring[k + 1]) a++; }
                    if (b < 2 || b > 6 || a != 1) continue;
                    if (step == 0 ? (p2 && p4 && p6) || (p4 && p6 && p8) : (p2 && p4 && p8) || (p2 && p6 && p8)) continue;
                    remove.add(i);
                }
                for (int i : remove) img[i] = false;
                if (!remove.isEmpty()) changed = true;
            }
        }
        return img;
    }

    // ───── sujeto principal ─────
    /** Agrupa líneas cercanas y se queda con el grupo más importante y céntrico. */
    private static List<Chain> subject(List<Chain> chains, int w, int h) {
        if (chains.size() < 3) return chains;
        float diagonal = (float) Math.hypot(w, h), gap = diagonal * .045f;
        List<Chain> candidates = new ArrayList<>();
        // Horizontes, rodapiés y esquinas de pared son tramos rectos larguísimos: se cortan y el
        // resto de la línea (la pata que tocaba el suelo) sigue siendo del objeto.
        for (Chain c : chains) {
            int n = c.pts.length / 2, from = 0;
            for (int i = 1; i <= n; i++) {
                boolean cut = false;
                if (i < n) {
                    float dx = Math.abs(c.pts[2 * i] - c.pts[2 * i - 2]), dy = Math.abs(c.pts[2 * i + 1] - c.pts[2 * i - 1]);
                    cut = (dx > w * .45f && dy < dx * .12f) || (dy > h * .6f && dx < dy * .12f)
                            || (Math.hypot(dx, dy) > diagonal * .08f && (border(c.pts[2 * i - 2], c.pts[2 * i - 1], w, h)
                                || border(c.pts[2 * i], c.pts[2 * i + 1], w, h)));
                }
                if (cut || i == n) {
                    if (i - from >= 2) {
                        float[] part = java.util.Arrays.copyOfRange(c.pts, 2 * from, 2 * i);
                        float len = 0;
                        for (int k = 2; k < part.length; k += 2) len += (float) Math.hypot(part[k] - part[k - 2], part[k + 1] - part[k - 1]);
                        if (len > diagonal * .02f) candidates.add(new Chain(part, len, c.strength, c.closed && from == 0 && i == n));
                    }
                    from = i;
                }
            }
        }
        if (candidates.isEmpty()) return chains;
        int n = candidates.size();
        int[] group = new int[n];
        for (int i = 0; i < n; i++) group[i] = i;
        float[][] box = new float[n][];
        for (int i = 0; i < n; i++) box[i] = bounds(candidates.get(i).pts);
        for (int i = 0; i < n; i++) for (int j = i + 1; j < n; j++)
            if (box[i][0] - gap <= box[j][2] && box[j][0] - gap <= box[i][2]
                    && box[i][1] - gap <= box[j][3] && box[j][1] - gap <= box[i][3]
                    && near(candidates.get(i).pts, candidates.get(j).pts, gap)) union(group, i, j);
        java.util.Map<Integer, float[]> score = new java.util.HashMap<>(); // [peso, xmin, ymin, xmax, ymax]
        for (int i = 0; i < n; i++) {
            int g = find(group, i);
            float[] s = score.computeIfAbsent(g, k -> new float[]{0, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE});
            s[0] += candidates.get(i).score();
            s[1] = Math.min(s[1], box[i][0]); s[2] = Math.min(s[2], box[i][1]);
            s[3] = Math.max(s[3], box[i][2]); s[4] = Math.max(s[4], box[i][3]);
        }
        int best = -1; float bestValue = -1;
        for (java.util.Map.Entry<Integer, float[]> e : score.entrySet()) {
            float[] s = e.getValue();
            float cx = (s[1] + s[3]) / 2 / w - .5f, cy = (s[2] + s[4]) / 2 / h - .5f;
            float centrality = 1f - Math.min(1f, (float) Math.hypot(cx, cy) * 1.4f);
            float value = s[0] * (.35f + centrality);
            if (value > bestValue) { bestValue = value; best = e.getKey(); }
        }
        List<Chain> out = new ArrayList<>();
        for (int i = 0; i < n; i++) if (find(group, i) == best) out.add(candidates.get(i));
        return out.isEmpty() ? chains : out;
    }

    private static boolean near(float[] a, float[] b, float gap) {
        float g2 = gap * gap;
        for (int i = 0; i < a.length; i += 2) for (int j = 0; j < b.length; j += 2) {
            float dx = a[i] - b[j], dy = a[i + 1] - b[j + 1];
            if (dx * dx + dy * dy <= g2) return true;
        }
        // Los extremos simplificados pueden quedar lejos aunque los tramos se crucen: prueba los cruces.
        for (int i = 2; i < a.length; i += 2) for (int j = 2; j < b.length; j += 2)
            if (cross(a[i - 2], a[i - 1], a[i], a[i + 1], b[j - 2], b[j - 1], b[j], b[j + 1])) return true;
        return false;
    }

    private static boolean cross(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy) {
        float d1 = (dx - cx) * (ay - cy) - (dy - cy) * (ax - cx), d2 = (dx - cx) * (by - cy) - (dy - cy) * (bx - cx);
        float d3 = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax), d4 = (bx - ax) * (dy - ay) - (by - ay) * (dx - ax);
        return d1 * d2 < 0 && d3 * d4 < 0;
    }

    /** Un tramo que sale del encuadre pertenece al fondo, no al objeto que me enseñan. */
    private static boolean border(float x, float y, int w, int h) {
        float m = Math.max(3f, Math.min(w, h) * .02f);
        return x < m || y < m || x > w - 1 - m || y > h - 1 - m;
    }

    private static float[] bounds(float[] p) {
        float[] b = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (int i = 0; i < p.length; i += 2) {
            b[0] = Math.min(b[0], p[i]); b[1] = Math.min(b[1], p[i + 1]);
            b[2] = Math.max(b[2], p[i]); b[3] = Math.max(b[3], p[i + 1]);
        }
        return b;
    }

    private static int find(int[] g, int i) { while (g[i] != i) { g[i] = g[g[i]]; i = g[i]; } return i; }
    private static void union(int[] g, int a, int b) { g[find(g, a)] = find(g, b); }

    // ───── 4. simplificar y quedarse con lo importante ─────
    static final class Chain {
        float[] pts;      // simplificados, en píxeles
        float length, strength;
        boolean closed;
        Chain(float[] pts, float length, float strength, boolean closed) {
            this.pts = pts; this.length = length; this.strength = strength; this.closed = closed;
        }
        float score() { return length * (float) Math.sqrt(strength); }
    }

    private static List<Chain> select(List<int[]> chains, float[] mag, int w, float diagonal) {
        List<Chain> all = new ArrayList<>();
        float minLength = Math.max(8f, diagonal * .025f);
        for (int[] c : chains) {
            int n = c.length / 2;
            if (n < 6) continue;
            float len = 0, s = 0;
            for (int i = 0; i < n; i++) {
                s += mag[c[2 * i + 1] * w + c[2 * i]];
                if (i > 0) len += (float) Math.hypot(c[2 * i] - c[2 * i - 2], c[2 * i + 1] - c[2 * i - 1]);
            }
            if (len < minLength) continue;
            float[] f = new float[c.length];
            for (int i = 0; i < c.length; i++) f[i] = c[i];
            boolean closed = n > 8 && c[0] == c[c.length - 2] && c[1] == c[c.length - 1];
            all.add(new Chain(simplify(f, 1.1f), len, s / n, closed));
        }
        all.sort((a, b) -> Float.compare(b.score(), a.score()));
        // Presupuesto de tinta: unas 9 diagonales de línea y como mucho 70 trazos.
        float budget = diagonal * 9f, used = 0;
        List<Chain> kept = new ArrayList<>();
        for (Chain c : all) {
            if (kept.size() >= 70) break;
            // Si una línea larga no cabe, sigue mirando: las cortas que caben son detalles (cara, manos).
            if (used > diagonal * 2 && used + c.length > budget) continue;
            kept.add(c);
            used += c.length;
        }
        return kept;
    }

    /** Douglas–Peucker iterativo. */
    static float[] simplify(float[] p, float epsilon) {
        int n = p.length / 2;
        if (n <= 2) return p.clone();
        boolean[] keep = new boolean[n];
        keep[0] = keep[n - 1] = true;
        int[] stack = new int[2 * n];
        int top = 0;
        stack[top++] = 0; stack[top++] = n - 1;
        while (top > 0) {
            int b = stack[--top], a = stack[--top];
            float ax = p[2 * a], ay = p[2 * a + 1], bx = p[2 * b], by = p[2 * b + 1];
            float dx = bx - ax, dy = by - ay, len = (float) Math.hypot(dx, dy);
            float worst = -1; int index = -1;
            for (int i = a + 1; i < b; i++) {
                float d = len < 1e-6f ? (float) Math.hypot(p[2 * i] - ax, p[2 * i + 1] - ay)
                        : Math.abs(dy * p[2 * i] - dx * p[2 * i + 1] + bx * ay - by * ax) / len;
                if (d > worst) { worst = d; index = i; }
            }
            if (worst > epsilon && index > 0) {
                keep[index] = true;
                stack[top++] = a; stack[top++] = index;
                stack[top++] = index; stack[top++] = b;
            }
        }
        int m = 0;
        for (boolean k : keep) if (k) m++;
        float[] out = new float[2 * m];
        for (int i = 0, j = 0; i < n; i++) if (keep[i]) { out[2 * j] = p[2 * i]; out[2 * j + 1] = p[2 * i + 1]; j++; }
        return out;
    }

    /** Orden de pluma: siempre el trazo más cercano a donde quedó (invirtiéndolo si conviene). */
    private static void order(List<Chain> chains) {
        if (chains.size() < 2) return;
        List<Chain> rest = new ArrayList<>(chains), out = new ArrayList<>();
        // Empieza por el trazo más largo: el contorno principal se dibuja primero.
        Chain current = rest.remove(0);
        out.add(current);
        while (!rest.isEmpty()) {
            float ex = current.pts[current.pts.length - 2], ey = current.pts[current.pts.length - 1];
            int best = 0; boolean flip = false; float bestD = Float.MAX_VALUE;
            for (int i = 0; i < rest.size(); i++) {
                float[] p = rest.get(i).pts;
                float d0 = (float) Math.hypot(p[0] - ex, p[1] - ey);
                float d1 = (float) Math.hypot(p[p.length - 2] - ex, p[p.length - 1] - ey);
                if (d0 < bestD) { bestD = d0; best = i; flip = false; }
                if (d1 < bestD) { bestD = d1; best = i; flip = true; }
            }
            current = rest.remove(best);
            if (flip) current.pts = reverse(current.pts);
            out.add(current);
        }
        chains.clear();
        chains.addAll(out);
    }

    private static float[] reverse(float[] p) {
        float[] r = new float[p.length];
        for (int i = 0, n = p.length / 2; i < n; i++) { r[2 * i] = p[2 * (n - 1 - i)]; r[2 * i + 1] = p[2 * (n - 1 - i) + 1]; }
        return r;
    }

    // ───── 5. geometría ─────
    private static Geometry analyze(List<Chain> chains, boolean[] edge, int w, int h) {
        int lines = 0, hor = 0, ver = 0, dia = 0, curves = 0, closed = 0;
        for (Chain c : chains) {
            if (c.closed) closed++;
            int n = c.pts.length / 2;
            // Curva: la dirección gira poco a poco en muchas articulaciones seguidas.
            int gentle = 0, joints = 0;
            for (int i = 1; i < n - 1; i++) {
                double a1 = Math.atan2(c.pts[2 * i + 1] - c.pts[2 * i - 1], c.pts[2 * i] - c.pts[2 * i - 2]);
                double a2 = Math.atan2(c.pts[2 * i + 3] - c.pts[2 * i + 1], c.pts[2 * i + 2] - c.pts[2 * i]);
                double turn = Math.abs(Math.toDegrees(Math.atan2(Math.sin(a2 - a1), Math.cos(a2 - a1))));
                joints++;
                if (turn > 6 && turn < 55) gentle++;
            }
            // Un rectángulo con esquinas suavizadas también gira poco a poco, pero casi todo es tramo largo.
            float total = 0, longStraight = 0;
            float[] lengths = new float[Math.max(0, n - 1)];
            for (int i = 1; i < n; i++) {
                lengths[i - 1] = (float) Math.hypot(c.pts[2 * i] - c.pts[2 * i - 2], c.pts[2 * i + 1] - c.pts[2 * i - 1]);
                total += lengths[i - 1];
            }
            for (float len : lengths) if (len >= total * .12f) longStraight += len;
            if (joints >= 3 && gentle >= joints * .5f && longStraight < total * .6f) { curves++; continue; }
            for (int i = 1; i < n; i++) {
                float dx = c.pts[2 * i] - c.pts[2 * i - 2], dy = c.pts[2 * i + 1] - c.pts[2 * i - 1];
                float len = (float) Math.hypot(dx, dy);
                if (len < Math.max(6f, Math.hypot(w, h) * .03f)) continue;
                lines++;
                double a = Math.abs(Math.toDegrees(Math.atan2(dy, dx))) % 180;
                if (a < 12 || a > 168) hor++;
                else if (Math.abs(a - 90) < 12) ver++;
                else dia++;
            }
        }
        int match = 0, total = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w / 2; x++) {
            if (!edge[y * w + x]) continue;
            total++;
            int mx = w - 1 - x;
            boolean hit = false;
            for (int d = -2; d <= 2 && !hit; d++) {
                int qx = mx + d;
                if (qx >= 0 && qx < w && edge[y * w + qx]) hit = true;
            }
            if (hit) match++;
        }
        float symmetry = total == 0 ? 0 : match / (float) total;
        return new Geometry(lines, hor, ver, dia, curves, closed, chains.size(), symmetry, w / (float) h);
    }

    // ───── 6. al lienzo de Salve ─────
    private static List<float[]> normalize(List<Chain> chains, int w, int h, int pointBudget) {
        List<float[]> out = new ArrayList<>();
        if (chains.isEmpty()) return out;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, total = 0;
        for (Chain c : chains) {
            for (int i = 0; i < c.pts.length; i += 2) {
                minX = Math.min(minX, c.pts[i]); maxX = Math.max(maxX, c.pts[i]);
                minY = Math.min(minY, c.pts[i + 1]); maxY = Math.max(maxY, c.pts[i + 1]);
            }
            total += c.length;
        }
        float scale = 1000f / Math.max(1f, Math.max(maxX - minX, maxY - minY));
        float ox = (1000 - (maxX - minX) * scale) / 2, oy = (1000 - (maxY - minY) * scale) / 2;
        float spacing = Math.max(1.5f, total / pointBudget);
        for (Chain c : chains) {
            float[] r = resample(c.pts, spacing);
            for (int i = 0; i < r.length; i += 2) {
                r[i] = (r[i] - minX) * scale + ox;
                r[i + 1] = (r[i + 1] - minY) * scale + oy;
            }
            if (r.length >= 4) out.add(r);
        }
        return out;
    }

    /** Puntos equiespaciados a lo largo de la polilínea (el trazo de Fourier queda suave). */
    static float[] resample(float[] p, float spacing) {
        int n = p.length / 2;
        List<Float> out = new ArrayList<>();
        out.add(p[0]); out.add(p[1]);
        float carry = 0;
        for (int i = 1; i < n; i++) {
            float x0 = p[2 * i - 2], y0 = p[2 * i - 1], x1 = p[2 * i], y1 = p[2 * i + 1];
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            if (len == 0) continue;
            float pos = spacing - carry;
            while (pos <= len) {
                float t = pos / len;
                out.add(x0 + (x1 - x0) * t); out.add(y0 + (y1 - y0) * t);
                pos += spacing;
            }
            carry = len - (pos - spacing);
        }
        float lx = p[2 * n - 2], ly = p[2 * n - 1];
        if (Math.hypot(out.get(out.size() - 2) - lx, out.get(out.size() - 1) - ly) > spacing * .25f) { out.add(lx); out.add(ly); }
        float[] r = new float[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }
}
