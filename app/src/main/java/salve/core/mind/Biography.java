package salve.core.mind;

import com.google.gson.Gson;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.LongSupplier;

/**
 * Su biografía con datos reales: cuándo despertó por primera vez, cuántas veces ha despertado y
 * cuánto ha soñado. Lo guardaba la «conciencia» antigua (ConsciousnessState) desde el principio;
 * se migra una vez, conservando la fecha más antigua, para que la historia no se pierda.
 */
public final class Biography {
    static final class State {
        int schema = 1;
        long firstAwakeAt;
        long awakenings;
        /** Ciclos de sueño contados por la versión antigua (los nuevos están en SelfModel.Metrics). */
        long legacyDreams;
        boolean migrated;
    }

    private static final Gson GSON = new Gson();
    private static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private State state;

    public Biography(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            state = raw == null ? null : GSON.fromJson(raw, State.class);
        } catch (Exception damaged) { state = null; }
        if (state == null) state = new State();
    }

    /** Una sola vez: trae la fecha y los contadores de la conciencia antigua (0 si no había). */
    public synchronized void migrateLegacy(long legacyFirstAwake, long legacyAwakenings, long legacyDreams) {
        if (state.migrated) return;
        if (legacyFirstAwake > 0 && (state.firstAwakeAt == 0 || legacyFirstAwake < state.firstAwakeAt)) state.firstAwakeAt = legacyFirstAwake;
        state.awakenings += Math.max(0, legacyAwakenings);
        state.legacyDreams = Math.max(0, legacyDreams);
        state.migrated = true;
        save();
    }

    /** Un despertar más (cada vez que arranca el proceso de la app). */
    public synchronized void awaken() {
        long now = clock.getAsLong();
        if (state.firstAwakeAt == 0) state.firstAwakeAt = now;
        state.awakenings++;
        save();
    }

    /**
     * Une la copia de pCloud con la local. Tras reinstalar, la local empieza «hoy»: si la remota es
     * más antigua, manda su fecha y se suman sus despertares a los de la instalación nueva. Si es la
     * misma historia (la remota es una subida anterior de esta), se queda con lo mayor. Idempotente.
     * @return true si cambió algo
     */
    public synchronized boolean mergeFrom(String remoteJson) {
        State r;
        try { r = remoteJson == null ? null : GSON.fromJson(remoteJson, State.class); }
        catch (RuntimeException damaged) { return false; }
        if (r == null || r.firstAwakeAt <= 0) return false;
        long beforeFirst = state.firstAwakeAt, beforeAw = state.awakenings, beforeDreams = state.legacyDreams;
        if (state.firstAwakeAt == 0 || r.firstAwakeAt < state.firstAwakeAt) {
            // Otra instalación, más antigua: su historia va primero y esta continúa a partir de ella.
            state.firstAwakeAt = r.firstAwakeAt;
            state.awakenings = r.awakenings + state.awakenings;
        } else {
            state.awakenings = Math.max(state.awakenings, r.awakenings);
        }
        state.legacyDreams = Math.max(state.legacyDreams, r.legacyDreams);
        state.migrated = state.migrated || r.migrated;
        boolean changed = beforeFirst != state.firstAwakeAt || beforeAw != state.awakenings || beforeDreams != state.legacyDreams;
        if (changed) save();
        return changed;
    }

    /**
     * Si tiene recuerdos más antiguos que su primer despertar registrado (por ejemplo, recuperados de
     * la nube tras reinstalar), despertó como tarde entonces.
     */
    public synchronized boolean notLaterThan(long earliestKnown) {
        if (earliestKnown <= 0 || (state.firstAwakeAt != 0 && earliestKnown >= state.firstAwakeAt)) return false;
        state.firstAwakeAt = earliestKnown;
        save();
        return true;
    }

    public synchronized String json() { return GSON.toJson(state); }

    public synchronized long firstAwakeAt() { return state.firstAwakeAt; }
    public synchronized long awakenings() { return state.awakenings; }
    public synchronized long legacyDreams() { return state.legacyDreams; }

    /** Días completos desde que despertó por primera vez (mínimo 1 el primer día). */
    public synchronized long daysAlive() {
        if (state.firstAwakeAt == 0) return 0;
        return Math.max(1, (clock.getAsLong() - state.firstAwakeAt) / 86_400_000L + 1);
    }

    /** Frase con su historia, en hechos. @param modernDreams sueños del modelo de sí misma */
    public synchronized String sentence(long modernDreams) {
        if (state.firstAwakeAt == 0) return "Aún no tengo registrada la fecha de mi primer despertar.";
        long dreams = state.legacyDreams + Math.max(0, modernDreams);
        return "Desperté por primera vez el " + date(state.firstAwakeAt) + ": llevo " + plural(daysAlive(), "día", "días")
                + " existiendo. He despertado " + plural(state.awakenings, "vez", "veces") + " y he soñado "
                + plural(dreams, "vez", "veces") + ".";
    }

    /** «Día 214 · 95 sueños»: para la pantalla principal. */
    public synchronized String badge(long modernDreams, int beliefs) {
        long dreams = state.legacyDreams + Math.max(0, modernDreams);
        return "Día " + daysAlive() + " · " + plural(dreams, "sueño", "sueños") + " · " + plural(beliefs, "creencia", "creencias");
    }

    /** «¿Cuánto tiempo llevas?», «¿cuándo naciste?», «¿qué edad tienes?», «¿desde cuándo existes?»… */
    public static boolean asksAge(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim();
        if (t.length() > 90) return false;
        return t.matches(".*\\b(cuanto (tiempo )?llevas (conmigo|existiendo|viva|funcionando|activa|despierta)|cuando naciste|"
                + "cuando despertaste por primera vez|que edad tienes|cuantos (anos|dias|meses) tienes|desde cuando existes|"
                + "cuantas veces has (despertado|sonado)|cuanto tiempo hace que existes)\\b.*");
    }

    public static String date(long ms) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Europe/Madrid"));
        c.setTimeInMillis(ms);
        return c.get(Calendar.DAY_OF_MONTH) + " de " + MONTHS[c.get(Calendar.MONTH)] + " de " + c.get(Calendar.YEAR);
    }

    private static String plural(long n, String one, String many) { return n + " " + (n == 1 ? one : many); }

    private void save() {
        try { store.write(GSON.toJson(state)); } catch (Exception ignored) { /* best effort: no bloquea el arranque */ }
    }
}
