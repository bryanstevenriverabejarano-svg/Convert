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

    public final List<GymSet> sets;
    public final List<SleepEntry> sleep;
    public final List<Meal> meals;
    public final List<Block> blocks;

    public VidaLog() { this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()); }

    public VidaLog(List<GymSet> sets, List<SleepEntry> sleep, List<Meal> meals, List<Block> blocks) {
        this.sets = Collections.unmodifiableList(new ArrayList<>(sets));
        this.sleep = Collections.unmodifiableList(new ArrayList<>(sleep));
        this.meals = Collections.unmodifiableList(new ArrayList<>(meals));
        this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
    }

    public boolean isEmpty() { return sets.isEmpty() && sleep.isEmpty() && meals.isEmpty() && blocks.isEmpty(); }

    public VidaLog withSets(List<GymSet> next) { return new VidaLog(next, sleep, meals, blocks); }
    public VidaLog withSleep(List<SleepEntry> next) { return new VidaLog(sets, next, meals, blocks); }
    public VidaLog withMeals(List<Meal> next) { return new VidaLog(sets, sleep, next, blocks); }
    public VidaLog withBlocks(List<Block> next) { return new VidaLog(sets, sleep, meals, next); }

    // ---------------------------------------------------------------- serialización

    public String serialize() {
        StringBuilder out = new StringBuilder("vida\t1\n");
        for (GymSet s : sets) line(out, "S", s.date, s.workout, s.exercise, String.valueOf(s.setIndex), s.type,
                num(s.kg), num(s.reps), num(s.km), num(s.seconds), s.source, s.key);
        for (SleepEntry e : sleep) line(out, "D", e.date, e.bed, e.wake, String.valueOf(e.minutes));
        for (Meal m : meals) line(out, "C", m.date, m.time, m.kind, m.text);
        for (Block b : blocks) line(out, "B", b.title, String.valueOf(b.day), b.time, String.valueOf(b.minutes));
        return out.toString();
    }

    public static VidaLog parse(String text) {
        if (text == null || text.trim().isEmpty()) return new VidaLog();
        String[] lines = text.split("\n");
        if (!lines[0].startsWith("vida\t")) throw new IllegalArgumentException("No es un registro de Vida.");
        List<GymSet> sets = new ArrayList<>(); List<SleepEntry> sleep = new ArrayList<>();
        List<Meal> meals = new ArrayList<>(); List<Block> blocks = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            String[] f = fields(lines[i]);
            switch (f[0]) {
                case "S": sets.add(new GymSet(f[1], f[2], f[3], Integer.parseInt(f[4]), f[5], dbl(f[6]), integer(f[7]),
                        dbl(f[8]), integer(f[9]), f[10], f[11])); break;
                case "D": sleep.add(new SleepEntry(f[1], f[2], f[3], Integer.parseInt(f[4]))); break;
                case "C": meals.add(new Meal(f[1], f[2], f[3], f[4])); break;
                case "B": blocks.add(new Block(f[1], Integer.parseInt(f[2]), f[3], Integer.parseInt(f[4]))); break;
                default: break; // tipos futuros: se ignoran sin perder el resto
            }
        }
        return new VidaLog(sets, sleep, meals, blocks);
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
