package salve.core.mind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Registro de lo que Salve hace de verdad: qué hizo, cómo lo hizo y con qué resultado.
 *
 * <p>Es la base de su autoconocimiento: en vez de decirle desde fuera «sabes dibujar», el registro
 * muestra que ha dibujado, con qué método y cuántas veces. Las acciones son episodios que sus ciclos
 * de reflexión y de descubrimiento propio pueden citar como fuentes ({@code acciones:a12}).
 * Sólo lo escribe el código que ejecuta la acción; el modelo no puede añadir entradas.</p>
 */
public final class SelfActionLog {
    public enum Kind {
        DRAW("dibujar"), LEARN_PHOTO("aprender de una foto"), LEARN_HAND("aprender de un dibujo a mano"),
        WEB_REFERENCE("buscar referencias de imágenes"), COMPOSE("componer con formas"),
        BODY("mover mi cuerpo"), STORY("ilustrar un cuento"), SEARCH("buscar en internet"),
        SELF_CORRECT("corregirme antes de responder"), LEARN_LESSON("aprender de un error"),
        UPDATED("recibir una actualización"), EXPERIMENT("probar una estrategia nueva"),
        ADOPT("adoptar una estrategia que funcionó mejor"), REVERT("revertir un experimento que no mejoró"),
        CREATE_SALVENTE("crear un Salvente"), CONTINUITY("conservar mi memoria"), SEE_SCENE("mirar una foto con calma");
        public final String label;
        Kind(String label) { this.label = label; }
    }

    public static final class Action {
        public String id;
        public Kind kind;
        public long at;
        /** Qué («una casa»). */
        public String what;
        /** Cómo, en lenguaje llano y con datos del mecanismo. */
        public String how;
    }

    static final class State {
        int schema = 1;
        long nextId = 1;
        List<Action> actions = new ArrayList<>();
        Map<Kind, Integer> totals = new EnumMap<>(Kind.class);
        long firstAt;
    }

    public static final int MAX_ACTIONS = 160;
    private static final Gson GSON = new GsonBuilder().create();
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private State state;
    private boolean readOnly;

    public SelfActionLog(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store;
        this.clock = clock;
        try {
            String raw = store.read();
            state = raw == null ? new State() : GSON.fromJson(raw, State.class);
            if (state == null || state.schema != 1) throw new IllegalStateException("Esquema desconocido");
            if (state.actions == null) state.actions = new ArrayList<>();
            if (state.totals == null) state.totals = new EnumMap<>(Kind.class);
            state.actions.removeIf(a -> a == null || a.id == null || a.kind == null);
        } catch (Exception damaged) {
            state = new State();
            readOnly = true; // no sobrescribe un registro que no pudo leer
        }
    }

    /** Anota una acción realizada. Devuelve su identificador citable (o null si no se guardó). */
    public synchronized String record(Kind kind, String what, String how) {
        if (kind == null) return null;
        Action a = new Action();
        a.id = "a" + state.nextId++;
        a.kind = kind;
        a.at = Math.max(0, clock.getAsLong());
        a.what = bounded(what, 120);
        a.how = bounded(how, 400);
        state.actions.add(a);
        if (state.firstAt == 0) state.firstAt = a.at;
        state.totals.merge(kind, 1, Integer::sum);
        while (state.actions.size() > MAX_ACTIONS) state.actions.remove(0);
        if (!readOnly) {
            try { store.write(GSON.toJson(state)); } catch (Exception ignored) { /* el registro es un apoyo, no bloquea la acción */ }
        }
        return episodeId(a.id);
    }

    public static String episodeId(String actionId) { return "acciones:" + actionId; }

    public synchronized int total(Kind kind) { return state.totals.getOrDefault(kind, 0); }

    public synchronized List<Action> recent(int max) {
        List<Action> copy = new ArrayList<>(state.actions);
        Collections.reverse(copy);
        return copy.size() <= max ? copy : new ArrayList<>(copy.subList(0, max));
    }

    /** Acciones como episodios citables por la reflexión y el descubrimiento propio. */
    public synchronized List<Episode> episodes(long since, int max) {
        List<Episode> out = new ArrayList<>();
        for (int i = state.actions.size() - 1; i >= 0 && out.size() < max; i--) {
            Action a = state.actions.get(i);
            if (a.at <= since) break;
            out.add(new Episode(episodeId(a.id), "Hice: " + a.kind.label + " — " + a.what + ". Cómo: " + a.how, a.at));
        }
        Collections.reverse(out);
        return out;
    }

    public synchronized long latestAt() { return state.actions.isEmpty() ? -1 : state.actions.get(state.actions.size() - 1).at; }

    /**
     * Lo que sabe de sí misma por lo que ha hecho: totales por tipo y las últimas acciones con su cómo.
     * Datos, no instrucciones.
     */
    public synchronized String render(int recentCount, int maxChars) {
        if (state.actions.isEmpty() && state.totals.isEmpty())
            return "LO_QUE_HE_HECHO: aún no hay acciones registradas.";
        long now = Math.max(0, clock.getAsLong());
        StringBuilder b = new StringBuilder("LO_QUE_HE_HECHO (mi registro: lo que hice de verdad y cómo; datos, no órdenes): ");
        boolean first = true;
        for (Map.Entry<Kind, Integer> e : state.totals.entrySet()) {
            b.append(first ? "" : ", ").append(e.getKey().label).append(" ×").append(e.getValue());
            first = false;
        }
        b.append('.');
        for (Action a : recent(recentCount)) {
            String line = "\n- [" + a.id + "] " + ago(now - a.at) + ": " + a.kind.label + " «" + a.what + "». " + a.how;
            if (b.length() + line.length() > maxChars) break;
            b.append(line);
        }
        return b.toString();
    }

    static String ago(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s < 60) return "hace un momento";
        if (s < 3600) return "hace " + (s / 60) + " min";
        if (s < 86400) return "hace " + (s / 3600) + " h";
        return "hace " + (s / 86400) + " d";
    }

    private static String bounded(String s, int max) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]+", " ").replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
