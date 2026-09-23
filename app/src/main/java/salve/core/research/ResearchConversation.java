package salve.core.research;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.conversation.ChatMessage;

/** Read-only research requests and their short-lived conversational continuity. */
public final class ResearchConversation {
    public enum Kind { SEARCH, REPLAY, CLARIFY, NONE }
    public static final class Route {
        public final Kind kind;
        public final String question, text;
        private Route(Kind kind, String question, String text) {
            this.kind = kind; this.question = question; this.text = text;
        }
    }
    private static final Pattern REQUEST = Pattern.compile(
            "^(?:(?:salve[, ]+|por favor[, ]+|oye[, ]+|ahora[, ]+))*"
            + "(?:(?:puedes|podrias|quiero que|necesito que|te pido que)\\s+)?"
            + "(?:busca(?:r|s|me|lo|lo de nuevo|s otra vez)?|busques|investiga(?:r|s)?|investigues|"
            + "(?:haz|hacer|hagas|realiza|realizar|realices)\\s+(?:(?:una|la)\\s+)?busqueda)\\b\\s*(.*)$");
    private static final Pattern DEFINITION = Pattern.compile(
            "^(?:que (?:es|significa)|quien es|significado de)\\s+(.+)$");
    private static final Pattern CONFIRM = Pattern.compile(
            "^(?:si(?:[, ]+(?:por favor|adelante|hazlo|busca(?:lo)?))?|te lo confirmo|confirmo|"
            + "adelante|hazlo|me parece bien|vale|de acuerdo|tienes mi permiso|te autorizo)$");
    private static final Pattern RETRY = Pattern.compile(
            "^(?:intentalo (?:de nuevo|otra vez)|vuelve a (?:buscar|intentarlo)|busca(?:lo)? (?:de nuevo|otra vez))$");
    private String lastQuestion = "";
    private String lastResult = "";
    private boolean adjacent;

    /** Input is already in history. Only an explicit request can start a network read. */
    public synchronized Route route(String input, List<ChatMessage> history) {
        String normalized = normalize(input);
        if (adjacent && !lastQuestion.isEmpty() && RETRY.matcher(normalized).matches()) {
            return new Route(Kind.SEARCH, lastQuestion, "");
        }
        if (adjacent && CONFIRM.matcher(normalized).matches() && !lastResult.isEmpty()) {
            return new Route(Kind.REPLAY, lastQuestion, lastResult);
        }
        boolean followUp = normalized.matches("^(?:y |por que|que significa eso|explica|amplia|resume|"
                + "cuales son las fuentes|de donde|segun esas fuentes).*");
        String topic = requestTopic(input);
        if (topic == null) {
            adjacent = adjacent && followUp;
            return new Route(Kind.NONE, "", "");
        }
        String question = resolveName(topic);
        if (isReference(question)) {
            question = adjacent && !lastQuestion.isEmpty() ? lastQuestion : previousUserTopic(history, input);
        }
        adjacent = false;
        if (question.isEmpty() || isReference(question)) {
            return new Route(Kind.CLARIFY, "", "¿Qué tema quieres que busque?");
        }
        return new Route(Kind.SEARCH, question, "");
    }

    public synchronized void complete(String question, String result) {
        lastQuestion = limit(question, 2048);
        lastResult = limit(result, 6000);
        adjacent = true;
    }

    /** The last tool result survives eviction of a long assistant turn from the transcript. */
    public synchronized String context() {
        return adjacent && !lastResult.isEmpty()
                ? "ÚLTIMA BÚSQUEDA FINALIZADA: " + lastQuestion + "\n" + lastResult : "";
    }

    public synchronized void clear() {
        lastQuestion = ""; lastResult = ""; adjacent = false;
    }

    public static String requestTopic(String input) {
        String text = normalize(input);
        // Negation and discussion of someone else's request never authorize a read.
        Matcher request = REQUEST.matcher(text);
        Matcher definition = DEFINITION.matcher(text);
        String topic;
        if (request.matches()) topic = request.group(1);
        else if (definition.matches()) topic = text;
        else return null;
        topic = topic.replaceFirst("^(?:en (?:internet|la web|wikipedia)|por internet)\\s*", "")
                .replaceFirst("^(?:sobre|acerca de)\\s+", "")
                .replaceFirst("\\s+(?:en internet|en la web|por favor)$", "").trim();
        // URLs have case-sensitive paths; do not send the normalized spelling to the reader.
        Matcher url = Pattern.compile("https?://[^\\s<>]+", Pattern.CASE_INSENSITIVE).matcher(input);
        if (url.find()) return limit(url.group().replaceFirst("[.,;!?]+$", ""), 2048);
        return limit(topic, 2048);
    }

    public static String lookupQuery(String question) {
        if (question.contains("://")) return limit(question, 2048);
        String query = resolveName(question).replaceFirst("^(?:que (?:es|significa)|quien es|(?:el )?significado de)\\s+", "")
                .replaceFirst("^(?:el |la )?(?:nombre|palabra|termino)\\s+", "");
        return limit(query.trim(), 256);
    }

    private static String resolveName(String text) {
        return text.replaceAll("\\b(?:tu|su) nombre(?:\\s+salve)?\\b", "Salve");
    }

    private static boolean isReference(String text) {
        return text.isEmpty() || text.matches("(?:eso|esto|lo|lo anterior|sobre eso|lo que te (?:dije|pedi)|"
                + "lo que acabamos de hablar)");
    }

    private static String previousUserTopic(List<ChatMessage> history, String current) {
        if (history == null) return "";
        boolean skippedCurrent = false;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage message = history.get(i);
            if (message.getRole() != ChatMessage.Role.USER) continue;
            if (!skippedCurrent && message.getContent().equals(current)) { skippedCurrent = true; continue; }
            String previous = normalize(message.getContent());
            if (CONFIRM.matcher(previous).matches()) continue;
            String topic = requestTopic(previous);
            // Resolve only the nearest meaningful user topic, never an assistant suggestion.
            if (topic != null && !isReference(topic)) return resolveName(topic);
            if (previous.matches("^(?:que|como|cual|quien|por que)\\b.*")) return resolveName(previous);
            return "";
        }
        return "";
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[¿?¡!]", " ").replaceFirst("[.,;]+$", "")
                .replaceAll("\\s+", " ").trim();
    }

    private static String limit(String text, int max) {
        if (text == null) return "";
        return text.substring(0, Math.min(text.length(), max));
    }
}
