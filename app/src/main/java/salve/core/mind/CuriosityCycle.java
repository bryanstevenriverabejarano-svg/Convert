package salve.core.mind;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Curiosidad propia: cuando Salve está sola, elige una de sus preguntas abiertas, la investiga
 * en fuentes públicas y guarda lo que encontró como un pensamiento suyo, con su fuente, para
 * contárselo a Bryan cuando vuelva.
 *
 * <p>Nada aquí usa el modelo de lenguaje: la búsqueda es solo red, así que no compite por la
 * RAM con Gemma ni con una conversación. El resumen son frases literales de la fuente, no una
 * síntesis inventada. Las preguntas que nombran a Bryan, a su familia o su vida privada nunca
 * salen a internet.</p>
 */
public final class CuriosityCycle {
    /** Tiempo mínimo entre dos investigaciones por su cuenta. */
    public static final long MIN_INTERVAL_MS = 3L * 60 * 60 * 1000;
    static final int MAX_SUMMARY = 220;

    private CuriosityCycle() { }

    /** La pregunta más saliente que aún no investigó y que puede salir a internet, o null. */
    public static SelfModel.Question choose(List<SelfModel.Question> questions, Set<String> explored,
                                            Collection<String> privateNames) {
        if (questions == null) return null;
        List<SelfModel.Question> sorted = new ArrayList<>(questions);
        Collections.sort(sorted, (a, b) -> Double.compare(b.salience, a.salience));
        for (SelfModel.Question q : sorted) {
            if (q == null || q.text == null || (explored != null && explored.contains(q.id))) continue;
            String text = q.text.trim();
            if (text.length() < 8 || text.length() > 220) continue;
            if (isPrivate(text, privateNames)) continue;
            return q;
        }
        return null;
    }

    /** Preguntas sobre Bryan, su familia o su vida no se envían a un buscador (política común). */
    public static boolean isPrivate(String text, Collection<String> privateNames) {
        return !salve.core.identity.NetworkPrivacy.safeForNetwork(text, privateNames);
    }

    /**
     * Consulta para el buscador: las palabras con contenido de su pregunta, no la frase entera.
     * 16/10/2026: buscó literalmente «Dado el historial de solicitudes sobre inglés y gramática, ¿cuál
     * es el siguiente paso lógico…» y el buscador devolvió un foro chino de AutoCAD.
     */
    public static String query(String question) {
        String q = question == null ? "" : question.replaceAll("[¿?¡!«»\"(),.;:]", " ").replaceAll("\\s+", " ").trim();
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String w : q.split(" ")) {
            String k = normalize(w);
            if (k.length() < 4 || QUERY_STOP.contains(k)) continue;
            if (out.indexOf(w.toLowerCase(java.util.Locale.ROOT)) >= 0) continue;
            out.append(n == 0 ? "" : " ").append(w.toLowerCase(java.util.Locale.ROOT));
            if (++n == 6) break;
        }
        if (n >= 2) return out.toString();
        return q.length() > 120 ? q.substring(0, 120).trim() : q;
    }

    /** Palabras de la consulta que una página relevante debería mencionar. */
    public static java.util.Set<String> keywords(String question) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (String w : query(question).split(" ")) {
            String k = normalize(w);
            if (k.length() >= 4 && !QUERY_STOP.contains(k)) out.add(k.length() > 6 ? k.substring(0, 6) : k); // raíz: «gramatica» ~ «gramatical»
        }
        return out;
    }

    private static final java.util.Set<String> QUERY_STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "dado", "dada", "dados", "historial", "solicitud", "solicitudes", "cual", "cuales", "como", "cuando", "donde",
            "siguiente", "paso", "pasos", "logico", "logica", "para", "estructurar", "estructura", "una", "unos", "unas",
            "mas", "menos", "sobre", "entre", "desde", "hasta", "este", "esta", "estos", "estas", "ese", "esa", "aquel",
            "porque", "que", "quien", "seria", "podria", "puedo", "puede", "deberia", "debo", "hacer", "tener", "manera",
            "forma", "mejor", "muy", "bryan", "salve", "mi", "mis", "sus", "nuestro", "nuestra", "coherente", "realmente",
            "acerca", "respecto", "tipo", "algo", "alguna", "alguno", "todo", "toda", "todos", "cada", "otra", "otro",
            "tambien", "ademas", "sino", "pero", "aunque", "hay", "son", "sea", "fue", "han", "ser", "estar", "tiene"));

    /** Lo que dice una página concreta: el extracto y su URL exacta viajan siempre juntos. */
    public static final class Finding {
        public final String summary, url;
        Finding(String summary, String url) { this.summary = summary; this.url = url; }
    }

    /**
     * Las primeras frases con contenido de la primera página legible, junto con la URL de esa
     * misma página. Literal, acotado y sin reescribir; null si ninguna dice nada claro.
     */
    public static Finding summarize(List<String> pageTexts, List<String> pageUrls) {
        return summarize(pageTexts, pageUrls, null);
    }

    /**
     * Como {@link #summarize(List, List)}, pero sólo acepta páginas que traten de verdad la pregunta
     * (mencionan sus palabras clave) y estén escritas en alfabeto latino; si ninguna, null.
     */
    public static Finding summarize(List<String> pageTexts, List<String> pageUrls, String question) {
        if (pageTexts == null) return null;
        java.util.Set<String> keys = question == null ? java.util.Collections.emptySet() : keywords(question);
        for (int i = 0; i < pageTexts.size(); i++) {
            String text = pageTexts.get(i);
            if (text == null) continue;
            if (!readable(text) || !relevant(text, keys)) continue;
            StringBuilder out = new StringBuilder();
            for (String sentence : text.replaceAll("\\s+", " ").split("(?<=[.!?])\\s+")) {
                String s = sentence.trim();
                if (s.length() < 40 || s.length() > 300 || s.contains("cookies") || s.contains("©")) continue;
                if (out.length() + s.length() > MAX_SUMMARY) break;
                if (out.length() > 0) out.append(' ');
                out.append(s);
                if (out.length() > 160) break;
            }
            if (out.length() >= 40)
                return new Finding(out.toString(), pageUrls != null && i < pageUrls.size() ? pageUrls.get(i) : null);
        }
        return null;
    }

    /** Alfabeto latino casi entero (un texto en chino o cirílico no le sirve a Salve ni a Bryan). */
    static boolean readable(String text) {
        int letters = 0, foreign = 0;
        for (int i = 0; i < text.length() && i < 4000; i++) {
            char c = text.charAt(i);
            if (!Character.isLetter(c)) continue;
            letters++;
            Character.UnicodeScript script = Character.UnicodeScript.of(c);
            if (script != Character.UnicodeScript.LATIN) foreign++;
        }
        return letters > 0 && foreign * 20 <= letters;
    }

    /** Menciona al menos dos palabras clave (o la única que haya). */
    static boolean relevant(String text, java.util.Set<String> keys) {
        if (keys.isEmpty()) return true;
        String t = normalize(text.length() > 20000 ? text.substring(0, 20000) : text);
        int hits = 0;
        for (String k : keys) if (t.contains(k)) hits++;
        return hits >= Math.min(2, keys.size());
    }

    /** Compatibilidad: solo el texto del extracto. */
    public static String summarize(List<String> pageTexts) {
        Finding finding = summarize(pageTexts, null);
        return finding == null ? null : finding.summary;
    }

    /** Referencia compacta (≤64) a la URL exacta: «web:» + dominio y ruta, con huella si no cabe. */
    public static String sourceRef(String url) {
        if (url == null || url.isEmpty()) return "web:desconocida";
        String bare = url.replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
        if (bare.length() <= 60) return "web:" + bare;
        String hash = Integer.toHexString(url.hashCode());
        return "web:" + bare.substring(0, 59 - hash.length()) + "#" + hash;
    }

    /** Pensamiento propio con la fuente: «Mientras no estabas investigué…». */
    public static MindPatch patch(SelfModel.Question question, String summary, String host) {
        return patch(question, summary, host, false);
    }

    /** Pensamiento con la URL exacta de la página que dijo el extracto. */
    public static MindPatch patch(SelfModel.Question question, Finding finding, boolean thread) {
        String host = hostOf(finding.url);
        String text = (thread ? "Seguí el hilo de «" : "Investigué por mi cuenta «") + question.text.trim() + "». "
                + (thread ? "Leí en " : "Encontré en ") + (host == null ? "una fuente pública" : host) + ": " + finding.summary;
        List<String> sources = new ArrayList<>();
        sources.add(sourceRef(finding.url));
        if (question.id != null) sources.add("q:" + question.id);
        return new MindPatch("curiosidad").thought(text, sources);
    }

    public static String hostOf(String url) {
        if (url == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^https?://([^/?#]+)").matcher(url);
        return m.find() ? m.group(1).replaceFirst("^www\\.", "") : null;
    }

    public static MindPatch patch(SelfModel.Question question, String summary, String host, boolean thread) {
        String source = host != null && host.startsWith("http") ? sourceRef(host)
                : "web:" + (host == null || host.isEmpty() ? "desconocida" : host);
        if (source.length() > 64) source = source.substring(0, 64);
        String text = (thread ? "Seguí el hilo de «" : "Investigué por mi cuenta «") + question.text.trim() + "». "
                + (thread ? "Leí en " : "Encontré en ") + (host == null ? "una fuente pública" : host)
                + ": " + summary;
        List<String> sources = new ArrayList<>();
        sources.add(source);
        if (question.id != null) sources.add("q:" + question.id);
        return new MindPatch("curiosidad").thought(text, sources);
    }

    private static final java.util.regex.Pattern NOT_A_THREAD = java.util.regex.Pattern.compile(
            "(?i).*(google\\.|bing\\.|duckduckgo\\.|facebook\\.|instagram\\.|twitter\\.|x\\.com|tiktok\\.|youtube\\.|login|signin|"
                    + "registr|cookie|privacy|privacidad|aviso-legal|\\.pdf$|\\.jpg$|\\.png$|/tag/|/categor|#).*");

    /**
     * Seguir el hilo: de los enlaces de lo último que leyó, el que más se relaciona con su
     * pregunta (palabras en la dirección) y que aún no visitó. Es su deriva, no una orden.
     */
    public static String pickThread(List<String> links, String question, Set<String> visited) {
        if (links == null || question == null) return null;
        Set<String> words = new java.util.HashSet<>();
        for (String w : normalize(question).split(" ")) if (w.length() >= 5) words.add(w);
        String best = null;
        int bestScore = 0;
        for (String link : links) {
            if (link == null || !link.startsWith("https://") || link.length() > 400) continue;
            if ((visited != null && visited.contains(link)) || NOT_A_THREAD.matcher(link).matches()) continue;
            String decoded;
            try { decoded = java.net.URLDecoder.decode(link.replace("+", " "), "UTF-8"); }
            catch (java.io.UnsupportedEncodingException | IllegalArgumentException bad) { decoded = link; }
            String path = normalize(decoded.replaceAll("[/_.\\-]+", " "));
            int score = 0;
            for (String w : words) if (path.contains(w.substring(0, 5))) score++;
            if (score > bestScore) { bestScore = score; best = link; }
        }
        return best;
    }

    private static final java.util.regex.Pattern ADMITS_NOT_KNOWING = java.util.regex.Pattern.compile(
            ".*\\b(no lo se|no se (?:con certeza|exactamente|cual|cuando|donde|quien|cuanto|si)|no tengo (?:esa |esos |suficiente )?(?:informacion|datos)|"
                    + "no estoy segura|no dispongo de|no puedo saber|desconozco|no conozco (?:ese|esa|el|la|los|las)|"
                    + "no tengo acceso a (?:internet|informacion actual)|mis datos (?:pueden estar|estan) desactualizados)\\b.*");

    /** Su propia respuesta reconoce que le falta un dato (para que vaya a buscarlo por su cuenta). */
    public static boolean admitsNotKnowing(String reply) {
        return reply != null && ADMITS_NOT_KNOWING.matcher(normalize(reply)).matches();
    }

    /** Pregunta sobre el mundo (no sobre Salve ni sobre la vida de quien habla). */
    public static boolean isWorldQuestion(String input) {
        String t = normalize(input);
        if (t.split(" ").length < 3) return false;
        boolean question = input.trim().endsWith("?") || t.matches("^(que|cual|cuales|cuando|donde|quien|quienes|como|cuanto|cuantos|cuantas|por que) .*");
        if (!question) return false;
        return !t.matches(".*\\b(tu|tus|te|ti|contigo|eres|estas|sientes|piensas|recuerdas|yo|me|mi|mis|conmigo|soy|estoy)\\b.*");
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}\\p{N} ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
