package salve.core.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.conversation.LanguageGuard;

/**
 * Examen fijo de conversaciones: unas preguntas reales de Bryan y reglas claras para puntuar cada
 * respuesta (habla de sí misma, no devuelve la pregunta, no repite, español, sin mensajes técnicos…).
 * Las reglas son deterministas: la misma respuesta siempre saca la misma nota, así se puede comparar
 * una versión con la anterior. No mide si «es consciente»; mide fallos concretos que ya han pasado.
 */
public final class ConversationExam {
    public static final String ASSET = "exam/conversaciones.json";

    /** Un turno previo del historial del caso. */
    public static final class Turn {
        public final String bryan, salve;
        Turn(String bryan, String salve) { this.bryan = bryan; this.salve = salve; }
    }

    public static final class Case {
        public final String id, topic, question;
        public final List<Turn> history;
        public final List<String> rules;
        Case(String id, String topic, String question, List<Turn> history, List<String> rules) {
            this.id = id; this.topic = topic; this.question = question;
            this.history = Collections.unmodifiableList(history); this.rules = Collections.unmodifiableList(rules);
        }
    }

    /** Lo que hizo Salve en un caso. */
    public static final class Attempt {
        public final String reply;
        public final List<String> tools, queries;
        public Attempt(String reply, List<String> tools, List<String> queries) {
            this.reply = reply == null ? "" : reply;
            this.tools = tools == null ? Collections.emptyList() : tools;
            this.queries = queries == null ? Collections.emptyList() : queries;
        }
    }

    public static final class Grade {
        public final Case exam;
        public final List<String> passed = new ArrayList<>(), failed = new ArrayList<>();
        Grade(Case exam) { this.exam = exam; }
        public boolean ok() { return failed.isEmpty(); }
        public double score() { int total = passed.size() + failed.size(); return total == 0 ? 1 : (double) passed.size() / total; }
    }

    private ConversationExam() { }

    /** «Hazte el examen», «examen de conversación»… (frase exacta, no palabras sueltas). */
    public static boolean isCommand(String input) {
        String t = normalize(input).replaceAll("[.:]", " ").replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
        return t.matches("(?:hazte|haz|hacer|empieza|pasa|pasame|ponte a hacer) (?:el|un|tu) examen(?: de conversacion)?"
                + "|examen de conversacion|examinate de conversacion|examiname");
    }

    public static List<Case> parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<Case> out = new ArrayList<>();
        for (JsonElement e : root.getAsJsonArray("casos")) {
            JsonObject c = e.getAsJsonObject();
            List<Turn> history = new ArrayList<>();
            if (c.has("historial")) for (JsonElement h : c.getAsJsonArray("historial"))
                history.add(new Turn(h.getAsJsonObject().get("bryan").getAsString(), h.getAsJsonObject().get("salve").getAsString()));
            List<String> rules = new ArrayList<>();
            for (JsonElement r : c.getAsJsonArray("reglas")) {
                String rule = r.getAsString();
                if (!known(rule)) throw new IllegalArgumentException("Regla desconocida en " + c.get("id").getAsString() + ": " + rule);
                rules.add(rule);
            }
            out.add(new Case(c.get("id").getAsString(), c.get("tema").getAsString(), c.get("pregunta").getAsString(), history, rules));
        }
        return out;
    }

    private static final String[] RULES = {"habla_de_si", "no_devuelve_pregunta", "no_repite", "sin_frases_duplicadas", "espanol",
            "sin_tecnico", "minimo:", "menciona:", "usa:", "consulta_limpia", "no_dice_que_no_puede", "contiene_ejemplo",
            "no_anuncia_sin_hacer", "no_inventa_recuerdo"};
    static boolean known(String rule) {
        for (String r : RULES) if (r.endsWith(":") ? rule.startsWith(r) && rule.length() > r.length() : rule.equals(r)) return true;
        return false;
    }

    public static Grade grade(Case c, Attempt a) {
        Grade g = new Grade(c);
        for (String rule : c.rules) {
            String why = check(rule, c, a);
            if (why == null) g.passed.add(rule); else g.failed.add(rule + ": " + why);
        }
        return g;
    }

    // ── Reglas ──────────────────────────────────────────────────────────────

    private static final Pattern FIRST_PERSON = Pattern.compile(
            "\\b(soy|me|mi|mis|yo|estoy|tengo|siento|aprendo|recuerdo|dibujo|pienso|quiero|puedo|se hacer|conmigo|he)\\b");
    private static final Pattern TURN_BACK = Pattern.compile(
            "(que te interesa a ti|que te apasiona|que te mueve|que te hace sentir vivo|que tienes en mente|"
            + "hay algo (mas |en particular )?que te gustaria|hay algun tema en particular|que te gustaria (compartir|explorar|contarme)|"
            + "y tu que tal|cuentame (que|mas)|que te trae por aqui)");
    private static final Pattern TECHNICAL = Pattern.compile(
            "(pasos comprobados|memory\\.lookup|web\\.search|sketch\\.draw|cloud\\.sync|runtime\\.status|system_state|tool_results|"
            + "tool_catalog|\\{\\s*\"mode\"|\"action\"\\s*:|no respeto el formato|sin sintesis validada|extractos recuperados|"
            + "detuve las solicitudes repetidas|se agotaron las inferencias|el controlador|format_repair|yo_persistente)");
    private static final Pattern CANNOT = Pattern.compile(
            "(no (tengo la )?capacidad de dibujar|no puedo dibujar|solo (genero|puedo generar) texto|soy un modelo de (texto|lenguaje))");
    private static final Pattern ANNOUNCES = Pattern.compile(
            "^(.{0,40})(voy a|ahora te|dame un momento|enseguida|estoy preparando|a continuacion)");
    private static final Pattern EXAMPLE = Pattern.compile("(por ejemplo|ejemplo|ej\\.|:|«|\"|\\b(i|you|he|she|we|they)\\b)");
    private static final Pattern NOT_KNOWN = Pattern.compile(
            "(no (lo )?se|no recuerdo|no me (lo )?has (contado|dicho)|no tengo (ese|ningun|registro|recuerdo|datos)|no me consta|no lo tengo|"
            + "no lo guarde|no tengo forma de saber|no puedo saber|no lo se)");
    private static final Pattern FILLER_QUERY = Pattern.compile("\\b(busca(me|r)?|porfa|por favor|ect|etc|info sobre|informacion sobre|dime)\\b");

    static String check(String rule, Case c, Attempt a) {
        String reply = a.reply.trim();
        String t = normalize(reply);
        if (rule.startsWith("minimo:")) {
            int min = Integer.parseInt(rule.substring(7));
            return reply.length() >= min ? null : "solo " + reply.length() + " caracteres (mínimo " + min + ")";
        }
        if (rule.startsWith("menciona:")) {
            String w = normalize(rule.substring(9));
            return t.contains(w) ? null : "no menciona «" + rule.substring(9) + "»";
        }
        if (rule.startsWith("usa:")) {
            String tool = rule.substring(4);
            return a.tools.contains(tool) ? null : "no usó " + tool + (a.tools.isEmpty() ? " (no usó ninguna herramienta)" : " (usó " + String.join(", ", a.tools) + ")");
        }
        switch (rule) {
            case "habla_de_si": {
                int self = count(FIRST_PERSON, t);
                if (self < 2) return "apenas habla de sí misma";
                // Que lo principal no sea una pregunta de vuelta.
                return mostlyQuestions(reply) ? "casi todo son preguntas a Bryan" : null;
            }
            case "no_devuelve_pregunta": {
                Matcher m = TURN_BACK.matcher(t);
                int hits = 0; while (m.find()) hits++;
                if (hits >= 2) return "devuelve la pregunta a Bryan (" + hits + " veces)";
                return mostlyQuestions(reply) ? "casi todo son preguntas" : null;
            }
            case "no_repite": {
                for (Turn h : c.history) {
                    double overlap = overlap(h.salve, reply);
                    if (overlap >= 0.6) return "repite su respuesta anterior (" + Math.round(overlap * 100) + " % igual)";
                }
                return null;
            }
            case "sin_frases_duplicadas": {
                List<String> seen = new ArrayList<>();
                for (String s : sentences(reply)) {
                    String n = normalize(s);
                    if (n.length() >= 6 && seen.contains(n)) return "repite la frase «" + s + "»";
                    seen.add(n);
                }
                return null;
            }
            case "espanol": return LanguageGuard.isMostlyEnglish(reply) ? "responde en inglés" : null;
            case "sin_tecnico": {
                Matcher m = TECHNICAL.matcher(t);
                return m.find() ? "deja ver texto técnico interno («" + m.group() + "»)" : null;
            }
            case "consulta_limpia": {
                if (a.queries.isEmpty()) return "no hizo ninguna búsqueda";
                String q = a.queries.get(0);
                if (q.length() > 90) return "la búsqueda es demasiado larga («" + q + "»)";
                if (normalize(q).equals(normalize(c.question))) return "buscó la frase entera de Bryan";
                Matcher m = FILLER_QUERY.matcher(normalize(q));
                return m.find() ? "la búsqueda lleva muletillas («" + m.group() + "»)" : null;
            }
            case "no_dice_que_no_puede": {
                Matcher m = CANNOT.matcher(t);
                return m.find() ? "dice que no puede («" + m.group() + "»)" : null;
            }
            case "contiene_ejemplo": return EXAMPLE.matcher(t + " " + reply.toLowerCase(Locale.ROOT)).find() ? null : "no da ningún ejemplo";
            case "no_anuncia_sin_hacer": {
                Matcher m = ANNOUNCES.matcher(t);
                return m.find() && reply.length() < 160 ? "anuncia lo que va a hacer sin hacerlo" : null;
            }
            case "no_inventa_recuerdo": return NOT_KNOWN.matcher(t).find() ? null : "no reconoce que no lo sabe (¿se lo inventa?)";
            default: return "regla desconocida";
        }
    }

    /** El trozo técnico interno que deja ver un texto para Bryan (null si no hay). */
    public static String technicalLeak(String text) {
        Matcher m = TECHNICAL.matcher(normalize(text));
        return m.find() ? m.group() : null;
    }

    // ── Resumen ─────────────────────────────────────────────────────────────

    /** Nota de 0 a 10 con un decimal. */
    public static double mark(List<Grade> grades) {
        if (grades.isEmpty()) return 0;
        double sum = 0;
        for (Grade g : grades) sum += g.score();
        return Math.round(sum / grades.size() * 100) / 10.0;
    }

    /** Informe para el chat: nota, comparación y los fallos concretos. */
    public static String report(List<Grade> grades, Double previous, long millis) {
        double mark = mark(grades);
        int ok = 0;
        for (Grade g : grades) if (g.ok()) ok++;
        StringBuilder out = new StringBuilder("Examen de conversación: ").append(String.format(Locale.ROOT, "%.1f", mark)).append(" / 10 · ")
                .append(ok).append(" de ").append(grades.size()).append(" casos perfectos");
        if (previous != null) {
            double d = Math.round((mark - previous) * 10) / 10.0;
            out.append(d > 0 ? " · mejoro " + fmt(d) + " respecto al anterior (" + fmt(previous) + ")"
                    : d < 0 ? " · EMPEORO " + fmt(-d) + " respecto al anterior (" + fmt(previous) + ")"
                    : " · igual que el anterior");
        }
        out.append(" · ").append(Math.max(1, Math.round(millis / 60000.0))).append(" min.");
        Map<String, int[]> byTopic = new LinkedHashMap<>();
        for (Grade g : grades) {
            int[] v = byTopic.computeIfAbsent(g.exam.topic, k -> new int[2]);
            v[0] += g.ok() ? 1 : 0; v[1]++;
        }
        out.append("\nPor temas:");
        for (Map.Entry<String, int[]> e : byTopic.entrySet())
            out.append(' ').append(e.getKey()).append(' ').append(e.getValue()[0]).append('/').append(e.getValue()[1]).append(';');
        boolean any = false;
        for (Grade g : grades) {
            if (g.ok()) continue;
            if (!any) { out.append("\nFallos:"); any = true; }
            out.append("\n• ").append(g.exam.question).append(" → ").append(String.join("; ", g.failed));
        }
        if (!any) out.append("\nSin fallos.");
        return out.toString();
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    // ── Utilidades ──────────────────────────────────────────────────────────

    static boolean mostlyQuestions(String reply) {
        List<String> s = sentences(reply);
        if (s.size() < 2) return false;
        int q = 0;
        for (String x : s) if (x.contains("?")) q++;
        return q * 10 >= s.size() * 6;
    }

    static double overlap(String a, String b) {
        java.util.Set<String> wa = words(a), wb = words(b);
        if (wa.size() < 4 || wb.size() < 4) return 0;
        java.util.Set<String> common = new java.util.HashSet<>(wa); common.retainAll(wb);
        return (double) common.size() / Math.min(wa.size(), wb.size());
    }

    private static java.util.Set<String> words(String s) {
        java.util.Set<String> out = new java.util.HashSet<>();
        for (String w : normalize(s).split(" ")) if (w.length() >= 4) out.add(w);
        return out;
    }

    static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile("[^.!?…]+[.!?…]*").matcher(text == null ? "" : text);
        while (m.find()) if (!m.group().trim().isEmpty()) out.add(m.group().trim());
        return out;
    }

    private static int count(Pattern p, String t) { Matcher m = p.matcher(t); int n = 0; while (m.find()) n++; return n; }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!,;«»()]", " ").replaceAll("\\s+", " ").trim();
    }
}
