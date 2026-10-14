package salve.core.cognition;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.autonomy.AutonomousToolCommand;

/** Conservative syntax detection, not an authorization mechanism. Accepted text is always inert. */
final class CognitiveTextFallback {
    private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern TOOL = Pattern.compile(
            "(?i)(?:[\\\"']?mode[\\\"']?\\s*[:=]\\s*[\\\"']?tool\\b"
            + "|[\\\"']?(?:tool|tool_calls?|function_call|arguments|parameters)[\\\"']?\\s*[:=]"
            + "|[\\\"']?action[\\\"']?\\s*[:=]\\s*(?!null\\b)[{\\[\\\"'A-Za-z]"
            + "|<\\s*(?:tool_call|function_call|action)\\b|<\\|(?:tool_call|python_tag)"
            + "|\\b(?:TAP|ESCRIBIR|DEPLOY_WEB|SOLVE_CHALLENGE|AVATAR_[A-Z_]+|memory\\.lookup|runtime\\.status)\\s*\\("
            + "|\\b[A-Za-z][A-Za-z0-9_.]{1,63}\\s*\\(\\s*\\{)");
    private static final Pattern ENVELOPE = Pattern.compile(
            "(?i)[\\\"'](?:mode|message|action|reasoning|thoughts)[\\\"']\\s*:");
    private static final Pattern PRIVATE_CHANNEL = Pattern.compile(
            "(?i)<\\s*/?(?:think|thinking|analysis)\\b|<\\|(?:channel|start|im_start|analysis)\\b|\\[start\\]assistant");
    private CognitiveTextFallback() { }

    static String checkedText(String raw) {
        if (raw == null || raw.length() > AutonomousToolCommand.MAX_INPUT_BYTES
                || raw.getBytes(StandardCharsets.UTF_8).length > AutonomousToolCommand.MAX_INPUT_BYTES) return null;
        String text = raw.trim();
        if (text.startsWith("\uFEFF")) text = text.substring(1).trim();
        return text.isEmpty() || PRIVATE_CHANNEL.matcher(text).find() ? null : text;
    }
    static boolean toolAttempt(String raw) {
        if (raw == null) return false;
        // Bound work even when a provider violates its output limit. Decode escaped field names only
        // for classification; the resulting string is NEVER parsed/executed as a recovered action.
        String text = raw.substring(0, Math.min(raw.length(), AutonomousToolCommand.MAX_INPUT_BYTES));
        Matcher escaped = ESCAPE.matcher(text);
        StringBuffer normalized = new StringBuffer();
        while (escaped.find()) escaped.appendReplacement(normalized,
                Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(escaped.group(1), 16))));
        escaped.appendTail(normalized);
        return TOOL.matcher(normalized).find();
    }
    static boolean brokenEnvelope(String text) { return ENVELOPE.matcher(text).find(); }
    static String unwrapFence(String text) {
        if (text.startsWith("```json\n") && text.endsWith("```")) return text.substring(8, text.length() - 3).trim();
        if (text.startsWith("```\n") && text.endsWith("```")) return text.substring(4, text.length() - 3).trim();
        return null;
    }
}
