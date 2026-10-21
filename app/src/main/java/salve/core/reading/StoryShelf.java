package salve.core.reading;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;
import salve.core.mind.SelfModelJournal;

/**
 * Su estantería de cuentos: los que ha leído (con lo que entendió de cada uno) y dónde guardó el
 * texto entero, para volver a leerlo cuando Bryan se lo pida.
 */
public final class StoryShelf {
    public static final class Story {
        public long at;
        public String title, summary, moral, file;
        public List<String> characters = new ArrayList<>(), places = new ArrayList<>(), scenes = new ArrayList<>();
    }

    private static final Gson GSON = new Gson();
    private static final int MAX = 300;
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private List<Story> list;

    public StoryShelf(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            list = raw == null ? null : GSON.fromJson(raw, new TypeToken<ArrayList<Story>>() { }.getType());
        } catch (Exception damaged) { list = null; }
        if (list == null) list = new ArrayList<>();
    }

    /** Guarda (o actualiza, si ya tenía uno con el mismo título) un cuento leído. */
    public synchronized Story add(DocumentReading.Analysis a, String file) {
        Story s = null;
        for (Story x : list) if (DocumentReading.normalize(x.title).equals(DocumentReading.normalize(a.title))) { s = x; break; }
        if (s == null) { s = new Story(); list.add(s); }
        s.at = clock.getAsLong(); s.title = a.title; s.summary = a.summary; s.moral = a.moral;
        s.file = file == null ? s.file : file;
        s.characters = new ArrayList<>(a.characters); s.places = new ArrayList<>(a.places); s.scenes = new ArrayList<>(a.scenes);
        while (list.size() > MAX) list.remove(0);
        save();
        return s;
    }

    public synchronized List<Story> all() { return Collections.unmodifiableList(new ArrayList<>(list)); }

    /** El cuento cuyo título (o personajes) encaja mejor con lo pedido; null si ninguno. */
    public synchronized Story find(String query) {
        String q = DocumentReading.normalize(query);
        Story best = null; int bestScore = 0;
        for (Story s : list) {
            String hay = DocumentReading.normalize(s.title + " " + String.join(" ", s.characters));
            int score = 0;
            for (String w : q.split("\\s+")) if (w.length() >= 3 && hay.contains(w)) score += w.length();
            if (score > bestScore) { bestScore = score; best = s; }
        }
        return best;
    }

    /** «Tengo 3 cuentos: «El dragón y la luna» (Lía, Fuego); …» */
    public synchronized String describe() {
        if (list.isEmpty()) return "Todavía no he leído ningún cuento. Dame uno en PDF desde «Documentos» y elige «Es un cuento».";
        StringBuilder b = new StringBuilder("Tengo ").append(list.size()).append(list.size() == 1 ? " cuento: " : " cuentos: ");
        for (int i = list.size() - 1, n = 0; i >= 0 && n < 12; i--, n++) {
            Story s = list.get(i);
            b.append(n == 0 ? "" : "; ").append('«').append(s.title).append('»');
            if (!s.characters.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (String c : s.characters) { names.add(c.split("[:,(—-]", 2)[0].trim()); if (names.size() == 3) break; }
                b.append(" (").append(String.join(", ", names)).append(')');
            }
        }
        return b.append(". Pídeme «léeme el cuento de …» y te lo vuelvo a leer.").toString();
    }

    private void save() {
        try { store.write(GSON.toJson(list)); } catch (Exception ignored) { /* best effort */ }
    }
}
