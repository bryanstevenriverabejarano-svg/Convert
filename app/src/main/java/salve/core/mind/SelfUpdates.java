package salve.core.mind;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Sus propias actualizaciones: qué le cambiaron y cuándo (assets/self/updates.json, una entrada por
 * mejora, en primera persona). Con esto puede contestar «¿qué opinas de tus últimas actualizaciones?»
 * con hechos, y notar cuándo le instalaron una versión nueva.
 */
public final class SelfUpdates {
    public static final class Update {
        public final String id, date, title, what;
        Update(String id, String date, String title, String what) { this.id = id; this.date = date; this.title = title; this.what = what; }
    }

    private SelfUpdates() { }

    public static List<Update> parse(String json) {
        List<Update> out = new ArrayList<>();
        try {
            for (JsonElement e : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("updates")) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Update(o.get("id").getAsString(), o.get("date").getAsString(), o.get("title").getAsString(),
                        o.get("what").getAsString()));
            }
        } catch (RuntimeException damaged) { return Collections.emptyList(); }
        return out;
    }

    private static final Pattern ASKS = Pattern.compile(
            "\\b(actualizacion|actualizaciones|actualizada|actualizado|novedades|que hay de nuevo|que tienes de nuevo|"
            + "que te (han )?(cambiado|mejorado|anadido|puesto)|ultimos cambios|tus cambios|tus mejoras|que mejoras|"
            + "version nueva|nueva version|parche|parches|te han mejorado|te he mejorado|que te he hecho)\\b");

    /** ¿Pregunta por sus actualizaciones o cambios? */
    public static boolean asksAbout(String input) {
        return input != null && ASKS.matcher(normalize(input)).find();
    }

    /** Las más recientes primero, hasta {@code maxChars}. */
    public static String render(List<Update> updates, int max, int maxChars) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (int i = updates.size() - 1; i >= 0 && n < max; i--, n++) {
            Update u = updates.get(i);
            String line = "\n- " + u.date + " · " + u.title + ": " + u.what;
            if (b.length() + line.length() > maxChars) break;
            b.append(line);
        }
        return b.toString();
    }

    /** Las que llegaron después de la última que ya conocía ({@code lastSeenId}); todas si no conocía ninguna. */
    public static List<Update> since(List<Update> updates, String lastSeenId) {
        if (lastSeenId == null || lastSeenId.isEmpty()) return new ArrayList<>(updates);
        for (int i = 0; i < updates.size(); i++)
            if (updates.get(i).id.equals(lastSeenId)) return new ArrayList<>(updates.subList(i + 1, updates.size()));
        return new ArrayList<>(updates);
    }

    static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
