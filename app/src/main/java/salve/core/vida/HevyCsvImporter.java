package salve.core.vida;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lee el CSV que exporta Hevy (Ajustes → exportar entrenamientos). Las columnas se buscan por
 * nombre, no por posición, para tolerar cambios de Hevy. Columnas usadas: title, start_time,
 * exercise_title, set_index, set_type, weight_kg, reps, distance_km, duration_seconds.
 * Solo exercise_title y start_time son obligatorias.
 */
public final class HevyCsvImporter {

    public static final class Result {
        public final List<VidaLog.GymSet> sets;
        public final int skipped;
        Result(List<VidaLog.GymSet> sets, int skipped) { this.sets = sets; this.skipped = skipped; }
    }

    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");

    private HevyCsvImporter() {}

    /** @throws IllegalArgumentException si el texto no parece un CSV de Hevy. */
    public static Result parse(String csv) {
        if (csv == null || csv.trim().isEmpty()) throw new IllegalArgumentException("El archivo está vacío.");
        List<List<String>> rows = rows(csv.startsWith("\uFEFF") ? csv.substring(1) : csv);
        if (rows.isEmpty()) throw new IllegalArgumentException("El archivo está vacío.");
        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) col.put(header.get(i).trim().toLowerCase(Locale.ROOT), i);
        if (!col.containsKey("exercise_title") || !col.containsKey("start_time"))
            throw new IllegalArgumentException("No parece un CSV de Hevy: faltan las columnas exercise_title y start_time.");
        List<VidaLog.GymSet> out = new ArrayList<>();
        // Hevy repite set_index si el mismo ejercicio aparece dos veces en un entreno: se numeran las apariciones.
        Map<String, Integer> seen = new HashMap<>();
        int skipped = 0;
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.size() == 1 && row.get(0).trim().isEmpty()) continue;
            String start = cell(row, col, "start_time");
            String exercise = cell(row, col, "exercise_title");
            String date = date(start);
            if (date == null || exercise.isEmpty()) { skipped++; continue; }
            int index = intOr(cell(row, col, "set_index"), r);
            String type = type(cell(row, col, "set_type"));
            try {
                out.add(new VidaLog.GymSet(date, cell(row, col, "title"), exercise, index, type,
                        dbl(cell(row, col, "weight_kg")), integer(cell(row, col, "reps")),
                        dbl(cell(row, col, "distance_km")), integer(cell(row, col, "duration_seconds")),
                        "hevy", key(seen, "hevy|" + start + "|" + exercise + "|" + index)));
            } catch (IllegalArgumentException bad) { skipped++; }
        }
        return new Result(out, skipped);
    }

    private static final Pattern DAY_MONTH_YEAR = Pattern.compile("^(\\d{1,2})\\s+([a-z\u00e1\u00e9\u00ed\u00f3\u00fa]{3,10})\\.?\\s+(\\d{4})");
    private static final String[][] MONTHS = {
            {"ene", "jan"}, {"feb"}, {"mar"}, {"abr", "apr"}, {"may"}, {"jun"},
            {"jul"}, {"ago", "aug"}, {"sep"}, {"oct"}, {"nov"}, {"dic", "dec"}};

    /**
     * Fecha ISO (yyyy-MM-dd) a partir de los formatos de Hevy: «12 nov 2026, 21:27» (español),
     * «12 Nov 2026, 21:27» (inglés) o ISO. Los meses se leen a mano: los nombres cortos de
     * Locale("es") cambian entre versiones de Android («sep» / «sept.»). null si no se reconoce.
     */
    static String date(String raw) {
        if (raw == null) return null;
        String text = raw.trim().toLowerCase(Locale.ROOT);
        Matcher iso = ISO_DATE.matcher(text);
        if (iso.find()) {
            try { return LocalDate.parse(iso.group(1)).toString(); } catch (DateTimeParseException ignored) { return null; }
        }
        Matcher m = DAY_MONTH_YEAR.matcher(text);
        if (!m.find()) return null;
        String month = m.group(2);
        for (int i = 0; i < MONTHS.length; i++)
            for (String prefix : MONTHS[i])
                if (month.startsWith(prefix)) {
                    try { return LocalDate.of(Integer.parseInt(m.group(3)), i + 1, Integer.parseInt(m.group(1))).toString(); }
                    catch (RuntimeException invalid) { return null; }
                }
        return null;
    }

    private static String key(Map<String, Integer> seen, String base) {
        int n = seen.merge(base, 1, Integer::sum);
        return n == 1 ? base : base + "#" + n;
    }

    private static String type(String raw) {
        String t = raw.trim().toLowerCase(Locale.ROOT);
        if (t.contains("fail")) return "fallo";
        if (t.contains("warm")) return "calentamiento";
        if (t.contains("drop")) return "dropset";
        return "normal";
    }

    private static String cell(List<String> row, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        return i == null || i >= row.size() ? "" : row.get(i).trim();
    }
    private static Double dbl(String s) {
        if (s.isEmpty()) return null;
        try { return Double.valueOf(s.replace(',', '.')); } catch (NumberFormatException e) { return null; }
    }
    private static Integer integer(String s) {
        if (s.isEmpty()) return null;
        try { return (int) Math.round(Double.parseDouble(s.replace(',', '.'))); } catch (NumberFormatException e) { return null; }
    }
    private static int intOr(String s, int fallback) { Integer v = integer(s); return v == null ? fallback : v; }

    /** CSV RFC 4180: comillas, comillas dobles escapadas y saltos de línea dentro de un campo. */
    static List<List<String>> rows(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { row.add(cur.toString()); cur.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                row.add(cur.toString()); cur.setLength(0); rows.add(row); row = new ArrayList<>();
            } else cur.append(c);
        }
        if (cur.length() > 0 || !row.isEmpty()) { row.add(cur.toString()); rows.add(row); }
        return rows;
    }
}
