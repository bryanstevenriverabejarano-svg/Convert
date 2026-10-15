package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Línea de tiempo de un dibujo por epiciclos: cada trazo recibe tiempo según su longitud y la
 * pluma salta (levantada) al siguiente. Puro y determinista: el renderer sólo pregunta "¿qué hay en t?".
 */
public final class SketchAnimation {
    public final SketchScene scene;
    public final List<FourierStroke> strokes;
    private final float[] start, span;
    private final float duration;

    public SketchAnimation(SketchScene scene) {
        if (scene == null) throw new IllegalArgumentException("Escena vacía");
        this.scene = scene;
        List<FourierStroke> list = new ArrayList<>();
        float total = 0;
        for (float[] s : scene.strokes) {
            FourierStroke f = new FourierStroke(s);
            if (f.length < 1f) continue;
            list.add(f);
            total += f.length;
        }
        if (list.isEmpty()) throw new IllegalArgumentException("Escena sin trazos");
        strokes = Collections.unmodifiableList(list);
        float base = scene.isSelfPortrait() ? 9.5f : clamp(1.4f + total / 1000f * .75f, 2.8f, 7.5f);
        float minimum = .22f;
        float budget = base - minimum * list.size();
        if (budget < base * .3f) { minimum = base * .7f / list.size(); budget = base * .3f; }
        start = new float[list.size()]; span = new float[list.size()];
        float t = 0;
        for (int i = 0; i < list.size(); i++) {
            start[i] = t;
            span[i] = minimum + budget * list.get(i).length / total;
            t += span[i];
        }
        duration = t;
    }

    public float duration() { return duration; }

    /** Índice del trazo que se dibuja en {@code seconds}; strokes.size() si ya terminó. */
    public int strokeAt(float seconds) {
        if (!(seconds >= 0)) return 0;
        for (int i = 0; i < start.length; i++) if (seconds < start[i] + span[i]) return i;
        return start.length;
    }

    /** Progreso 0..1 del trazo i en {@code seconds}. */
    public float progress(int i, float seconds) {
        if (i < 0 || i >= span.length) return 1f;
        float p = (seconds - start[i]) / span[i];
        // Suaviza la entrada y salida de la pluma.
        p = clamp(p, 0, 1);
        return p * p * (3 - 2 * p);
    }

    public boolean finished(float seconds) { return seconds >= duration; }

    private static float clamp(float v, float lo, float hi) { return Math.max(lo, Math.min(hi, v)); }
}
