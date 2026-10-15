package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lo que Salve va a dibujar: uno o varios motivos colocados en un lienzo (cielo arriba, suelo abajo).
 * La clave ("casa+luna") viaja dentro del texto como [[salve_draw:casa+luna]] y reconstruye la escena.
 */
public final class SketchScene {
    public static final int MAX_MOTIFS = 4;
    public final String key, title;
    public final float width, height;
    public final List<float[]> strokes;
    public final List<SketchLibrary.Motif> motifs;

    private SketchScene(String key, String title, float width, float height, List<float[]> strokes,
                        List<SketchLibrary.Motif> motifs) {
        this.key = key; this.title = title; this.width = width; this.height = height;
        this.strokes = Collections.unmodifiableList(strokes);
        this.motifs = Collections.unmodifiableList(motifs);
    }

    public boolean isSelfPortrait() { return motifs.size() == 1 && motifs.get(0).self; }

    /** null si la clave no tiene ningún motivo conocido. */
    public static SketchScene fromKey(SketchLibrary library, String key) {
        if (library == null || key == null || key.length() > 120) return null;
        List<SketchLibrary.Motif> motifs = new ArrayList<>();
        for (String part : key.split("\\+")) {
            SketchLibrary.Motif m = library.byId(part.trim());
            if (m == null) m = library.find(part);
            if (m != null) { if (!motifs.contains(m)) motifs.add(m); }
            else for (SketchLibrary.Mention mention : library.mentions(part, false))
                if (!motifs.contains(mention.motif)) motifs.add(mention.motif);
        }
        if (motifs.size() > MAX_MOTIFS) motifs = new ArrayList<>(motifs.subList(0, MAX_MOTIFS));
        return compose(motifs);
    }

    public static SketchScene compose(List<SketchLibrary.Motif> chosen) {
        if (chosen == null || chosen.isEmpty()) return null;
        List<SketchLibrary.Motif> motifs = new ArrayList<>(chosen.subList(0, Math.min(MAX_MOTIFS, chosen.size())));
        StringBuilder key = new StringBuilder(), title = new StringBuilder();
        for (int i = 0; i < motifs.size(); i++) {
            if (i > 0) { key.append('+'); title.append(i == motifs.size() - 1 ? " y " : ", "); }
            key.append(motifs.get(i).id); title.append(motifs.get(i).name);
        }
        List<float[]> strokes = new ArrayList<>();
        if (motifs.size() == 1) {
            SketchLibrary.Motif m = motifs.get(0);
            float aspect = Math.max(.3f, Math.min(3f, m.aspect));
            // El motivo se normalizó en un cuadrado de 1000 centrado; se recorta al alto/ancho reales.
            float w = aspect >= 1 ? 1000 : 1000 * aspect, h = aspect >= 1 ? 1000 / aspect : 1000;
            float ox = (1000 - w) / 2, oy = (1000 - h) / 2;
            for (float[] s : m.strokes) strokes.add(place(s, -ox, -oy, 1, 0, 0));
            return new SketchScene(key.toString(), title.toString(), w, h, strokes, motifs);
        }
        float width = 1000, height = 720;
        List<SketchLibrary.Motif> sky = new ArrayList<>(), ground = new ArrayList<>();
        for (SketchLibrary.Motif m : motifs) {
            if (m.place == SketchLibrary.Place.SKY) sky.add(m);
            else if (m.place == SketchLibrary.Place.GROUND) ground.add(m);
        }
        for (SketchLibrary.Motif m : motifs)
            if (m.place == SketchLibrary.Place.ANY) (ground.size() <= sky.size() ? ground : sky).add(m);
        if (sky.isEmpty() || ground.isEmpty()) {
            List<SketchLibrary.Motif> row = sky.isEmpty() ? ground : sky;
            layoutRow(row, strokes, 30, 690, width, true);
        } else {
            layoutRow(sky, strokes, 20, 270, width, false);
            layoutRow(ground, strokes, 250, 700, width, true);
        }
        return new SketchScene(key.toString(), title.toString(), width, height, strokes, motifs);
    }

    private static void layoutRow(List<SketchLibrary.Motif> row, List<float[]> out, float top, float bottom,
                                  float width, boolean groundAligned) {
        int n = row.size();
        float cell = width / n, pad = 18;
        for (int i = 0; i < n; i++) {
            SketchLibrary.Motif m = row.get(i);
            float aspect = Math.max(.3f, Math.min(3f, m.aspect));
            float boxW = cell - 2 * pad, boxH = bottom - top;
            float w = Math.min(boxW, boxH * aspect), h = w / aspect;
            float scale = Math.max(w, h) / 1000f;
            // Esquina del cuadrado normalizado (motivo centrado en 0..1000).
            float cx = i * cell + cell / 2, cy = groundAligned ? bottom - h / 2 : top + (bottom - top) / 2;
            float x0 = cx - 500 * scale, y0 = cy - 500 * scale;
            for (float[] s : m.strokes) out.add(place(s, 0, 0, scale, x0, y0));
        }
    }

    private static float[] place(float[] s, float dx, float dy, float scale, float ox, float oy) {
        float[] out = new float[s.length];
        for (int i = 0; i < s.length; i += 2) {
            out[i] = (s[i] + dx) * scale + ox;
            out[i + 1] = (s[i + 1] + dy) * scale + oy;
        }
        return out;
    }
}
