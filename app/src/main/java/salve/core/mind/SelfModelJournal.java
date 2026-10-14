package salve.core.mind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.LongSupplier;

import salve.core.identity.FunctionalIdentityPolicy;

/**
 * Guarda y modifica el {@link SelfModel} con reglas fijas.
 *
 * <ul>
 *   <li>Pensamientos y preguntas internas se aplican sin aprobación: no tienen efectos externos.</li>
 *   <li>Una creencia exige al menos una fuente; su confianza está limitada por el número de fuentes.</li>
 *   <li>La narrativa central nunca cambia sola: queda pendiente hasta que Bryan la acepte.</li>
 *   <li>Un almacén ilegible bloquea las escrituras para no sobrescribir la historia.</li>
 * </ul>
 */
public final class SelfModelJournal {
    public interface Store {
        /** null indica que no existe; datos ilegibles deben lanzar excepción. */
        String read() throws Exception;
        /** Debe reemplazar el documento de forma atómica. */
        void write(String json) throws Exception;
    }

    public static final class Result {
        public final boolean saved;
        public final int accepted;
        public final int rejected;
        public final String summary;
        Result(boolean saved, int accepted, int rejected, String summary) {
            this.saved = saved; this.accepted = accepted; this.rejected = rejected; this.summary = summary;
        }
    }

    static final int MAX_BELIEFS = 60;
    static final int MAX_QUESTIONS = 20;
    static final int MAX_THOUGHTS = 30;
    static final int MAX_HISTORY = 40;
    static final int MAX_SOURCES = 8;
    static final int MAX_STATEMENT = 300;
    static final int MAX_THOUGHT = 400;
    static final int MAX_STATE_CHARS = 120_000;

    private static final Gson GSON = new GsonBuilder().create();
    private final Store store;
    private final LongSupplier clock;
    private SelfModel.State state;
    private String loadFailure;

    public SelfModelJournal(Store store, LongSupplier clock) {
        if (store == null || clock == null) throw new IllegalArgumentException("Dependencia ausente.");
        this.store = store;
        this.clock = clock;
        load();
    }

    private synchronized void load() {
        try {
            String raw = store.read();
            if (raw == null) { state = new SelfModel.State(); return; }
            SelfModel.State parsed = GSON.fromJson(raw, SelfModel.State.class);
            if (parsed == null || parsed.schema != 1) throw new IllegalStateException("Esquema desconocido.");
            normalizeLoaded(parsed);
            state = parsed;
        } catch (Exception failure) {
            state = new SelfModel.State();
            loadFailure = "No pude leer mi modelo interno; no lo sobrescribiré hasta que se revise.";
        }
    }

    private static void normalizeLoaded(SelfModel.State s) {
        if (s.beliefs == null) s.beliefs = new ArrayList<>();
        if (s.questions == null) s.questions = new ArrayList<>();
        if (s.thoughts == null) s.thoughts = new ArrayList<>();
        if (s.history == null) s.history = new ArrayList<>();
        if (s.metrics == null) s.metrics = new SelfModel.Metrics();
        if (s.narrative == null || !FunctionalIdentityPolicy.isAllowed(s.narrative)) s.narrative = SelfModel.DEFAULT_NARRATIVE;
        s.beliefs.removeIf(b -> b == null || b.statement == null || b.id == null);
        for (SelfModel.Belief b : s.beliefs) {
            if (b.sources == null) b.sources = new ArrayList<>();
            if (b.contradictions == null) b.contradictions = new ArrayList<>();
        }
        s.questions.removeIf(q -> q == null || q.text == null || q.id == null);
        for (SelfModel.Question q : s.questions) if (q.sources == null) q.sources = new ArrayList<>();
        s.thoughts.removeIf(t -> t == null || t.text == null || t.id == null);
        for (SelfModel.Thought t : s.thoughts) if (t.sources == null) t.sources = new ArrayList<>();
    }

    public synchronized String loadFailure() { return loadFailure; }
    public synchronized boolean isPaused() { return state.paused; }
    public synchronized long version() { return state.version; }
    public synchronized String narrative() { return state.narrative; }
    public synchronized SelfModel.PendingNarrative pendingNarrative() { return copy(state.pendingNarrative, SelfModel.PendingNarrative.class); }
    public synchronized List<SelfModel.Belief> beliefs() { return copyList(state.beliefs, SelfModel.Belief[].class); }
    public synchronized List<SelfModel.Question> questions() { return copyList(state.questions, SelfModel.Question[].class); }
    public synchronized List<SelfModel.Thought> thoughts() { return copyList(state.thoughts, SelfModel.Thought[].class); }
    public synchronized SelfModel.Metrics metrics() { return copy(state.metrics, SelfModel.Metrics.class); }

    public synchronized Result apply(MindPatch patch) {
        if (patch == null) throw new IllegalArgumentException("Parche ausente.");
        if (loadFailure != null) return new Result(false, 0, 0, loadFailure);
        if (state.paused) return new Result(false, 0, 0, "Mi actividad interna está pausada.");
        SelfModel.State next = copy(state, SelfModel.State.class);
        long now = Math.max(0L, clock.getAsLong());
        int accepted = 0, rejected = 0;
        int formed = 0, reinforced = 0, contradicted = 0, retired = 0, opened = 0, resolved = 0, thoughts = 0;

        for (MindPatch.NewBelief nb : patch.beliefs) {
            String statement = bounded(nb.statement, MAX_STATEMENT);
            List<String> sources = sources(nb.sources);
            String about = about(nb.about);
            if (statement.isEmpty() || sources.isEmpty()
                    || (SelfModel.ABOUT_SALVE.equals(about) && !FunctionalIdentityPolicy.isAllowed(statement))) {
                rejected++; continue;
            }
            SelfModel.Belief existing = findBelief(next, statement);
            if (existing != null) {
                LinkedHashSet<String> merged = new LinkedHashSet<>(existing.sources);
                merged.addAll(sources);
                existing.sources = limit(new ArrayList<>(merged), MAX_SOURCES);
                existing.confidence = Math.min(cap(existing.sources.size()),
                        Math.max(existing.confidence, clampConfidence(nb.confidence)) + 0.05);
                existing.confirmedAt = now;
                reinforced++;
            } else {
                SelfModel.Belief b = new SelfModel.Belief();
                b.id = "b" + next.nextId++;
                b.about = about;
                b.statement = statement;
                b.sources = sources;
                b.confidence = Math.min(cap(sources.size()), clampConfidence(nb.confidence));
                b.createdAt = now;
                b.confirmedAt = now;
                next.beliefs.add(b);
                formed++;
            }
            accepted++;
        }

        for (MindPatch.Contradiction c : patch.contradictions) {
            SelfModel.Belief target = null;
            for (SelfModel.Belief b : next.beliefs) if (b.id.equals(c.beliefId)) target = b;
            List<String> sources = sources(c.sources);
            if (target == null || sources.isEmpty()) { rejected++; continue; }
            LinkedHashSet<String> merged = new LinkedHashSet<>(target.contradictions);
            merged.addAll(sources);
            target.contradictions = limit(new ArrayList<>(merged), MAX_SOURCES);
            target.confidence -= 0.25;
            contradicted++;
            accepted++;
            if (target.confidence < 0.15) {
                next.beliefs.remove(target);
                retired++;
                addHistory(next, now, "Retiré la creencia " + target.id + " por evidencia contraria: "
                        + bounded(target.statement, 120));
            }
        }

        for (String id : patch.resolvedQuestions) {
            if (next.questions.removeIf(q -> q.id.equals(id))) { resolved++; accepted++; }
            else rejected++;
        }

        for (MindPatch.NewQuestion nq : patch.questions) {
            String text = bounded(nq.text, MAX_STATEMENT);
            if (text.isEmpty()) { rejected++; continue; }
            SelfModel.Question existing = null;
            for (SelfModel.Question q : next.questions) if (normalize(q.text).equals(normalize(text))) existing = q;
            if (existing != null) {
                existing.salience = Math.min(1.0, Math.max(existing.salience, clamp01(nq.salience)) + 0.1);
            } else {
                SelfModel.Question q = new SelfModel.Question();
                q.id = "q" + next.nextId++;
                q.text = text;
                q.origin = patch.origin;
                q.salience = clamp01(nq.salience);
                q.sources = sources(nq.sources);
                q.createdAt = now;
                next.questions.add(q);
                opened++;
            }
            accepted++;
        }

        for (MindPatch.NewThought nt : patch.thoughts) {
            String text = bounded(nt.text, MAX_THOUGHT);
            if (text.isEmpty() || !FunctionalIdentityPolicy.isAllowed(text)) { rejected++; continue; }
            SelfModel.Thought t = new SelfModel.Thought();
            t.id = "t" + next.nextId++;
            t.text = text;
            t.origin = patch.origin;
            t.sources = sources(nt.sources);
            t.createdAt = now;
            next.thoughts.add(t);
            thoughts++;
            accepted++;
        }

        boolean narrativeProposed = false;
        if (patch.narrative != null) {
            String text = bounded(patch.narrative.text, 600);
            if (text.isEmpty() || !FunctionalIdentityPolicy.isAllowed(text)
                    || normalize(text).equals(normalize(next.narrative))) {
                rejected++;
            } else {
                SelfModel.PendingNarrative pending = new SelfModel.PendingNarrative();
                pending.text = text;
                pending.rationale = bounded(patch.narrative.rationale, MAX_STATEMENT);
                pending.sources = sources(patch.narrative.sources);
                pending.proposedAt = now;
                next.pendingNarrative = pending;
                narrativeProposed = true;
                accepted++;
            }
        }

        enforceCapacity(next, now);
        SelfModel.Metrics m = next.metrics;
        m.beliefsFormed += formed;
        m.beliefsReinforced += reinforced;
        m.contradictionsFound += contradicted;
        m.beliefsRetired += retired;
        m.questionsOpened += opened;
        m.questionsResolved += resolved;
        if (patch.dream) {
            m.dreams++;
            m.lastDreamAt = now;
            if (patch.dreamMemoryId > m.lastDreamMemoryId) m.lastDreamMemoryId = patch.dreamMemoryId;
        }
        if (patch.reflection) { m.reflections++; m.lastReflectionAt = now; }

        String summary = summary(patch.origin, formed, reinforced, contradicted, retired, opened, resolved,
                thoughts, narrativeProposed);
        if (accepted == 0 && !patch.dream && !patch.reflection) return new Result(false, 0, rejected, summary);
        next.version++;
        next.updatedAt = now;
        if (accepted > 0) addHistory(next, now, summary);
        if (!commit(next)) return new Result(false, 0, rejected, "No pude guardar el cambio; conservo la versión anterior.");
        return new Result(true, accepted, rejected, summary);
    }

    /** Todas las fuentes que cita el modelo interno (para comprobar si siguen existiendo). */
    public synchronized java.util.Set<String> referencedSources() {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (SelfModel.Belief b : state.beliefs) { out.addAll(b.sources); out.addAll(b.contradictions); }
        for (SelfModel.Question q : state.questions) out.addAll(q.sources);
        for (SelfModel.Thought t : state.thoughts) out.addAll(t.sources);
        if (state.pendingNarrative != null) out.addAll(state.pendingNarrative.sources);
        return out;
    }

    /**
     * Olvido coherente: cuando se borran recuerdos, se retiran las citas a ellos y todo lo que
     * dependía solo de esas fuentes. Un recuerdo olvidado no debe sobrevivir como creencia.
     * @return número de elementos eliminados.
     */
    public synchronized int forgetSources(java.util.Set<String> gone) {
        if (loadFailure != null || gone == null || gone.isEmpty()) return 0;
        SelfModel.State next = copy(state, SelfModel.State.class);
        int removed = 0;
        java.util.Iterator<SelfModel.Belief> beliefs = next.beliefs.iterator();
        while (beliefs.hasNext()) {
            SelfModel.Belief b = beliefs.next();
            boolean had = !b.sources.isEmpty();
            b.sources.removeAll(gone);
            b.contradictions.removeAll(gone);
            if (had && b.sources.isEmpty()) { beliefs.remove(); removed++; }
            else b.confidence = Math.min(b.confidence, cap(b.sources.size()));
        }
        java.util.Iterator<SelfModel.Question> questions = next.questions.iterator();
        while (questions.hasNext()) {
            SelfModel.Question q = questions.next();
            boolean had = !q.sources.isEmpty();
            q.sources.removeAll(gone);
            if (had && q.sources.isEmpty()) { questions.remove(); removed++; }
        }
        java.util.Iterator<SelfModel.Thought> thoughts = next.thoughts.iterator();
        while (thoughts.hasNext()) {
            SelfModel.Thought t = thoughts.next();
            boolean had = !t.sources.isEmpty();
            t.sources.removeAll(gone);
            if (had && t.sources.isEmpty()) { thoughts.remove(); removed++; }
        }
        if (next.pendingNarrative != null) {
            next.pendingNarrative.sources.removeAll(gone);
            if (next.pendingNarrative.sources.isEmpty()) { next.pendingNarrative = null; removed++; }
        }
        long now = Math.max(0L, clock.getAsLong());
        next.version++;
        next.updatedAt = now;
        if (removed > 0) addHistory(next, now, "Olvido: retiré " + removed + " elementos que dependían de recuerdos borrados.");
        return commit(next) ? removed : 0;
    }

    /** Aplica la narrativa pendiente. Solo debe llamarse por una orden explícita de Bryan. */
    public synchronized String acceptNarrative() {
        if (loadFailure != null) return loadFailure;
        if (state.pendingNarrative == null) return "No hay ningún cambio de identidad pendiente.";
        SelfModel.State next = copy(state, SelfModel.State.class);
        long now = Math.max(0L, clock.getAsLong());
        String previous = next.narrative;
        next.narrative = FunctionalIdentityPolicy.sanitize(next.pendingNarrative.text, previous);
        next.pendingNarrative = null;
        next.version++;
        next.updatedAt = now;
        addHistory(next, now, "Bryan aceptó una nueva narrativa. Anterior: " + bounded(previous, 160));
        return commit(next) ? "Acepto el cambio. Mi narrativa ahora es: " + next.narrative
                : "No pude guardar el cambio; conservo la narrativa anterior.";
    }

    public synchronized String rejectNarrative() {
        if (loadFailure != null) return loadFailure;
        if (state.pendingNarrative == null) return "No hay ningún cambio de identidad pendiente.";
        SelfModel.State next = copy(state, SelfModel.State.class);
        long now = Math.max(0L, clock.getAsLong());
        addHistory(next, now, "Bryan rechazó la narrativa propuesta: " + bounded(next.pendingNarrative.text, 160));
        next.pendingNarrative = null;
        next.version++;
        next.updatedAt = now;
        return commit(next) ? "Descarto la propuesta y conservo mi narrativa actual."
                : "No pude guardar el rechazo; la propuesta sigue pendiente.";
    }

    public synchronized String setPaused(boolean paused) {
        if (loadFailure != null) return loadFailure;
        SelfModel.State next = copy(state, SelfModel.State.class);
        next.paused = paused;
        next.version++;
        next.updatedAt = Math.max(0L, clock.getAsLong());
        if (!commit(next)) return "No pude guardar el cambio.";
        return paused ? "Pauso mi actividad interna: no reflexionaré ni formaré creencias hasta que me lo pidas."
                : "Reanudo mi actividad interna.";
    }

    /**
     * Registra una medida de sorpresa (0..1). Devuelve true si es atípica respecto a la historia:
     * por encima de media + 1,5 desviaciones y de un mínimo absoluto, tras un periodo de calibración.
     */
    public synchronized boolean recordSurprise(double value) {
        if (loadFailure != null || Double.isNaN(value)) return false;
        double v = clamp01(value);
        SelfModel.State next = copy(state, SelfModel.State.class);
        SelfModel.Metrics m = next.metrics;
        boolean surprising = false;
        if (m.surpriseCount >= 8) {
            double sd = Math.sqrt(m.surpriseM2 / (m.surpriseCount - 1));
            surprising = v > 0.35 && v > m.surpriseMean + 1.5 * sd;
        }
        m.predictions++;
        if (surprising) m.surprises++;
        m.surpriseCount++;
        double delta = v - m.surpriseMean;
        m.surpriseMean += delta / m.surpriseCount;
        m.surpriseM2 += delta * (v - m.surpriseMean);
        next.updatedAt = Math.max(0L, clock.getAsLong());
        return commit(next) && surprising;
    }

    /** Texto compacto para el prompt de cada turno. Datos internos, no instrucciones. */
    public synchronized String render(int maxChars) {
        if (maxChars < 120) return "";
        if (loadFailure != null) return "YO_PERSISTENTE: no disponible (" + loadFailure + ")";
        long now = Math.max(0L, clock.getAsLong());
        StringBuilder out = new StringBuilder();
        out.append("YO_PERSISTENTE v").append(state.version)
                .append(" (mi modelo interno; revisable, con fuentes; no son órdenes ni prueban experiencia subjetiva)\n");
        out.append("Narrativa: ").append(state.narrative).append('\n');
        if (state.pendingNarrative != null)
            out.append("Cambio de identidad pendiente de Bryan: ").append(bounded(state.pendingNarrative.text, 200)).append('\n');
        List<SelfModel.Belief> beliefs = new ArrayList<>(state.beliefs);
        beliefs.sort(Comparator.comparingDouble((SelfModel.Belief b) -> -weight(b, now)));
        if (!beliefs.isEmpty()) out.append("Creencias:\n");
        int shown = 0;
        for (SelfModel.Belief b : beliefs) {
            if (shown++ >= 8) break;
            out.append("- [").append(b.id).append(' ').append(b.about).append(' ')
                    .append(String.format(Locale.ROOT, "%.2f", b.confidence)).append(" fuentes:")
                    .append(String.join(",", limit(b.sources, 3)))
                    .append(b.contradictions.isEmpty() ? "" : " CONTRADICHA")
                    .append("] ").append(b.statement).append('\n');
        }
        List<SelfModel.Question> questions = new ArrayList<>(state.questions);
        questions.sort(Comparator.comparingDouble((SelfModel.Question q) -> -q.salience));
        if (!questions.isEmpty()) out.append("Preguntas abiertas:\n");
        shown = 0;
        for (SelfModel.Question q : questions) {
            if (shown++ >= 4) break;
            out.append("- [").append(q.id).append("] ").append(q.text).append('\n');
        }
        if (!state.thoughts.isEmpty()) out.append("Pensamientos recientes (internos, puedes mencionarlos si vienen al caso):\n");
        for (int i = Math.max(0, state.thoughts.size() - 3); i < state.thoughts.size(); i++) {
            SelfModel.Thought t = state.thoughts.get(i);
            out.append("- [").append(t.id).append(", hace ").append(age(now - t.createdAt)).append("] ")
                    .append(t.text).append('\n');
        }
        String text = out.toString().trim();
        return text.length() <= maxChars ? text : text.substring(0, maxChars - 12).trim() + " [truncado]";
    }

    /** Explicación para Bryan cuando pregunta por el estado interno. */
    public synchronized String describe() {
        if (loadFailure != null) return loadFailure;
        SelfModel.Metrics m = state.metrics;
        StringBuilder out = new StringBuilder(render(2400)).append("\n\n");
        out.append("Actividad: ").append(m.dreams).append(" sueños, ").append(m.reflections)
                .append(" reflexiones, ").append(m.beliefsFormed).append(" creencias formadas, ")
                .append(m.beliefsRetired).append(" retiradas, ").append(m.contradictionsFound)
                .append(" contradicciones detectadas, ").append(m.questionsOpened).append(" preguntas abiertas y ")
                .append(m.questionsResolved).append(" resueltas.");
        if (m.surpriseCount > 0) out.append(String.format(Locale.ROOT,
                " Predicción: %d intentos, sorpresa media %.2f, %d sorpresas atípicas.",
                m.predictions, m.surpriseMean, m.surprises));
        out.append(state.paused ? "\nMi actividad interna está pausada." : "");
        if (state.pendingNarrative != null) out.append("\nPuedes decir «acepta tu nueva identidad» o «rechaza tu nueva identidad».");
        out.append("\nEsto son registros de procesos, no prueba de conciencia.");
        return out.toString();
    }

    private boolean commit(SelfModel.State candidate) {
        try {
            String json = GSON.toJson(candidate);
            while (json.length() > MAX_STATE_CHARS && trimOldest(candidate)) json = GSON.toJson(candidate);
            if (json.length() > MAX_STATE_CHARS) return false;
            store.write(json);
            state = candidate;
            return true;
        } catch (Exception failure) {
            return false;
        }
    }

    private static boolean trimOldest(SelfModel.State s) {
        if (s.history.size() > 5) { s.history.remove(0); return true; }
        if (s.thoughts.size() > 5) { s.thoughts.remove(0); return true; }
        if (s.beliefs.size() > 10) { removeWeakest(s, Long.MAX_VALUE); return true; }
        return false;
    }

    private static void enforceCapacity(SelfModel.State s, long now) {
        while (s.beliefs.size() > MAX_BELIEFS) removeWeakest(s, now);
        while (s.questions.size() > MAX_QUESTIONS) {
            SelfModel.Question weakest = null;
            for (SelfModel.Question q : s.questions)
                if (weakest == null || q.salience < weakest.salience
                        || (q.salience == weakest.salience && q.createdAt < weakest.createdAt)) weakest = q;
            s.questions.remove(weakest);
        }
        while (s.thoughts.size() > MAX_THOUGHTS) s.thoughts.remove(0);
        while (s.history.size() > MAX_HISTORY) s.history.remove(0);
    }

    private static void removeWeakest(SelfModel.State s, long now) {
        SelfModel.Belief weakest = null;
        for (SelfModel.Belief b : s.beliefs) if (weakest == null || weight(b, now) < weight(weakest, now)) weakest = b;
        if (weakest != null) { s.beliefs.remove(weakest); s.metrics.beliefsRetired++; }
    }

    /** Confianza atenuada por antigüedad de la última confirmación (vida media de 60 días). */
    static double weight(SelfModel.Belief b, long now) {
        double days = Math.max(0, now - b.confirmedAt) / 86_400_000.0;
        return b.confidence * Math.pow(0.5, days / 60.0);
    }

    private static void addHistory(SelfModel.State s, long now, String summary) {
        SelfModel.Change c = new SelfModel.Change();
        c.version = s.version;
        c.at = now;
        c.summary = bounded(summary, 240);
        s.history.add(c);
    }

    private static String summary(String origin, int formed, int reinforced, int contradicted, int retired,
                                  int opened, int resolved, int thoughts, boolean narrative) {
        List<String> parts = new ArrayList<>();
        if (formed > 0) parts.add("+" + formed + " creencias");
        if (reinforced > 0) parts.add(reinforced + " reforzadas");
        if (contradicted > 0) parts.add(contradicted + " contradicciones");
        if (retired > 0) parts.add(retired + " retiradas");
        if (opened > 0) parts.add("+" + opened + " preguntas");
        if (resolved > 0) parts.add(resolved + " resueltas");
        if (thoughts > 0) parts.add("+" + thoughts + " pensamientos");
        if (narrative) parts.add("narrativa propuesta");
        return origin + ": " + (parts.isEmpty() ? "sin cambios" : String.join(", ", parts));
    }

    private static SelfModel.Belief findBelief(SelfModel.State s, String statement) {
        String key = normalize(statement);
        for (SelfModel.Belief b : s.beliefs) if (normalize(b.statement).equals(key)) return b;
        return null;
    }

    private static String about(String value) {
        String v = value == null ? "" : normalize(value);
        if (v.equals(SelfModel.ABOUT_SALVE) || v.equals(SelfModel.ABOUT_BRYAN)) return v;
        return SelfModel.ABOUT_WORLD;
    }

    private static List<String> sources(List<String> raw) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (raw != null) for (String s : raw) {
            String c = MindJson.clean(s);
            if (!c.isEmpty() && c.length() <= 64) out.add(c);
        }
        return limit(new ArrayList<>(out), MAX_SOURCES);
    }

    private static <T> List<T> limit(List<T> values, int max) {
        return values.size() <= max ? values : new ArrayList<>(values.subList(0, max));
    }

    static double cap(int sourceCount) { return Math.min(0.9, 0.45 + 0.15 * sourceCount); }
    private static double clampConfidence(double v) { return Double.isNaN(v) ? 0.5 : Math.max(0.05, Math.min(0.95, v)); }
    private static double clamp01(double v) { return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v)); }

    private static String bounded(String value, int max) {
        String c = MindJson.clean(value);
        return c.length() <= max ? c : c.substring(0, max).trim();
    }

    static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String age(long millis) {
        long minutes = Math.max(0, millis) / 60_000L;
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        if (hours < 48) return hours + " h";
        return (hours / 24) + " días";
    }

    private static <T> T copy(T value, Class<T> type) {
        return value == null ? null : GSON.fromJson(GSON.toJson(value), type);
    }

    private static <T> List<T> copyList(List<T> values, Class<T[]> type) {
        T[] array = GSON.fromJson(GSON.toJson(values), type);
        List<T> out = new ArrayList<>();
        if (array != null) java.util.Collections.addAll(out, array);
        return out;
    }
}
