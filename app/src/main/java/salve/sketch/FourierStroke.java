package salve.sketch;

import java.util.Arrays;

/**
 * Un trazo convertido en epiciclos: la transformada discreta de Fourier de la línea, ordenada de
 * mayor a menor radio. Cada término es un círculo que gira a su frecuencia; encadenados, la punta
 * del último recorre exactamente la línea original (con todos los términos).
 *
 * Una línea abierta se recorre de ida y vuelta para formar un lazo cerrado: así no aparece el salto
 * del final al principio (fenómeno de Gibbs) y sólo se dibuja la mitad de ida.
 */
public final class FourierStroke {
    private final int[] frequency;
    private final float[] radius, phase;
    private final int samples;
    /** Fracción del ciclo que corresponde al trazo visible (1 cerrado, ~0,5 abierto). */
    public final float visibleTurn;
    public final boolean closed;
    /** Longitud aproximada en unidades del lienzo (para repartir el tiempo de dibujo). */
    public final float length;
    private final float[] source;

    public FourierStroke(float[] points) {
        if (points == null || points.length < 4 || points.length % 2 != 0)
            throw new IllegalArgumentException("Un trazo necesita al menos dos puntos");
        int n = points.length / 2;
        for (float v : points) if (!Float.isFinite(v)) throw new IllegalArgumentException("Coordenada inválida");
        source = points.clone();
        float len = 0;
        for (int i = 1; i < n; i++) len += (float) Math.hypot(points[2 * i] - points[2 * i - 2], points[2 * i + 1] - points[2 * i - 1]);
        length = len;
        float gap = (float) Math.hypot(points[0] - points[2 * n - 2], points[1] - points[2 * n - 1]);
        closed = n > 2 && gap <= Math.max(1f, len * .02f);
        float[] loop;
        if (closed) {
            int m = n - 1; // el último repite el primero
            loop = Arrays.copyOf(points, 2 * m);
            visibleTurn = 1f;
        } else {
            int m = 2 * n - 2;
            loop = new float[2 * m];
            System.arraycopy(points, 0, loop, 0, 2 * n);
            for (int i = 1; i < n - 1; i++) {
                loop[2 * (n - 1 + i)] = points[2 * (n - 1 - i)];
                loop[2 * (n - 1 + i) + 1] = points[2 * (n - 1 - i) + 1];
            }
            visibleTurn = (n - 1f) / m;
        }
        samples = loop.length / 2;
        int m = samples;
        int[] freq = new int[m];
        float[] re = new float[m], im = new float[m];
        for (int k = 0; k < m; k++) {
            int f = k <= m / 2 ? k : k - m; // frecuencias centradas: -m/2..m/2
            double sr = 0, si = 0;
            for (int t = 0; t < m; t++) {
                double angle = -2 * Math.PI * f * t / m;
                double c = Math.cos(angle), s = Math.sin(angle);
                double x = loop[2 * t], y = loop[2 * t + 1];
                sr += x * c - y * s;
                si += x * s + y * c;
            }
            freq[k] = f; re[k] = (float) (sr / m); im[k] = (float) (si / m);
        }
        Integer[] order = new Integer[m];
        for (int i = 0; i < m; i++) order[i] = i;
        final float[] amp = new float[m];
        for (int i = 0; i < m; i++) amp[i] = (float) Math.hypot(re[i], im[i]);
        // El término de frecuencia 0 (centro) va primero: es el ancla fija de la cadena.
        Arrays.sort(order, (a, b) -> freq[a] == 0 ? -1 : freq[b] == 0 ? 1 : Float.compare(amp[b], amp[a]));
        frequency = new int[m]; radius = new float[m]; phase = new float[m];
        for (int i = 0; i < m; i++) {
            int k = order[i];
            frequency[i] = freq[k]; radius[i] = amp[k]; phase[i] = (float) Math.atan2(im[k], re[k]);
        }
    }

    public int terms() { return frequency.length; }
    public float radius(int i) { return radius[i]; }
    public int frequency(int i) { return frequency[i]; }

    /**
     * Posición de la punta tras {@code turn} vueltas (0..1 del ciclo completo), usando los
     * {@code terms} términos más grandes.
     */
    public void point(float turn, int terms, float[] out) {
        int count = Math.max(1, Math.min(terms, frequency.length));
        double x = 0, y = 0;
        for (int i = 0; i < count; i++) {
            double a = 2 * Math.PI * frequency[i] * turn + phase[i];
            x += radius[i] * Math.cos(a);
            y += radius[i] * Math.sin(a);
        }
        out[0] = (float) x; out[1] = (float) y;
    }

    /**
     * Centros de la cadena de epiciclos: {@code out[2i], out[2i+1]} es el centro del círculo i.
     * Devuelve cuántos centros se escribieron (incluye la punta final).
     */
    public int chain(float turn, int terms, float[] out) {
        int count = Math.max(1, Math.min(Math.min(terms, frequency.length), out.length / 2 - 1));
        double x = 0, y = 0;
        for (int i = 0; i < count; i++) {
            out[2 * i] = (float) x; out[2 * i + 1] = (float) y;
            double a = 2 * Math.PI * frequency[i] * turn + phase[i];
            x += radius[i] * Math.cos(a);
            y += radius[i] * Math.sin(a);
        }
        out[2 * count] = (float) x; out[2 * count + 1] = (float) y;
        return count + 1;
    }

    /** Número de muestras del lazo (resolución del trazo visible). */
    public int samples() { return samples; }

    /** Copia de los puntos originales (para dibujar el trazo terminado sin recalcular). */
    public float[] points() { return source.clone(); }
}
