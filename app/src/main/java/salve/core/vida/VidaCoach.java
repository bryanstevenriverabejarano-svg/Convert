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

    /** Consejos basados en pasos y antigüedad; no se infiere inactividad sin historial. */
    public static List<String> projectInsights(VidaLog log, LocalDate today) {
        List<String> out = new ArrayList<>();
        for (VidaLog.Project project : log.projects) {
            if (!ACTIVE.equals(project.status)) continue;
            int[] progress = progress(project);
            if (progress[1] == 0 || progress[0] == progress[1]) continue;
            VidaLog.Project.Step next = nextStep(project);
            if (progress[1] >= 4 && progress[0] * 100 >= progress[1] * 80)
                out.add("Modo Terminar: " + project.title + ". Completa «" + next.text + "» antes de añadir más mejoras.");
            else if (java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(project.created), today) >= 14)
                out.add("Revisa " + project.title + ": lleva tiempo abierto. ¿Qué dificulta «" + next.text + "»?");
        }
        if (activeCount(log) >= MAX_ACTIVE)
            out.add("Ya hay dos proyectos activos: pausa uno antes de iniciar otro.");
        return out;
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
        for (String insight : projectInsights(log, day)) out.append("• ").append(insight).append('\n');
        return out.toString().trim();
    }

    // ---------------------------------------------------------------- plan del día

    /** Una actividad propuesta para hoy, con su motivo (para «Explicación del plan»). */
    public static final class PlanItem {
        public final String title, reason, kind; public final int minutes;
        PlanItem(String title, int minutes, String kind, String reason) { this.title = title; this.minutes = minutes; this.kind = kind; this.reason = reason; }
    }

    /**
     * Plan realista para {@code day} a partir de datos registrados: el siguiente paso del proyecto
     * activo (o la tarea principal), los hábitos que van por detrás de su meta semanal y el sueño.
     * Como mucho 4 actividades; se adapta solo porque lee lo que ya cumpliste esta semana.
     */
    public static List<PlanItem> dayPlan(VidaLog log, LocalDate day) {
        List<PlanItem> out = new ArrayList<>();
        // Priorizar la última noche conocida, no ocultar una noche de 3 h con la media semanal.
        List<VidaLog.SleepEntry> recent = VidaPlanner.lastNights(log, 7);
        VidaLog.SleepEntry latest = null;
        for (VidaLog.SleepEntry e : recent)
            if (!LocalDate.parse(e.date).isAfter(day)
                    && (latest == null || e.date.compareTo(latest.date) > 0)) latest = e;
        // No aplicar un registro antiguo como si describiera el descanso actual.
        int sleepMinutes = latest != null && !LocalDate.parse(latest.date).isBefore(day.minusDays(1))
                ? latest.minutes : 0;
        boolean severeSleepDebt = sleepMinutes > 0 && sleepMinutes < 4 * 60;
        boolean lowSleep = sleepMinutes > 0 && sleepMinutes < 6 * 60;
        if (severeSleepDebt)
            out.add(new PlanItem("Priorizar descanso y recuperación", 20, "descanso",
                    "La última noche registrada fue de " + VidaConversation.hours(sleepMinutes)
                    + ". Hoy evita exigirte y considera consultar con un profesional si se repite."));
        VidaLog.Focus f = focusOn(log, day);
        int daysLeft = 8 - day.getDayOfWeek().getValue(); // contando hoy
        if (f != null && !f.done) out.add(new PlanItem(f.text, lowSleep ? 20 : 90, "proyecto", "Es tu tarea más importante de hoy: terminarla gana el día."));
        else if (f == null) {
            for (VidaLog.Project j : log.projects) {
                if (!ACTIVE.equals(j.status)) continue;
                VidaLog.Project.Step s = nextStep(j);
                if (s == null) continue;
                int[] p = progress(j);
                String due = "";
                for (VidaLog.Goal g : log.goals)
                    if (!g.done && !g.due.isEmpty() && overlapWords(g.title, j.title)) {
                        long d = java.time.temporal.ChronoUnit.DAYS.between(day, LocalDate.parse(g.due));
                        due = d >= 0 ? " y su objetivo vence en " + d + " días" : " y su objetivo ya venció";
                    }
                out.add(new PlanItem(s.text, lowSleep ? 20 : 90, "proyecto", "Es el siguiente paso de «" + j.title + "» (" + p[0] + "/" + p[1] + " pasos)" + due + "."));
                break;
            }
        }
        for (VidaLog.Habit h : log.habits) {
            if (out.size() >= 4) break;
            if (lowSleep && h.id.equals("gym")) continue;
            if (h.id.equals("sueno") || h.id.equals("revision") && day.getDayOfWeek() != DayOfWeek.SUNDAY) continue;
            if (VidaPlanner.checked(log, h.id, day)) continue;
            int done = VidaPlanner.doneThisWeek(log, h.id, day), missing = h.perWeek - done;
            if (missing <= 0) continue;
            // Solo si va por detrás: lo que falta no cabe en los días que quedan sin hacerlo hoy.
            boolean behind = missing >= daysLeft || h.perWeek >= 4 || missing == 1 && daysLeft <= 3;
            if (!behind) continue;
            int minutes = lowSleep ? 20 : h.id.equals("gym") ? 60 : h.id.equals("mates") ? 60 : h.id.equals("proyecto") ? 120 : h.id.equals("revision") ? 20 : 45;
            String title = h.id.equals("gym") ? "Entrenamiento (Gym)" : h.name;
            out.add(new PlanItem(title, minutes, h.id.equals("gym") ? "gym" : h.id.equals("mates") ? "estudio" : "habito",
                    "Llevas " + done + " de " + h.perWeek + " esta semana y quedan " + daysLeft + (daysLeft == 1 ? " día." : " días.")));
        }
        int avg = VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 7));
        if (!severeSleepDebt && out.size() < 4 && avg > 0 && avg < 7 * 60)
            out.add(new PlanItem("Preparar el sueño (pantallas fuera)", 15, "descanso",
                    "Tu media de sueño es " + VidaConversation.hours(avg) + "; dormir 7 h mejora el resto del plan."));
        return out;
    }

    /** Texto breve del plan para la tarjeta «Salve te recomienda». */
    public static String planSummary(List<PlanItem> plan) {
        if (plan.isEmpty()) return "Vas al día con tus metas de esta semana. Elige una tarea principal o adelanta un paso de tu proyecto.";
        StringBuilder out = new StringBuilder("Hoy puedes avanzar en «").append(plan.get(0).title).append("»");
        boolean study = false, gym = false;
        for (PlanItem p : plan) { if (p.kind.equals("estudio")) study = true; if (p.kind.equals("gym")) gym = true; }
        if (study && gym) out.append(", mantener tu rutina de estudio y entrenar");
        else if (study) out.append(" y mantener tu rutina de estudio");
        else if (gym) out.append(" y entrenar");
        return out.append(". He creado un plan equilibrado según lo que llevas esta semana.").toString();
    }

    /** Explicación paso a paso (sin modelo): cada actividad con su motivo. */
    public static String planReasons(List<PlanItem> plan) {
        StringBuilder out = new StringBuilder();
        for (PlanItem p : plan) out.append("• ").append(p.title).append(" (≈ ").append(duration(p.minutes)).append("): ").append(p.reason).append('\n');
        return out.toString().trim();
    }

    /**
     * Aplica el plan: fija la primera actividad como tarea principal (si no hay) y añade a la agenda
     * de ese día de la semana las que aún no tienen bloque, en huecos libres entre 08:00 y 23:00
     * (el gym prefiere la tarde). No mueve ni borra nada de lo que ya tenías.
     */
    public static VidaLog applyPlan(VidaLog log, LocalDate day, List<PlanItem> plan) {
        VidaLog out = log;
        if (focusOn(out, day) == null && !plan.isEmpty()) out = setFocus(out, day, plan.get(0).title);
        int dow = day.getDayOfWeek().getValue();
        List<int[]> busy = new ArrayList<>();
        for (VidaLog.Block b : out.blocks) if (b.day == dow) { int s0 = VidaPlanner.minutes(b.time); busy.add(new int[]{s0, s0 + b.minutes}); }
        List<VidaLog.Block> next = new ArrayList<>(out.blocks);
        for (PlanItem p : plan) {
            boolean exists = false;
            for (VidaLog.Block b : out.blocks)
                if (b.day == dow && (VidaConversation.normalize(b.title).equals(VidaConversation.normalize(p.title))
                        || p.kind.equals("gym") && VidaPlanner.kind(b.title) == VidaPlanner.Kind.GYM)) exists = true;
            if (exists) continue;
            int start = freeSlot(busy, p.minutes, p.kind.equals("gym") ? 17 * 60 : p.kind.equals("descanso") ? 22 * 60 + 30 : 8 * 60);
            if (start < 0) start = freeSlot(busy, p.minutes, 8 * 60);
            if (start < 0) continue;
            busy.add(new int[]{start, start + p.minutes});
            next.add(new VidaLog.Block(p.title, dow, VidaPlanner.hm(start), p.minutes));
        }
        return out.withBlocks(next);
    }

    /** Primer hueco de {@code minutes} a partir de {@code from} (en tramos de 15 min) antes de las 23:30. */
    static int freeSlot(List<int[]> busy, int minutes, int from) {
        for (int t = from; t + minutes <= 23 * 60 + 30; t += 15) {
            boolean clash = false;
            for (int[] b : busy) if (t < b[1] && b[0] < t + minutes) { clash = true; break; }
            if (!clash) return t;
        }
        return -1;
    }

    public static String duration(int minutes) {
        if (minutes < 60) return minutes + " min";
        return minutes / 60 + " h" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " min");
    }

    private static boolean overlapWords(String a, String b) {
        String na = VidaConversation.normalize(a), nb = VidaConversation.normalize(b).replaceAll("[^a-z0-9 ]", " ");
        for (String w : nb.split("\\s+")) if (w.length() > 4 && na.contains(w)) return true;
        return false;
    }

    /** Puntos de un día (hábitos cumplidos + bloques hechos + tarea principal), para la gráfica diaria. */
    public static int dayScore(VidaLog log, LocalDate day) {
        int n = 0;
        for (VidaLog.Habit h : log.habits) if (VidaPlanner.checked(log, h.id, day)) n++;
        for (VidaLog.Block b : log.blocks) if (b.day == day.getDayOfWeek().getValue() && VidaPlanner.blockDone(log, b, day)) n++;
        VidaLog.Focus f = focusOn(log, day);
        if (f != null && f.done) n++;
        return n;
    }

    /** Cambio porcentual de una medida entre la semana anterior y esta (null si no hay base). */
    public static Integer change(double before, double now) {
        if (before <= 0) return now > 0 ? null : 0;
        return (int) Math.round((now - before) * 100 / before);
    }

    /** Proyecto activo cuyo siguiente paso es {@code text} (la tarea principal sale de ahí), o null. */
    public static VidaLog.Project projectOf(VidaLog log, String text) {
        if (text == null) return null;
        for (VidaLog.Project j : log.projects) {
            VidaLog.Project.Step s = nextStep(j);
            if (s != null && s.text.equals(text)) return j;
        }
        return null;
    }

    /**
     * Marca o desmarca la tarea principal. Si era el siguiente paso de un proyecto, al completarla
     * también se marca ese paso, así el progreso del proyecto avanza solo.
     */
    public static VidaLog completeFocus(VidaLog log, LocalDate day) {
        VidaLog.Focus f = focusOn(log, day);
        if (f == null) return log;
        VidaLog.Project j = f.done ? null : projectOf(log, f.text);
        VidaLog out = toggleFocus(log, day);
        if (j != null) out = toggleStep(out, j.id, j.steps.indexOf(nextStep(j)));
        return out;
    }

    /**
     * Medidas de la semana de {@code day} hasta ese mismo día de la semana (para comparar semanas
     * de forma justa): {hábitos marcados, entrenos, media de sueño en minutos, días ganados}.
     */
    public static int[] weekStats(VidaLog log, LocalDate day) {
        LocalDate mon = monday(day);
        int habits = 0, gym = 0, wins = 0, sleepSum = 0, nights = 0;
        for (LocalDate d = mon; !d.isAfter(day); d = d.plusDays(1)) {
            for (VidaLog.Habit h : log.habits) if (!h.id.equals("gym") && VidaPlanner.checked(log, h.id, d)) habits++;
            if (VidaPlanner.checked(log, "gym", d)) gym++;
            VidaLog.Focus f = focusOn(log, d);
            if (f != null && f.done) wins++;
            for (VidaLog.SleepEntry e : log.sleep) if (e.date.equals(d.toString())) { sleepSum += e.minutes; nights++; }
        }
        return new int[]{habits, gym, nights == 0 ? 0 : sleepSum / nights, wins};
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
