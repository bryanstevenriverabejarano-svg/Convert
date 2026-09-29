package salve.avatar;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A small visual vocabulary, never executable tools, persisted emotions or claims of consciousness. */
public final class AvatarMotionProtocol {
    private static final Pattern RESERVED = Pattern.compile("(?i)\\[{1,2}\\s*salve[_\\s-]*motion\\b");
    private static final Pattern DIRECTIVE = Pattern.compile(
            "\\[\\[salve_motion:(NONE|WAVE|NOD|SHAKE|EXPLAIN|THINK|CELEBRATE|LAUGH|CRY|STARTLE):"
            + "(NEUTRAL|WARM|CURIOUS|CONCERNED|SAD|ANGRY|SURPRISED|SHY)"
            + "(?::(KEEP|STAND|WALK|SIT|KNEEL|CROUCH|DANCE_POP|DANCE_URBAN))?\\]\\]");
    private AvatarMotionProtocol() { }

    public static final class Result {
        public final String text;
        public final AvatarMotion.Gesture gesture;
        public final AvatarMotion.Expression expression;
        public final boolean hasDirective;
        public final AvatarBodyCue body;
        private Result(String text, AvatarMotion.Gesture gesture, AvatarMotion.Expression expression,
                       boolean hasDirective, AvatarBodyCue body) {
            this.text = text;
            this.gesture = gesture;
            this.expression = expression;
            this.hasDirective = hasDirective;
            this.body = body;
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
                + "Reír al contar algo divertido: [[salve_motion:LAUGH:WARM]]\n"
                + "Mostrar tristeza: [[salve_motion:CRY:SAD]]\n"
                + "Mostrar determinación: [[salve_motion:NONE:ANGRY]]\n"
                + "Mostrar asombro: [[salve_motion:STARTLE:SURPRISED]]\n"
                + "Mostrar timidez: [[salve_motion:NONE:SHY]]\n"
                + "Caminar mientras hablas: [[salve_motion:EXPLAIN:NEUTRAL:WALK]]\n"
                + "Sentarse: [[salve_motion:NONE:NEUTRAL:SIT]]\n"
                + "Arrodillarse: [[salve_motion:NONE:NEUTRAL:KNEEL]]\n"
                + "Cuclillas: [[salve_motion:NONE:NEUTRAL:CROUCH]]\n"
                + "Baile ilustrado pop: [[salve_motion:NONE:WARM:DANCE_POP]]\n"
                + "Baile ilustrado urbano: [[salve_motion:NONE:WARM:DANCE_URBAN]]\n"
                + "Volver a ponerse de pie: [[salve_motion:NONE:NEUTRAL:STAND]]\n"
                + "Omitir la tercera parte conserva la postura actual durante la conversación. "
                + "Usa cambios de postura al solicitarlos el usuario. Las poses sentada, de rodillas y cuclillas "
                + "son vistas 2D del traje núcleo; los bailes son movimiento suave de la ilustración, no coreografías 3D. "
                + "La risa es un gesto visual, no una grabación de audio. No prometas movimientos 3D aún no disponibles. "
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
        AvatarBodyCue body = AvatarBodyCue.KEEP;
        boolean valid = false;
        if (count == 1) {
            String tail = raw.substring(first).trim();
            if (tail.length() <= 96) {
                Matcher directive = DIRECTIVE.matcher(tail);
                if (directive.matches()) {
                    gesture = AvatarMotion.Gesture.valueOf(directive.group(1));
                    expression = AvatarMotion.Expression.valueOf(directive.group(2));
                    body = directive.group(3) == null ? AvatarBodyCue.KEEP : AvatarBodyCue.valueOf(directive.group(3));
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
        return new Result(clean.toString().trim(), gesture, expression, valid, body);
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
            case "rie": case "riete": case "sonrie":
                return directive("Así sonrío.", AvatarMotion.Gesture.LAUGH, AvatarMotion.Expression.WARM);
            case "llora": case "muestra tristeza":
                return directive("Esta es mi expresión de tristeza.", AvatarMotion.Gesture.CRY, AvatarMotion.Expression.SAD);
            case "enfadate": case "muestra enfado":
                return directive("Así muestro enfado.", AvatarMotion.Gesture.NONE, AvatarMotion.Expression.ANGRY);
            case "sorprendete": case "muestra sorpresa":
                return directive("¡Oh!", AvatarMotion.Gesture.STARTLE, AvatarMotion.Expression.SURPRISED);
            case "muestra timidez":
                return directive("Así.", AvatarMotion.Gesture.NONE, AvatarMotion.Expression.SHY);
            case "camina": case "camina mientras hablamos": case "camina mientras hablas":
                return body("Puedo moverme mientras hablamos.", AvatarBodyCue.WALK);
            case "sientate": return body("Me siento.", AvatarBodyCue.SIT);
            case "arrodillate": case "ponte de rodillas": return body("Me pongo de rodillas.", AvatarBodyCue.KNEEL);
            case "agachate": case "ponte en cuclillas": return body("Me pongo en cuclillas.", AvatarBodyCue.CROUCH);
            case "baila": case "baila pop": case "baila k-pop": return body("Vamos con un poco de ritmo.", AvatarBodyCue.DANCE_POP);
            case "baila urbano": case "baila estilo urbano": return body("Vamos con ese ritmo.", AvatarBodyCue.DANCE_URBAN);
            case "ponte de pie": case "deja de bailar": case "deja de caminar": return body("Me quedo de pie.", AvatarBodyCue.STAND);
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
        return new Result(text, gesture, expression, true, AvatarBodyCue.KEEP);
    }
    private static Result body(String text, AvatarBodyCue body) {
        return new Result(text, AvatarMotion.Gesture.NONE, AvatarMotion.Expression.NEUTRAL, true, body);
    }
    private static Result empty(String text) {
        return new Result(text, AvatarMotion.Gesture.NONE, AvatarMotion.Expression.NEUTRAL, false, AvatarBodyCue.KEEP);
    }
    private static String normalize(String value) {
        if (value == null || value.length() > 2000) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .trim().toLowerCase(Locale.ROOT).replaceAll("[.!¡]+$", "").trim();
    }
}
