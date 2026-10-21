package salve.core.trace;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Application events and explicit task notes. Never receives model prompts or private reasoning channels. */
public final class ExecutionJournal {
    public static final int MAX_TASKS = 20, MAX_EVENTS = 192, MAX_TASK_CHARS = 65536;
    private static final long MAX_FILE_BYTES = 12L * 1024 * 1024;
    private static final Gson JSON = new Gson();
    public static final class Event {
        public long at;
        public String kind, text;
    }
    public static final class Task {
        public String id, type, status = "EN_CURSO";
        public long started, ended;
        public int omitted;
        public List<Event> events = new ArrayList<>();
    }
    private static final class State {
        int version = 1;
        List<Task> tasks = new ArrayList<>();
    }
    private final File file;
    private State state;
    private boolean readOnly;
    private String notice = "";

    public ExecutionJournal(File directory) { file = new File(directory, "execution.json"); }

    private void load() {
        if (state != null) return;
        try {
            if (!file.exists()) { state = new State(); return; }
            if (file.length() > MAX_FILE_BYTES) throw new IOException("size");
            State loaded = JSON.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), State.class);
            if (loaded == null || loaded.version != 1 || loaded.tasks == null || loaded.tasks.size() > MAX_TASKS)
                throw new IOException("format");
            Set<String> ids = new HashSet<>();
            for (Task task : loaded.tasks) {
                if (task == null || task.id == null || !ids.add(task.id) || task.type == null || task.status == null
                        || task.events == null || task.events.size() > MAX_EVENTS || task.omitted < 0) throw new IOException("task");
                int chars = 0;
                for (Event event : task.events) {
                    if (event == null || event.kind == null || event.text == null) throw new IOException("event");
                    chars += event.text.length() + event.kind.length();
                    if (chars > MAX_TASK_CHARS) throw new IOException("limit");
                }
                if ("EN_CURSO".equals(task.status)) task.status = "INTERRUMPIDA_AL_CERRAR";
            }
            state = loaded;
        } catch (IOException | RuntimeException invalid) {
            state = new State(); readOnly = true;
            notice = "No pude leer el registro guardado. Lo conservo sin sobrescribir; puedes borrarlo para iniciar uno nuevo.";
        }
    }
    public synchronized String begin(String type, String request) {
        load();
        if (readOnly) return null;
        Task task = new Task(); task.id = UUID.randomUUID().toString(); task.type = type;
        task.started = System.currentTimeMillis(); state.tasks.add(task);
        while (state.tasks.size() > MAX_TASKS) state.tasks.remove(0);
        append(task.id, "PETICIÓN", request == null ? "" : request);
        return task.id;
    }
    /** Retains the complete string, including whitespace. Oversized events are omitted, never silently cut. */
    public synchronized void append(String id, String kind, String text) {
        load(); Task task = find(id);
        if (readOnly || task == null || !"EN_CURSO".equals(task.status) || text == null || kind == null) return;
        int chars = kind.length() + text.length();
        for (Event event : task.events) chars += event.kind.length() + event.text.length();
        if (task.events.size() >= MAX_EVENTS || chars > MAX_TASK_CHARS) task.omitted++;
        else {
            Event event = new Event(); event.at = System.currentTimeMillis(); event.kind = kind; event.text = text;
            task.events.add(event);
        }
        save();
    }
    public synchronized void finish(String id, String status) {
        load(); Task task = find(id);
        if (readOnly || task == null || !"EN_CURSO".equals(task.status)) return;
        task.status = status; task.ended = System.currentTimeMillis(); save();
    }
    public synchronized List<Task> recent() {
        load(); List<Task> copy = new ArrayList<>();
        for (int i = state.tasks.size() - 1; i >= 0; i--) copy.add(JSON.fromJson(JSON.toJson(state.tasks.get(i)), Task.class));
        return copy;
    }
    public synchronized String notice() { load(); return notice; }
    public synchronized void clear() { state = new State(); readOnly = false; save(); }
    private Task find(String id) {
        if (id != null) for (Task task : state.tasks) if (id.equals(task.id)) return task;
        return null;
    }
    private void save() {
        try {
            Files.createDirectories(file.getParentFile().toPath());
            File tmp = new File(file.getParentFile(), "execution.tmp");
            Files.write(tmp.toPath(), JSON.toJson(state).getBytes(StandardCharsets.UTF_8));
            try { Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING); }
            notice = "";
        } catch (IOException | RuntimeException failure) {
            notice = "No pude guardar los últimos cambios. Lo mostrado sigue en memoria hasta que se cierre la aplicación.";
        }
    }
    public static String transcript(Task task) {
        StringBuilder out = new StringBuilder(task.type).append(" · ").append(task.status)
                .append("\nID: ").append(task.id).append("\nInicio: ").append(date(task.started)).append('\n');
        if (task.ended > 0) out.append("Fin: ").append(date(task.ended)).append('\n');
        out.append("\nRegistro de acciones y notas de tarea; las valoraciones del modelo son interpretaciones.\n");
        for (Event event : task.events)
            out.append("\n[").append(date(event.at)).append("] ").append(event.kind).append('\n').append(event.text).append('\n');
        if (task.omitted > 0) out.append("\nAVISO: ").append(task.omitted)
                .append(" entradas omitidas por el límite del registro. Esta transcripción está incompleta.\n");
        return out.toString();
    }
    public static String date(long at) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", java.util.Locale.ROOT).format(new java.util.Date(at));
    }
}
