package salve.core.mind;

import com.google.gson.Gson;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Lo que aprende de sus errores, sin reentrenar el modelo: cada vez que Bryan la corrige («no te
 * pedí eso», «otra vez lo mismo», «no me estás mostrando el dibujo») o ella misma se pilla un fallo
 * antes de hablar, guarda una lección (qué hizo → qué se esperaba). Las lecciones parecidas se
 * refuerzan en vez de duplicarse, y las relevantes entran en su contexto en cada turno.
 *
 * Datos, no órdenes de terceros: sólo las escriben las correcciones de Bryan y su autocrítica.
 */
public final class LessonBook {
    public static final class Lesson {
        public long at, lastAt;
        public int times = 1;
        /** «bryan» si la corrigió él; «yo» si se pilló ella antes de hablar. */
        public String source;
        public String situation, did, expected;
    }

    static final class State { int schema = 1; List<Lesson> lessons = new ArrayList<>(); }

    public static final int MAX = 40;
    private static final Gson GSON = new Gson();
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private State state;

    public LessonBook(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            state = raw == null ? new State() : GSON.fromJson(raw, State.class);
        } catch (Exception damaged) { state = null; }
        if (state == null || state.lessons == null) state = new State();
        state.lessons.removeIf(l -> l == null || l.expected == null);
    }

    private static final Pattern CORRECTION = Pattern.compile(
            "^(no[ ,.]|no$)|\\b(no te pedi|no es eso|eso no|te equivocas|no me (estas|has)|otra vez lo mismo|ya te (lo )?dije|"
            + "no me hagas|no hagas|deja de|no entiendes|no has entendido|no era eso|te he dicho|no quiero que|"
            + "por que no me|no lo hiciste|no lo has hecho|sigues sin|me estas repitiendo|no me repitas|joder)\\b");

    /** ¿El mensaje corrige lo que ella acaba de hacer? */
    public static boolean isCorrection(String input) {
        String t = normalize(input);
        return !t.isEmpty() && t.length() <= 400 && CORRECTION.matcher(t).find();
    }

    /** Guarda (o refuerza) una lección. @return la lección guardada */
    public synchronized Lesson learn(String source, String situation, String did, String expected) {
        String e = bounded(expected, 200);
        if (e.isEmpty()) return null;
        long now = clock.getAsLong();
        for (Lesson l : state.lessons) {
            if (similar(l.expected, e) && (l.situation == null || situation == null || similar(l.situation, situation) || similar(l.expected, e))) {
                l.times++; l.lastAt = now;
                if (did != null && !did.isEmpty()) l.did = bounded(did, 160);
                save();
                return l;
            }
        }
        Lesson l = new Lesson();
        l.at = l.lastAt = now; l.source = source; l.situation = bounded(situation, 140);
        l.did = bounded(did, 160); l.expected = e;
        state.lessons.add(l);
        if (state.lessons.size() > MAX) {
            // Olvida la menos reforzada y más antigua.
            state.lessons.sort((a, b) -> a.times != b.times ? Integer.compare(a.times, b.times) : Long.compare(a.lastAt, b.lastAt));
            state.lessons.remove(0);
        }
        save();
        return l;
    }

    /** Las lecciones que más aplican a este mensaje (por palabras en común y refuerzo). */
    public synchronized List<Lesson> relevant(String input, int max) {
        Set<String> said = words(input);
        List<Lesson> scored = new ArrayList<>(state.lessons);
        scored.sort((a, b) -> Double.compare(score(b, said), score(a, said)));
        List<Lesson> out = new ArrayList<>();
        for (Lesson l : scored) if (out.size() < max && score(l, said) > 0.9) out.add(l);
        return out;
    }

    public synchronized int size() { return state.lessons.size(); }

    /** Bloque para su contexto; vacío si no hay ninguna que aplicar. */
    public String render(String input, int max) {
        List<Lesson> list = relevant(input, max);
        if (list.isEmpty()) return "";
        StringBuilder b = new StringBuilder(" | LECCIONES_APRENDIDAS (de mis errores; aplícalas): ");
        for (Lesson l : list) {
            b.append("• ").append(l.expected);
            if (l.did != null && !l.did.isEmpty()) b.append(" (antes hice: «").append(l.did).append("»)");
            if (l.times > 1) b.append(" ×").append(l.times);
            b.append(' ');
        }
        return b.toString().trim();
    }

    private static double score(Lesson l, Set<String> said) {
        Set<String> lw = words((l.situation == null ? "" : l.situation) + " " + l.expected);
        int common = 0;
        for (String w : said) if (lw.contains(w)) common++;
        return common + Math.min(3, l.times) * 0.5 + ("bryan".equals(l.source) ? 0.5 : 0);
    }

    static boolean similar(String a, String b) {
        Set<String> wa = words(a), wb = words(b);
        if (wa.isEmpty() || wb.isEmpty()) return false;
        int common = 0;
        for (String w : wa) if (wb.contains(w)) common++;
        return common * 2 >= Math.min(wa.size(), wb.size());
    }

    private static final Set<String> STOP = new HashSet<>(java.util.Arrays.asList(
            "que", "con", "los", "las", "una", "uno", "para", "por", "como", "pero", "esto", "eso", "esta", "este", "salve",
            "bryan", "cuando", "antes", "hice", "porque", "sobre", "desde", "muy", "mas"));

    static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        for (String w : normalize(text).split(" ")) if (w.length() >= 4 && !STOP.contains(w)) out.add(w);
        return out;
    }

    private void save() {
        try { store.write(GSON.toJson(state)); } catch (Exception ignored) { /* aprender nunca bloquea el turno */ }
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").trim();
    }

    private static String bounded(String s, int max) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]+", " ").replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    /** Lecciones como texto (para la reflexión y el descubrimiento propio). */
    public synchronized List<Lesson> all() { return Collections.unmodifiableList(new ArrayList<>(state.lessons)); }
}
