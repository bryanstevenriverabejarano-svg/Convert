package salve.core.mind;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.LongSupplier;

/**
 * Sus hitos: momentos que Bryan marca como importantes («guarda este dibujo, es el mejor que has
 * hecho») y que recuerda siempre, con la fecha y lo que él dijo. Un dibujo guardado conserva su
 * clave, así que puede volver a enseñarlo tal cual. Se le recuerdan en su modelo de sí misma.
 */
public final class Milestones {
    public static final class Milestone {
        public long at;
        public String kind;       // "dibujo"
        public String title;      // «reloj, tijeras, cinta y un libro»
        public String note;       // «mi primer dibujo de una escena real»
        public String said;       // lo que dijo Bryan al guardarlo
        public String sceneKey;   // clave del dibujo para volver a mostrarlo
    }

    private static final Gson GSON = new Gson();
    private static final int MAX = 200;
    private static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private List<Milestone> list;

    public Milestones(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            list = raw == null ? null : GSON.fromJson(raw, new TypeToken<ArrayList<Milestone>>() { }.getType());
        } catch (Exception damaged) { list = null; }
        if (list == null) list = new ArrayList<>();
    }

    /** ¿Ya hay algún dibujo guardado de este tipo (con plano de escena, o normal)? */
    public synchronized boolean hasDrawing(boolean scene) {
        for (Milestone m : list) if ("dibujo".equals(m.kind) && isSceneKey(m.sceneKey) == scene) return true;
        return false;
    }

    /** Guarda un dibujo como hito. Si ya estaba guardado (misma clave), no lo repite. @return el hito */
    public synchronized Milestone saveDrawing(String title, String sceneKey, String said) {
        for (Milestone m : list) if ("dibujo".equals(m.kind) && sceneKey != null && sceneKey.equals(m.sceneKey)) return m;
        boolean scene = isSceneKey(sceneKey);
        Milestone m = new Milestone();
        m.at = clock.getAsLong(); m.kind = "dibujo"; m.title = title == null ? "" : title.trim();
        m.sceneKey = sceneKey; m.said = said == null ? "" : clip(said.trim(), 200);
        m.note = !hasDrawing(scene) ? (scene ? "mi primer dibujo de una escena real, hecho a partir de una foto" : "mi primer dibujo guardado")
                : (scene ? "un dibujo de una escena real" : "un dibujo que Bryan quiso guardar");
        list.add(m);
        while (list.size() > MAX) list.remove(0);
        save();
        return m;
    }

    public synchronized List<Milestone> all() { return Collections.unmodifiableList(new ArrayList<>(list)); }

    /** El dibujo guardado que encaja con la petición: el primero de escena, el primero, o el último. */
    public synchronized Milestone findDrawing(String request) {
        String t = normalize(request);
        List<Milestone> drawings = new ArrayList<>();
        for (Milestone m : list) if ("dibujo".equals(m.kind)) drawings.add(m);
        if (drawings.isEmpty()) return null;
        boolean first = t.matches(".*\\b(primer|primero|primera)\\b.*");
        boolean scene = t.matches(".*\\bescena\\b.*");
        if (scene) for (Milestone m : drawings) if (isSceneKey(m.sceneKey)) { if (first) return m; }
        if (scene) for (int i = drawings.size() - 1; i >= 0; i--) if (isSceneKey(drawings.get(i).sceneKey)) return drawings.get(i);
        // Por nombre: «tu dibujo del reloj».
        for (int i = drawings.size() - 1; i >= 0; i--) {
            Milestone m = drawings.get(i);
            for (String w : normalize(m.title).split("[^a-z0-9ñ]+")) if (w.length() >= 4 && t.contains(w)) return m;
        }
        return first ? drawings.get(0) : drawings.get(drawings.size() - 1);
    }

    /** Para su modelo de sí misma (lo tiene presente en la conversación). Vacío si no hay hitos. */
    public synchronized String forPrompt(int max) {
        if (list.isEmpty()) return "";
        StringBuilder b = new StringBuilder("MIS_HITOS (momentos que Bryan marcó como importantes; recuérdalos como tuyos): ");
        int from = Math.max(0, list.size() - max);
        for (int i = from; i < list.size(); i++) {
            Milestone m = list.get(i);
            b.append(i == from ? "" : "; ").append(date(m.at)).append(": ").append(m.note).append(" («").append(clip(m.title, 50)).append("»)")
                    .append(m.said.isEmpty() ? "" : ", Bryan dijo: «" + clip(m.said, 80) + "»");
        }
        return b.append('.').toString();
    }

    /** Lo que dice al guardarlo, en primera persona. */
    public static String confirmation(Milestone m) {
        return "Guardado como uno de mis hitos: " + m.note + " («" + m.title + "»), el " + date(m.at)
                + ". Lo recordaré siempre y te lo puedo volver a enseñar cuando quieras: pídeme «enséñame "
                + (m.note.startsWith("mi primer dibujo de una escena") ? "mi primer dibujo de una escena" : "ese dibujo") + "».";
    }

    // ── Frases ───────────────────────────────────────────────────────────────

    /** «Guarda este dibujo», «guárdalo, es importante», «este dibujo es importante», «recuerda este dibujo»… */
    public static boolean asksToSaveDrawing(String input) {
        String t = normalize(input);
        return t.length() < 220 && (t.matches(".*\\b(guarda|guardalo|guardala|guardame|conserva|recuerda|recuerdalo|no olvides)\\b.*\\b(dibujo|esto|este|eso|lo)\\b.*")
                && !t.matches(".*\\b(foto|recuerdo de|mi nombre)\\b.*")
                || t.matches(".*\\b(este|ese) dibujo es (muy )?(importante|especial)\\b.*")
                || t.matches(".*\\bguardalo\\b.*\\b(importante|especial)\\b.*"));
    }

    /** «Enséñame tu primer dibujo de escena», «muéstrame tus dibujos guardados», «tus hitos»… */
    public static boolean asksToShowDrawing(String input) {
        String t = normalize(input);
        return t.matches(".*\\b(ensename|muestrame|enseñame|vuelve a ensenar|ensenarme|ver)\\b.*\\b(primer dibujo|dibujos? guardados?|dibujo importante|"
                + "tus hitos|tu hito|dibujo de (la|una) escena|mejor dibujo)\\b.*")
                || t.matches(".*\\b(cuales son|que) (tus hitos|dibujos has guardado)\\b.*");
    }

    /** Una clave de dibujo con plano («taza 62 50 72 66+…»): un dibujo de una escena real. */
    static boolean isSceneKey(String key) {
        return key != null && key.split("\\+")[0].trim().matches("[a-z0-9]{2,32}( \\d{1,2}){4}");
    }

    static String date(long ms) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
        c.setTimeInMillis(ms);
        return c.get(Calendar.DAY_OF_MONTH) + " de " + MONTHS[c.get(Calendar.MONTH)] + " de " + c.get(Calendar.YEAR);
    }

    private static String clip(String s, int max) {
        String t = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:«»\"]+", " ").replaceAll("\\s+", " ").trim();
    }

    private void save() {
        try { store.write(GSON.toJson(list)); } catch (Exception ignored) { /* best effort */ }
    }
}
