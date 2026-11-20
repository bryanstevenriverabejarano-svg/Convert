package salve.core.vida;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Lógica de los módulos de Vida que no son el gym: hábitos con rachas, plan semanal de comidas,
 * sueño, objetivos y rutinas guardadas. Pura (sin Android) para poder probarla.
 */
public final class VidaPlanner {
    private VidaPlanner() {}

    public static final String[] DAYS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    public static final String[] MEAL_KINDS = {"desayuno", "comida", "merienda", "cena"};

    // ---------------------------------------------------------------- arranque

    /**
     * Primera vez: hábitos y objetivos del plan de 12 meses de Bryan, para no empezar en blanco.
     * Solo se siembran si no hay ninguno; nunca pisa lo que ya existe.
     */
    public static VidaLog seedDefaults(VidaLog log) {
        VidaLog out = log;
        if (log.habits.isEmpty()) {
            List<VidaLog.Habit> h = new ArrayList<>();
            h.add(new VidaLog.Habit("gym", "Gym", 4));
            h.add(new VidaLog.Habit("mates", "1 vídeo de matemáticas", 5));
            h.add(new VidaLog.Habit("sueno", "Dormir 7 h o más", 7));
            h.add(new VidaLog.Habit("proyecto", "2 h en la prioridad del trimestre", 5));
            h.add(new VidaLog.Habit("historia", "Escribir una microhistoria", 1));
            h.add(new VidaLog.Habit("revision", "Revisión semanal (domingo)", 1));
            out = out.withHabits(h);
        }
        if (log.goals.isEmpty()) {
            List<VidaLog.Goal> g = new ArrayList<>();
            g.add(new VidaLog.Goal("video-bray", "Publicar «Me dicen Bray» y 3 reels", "2026-11-30", "YouTube", false));
            g.add(new VidaLog.Goal("vida-salve", "Módulo Vida en Salve funcionando", "2026-12-31", "Salve", false));
            g.add(new VidaLog.Goal("gym-8", "8 semanas seguidas de gym registradas", "2027-02-28", "Salud", false));
            g.add(new VidaLog.Goal("historias-12", "12 microhistorias escritas", "2027-02-28", "Historias", false));
            g.add(new VidaLog.Goal("mates-curso", "Terminar el curso de matemáticas de Udemy", "2027-05-31", "UNED", false));
            g.add(new VidaLog.Goal("uned", "Matricularme en Ingeniería en IA (UNED)", "2027-10-31", "UNED", false));
            g.add(new VidaLog.Goal("cliente", "Primer cliente de pago de Zenitcorp", "2027-08-31", "Zenitcorp", false));
            out = out.withGoals(g);
        }
        return out;
    }

    // ---------------------------------------------------------------- hábitos

    public static boolean checked(VidaLog log, String habitId, LocalDate day) {
        String d = day.toString();
        for (VidaLog.HabitCheck k : log.checks) if (k.habitId.equals(habitId) && k.date.equals(d)) return true;
        return false;
    }

    public static VidaLog toggle(VidaLog log, String habitId, LocalDate day) {
        List<VidaLog.HabitCheck> next = new ArrayList<>();
        boolean removed = false;
        for (VidaLog.HabitCheck k : log.checks) {
            if (k.habitId.equals(habitId) && k.date.equals(day.toString())) { removed = true; continue; }
            next.add(k);
        }
        if (!removed) next.add(new VidaLog.HabitCheck(habitId, day.toString()));
        return log.withChecks(next);
    }

    /** Veces cumplido en la semana (lunes–domingo) que contiene {@code day}. */
    public static int doneThisWeek(VidaLog log, String habitId, LocalDate day) {
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int n = 0;
        for (VidaLog.HabitCheck k : log.checks) {
            if (!k.habitId.equals(habitId)) continue;
            LocalDate d = LocalDate.parse(k.date);
            if (!d.isBefore(monday) && !d.isAfter(monday.plusDays(6))) n++;
        }
        return n;
    }

    /** Semanas seguidas (hacia atrás, sin contar la actual si aún no está cumplida) que llegaron a la meta. */
    public static int weekStreak(VidaLog log, VidaLog.Habit h, LocalDate today) {
        int streak = 0;
        LocalDate week = today;
        if (doneThisWeek(log, h.id, week) < h.perWeek) week = week.minusWeeks(1);
        while (doneThisWeek(log, h.id, week) >= h.perWeek && streak < 520) { streak++; week = week.minusWeeks(1); }
        return streak;
    }

    /** El gym cuenta solo: un día con series registradas marca el hábito «gym». */
    public static VidaLog syncGymHabit(VidaLog log) {
        Set<String> have = new HashSet<>();
        for (VidaLog.HabitCheck k : log.checks) if (k.habitId.equals("gym")) have.add(k.date);
        boolean exists = false;
        for (VidaLog.Habit h : log.habits) if (h.id.equals("gym")) exists = true;
        if (!exists) return log;
        List<VidaLog.HabitCheck> next = new ArrayList<>(log.checks);
        boolean changed = false;
        for (VidaLog.GymSet s : log.sets) if (have.add(s.date)) { next.add(new VidaLog.HabitCheck("gym", s.date)); changed = true; }
        return changed ? log.withChecks(next) : log;
    }

    /** Igual con el sueño: una noche de 7 h o más marca «sueno». */
    public static VidaLog syncSleepHabit(VidaLog log) {
        Set<String> have = new HashSet<>();
        for (VidaLog.HabitCheck k : log.checks) if (k.habitId.equals("sueno")) have.add(k.date);
        boolean exists = false;
        for (VidaLog.Habit h : log.habits) if (h.id.equals("sueno")) exists = true;
        if (!exists) return log;
        List<VidaLog.HabitCheck> next = new ArrayList<>(log.checks);
        boolean changed = false;
        for (VidaLog.SleepEntry e : log.sleep) if (e.minutes >= 7 * 60 && have.add(e.date)) { next.add(new VidaLog.HabitCheck("sueno", e.date)); changed = true; }
        return changed ? log.withChecks(next) : log;
    }

    public static VidaLog addHabit(VidaLog log, String name, int perWeek) {
        List<VidaLog.Habit> next = new ArrayList<>(log.habits);
        next.add(new VidaLog.Habit(newId(), name, perWeek));
        return log.withHabits(next);
    }

    public static VidaLog removeHabit(VidaLog log, String id) {
        List<VidaLog.Habit> next = new ArrayList<>();
        for (VidaLog.Habit h : log.habits) if (!h.id.equals(id)) next.add(h);
        List<VidaLog.HabitCheck> checks = new ArrayList<>();
        for (VidaLog.HabitCheck k : log.checks) if (!k.habitId.equals(id)) checks.add(k);
        return log.withHabits(next).withChecks(checks);
    }

    // ---------------------------------------------------------------- comidas

    public static String planned(VidaLog log, int day, String kind) {
        for (VidaLog.PlannedMeal p : log.plan) if (p.day == day && p.kind.equals(kind)) return p.text;
        return null;
    }

    /** Texto vacío = quitar la comida del plan. */
    public static VidaLog setPlanned(VidaLog log, int day, String kind, String text) {
        List<VidaLog.PlannedMeal> next = new ArrayList<>();
        for (VidaLog.PlannedMeal p : log.plan) if (!(p.day == day && p.kind.equals(kind))) next.add(p);
        if (text != null && !text.trim().isEmpty()) next.add(new VidaLog.PlannedMeal(day, kind, text.trim()));
        next.sort(Comparator.comparingInt((VidaLog.PlannedMeal p) -> p.day).thenComparingInt(p -> kindOrder(p.kind)));
        return log.withPlan(next);
    }

    /** Copia el plan de un día a otro (p. ej. repetir el lunes el martes). */
    public static VidaLog copyDay(VidaLog log, int from, int to) {
        VidaLog out = log;
        for (String kind : MEAL_KINDS) out = setPlanned(out, to, kind, planned(log, from, kind));
        return out;
    }

    /** Lista de la compra: platos de la semana sin repetir, en orden. */
    public static List<String> shoppingList(VidaLog log) {
        List<String> out = new ArrayList<>();
        for (VidaLog.PlannedMeal p : log.plan) {
            String item = p.text.trim();
            if (!out.contains(item)) out.add(item);
        }
        return out;
    }

    public static VidaLog logMeal(VidaLog log, LocalDate day, String time, String kind, String text) {
        List<VidaLog.Meal> next = new ArrayList<>(log.meals);
        next.add(new VidaLog.Meal(day.toString(), time, kind, text));
        return log.withMeals(next);
    }

    public static List<VidaLog.Meal> mealsOn(VidaLog log, LocalDate day) {
        List<VidaLog.Meal> out = new ArrayList<>();
        for (VidaLog.Meal m : log.meals) if (m.date.equals(day.toString())) out.add(m);
        out.sort(Comparator.comparingInt(m -> kindOrder(m.kind)));
        return out;
    }

    // ---------------------------------------------------------------- sueño

    /** Registra la noche que termina el día {@code day}. Horas «HH:mm». Cruza medianoche. */
    public static VidaLog logSleep(VidaLog log, LocalDate day, String bed, String wake) {
        int b = minutes(bed), w = minutes(wake);
        int total = Math.floorMod(w - b, 24 * 60);
        if (total < 30 || total > 16 * 60) throw new IllegalArgumentException("Esa noche dura " + VidaConversation.hours(total) + "; revisa las horas.");
        List<VidaLog.SleepEntry> next = new ArrayList<>();
        for (VidaLog.SleepEntry e : log.sleep) if (!e.date.equals(day.toString())) next.add(e);
        next.add(new VidaLog.SleepEntry(day.toString(), bed, wake, total));
        next.sort(Comparator.comparing(e -> e.date));
        return syncSleepHabit(log.withSleep(next));
    }

    /** Últimas {@code n} noches, de la más antigua a la más reciente. */
    public static List<VidaLog.SleepEntry> lastNights(VidaLog log, int n) {
        List<VidaLog.SleepEntry> all = new ArrayList<>(log.sleep);
        all.sort(Comparator.comparing(e -> e.date));
        return all.subList(Math.max(0, all.size() - n), all.size());
    }

    public static int averageMinutes(List<VidaLog.SleepEntry> nights) {
        if (nights.isEmpty()) return 0;
        int t = 0; for (VidaLog.SleepEntry e : nights) t += e.minutes;
        return t / nights.size();
    }

    // ---------------------------------------------------------------- objetivos

    public static VidaLog addGoal(VidaLog log, String title, String due, String area) {
        List<VidaLog.Goal> next = new ArrayList<>(log.goals);
        next.add(new VidaLog.Goal(newId(), title, due, area, false));
        return log.withGoals(next);
    }

    public static VidaLog setGoalDone(VidaLog log, String id, boolean done) {
        List<VidaLog.Goal> next = new ArrayList<>();
        for (VidaLog.Goal g : log.goals) next.add(g.id.equals(id) ? g.withDone(done) : g);
        return log.withGoals(next);
    }

    public static VidaLog removeGoal(VidaLog log, String id) {
        List<VidaLog.Goal> next = new ArrayList<>();
        for (VidaLog.Goal g : log.goals) if (!g.id.equals(id)) next.add(g);
        return log.withGoals(next);
    }

    /** Pendientes primero por fecha; los hechos al final. */
    public static List<VidaLog.Goal> sortedGoals(VidaLog log) {
        List<VidaLog.Goal> out = new ArrayList<>(log.goals);
        out.sort(Comparator.comparing((VidaLog.Goal g) -> g.done).thenComparing(g -> g.due.isEmpty() ? "9999" : g.due));
        return out;
    }

    // ---------------------------------------------------------------- rutinas guardadas

    public static VidaLog saveRoutine(VidaLog log, VidaLog.Routine routine) {
        List<VidaLog.Routine> next = new ArrayList<>();
        boolean replaced = false;
        for (VidaLog.Routine r : log.routines) {
            if (r.id.equals(routine.id)) { next.add(routine); replaced = true; } else next.add(r);
        }
        if (!replaced) next.add(0, routine);
        return log.withRoutines(next);
    }

    public static VidaLog removeRoutine(VidaLog log, String id) {
        List<VidaLog.Routine> next = new ArrayList<>();
        for (VidaLog.Routine r : log.routines) if (!r.id.equals(id)) next.add(r);
        return log.withRoutines(next);
    }

    public static VidaLog duplicateRoutine(VidaLog log, String id) {
        for (VidaLog.Routine r : log.routines)
            if (r.id.equals(id)) return saveRoutine(log, new VidaLog.Routine(newId(), r.title + " (copia)", r.items));
        return log;
    }

    /** Convierte una rutina sacada del historial en una rutina guardada y editable. */
    public static VidaLog.Routine fromHistory(VidaLog log, GymTracker.Routine derived) {
        List<VidaLog.Routine.Item> items = new ArrayList<>();
        for (String ex : derived.exercises) items.add(new VidaLog.Routine.Item(ex, Math.max(1, GymTracker.previous(log, ex).size()), 90));
        return new VidaLog.Routine(newId(), derived.title, items);
    }

    /** Entreno a partir de una rutina guardada: sus series, con pesos y reps de la última vez. */
    public static List<GymTracker.LiveExercise> start(VidaLog log, VidaLog.Routine routine) {
        List<GymTracker.LiveExercise> out = new ArrayList<>();
        for (VidaLog.Routine.Item item : routine.items) {
            GymTracker.LiveExercise ex = new GymTracker.LiveExercise(item.exercise);
            ex.restSeconds = item.restSeconds;
            List<VidaLog.GymSet> prev = GymTracker.previous(log, item.exercise);
            for (int i = 0; i < item.sets; i++) {
                GymTracker.LiveSet s = new GymTracker.LiveSet();
                if (i < prev.size()) {
                    VidaLog.GymSet p = prev.get(i);
                    s.type = p.type; s.kg = p.kg; s.reps = p.reps; s.km = p.km; s.seconds = p.seconds;
                } else if (!prev.isEmpty()) {
                    VidaLog.GymSet p = prev.get(prev.size() - 1);
                    s.kg = p.kg; s.reps = p.reps;
                }
                ex.sets.add(s);
            }
            out.add(ex);
        }
        return out;
    }

    // ---------------------------------------------------------------- horario

    /** Tipo de bloque, para el color de la tabla. Se deduce del título. */
    public enum Kind { GYM, TRABAJO, ESTUDIO, PROYECTO, CREAR, DESCANSO, REVISION, OTRO }

    public static Kind kind(String title) {
        String t = " " + VidaConversation.normalize(title).replaceAll("[^a-z0-9 ]", " ") + " ";
        if (has(t, "gym", "gimnasio", "entreno", "pesas", "correr", "cardio")) return Kind.GYM;
        if (has(t, "trabajo", "zenitcorp", "empresa", "oficina", "cliente", "clientes")) return Kind.TRABAJO;
        if (has(t, "matematicas", "mates", "estudio", "estudiar", "uned", "curso", "clase", "leer", "lectura")) return Kind.ESTUDIO;
        if (has(t, "salve", "prioridad", "proyecto", "programar", "codigo")) return Kind.PROYECTO;
        if (has(t, "youtube", "video", "canal", "grabar", "editar", "historia", "microhistoria", "guion", "reels", "reel")) return Kind.CREAR;
        if (has(t, "revision", "planificar", "planificacion")) return Kind.REVISION;
        if (has(t, "descanso", "libre", "familia", "dormir", "sueno", "siesta", "desconectar")) return Kind.DESCANSO;
        return Kind.OTRO;
    }

    private static boolean has(String padded, String... words) {
        for (String w : words) if (padded.contains(" " + w + " ")) return true;
        return false;
    }

    /**
     * Semana propuesta a partir de los objetivos: gym 4 días, matemáticas 45 min × 5 (base UNED),
     * prioridad del trimestre 2 h × 5, microhistoria el miércoles, canal y revisión el domingo,
     * sábado de descanso total y desconectar pantallas a las 23:00. Encaja alrededor del trabajo.
     */
    public static List<VidaLog.Block> suggestedWeek(int workStart, int workEnd, Set<Integer> workDays) {
        List<VidaLog.Block> out = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            boolean works = workDays.contains(d);
            if (works) out.add(new VidaLog.Block("Trabajo (Zenitcorp)", d, hm(workStart * 60), Math.max(60, (workEnd - workStart) * 60)));
            int t = works ? Math.max(workEnd, 8) * 60 + 30 : 10 * 60;
            if (d == 6) { out.add(new VidaLog.Block("Descanso total (sin proyectos)", d, hm(Math.max(t, 10 * 60)), 10 * 60)); }
            else {
                if (d == 1 || d == 2 || d == 4 || d == 5) { out.add(new VidaLog.Block("Gym", d, hm(t), 75)); t += 90; }
                if (d <= 5) { out.add(new VidaLog.Block("Matemáticas (1 vídeo)", d, hm(t), 45)); t += 60; }
                if (d <= 5) { out.add(new VidaLog.Block("Prioridad del trimestre", d, hm(t), 120)); t += 120; }
                if (d == 3) out.add(new VidaLog.Block("Escribir microhistoria", d, hm(t), 45));
                if (d == 7) {
                    out.add(new VidaLog.Block("Canal: grabar y editar", d, "11:00", 120));
                    out.add(new VidaLog.Block("Revisión semanal", d, "19:00", 30));
                }
            }
            out.add(new VidaLog.Block("Desconectar pantallas", d, "23:00", 30));
        }
        return out;
    }

    /** Pone la propuesta y conserva los bloques tuyos que no choquen con ella. */
    public static VidaLog applySuggested(VidaLog log, List<VidaLog.Block> week) {
        List<VidaLog.Block> next = new ArrayList<>(week);
        for (VidaLog.Block b : log.blocks) {
            boolean clash = false;
            for (VidaLog.Block w : week) if (w.day == b.day && overlaps(w, b)) clash = true;
            if (!clash) next.add(b);
        }
        return log.withBlocks(next);
    }

    static boolean overlaps(VidaLog.Block a, VidaLog.Block b) {
        int as = minutes(a.time), bs = minutes(b.time);
        return as < bs + b.minutes && bs < as + a.minutes;
    }

    /** Un bloque concreto hecho en una fecha se guarda como un día cumplido con este id. */
    public static String blockId(VidaLog.Block b) { return "b:" + VidaConversation.normalize(b.title) + "@" + b.time; }
    public static boolean blockDone(VidaLog log, VidaLog.Block b, LocalDate date) { return checked(log, blockId(b), date); }
    public static VidaLog toggleBlock(VidaLog log, VidaLog.Block b, LocalDate date) { return toggle(log, blockId(b), date); }

    /** {hechos, planificados} de lunes a hoy en la semana de {@code today}. */
    public static int[] weekCompliance(VidaLog log, LocalDate today) {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int planned = 0, done = 0;
        for (VidaLog.Block b : log.blocks) {
            if (b.day > today.getDayOfWeek().getValue() || kind(b.title) == Kind.DESCANSO) continue;
            planned++;
            if (blockDone(log, b, monday.plusDays(b.day - 1))) done++;
        }
        return new int[]{done, planned};
    }

    /** Minutos planificados por tipo en la semana, para ver dónde va tu tiempo. */
    public static java.util.Map<Kind, Integer> minutesByKind(VidaLog log) {
        java.util.Map<Kind, Integer> out = new java.util.EnumMap<>(Kind.class);
        for (VidaLog.Block b : log.blocks) out.merge(kind(b.title), b.minutes, Integer::sum);
        return out;
    }

    public static VidaLog addBlocks(VidaLog log, String title, Set<Integer> days, String time, int minutes) {
        List<VidaLog.Block> next = new ArrayList<>(log.blocks);
        for (int d : days) next.add(new VidaLog.Block(title, d, time, minutes));
        return log.withBlocks(next);
    }

    public static VidaLog removeBlock(VidaLog log, VidaLog.Block target, boolean allDays) {
        List<VidaLog.Block> next = new ArrayList<>();
        for (VidaLog.Block o : log.blocks) {
            boolean same = o.title.equalsIgnoreCase(target.title) && (allDays || (o.day == target.day && o.time.equals(target.time)));
            if (!same) next.add(o);
        }
        return log.withBlocks(next);
    }

    public static VidaLog moveBlock(VidaLog log, VidaLog.Block target, String time, int minutes) {
        List<VidaLog.Block> next = new ArrayList<>();
        for (VidaLog.Block o : log.blocks)
            next.add(o == target || (o.day == target.day && o.time.equals(target.time) && o.title.equals(target.title))
                    ? new VidaLog.Block(o.title, o.day, time, minutes) : o);
        return log.withBlocks(next);
    }

    /** Reprograma un bloque y conserva su marca de hecho en la semana seleccionada. */
    public static VidaLog rescheduleBlock(VidaLog log, VidaLog.Block target, int newDay,
                                          String newTime, LocalDate weekMonday) {
        if (newDay < 1 || newDay > 7) throw new IllegalArgumentException("Día no válido.");
        int start = minutes(newTime);
        if (start + target.minutes > 24 * 60)
            throw new IllegalArgumentException("La actividad debe terminar antes de medianoche.");
        for (VidaLog.Block other : log.blocks) {
            if (other == target || other.day != newDay) continue;
            int otherStart = minutes(other.time);
            if (start < otherStart + other.minutes && otherStart < start + target.minutes)
                throw new IllegalArgumentException("Ese horario ya tiene otra actividad.");
        }
        List<VidaLog.Block> blocks = new ArrayList<>(log.blocks);
        int index = blocks.indexOf(target);
        if (index < 0) {
            for (int i = 0; i < blocks.size(); i++) {
                VidaLog.Block b = blocks.get(i);
                if (b.day == target.day && b.time.equals(target.time) && b.title.equals(target.title)) {
                    index = i; break;
                }
            }
        }
        if (index < 0) throw new IllegalArgumentException("Actividad no encontrada.");
        VidaLog.Block moved = new VidaLog.Block(target.title, newDay, newTime, target.minutes);
        blocks.set(index, moved);
        LocalDate oldDate = weekMonday.plusDays(target.day - 1);
        LocalDate newDate = weekMonday.plusDays(newDay - 1);
        List<VidaLog.HabitCheck> checks = new ArrayList<>();
        boolean wasDone = false;
        for (VidaLog.HabitCheck check : log.checks) {
            if (check.habitId.equals(blockId(target)) && check.date.equals(oldDate.toString())) {
                wasDone = true;
            } else checks.add(check);
        }
        if (wasDone) checks.add(new VidaLog.HabitCheck(blockId(moved), newDate.toString()));
        return log.withBlocks(blocks).withChecks(checks);
    }

    public static String hm(int minutes) { return String.format(Locale.ROOT, "%02d:%02d", (minutes / 60) % 24, minutes % 60); }

    // ---------------------------------------------------------------- utilidades

    public static String newId() { return UUID.randomUUID().toString().substring(0, 8); }

    public static int minutes(String hhmm) {
        String[] p = hhmm.trim().split("[:.]");
        int h = Integer.parseInt(p[0]), m = p.length > 1 ? Integer.parseInt(p[1]) : 0;
        if (h < 0 || h > 23 || m < 0 || m > 59) throw new IllegalArgumentException("Hora no válida: " + hhmm);
        return h * 60 + m;
    }

    private static int kindOrder(String kind) {
        for (int i = 0; i < MEAL_KINDS.length; i++) if (MEAL_KINDS[i].equals(kind)) return i;
        return MEAL_KINDS.length;
    }

    public static String dayName(int day) { return DAYS[day - 1]; }
    public static String cap(String s) { return s == null || s.isEmpty() ? "" : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1); }
}
