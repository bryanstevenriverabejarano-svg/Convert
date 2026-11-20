package salve.core.vida;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializa leer → interpretar → guardar del módulo Vida (mismo contrato que
 * {@code PersonalBudgetService}): una respuesta de éxito solo se emite tras guardar.
 *
 * <p>Copia en la nube: antes del primer cambio se fusiona la copia remota con la local
 * (una ausencia local no prueba que los datos se borraran). Solo después de esa comprobación
 * se sube el registro; si la nube no responde, nada se sube y se reintenta en el siguiente turno.
 */
public final class VidaService {

    /** Copia remota. Implementación en {@code CloudSyncManager} (pCloud). */
    public interface Cloud {
        /** Texto remoto, {@code null} si no hay copia; IOException si no se pudo comprobar. */
        String fetch() throws IOException;
        /** Sube el registro completo; asíncrono y en orden. */
        void push(String content);
    }

    public static final int MAX_CLOUD_BYTES = VidaStore.MAX_BYTES;

    private final Object lock = new Object();
    private final VidaStore store;
    private final VidaConversation conversation;
    private final Cloud cloud;
    /** Existe cuando la copia remota ya se fusionó: a partir de ahí se puede subir. */
    private final File cloudChecked;
    private volatile VidaLog cached;

    public VidaService(File file) { this(file, Clock.systemDefaultZone(), null); }
    public VidaService(File file, Clock clock) { this(file, clock, null); }
    public VidaService(File file, Clock clock, Cloud cloud) {
        store = new VidaStore(file);
        conversation = new VidaConversation(clock);
        this.cloud = cloud;
        cloudChecked = new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".nube");
    }

    public boolean handles(String input) {
        return isCloudCommand(input) || VidaConversation.handles(input, query -> VidaConversation.knows(snapshot(), query));
    }

    public String respond(String input) {
        synchronized (lock) {
            if (isCloudCommand(input)) return recoverFromCloud();
            VidaLog log;
            try { log = load(); }
            catch (IOException unreadable) {
                if (VidaConversation.normalize(input).matches("(?:borra|olvida|elimina) (?:todos )?mis datos de vida")) {
                    try { store.clear(); cached = new VidaLog(); push(cached); return "He borrado tus datos de Vida de este móvil y de la nube."; }
                    catch (IOException failed) { return "No pude borrar los datos de Vida. No se ha eliminado nada."; }
                }
                return "No puedo leer tu registro de Vida y lo he conservado sin reemplazarlo. Di «borra mis datos de vida» si quieres empezar de cero.";
            }
            try {
                VidaConversation.Response r = conversation.handle(input, log);
                if (r.clearAll) {
                    store.clear(); cached = new VidaLog();
                    // La copia vacía hace de lápida: otra instalación no resucita lo borrado.
                    boolean remote = push(cached);
                    return r.reply + (remote ? " También se vacía la copia de la nube." : "");
                }
                if (r.next != null) { store.save(r.next); cached = r.next; push(r.next); }
                else cached = log;
                return r.reply;
            } catch (IOException saveFailed) {
                return "No pude guardar la anotación. Tu registro anterior se conserva; vuelve a intentarlo.";
            } catch (IllegalArgumentException invalid) {
                return "No he guardado eso: faltan datos. Di «módulo vida» para ver ejemplos.";
            }
        }
    }

    /** Fusiona un CSV de Hevy sin duplicar series ya importadas. */
    public String importHevy(String csv) {
        HevyCsvImporter.Result parsed;
        try { parsed = HevyCsvImporter.parse(csv); }
        catch (IllegalArgumentException invalid) { return invalid.getMessage(); }
        synchronized (lock) {
            VidaLog log;
            try { log = load(); }
            catch (IOException unreadable) { return "No puedo leer tu registro de Vida; no he importado nada para no reemplazarlo."; }
            Set<String> keys = new HashSet<>();
            for (VidaLog.GymSet s : log.sets) keys.add(s.key);
            List<VidaLog.GymSet> next = new ArrayList<>(log.sets);
            Set<String> sessions = new HashSet<>();
            int added = 0;
            for (VidaLog.GymSet s : parsed.sets) {
                if (!keys.add(s.key)) continue;
                next.add(s); added++; sessions.add(s.date + "|" + s.workout);
            }
            if (added == 0) return "Hevy: no hay series nuevas (" + parsed.sets.size() + " ya estaban importadas).";
            VidaLog updated = log.withSets(next);
            try { store.save(updated); cached = updated; }
            catch (IOException failed) { return "No pude guardar la importación de Hevy. Tu registro anterior se conserva."; }
            boolean remote = push(updated);
            return "Importado de Hevy: " + added + " series de " + sessions.size() + " entrenamientos"
                    + (parsed.skipped > 0 ? " (" + parsed.skipped + " filas sin fecha o ejercicio, ignoradas)" : "")
                    + (remote ? "; copia en la nube en camino" : "")
                    + ". Pregúntame «resumen semanal» o «historial de» un ejercicio.";
        }
    }

    /** Registro actual para la pantalla del gym (fusiona la nube la primera vez). Fuera del hilo de la pantalla. */
    public VidaLog read() throws IOException {
        synchronized (lock) { VidaLog log = load(); cached = log; return log; }
    }

    /**
     * Guarda un entreno terminado en la pantalla del gym. Calcula los récords contra el
     * historial anterior al entreno y solo devuelve el resumen después de guardar.
     */
    public GymTracker.Summary saveWorkout(List<VidaLog.GymSet> done) throws IOException {
        synchronized (lock) {
            VidaLog log = load();
            GymTracker.Summary summary = GymTracker.summarize(log, done);
            Set<String> keys = new HashSet<>();
            for (VidaLog.GymSet s : log.sets) keys.add(s.key);
            List<VidaLog.GymSet> next = new ArrayList<>(log.sets);
            for (VidaLog.GymSet s : done) if (keys.add(s.key)) next.add(s);
            VidaLog updated = log.withSets(next);
            store.save(updated); cached = updated; push(updated);
            return summary;
        }
    }

    /** Cambio hecho desde las pantallas de Vida: leer → aplicar → guardar → subir, con el mismo candado. */
    public VidaLog update(java.util.function.UnaryOperator<VidaLog> change) throws IOException {
        synchronized (lock) {
            VidaLog current = load();
            VidaLog next = change.apply(current);
            if (next == current) { cached = current; return current; } // sin cambios: ni disco ni nube
            store.save(next); cached = next; push(next);
            return next;
        }
    }

    // ---------------------------------------------------------------- nube

    private static boolean isCloudCommand(String input) {
        return VidaConversation.normalize(input).matches(
                "(?:recupera|restaura|descarga|sincroniza)(?: mis datos de| mi registro de| el modulo)? vida(?: de la nube| desde la nube)?");
    }

    private String recoverFromCloud() {
        if (cloud == null) return "La copia en la nube no está disponible en esta versión.";
        try {
            VidaLog local = store.load();
            String remote = cloud.fetch();
            VidaLog merged = remote == null ? local : merge(local, VidaLog.parse(remote));
            store.save(merged); cached = merged;
            markChecked();
            push(merged);
            return remote == null
                    ? "No había copia de Vida en la nube; acabo de subir la de este móvil."
                    : "Vida sincronizada con la nube: " + merged.sets.size() + " series, " + merged.sleep.size()
                    + " noches, " + merged.meals.size() + " comidas y " + merged.blocks.size() + " bloques de horario.";
        } catch (IOException unavailable) {
            return "No pude conectar con la nube (" + unavailable.getMessage() + "). Revisa IA y cámara → Nube pCloud; tus datos del móvil se conservan.";
        } catch (RuntimeException unreadable) {
            return "La copia de la nube no se puede leer; no he cambiado nada.";
        }
    }

    /** Lee el registro y, la primera vez, lo fusiona con la copia remota. */
    private VidaLog load() throws IOException {
        VidaLog local = store.load();
        if (cloud == null || cloudChecked.exists()) return local;
        try {
            String remote = cloud.fetch();
            VidaLog merged = remote == null ? local : merge(local, VidaLog.parse(remote));
            if (remote != null) store.save(merged);
            markChecked();
            if (remote == null && !local.isEmpty()) push(local); // primera copia de lo que ya había
            return merged;
        } catch (IOException | RuntimeException unavailable) {
            return local; // sin comprobar: no se sube nada que pueda pisar la copia remota
        }
    }

    private boolean push(VidaLog log) {
        if (cloud == null || !cloudChecked.exists()) return false;
        try { cloud.push(log.serialize()); return true; } catch (RuntimeException ignored) { return false; }
    }

    private void markChecked() throws IOException {
        if (!cloudChecked.exists()) {
            File parent = cloudChecked.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("No se pudo preparar el almacenamiento.");
            Files.write(cloudChecked.toPath(), new byte[0]);
        }
    }

    /** Unión sin duplicados; ante el mismo registro gana la versión local. */
    static VidaLog merge(VidaLog local, VidaLog remote) {
        Map<String, VidaLog.GymSet> sets = new LinkedHashMap<>();
        for (VidaLog.GymSet s : remote.sets) sets.put(s.key, s);
        for (VidaLog.GymSet s : local.sets) sets.put(s.key, s);
        Map<String, VidaLog.SleepEntry> sleep = new LinkedHashMap<>();
        for (VidaLog.SleepEntry e : remote.sleep) sleep.put(e.date, e);
        for (VidaLog.SleepEntry e : local.sleep) sleep.put(e.date, e);
        Map<String, VidaLog.Meal> meals = new LinkedHashMap<>();
        for (VidaLog.Meal m : remote.meals) meals.put(m.date + "|" + m.time + "|" + m.kind + "|" + m.text, m);
        for (VidaLog.Meal m : local.meals) meals.put(m.date + "|" + m.time + "|" + m.kind + "|" + m.text, m);
        Map<String, VidaLog.Block> blocks = new LinkedHashMap<>();
        for (VidaLog.Block b : remote.blocks) blocks.put(VidaConversation.normalize(b.title) + "|" + b.day, b);
        for (VidaLog.Block b : local.blocks) blocks.put(VidaConversation.normalize(b.title) + "|" + b.day, b);
        Map<String, VidaLog.PlannedMeal> plan = new LinkedHashMap<>();
        for (VidaLog.PlannedMeal p : remote.plan) plan.put(p.day + "|" + p.kind, p);
        for (VidaLog.PlannedMeal p : local.plan) plan.put(p.day + "|" + p.kind, p);
        Map<String, VidaLog.Habit> habits = new LinkedHashMap<>();
        for (VidaLog.Habit h : remote.habits) habits.put(h.id, h);
        for (VidaLog.Habit h : local.habits) habits.put(h.id, h);
        Map<String, VidaLog.HabitCheck> checks = new LinkedHashMap<>();
        for (VidaLog.HabitCheck k : remote.checks) checks.put(k.habitId + "|" + k.date, k);
        for (VidaLog.HabitCheck k : local.checks) checks.put(k.habitId + "|" + k.date, k);
        Map<String, VidaLog.Goal> goals = new LinkedHashMap<>();
        for (VidaLog.Goal g : remote.goals) goals.put(g.id, g);
        // Un objetivo marcado como hecho en cualquier móvil sigue hecho.
        for (VidaLog.Goal g : local.goals) { VidaLog.Goal r = goals.get(g.id); goals.put(g.id, r != null && r.done && !g.done ? r : g); }
        Map<String, VidaLog.Routine> routines = new LinkedHashMap<>();
        for (VidaLog.Routine r : remote.routines) routines.put(r.id, r);
        for (VidaLog.Routine r : local.routines) routines.put(r.id, r);
        Map<String, VidaLog.Focus> focus = new LinkedHashMap<>();
        for (VidaLog.Focus f : remote.focus) focus.put(f.date, f);
        for (VidaLog.Focus f : local.focus) { VidaLog.Focus r = focus.get(f.date); focus.put(f.date, r != null && r.done && !f.done && r.text.equals(f.text) ? r : f); }
        Map<String, VidaLog.Project> projects = new LinkedHashMap<>();
        for (VidaLog.Project j : remote.projects) projects.put(j.id, j);
        for (VidaLog.Project j : local.projects) projects.put(j.id, j);
        Map<String, VidaLog.Review> reviews = new LinkedHashMap<>();
        for (VidaLog.Review w : remote.reviews) reviews.put(w.week, w);
        for (VidaLog.Review w : local.reviews) reviews.put(w.week, w);
        Map<String, VidaLog.ProjectEvent> events = new LinkedHashMap<>();
        for (VidaLog.ProjectEvent e : remote.projectEvents) events.put(e.id, e);
        for (VidaLog.ProjectEvent e : local.projectEvents) events.put(e.id, e);
        return new VidaLog(new ArrayList<>(sets.values()), new ArrayList<>(sleep.values()),
                new ArrayList<>(meals.values()), new ArrayList<>(blocks.values()), new ArrayList<>(plan.values()),
                new ArrayList<>(habits.values()), new ArrayList<>(checks.values()), new ArrayList<>(goals.values()),
                new ArrayList<>(routines.values()), new ArrayList<>(focus.values()), new ArrayList<>(projects.values()),
                new ArrayList<>(reviews.values())).withProjectEvents(new ArrayList<>(events.values()));
    }

    private VidaLog snapshot() {
        VidaLog c = cached;
        if (c != null) return c;
        synchronized (lock) {
            try { cached = store.load(); } catch (IOException unreadable) { cached = new VidaLog(); }
            return cached;
        }
    }
}
