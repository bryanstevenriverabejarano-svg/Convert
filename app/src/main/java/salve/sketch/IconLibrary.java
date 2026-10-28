package salve.sketch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Iconos de líneas que lleva dentro (assets/sketch/icons.json, de Tabler Icons, licencia MIT; los
 * genera scripts/build_icon_library.py). Son dibujos de líneas limpios de cosas comunes: una iglesia
 * con su puerta y su cruz, un gato, un coche… Le sirven de referencia sin internet y sin el ruido de
 * calcar una foto. Se buscan por el nombre en inglés que da su concepto («INGLES: church»).
 */
public final class IconLibrary {
    public static final class Icon {
        public final String name, category;
        public final List<String> tags;
        public final float aspect;
        private final float[][] strokes;
        Icon(String name, String category, List<String> tags, float aspect, float[][] strokes) {
            this.name = name; this.category = category; this.tags = tags; this.aspect = aspect; this.strokes = strokes;
        }
        /** Copia de sus trazos en el lienzo 0..1000 (centrados, con su proporción). */
        public List<float[]> strokes() {
            List<float[]> out = new ArrayList<>();
            for (float[] s : strokes) out.add(s.clone());
            return out;
        }
    }

    private final List<Icon> icons;

    private IconLibrary(List<Icon> icons) { this.icons = Collections.unmodifiableList(icons); }

    public int size() { return icons.size(); }

    public static IconLibrary parse(String json) {
        JsonArray raw = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("icons");
        List<Icon> list = new ArrayList<>();
        if (raw != null) for (JsonElement e : raw) {
            try {
                JsonObject o = e.getAsJsonObject();
                List<String> tags = new ArrayList<>();
                if (o.has("t")) for (JsonElement t : o.getAsJsonArray("t")) tags.add(t.getAsString().toLowerCase(Locale.ROOT));
                JsonArray s = o.getAsJsonArray("s");
                float[][] strokes = new float[s.size()][];
                for (int i = 0; i < s.size(); i++) {
                    JsonArray line = s.get(i).getAsJsonArray();
                    float[] pts = new float[line.size()];
                    for (int k = 0; k < pts.length; k++) pts[k] = line.get(k).getAsFloat();
                    strokes[i] = pts;
                }
                if (strokes.length > 0) list.add(new Icon(o.get("n").getAsString(), o.has("c") ? o.get("c").getAsString() : "",
                        Collections.unmodifiableList(tags), o.has("a") ? o.get("a").getAsFloat() : 1f, strokes));
            } catch (RuntimeException skip) { /* un icono mal formado no estropea los demás */ }
        }
        return new IconLibrary(list);
    }

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "a", "an", "the", "of", "with", "and", "for", "in", "on", "to", "big", "small", "little", "old", "new"));

    /** Palabras de búsqueda en inglés, en singular simple («churches» → «church»). */
    static List<String> words(String english) {
        List<String> out = new ArrayList<>();
        for (String w : (english == null ? "" : english.toLowerCase(Locale.ROOT)).split("[^a-z]+")) {
            if (w.length() < 2 || STOP.contains(w)) continue;
            if (w.endsWith("ies") && w.length() > 4) w = w.substring(0, w.length() - 3) + "y";
            else if (w.endsWith("ches") || w.endsWith("shes") || w.endsWith("xes")) w = w.substring(0, w.length() - 2);
            else if (w.endsWith("s") && !w.endsWith("ss") && w.length() > 3) w = w.substring(0, w.length() - 1);
            out.add(w);
        }
        return out;
    }

    /**
     * El icono que mejor representa lo pedido, o null si ninguno lo es de verdad. Manda el nombre del
     * icono («building-church» para «church»); las etiquetas ayudan, pero solas no bastan salvo que
     * coincida la frase entera (así «gato» no acaba siendo un icono cualquiera etiquetado «pet»).
     */
    public Icon find(String english) {
        List<String> q = words(english);
        if (q.isEmpty()) return null;
        String joined = String.join("-", q), phrase = String.join(" ", q);
        Icon best = null;
        float bestScore = 0;
        for (Icon icon : icons) {
            List<String> segments = Arrays.asList(icon.name.split("-"));
            float score = 0;
            int inName = 0;
            for (String w : q) {
                if (segments.contains(w)) { score += 3; inName++; }
                if (icon.tags.contains(w)) score += 1.5f;
            }
            if (icon.name.equals(joined)) score += 10;
            else if (icon.name.endsWith("-" + joined) && segments.size() == q.size() + 1) score += 6; // building-church
            if (inName == q.size()) score += 3;
            boolean phraseTag = icon.tags.contains(phrase);
            if (inName == 0 && !phraseTag) continue;
            if (phraseTag) score += 2;
            if (icon.name.matches(".*\\d.*")) score -= .5f;           // «-2», «-3»: variantes
            score -= .25f * Math.max(0, segments.size() - q.size());  // nombres más específicos, menos
            if (score > bestScore) { bestScore = score; best = icon; }
        }
        return bestScore >= 4 ? best : null;
    }
}
