package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Seguir el hilo: qué tarea está en marcha, qué opciones le ofreció Salve en su última pregunta y
 * qué significa, respecto a eso, lo que Bryan acaba de decir.
 *
 * <p>16/10/2026: «enséñame inglés desde cero» → Salve: «¿frases básicas o gramática?» → «gramática
 * desde el inicio» → volvió a preguntar lo mismo. Después: «¿el orden de las palabras o los verbos?»
 * → «sí, el orden de las palabras, dale, hazlo» → escribió un relato poético sobre el orden de las
 * palabras. Un modelo pequeño lee cada mensaje casi suelto; aquí se calcula, con reglas, que «hazlo»
 * acepta lo que ella misma propuso y que «el orden de las palabras» es una de sus opciones, y se le
 * dice explícitamente: empieza ya.</p>
 */
public final class DialogueFocus {
    public enum Kind { NONE, ACCEPT, CHOOSE, REJECT }

    /** Una tarea sin tocar durante este tiempo deja de condicionar la conversación. */
    public static final long TASK_IDLE_MS = 40L * 60 * 1000;

    public static final class State {
        public String task = "";
        /** Lo que concretó después dentro de la misma tarea («hazme un plan…»). */
        public String detail = "";
        public long taskAt;
    }

    public static final class Reading {
        public final Kind kind;
        public final String choice, task, note;
        public final List<String> options;
        Reading(Kind kind, String choice, String task, List<String> options, String note) {
            this.kind = kind; this.choice = choice; this.task = task; this.options = options; this.note = note;
        }
        /** Bloque para el contexto del modelo; vacío si no hay nada que aclarar. */
        public String prompt() { return note.isEmpty() ? "" : " | ENTENDIMIENTO (cómo interpreto lo que dijo; datos): " + note; }
    }

    private DialogueFocus() { }

    private static final Pattern TASK = Pattern.compile(
            "\\b(ensename|ensenarme|ensena|explicame|explica|hazme un plan|haz un plan|quiero aprender|ayudame a|ayudame con|"
            + "practiquemos|practicar|aprender|clase de|curso de|leccion de|como se dice|traduce|corrige)\\b");
    private static final Pattern ACCEPT = Pattern.compile(
            "^(si|sip|vale|ok|okey|dale|venga|claro|perfecto|de acuerdo|adelante|empieza|empecemos|hazlo|hazla|por favor|porfa|"
            + "eso|esa|ese|me parece bien|bien|bueno|genial|ya|pues|vamos|y|el|la|lo|los|las|primero|primera)$");
    private static final Pattern REJECT = Pattern.compile("^(no|nop|ninguna|ninguno|otra cosa|ni una ni otra)\\b");
    private static final Pattern WANTS_ACTION = Pattern.compile("\\b(hazlo|dale|empieza|empecemos|adelante|venga|vamos|ya)\\b");
    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "que", "con", "los", "las", "una", "uno", "unos", "unas", "del", "para", "por", "como", "mas", "muy", "desde",
            "empecemos", "empezar", "empezamos", "prefieres", "gustaria", "quieres", "podemos", "quizas", "algo", "algun",
            "alguna", "sobre", "cosas", "nos", "enfoquemos", "revisemos", "sea", "este", "esta", "eso", "esa", "pues"));

    /** Arreglos de erratas de órdenes cortas que cambian el sentido («haslo» → «hazlo»). */
    static String normalize(String value) {
        String t = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").trim();
        return (" " + t + " ").replace(" haslo ", " hazlo ").replace(" hasla ", " hazla ").replace(" hasme ", " hazme ")
                .replace(" has lo ", " hazlo ").replace(" ase lo ", " hazlo ").replace(" pla ", " plan ").replace(" apra ", " para ")
                .replace(" gramtica ", " gramatica ").replace(" inici ", " inicio ").trim();
    }

    /** Registra una tarea si el mensaje la plantea («enséñame inglés desde cero»). */
    public static void observeUser(State state, String input, long now) {
        if (state == null || input == null) return;
        if (state.task != null && !state.task.isEmpty() && now - state.taskAt > TASK_IDLE_MS) state.task = "";
        String t = normalize(input);
        if (TASK.matcher(t).find() && t.split(" ").length >= 2) {
            // Dentro de una tarea reciente, un nuevo «enséñame…» la concreta; no la sustituye.
            if (state.task == null || state.task.isEmpty()) { state.task = bounded(input.trim(), 120); state.detail = ""; }
            else state.detail = bounded(input.trim(), 120);
            state.taskAt = now;
        } else if (state.task != null && !state.task.isEmpty()) {
            state.taskAt = now; // la conversación sigue dentro de la tarea
        }
    }

    /** Opciones que ofreció en su última pregunta («¿frases básicas o gramática desde el principio?»). */
    public static List<String> options(String lastReply) {
        if (lastReply == null) return Collections.emptyList();
        String[] sentences = lastReply.split("(?<=[.!?…])\\s+");
        String question = null;
        for (int i = sentences.length - 1; i >= 0; i--) if (sentences[i].contains("?")) { question = sentences[i]; break; }
        if (question == null) return Collections.emptyList();
        String q = question.replaceAll("[¿?]", " ").trim();
        // Si la pregunta empieza por «O si prefieres…», se une a la anterior.
        q = q.replaceFirst("(?i)^(te gustaria|te gustaría|prefieres|quieres|podemos|o si prefieres|o prefieres)\\s+(que\\s+)?", "");
        String[] raw = q.split("(?i),?\\s+o\\s+(?:quiz[aá]s\\s+|bien\\s+|mejor\\s+)?|\\s*,\\s+(?=(?:o |quiz))");
        List<String> out = new ArrayList<>();
        for (String r : raw) {
            String c = r.replaceFirst("(?i)^(empecemos|empezamos|nos enfoquemos|enfoquemos|revisemos|empezar|aprender|practicar)\\s+(con|en|por)?\\s*", "")
                    .replaceFirst("(?i)^(con|en|por|sobre)\\s+", "").trim();
            if (c.length() >= 3 && c.length() <= 90 && out.size() < 4) out.add(c);
        }
        return out.size() >= 2 ? out : Collections.emptyList();
    }

    /** Qué significa lo que dijo, respecto a la tarea y a la última pregunta de Salve. */
    public static Reading read(State state, String input, String lastReply, long now) {
        String task = state == null || state.task == null ? "" : state.task;
        if (state != null && !task.isEmpty() && now - state.taskAt > TASK_IDLE_MS) task = "";
        String detail = task.isEmpty() || state.detail == null ? "" : state.detail;
        String t = normalize(input);
        List<String> options = options(lastReply);
        Kind kind = Kind.NONE;
        String choice = "";
        if (!options.isEmpty()) {
            choice = bestOption(t, options);
            if (!choice.isEmpty()) kind = Kind.CHOOSE;
            else if (REJECT.matcher(t).find()) kind = Kind.REJECT;
            else if (onlyAcceptWords(t)) kind = Kind.ACCEPT;
        } else if (lastReply != null && lastReply.contains("?") && onlyAcceptWords(t)) {
            kind = Kind.ACCEPT;
        }
        StringBuilder note = new StringBuilder();
        if (!task.isEmpty() && !task.equalsIgnoreCase(input == null ? "" : input.trim()))
            note.append("tarea en curso que te pidió Bryan: «").append(task).append("»")
                    .append(detail.isEmpty() || detail.equalsIgnoreCase(input == null ? "" : input.trim()) ? ""
                            : " (después concretó: «" + detail + "»)").append(". ");
        switch (kind) {
            case CHOOSE:
                note.append("Le ofreciste: ").append(join(options)).append(". Ha ELEGIDO «").append(choice).append("»")
                        .append(WANTS_ACTION.matcher(t).find() ? " y te dice que lo hagas ya" : "")
                        .append(". Empieza AHORA con eso: contenido concreto (explicación breve, ejemplos y un pequeño ejercicio). ")
                        .append("No vuelvas a preguntar qué prefiere ni ofrezcas las mismas opciones. ");
                break;
            case ACCEPT:
                note.append("Bryan acepta lo que tú misma propusiste en tu último mensaje");
                if (!options.isEmpty()) note.append(" (opciones: ").append(join(options)).append("; no eligió: empieza por la primera)");
                note.append(". Hazlo AHORA, con contenido concreto; no vuelvas a preguntar. ");
                break;
            case REJECT:
                note.append("Bryan no quiere ninguna de las opciones que ofreciste (").append(join(options))
                        .append("). Pregúntale en una frase qué quiere o propón algo distinto. ");
                break;
            default:
                break;
        }
        if (!task.isEmpty() && kind != Kind.NONE)
            note.append("Responde como ayuda práctica, no como relato, poema ni descripción poética. ");
        String slip = languageSlip(task, t);
        if (!slip.isEmpty()) note.append(slip);
        return new Reading(kind, choice, task, options, note.toString().trim());
    }

    /** «No sé nada de español» en medio de «enséñame inglés»: casi seguro un lapsus. */
    static String languageSlip(String task, String normalizedInput) {
        String tk = normalize(task);
        boolean learningEnglish = tk.contains("ingles"), learningSpanish = tk.contains("espanol") || tk.contains("español");
        if (learningEnglish && !learningSpanish && (normalizedInput.contains("espanol") || normalizedInput.contains("español"))
                && !normalizedInput.contains("ingles"))
            return "Dice «español» pero la tarea es aprender inglés: interprétalo por el contexto (no sabe inglés) y, si dudas, confírmalo en una frase. ";
        return "";
    }

    private static boolean onlyAcceptWords(String t) {
        if (t.isEmpty()) return false;
        t = (" " + t + " ").replace(" por favor ", " si ").replace(" de acuerdo ", " si ").replace(" me parece bien ", " si ").trim();
        int accepted = 0;
        for (String w : t.split(" ")) {
            if (ACCEPT.matcher(w).matches()) accepted++;
            else return false;
        }
        return accepted > 0;
    }

    static String bestOption(String normalizedInput, List<String> options) {
        Set<String> said = words(normalizedInput);
        String best = "";
        int bestScore = 0;
        boolean tie = false;
        for (String o : options) {
            Set<String> ow = words(normalize(o));
            int score = 0;
            for (String w : ow) for (String s : said) if (w.equals(s) || (w.length() >= 6 && s.length() >= 6 && w.startsWith(s.substring(0, 5))))
                { score++; break; }
            if (score > bestScore) { bestScore = score; best = o; tie = false; }
            else if (score == bestScore && score > 0) tie = true;
        }
        return bestScore == 0 || tie ? "" : best;
    }

    private static Set<String> words(String normalized) {
        Set<String> out = new HashSet<>();
        for (String w : normalized.split(" ")) if (w.length() >= 4 && !STOP.contains(w)) out.add(w);
        return out;
    }

    private static String join(List<String> options) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < options.size(); i++) b.append(i == 0 ? "«" : i == options.size() - 1 ? " o «" : ", «").append(options.get(i)).append('»');
        return b.toString();
    }

    private static String bounded(String v, int max) {
        String s = v.replaceAll("\\s+", " ").trim();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
