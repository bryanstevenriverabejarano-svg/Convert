package salve.core.vida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registro personal del módulo Vida: series del gym, sueño, comidas y bloques del horario.
 * Inmutable para quien lo lee: cada cambio devuelve una copia nueva que el servicio guarda
 * antes de responder. Formato de disco: una línea por registro, campos separados por tabulador.
 */
public final class VidaLog {

    /** Una serie de un ejercicio. {@code key} evita duplicar series al reimportar un CSV. */
    public static final class GymSet {
        public final String date, workout, exercise, type, source, key;
        public final int setIndex;
        public final Double kg, km;
        public final Integer reps, seconds;

        public GymSet(String date, String workout, String exercise, int setIndex, String type,
                      Double kg, Integer reps, Double km, Integer seconds, String source, String key) {
            this.date = req(date); this.workout = opt(workout); this.exercise = req(exercise);
            this.setIndex = setIndex; this.type = type == null || type.isEmpty() ? "normal" : type;
            this.kg = kg; this.reps = reps; this.km = km; this.seconds = seconds;
            this.source = opt(source); this.key = key == null || key.isEmpty() ? date + "|" + exercise + "|" + setIndex : key;
        }
        /** Volumen de la serie (kg × reps); 0 si no hay peso o repeticiones. */
        public double volume() { return kg == null || reps == null ? 0 : kg * reps; }
    }

    public static final class SleepEntry {
        public final String date, bed, wake;
        public final int minutes;
        public SleepEntry(String date, String bed, String wake, int minutes) {
            this.date = req(date); this.bed = req(bed); this.wake = req(wake); this.minutes = minutes;
        }
    }

    public static final class Meal {
        public final String date, time, kind, text;
        public Meal(String date, String time, String kind, String text) {
            this.date = req(date); this.time = opt(time); this.kind = req(kind); this.text = req(text);
        }
    }

    /** Bloque fijo del horario semanal. {@code day}: 1 = lunes … 7 = domingo. */
    public static final class Block {
        public final String title, time;
        public final int day, minutes;
        public Block(String title, int day, String time, int minutes) {
            if (day < 1 || day > 7) throw new IllegalArgumentException("Día no válido.");
            this.title = req(title); this.day = day; this.time = req(time); this.minutes = minutes;
        }
    }

    /** Comida planificada para un día de la semana. {@code kind}: desayuno, comida, merienda, cena. */
    public static final class PlannedMeal {
        public final int day; public final String kind, text;
        public PlannedMeal(int day, String kind, String text) {
            if (day < 1 || day > 7) throw new IllegalArgumentException("Día no válido.");
            this.day = day; this.kind = req(kind); this.text = req(text);
        }
    }

    /** Hábito diario o semanal. {@code perWeek}: veces por semana que cuenta como cumplido (1–7). */
    public static final class Habit {
        public final String id, name; public final int perWeek;
        public Habit(String id, String name, int perWeek) {
            this.id = req(id); this.name = req(name); this.perWeek = Math.max(1, Math.min(7, perWeek));
        }
    }

    /** Un día en que se cumplió un hábito. */
    public static final class HabitCheck {
        public final String habitId, date;
        public HabitCheck(String habitId, String date) { this.habitId = req(habitId); this.date = req(date); }
    }

    /** Objetivo o hito con fecha límite opcional (yyyy-MM-dd o vacío). */
    public static final class Goal {
        public final String id, title, due, area; public final boolean done;
        public Goal(String id, String title, String due, String area, boolean done) {
            this.id = req(id); this.title = req(title); this.due = opt(due); this.area = opt(area); this.done = done;
        }
        public Goal withDone(boolean d) { return new Goal(id, title, due, area, d); }
    }

    /** Rutina guardada (plantilla): ejercicios en orden con nº de series y descanso. */
    public static final class Routine {
        public static final class Item {
            public final String exercise; public final int sets, restSeconds;
            public Item(String exercise, int sets, int restSeconds) {
                this.exercise = req(exercise); this.sets = Math.max(1, Math.min(20, sets)); this.restSeconds = Math.max(0, Math.min(600, restSeconds));
            }
        }
        public final String id, title; public final List<Item> items;
        public Routine(String id, String title, List<Item> items) {
            this.id = req(id); this.title = req(title); this.items = Collections.unmodifiableList(new ArrayList<>(items));
        }
        String encode() {
            StringBuilder b = new StringBuilder();
            for (Item i : items) {
                if (b.length() > 0) b.append('\u001E');
                b.append(i.exercise.replace('\u001E', ' ').replace('\u001F', ' ')).append('\u001F').append(i.sets).append('\u001F').append(i.restSeconds);
            }
            return b.toString();
        }
        static List<Item> decode(String s) {
            List<Item> out = new ArrayList<>();
            if (s == null || s.isEmpty()) return out;
            for (String part : s.split("\u001E")) {
                String[] f = part.split("\u001F");
                if (f.length == 3) out.add(new Item(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])));
            }
            return out;
        }
    }

    /** La tarea más importante de un día. */
    public static final class Focus {
        public final String date, text; public final boolean done;
        public Focus(String date, String text, boolean done) { this.date = req(date); this.text = req(text); this.done = done; }
    }

    /** Proyecto con pasos. {@code status}: activo, pausado o hecho. */
    public static final class Project {
        public static final class Step {
            public final String text; public final boolean done;
            public Step(String text, boolean done) { this.text = req(text); this.done = done; }
        }
        public final String id, title, status, created; public final List<Step> steps;
        public Project(String id, String title, String status, String created, List<Step> steps) {
            this.id = req(id); this.title = req(title); this.status = req(status); this.created = opt(created);
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
        }
        String encode() {
            StringBuilder b = new StringBuilder();
            for (Step st : steps) {
                if (b.length() > 0) b.append('\u001E');
                b.append(st.text.replace('\u001E', ' ').replace('\u001F', ' ')).append('\u001F').append(st.done ? 1 : 0);
            }
            return b.toString();
        }
        static List<Step> decode(String s) {
            List<Step> out = new ArrayList<>();
            if (s == null || s.isEmpty()) return out;
            for (String part : s.split("\u001E")) {
                String[] f = part.split("\u001F");
                if (f.length == 2) out.add(new Step(f[0], "1".equals(f[1])));
            }
            return out;
        }
    }

    /** Revisión semanal: lunes de la semana revisada y las tres respuestas. */
    public static final class Review {
        public final String week, good, bad, priority;
        public Review(String week, String good, String bad, String priority) {
            this.week = req(week); this.good = opt(good); this.bad = opt(bad); this.priority = opt(priority);
        }
    }

    /** Evento explícito de un proyecto, conservado en el archivo TSV. */
    public static final class ProjectEvent {
        public final String id, projectId, date, action, detail;
        public ProjectEvent(String id, String projectId, String date, String action, String detail) {
            this.id = req(id); this.projectId = req(projectId); this.date = req(date);
            if (!("crear".equals(action) || "corregir".equals(action)
                    || "completar".equals(action) || "aplazar".equals(action)))
                throw new IllegalArgumentException("Tipo de avance no válido.");
            this.action = action; this.detail = opt(detail);
        }
    }

    /** Cambio puntual de una actividad semanal, sin alterar su plantilla. */
    public static final class DayOverride {
        public final String originalDate, title, originalTime, targetDate, targetTime;
        public DayOverride(String originalDate, String title, String originalTime, String targetDate, String targetTime) {
            this.originalDate = req(originalDate); this.title = req(title); this.originalTime = req(originalTime);
            this.targetDate = req(targetDate); this.targetTime = req(targetTime);
        }
    }

    public final List<GymSet> sets;
    public final List<SleepEntry> sleep;
    public final List<Meal> meals;
    public final List<Block> blocks;
    public final List<PlannedMeal> plan;
    public final List<Habit> habits;
    public final List<HabitCheck> checks;
    public final List<Goal> goals;
    public final List<Routine> routines;
    public final List<Focus> focus;
    public final List<Project> projects;
    public final List<Review> reviews;
    public final List<ProjectEvent> projectEvents;
    public final List<DayOverride> dayOverrides;

    public VidaLog() { this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()); }

    public VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks) {
        this(sets, sleep, meals, blocks, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks,
                   List<PlannedMeal> plan, List<Habit> habits, List<HabitCheck> checks, List<Goal> goals) {
        this(sets, sleep, meals, blocks, plan, habits, checks, goals, new ArrayList<>());
    }

    public VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks,
                   List<PlannedMeal> plan, List<Habit> habits, List<HabitCheck> checks, List<Goal> goals, List<Routine> routines) {
        this(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }

    public VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks,
                   List<PlannedMeal> plan, List<Habit> habits, List<HabitCheck> checks, List<Goal> goals, List<Routine> routines,
                   List<Focus> focus, List<Project> projects, List<Review> reviews) {
        this(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, new ArrayList<>(), new ArrayList<>());
    }

    private VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks,
                   List<PlannedMeal> plan, List<Habit> habits, List<HabitCheck> checks, List<Goal> goals, List<Routine> routines,
                   List<Focus> focus, List<Project> projects, List<Review> reviews, List<ProjectEvent> projectEvents) {
        this(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, new ArrayList<>());
    }

    private VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks,
                   List<PlannedMeal> plan, List<Habit> habits, List<HabitCheck> checks, List<Goal> goals, List<Routine> routines,
                   List<Focus> focus, List<Project> projects, List<Review> reviews, List<ProjectEvent> projectEvents,
                   List<DayOverride> dayOverrides) {
        this.dayOverrides = Collections.unmodifiableList(new ArrayList<>(dayOverrides));
        this.projectEvents = Collections.unmodifiableList(new ArrayList<>(projectEvents));
        this.sets = Collections.unmodifiableList(new ArrayList<>(sets));
        this.sleep = Collections.unmodifiableList(new ArrayList<>(sleep));
        this.meals = Collections.unmodifiableList(new ArrayList<>(meals));
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
        this.plan = Collections.unmodifiableList(new ArrayList<>(plan));
        this.habits = Collections.unmodifiableList(new ArrayList<>(habits));
        this.checks = Collections.unmodifiableList(new ArrayList<>(checks));
        this.goals = Collections.unmodifiableList(new ArrayList<>(goals));
        this.routines = Collections.unmodifiableList(new ArrayList<>(routines));
        this.focus = Collections.unmodifiableList(new ArrayList<>(focus));
        this.projects = Collections.unmodifiableList(new ArrayList<>(projects));
        this.reviews = Collections.unmodifiableList(new ArrayList<>(reviews));
    }

    public boolean isEmpty() {
        return sets.isEmpty() && sleep.isEmpty() && meals.isEmpty() && blocks.isEmpty()
                && plan.isEmpty() && habits.isEmpty() && checks.isEmpty() && goals.isEmpty() && routines.isEmpty()
                && focus.isEmpty() && projects.isEmpty() && reviews.isEmpty() && projectEvents.isEmpty() && dayOverrides.isEmpty();
    }

    public VidaLog withSets(List<GymSet> next) { return new VidaLog(next, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withSleep(List<SleepEntry> next) { return new VidaLog(sets, next, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withMeals(List<Meal> next) { return new VidaLog(sets, sleep, next, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withBlocks(List<Block> next) { return new VidaLog(sets, sleep, meals, next, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withPlan(List<PlannedMeal> next) { return new VidaLog(sets, sleep, meals, blocks, next, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withHabits(List<Habit> next) { return new VidaLog(sets, sleep, meals, blocks, plan, next, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withChecks(List<HabitCheck> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, next, goals, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withGoals(List<Goal> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, next, routines, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withRoutines(List<Routine> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, next, focus, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withFocus(List<Focus> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, next, projects, reviews, projectEvents, dayOverrides); }
    public VidaLog withProjects(List<Project> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, next, reviews, projectEvents, dayOverrides); }
    public VidaLog withReviews(List<Review> next) { return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, next, projectEvents, dayOverrides); }

    public VidaLog withProjectEvents(List<ProjectEvent> next) {
        return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, next, dayOverrides);
    }

    public VidaLog withDayOverrides(List<DayOverride> next) {
        return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, next);
    }

    // ---------------------------------------------------------------- serialización

    public String serialize() {
        StringBuilder out = new StringBuilder("vida\t1\n");
        for (GymSet s : sets) line(out, "S", s.date, s.workout, s.exercise, String.valueOf(s.setIndex), s.type,
                num(s.kg), num(s.reps), num(s.km), num(s.seconds), s.source, s.key);
        for (SleepEntry e : sleep) line(out, "D", e.date, e.bed, e.wake, String.valueOf(e.minutes));
        for (Meal m : meals) line(out, "C", m.date, m.time, m.kind, m.text);
        for (Block b : blocks) line(out, "B", b.title, String.valueOf(b.day), b.time, String.valueOf(b.minutes));
        for (PlannedMeal p : plan) line(out, "P", String.valueOf(p.day), p.kind, p.text);
        for (Habit h : habits) line(out, "H", h.id, h.name, String.valueOf(h.perWeek));
        for (HabitCheck k : checks) line(out, "K", k.habitId, k.date);
        for (Goal g : goals) line(out, "G", g.id, g.title, g.due, g.area, g.done ? "1" : "0");
        for (Routine r : routines) line(out, "R", r.id, r.title, r.encode());
        for (Focus f : focus) line(out, "F", f.date, f.text, f.done ? "1" : "0");
        for (Project j : projects) line(out, "J", j.id, j.title, j.status, j.created, j.encode());
        for (Review w : reviews) line(out, "W", w.week, w.good, w.bad, w.priority);
        for (ProjectEvent e : projectEvents) line(out, "E", e.id, e.projectId, e.date, e.action, e.detail);
        for (DayOverride o : dayOverrides) line(out, "O", o.originalDate, o.title, o.originalTime, o.targetDate, o.targetTime);
        return out.toString();
    }

    public static VidaLog parse(String text) {
        if (text == null || text.trim().isEmpty()) return new VidaLog();
        String[] lines = text.split("\n");
        if (!lines[0].startsWith("vida\t")) throw new IllegalArgumentException("No es un registro de Vida.");
        List<GymSet> sets = new ArrayList<>(); List<SleepEntry> sleep = new ArrayList<>();
        List<Meal> meals = new ArrayList<>(); List<Block> blocks = new ArrayList<>();
        List<PlannedMeal> plan = new ArrayList<>(); List<Habit> habits = new ArrayList<>();
        List<HabitCheck> checks = new ArrayList<>(); List<Goal> goals = new ArrayList<>();
        List<Routine> routines = new ArrayList<>();
        List<Focus> focus = new ArrayList<>(); List<Project> projects = new ArrayList<>(); List<Review> reviews = new ArrayList<>();
        List<ProjectEvent> projectEvents = new ArrayList<>();
        List<DayOverride> dayOverrides = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            String[] f = fields(lines[i]);
            switch (f[0]) {
                case "S": sets.add(new GymSet(f[1], f[2], f[3], Integer.parseInt(f[4]), f[5], dbl(f[6]), integer(f[7]),
                        dbl(f[8]), integer(f[9]), f[10], f[11])); break;
                case "D": sleep.add(new SleepEntry(f[1], f[2], f[3], Integer.parseInt(f[4]))); break;
                case "C": meals.add(new Meal(f[1], f[2], f[3], f[4])); break;
                case "B": blocks.add(new Block(f[1], Integer.parseInt(f[2]), f[3], Integer.parseInt(f[4]))); break;
                case "P": plan.add(new PlannedMeal(Integer.parseInt(f[1]), f[2], f[3])); break;
                case "H": habits.add(new Habit(f[1], f[2], Integer.parseInt(f[3]))); break;
                case "K": checks.add(new HabitCheck(f[1], f[2])); break;
                case "G": goals.add(new Goal(f[1], f[2], f[3], f[4], "1".equals(f[5]))); break;
                case "R": routines.add(new Routine(f[1], f[2], Routine.decode(f[3]))); break;
                case "F": focus.add(new Focus(f[1], f[2], "1".equals(f[3]))); break;
                case "J": projects.add(new Project(f[1], f[2], f[3], f[4], Project.decode(f[5]))); break;
                case "W": reviews.add(new Review(f[1], f[2], f[3], f[4])); break;
                case "E": projectEvents.add(new ProjectEvent(f[1], f[2], f[3], f[4], f[5])); break;
                case "O": dayOverrides.add(new DayOverride(f[1], f[2], f[3], f[4], f[5])); break;
                default: break; // tipos futuros: se ignoran sin perder el resto
            }
        }
        return new VidaLog(sets, sleep, meals, blocks, plan, habits, checks, goals, routines, focus, projects, reviews, projectEvents, dayOverrides);
    }

    private static void line(StringBuilder out, String... f) {
        for (int i = 0; i < f.length; i++) { if (i > 0) out.append('\t'); out.append(escape(f[i])); }
        out.append('\n');
    }
    private static String escape(String v) {
        if (v == null) return "";
        return v.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "");
    }
    private static String[] fields(String line) {
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) {
                char n = line.charAt(++i);
                cur.append(n == 't' ? '\t' : n == 'n' ? '\n' : n);
            } else if (c == '\t') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        while (out.size() < 12) out.add("");
        return out.toArray(new String[0]);
    }
    private static String num(Number n) { return n == null ? "" : n.toString(); }
    private static Double dbl(String s) { return s == null || s.isEmpty() ? null : Double.valueOf(s); }
    private static Integer integer(String s) { return s == null || s.isEmpty() ? null : Integer.valueOf(s); }
    private static String req(String s) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException("Falta un dato obligatorio.");
        return s.trim();
    }
    private static String opt(String s) { return s == null ? "" : s.trim(); }
}
