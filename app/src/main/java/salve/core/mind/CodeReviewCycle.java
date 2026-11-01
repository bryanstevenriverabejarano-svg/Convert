package salve.core.mind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * Revisión nocturna de su propio código. Primero lo que trae un parche nuevo (solo las líneas que
 * cambiaron, junto a lo que había y a su nota anterior); después, poco a poco, lo que aún no ha leído
 * nunca, empezando por memoria, LLM y autonomía. Cada respuesta del modelo es una nota sobre líneas
 * concretas: nunca se marca como revisado lo que no llegó a leer. Lógica pura: el modelo, el texto y
 * el reloj los pone quien la llama.
 */
public final class CodeReviewCycle {
    public interface Model { String generate(String prompt); } // null = el modelo no respondió

    static final int CHUNK_CHARS = 3000, DIFF_CHARS = 3000, OLD_NOTE = 600;
    /** Memoria, LLM y autonomía, dentro de su núcleo (no la animación del avatar que se llama «sleep…»). */
    private static final java.util.regex.Pattern PRIORITY = java.util.regex.Pattern.compile(
            "^salve/(core|work|data)/.*(memor|recuerd|llm|gemma|autonom|goal|/mind/|cognit|sleep|dream|sueno)");

    public enum Kind { PATCH, NEW_FILE, FIRST_READ }

    public static final class Plan {
        public final List<String> changed = new ArrayList<>(), added = new ArrayList<>(), backlog = new ArrayList<>();
        public final Map<String, String> hashes = new LinkedHashMap<>();
        public boolean patchDetected() { return !changed.isEmpty() || !added.isEmpty(); }
        public int pending() { return changed.size() + added.size() + backlog.size(); }
    }

    public static final class Result {
        public final List<String> done = new ArrayList<>();
        public int chunks;
        public boolean modelFailed, outOfTime;
        public int pendingAfter;
        public boolean patchDetected;
        public String describe() {
            if (done.isEmpty() && pendingAfter == 0) return "Mi código ya está revisado; no hay parches nuevos.";
            StringBuilder sb = new StringBuilder();
            sb.append(patchDetected ? "Parche nuevo detectado. " : "")
                    .append("Revisé ").append(done.size()).append(done.size() == 1 ? " archivo" : " archivos")
                    .append(" (").append(chunks).append(" trozos leídos)");
            if (pendingAfter > 0) sb.append("; quedan ").append(pendingAfter).append(" pendientes");
            if (modelFailed) sb.append("; el modelo no respondió y paré sin marcar nada como revisado");
            else if (outOfTime) sb.append("; se acabó el tiempo de esta noche");
            return sb.append('.').toString();
        }
    }

    private CodeReviewCycle() { }

    /** Qué ha cambiado desde lo que ya revisó y en qué orden mirarlo. Actualiza las huellas vistas. */
    public static Plan plan(SelfCode index, Function<String, String> source, CodeReviewStore store) {
        Plan plan = new Plan();
        boolean baseline = store.hasBaseline();
        List<String> priority = new ArrayList<>(), rest = new ArrayList<>();
        for (SelfCode.FileInfo f : index.files()) {
            String text = source.apply(f.path);
            if (text == null) continue;
            String hash = CodeReviewStore.hash(text);
            plan.hashes.put(f.path, hash);
            String reviewed = store.reviewed(f.path), seen = store.seen(f.path);
            if (reviewed != null && reviewed.equals(hash)) { store.setSeen(f.path, hash); continue; }
            if (reviewed != null) plan.changed.add(f.path);
            else if (baseline && seen == null) plan.added.add(f.path);
            else if (PRIORITY.matcher(f.path.toLowerCase(Locale.ROOT)).find()) priority.add(f.path);
            else rest.add(f.path);
            store.setSeen(f.path, hash);
        }
        // Lo que dejó a medias va primero dentro de lo pendiente: no tira el trabajo de anoche.
        for (List<String> group : java.util.Arrays.asList(priority, rest)) {
            for (String p : group) if (store.partialLine(p, plan.hashes.get(p)) > 1) plan.backlog.add(p);
            for (String p : group) if (!plan.backlog.contains(p)) plan.backlog.add(p);
        }
        store.markBaseline();
        return plan;
    }

    /**
     * Trabaja hasta {@code budgetMs} o hasta que {@code stop} lo pida. Guarda el progreso tras cada
     * trozo para que una interrupción (Bryan coge el móvil) no pierda lo leído.
     */
    public static Result run(SelfCode index, Function<String, String> source, CodeReviewStore store,
                             Model model, BooleanSupplier stop, LongSupplier clock, long budgetMs) {
        Result result = new Result();
        Plan plan = plan(index, source, store);
        result.patchDetected = plan.patchDetected();
        store.save();
        long start = clock.getAsLong();
        List<Object[]> queue = new ArrayList<>();
        for (String p : plan.changed) queue.add(new Object[]{p, Kind.PATCH});
        for (String p : plan.added) queue.add(new Object[]{p, Kind.NEW_FILE});
        for (String p : plan.backlog) queue.add(new Object[]{p, Kind.FIRST_READ});
        int index0 = 0;
        for (; index0 < queue.size(); index0++) {
            String path = (String) queue.get(index0)[0];
            Kind kind = (Kind) queue.get(index0)[1];
            String text = source.apply(path);
            String hash = plan.hashes.get(path);
            boolean finished;
            if (kind == Kind.PATCH && store.reviewedSource(path) != null)
                finished = reviewPatch(path, text, hash, store, model, stop, clock, start, budgetMs, result);
            else
                finished = readWhole(path, text, hash, kind, store, model, stop, clock, start, budgetMs, result);
            store.save();
            if (!finished) break;
        }
        int left = 0;
        for (int i = index0; i < queue.size(); i++) left++;
        result.pendingAfter = left;
        store.addSpent(clock.getAsLong() - start);
        store.save();
        return result;
    }

    private static boolean timeLeft(BooleanSupplier stop, LongSupplier clock, long start, long budgetMs, Result r) {
        if (stop.getAsBoolean()) return false;
        if (clock.getAsLong() - start >= budgetMs) { r.outOfTime = true; return false; }
        return true;
    }

    /** Solo lo que cambió, con lo que había antes y lo que ella ya sabía del archivo. */
    private static boolean reviewPatch(String path, String text, String hash, CodeReviewStore store, Model model,
                                       BooleanSupplier stop, LongSupplier clock, long start, long budgetMs, Result r) {
        List<CodeDiff.Hunk> hunks = CodeDiff.diff(store.reviewedSource(path), text, 2);
        String old = store.note(path);
        if (hunks.isEmpty()) { store.complete(path, hash, old == null ? "" : old, text, clock.getAsLong()); return true; }
        // Trozos del diff que caben en su contexto, en orden.
        List<String> pieces = new ArrayList<>();
        List<CodeDiff.Hunk> group = new ArrayList<>();
        for (CodeDiff.Hunk h : hunks) {
            group.add(h);
            if (CodeDiff.render(group, Integer.MAX_VALUE).length() > DIFF_CHARS && group.size() > 1) {
                group.remove(group.size() - 1);
                pieces.add(CodeDiff.render(group, DIFF_CHARS));
                group = new ArrayList<>(); group.add(h);
            }
        }
        if (!group.isEmpty()) pieces.add(CodeDiff.render(group, DIFF_CHARS));
        int added = 0, removed = 0;
        for (CodeDiff.Hunk h : hunks) { added += h.added(); removed += h.removed(); }
        StringBuilder notes = new StringBuilder();
        for (int i = 0; i < pieces.size(); i++) {
            if (!timeLeft(stop, clock, start, budgetMs, r)) return false;
            String answer = model.generate(patchPrompt(path, old, pieces.get(i), i + 1, pieces.size()));
            if (answer == null || answer.trim().isEmpty()) { r.modelFailed = true; return false; }
            r.chunks++;
            if (notes.length() > 0) notes.append('\n');
            notes.append(answer.trim());
        }
        String note = "Último parche (+" + added + " / -" + removed + " líneas): " + notes
                + (old == null || old.isEmpty() ? "" : "\nAntes sabía: " + CodeReviewStore.cut(old, 500));
        store.complete(path, hash, note, text, clock.getAsLong());
        store.addFinding("PARCHE · " + path + " (+" + added + " / -" + removed + ")\n" + notes);
        r.done.add(path);
        return true;
    }

    /** Lectura completa por trozos (archivo nuevo o que nunca leyó), continuando donde lo dejó. */
    private static boolean readWhole(String path, String text, String hash, Kind kind, CodeReviewStore store, Model model,
                                     BooleanSupplier stop, LongSupplier clock, long start, long budgetMs, Result r) {
        String[] lines = CodeDiff.lines(text);
        int line = Math.max(1, store.partialLine(path, hash));
        StringBuilder notes = new StringBuilder(store.partialNotes(path, hash) == null ? "" : store.partialNotes(path, hash));
        while (line <= lines.length) {
            if (!timeLeft(stop, clock, start, budgetMs, r)) return false;
            StringBuilder chunk = new StringBuilder();
            int from = line;
            while (line <= lines.length) {
                String l = String.format(Locale.ROOT, "%5d| %s\n", line, lines[line - 1]);
                if (chunk.length() + l.length() > CHUNK_CHARS && chunk.length() > 0) break;
                chunk.append(l);
                line++;
            }
            String answer = model.generate(readPrompt(path, chunk.toString(), from, line - 1, lines.length));
            if (answer == null || answer.trim().isEmpty()) { r.modelFailed = true; return false; }
            r.chunks++;
            if (notes.length() > 0) notes.append('\n');
            notes.append("[").append(from).append('–').append(line - 1).append("] ").append(answer.trim());
            store.setPartial(path, hash, line, CodeReviewStore.cut(notes.toString(), 6000));
            store.save();
        }
        String note = notes.toString();
        if (note.length() > CodeReviewStore.MAX_NOTE) {
            if (!timeLeft(stop, clock, start, budgetMs, r)) return false;
            String summary = model.generate(summaryPrompt(path, CodeReviewStore.cut(note, 5000)));
            if (summary == null || summary.trim().isEmpty()) { r.modelFailed = true; return false; }
            r.chunks++;
            note = summary.trim();
        }
        store.complete(path, hash, note, text, clock.getAsLong());
        store.addFinding((kind == Kind.NEW_FILE ? "ARCHIVO NUEVO · " : "PRIMERA LECTURA · ") + path + "\n" + note);
        r.done.add(path);
        return true;
    }

    static String patchPrompt(String path, String oldNote, String diff, int part, int parts) {
        return "Eres Salve revisando un parche nuevo de tu propio código fuente (app Android). Archivo: " + path
                + (parts > 1 ? " (parte " + part + " de " + parts + ")" : "") + ".\n"
                + (oldNote == null || oldNote.isEmpty() ? "" : "Lo que ya sabías de este archivo: " + CodeReviewStore.cut(oldNote, OLD_NOTE) + "\n")
                + "CAMBIOS (líneas con - se quitaron, con + son nuevas, el resto es contexto):\n" + diff
                + "\nEscribe en español, máximo 120 palabras:\nCAMBIO: qué se sustituyó por qué.\n"
                + "EFECTO: si mejora, empeora o es neutro, y por qué según estas líneas.\n"
                + "POSIBLES PROBLEMAS: solo los que se vean aquí, o «ninguno visto».\n"
                + "MEJORA: una propuesta concreta, o «ninguna».\nNo inventes nada que no esté en estas líneas.";
    }

    static String readPrompt(String path, String chunk, int from, int to, int total) {
        return "Eres Salve leyendo tu propio código fuente (app Android). Archivo: " + path
                + ", líneas " + from + "–" + to + " de " + total + ":\n" + chunk
                + "\nEscribe en español, máximo 90 palabras:\nQUÉ HACE: estas líneas.\n"
                + "POSIBLES PROBLEMAS: solo los que se vean aquí, o «ninguno visto».\n"
                + "MEJORA: una propuesta concreta, o «ninguna».\nNo inventes nada que no esté en estas líneas.";
    }

    static String summaryPrompt(String path, String notes) {
        return "Eres Salve. Estas son tus notas, trozo a trozo, sobre tu archivo " + path + ":\n" + notes
                + "\nResúmelas en español en máximo 150 palabras: qué hace el archivo, problemas vistos (con su "
                + "rango de líneas) y mejoras propuestas. No añadas nada que no esté en las notas.";
    }

    /** El texto del informe PDF de la noche: lo que revisó, con sus notas tal cual, y lo que falta. */
    public static String report(List<String> findings, int pending, int reviewedTotal, String night) {
        int patches = 0;
        for (String f : findings) if (f.startsWith("PARCHE")) patches++;
        StringBuilder sb = new StringBuilder("INFORME DE SALVE\nRevisión de mi propio código · noche del ").append(night)
                .append("\n\nEstado: notas y propuestas sin implementar ni verificar. Cada nota se basa solo en las líneas que leí.")
                .append("\n\nResumen: ").append(findings.size()).append(findings.size() == 1 ? " archivo revisado" : " archivos revisados");
        if (patches > 0) sb.append(" (").append(patches).append(" con cambios del último parche)");
        sb.append(". Llevo ").append(reviewedTotal).append(" archivos de mi código revisados en total.");
        if (pending > 0) sb.append(" Me quedan ").append(pending).append(" por leer; seguiré otras noches.");
        else if (pending == 0) sb.append(" No me queda nada pendiente: a partir de ahora solo miraré los parches nuevos.");
        for (String f : findings) sb.append("\n\n").append(f);
        return sb.toString();
    }
}
