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
    /** Partes en orden de trazado (siempre al menos una que cubre todo). */
    public final List<SketchLibrary.Part> parts;

    private SketchScene(String key, String title, float width, float height, List<float[]> strokes,
                        List<SketchLibrary.Motif> motifs, List<SketchLibrary.Part> parts) {
        this.key = key; this.title = title; this.width = width; this.height = height;
        this.parts = parts == null || parts.isEmpty()
                ? Collections.singletonList(new SketchLibrary.Part(title, "", 0, strokes.size()))
                : Collections.unmodifiableList(parts);
        this.strokes = Collections.unmodifiableList(strokes);
        this.motifs = Collections.unmodifiableList(motifs);
        int hash = 17;
        for (float[] s : strokes) hash = 31 * hash + java.util.Arrays.hashCode(s);
        this.cacheKey = key + "#" + Integer.toHexString(hash);
    }

    /** Clave + huella de las líneas: si vuelve a aprender «mueble», no reutiliza el trazado viejo. */
    public final String cacheKey;

    public boolean isSelfPortrait() { return motifs.size() == 1 && motifs.get(0).self; }

    /** Un motivo colocado en su caja de una escena real: «taza 62 50 72 66» (id y caja en %, 0..99). */
    private static final java.util.regex.Pattern PLACED =
            java.util.regex.Pattern.compile("^([a-z0-9]{2,32}) (\\d{1,2}) (\\d{1,2}) (\\d{1,2}) (\\d{1,2})$");
    /** Lienzo de las escenas colocadas (proporción 4:3, como una foto). */
    public static final float LAYOUT_W = 1000, LAYOUT_H = 750;

    /** null si la clave no tiene ningún motivo conocido. */
    public static SketchScene fromKey(SketchLibrary library, String key) {
        if (library == null || key == null || key.length() > 120) return null;
        if (PLACED.matcher(key.split("\\+")[0].trim()).matches()) {
            // Una escena con plano: cada motivo en su caja (viene de una foto real).
            List<SketchLibrary.Motif> motifs = new ArrayList<>();
            List<int[]> boxes = new ArrayList<>();
            for (String part : key.split("\\+")) {
                java.util.regex.Matcher m = PLACED.matcher(part.trim());
                if (!m.matches()) continue;
                SketchLibrary.Motif motif = library.byId(m.group(1));
                if (motif == null) continue;
                motifs.add(motif);
                boxes.add(new int[]{Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5))});
            }
            return layout(motifs, boxes, null);
        }
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
            return new SketchScene(key.toString(), title.toString(), w, h, strokes, motifs, m.parts);
        }
        float width = 1000, height = 720;
        List<SketchLibrary.Motif> sky = new ArrayList<>(), ground = new ArrayList<>();
        for (SketchLibrary.Motif m : motifs) {
            if (m.place == SketchLibrary.Place.SKY) sky.add(m);
            else if (m.place == SketchLibrary.Place.GROUND) ground.add(m);
        }
        for (SketchLibrary.Motif m : motifs)
            if (m.place == SketchLibrary.Place.ANY) (ground.size() <= sky.size() ? ground : sky).add(m);
        List<SketchLibrary.Motif> drawOrder = new ArrayList<>();
        if (sky.isEmpty() || ground.isEmpty()) {
            List<SketchLibrary.Motif> row = sky.isEmpty() ? ground : sky;
            drawOrder.addAll(row);
            layoutRow(row, strokes, 30, 690, width, true);
        } else {
            drawOrder.addAll(sky);
            drawOrder.addAll(ground);
            layoutRow(sky, strokes, 15, 255, width, false);
            layoutRow(ground, strokes, 290, 705, width, true);
        }
        // Cada motivo es una parte de la escena, en el orden en que se traza.
        List<SketchLibrary.Part> parts = new ArrayList<>();
        int at = 0;
        for (SketchLibrary.Motif m : drawOrder) {
            parts.add(new SketchLibrary.Part(m.name, "", at, at + m.strokes.length));
            at += m.strokes.length;
        }
        return new SketchScene(key.toString(), title.toString(), width, height, strokes, motifs, parts);
    }

    /**
     * Cada motivo dentro de su caja (en % del lienzo, 0..99), con su proporción, centrado y apoyado en
     * la base de la caja; primero lo de detrás (lo que acaba más arriba en la imagen). Es el plano que
     * sale de una foto: las referencias dicen cómo es cada cosa y la escena dónde va.
     * @param title null para componerlo con los nombres
     */
    public static SketchScene layout(List<SketchLibrary.Motif> chosen, List<int[]> boxes, String title) {
        if (chosen == null || boxes == null || chosen.isEmpty() || chosen.size() != boxes.size()) return null;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_MOTIFS, chosen.size()); i++) order.add(i);
        order.sort((a, b) -> Integer.compare(boxes.get(a)[3], boxes.get(b)[3]));
        List<float[]> strokes = new ArrayList<>();
        List<SketchLibrary.Part> parts = new ArrayList<>();
        List<SketchLibrary.Motif> motifs = new ArrayList<>();
        StringBuilder key = new StringBuilder(), names = new StringBuilder();
        for (int idx = 0; idx < order.size(); idx++) {
            SketchLibrary.Motif m = chosen.get(order.get(idx));
            int[] box = clampBox(boxes.get(order.get(idx)));
            float bx = box[0] * LAYOUT_W / 100f, by = box[1] * LAYOUT_H / 100f;
            float bw = Math.max(20, (box[2] - box[0]) * LAYOUT_W / 100f), bh = Math.max(20, (box[3] - box[1]) * LAYOUT_H / 100f);
            float aspect = Math.max(.3f, Math.min(3f, m.aspect));
            // El motivo ocupa (w × h) dentro de su cuadrado normalizado de 1000 centrado.
            float w = aspect >= 1 ? 1000 : 1000 * aspect, h = aspect >= 1 ? 1000 / aspect : 1000;
            float scale = Math.min(bw / w, bh / h) * 0.95f;
            float cx = bx + bw / 2, cy = by + bh - h * scale / 2;
            int at = strokes.size();
            for (float[] st : m.strokes) strokes.add(place(st, 0, 0, scale, cx - 500 * scale, cy - 500 * scale));
            parts.add(new SketchLibrary.Part(m.name, "", at, strokes.size()));
            motifs.add(m);
            if (idx > 0) key.append('+');
            key.append(m.id).append(' ').append(box[0]).append(' ').append(box[1]).append(' ').append(box[2]).append(' ').append(box[3]);
            names.append(idx == 0 ? "" : idx == order.size() - 1 ? " y " : ", ").append(m.name);
        }
        if (strokes.isEmpty()) return null;
        return new SketchScene(key.toString(), title == null ? names.toString() : title, LAYOUT_W, LAYOUT_H, strokes, motifs, parts);
    }

    private static int[] clampBox(int[] b) {
        int x1 = Math.max(0, Math.min(99, Math.min(b[0], b[2]))), x2 = Math.max(0, Math.min(99, Math.max(b[0], b[2])));
        int y1 = Math.max(0, Math.min(99, Math.min(b[1], b[3]))), y2 = Math.max(0, Math.min(99, Math.max(b[1], b[3])));
        return new int[]{x1, y1, Math.max(x2, x1 + 1), Math.max(y2, y1 + 1)};
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
