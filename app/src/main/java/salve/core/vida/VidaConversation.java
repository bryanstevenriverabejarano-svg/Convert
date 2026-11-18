package salve.core.vida;

import java.text.Normalizer;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diálogo determinista en español del módulo Vida. No llama al modelo ni guarda nada:
 * devuelve el registro siguiente y el servicio lo persiste antes de responder.
 * Solo frases explícitas cambian datos; las consultas nunca los modifican.
 */
public final class VidaConversation {

    private static final String NUM = "(\\d{1,4}(?:[.,]\\d{1,2})?)";
    private static final Pattern SET = Pattern.compile(
            "^(?:anota |apunta |registra )?(?:una )?(?:serie (?:de |en )?)?([a-zñ][a-zñ0-9 ()/-]{2,60}?)[, ]+" + NUM
                    + " ?(?:kg|kilos?)[, ]*(?:x|por|a|y|con)? ?(\\d{1,3}) ?(?:reps?|repeticiones)(?: al fallo)?$");
    private static final Pattern SLEEP = Pattern.compile(
            "^(?:anoche |ayer )?me (?:acoste|dormi) a las? (\\d{1,2})(?:[:.](\\d{2})| y (media|cuarto))?(?: de la (?:noche|madrugada|manana))?"
                    + "(?:,)? y me (?:levante|desperte) a las? (\\d{1,2})(?:[:.](\\d{2})| y (media|cuarto))?(?: de la (?:manana|tarde))?$");
    private static final Pattern MEAL = Pattern.compile("^(?:hoy )?(?:he |ya he )?(desayunado|comido|almorzado|merendado|cenado) (.{2,200})$");
    private static final Pattern BLOCK = Pattern.compile(
            "^(?:anade|pon|agrega|mete) (?:al|a mi|en mi) horario (.{2,60}?) (?:los|el|cada) (.+?) a las (\\d{1,2})(?:[:.](\\d{2}))?"
                    + "(?: (?:durante|de|por) (\\d{1,3}) ?(minutos?|min|horas?|h))?$");
    private static final Pattern REMOVE_BLOCK = Pattern.compile("^(?:quita|borra|elimina) (.{2,60}) (?:del|de mi) horario$");
    private static final Pattern HISTORY = Pattern.compile("^(?:historial|progreso|como voy) (?:de|en|con) (?:el |la |mi )?(.{3,60})$");
    private static final Pattern TODAY = Pattern.compile("^(?:que tengo hoy|mi (?:dia|horario) de hoy|plan de hoy|que me toca hoy)$");
    private static final Pattern WEEK = Pattern.compile("^(?:resumen (?:de vida|semanal|de la semana)|como voy esta semana|estadisticas de vida|mis estadisticas)$");
    private static final Pattern SCHEDULE = Pattern.compile("^(?:mi horario|ensename mi horario|muestra mi horario|horario semanal)$");
    private static final Pattern HELP = Pattern.compile("^(?:modulo vida|ayuda (?:de |con )?vida|que puedes hacer con vida)$");
    private static final Pattern CLEAR = Pattern.compile("^(?:borra|olvida|elimina) (?:todos )?mis datos de vida$");
    private static final Pattern UNDO = Pattern.compile("^(?:deshaz|borra|quita) (?:la )?ultima anotacion(?: de vida)?$");

    private static final String[] DAYS = {"lunes", "martes", "miercoles", "jueves", "viernes", "sabado", "domingo"};
    private static final String[] HOUR_WORDS = {"una", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce"};

    static final String HELP_TEXT = "Módulo Vida: lo guardo en este móvil y, si tienes pCloud conectado, una copia en Salve/vida/vida.tsv.\n"
            + "• Gym: «curl predicador, 15 kilos, 8 reps» o compárteme el CSV que exporta Hevy.\n"
            + "• Sueño: «me acosté a la una y me levanté a las ocho».\n"
            + "• Comidas: «he comido lentejas con arroz».\n"
            + "• Horario: «añade al horario gym los lunes, martes y jueves a las 19:00 durante 60 minutos».\n"
            + "• Consultas: «qué tengo hoy», «mi horario», «resumen semanal», «historial de curl predicador».\n"
            + "• «deshaz la última anotación», «recupera mis datos de vida» o «borra mis datos de vida».";

    public static final class Response {
        public final String reply;
        public final VidaLog next;   // null = sin cambios
        public final boolean clearAll;
        Response(String reply, VidaLog next, boolean clearAll) { this.reply = reply; this.next = next; this.clearAll = clearAll; }
        public boolean changed() { return next != null || clearAll; }
    }

    private final Clock clock;
    /** Último tipo dictado en esta sesión ("serie" o "comida"), para que «deshaz» acierte con empates de hora. */
    private String lastVoiceKind;
    public VidaConversation() { this(Clock.systemDefaultZone()); }
    public VidaConversation(Clock clock) { this.clock = clock; }

    /**
     * @param knownExercise dice si un texto coincide con un ejercicio ya registrado; las consultas
     *                      de historial solo se capturan entonces, para no secuestrar otras charlas
     *                      («cómo voy con el trabajo»).
     */
    public static boolean handles(String input, java.util.function.Predicate<String> knownExercise) {
        if (input == null || input.length() > 300) return false;
        String t = normalize(input);
        if (t.isEmpty()) return false;
        Matcher history = HISTORY.matcher(t);
        if (history.matches()) return knownExercise != null && knownExercise.test(history.group(1));
        // Una pregunta sobre lo que comió no es una anotación («he cenado fuera, ¿qué opinas?»).
        if (MEAL.matcher(t).matches()) return input.indexOf('?') < 0;
        return SET.matcher(t).matches() || SLEEP.matcher(t).matches()
                || BLOCK.matcher(t).matches() || REMOVE_BLOCK.matcher(t).matches()
                || TODAY.matcher(t).matches() || WEEK.matcher(t).matches() || SCHEDULE.matcher(t).matches()
                || HELP.matcher(t).matches() || CLEAR.matcher(t).matches() || UNDO.matcher(t).matches();
    }

    /** Coincidencia exacta o parcial con un ejercicio del registro. */
    public static boolean knows(VidaLog log, String query) { return canonicalExercise(log, query) != null; }

    public Response handle(String input, VidaLog log) {
        if (log == null) throw new IllegalArgumentException("Falta el registro actual.");
        String t = normalize(input);
        LocalDate today = LocalDate.now(clock);
        Matcher m;
        if (HELP.matcher(t).matches()) return same(HELP_TEXT);
        if (CLEAR.matcher(t).matches()) return new Response("He borrado todos tus datos de Vida de este móvil.", null, true);
        if (UNDO.matcher(t).matches()) return undo(log, today);
        if (TODAY.matcher(t).matches()) return same(today(log, today));
        if (WEEK.matcher(t).matches()) return same(week(log, today));
        if (SCHEDULE.matcher(t).matches()) return same(schedule(log));
        if ((m = SLEEP.matcher(t)).matches()) return sleep(log, today, m);
        if ((m = BLOCK.matcher(t)).matches()) return block(log, m);
        if ((m = REMOVE_BLOCK.matcher(t)).matches()) return removeBlock(log, m.group(1));
        if ((m = MEAL.matcher(t)).matches()) return meal(log, today, m);
        if ((m = SET.matcher(t)).matches()) return set(log, today, m);
        if ((m = HISTORY.matcher(t)).matches()) return same(history(log, m.group(1)));
        return same(HELP_TEXT);
    }

    // ---------------------------------------------------------------- registros

    private Response set(VidaLog log, LocalDate today, Matcher m) {
        String exercise = capitalize(m.group(1).trim());
        double kg = Double.parseDouble(m.group(2).replace(',', '.'));
        int reps = Integer.parseInt(m.group(3));
        if (kg <= 0 || kg > 1000 || reps <= 0) return same("No he guardado esa serie: el peso o las repeticiones no son válidos.");
        String date = today.toString();
        int index = 0;
        for (VidaLog.GymSet s : log.sets) if (s.date.equals(date) && sameExercise(s.exercise, exercise)) index = Math.max(index, s.setIndex + 1);
        String existing = canonicalExercise(log, exercise);
        String name = existing != null ? existing : exercise;
        List<VidaLog.GymSet> next = new ArrayList<>(log.sets);
        next.add(new VidaLog.GymSet(date, "", name, index, "normal", kg, reps, null, null, "voz",
                "voz|" + date + "|" + LocalTime.now(clock).withNano(0) + "|" + name + "|" + index));
        lastVoiceKind = "serie";
        return new Response("Anotado: " + name + ", serie " + (index + 1) + " — " + fmt(kg) + " kg × " + reps + ".",
                log.withSets(next), false);
    }

    private Response sleep(VidaLog log, LocalDate today, Matcher m) {
        int bedH = Integer.parseInt(m.group(1)), wakeH = Integer.parseInt(m.group(4));
        int bedM = minutes(m.group(2), m.group(3)), wakeM = minutes(m.group(5), m.group(6));
        if (bedH > 23 || wakeH > 23) return same("No he guardado el sueño: la hora no es válida.");
        // Hablando, «a las once» es de noche: entre 7 y 12 se interpreta como p. m. si el despertar es antes.
        if (bedH >= 7 && bedH <= 12 && wakeH < bedH) bedH = (bedH + 12) % 24;
        int bed = bedH * 60 + bedM, wake = wakeH * 60 + wakeM;
        int total = Math.floorMod(wake - bed, 24 * 60);
        if (total < 60 || total > 16 * 60) return same("No he guardado el sueño: me salen " + hours(total) + ", ¿puedes decirme las horas con formato 24 h?");
        List<VidaLog.SleepEntry> next = new ArrayList<>();
        for (VidaLog.SleepEntry e : log.sleep) if (!e.date.equals(today.toString())) next.add(e);
        next.add(new VidaLog.SleepEntry(today.toString(), clock(bed), clock(wake), total));
        return new Response("Anotado: dormiste " + hours(total) + " (" + clock(bed) + " → " + clock(wake) + ").", log.withSleep(next), false);
    }

    private Response meal(VidaLog log, LocalDate today, Matcher m) {
        String kind;
        switch (m.group(1)) {
            case "desayunado": kind = "desayuno"; break;
            case "merendado": kind = "merienda"; break;
            case "cenado": kind = "cena"; break;
            default: kind = "comida";
        }
        String text = m.group(2).trim();
        List<VidaLog.Meal> next = new ArrayList<>(log.meals);
        next.add(new VidaLog.Meal(today.toString(), LocalTime.now(clock).withNano(0).withSecond(0).toString(), kind, text));
        lastVoiceKind = "comida";
        return new Response("Anotado en " + kind + ": " + text + ".", log.withMeals(next), false);
    }

    private Response block(VidaLog log, Matcher m) {
        String title = m.group(1).trim();
        TreeSet<Integer> days = days(m.group(2));
        if (days.isEmpty()) return same("No he entendido los días. Prueba: «los lunes y jueves» o «cada día».");
        int h = Integer.parseInt(m.group(3)), min = m.group(4) == null ? 0 : Integer.parseInt(m.group(4));
        if (h > 23 || min > 59) return same("No he guardado el bloque: la hora no es válida.");
        int duration = 60;
        if (m.group(5) != null) {
            duration = Integer.parseInt(m.group(5));
            if (m.group(6).startsWith("h")) duration *= 60;
        }
        if (duration <= 0 || duration > 16 * 60) return same("No he guardado el bloque: la duración no es válida.");
        String time = clock(h * 60 + min);
        List<VidaLog.Block> next = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (!(sameExercise(b.title, title) && days.contains(b.day))) next.add(b);
        for (int d : days) next.add(new VidaLog.Block(capitalize(title), d, time, duration));
        StringBuilder names = new StringBuilder();
        for (int d : days) { if (names.length() > 0) names.append(", "); names.append(DAYS[d - 1]); }
        return new Response("Añadido al horario: " + capitalize(title) + " — " + names + " a las " + time + " (" + duration + " min).",
                log.withBlocks(next), false);
    }

    private Response removeBlock(VidaLog log, String title) {
        List<VidaLog.Block> next = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (!sameExercise(b.title, title.trim())) next.add(b);
        if (next.size() == log.blocks.size()) return same("No encuentro «" + title.trim() + "» en tu horario.");
        return new Response("Quitado del horario: " + title.trim() + ".", log.withBlocks(next), false);
    }

    private Response undo(VidaLog log, LocalDate today) {
        // Solo deshace lo dictado hoy por voz (serie o comida, la más reciente); las importaciones se borran aparte.
        String date = today.toString();
        int lastSet = -1, lastMeal = -1;
        for (int i = 0; i < log.sets.size(); i++) if (log.sets.get(i).date.equals(date) && "voz".equals(log.sets.get(i).source)) lastSet = i;
        for (int i = 0; i < log.meals.size(); i++) if (log.meals.get(i).date.equals(date)) lastMeal = i;
        if (lastSet < 0 && lastMeal < 0) return same("Hoy no hay ninguna anotación por voz que deshacer.");
        String setStamp = lastSet < 0 ? "" : log.sets.get(lastSet).key.split("\\|").length > 2 ? log.sets.get(lastSet).key.split("\\|")[2] : "";
        String mealStamp = lastMeal < 0 ? "" : log.meals.get(lastMeal).time;
        boolean undoSet = lastMeal < 0 || (lastSet >= 0 && (lastVoiceKind != null ? lastVoiceKind.equals("serie") : setStamp.compareTo(mealStamp) >= 0));
        lastVoiceKind = null;
        if (undoSet) {
            List<VidaLog.GymSet> next = new ArrayList<>(log.sets);
            VidaLog.GymSet gone = next.remove(lastSet);
            return new Response("Deshecho: " + gone.exercise + " " + fmt(gone.kg) + " kg × " + gone.reps + ".", log.withSets(next), false);
        }
        List<VidaLog.Meal> next = new ArrayList<>(log.meals);
        VidaLog.Meal gone = next.remove(lastMeal);
        return new Response("Deshecho: " + gone.kind + " — " + gone.text + ".", log.withMeals(next), false);
    }

    // ---------------------------------------------------------------- consultas

    private String today(VidaLog log, LocalDate today) {
        StringBuilder out = new StringBuilder("Hoy, " + DAYS[today.getDayOfWeek().getValue() - 1] + ":\n");
        List<VidaLog.Block> blocks = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (b.day == today.getDayOfWeek().getValue()) blocks.add(b);
        blocks.sort(Comparator.comparing(b -> b.time));
        if (blocks.isEmpty()) out.append("• Sin bloques en el horario.\n");
        for (VidaLog.Block b : blocks) out.append("• ").append(b.time).append(" ").append(b.title).append(" (").append(b.minutes).append(" min)\n");
        Map<String, Integer> exercises = new LinkedHashMap<>();
        for (VidaLog.GymSet s : log.sets) if (s.date.equals(today.toString())) exercises.merge(s.exercise, 1, Integer::sum);
        if (!exercises.isEmpty()) {
            out.append("• Gym: ");
            boolean first = true;
            for (Map.Entry<String, Integer> e : exercises.entrySet()) { if (!first) out.append(", "); out.append(e.getKey()).append(" (").append(e.getValue()).append(")"); first = false; }
            out.append("\n");
        }
        for (VidaLog.Meal m : log.meals) if (m.date.equals(today.toString())) out.append("• ").append(capitalize(m.kind)).append(": ").append(m.text).append("\n");
        for (VidaLog.SleepEntry e : log.sleep) if (e.date.equals(today.toString())) out.append("• Anoche dormiste ").append(hours(e.minutes)).append("\n");
        String hint = sleepHint(log, today, blocks);
        if (hint != null) out.append(hint);
        return out.toString().trim();
    }

    private String sleepHint(VidaLog log, LocalDate today, List<VidaLog.Block> blocks) {
        for (VidaLog.SleepEntry e : log.sleep) {
            if (!e.date.equals(today.toString()) || e.minutes >= 6 * 60) continue;
            for (VidaLog.Block b : blocks)
                if (normalize(b.title).contains("gym") || normalize(b.title).contains("entreno"))
                    return "Dormiste poco y hoy tienes gym: baja un poco el peso o el volumen.";
        }
        return null;
    }

    private String week(VidaLog log, LocalDate today) {
        LocalDate from = today.minusDays(6);
        TreeSet<String> gymDays = new TreeSet<>();
        int sets = 0; double volume = 0;
        for (VidaLog.GymSet s : log.sets) {
            LocalDate d = LocalDate.parse(s.date);
            if (d.isBefore(from) || d.isAfter(today)) continue;
            gymDays.add(s.date); sets++; volume += s.volume();
        }
        int sleepDays = 0, sleepTotal = 0;
        for (VidaLog.SleepEntry e : log.sleep) {
            LocalDate d = LocalDate.parse(e.date);
            if (d.isBefore(from) || d.isAfter(today)) continue;
            sleepDays++; sleepTotal += e.minutes;
        }
        int meals = 0;
        for (VidaLog.Meal m : log.meals) { LocalDate d = LocalDate.parse(m.date); if (!d.isBefore(from) && !d.isAfter(today)) meals++; }
        int plannedGym = 0;
        for (VidaLog.Block b : log.blocks) if (normalize(b.title).contains("gym") || normalize(b.title).contains("entreno")) plannedGym++;
        StringBuilder out = new StringBuilder("Últimos 7 días:\n");
        out.append("• Gym: ").append(gymDays.size()).append(gymDays.size() == 1 ? " día" : " días");
        if (plannedGym > 0) out.append(" de ").append(plannedGym).append(" planificados");
        out.append(", ").append(sets).append(" series, ").append(fmt(volume)).append(" kg de volumen.\n");
        out.append("• Sueño: ").append(sleepDays == 0 ? "sin registros" : "media de " + hours(sleepTotal / sleepDays) + " (" + sleepDays + " noches)").append(".\n");
        out.append("• Comidas anotadas: ").append(meals).append(".");
        if (sleepDays > 0 && sleepTotal / sleepDays < 7 * 60) out.append("\nEstás por debajo de 7 horas de media: es lo primero que mejoraría.");
        return out.toString();
    }

    private String schedule(VidaLog log) {
        if (log.blocks.isEmpty()) return "Tu horario está vacío. Prueba: «añade al horario gym los lunes y jueves a las 19:00».";
        List<VidaLog.Block> sorted = new ArrayList<>(log.blocks);
        sorted.sort(Comparator.comparingInt((VidaLog.Block b) -> b.day).thenComparing(b -> b.time));
        StringBuilder out = new StringBuilder("Tu horario semanal:");
        int day = 0;
        for (VidaLog.Block b : sorted) {
            if (b.day != day) { day = b.day; out.append("\n").append(capitalize(DAYS[day - 1])).append(":"); }
            out.append(" ").append(b.time).append(" ").append(b.title).append(" (").append(b.minutes).append(" min);");
        }
        return out.toString();
    }

    private String history(VidaLog log, String query) {
        String name = canonicalExercise(log, query);
        if (name == null) return "No tengo series de «" + query.trim() + "». Anótalas por voz o compárteme el CSV de Hevy.";
        // Mejor serie (más peso, luego más reps) de cada sesión, por fecha.
        Map<String, VidaLog.GymSet> top = new java.util.TreeMap<>();
        for (VidaLog.GymSet s : log.sets) {
            if (!s.exercise.equals(name) || s.kg == null || s.reps == null || "calentamiento".equals(s.type)) continue;
            VidaLog.GymSet best = top.get(s.date);
            if (best == null || s.kg > best.kg || (s.kg.equals(best.kg) && s.reps > best.reps)) top.put(s.date, s);
        }
        if (top.isEmpty()) return name + ": hay registros pero sin peso y repeticiones (quizá es cardio).";
        List<VidaLog.GymSet> sessions = new ArrayList<>(top.values());
        VidaLog.GymSet record = sessions.get(0);
        for (VidaLog.GymSet s : sessions) if (s.kg > record.kg || (s.kg.equals(record.kg) && s.reps > record.reps)) record = s;
        VidaLog.GymSet last = sessions.get(sessions.size() - 1);
        StringBuilder out = new StringBuilder(name + ": " + sessions.size() + " sesiones.\n");
        out.append("• Mejor serie: ").append(fmt(record.kg)).append(" kg × ").append(record.reps).append(" (").append(record.date).append(").\n");
        out.append("• Última sesión: ").append(fmt(last.kg)).append(" kg × ").append(last.reps).append(" (").append(last.date).append(").");
        String advice = progression(sessions);
        if (advice != null) out.append("\n").append(advice);
        return out.toString();
    }

    /** Sobrecarga progresiva simple: tres sesiones seguidas con el mismo peso y ≥ 8 reps → subir. */
    static String progression(List<VidaLog.GymSet> sessions) {
        if (sessions.size() < 3) return null;
        List<VidaLog.GymSet> last = sessions.subList(sessions.size() - 3, sessions.size());
        double kg = last.get(0).kg;
        for (VidaLog.GymSet s : last) if (s.kg != kg || s.reps < 8) return null;
        double step = kg < 20 ? 1.25 : 2.5;
        return "Llevas 3 sesiones con " + fmt(kg) + " kg y 8 o más reps: prueba " + fmt(kg + step) + " kg en la primera serie.";
    }

    // ---------------------------------------------------------------- utilidades

    static String normalize(String input) {
        if (input == null) return "";
        String t = Normalizer.normalize(input.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        t = t.replaceAll("\\p{M}+", ""); // sin tildes ni eñes: «añade» = «anade»
        t = t.replaceFirst("^(?:salve[,:]?\\s+)", "").replaceAll("[¿?¡!.;]+$", "").replaceAll("^[¿¡]+", "").replaceAll("\\s+", " ").trim();
        for (int i = 0; i < HOUR_WORDS.length; i++) t = t.replaceAll("\\b(a las?) " + HOUR_WORDS[i] + "\\b", "$1 " + (i + 1));
        return t;
    }

    private static TreeSet<Integer> days(String text) {
        TreeSet<Integer> out = new TreeSet<>();
        if (text.matches(".*\\b(?:dia|dias|todos los dias)\\b.*")) { for (int d = 1; d <= 7; d++) out.add(d); return out; }
        if (text.contains("entre semana") || text.contains("laborables")) { for (int d = 1; d <= 5; d++) out.add(d); return out; }
        if (text.contains("fin de semana") || text.contains("findes")) { out.add(6); out.add(7); return out; }
        for (int i = 0; i < DAYS.length; i++) if (text.matches(".*\\b" + DAYS[i] + "s?\\b.*")) out.add(i + 1);
        return out;
    }

    private static String canonicalExercise(VidaLog log, String query) {
        String q = normalize(query);
        String partial = null;
        for (VidaLog.GymSet s : log.sets) {
            String e = normalize(s.exercise);
            if (e.equals(q)) return s.exercise;
            // Parcial solo desde el inicio de una palabra y con 4+ letras: «eso» no es «peso muerto».
            if (partial == null && q.length() >= 4 && (e.startsWith(q) || e.contains(" " + q))) partial = s.exercise;
        }
        return partial;
    }

    private static boolean sameExercise(String a, String b) { return normalize(a).equals(normalize(b)); }
    private static int minutes(String digits, String word) {
        if (digits != null) return Math.min(59, Integer.parseInt(digits));
        if ("media".equals(word)) return 30;
        if ("cuarto".equals(word)) return 15;
        return 0;
    }
    private static String clock(int minutes) { return String.format(Locale.ROOT, "%02d:%02d", minutes / 60 % 24, minutes % 60); }
    static String hours(int minutes) { return minutes / 60 + " h" + (minutes % 60 == 0 ? "" : " " + minutes % 60 + " min"); }
    static String fmt(Double v) {
        if (v == null) return "-";
        return v == Math.rint(v) ? String.valueOf(v.longValue()) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replace('.', ',');
    }
    private static String capitalize(String s) { return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static Response same(String reply) { return new Response(reply, null, false); }

    /** Para pruebas: día de la semana en español. */
    static String dayName(DayOfWeek d) { return DAYS[d.getValue() - 1]; }
}
