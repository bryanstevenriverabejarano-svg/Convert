package salve.core.selfpr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import salve.core.evaluation.ImprovementProposals;
import salve.core.evaluation.SleepImprovementCycle;

/**
 * Las cinco fases con las que Salve decide si se cambia a sí misma:
 * Observar → Registrar → Analizar → Proponer → Verificar.
 *
 * No guarda nada propio ni duplica sistemas: juzga lo que ya existe (el examen, la validación del
 * sueño, el linaje de sus propuestas y los veredictos tras instalarlas) y explica cada fase con una
 * frase. Lo usan la pestaña «Evolución» (para enseñarlo) y el sueño (para decidir si actúa sola).
 *
 * Solo puede actuar cuando las tres primeras fases están cumplidas con datos reales y la cuarta no
 * tiene ningún bloqueo. Proponer es escribir el parche (con los límites de {@link SelfPatch}); fusionar
 * lo decide GitHub Actions si pasan todas las pruebas y la compilación de Android; verificar es el
 * examen con la versión instalada ({@link SelfVerification}). Si empeora, se propone revertirlo.
 */
public final class EvolutionCycle {
    public enum State { DONE, WAITING, BLOCKED, IDLE }

    public static final String[] NAMES = {"Observar", "Registrar", "Analizar", "Proponer", "Verificar"};
    /** Un cambio sin veredicto bloquea otros durante este tiempo (CI + publicar + instalar + examen). */
    public static final long VERDICT_WAIT_MS = 3L * 24 * 60 * 60 * 1000;
    /** Intentos sin mejora medible con el mismo problema antes de dejarlo para Bryan. */
    public static final int MAX_TRIES_PER_PROBLEM = 3;
    /** Empeorar en al menos esto (casos del examen) justifica revertir sola; menos puede ser ruido. */
    public static final int REVERT_MARGIN = 2;
    /** Origen de las entradas del linaje que propuso ella sola. */
    public static final String ORIGIN_AUTONOMOUS = "autonomo", ORIGIN_REVERT = "reversion";

    private EvolutionCycle() { }

    public static final class Phase {
        public final int number;
        public final String name, reasoning;
        public final State state;
        Phase(int number, State state, String reasoning) {
            this.number = number; this.name = NAMES[number - 1]; this.state = state; this.reasoning = reasoning;
        }
    }

    /** Cómo se verifica un cambio: con el examen, o comprobando que un error del registro deja de repetirse. */
    public static final String VERIFY_EXAM = "examen", VERIFY_ERRORS = "errores";

    /**
     * Algo que observó fuera del examen: un error que se repite en su registro, un posible problema que vio al
     * leer su propio código de noche, o una mejora que propuso en una investigación o en un informe de sueño.
     * Lo construye {@link EvolutionSignals} con datos guardados; aquí solo se juzga.
     */
    public static final class Signal {
        /** De dónde viene: «Registro de ejecución», «Revisión de mi código», «Investigación», «Sueño». */
        public final String source;
        /** Firma estable (para contar repeticiones y comprobar después si vuelve a pasar). */
        public final String key;
        public final String problem, idea, path, verify, observed;
        public final List<String> evidence;
        public final int count;
        public Signal(String source, String key, String problem, String idea, String path, String verify,
                      String observed, List<String> evidence, int count) {
            this.source = source; this.key = key == null ? "" : key; this.problem = problem; this.idea = idea == null ? "" : idea;
            this.path = path == null ? "" : path; this.verify = verify; this.observed = observed;
            this.evidence = evidence == null ? Collections.<String>emptyList() : evidence; this.count = count;
        }
        JsonObject candidate() {
            JsonObject c = new JsonObject();
            c.addProperty("issue_id", "");
            c.addProperty("source_module_candidate", path);
            c.addProperty("observed_failures", count);
            c.addProperty("problem", problem);
            c.addProperty("idea", idea);
            c.addProperty("fuente", source);
            c.addProperty("verificar", verify);
            c.addProperty("firma", key);
            JsonArray ev = new JsonArray();
            for (String e : evidence) ev.add(e);
            c.add("examples", ev);
            return c;
        }
    }

    public static final class Plan {
        public final List<Phase> phases;
        /** De dónde sale lo que estudia: «Examen», «Registro de ejecución», «Investigación»… */
        public final String source;
        /** El problema en estudio (o «Sin problema medido»). */
        public final String title;
        /** Etiqueta corta: «Lista para proponer», «Verificando», «En estudio», «Sin examen»… */
        public final String status;
        /** Todas las puertas cumplidas: puede escribir y enviar el parche ya. */
        public final boolean ready;
        /** La fase en la que está ahora (1-5). */
        public final int current;
        /** El candidato validado (issue_id, source_module_candidate, observed_failures) o null. */
        public final JsonObject candidate;
        Plan(List<Phase> phases, String source, String title, String status, boolean ready, int current, JsonObject candidate) {
            this.phases = Collections.unmodifiableList(phases); this.source = source; this.title = title; this.status = status;
            this.ready = ready; this.current = current; this.candidate = candidate;
        }
        public Phase phase(int number) { return phases.get(number - 1); }
    }

    /**
     * @param exam           el último examen guardado (o null)
     * @param nightCandidate el candidato que el sueño confirmó contra ese mismo examen (o null)
     * @param anyCandidate   el fallo repetido que se ve en el examen aunque el sueño no lo haya confirmado
     * @param lineage        sus propuestas, de la más antigua a la más reciente
     * @param verdicts       los veredictos guardados tras examinarse con cada cambio
     * @param appUpdatedAt   cuándo se instaló la versión que corre
     * @param token          ¿tiene token de GitHub para enviar propuestas?
     */
    public static Plan decide(JsonObject exam, JsonObject nightCandidate, JsonObject anyCandidate,
                              List<JsonObject> lineage, List<JsonObject> verdicts,
                              long appUpdatedAt, boolean token, long now) {
        return decide(exam, nightCandidate, anyCandidate, lineage, verdicts, appUpdatedAt, token, now, null);
    }

    /**
     * @param signals lo que observó fuera del examen, lo más fuerte primero. El examen manda cuando tiene un
     *                fallo registrado; si no, estudia la primera señal (registro, revisión, investigación, sueño).
     */
    public static Plan decide(JsonObject exam, JsonObject nightCandidate, JsonObject anyCandidate,
                              List<JsonObject> lineage, List<JsonObject> verdicts,
                              long appUpdatedAt, boolean token, long now, List<Signal> signals) {
        if (lineage == null) lineage = Collections.emptyList();
        if (verdicts == null) verdicts = Collections.emptyList();
        List<Phase> phases = new ArrayList<>();
        JsonObject candidate = nightCandidate != null ? nightCandidate : anyCandidate;
        String issueId = candidate == null ? "" : str(candidate, "issue_id");
        String[] what = issueId.isEmpty() ? null : ImprovementProposals.problem(issueId);
        String problem = what != null ? what[0] : issueId.isEmpty() ? "" : issueId;

        // 1. Observar: un examen hecho con la versión que corre, reciente y con fallos.
        long examAt = exam == null ? 0 : num(exam, "at");
        int total = Math.max(0, SelfLineage.totalCases(exam)), failed = Math.max(0, SelfLineage.failedCases(exam));
        boolean observed = false;
        if (exam == null || examAt <= 0)
            phases.add(new Phase(1, State.WAITING, "Aún no me he examinado. Sin examen no sé qué me falla de verdad, "
                    + "así que no cambio nada. Pídeme «hazte el examen»."));
        else if (appUpdatedAt > 0 && examAt < appUpdatedAt)
            phases.add(new Phase(1, State.WAITING, "Mi último examen (" + date(examAt) + ") es de antes de la versión que "
                    + "tengo instalada: tengo que repetirlo para observar el código que corre ahora."));
        else if (now - examAt > SleepImprovementCycle.MAX_EXAM_AGE_MS)
            phases.add(new Phase(1, State.WAITING, "Mi último examen (" + date(examAt) + ") tiene más de dos semanas; "
                    + "lo repito antes de decidir nada."));
        else if (failed == 0)
            phases.add(new Phase(1, State.DONE, "Examen del " + date(examAt) + " con la versión instalada: "
                    + total + " casos y ninguno fallido. No hay nada medido que corregir."));
        else {
            observed = true;
            phases.add(new Phase(1, State.DONE, "Examen del " + date(examAt) + " con la versión instalada: "
                    + failed + " de " + total + " casos fallidos."));
        }

        // 2. Registrar: el fallo se repite y hay casos concretos guardados como prueba.
        boolean registered = false;
        String source = "Examen";
        if (!observed) phases.add(new Phase(2, State.IDLE, "Espera a que la observación encuentre fallos."));
        else if (candidate == null)
            phases.add(new Phase(2, State.WAITING, "Los fallos no se repiten: necesito al menos dos del mismo tipo "
                    + "en una parte de mi código que sepa tocar. Un fallo suelto puede ser azar."));
        else {
            List<String> examples = ImprovementProposals.examples(exam, issueId, 3);
            if (examples.isEmpty())
                phases.add(new Phase(2, State.WAITING, "Veo el fallo «" + problem + "» pero no tengo guardado ningún caso "
                        + "concreto que lo demuestre; no lo uso como prueba."));
            else {
                registered = true;
                phases.add(new Phase(2, State.DONE, "Guardé " + num(candidate, "observed_failures") + " fallos de «" + problem
                        + "». Ejemplo real: " + examples.get(0)));
            }
        }

        // 3. Analizar: el sueño lo confirmó contra el mismo examen y el módulo se puede tocar.
        boolean analyzed = false;
        String path = candidate == null ? "" : str(candidate, "source_module_candidate");
        if (candidate != null) {
            candidate = candidate.deepCopy();
            candidate.addProperty("fuente", "Examen");
            candidate.addProperty("verificar", VERIFY_EXAM);
        }
        if (!registered) phases.add(new Phase(3, State.IDLE, "Espera a tener el fallo registrado."));
        else if (!SelfChangeScope.allowed(path))
            phases.add(new Phase(3, State.BLOCKED, "La causa probable está en " + file(path) + ", que forma parte de lo que "
                    + "me mide o me autoriza. Eso no me lo cambio yo."));
        else if (nightCandidate == null)
            phases.add(new Phase(3, State.WAITING, "Hipótesis: «" + problem + "» viene de " + file(path) + ". Mientras "
                    + "duermo la contrasto con el mismo examen; hasta entonces es una sospecha, no un hecho."));
        else {
            analyzed = true;
            phases.add(new Phase(3, State.DONE, "Hecho: " + num(candidate, "observed_failures") + " fallos de «" + problem
                    + "», confirmados al dormir contra el mismo examen. Hipótesis: la causa está en " + file(path)
                    + (what == null ? "." : ". Idea: " + what[1] + ".")));
        }

        // Fuera del examen: si el examen no tiene nada registrado, estudia lo más fuerte que observó en otra parte.
        if (!registered && signals != null && !signals.isEmpty()) {
            // La primera señal que no haya intentado ya demasiadas veces sin mejora.
            Signal sig = signals.get(0);
            for (Signal s : signals)
                if (failedTries(lineage, verdicts, s.problem) < MAX_TRIES_PER_PROBLEM) { sig = s; break; }
            phases.clear();
            source = sig.source;
            problem = sig.problem;
            path = sig.path;
            what = new String[]{sig.problem, sig.idea};
            candidate = sig.candidate();
            phases.add(new Phase(1, State.DONE, sig.source + ": " + sig.observed));
            registered = !sig.evidence.isEmpty();
            phases.add(new Phase(2, registered ? State.DONE : State.WAITING, registered
                    ? "Guardé " + sig.evidence.size() + (sig.evidence.size() == 1 ? " prueba" : " pruebas") + " de «" + sig.problem
                    + "». " + clip(sig.evidence.get(0), 260)
                    : "Veo «" + sig.problem + "» pero no tengo ninguna prueba concreta guardada; no lo uso."));
            analyzed = false;
            if (!registered) phases.add(new Phase(3, State.IDLE, "Espera a tener pruebas guardadas."));
            else if (path.isEmpty())
                phases.add(new Phase(3, State.WAITING, "No encuentro en mi código dónde nace «" + sig.problem
                        + "». No toco nada a ciegas."));
            else if (!SelfChangeScope.allowed(path))
                phases.add(new Phase(3, State.BLOCKED, "Lo que habría que cambiar está en " + file(path)
                        + ", que forma parte de lo que me mide o me autoriza. Eso no me lo cambio yo."));
            else {
                analyzed = true;
                phases.add(new Phase(3, State.DONE, "Hipótesis: la causa está en " + file(path)
                        + (sig.idea.isEmpty() ? "." : ". Idea: " + sig.idea + ".")
                        + (VERIFY_ERRORS.equals(sig.verify)
                        ? " Lo compruebo después viendo si el mismo error vuelve a mi registro."
                        : " Es una idea, no un fallo medido: la doy por buena solo si mi examen no empeora.")));
            }
        }

        // 4. Proponer: puertas antes de escribir el parche.
        JsonObject last = lineage.isEmpty() ? null : lineage.get(lineage.size() - 1);
        JsonObject lastVerdict = last == null ? null : verdictFor(verdicts, str(last, "issue"));
        String block = null;
        State blockState = State.WAITING;
        if (!analyzed) block = "Espera al análisis.";
        else if (!token) { block = "Sin token de GitHub no puedo enviar propuestas (Ajustes → IA y cámara → PRs de Salve)."; blockState = State.BLOCKED; }
        else if (last != null && lastVerdict == null && now - num(last, "t") < VERDICT_WAIT_MS)
            block = "Mi cambio anterior (" + file(str(last, "archivo")) + ") aún no tiene veredicto. No propongo otro "
                    + "hasta medir ese: así sé qué cambio causó qué.";
        else if (lastVerdict != null && "peor".equals(str(lastVerdict, "resultado")) && !ORIGIN_REVERT.equals(str(last, "origen"))
                && num(lastVerdict, "ahora") - num(lastVerdict, "antes") >= REVERT_MARGIN) {
            block = "Mi último cambio empeoró las cosas. Antes de proponer nada nuevo hay que revertirlo.";
            blockState = State.BLOCKED;
        } else if (failedTries(lineage, verdicts, problem) >= MAX_TRIES_PER_PROBLEM) {
            block = "Ya intenté arreglar «" + problem + "» " + MAX_TRIES_PER_PROBLEM + " veces sin mejora medible. "
                    + "No insisto: necesito otra idea o que lo mire Bryan.";
            blockState = State.BLOCKED;
        }
        boolean ready = analyzed && block == null;
        phases.add(new Phase(4, !analyzed ? State.IDLE : ready ? State.DONE : blockState, ready
                ? "Puedo escribir el parche en " + file(path) + ", del tamaño que haga falta, sin llamadas peligrosas y "
                + "sin tocar mi examen, la puerta de mis PR ni mi identidad. Si mi modelo no escribe uno válido en dos intentos, no envío nada."
                : block));

        // 5. Verificar: GitHub prueba y fusiona; luego me examino con la versión instalada y comparo.
        Phase verify;
        if (last == null) verify = new Phase(5, State.IDLE, "Aún no he propuesto ningún cambio. Cuando lo haga, GitHub "
                + "ejecuta todas mis pruebas y compila Android; solo si pasa se fusiona y se publica. Después me examino con "
                + "la versión nueva y comparo: si empeoro en " + REVERT_MARGIN + " casos o más, me revierto.");
        else if (lastVerdict == null) verify = new Phase(5, State.WAITING, "Cambio en " + file(str(last, "archivo"))
                + " enviado el " + date(num(last, "t")) + ". GitHub lo prueba (tests + compilación de Android) y lo fusiona "
                + "si pasa. " + (VERIFY_ERRORS.equals(str(last, "verificar"))
                ? "Cuando esté instalado miro si el mismo error (" + num(last, "fallos_examen") + " veces antes) vuelve a mi registro."
                : "Cuando esté instalado me examino y comparo con los " + num(last, "fallos_examen") + " fallos de antes."));
        else {
            String result = str(lastVerdict, "resultado");
            verify = new Phase(5, "peor".equals(result) ? State.BLOCKED : State.DONE, SelfVerification.sentence(lastVerdict));
        }
        phases.add(verify);

        int current = 1;
        while (current < 5 && phases.get(current - 1).state == State.DONE) current++;
        if (!ready && verify.state == State.WAITING && last != null && lastVerdict == null) current = 5;
        String status = ready ? "Lista para proponer"
                : current == 5 && verify.state == State.WAITING ? "Verificando"
                : verify.state == State.BLOCKED ? "Revisar cambio"
                : exam == null ? "Sin examen"
                : phases.get(current - 1).state == State.BLOCKED ? "Bloqueada"
                : "En estudio";
        String title = !problem.isEmpty() ? problem : failed == 0 && exam != null ? "Sin fallos medidos" : "Sin problema medido";
        return new Plan(phases, source, title, status, ready, current, ready ? candidate : null);
    }

    /** Las cinco fases con los datos guardados en el teléfono (sin red ni inferencia). */
    public static Plan load(android.content.Context context) {
        android.content.Context app = context.getApplicationContext();
        JsonObject exam = salve.core.evaluation.SleepImprovementRuntime.lastExam(app);
        return decide(exam, salve.core.evaluation.SleepImprovementRuntime.eligibleCandidate(app, exam),
                salve.core.synergy.SynergyEvolution.candidate(exam),
                SelfLineage.read(SelfVerification.lineageFile(app), 50),
                SelfLineage.read(SelfVerification.verdictsFile(app), 50),
                SelfVerification.appUpdatedAt(app), SelfPrTokenStore.token(app) != null, System.currentTimeMillis(),
                EvolutionSignals.collect(app));
    }

    private static final String PREFS = "salve_evolution", AUTONOMOUS = "autonomous";

    /** ¿Puede proponer (y revertir) sola mientras duerme? Encendido salvo que Bryan lo apague. */
    public static boolean autonomous(android.content.Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .getBoolean(AUTONOMOUS, true);
    }

    public static void setAutonomous(android.content.Context context, boolean on) {
        context.getApplicationContext().getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean(AUTONOMOUS, on).apply();
    }

    /** ¿Debe revertir sola el último cambio? Solo si empeoró con margen y aún no se ha revertido. */
    public static JsonObject revertTarget(List<JsonObject> lineage, List<JsonObject> verdicts) {
        if (lineage == null || lineage.isEmpty()) return null;
        JsonObject last = lineage.get(lineage.size() - 1);
        if (ORIGIN_REVERT.equals(str(last, "origen")) || !last.has("cambios")) return null;
        JsonObject v = verdictFor(verdicts, str(last, "issue"));
        if (v == null || !"peor".equals(str(v, "resultado"))) return null;
        return num(v, "ahora") - num(v, "antes") >= REVERT_MARGIN ? last : null;
    }

    /**
     * Las ediciones que deshacen un cambio guardado en el linaje (cada «poner» vuelve a ser «buscar»).
     * Vacío si no se puede deshacer entero (p. ej. una edición que solo borró líneas).
     */
    public static List<SelfPatch.Edit> inverse(JsonObject entry) {
        List<SelfPatch.Edit> out = new ArrayList<>();
        if (entry == null || !entry.has("cambios") || !entry.get("cambios").isJsonArray()) return out;
        JsonArray changes = entry.getAsJsonArray("cambios");
        for (int i = changes.size() - 1; i >= 0; i--) {
            JsonElement e = changes.get(i);
            if (!e.isJsonObject()) continue;
            String search = str(e.getAsJsonObject(), "buscar"), replace = str(e.getAsJsonObject(), "poner");
            // Una edición que solo borró no deja dónde volver a ponerlo: mejor no revertir a medias.
            if (replace.trim().isEmpty()) return new ArrayList<>();
            out.add(new SelfPatch.Edit(replace, search));
        }
        return out;
    }

    /** Las ediciones de un parche, para guardarlas en el linaje y poder deshacerlas. */
    public static JsonArray record(List<SelfPatch.Edit> edits) {
        JsonArray out = new JsonArray();
        if (edits != null) for (SelfPatch.Edit e : edits) {
            JsonObject o = new JsonObject();
            o.addProperty("buscar", e.search);
            o.addProperty("poner", e.replace);
            out.add(o);
        }
        return out;
    }

    // ───────── apoyo ─────────

    public static JsonObject verdictFor(List<JsonObject> verdicts, String issue) {
        if (issue == null || issue.isEmpty() || verdicts == null) return null;
        for (int i = verdicts.size() - 1; i >= 0; i--) if (issue.equals(str(verdicts.get(i), "issue"))) return verdicts.get(i);
        return null;
    }

    static int failedTries(List<JsonObject> lineage, List<JsonObject> verdicts, String problem) {
        int tries = 0;
        if (problem.isEmpty()) return 0;
        for (JsonObject e : lineage) {
            if (!problem.equals(str(e, "objetivo"))) continue;
            JsonObject v = verdictFor(verdicts, str(e, "issue"));
            if (v != null && !"mejor".equals(str(v, "resultado"))) tries++;
        }
        return tries;
    }

    static String clip(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    static String file(String path) {
        return path == null || path.isEmpty() ? "mi código" : path.substring(path.lastIndexOf('/') + 1);
    }

    static String date(long t) {
        return new SimpleDateFormat("d MMM, HH:mm", new Locale("es", "ES")).format(new Date(t));
    }

    static String str(JsonObject o, String k) {
        if (o == null || !o.has(k) || !o.get(k).isJsonPrimitive()) return "";
        return o.get(k).getAsString();
    }

    static long num(JsonObject o, String k) {
        try { return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsLong() : 0; }
        catch (RuntimeException notNumber) { return 0; }
    }
}
