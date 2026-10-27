package salve.core.reasoning;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.cognitive.ReasoningEngine;

/**
 * Le da uso al {@link ReasoningEngine}, que existía pero nada alimentaba ni consultaba.
 *
 * <p><b>Aprende</b> solo de causas que Bryan enuncia («no duermo bien porque tomo café tarde»,
 * «si llueve, se corta la luz del taller», «el estrés provoca dolor de cabeza»): causalidad declarada,
 * no coocurrencia. Nunca aprende de la salida del modelo ni de páginas web.</p>
 *
 * <p><b>Razona</b> cuando Bryan pregunta «¿por qué X?» (hacia atrás: abducción) o «¿qué pasa si X?»
 * (hacia delante, encadenando A → B → C: deducción). Lo que encuentra entra en el contexto marcado
 * como «lo que Bryan te explicó», con las cadenas deducidas señaladas como tales.</p>
 */
public final class CausalMemory {
    static final float STATED_STRENGTH = 0.8f;
    private static final int MAX_CONCEPT_WORDS = 8, MAX_CONCEPT_CHARS = 70;

    private final ReasoningEngine engine;
    private final File file;

    public CausalMemory(ReasoningEngine engine, File file) {
        this.engine = engine;
        this.file = file;
        load();
    }

    /** Una relación causa → efecto extraída de una frase. */
    public static final class Relation {
        public final String cause, effect;
        public final ReasoningEngine.LinkType type;
        Relation(String cause, String effect, ReasoningEngine.LinkType type) { this.cause = cause; this.effect = effect; this.type = type; }
        @Override public String toString() { return cause + " → " + effect; }
    }

    // ── Extracción ──────────────────────────────────────────────────────

    private static final Pattern IF_THEN = Pattern.compile("^si (.+?)(?:,\\s*| entonces )(?:entonces )?(.+)$");
    private static final Pattern WHEN = Pattern.compile("^cuando (.+?),\\s*(.+)$");
    private static final Pattern BECAUSE = Pattern.compile("^(.+?),? (?:porque|ya que|debido a que|puesto que) (.+)$");
    private static final Pattern DUE_TO = Pattern.compile("^(.+?),? (?:por culpa de|debido a|a causa de) (.+)$");
    private static final Pattern CAUSES = Pattern.compile(
            "^(.+?) (?:provoca|provocan|causa|causan|produce|producen|genera|generan|hace que|hacen que|me hace|me hacen|nos hace|lleva a|llevan a|me da|me dan) (.+)$");
    /** «el café ya no me quita el sueño», «el estrés no provoca dolor de cabeza». */
    private static final Pattern NEG_CAUSES = Pattern.compile(
            "^(.+?) (?:ya )?no (?:me |nos |te |le )?(?:provoca|provocan|causa|causan|produce|producen|genera|generan|hace que|hacen que|"
            + "hace|hacen|da|dan|lleva a|llevan a|quita|quitan|tiene que ver con) (.+)$");
    /** «el dolor de cabeza no es por el estrés». («No duermo bien porque…» es una afirmación: su efecto es «no dormir bien».) */
    private static final Pattern NEG_BECAUSE = Pattern.compile(
            "^(.+?) no (?:es |viene )?(?:por|porque|debido a|a causa de) (.+)$");
    private static final Pattern NOT_TRUE = Pattern.compile("^(?:no es (?:verdad|cierto) que|ya no es (?:verdad|cierto) que|eso no es asi,? ) ?(.+)$");
    private static final Pattern QUESTION_START = Pattern.compile("^(?:por que|que|como|donde|quien|cual|cuanto|sabes|crees|puedes)\\b");
    private static final Set<String> EMPTY = new HashSet<>(Arrays.asList(
            "eso", "esto", "lo", "ello", "asi", "si", "no", "todo", "nada", "algo", "pasa", "pasa eso", "ocurre", "es asi", "me gusta",
            "quiero", "puedo", "lo hago", "lo se", "tu", "yo"));

    /** Relaciones causales que Bryan afirma en su mensaje (no en preguntas). */
    public static List<Relation> extract(String input) { return parse(input, false); }

    /** Relaciones que Bryan niega («X ya no provoca Y», «no es verdad que X porque Y»). */
    public static List<Relation> denials(String input) { return parse(input, true); }

    private static List<Relation> parse(String input, boolean wantDenials) {
        List<Relation> out = new ArrayList<>();
        if (input == null || input.length() > 600) return out;
        for (String raw : input.split("(?<=[.;!?\\n])")) {
            if (raw.contains("?") || raw.contains("¿")) continue;
            String s = normalize(raw).replaceFirst("^(?:y |pues |bueno,? |es que |mira,? |ademas,? |salve,? )+", "").trim();
            if (s.isEmpty() || QUESTION_START.matcher(s).find()) continue;
            Relation denied = denial(s);
            if (denied != null) {
                // Una negación nunca se aprende como causa (antes «el café no me hace dormir mal» habría
                // dado «café no → dormir mal»).
                if (wantDenials && denied != null) out.add(denied);
                continue;
            }
            if (wantDenials) continue;
            Relation r = match(IF_THEN, s, false, ReasoningEngine.LinkType.LOGICAL);
            if (r == null) r = match(WHEN, s, false, ReasoningEngine.LinkType.TEMPORAL);
            if (r == null) r = match(CAUSES, s, false, ReasoningEngine.LinkType.LOGICAL);
            if (r == null) r = match(DUE_TO, s, true, ReasoningEngine.LinkType.LOGICAL);
            if (r == null) r = match(BECAUSE, s, true, ReasoningEngine.LinkType.LOGICAL);
            if (r != null) out.add(r);
        }
        return out;
    }

    private static Relation denial(String s) {
        Matcher notTrue = NOT_TRUE.matcher(s);
        if (notTrue.matches()) {
            String inner = notTrue.group(1);
            List<Relation> stated = parse(inner, false);
            if (!stated.isEmpty()) return stated.get(0);
        }
        Relation r = match(NEG_CAUSES, s, false, ReasoningEngine.LinkType.LOGICAL);
        if (r == null) r = match(NEG_BECAUSE, s, true, ReasoningEngine.LinkType.LOGICAL);
        return r;
    }

    private static Relation match(Pattern p, String s, boolean effectFirst, ReasoningEngine.LinkType type) {
        Matcher m = p.matcher(s);
        if (!m.matches()) return null;
        String a = concept(m.group(1)), b = concept(m.group(2));
        if (a == null || b == null || a.equals(b)) return null;
        return effectFirst ? new Relation(b, a, type) : new Relation(a, b, type);
    }

    /** «el café que tomo por la tarde» → «cafe que tomo por la tarde»; null si es vacío o demasiado largo. */
    static String concept(String text) {
        String c = text.trim().replaceAll("[\"«»“”.,:;!¡]+", " ").replaceAll("\\s+", " ").trim();
        c = c.replaceFirst("^(?:el|la|los|las|un|una|unos|unas|que|yo|me|se) ", "").trim();
        if (c.isEmpty() || EMPTY.contains(c) || c.length() > MAX_CONCEPT_CHARS || c.split(" ").length > MAX_CONCEPT_WORDS) return null;
        if (c.length() < 3) return null;
        return c;
    }

    // ── Aprender y consultar ────────────────────────────────────────────

    /** Aprende las relaciones del mensaje de Bryan; devuelve las nuevas o reforzadas. */
    public synchronized List<Relation> learn(String input) {
        List<Relation> found = extract(input);
        for (Relation r : found)
            engine.registerCausalLink(canonical(r.cause), canonical(r.effect), STATED_STRENGTH, r.type);
        if (!found.isEmpty()) save();
        return found;
    }

    /**
     * Revisión de creencias: si Bryan niega una relación que tenía guardada, la borra (lo último que
     * dice Bryan sobre sí mismo manda). Devuelve las relaciones retiradas.
     */
    public synchronized List<Relation> revise(String input) {
        List<Relation> removed = new ArrayList<>();
        for (Relation r : denials(input)) {
            Relation known = known(r);
            if (known != null && engine.removeCausalLink(known.cause, known.effect)) removed.add(known);
        }
        if (!removed.isEmpty()) save();
        return removed;
    }

    /** La relación guardada que corresponde a {@code r} aunque esté dicha con otras palabras, o null. */
    Relation known(Relation r) {
        String cause = closest(r.cause), effect = closest(r.effect);
        if (cause == null || effect == null) return null;
        ReasoningEngine.CausalLink link = engine.link(cause, effect);
        return link == null ? null : new Relation(cause, effect, link.type);
    }

    private static final Pattern FORGET = Pattern.compile("^(?:salve,? )?(?:olvida|olvidate de|borra|deja de pensar) que (.+)$");
    private static final Pattern WHAT_LEARNED = Pattern.compile(
            "^(?:salve,? )?(?:que (?:has aprendido|sabes|causas (?:has aprendido|conoces)) (?:de mi|sobre mi)|"
            + "que causas (?:has aprendido|conoces|tienes guardadas)|tu memoria causal|ensename tu memoria causal)$");

    /**
     * Órdenes de Bryan sobre esta memoria: «¿qué has aprendido de mí?» y «olvida que X provoca Y».
     * Devuelve la respuesta, o null si el mensaje no es una de ellas.
     */
    public synchronized String command(String input) {
        if (input == null || input.length() > 300) return null;
        String t = normalize(input).replaceAll("[¿?¡!.]+", " ").replaceAll("\\s+", " ").trim();
        if (WHAT_LEARNED.matcher(t).matches()) return describe();
        Matcher forget = FORGET.matcher(t);
        if (!forget.matches()) return null;
        List<Relation> asked = extract(forget.group(1));
        if (asked.isEmpty()) return null; // «olvida que te lo dije»: no es una causa, sigue la conversación
        List<String> gone = new ArrayList<>();
        for (Relation r : asked) {
            Relation known = known(r);
            if (known != null && engine.removeCausalLink(known.cause, known.effect)) gone.add(known.toString());
        }
        if (!gone.isEmpty()) save();
        // Nada causal que olvidar: el olvido de memoria normal de Salve se encarga.
        if (gone.isEmpty()) return null;
        return "Hecho, he olvidado: " + String.join("; ", gone) + ".";
    }

    /** Lo aprendido, con cuántas veces se lo ha dicho Bryan. */
    String describe() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, List<ReasoningEngine.CausalLink>> e : engine.getAllCausalLinks().entrySet())
            for (ReasoningEngine.CausalLink l : e.getValue())
                lines.add(e.getKey() + " → " + l.effect + (l.observationCount > 1 ? " (me lo has dicho " + l.observationCount + " veces)" : ""));
        if (lines.isEmpty()) return "Todavía no he aprendido ninguna causa de lo que me cuentas. "
                + "Cuando me digas cosas como «X porque Y» o «si X, Y», las recordaré.";
        java.util.Collections.sort(lines);
        int shown = Math.min(lines.size(), 12);
        return "Esto es lo que he aprendido de lo que me has explicado:\n- " + String.join("\n- ", lines.subList(0, shown))
                + (lines.size() > shown ? "\n…y " + (lines.size() - shown) + " más." : "")
                + "\nSi algo ya no es así, dímelo («X ya no provoca Y») o pídeme que lo olvide.";
    }

    private static final Pattern WHY = Pattern.compile("(?:^|\\b)por que (?:me |se |te |nos )?(.+)$");
    private static final Pattern WHAT_IF = Pattern.compile(
            "(?:^|\\b)(?:que (?:pasa|pasaria|ocurre|ocurriria|sucede|sucederia)|que puede pasar) (?:si|cuando) (.+)$");

    /** Contexto causal para la pregunta de Bryan; vacío si no pregunta por causas o no sabe nada. */
    public synchronized String promptBlock(String input) {
        if (input == null || input.length() > 300) return "";
        String q = normalize(input).replaceAll("[¿?¡!.]+", " ").replaceAll("\\s+", " ").trim();
        List<String> lines = new ArrayList<>();
        Matcher why = WHY.matcher(q);
        Matcher whatIf = WHAT_IF.matcher(q);
        if (whatIf.find()) {
            String known = closest(whatIf.group(1));
            if (known != null) for (ReasoningEngine.Prediction p : engine.predict(known, 3)) {
                if (lines.size() >= 4) break;
                lines.add(known + " → " + p.concept + (p.chainLength > 1 ? " (deducido encadenando lo que te contó)" : ""));
            }
        } else if (why.find()) {
            String known = closest(why.group(1));
            if (known != null) for (ReasoningEngine.Prediction p : engine.explain(known, 3)) {
                if (lines.size() >= 4) break;
                lines.add(p.concept + " → " + known + (p.chainLength > 1 ? " (deducido encadenando lo que te contó)" : ""));
            }
        }
        if (lines.isEmpty()) return "";
        return " | CONOCIMIENTO_CAUSAL (lo que Bryan te explicó antes; úsalo si encaja con la pregunta y di que te lo contó él): "
                + String.join("; ", lines) + ".";
    }

    /**
     * Si ya conoce el mismo concepto dicho de otra forma («tomar café tarde» / «tomo café tarde»),
     * usa el guardado para que las cadenas se unan; si no, el nuevo.
     */
    String canonical(String concept) {
        Set<String> want = contentWords(concept);
        if (want.isEmpty()) return concept;
        for (String c : engine.concepts()) if (contentWords(c).equals(want)) return c;
        return concept;
    }

    /** El concepto guardado que mejor encaja con la frase (palabras con contenido en común), o null. */
    String closest(String phrase) {
        Set<String> want = contentWords(phrase);
        if (want.isEmpty()) return null;
        String best = null;
        double bestScore = 0;
        for (String c : engine.concepts()) {
            Set<String> have = contentWords(c);
            if (have.isEmpty()) continue;
            Set<String> both = new HashSet<>(want);
            both.retainAll(have);
            double score = both.size() / (double) Math.min(want.size(), have.size());
            if (score > bestScore || (score == bestScore && best != null && c.length() < best.length())) { best = c; bestScore = score; }
        }
        return bestScore >= 0.6 ? best : null;
    }

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "el", "la", "los", "las", "un", "una", "de", "del", "al", "a", "en", "y", "o", "que", "me", "se", "te", "mi", "mis", "tu",
            "su", "lo", "le", "por", "para", "con", "es", "son", "esta", "estoy", "muy", "mas", "tan", "no", "yo", "cuando", "si"));

    private static Set<String> contentWords(String text) {
        Set<String> out = new LinkedHashSet<>();
        for (String w : normalize(text).split("[^a-z0-9ñ]+")) if (w.length() > 2 && !STOP.contains(w)) out.add(stem(w));
        return out;
    }

    /** Raíz simple para plurales y género: «dolores» ~ «dolor», «cansada» ~ «cansado». */
    private static String stem(String w) {
        if (w.length() > 5 && w.endsWith("es")) w = w.substring(0, w.length() - 2);
        else if (w.length() > 4 && w.endsWith("s")) w = w.substring(0, w.length() - 1);
        if (w.length() > 4 && (w.endsWith("ar") || w.endsWith("er") || w.endsWith("ir"))) w = w.substring(0, w.length() - 2);
        else if (w.length() > 3 && (w.endsWith("a") || w.endsWith("o") || w.endsWith("e"))) w = w.substring(0, w.length() - 1);
        return w;
    }

    static String normalize(String text) {
        return Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "").trim();
    }

    // ── Persistencia (texto plano, sin dependencias) ────────────────────

    private void load() {
        if (file == null || !file.isFile()) return;
        try {
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String[] f = line.split("\t");
                if (f.length < 5) continue;
                try {
                    ReasoningEngine.LinkType type = ReasoningEngine.LinkType.valueOf(f[3]);
                    int count = Math.max(1, Math.min(1000, Integer.parseInt(f[4])));
                    engine.registerCausalLink(f[0], f[1], Float.parseFloat(f[2]), type);
                    ReasoningEngine.CausalLink link = engine.link(f[0], f[1]);
                    if (link != null) link.observationCount = count;
                } catch (IllegalArgumentException skip) { /* línea dañada: se ignora */ }
            }
        } catch (IOException | RuntimeException unreadable) { /* empieza sin memoria causal */ }
    }

    private void save() {
        if (file == null) return;
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, List<ReasoningEngine.CausalLink>> e : engine.getAllCausalLinks().entrySet())
            for (ReasoningEngine.CausalLink l : e.getValue())
                b.append(clean(e.getKey())).append('\t').append(clean(l.effect)).append('\t').append(l.strength).append('\t')
                        .append(l.type).append('\t').append(l.observationCount).append('\n');
        try {
            File tmp = new File(file.getPath() + ".tmp");
            Files.write(tmp.toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
            if (!tmp.renameTo(file)) Files.write(file.toPath(), b.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException unwritable) { /* se reintenta en el próximo aprendizaje */ }
    }

    private static String clean(String s) { return s.replace('\t', ' ').replace('\n', ' '); }
}
