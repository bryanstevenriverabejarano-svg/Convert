package salve.avatar;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small visual vocabulary, never executable tools, persisted emotions or claims of consciousness. */
public final class AvatarMotionProtocol {
    private static final Pattern RESERVED = Pattern.compile("(?i)\\[{1,2}\\s*salve[_\\s-]*motion\\b");
    private static final Pattern DIRECTIVE = Pattern.compile(
            "\\[\\[salve_motion:(NONE|WAVE|NOD|SHAKE|EXPLAIN|THINK|CELEBRATE):(NEUTRAL|WARM|CURIOUS|CONCERNED)\\]\\]");
    private AvatarMotionProtocol() { }

    public static final class Result {
        public final String text;
        public final AvatarMotion.Gesture gesture;
        public final AvatarMotion.Expression expression;
        public final boolean hasDirective;
        private Result(String text, AvatarMotion.Gesture gesture, AvatarMotion.Expression expression,
                       boolean hasDirective) {
            this.text = text;
            this.gesture = gesture;
            this.expression = expression;
            this.hasDirective = hasDirective;
        }
    }

    public static String instruction() {
        return "Animación opcional de Salve: después de responder puedes copiar EXACTAMENTE UNO "
                + "de estos sufijos completos, sólo al FINAL del texto, sin modificarlo ni explicarlo:\n"
                + "Saludar con la mano: [[salve_motion:WAVE:WARM]]\n"
                + "Asentir: [[salve_motion:NOD:NEUTRAL]]\n"
                + "Negar con la cabeza: [[salve_motion:SHAKE:NEUTRAL]]\n"
                + "Acompañar una explicación: [[salve_motion:EXPLAIN:NEUTRAL]]\n"
                + "Considerar una cuestión: [[salve_motion:THINK:CURIOUS]]\n"
                + "Celebrar: [[salve_motion:CELEBRATE:WARM]]\n"
                + "Pedir una aclaración: [[salve_motion:NONE:CURIOUS]]\n"
                + "Ejemplo de respuesta completa: Hola. [[salve_motion:WAVE:WARM]]\n"
                + "Si ningún gesto corresponde, omite el sufijo. NUNCA añadas sufijos a un JSON de herramientas. "
                + "Es animación, no consciencia ni sentimientos reales del usuario. "
                + "El sufijo nunca ejecuta herramientas.\n";
    }

    /** Accept exactly one valid final directive; scrub all reserved fragments before history or TTS. */
    public static Result parse(String raw) {
        if (raw == null || raw.isEmpty()) return empty("");
        Matcher reserved = RESERVED.matcher(raw);
        int count = 0, first = -1;
        while (reserved.find()) { if (first < 0) first = reserved.start(); count++; }
        AvatarMotion.Gesture gesture = AvatarMotion.Gesture.NONE;
        AvatarMotion.Expression expression = AvatarMotion.Expression.NEUTRAL;
        boolean valid = false;
        if (count == 1) {
            String tail = raw.substring(first).trim();
            if (tail.length() <= 96) {
                Matcher directive = DIRECTIVE.matcher(tail);
                if (directive.matches()) {
                    gesture = AvatarMotion.Gesture.valueOf(directive.group(1));
                    expression = AvatarMotion.Expression.valueOf(directive.group(2));
                    valid = true;
                }
            }
        }
        if (count == 0) return empty(raw.trim());
        StringBuilder clean = new StringBuilder();
        reserved.reset();
        int cursor = 0;
        while (reserved.find(cursor)) {
            clean.append(raw, cursor, reserved.start());
            int close = raw.indexOf(']', reserved.end());
            if (close < 0) { cursor = raw.length(); break; }
            cursor = close + 1;
            if (cursor < raw.length() && raw.charAt(cursor) == ']') cursor++;
            // Never join words around a removed in-line fragment.
            clean.append(' ');
        }
        clean.append(raw, cursor, raw.length());
        return new Result(clean.toString().trim(), gesture, expression, valid);
    }

    /** Exact visual commands only: questions, negations and quoted discussion remain conversation. */
    public static Result parseCommand(String input) {
        String value = normalize(input);
        switch (value) {
            case "saluda con la mano": case "saludame con la mano":
                return directive("Hola.", AvatarMotion.Gesture.WAVE, AvatarMotion.Expression.WARM);
            case "asiente": case "asiente con la cabeza":
                return directive("Así.", AvatarMotion.Gesture.NOD, AvatarMotion.Expression.NEUTRAL);
            case "niega con la cabeza":
                return directive("Así.", AvatarMotion.Gesture.SHAKE, AvatarMotion.Expression.NEUTRAL);
            case "haz un gesto de explicacion":
                return directive("Así acompaño una explicación.", AvatarMotion.Gesture.EXPLAIN, AvatarMotion.Expression.NEUTRAL);
            case "celebra":
                return directive("¡Vamos!", AvatarMotion.Gesture.CELEBRATE, AvatarMotion.Expression.WARM);
            case "mirame":
                return directive("Aquí estoy.", AvatarMotion.Gesture.NONE, AvatarMotion.Expression.CURIOUS);
            default: return null;
        }
    }

    /** A fallback describes the conversational act; it does not diagnose anybody's emotions. */
    static Result fallback(String input) {
        Result command = parseCommand(input);
        if (command != null) return command;
        String value = normalize(input);
        if (value.equals("hola") || value.equals("hola salve") || value.equals("buenos dias")
                || value.equals("buenas tardes") || value.equals("buenas noches"))
            return directive("", AvatarMotion.Gesture.WAVE, AvatarMotion.Expression.WARM);
        if (value.startsWith("corrige ") || value.equals("eso es incorrecto")
                || value.equals("te has equivocado"))
            return directive("", AvatarMotion.Gesture.NOD, AvatarMotion.Expression.NEUTRAL);
        if (value.startsWith("explica ") || value.startsWith("explicame "))
            return directive("", AvatarMotion.Gesture.EXPLAIN, AvatarMotion.Expression.NEUTRAL);
        return empty("");
    }

    private static Result directive(String text, AvatarMotion.Gesture gesture, AvatarMotion.Expression expression) {
        return new Result(text, gesture, expression, true);
    }
    private static Result empty(String text) {
        return new Result(text, AvatarMotion.Gesture.NONE, AvatarMotion.Expression.NEUTRAL, false);
    }
    private static String normalize(String value) {
        if (value == null || value.length() > 2000) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .trim().toLowerCase(Locale.ROOT).replaceAll("[.!¡]+$", "").trim();
    }
}
