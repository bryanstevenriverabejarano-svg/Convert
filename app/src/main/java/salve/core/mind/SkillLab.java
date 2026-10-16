package salve.core.mind;

import com.google.gson.Gson;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Laboratorio de habilidades: la experiencia sólo se vuelve aprendizaje cuando se observa, se verifica
 * y se convierte en algo reutilizable (ideas de Agent-Reflex, Socratic Skill Learning, «Imitation holds
 * a key to evolution», «Can an agent learn to predict its own failure?» y SpecRSI, adaptadas a un móvil).
 *
 * <p>Cada habilidad es un procedimiento con pasos, y cada paso tiene variantes (estrategias). El ciclo:</p>
 * <ol>
 *   <li><b>Plan</b>: elige la variante vigente de cada paso y <b>predice</b> si saldrá bien (su historial).</li>
 *   <li><b>Observación real</b>: el resultado lo da un verificador que no es su propia opinión (la búsqueda
 *       devolvió algo usable, su visión reconoce el dibujo sin saber qué quería, Bryan la corrige o lo
 *       aprueba). Sin evidencia no aprende nada.</li>
 *   <li><b>Reparación local</b>: el fallo se atribuye al paso que falló, no a todo el procedimiento; si la
 *       variante de ese paso falla a menudo, abre un experimento con otra variante sólo para ese paso.</li>
 *   <li><b>Evaluación y adoptar/revertir</b>: el experimento tiene presupuesto (pocas pruebas, pocos al
 *       día); sólo se adopta si supera claramente a la vigente; si no, se revierte y espera antes de
 *       repetirlo. Se guarda también lo que falló, no sólo lo que salió bien.</li>
 *   <li><b>Calibración</b>: compara cada predicción con el resultado (puntuación de Brier) para saber si
 *       su confianza en sí misma es fiable.</li>
 * </ol>
 * Es metacognición funcional y medible, no conciencia; no reentrena el modelo.
 */
public final class SkillLab {

    // ───────────── habilidades que conoce ─────────────

    /** Un paso de una habilidad y sus variantes; la primera es la de partida. */
    public static final class Step {
        public final String name, about;
        public final List<String> variants;
        Step(String name, String about, String... variants) {
            this.name = name; this.about = about; this.variants = java.util.Arrays.asList(variants);
        }
    }

    public static final class Skill {
        public final String name, label;
        public final List<Step> steps;
        Skill(String name, String label, Step... steps) { this.name = name; this.label = label; this.steps = java.util.Arrays.asList(steps); }
        Step step(String n) { for (Step s : steps) if (s.name.equals(n)) return s; return null; }
    }

    public static final String DIBUJAR = "dibujar", REFERENCIAS = "referencias", RESPONDER = "responder";

    /** Habilidades de Salve con pasos que se pueden reparar por separado. */
    public static List<Skill> defaults() {
        List<Skill> out = new ArrayList<>();
        out.add(new Skill(DIBUJAR, "dibujar algo nuevo",
                new Step("estrategia", "qué intento primero", "referencia_primero", "imaginar_primero")));
        out.add(new Skill(REFERENCIAS, "buscar imágenes de referencia",
                new Step("fuente", "dónde busco primero", "commons", "wikipedia", "openverse")));
        out.add(new Skill(RESPONDER, "responder a lo que Bryan pide",
                new Step("enfoque", "cómo encaro la respuesta", "directo", "anunciar_y_hacer", "un_paso")));
        return out;
    }

    /** Qué significa cada variante, en palabras (para contarlo y para el contexto). */
    public static String describe(String variant) {
        switch (variant == null ? "" : variant) {
            case "referencia_primero": return "buscar referencias reales primero";
            case "imaginar_primero": return "imaginarlo parte por parte primero";
            case "commons": return "Wikimedia Commons primero";
            case "wikipedia": return "la imagen del artículo de Wikipedia primero";
            case "openverse": return "Openverse primero";
            case "directo": return "responder directamente";
            case "anunciar_y_hacer": return "decir en media frase qué voy a hacer y hacerlo";
            case "un_paso": return "hacer sólo el siguiente paso concreto";
            default: return variant == null ? "" : variant.replace('_', ' ');
        }
    }

    // ───────────── estado persistente ─────────────

    /** Historial reciente («1» bien, «0» mal), los últimos {@link #WINDOW}. */
    public static final class Stat { public int n, ok; public String recent = ""; }

    public static final class Trial {
        public String skill, step, context, champion, challenger;
        public long startedAt;
        public int n, ok;
        /** Lo que llevaba la vigente cuando empezó el experimento. */
        public int baseN, baseOk;
    }

    public static final class Outcome {
        public long at;
        public String skill, context, variants, failedStep, verifier, evidence;
        public boolean ok, trial;
        public float predicted;
    }

    public static final class Decision {
        public long at;
        public String skill, step, context, from, to, verdict, why;
    }

    static final class State {
        int schema = 1;
        Map<String, String> champions = new LinkedHashMap<>();
        Map<String, Stat> stats = new LinkedHashMap<>();
        List<Trial> trials = new ArrayList<>();
        List<Outcome> history = new ArrayList<>();
        List<Decision> decisions = new ArrayList<>();
        Map<String, Long> cooldown = new LinkedHashMap<>();
        Map<String, float[]> brier = new LinkedHashMap<>(); // skill → {suma, n}
        String budgetDay = "";
        int trialsToday;
    }

    public static final int WINDOW = 12, MIN_TRIAL = 3, MAX_TRIAL = 6, MAX_TRIALS_PER_DAY = 3, HISTORY = 80, DECISIONS = 20;
    public static final float ADOPT_MARGIN = 0.15f;
    public static final long COOLDOWN_MS = 7L * 24 * 3600 * 1000, TRIAL_IDLE_MS = 14L * 24 * 3600 * 1000,
            PENDING_MS = 15L * 60 * 1000;

    private static final Gson GSON = new Gson();
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private final Map<String, Skill> skills = new LinkedHashMap<>();
    private State state;

    public SkillLab(SelfModelJournal.Store store, LongSupplier clock) { this(store, clock, defaults()); }

    public SkillLab(SelfModelJournal.Store store, LongSupplier clock, List<Skill> known) {
        this.store = store; this.clock = clock;
        for (Skill s : known) skills.put(s.name, s);
        try {
            String raw = store.read();
            state = raw == null ? new State() : GSON.fromJson(raw, State.class);
        } catch (Exception damaged) { state = null; }
        if (state == null) state = new State();
        if (state.champions == null) state.champions = new LinkedHashMap<>();
        if (state.stats == null) state.stats = new LinkedHashMap<>();
        if (state.trials == null) state.trials = new ArrayList<>();
        if (state.history == null) state.history = new ArrayList<>();
        if (state.decisions == null) state.decisions = new ArrayList<>();
        if (state.cooldown == null) state.cooldown = new LinkedHashMap<>();
        if (state.brier == null) state.brier = new LinkedHashMap<>();
        if (state.budgetDay == null) state.budgetDay = "";
    }

    // ───────────── 1. plan y predicción ─────────────

    /** Lo que va a hacer: una variante por paso, y cuánto confía en que saldrá bien. */
    public static final class Plan {
        public final String skill, context;
        public final Map<String, String> variants;
        public final float predicted;
        /** Cuántos resultados respaldan la predicción. */
        public final int evidence;
        /** Paso en experimento (o null). */
        public final String trialStep;
        Plan(String skill, String context, Map<String, String> variants, float predicted, int evidence, String trialStep) {
            this.skill = skill; this.context = context; this.variants = variants; this.predicted = predicted;
            this.evidence = evidence; this.trialStep = trialStep;
        }
        public String variant(String step) { String v = variants.get(step); return v == null ? "" : v; }
        public boolean lowConfidence() { return evidence >= 4 && predicted < 0.4f; }
    }

    public synchronized Plan plan(String skill, String context) {
        Skill s = skills.get(skill);
        if (s == null) throw new IllegalArgumentException("Habilidad desconocida: " + skill);
        String ctx = context(context);
        expireIdleTrials();
        Map<String, String> chosen = new LinkedHashMap<>();
        String trialStep = null;
        for (Step step : s.steps) {
            Trial t = trial(skill, step.name, ctx);
            if (t != null) { chosen.put(step.name, t.challenger); trialStep = step.name; }
            else chosen.put(step.name, champion(s, step, ctx));
        }
        // Predicción: su historial reciente en este contexto (o en general si aún no hay bastante).
        Stat local = state.stats.get(key(skill, "*", "*", ctx)), global = state.stats.get(key(skill, "*", "*", ""));
        Stat basis = local != null && local.recent.length() >= 3 ? local : global;
        int n = basis == null ? 0 : basis.recent.length(), ok = basis == null ? 0 : count(basis.recent);
        float predicted = (ok + 1f) / (n + 2f);
        return new Plan(skill, ctx, chosen, predicted, n, trialStep);
    }

    private String champion(Skill s, Step step, String ctx) {
        String v = state.champions.get(key(s.name, step.name, "", ctx));
        if (v == null || !step.variants.contains(v)) v = state.champions.get(key(s.name, step.name, "", ""));
        return v == null || !step.variants.contains(v) ? step.variants.get(0) : v;
    }

    // ───────────── 2. observación real y 3. reparación local ─────────────

    /**
     * Registra un resultado verificado. {@code failedStep}: el paso que falló (sólo él carga con el fallo);
     * null si falló todo o no se sabe. @return la decisión del laboratorio si la hubo (empezar, adoptar o
     * revertir un experimento), o null.
     */
    public synchronized Decision record(Plan plan, boolean ok, String failedStep, String verifier, String evidence) {
        if (plan == null) return null;
        long now = clock.getAsLong();
        Outcome o = new Outcome();
        o.at = now; o.skill = plan.skill; o.context = plan.context; o.ok = ok; o.predicted = plan.predicted;
        o.failedStep = ok ? null : failedStep; o.verifier = verifier; o.evidence = bounded(evidence, 160);
        o.trial = plan.trialStep != null;
        StringBuilder vs = new StringBuilder();
        for (Map.Entry<String, String> e : plan.variants.entrySet())
            vs.append(vs.length() == 0 ? "" : ", ").append(e.getKey()).append('=').append(e.getValue());
        o.variants = vs.toString();
        state.history.add(o);
        while (state.history.size() > HISTORY) state.history.remove(0);
        // Calibración: ¿acertó al predecir?
        float[] b = state.brier.get(plan.skill);
        if (b == null) state.brier.put(plan.skill, b = new float[2]);
        float err = plan.predicted - (ok ? 1f : 0f);
        b[0] += err * err; b[1] += 1;
        bump(key(plan.skill, "*", "*", plan.context), ok);
        if (!plan.context.isEmpty()) bump(key(plan.skill, "*", "*", ""), ok);
        Decision decision = null;
        for (Map.Entry<String, String> e : plan.variants.entrySet()) {
            String step = e.getKey(), variant = e.getValue();
            // Crédito local: si se sabe qué paso falló, los demás no cargan con ese fallo.
            if (!ok && failedStep != null && !failedStep.equals(step)) continue;
            bump(key(plan.skill, step, variant, plan.context), ok);
            if (!plan.context.isEmpty()) bump(key(plan.skill, step, variant, ""), ok);
            Trial t = trial(plan.skill, step, plan.context);
            if (t != null && t.challenger.equals(variant)) {
                t.n++; if (ok) t.ok++;
                Decision d = evaluate(t, now);
                if (d != null) decision = d;
            } else if (!ok && t == null) {
                Decision d = maybeOpenTrial(plan.skill, step, variant, plan.context, now);
                if (d != null) decision = d;
            }
        }
        save();
        return decision;
    }

    private void bump(String k, boolean ok) {
        Stat s = state.stats.get(k);
        if (s == null) state.stats.put(k, s = new Stat());
        s.n++; if (ok) s.ok++;
        s.recent = (s.recent == null ? "" : s.recent) + (ok ? '1' : '0');
        if (s.recent.length() > WINDOW) s.recent = s.recent.substring(s.recent.length() - WINDOW);
    }

    /** La variante vigente de un paso falla a menudo: prueba otra, sólo en ese paso. */
    private Decision maybeOpenTrial(String skill, String step, String champion, String ctx, long now) {
        Skill s = skills.get(skill);
        Step st = s == null ? null : s.step(step);
        if (st == null || st.variants.size() < 2) return null;
        if (!champion.equals(champion(s, st, ctx))) return null;
        Stat c = state.stats.get(key(skill, step, champion, ctx));
        if (c == null || c.recent.length() < 4) return null;
        int fails = c.recent.length() - count(c.recent);
        if (fails < 3 || fails * 2 < c.recent.length()) return null;
        for (Trial t : state.trials) if (t.skill.equals(skill)) return null; // un experimento por habilidad
        String day = day(now);
        if (!day.equals(state.budgetDay)) { state.budgetDay = day; state.trialsToday = 0; }
        if (state.trialsToday >= MAX_TRIALS_PER_DAY) return null;
        String challenger = null;
        int from = st.variants.indexOf(champion);
        for (int i = 1; i < st.variants.size() && challenger == null; i++) {
            String v = st.variants.get((from + i) % st.variants.size());
            Long until = state.cooldown.get(key(skill, step, v, ctx));
            if (until == null || until <= now) challenger = v;
        }
        if (challenger == null) return null;
        Trial t = new Trial();
        t.skill = skill; t.step = step; t.context = ctx; t.champion = champion; t.challenger = challenger;
        t.startedAt = now; t.baseN = c.recent.length(); t.baseOk = count(c.recent);
        state.trials.add(t);
        state.trialsToday++;
        return decision(t, "experimento", "«" + describe(champion) + "» falló " + fails + " de las últimas "
                + c.recent.length() + " veces" + where(ctx) + "; pruebo «" + describe(challenger) + "» sólo en ese paso", now);
    }

    /** Evaluación independiente del experimento: adoptar, revertir o seguir probando. */
    private Decision evaluate(Trial t, long now) {
        if (t.n < MIN_TRIAL) return null;
        float challenger = (t.ok + 1f) / (t.n + 2f), base = (t.baseOk + 1f) / (t.baseN + 2f);
        String score = t.ok + "/" + t.n + " frente a " + t.baseOk + "/" + t.baseN;
        if (t.ok >= 2 && challenger >= base + ADOPT_MARGIN) {
            state.champions.put(key(t.skill, t.step, "", t.context), t.challenger);
            state.trials.remove(t);
            return decision(t, "adoptado", "«" + describe(t.challenger) + "» funcionó mejor (" + score + ")" + where(t.context), now);
        }
        if (t.n >= MAX_TRIAL || t.ok == 0) {
            state.cooldown.put(key(t.skill, t.step, t.challenger, t.context), now + COOLDOWN_MS);
            state.trials.remove(t);
            return decision(t, "revertido", "«" + describe(t.challenger) + "» no fue mejor (" + score + "); vuelvo a «"
                    + describe(t.champion) + "»" + where(t.context), now);
        }
        return null;
    }

    private void expireIdleTrials() {
        long now = clock.getAsLong();
        for (Trial t : new ArrayList<>(state.trials))
            if (now - t.startedAt > TRIAL_IDLE_MS) {
                state.trials.remove(t);
                decision(t, "revertido", "el experimento con «" + describe(t.challenger) + "» se quedó sin pruebas suficientes", now);
            }
    }

    private Decision decision(Trial t, String verdict, String why, long now) {
        Decision d = new Decision();
        d.at = now; d.skill = t.skill; d.step = t.step; d.context = t.context; d.from = t.champion; d.to = t.challenger;
        d.verdict = verdict; d.why = bounded(why, 220);
        state.decisions.add(d);
        while (state.decisions.size() > DECISIONS) state.decisions.remove(0);
        return d;
    }

    private Trial trial(String skill, String step, String ctx) {
        for (Trial t : state.trials) if (t.skill.equals(skill) && t.step.equals(step) && t.context.equals(ctx)) return t;
        return null;
    }

    // ───────────── resultados que sólo Bryan puede verificar ─────────────

    private static final class Pending {
        final Plan plan; final Boolean provisional; final String verifier, evidence; final long at;
        Pending(Plan plan, Boolean provisional, String verifier, String evidence, long at) {
            this.plan = plan; this.provisional = provisional; this.verifier = verifier; this.evidence = evidence; this.at = at;
        }
    }

    private final List<Pending> pending = new ArrayList<>();

    /**
     * Deja un resultado pendiente de la reacción de Bryan. {@code provisional}: lo que dice otro
     * verificador (p. ej. su visión) si Bryan no opina; null = sin evidencia, no se aprende nada.
     */
    public synchronized void await(Plan plan, Boolean provisional, String verifier, String evidence) {
        if (plan == null) return;
        pending.removeIf(p -> p.plan.skill.equals(plan.skill));
        pending.add(new Pending(plan, provisional, verifier, evidence, clock.getAsLong()));
    }

    public enum Feedback { NONE, PRAISE, CORRECTION }

    private static final Pattern PRAISE = Pattern.compile(
            "\\b(me gusta|me encanta|que bonito|que bonita|precioso|preciosa|perfecto|genial|muy bien|asi si|eso es|exacto|"
            + "me sirve|esta bien|bien hecho|chulo|chula|guay|increible|buenisimo|gracias)\\b");

    /** Lo que dice Bryan sobre lo último que ella hizo. */
    public static Feedback feedback(String input) {
        if (LessonBook.isCorrection(input)) return Feedback.CORRECTION;
        String t = normalize(input);
        if (t.matches("^(parece|eso parece) (un|una) .*") || t.matches(".*\\b(no se parece|no parece|esta feo|esta fea|que feo|que fea|mal hecho|horrible)\\b.*"))
            return Feedback.CORRECTION;
        return PRAISE.matcher(t).find() ? Feedback.PRAISE : Feedback.NONE;
    }

    /**
     * Resuelve lo pendiente con lo que acaba de decir Bryan. @return las decisiones del laboratorio que
     * provocó (para registrarlas y contarlas).
     */
    public synchronized List<Decision> observe(String input) {
        long now = clock.getAsLong();
        Feedback f = feedback(input);
        List<Decision> out = new ArrayList<>();
        for (Pending p : new ArrayList<>(pending)) {
            if (now - p.at > PENDING_MS) continue; // demasiado tarde: ya no se sabe a qué reacciona
            Boolean ok = f == Feedback.CORRECTION ? Boolean.FALSE : f == Feedback.PRAISE ? Boolean.TRUE : p.provisional;
            if (ok == null) continue; // sin evidencia, no aprende
            String verifier = f == Feedback.NONE ? p.verifier : "bryan";
            Decision d = record(p.plan, ok, null, verifier, f == Feedback.NONE ? p.evidence : "Bryan: «" + bounded(input, 90) + "»");
            if (d != null) out.add(d);
        }
        pending.clear();
        return out;
    }

    // ───────────── lo que sabe de sí misma ─────────────

    private static final Pattern ASKS = Pattern.compile(
            "\\b(que has aprendido|has aprendido|en que (fallas|te equivocas)|que se te da (bien|mal)|tus habilidades|"
            + "tus experimentos|tu laboratorio|como (vas|estas) aprendiendo|que estrategias|confias en ti|que tal se te da|"
            + "has mejorado|en que has mejorado|tus fallos|tus errores)\\b");

    public static boolean asksAbout(String input) { return ASKS.matcher(normalize(input)).find(); }

    /** Confianza baja antes de actuar: para decirlo con honestidad, no para preguntar más. */
    public synchronized String confidenceNote(Plan plan) {
        if (plan == null || !plan.lowConfidence()) return "";
        Skill s = skills.get(plan.skill);
        return " | CONFIANZA (mi historial, datos): al " + (s == null ? plan.skill : s.label) + where(plan.context)
                + " acierto en torno al " + Math.round(plan.predicted * 100) + "% (últimos " + plan.evidence + " intentos).";
    }

    /** Resumen de sus habilidades, experimentos y calibración, con hechos. */
    public synchronized String render(int maxChars) {
        StringBuilder b = new StringBuilder();
        for (Skill s : skills.values()) {
            Stat all = state.stats.get(key(s.name, "*", "*", ""));
            if (all == null || all.n == 0) continue;
            b.append("\n- ").append(s.label).append(": ").append(all.ok).append(" de ").append(all.n).append(" bien");
            if (all.recent.length() >= 3) b.append(" (últimamente ").append(count(all.recent)).append('/').append(all.recent.length()).append(')');
            for (Step st : s.steps) {
                String v = champion(s, st, "");
                if (!v.equals(st.variants.get(0))) b.append("; ").append(st.about).append(": ahora ").append(describe(v));
            }
            for (Map.Entry<String, String> e : state.champions.entrySet())
                if (e.getKey().startsWith(s.name + "|") && !e.getKey().endsWith("|"))
                    b.append("; ").append(where(e.getKey().substring(e.getKey().lastIndexOf('|') + 1)).trim())
                            .append(" uso ").append(describe(e.getValue()));
            float[] br = state.brier.get(s.name);
            if (br != null && br[1] >= 5) b.append("; mi confianza ").append(br[0] / br[1] <= 0.2f ? "acierta bastante"
                    : br[0] / br[1] <= 0.3f ? "acierta a medias" : "aún no es fiable").append(" (Brier ").append(String.format(Locale.ROOT, "%.2f", br[0] / br[1])).append(')');
        }
        for (Trial t : state.trials)
            b.append("\n- experimento en curso: ").append(describe(t.challenger)).append(" en vez de ").append(describe(t.champion))
                    .append(where(t.context)).append(" (").append(t.ok).append('/').append(t.n).append(')');
        for (int i = state.decisions.size() - 1, shown = 0; i >= 0 && shown < 3; i--) {
            Decision d = state.decisions.get(i);
            if ("experimento".equals(d.verdict)) continue;
            b.append("\n- ").append(d.verdict).append(": ").append(d.why);
            shown++;
        }
        String out = b.toString();
        if (out.isEmpty()) return "";
        return out.length() <= maxChars ? out : out.substring(0, maxChars - 1) + "…";
    }

    public synchronized List<Decision> decisions() { return Collections.unmodifiableList(new ArrayList<>(state.decisions)); }
    public synchronized List<Outcome> history() { return Collections.unmodifiableList(new ArrayList<>(state.history)); }
    public synchronized List<Trial> trials() { return Collections.unmodifiableList(new ArrayList<>(state.trials)); }
    public synchronized Stat stat(String skill, String step, String variant, String context) {
        return state.stats.get(key(skill, step == null ? "*" : step, variant == null ? "*" : variant, context(context)));
    }

    // ───────────── utilidades ─────────────

    /** Contexto grueso («animal», «objeto», «tarea»): sin espacios ni símbolos. */
    public static String context(String raw) {
        String t = normalize(raw);
        if (t.isEmpty()) return "";
        String first = t.split(" ")[0];
        return first.length() > 24 ? first.substring(0, 24) : first;
    }

    private static String where(String ctx) { return ctx == null || ctx.isEmpty() ? "" : " con «" + ctx + "»"; }

    static String key(String skill, String step, String variant, String ctx) {
        return skill + "|" + step + "|" + variant + "|" + (ctx == null ? "" : ctx);
    }

    private static int count(String recent) { int c = 0; for (char ch : recent.toCharArray()) if (ch == '1') c++; return c; }

    private static String day(long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        return c.get(Calendar.YEAR) + "-" + c.get(Calendar.DAY_OF_YEAR);
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
}
