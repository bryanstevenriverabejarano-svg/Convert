package salve.core.vida;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lo que ayuda a terminar las cosas: la tarea más importante del día, proyectos con un límite de
 * activos, la revisión del domingo y los avisos del horario. Lógica pura y probada.
 */
public final class VidaCoach {
    private VidaCoach() {}

    /** Máximo de proyectos activos a la vez: para abrir otro hay que cerrar o pausar uno. */
    public static final int MAX_ACTIVE = 2;
    public static final String ACTIVE = "activo", PAUSED = "pausado", DONE = "hecho";

    // ---------------------------------------------------------------- tarea más importante

    public static VidaLog.Focus focusOn(VidaLog log, LocalDate day) {
        for (VidaLog.Focus f : log.focus) if (f.date.equals(day.toString())) return f;
        return null;
    }

    public static VidaLog setFocus(VidaLog log, LocalDate day, String text) {
        List<VidaLog.Focus> next = new ArrayList<>();
        for (VidaLog.Focus f : log.focus) if (!f.date.equals(day.toString())) next.add(f);
        if (text != null && !text.trim().isEmpty()) next.add(new VidaLog.Focus(day.toString(), text.trim(), false));
        next.sort(Comparator.comparing(f -> f.date));
        return log.withFocus(next);
    }

    public static VidaLog toggleFocus(VidaLog log, LocalDate day) {
        List<VidaLog.Focus> next = new ArrayList<>();
        for (VidaLog.Focus f : log.focus) next.add(f.date.equals(day.toString()) ? new VidaLog.Focus(f.date, f.text, !f.done) : f);
        return log.withFocus(next);
    }

    /** Días ganados (tarea principal hecha) en la semana de {@code day}. */
    public static int focusWinsThisWeek(VidaLog log, LocalDate day) {
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int n = 0;
        for (VidaLog.Focus f : log.focus) {
            LocalDate d = LocalDate.parse(f.date);
            if (f.done && !d.isBefore(monday) && !d.isAfter(monday.plusDays(6))) n++;
        }
        return n;
    }

    /** Días seguidos ganados terminando ayer u hoy. */
    public static int focusStreak(VidaLog log, LocalDate today) {
        LocalDate d = today;
        VidaLog.Focus f = focusOn(log, d);
        if (f == null || !f.done) d = d.minusDays(1);
        int n = 0;
        while (n < 3650) {
            VidaLog.Focus x = focusOn(log, d);
            if (x == null || !x.done) break;
            n++; d = d.minusDays(1);
        }
        return n;
    }

    /** Sugerencias para elegir la tarea del día: siguiente paso de cada proyecto activo y objetivos cercanos. */
    public static List<String> focusIdeas(VidaLog log, LocalDate today) {
        List<String> out = new ArrayList<>();
        for (VidaLog.Project j : log.projects) {
            if (!ACTIVE.equals(j.status)) continue;
            VidaLog.Project.Step s = nextStep(j);
            if (s != null) out.add(j.title + ": " + s.text);
        }
        for (VidaLog.Goal g : VidaPlanner.sortedGoals(log)) {
            if (g.done || g.due.isEmpty()) continue;
            if (!LocalDate.parse(g.due).isAfter(today.plusDays(21))) out.add(g.title);
        }
        return out;
    }

    // ---------------------------------------------------------------- proyectos

    public static int activeCount(VidaLog log) {
        int n = 0; for (VidaLog.Project j : log.projects) if (ACTIVE.equals(j.status)) n++;
        return n;
    }

    /** @throws IllegalArgumentException si ya hay {@link #MAX_ACTIVE} activos. */
    public static VidaLog addProject(VidaLog log, String title, List<String> steps, LocalDate today) {
        if (activeCount(log) >= MAX_ACTIVE)
            throw new IllegalArgumentException("Ya tienes " + MAX_ACTIVE + " proyectos activos. Termina o pausa uno antes de abrir otro.");
        List<VidaLog.Project.Step> list = new ArrayList<>();
        for (String s : steps) if (s != null && !s.trim().isEmpty()) list.add(new VidaLog.Project.Step(s.trim(), false));
        List<VidaLog.Project> next = new ArrayList<>(log.projects);
        next.add(new VidaLog.Project(VidaPlanner.newId(), title, ACTIVE, today.toString(), list));
        return log.withProjects(next);
    }

    public static VidaLog setStatus(VidaLog log, String id, String status) {
        if (ACTIVE.equals(status)) {
            for (VidaLog.Project j : log.projects)
                if (j.id.equals(id) && !ACTIVE.equals(j.status) && activeCount(log) >= MAX_ACTIVE)
                    throw new IllegalArgumentException("Ya tienes " + MAX_ACTIVE + " proyectos activos. Pausa uno para reactivar este.");
        }
        List<VidaLog.Project> next = new ArrayList<>();
        for (VidaLog.Project j : log.projects) next.add(j.id.equals(id) ? new VidaLog.Project(j.id, j.title, status, j.created, j.steps) : j);
        return log.withProjects(next);
    }

    public static VidaLog addStep(VidaLog log, String id, String text) {
        return editSteps(log, id, steps -> steps.add(new VidaLog.Project.Step(text.trim(), false)));
    }

    public static VidaLog toggleStep(VidaLog log, String id, int index) {
        return editSteps(log, id, steps -> {
            VidaLog.Project.Step s = steps.get(index);
            steps.set(index, new VidaLog.Project.Step(s.text, !s.done));
        });
    }

    public static VidaLog removeStep(VidaLog log, String id, int index) { return editSteps(log, id, steps -> steps.remove(index)); }

    public static VidaLog removeProject(VidaLog log, String id) {
        List<VidaLog.Project> next = new ArrayList<>();
        for (VidaLog.Project j : log.projects) if (!j.id.equals(id)) next.add(j);
        return log.withProjects(next);
    }

    private interface StepEdit { void apply(List<VidaLog.Project.Step> steps); }

    private static VidaLog editSteps(VidaLog log, String id, StepEdit edit) {
        List<VidaLog.Project> next = new ArrayList<>();
        for (VidaLog.Project j : log.projects) {
            if (!j.id.equals(id)) { next.add(j); continue; }
            List<VidaLog.Project.Step> steps = new ArrayList<>(j.steps);
            edit.apply(steps);
            next.add(new VidaLog.Project(j.id, j.title, j.status, j.created, steps));
        }
        return log.withProjects(next);
    }

    public static VidaLog.Project.Step nextStep(VidaLog.Project j) {
        for (VidaLog.Project.Step s : j.steps) if (!s.done) return s;
        return null;
    }

    /** {hechos, total} */
    public static int[] progress(VidaLog.Project j) {
        int d = 0; for (VidaLog.Project.Step s : j.steps) if (s.done) d++;
        return new int[]{d, j.steps.size()};
    }

    /** Primer proyecto: el vídeo «Me dicen Bray», que está casi terminado. Solo si no hay ninguno. */
    public static VidaLog seedProjects(VidaLog log, LocalDate today) {
        if (!log.projects.isEmpty()) return log;
        List<String> steps = new ArrayList<>();
        steps.add("Terminar escena pendiente 1");
        steps.add("Terminar escena pendiente 2");
        steps.add("Terminar escena pendiente 3");
        steps.add("Revisar el guion final (erratas y escena 2 más corta)");
        steps.add("Exportar y publicar en YouTube");
        steps.add("Cortar 3 reels para Instagram");
        return addProject(log, "Vídeo «Me dicen Bray»", steps, today);
    }

    // ---------------------------------------------------------------- revisión semanal

    public static LocalDate monday(LocalDate day) { return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }

    public static VidaLog.Review reviewFor(VidaLog log, LocalDate monday) {
        for (VidaLog.Review r : log.reviews) if (r.week.equals(monday.toString())) return r;
        return null;
    }

    public static VidaLog saveReview(VidaLog log, LocalDate monday, String good, String bad, String priority) {
        List<VidaLog.Review> next = new ArrayList<>();
        for (VidaLog.Review r : log.reviews) if (!r.week.equals(monday.toString())) next.add(r);
        next.add(new VidaLog.Review(monday.toString(), good, bad, priority));
        next.sort(Comparator.comparing(r -> r.week));
        return log.withReviews(next);
    }

    /** Prioridad elegida en la última revisión (la que guía esta semana), o null. */
    public static String currentPriority(VidaLog log, LocalDate today) {
        VidaLog.Review latest = null;
        for (VidaLog.Review r : log.reviews) if (!LocalDate.parse(r.week).isAfter(monday(today))) latest = r;
        return latest == null || latest.priority.isEmpty() ? null : latest.priority;
    }

    /** Resumen numérico de la semana de {@code day} para la revisión. */
    public static String weekReport(VidaLog log, LocalDate day) {
        LocalDate mon = monday(day), sun = mon.plusDays(6);
        java.util.Set<String> gymDays = new java.util.HashSet<>();
        double volume = 0;
        for (VidaLog.GymSet s : log.sets) {
            LocalDate d = LocalDate.parse(s.date);
            if (d.isBefore(mon) || d.isAfter(sun)) continue;
            gymDays.add(s.date); if (!"calentamiento".equals(s.type)) volume += s.volume();
        }
        List<VidaLog.SleepEntry> nights = new ArrayList<>();
        for (VidaLog.SleepEntry e : log.sleep) { LocalDate d = LocalDate.parse(e.date); if (!d.isBefore(mon) && !d.isAfter(sun)) nights.add(e); }
        int[] blocks = VidaPlanner.weekCompliance(log, day.isAfter(sun) ? sun : day);
        StringBuilder out = new StringBuilder();
        out.append("Gym: ").append(gymDays.size()).append(" días, ").append(VidaConversation.fmt(volume)).append(" kg de volumen\n");
        out.append("Sueño: ").append(nights.isEmpty() ? "sin registros" : "media " + VidaConversation.hours(VidaPlanner.averageMinutes(nights)) + " (" + nights.size() + " noches)").append('\n');
        out.append("Horario: ").append(blocks[0]).append(" de ").append(blocks[1]).append(" bloques hechos\n");
        out.append("Días ganados (tarea principal): ").append(focusWinsThisWeek(log, day)).append(" de 7\n");
        for (VidaLog.Habit h : log.habits)
            out.append("• ").append(h.name).append(": ").append(VidaPlanner.doneThisWeek(log, h.id, day)).append("/").append(h.perWeek).append('\n');
        for (VidaLog.Project j : log.projects) {
            if (!ACTIVE.equals(j.status)) continue;
            int[] p = progress(j);
            out.append("Proyecto ").append(j.title).append(": ").append(p[0]).append("/").append(p[1]).append(" pasos\n");
        }
        return out.toString().trim();
    }

    // ---------------------------------------------------------------- avisos

    public static final class Reminder {
        public final LocalDateTime at; public final String title, text;
        Reminder(LocalDateTime at, String title, String text) { this.at = at; this.title = title; this.text = text; }
    }

    /**
     * Siguiente aviso después de {@code now}: el resumen de la mañana a {@code morning} y cada
     * bloque del horario {@code leadMinutes} antes (los de descanso no avisan). Mira 8 días.
     */
    public static Reminder nextReminder(VidaLog log, LocalDateTime now, LocalTime morning, int leadMinutes) {
        Reminder best = null;
        for (int add = 0; add <= 7; add++) {
            LocalDate day = now.toLocalDate().plusDays(add);
            int dow = day.getDayOfWeek().getValue();
            if (morning != null) {
                LocalDateTime at = day.atTime(morning);
                if (at.isAfter(now) && (best == null || at.isBefore(best.at)))
                    best = new Reminder(at, "Tu día con Salve", morningText(log, day));
            }
            for (VidaLog.Block b : log.blocks) {
                if (b.day != dow || VidaPlanner.kind(b.title) == VidaPlanner.Kind.DESCANSO) continue;
                LocalDateTime start = day.atTime(LocalTime.parse(b.time));
                LocalDateTime at = start.minusMinutes(leadMinutes);
                if (!at.isAfter(now)) continue;
                if (best == null || at.isBefore(best.at))
                    best = new Reminder(at, b.title + " a las " + b.time, "Empieza en " + leadMinutes + " min · " + b.minutes + " min. Márcalo como hecho en Vida al terminar.");
            }
            if (best != null && best.at.toLocalDate().equals(day)) return best;
        }
        return best;
    }

    static String morningText(VidaLog log, LocalDate day) {
        int dow = day.getDayOfWeek().getValue();
        List<VidaLog.Block> blocks = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (b.day == dow && VidaPlanner.kind(b.title) != VidaPlanner.Kind.DESCANSO) blocks.add(b);
        blocks.sort(Comparator.comparing(b -> b.time));
        StringBuilder out = new StringBuilder();
        VidaLog.Focus f = focusOn(log, day);
        out.append(f == null ? "Elige tu tarea más importante de hoy. " : "Lo más importante: " + f.text + ". ");
        if (!blocks.isEmpty()) {
            out.append("Hoy: ");
            for (int i = 0; i < Math.min(4, blocks.size()); i++) { if (i > 0) out.append(", "); out.append(blocks.get(i).time).append(" ").append(blocks.get(i).title); }
            out.append(".");
        }
        String p = currentPriority(log, day);
        if (p != null) out.append(" Prioridad de la semana: ").append(p).append(".");
        return out.toString().trim();
    }
}
