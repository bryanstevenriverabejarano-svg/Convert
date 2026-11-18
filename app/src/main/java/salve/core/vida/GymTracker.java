package salve.core.vida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lógica del registro de entrenos al estilo Hevy, sin Android: rutinas a partir del historial,
 * columna «anterior», récords personales y resumen al terminar. La pantalla solo la muestra.
 */
public final class GymTracker {
    private GymTracker() {}

    /** Una serie del entreno en curso. kg/reps null = sin rellenar. */
    public static final class LiveSet {
        public String type = "normal"; // normal | calentamiento | fallo | dropset
        public Double kg; public Integer reps;
        /** Cardio (elíptica, cinta): se copian de la última vez; la pantalla no los edita aún. */
        public Double km; public Integer seconds;
        public boolean done;
        public boolean hasData() { return kg != null || reps != null || km != null || seconds != null; }
    }

    /** Un ejercicio del entreno en curso. */
    public static final class LiveExercise {
        public final String name;
        public int restSeconds = 90;
        public final List<LiveSet> sets = new ArrayList<>();
        public LiveExercise(String name) { this.name = name; }
    }

    /** Plantilla: el título de un entreno y sus ejercicios con las series de la última vez. */
    public static final class Routine {
        public final String title, lastDate;
        public final int sessions;
        public final List<String> exercises;
        Routine(String title, String lastDate, int sessions, List<String> exercises) {
            this.title = title; this.lastDate = lastDate; this.sessions = sessions; this.exercises = exercises;
        }
    }

    /** Un entreno pasado para el historial. */
    public static final class Session {
        public final String date, title;
        public final int sets;
        public final double volume;
        public final List<String> exercises;
        Session(String date, String title, int sets, double volume, List<String> exercises) {
            this.date = date; this.title = title; this.sets = sets; this.volume = volume; this.exercises = exercises;
        }
    }

    public static final class Summary {
        public final int sets, exercises;
        public final double volume;
        public final List<String> records;
        Summary(int sets, int exercises, double volume, List<String> records) {
            this.sets = sets; this.exercises = exercises; this.volume = volume; this.records = records;
        }
    }

    // ---------------------------------------------------------------- historial

    /** Entrenos agrupados por fecha + título, del más reciente al más antiguo. */
    public static List<Session> sessions(VidaLog log) {
        Map<String, List<VidaLog.GymSet>> groups = group(log);
        List<Session> out = new ArrayList<>();
        for (List<VidaLog.GymSet> sets : groups.values()) {
            double volume = 0; int working = 0;
            List<String> names = new ArrayList<>();
            for (VidaLog.GymSet s : sets) {
                if (!"calentamiento".equals(s.type)) { volume += s.volume(); working++; }
                if (!names.contains(s.exercise)) names.add(s.exercise);
            }
            VidaLog.GymSet first = sets.get(0);
            out.add(new Session(first.date, first.workout.isEmpty() ? "Entreno" : first.workout, working, volume, names));
        }
        out.sort(Comparator.comparing((Session s) -> s.date).reversed());
        return out;
    }

    /**
     * Rutinas: un título por plantilla. Los ejercicios salen de la sesión más completa de las
     * tres últimas (si un día te fuiste antes, la rutina no pierde ejercicios). Las usadas más
     * recientemente primero.
     */
    public static List<Routine> routines(VidaLog log) {
        Map<String, List<Session>> byTitle = new LinkedHashMap<>();
        for (Session s : sessions(log)) {
            if (s.title.equals("Entreno")) continue;
            byTitle.computeIfAbsent(s.title, k -> new ArrayList<>()).add(s);
        }
        List<Routine> out = new ArrayList<>();
        for (Map.Entry<String, List<Session>> e : byTitle.entrySet()) {
            List<Session> list = e.getValue();
            Session fullest = list.get(0);
            for (int i = 1; i < Math.min(3, list.size()); i++)
                if (list.get(i).exercises.size() > fullest.exercises.size()) fullest = list.get(i);
            out.add(new Routine(e.getKey(), list.get(0).date, list.size(), fullest.exercises));
        }
        out.sort(Comparator.comparing((Routine r) -> r.lastDate, Comparator.reverseOrder()).thenComparingInt(r -> -r.sessions));
        return out;
    }

    /** Ejercicios conocidos, los más usados primero (para el buscador de «añadir ejercicio»). */
    public static List<String> exercises(VidaLog log) {
        Map<String, Integer> count = new HashMap<>();
        for (VidaLog.GymSet s : log.sets) count.merge(s.exercise, 1, Integer::sum);
        List<String> out = new ArrayList<>(count.keySet());
        out.sort(Comparator.comparingInt((String n) -> -count.get(n)).thenComparing(n -> n.toLowerCase(Locale.ROOT)));
        return out;
    }

    /** Series de la última sesión de un ejercicio, en orden (columna «Anterior»). */
    public static List<VidaLog.GymSet> previous(VidaLog log, String exercise) {
        String lastDate = null;
        for (VidaLog.GymSet s : log.sets)
            if (s.exercise.equals(exercise) && (lastDate == null || s.date.compareTo(lastDate) > 0)) lastDate = s.date;
        List<VidaLog.GymSet> out = new ArrayList<>();
        if (lastDate == null) return out;
        for (VidaLog.GymSet s : log.sets) if (s.exercise.equals(exercise) && s.date.equals(lastDate)) out.add(s);
        out.sort(Comparator.comparingInt(s -> s.setIndex));
        return out;
    }

    /** Entreno nuevo a partir de una rutina: mismas series que la última vez, con sus pesos y reps. */
    public static List<LiveExercise> start(VidaLog log, Routine routine) {
        List<LiveExercise> out = new ArrayList<>();
        if (routine == null) return out;
        for (String name : routine.exercises) out.add(prefilled(log, name));
        return out;
    }

    /** Ejercicio con tantas series como la última vez (mínimo una), copiando peso, reps y tipo. */
    public static LiveExercise prefilled(VidaLog log, String name) {
        LiveExercise ex = new LiveExercise(name);
        for (VidaLog.GymSet p : previous(log, name)) {
            LiveSet set = new LiveSet();
            set.type = p.type; set.kg = p.kg; set.reps = p.reps; set.km = p.km; set.seconds = p.seconds;
            ex.sets.add(set);
        }
        if (ex.sets.isEmpty()) ex.sets.add(new LiveSet());
        return ex;
    }

    /** Un punto de la gráfica de un ejercicio: mejor serie de la sesión. */
    public static final class Point {
        public final String date; public final double maxKg, oneRm, volume; public final int bestReps;
        Point(String date, double maxKg, double oneRm, double volume, int bestReps) {
            this.date = date; this.maxKg = maxKg; this.oneRm = oneRm; this.volume = volume; this.bestReps = bestReps;
        }
    }

    /** Evolución de un ejercicio por sesión (sin calentamientos), de la más antigua a la más reciente. */
    public static List<Point> progress(VidaLog log, String exercise) {
        Map<String, double[]> byDate = new TreeMap<>(); // maxKg, oneRm, volume, repsAtMax
        for (VidaLog.GymSet s : log.sets) {
            if (!s.exercise.equals(exercise) || "calentamiento".equals(s.type) || s.kg == null || s.reps == null) continue;
            double[] p = byDate.computeIfAbsent(s.date, k -> new double[]{0, 0, 0, 0});
            if (s.kg > p[0] || (s.kg == p[0] && s.reps > p[3])) { p[0] = s.kg; p[3] = s.reps; }
            p[1] = Math.max(p[1], estimatedOneRepMax(s.kg, s.reps));
            p[2] += s.volume();
        }
        List<Point> out = new ArrayList<>();
        for (Map.Entry<String, double[]> e : byDate.entrySet())
            out.add(new Point(e.getKey(), e.getValue()[0], e.getValue()[1], e.getValue()[2], (int) e.getValue()[3]));
        return out;
    }

    /** Ejercicios con peso, ordenados por número de sesiones (los que más haces primero). */
    public static List<String> trackedExercises(VidaLog log) {
        Map<String, java.util.Set<String>> dates = new HashMap<>();
        for (VidaLog.GymSet s : log.sets) if (s.kg != null && s.reps != null) dates.computeIfAbsent(s.exercise, k -> new java.util.HashSet<>()).add(s.date);
        List<String> out = new ArrayList<>(dates.keySet());
        out.sort(Comparator.comparingInt((String n) -> -dates.get(n).size()).thenComparing(n -> n));
        return out;
    }

    public static int sessionCount(VidaLog log, String exercise) {
        java.util.Set<String> d = new java.util.HashSet<>();
        for (VidaLog.GymSet s : log.sets) if (s.exercise.equals(exercise)) d.add(s.date);
        return d.size();
    }

    // ---------------------------------------------------------------- récords

    /** 1RM estimado (Epley); solo con 1–12 reps, donde la fórmula es razonable. */
    public static double estimatedOneRepMax(double kg, int reps) {
        if (reps <= 0 || reps > 12) return 0;
        return reps == 1 ? kg : kg * (1 + reps / 30.0);
    }

    /** Mejor peso y mejor 1RM estimado del historial para un ejercicio (sin calentamientos). */
    public static double[] bests(VidaLog log, String exercise) {
        double heaviest = 0, oneRm = 0;
        for (VidaLog.GymSet s : log.sets) {
            if (!s.exercise.equals(exercise) || s.kg == null || s.reps == null || "calentamiento".equals(s.type)) continue;
            heaviest = Math.max(heaviest, s.kg);
            oneRm = Math.max(oneRm, estimatedOneRepMax(s.kg, s.reps));
        }
        return new double[]{heaviest, oneRm};
    }

    /** Récord en vivo al marcar una serie, o null. Nunca con historial vacío ni calentamientos. */
    public static String liveRecord(VidaLog log, String exercise, LiveSet set) {
        if (set.kg == null || set.reps == null || "calentamiento".equals(set.type)) return null;
        double[] best = bests(log, exercise);
        if (best[0] == 0 && best[1] == 0) return null;
        if (set.kg > best[0]) return "¡Récord de peso en " + exercise + ": " + VidaConversation.fmt(set.kg) + " kg!";
        double rm = estimatedOneRepMax(set.kg, set.reps);
        if (rm > best[1] + 0.01) return "¡Récord de 1RM estimado en " + exercise + ": " + VidaConversation.fmt(Math.round(rm * 10) / 10.0) + " kg!";
        return null;
    }

    // ---------------------------------------------------------------- terminar

    /** Series completadas y válidas, listas para guardar. */
    public static List<VidaLog.GymSet> toSets(String date, String startStamp, String title, List<LiveExercise> workout) {
        List<VidaLog.GymSet> out = new ArrayList<>();
        for (LiveExercise ex : workout) {
            int index = 0;
            for (LiveSet s : ex.sets) {
                if (!s.done || !s.hasData()) continue;
                out.add(new VidaLog.GymSet(date, title, ex.name, index, s.type, s.kg, s.reps, s.km, s.seconds, "salve",
                        "salve|" + startStamp + "|" + ex.name + "|" + index));
                index++;
            }
        }
        return out;
    }

    public static Summary summarize(VidaLog before, List<VidaLog.GymSet> done) {
        double volume = 0; int working = 0;
        List<String> names = new ArrayList<>();
        Map<String, double[]> beats = new TreeMap<>();
        for (VidaLog.GymSet s : done) {
            if (!names.contains(s.exercise)) names.add(s.exercise);
            if ("calentamiento".equals(s.type)) continue;
            working++; volume += s.volume();
            if (s.kg == null || s.reps == null) continue;
            double[] best = bests(before, s.exercise);
            if (best[0] == 0 && best[1] == 0) continue; // primera vez: no es récord, es el punto de partida
            double[] mine = beats.computeIfAbsent(s.exercise, k -> new double[]{0, 0});
            if (s.kg > best[0]) mine[0] = Math.max(mine[0], s.kg);
            double rm = estimatedOneRepMax(s.kg, s.reps);
            if (rm > best[1] + 0.01) mine[1] = Math.max(mine[1], rm);
        }
        List<String> records = new ArrayList<>();
        for (Map.Entry<String, double[]> e : beats.entrySet()) {
            if (e.getValue()[0] > 0) records.add(e.getKey() + ": peso máximo " + VidaConversation.fmt(e.getValue()[0]) + " kg");
            else if (e.getValue()[1] > 0) records.add(e.getKey() + ": 1RM estimado " + VidaConversation.fmt(Math.round(e.getValue()[1] * 10) / 10.0) + " kg");
        }
        return new Summary(working, names.size(), volume, Collections.unmodifiableList(records));
    }

    // ---------------------------------------------------------------- borrador

    /** Entreno en curso guardado a cada cambio: si Android cierra la app, se recupera tal cual. */
    public static String draftToText(String title, long startMillis, List<LiveExercise> workout) {
        StringBuilder out = new StringBuilder("W\t").append(clean(title)).append('\t').append(startMillis).append('\n');
        for (LiveExercise ex : workout) {
            out.append("E\t").append(clean(ex.name)).append('\t').append(ex.restSeconds).append('\n');
            for (LiveSet s : ex.sets)
                out.append("S\t").append(s.type).append('\t').append(s.kg == null ? "" : s.kg).append('\t')
                        .append(s.reps == null ? "" : s.reps).append('\t').append(s.done ? 1 : 0).append('\t')
                        .append(s.km == null ? "" : s.km).append('\t').append(s.seconds == null ? "" : s.seconds).append('\n');
        }
        return out.toString();
    }

    public static final class Draft {
        public String title = "Entreno"; public long startMillis;
        public final List<LiveExercise> exercises = new ArrayList<>();
    }

    /** null si el texto no es un borrador válido. */
    public static Draft draftFromText(String text) {
        if (text == null || !text.startsWith("W\t")) return null;
        Draft d = new Draft();
        LiveExercise current = null;
        try {
            for (String line : text.split("\n")) {
                String[] f = line.split("\t", -1);
                switch (f[0]) {
                    case "W": d.title = f[1].isEmpty() ? "Entreno" : f[1]; d.startMillis = Long.parseLong(f[2]); break;
                    case "E": current = new LiveExercise(f[1]); current.restSeconds = Integer.parseInt(f[2]); d.exercises.add(current); break;
                    case "S":
                        if (current == null) return null;
                        LiveSet s = new LiveSet(); s.type = f[1];
                        s.kg = f[2].isEmpty() ? null : Double.valueOf(f[2]);
                        s.reps = f[3].isEmpty() ? null : Integer.valueOf(f[3]);
                        s.done = "1".equals(f[4]);
                        if (f.length > 6) { s.km = f[5].isEmpty() ? null : Double.valueOf(f[5]); s.seconds = f[6].isEmpty() ? null : Integer.valueOf(f[6]); }
                        current.sets.add(s); break;
                    default: break;
                }
            }
        } catch (RuntimeException invalid) { return null; }
        return d;
    }

    private static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' ').trim(); }

    private static Map<String, List<VidaLog.GymSet>> group(VidaLog log) {
        Map<String, List<VidaLog.GymSet>> groups = new LinkedHashMap<>();
        for (VidaLog.GymSet s : log.sets) groups.computeIfAbsent(s.date + "|" + s.workout, k -> new ArrayList<>()).add(s);
        return groups;
    }
}
