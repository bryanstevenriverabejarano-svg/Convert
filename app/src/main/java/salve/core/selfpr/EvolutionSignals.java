package salve.core.selfpr;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.trace.ExecutionJournal;

/**
 * Lo que Salve observa de sí misma fuera del examen, para que también evolucione con ello:
 * <ul>
 *   <li><b>Registro de ejecución</b>: una tarea que termina en error varias veces con la misma causa
 *       desde que se instaló la versión que corre (p. ej. la voz).</li>
 *   <li><b>Revisión de mi código</b>: los «POSIBLES PROBLEMAS» y la «MEJORA» que apunta al leer su
 *       propio código de noche ({@code CodeReviewStore}).</li>
 *   <li><b>Investigación</b> y <b>Sueño</b>: la línea «MEJORA PARA MI CÓDIGO» de sus informes.</li>
 * </ul>
 * No guarda nada propio: lee el registro, la revisión y la biblioteca de informes que ya existen. Las partes
 * puras (sin Android) se prueban en la JVM; {@link #collect} solo junta los datos del teléfono.
 */
public final class EvolutionSignals {
    public static final String JOURNAL = "Registro de ejecución", REVIEW = "Revisión de mi código",
            RESEARCH = "Investigación", SLEEP = "Sueño";
    /** Un error repetido necesita al menos esto para no ser azar. */
    public static final int MIN_REPEATS = 2;
    /** Para dar el veredicto de un arreglo de error: veces que esa tarea ha corrido con la versión nueva. */
    public static final int RUNS_TO_JUDGE = 3;
    static final long IDEA_MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000;
    static final String[] FAILED = {"ERROR", "FALLO", "INTERRUMPIDA_AL_CERRAR"};

    private EvolutionSignals() { }

    // ───────── registro de ejecución ─────────

    /** La primera línea de error de una tarea fallida, o "" si la tarea no falló. */
    static String errorOf(ExecutionJournal.Task task) {
        if (task == null || task.status == null || !failed(task)) return "";
        String text = "";
        if (task.events != null) for (ExecutionJournal.Event e : task.events) {
            if (e == null || e.text == null) continue;
            if (e.kind != null && e.kind.toUpperCase(Locale.ROOT).contains("ERROR")) { text = e.text; break; }
            text = e.text;
        }
        if (text.trim().isEmpty()) text = task.status;
        String first = text.trim().split("\n", 2)[0].trim();
        return first.length() > 140 ? first.substring(0, 140) : first;
    }

    static boolean failed(ExecutionJournal.Task task) {
        for (String f : FAILED) if (f.equals(task.status)) return true;
        return "CIERRE INESPERADO".equals(task.type);
    }

    /** Firma estable de un error: tipo de tarea + causa sin números (horas, ids y tamaños cambian). */
    public static String signature(String type, String error) {
        String clean = error == null ? "" : error.toLowerCase(Locale.ROOT).replaceAll("[0-9]+", "#")
                .replaceAll("\\s+", " ").trim();
        if (clean.length() > 90) clean = clean.substring(0, 90);
        return (type == null ? "" : type.trim()) + " | " + clean;
    }

    private static final Pattern FRAME = Pattern.compile("at (salve\\.[\\w.$]+)\\.[\\w$<>]+\\((\\w+\\.(?:java|kt)):\\d+\\)");

    /** Archivo de su código donde ocurrió, sacado de la traza (p. ej. {@code at salve.services.NeuralTtsService.speak(NeuralTtsService.kt:164)}). */
    public static String fileFromTrace(String text) {
        if (text == null) return "";
        Matcher m = FRAME.matcher(text);
        while (m.find()) {
            String cls = m.group(1);
            int dollar = cls.indexOf('$');
            if (dollar > 0) cls = cls.substring(0, dollar);
            int dot = cls.lastIndexOf('.');
            if (dot < 0) continue;
            String path = "app/src/main/java/" + cls.substring(0, dot).replace('.', '/') + "/" + m.group(2);
            if (SelfChangeScope.allowed(path)) return path;
        }
        return "";
    }

    /** Errores que se repiten con la misma causa desde que se instaló la versión que corre. */
    public static List<EvolutionCycle.Signal> fromJournal(List<ExecutionJournal.Task> tasks, long installedAt,
                                                          java.util.function.Function<String, String> locate) {
        Map<String, List<ExecutionJournal.Task>> groups = new LinkedHashMap<>();
        if (tasks != null) for (ExecutionJournal.Task t : tasks) {
            if (t == null || t.started < installedAt) continue;
            String error = errorOf(t);
            if (error.isEmpty()) continue;
            groups.computeIfAbsent(signature(t.type, error), k -> new ArrayList<>()).add(t);
        }
        List<EvolutionCycle.Signal> out = new ArrayList<>();
        for (Map.Entry<String, List<ExecutionJournal.Task>> g : groups.entrySet()) {
            List<ExecutionJournal.Task> same = g.getValue();
            if (same.size() < MIN_REPEATS) continue;
            ExecutionJournal.Task first = same.get(0);
            String error = errorOf(first);
            List<String> evidence = new ArrayList<>();
            String path = "";
            for (ExecutionJournal.Task t : same) {
                if (evidence.size() < 3) evidence.add(ExecutionJournal.date(t.started) + " · " + t.type + ": " + clip(errorOf(t), 200));
                if (path.isEmpty() && t.events != null)
                    for (ExecutionJournal.Event e : t.events) { path = fileFromTrace(e.text); if (!path.isEmpty()) break; }
            }
            if (path.isEmpty() && locate != null) {
                String found = locate.apply(first.type + " " + error);
                path = found == null ? "" : found;
            }
            out.add(new EvolutionCycle.Signal(JOURNAL, g.getKey(), "«" + first.type + "» falla: " + clip(error, 120),
                    "que «" + first.type + "» no vuelva a fallar por esta causa, sin quitar nada de lo que hace",
                    path, EvolutionCycle.VERIFY_ERRORS,
                    "desde que instalé esta versión, «" + first.type + "» terminó en error " + same.size()
                            + " veces con la misma causa.", evidence, same.size()));
        }
        out.sort((a, b) -> Integer.compare(b.count, a.count));
        return out;
    }

    /**
     * Veredicto de un arreglo de error: con la versión instalada, ¿vuelve a aparecer la misma firma?
     * Null si esa tarea aún no ha corrido {@link #RUNS_TO_JUDGE} veces desde que se instaló.
     */
    public static JsonObject errorVerdict(JsonObject change, List<ExecutionJournal.Task> tasks, long installedAt, long now) {
        if (change == null || !EvolutionCycle.VERIFY_ERRORS.equals(EvolutionCycle.str(change, "verificar"))) return null;
        String key = EvolutionCycle.str(change, "firma");
        String type = key.contains(" | ") ? key.substring(0, key.indexOf(" | ")) : "";
        if (type.isEmpty() || tasks == null) return null;
        long since = Math.max(installedAt, EvolutionCycle.num(change, "t"));
        int runs = 0, again = 0;
        for (ExecutionJournal.Task t : tasks) {
            if (t == null || t.started < since || !type.equals(t.type) || "EN_CURSO".equals(t.status)) continue;
            runs++;
            String error = errorOf(t);
            if (!error.isEmpty() && key.equals(signature(t.type, error))) again++;
        }
        if (runs < RUNS_TO_JUDGE) return null;
        long before = Math.max(0, EvolutionCycle.num(change, "fallos_examen"));
        JsonObject v = new JsonObject();
        v.addProperty("t", now);
        v.addProperty("issue", EvolutionCycle.str(change, "issue"));
        v.addProperty("archivo", EvolutionCycle.str(change, "archivo"));
        v.addProperty("origen", EvolutionCycle.str(change, "origen"));
        v.addProperty("objetivo", EvolutionCycle.str(change, "objetivo"));
        v.addProperty("revision", EvolutionCycle.str(change, "revision"));
        v.addProperty("verificar", EvolutionCycle.VERIFY_ERRORS);
        v.addProperty("antes", before);
        v.addProperty("ahora", again);
        v.addProperty("resultado", again == 0 ? "mejor" : again >= before ? "peor" : "igual");
        return v;
    }

    // ───────── revisión nocturna de su código ─────────

    private static final Pattern SECTION = Pattern.compile("(?is)(POSIBLES PROBLEMAS|MEJORA)\\s*:\\s*(.+?)(?=\\n\\s*[A-ZÁÉÍÓÚ ]{4,}\\s*:|$)");

    /** {problemas, mejora} de una nota de revisión; "" donde dice «ninguno»/«ninguna». */
    public static String[] reviewParts(String note) {
        String problems = "", improvement = "";
        if (note != null) {
            Matcher m = SECTION.matcher(note);
            while (m.find()) {
                String value = m.group(2).trim().replaceAll("\\s+", " ");
                if (value.toLowerCase(Locale.ROOT).matches("[«\"']?ningun[oa]( visto)?[»\"'.]*.*") && value.length() < 40) value = "";
                if (m.group(1).toUpperCase(Locale.ROOT).startsWith("POSIBLES")) { if (problems.isEmpty()) problems = value; }
                else if (improvement.isEmpty()) improvement = value;
            }
        }
        return new String[]{problems, improvement};
    }

    /** Lo que vio al leer su propio código: un archivo con un posible problema (y su mejora, si la apuntó). */
    public static List<EvolutionCycle.Signal> fromReview(List<String[]> notes) {
        List<EvolutionCycle.Signal> out = new ArrayList<>();
        if (notes != null) for (String[] n : notes) {
            String[] parts = reviewParts(n[1]);
            if (parts[0].isEmpty()) continue;
            String path = n[0].startsWith("app/") ? n[0] : "app/src/main/java/" + n[0];
            String file = EvolutionCycle.file(path);
            out.add(new EvolutionCycle.Signal(REVIEW, "revision | " + path, "Posible problema en " + file + ": " + clip(parts[0], 120),
                    parts[1].isEmpty() ? "corregir ese problema con el cambio que haga falta" : clip(parts[1], 200), path,
                    EvolutionCycle.VERIFY_EXAM, "al leer " + file + " de noche apunté un posible problema.",
                    Collections.singletonList("Mi nota de " + file + ": " + clip(n[1], 400)), 1));
        }
        return out;
    }

    // ───────── investigaciones y sueños ─────────

    /** Línea que el informe autónomo pide al modelo al final (ver {@code AutonomousReportWriter}). */
    public static final String IMPROVEMENT_LINE = "MEJORA PARA MI CÓDIGO:";

    /** La mejora concreta que un informe propone para su código, o "" si no propone ninguna. */
    public static String improvementOf(String body) {
        if (body == null) return "";
        String found = "";
        for (String line : body.split("\n")) {
            String l = line.trim().replaceFirst("^[*#\\-\\s]+", "");
            if (l.toUpperCase(Locale.ROOT).startsWith(IMPROVEMENT_LINE)) { found = l.substring(IMPROVEMENT_LINE.length()).trim(); break; }
        }
        found = found.replaceAll("[*_`]", "").trim();
        if (found.toLowerCase(Locale.ROOT).matches("[«\"']?(ninguna|ninguno|no hay|nada)\\b.*") && found.length() < 60) return "";
        return found.length() > 240 ? found.substring(0, 240) : found;
    }

    /**
     * Una mejora propuesta en un informe de investigación o de sueño.
     * @param reports {fuente, título, mejora, cuándo} de cada informe con mejora, el más reciente primero
     */
    public static List<EvolutionCycle.Signal> fromReports(List<String[]> reports, long now,
                                                          java.util.function.Function<String, String> locate) {
        List<EvolutionCycle.Signal> out = new ArrayList<>();
        if (reports != null) for (String[] r : reports) {
            long at;
            try { at = Long.parseLong(r[3]); } catch (RuntimeException bad) { continue; }
            if (r[2] == null || r[2].isEmpty() || now - at > IDEA_MAX_AGE_MS) continue;
            String found = locate == null ? null : locate.apply(r[2]);
            out.add(new EvolutionCycle.Signal(r[0], "informe | " + r[2], clip(r[2], 140), r[2], found == null ? "" : found,
                    EvolutionCycle.VERIFY_EXAM, "en mi informe «" + clip(r[1], 70) + "» propuse una mejora para mi código.",
                    Collections.singletonList("«" + clip(r[1], 80) + "»: " + clip(r[2], 240)), 1));
        }
        return out;
    }

    /** Todas, en orden de fuerza: errores repetidos, revisión de su código, investigaciones y sueños. */
    public static List<EvolutionCycle.Signal> rank(List<EvolutionCycle.Signal> journal, List<EvolutionCycle.Signal> review,
                                                   List<EvolutionCycle.Signal> reports) {
        List<EvolutionCycle.Signal> out = new ArrayList<>();
        if (journal != null) out.addAll(journal);
        if (review != null) out.addAll(review);
        if (reports != null) out.addAll(reports);
        return out;
    }

    // ───────── teléfono ─────────

    /** Lee el registro, la revisión de código y los informes guardados (sin red ni inferencia). */
    public static List<EvolutionCycle.Signal> collect(android.content.Context context) {
        android.content.Context app = context.getApplicationContext();
        long installed = SelfVerification.appUpdatedAt(app), now = System.currentTimeMillis();
        java.util.function.Function<String, String> locate = goal -> {
            try { return SelfChangeScope.choose(salve.core.mind.SelfCodeLoader.get(app), goal); }
            catch (RuntimeException | LinkageError unavailable) { return null; }
        };
        List<EvolutionCycle.Signal> journal = Collections.emptyList(), review = Collections.emptyList(),
                reports = Collections.emptyList();
        try { journal = fromJournal(salve.core.trace.ExecutionTrace.journal(app).recent(), installed, locate); }
        catch (RuntimeException unreadable) { /* sin registro */ }
        try {
            review = fromReview(new salve.core.mind.CodeReviewStore(
                    new java.io.File(app.getFilesDir(), "self-code-review")).reviewedNotes());
        } catch (RuntimeException unreadable) { /* sin revisión */ }
        try {
            List<String[]> found = new ArrayList<>();
            for (salve.core.reports.ReportLibrary.Entry e : salve.core.reports.ReportLibrary.list(
                    new java.io.File(app.getFilesDir(), salve.core.reports.AutonomousReportWriter.DIRECTORY), null)) {
                if (e.improvement.isEmpty() || (e.category != salve.core.reports.ReportLibrary.Category.RESEARCH
                        && e.category != salve.core.reports.ReportLibrary.Category.SLEEP)) continue;
                String source = e.category == salve.core.reports.ReportLibrary.Category.SLEEP ? SLEEP : RESEARCH;
                found.add(new String[]{source, e.title, e.improvement, String.valueOf(e.createdAt)});
            }
            reports = fromReports(found, now, locate);
        } catch (RuntimeException unreadable) { /* sin informes */ }
        return rank(journal, review, reports);
    }

    static String clip(String s, int max) { return EvolutionCycle.clip(s, max); }
}
