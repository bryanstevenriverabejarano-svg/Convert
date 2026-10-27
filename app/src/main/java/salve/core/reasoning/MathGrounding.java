package salve.core.reasoning;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las cuentas de la conversación se hacen con {@link ExactCalculator}, no con el modelo:
 * <ol>
 *   <li>Antes de generar: si Bryan pide una cuenta, el resultado exacto entra en el contexto.</li>
 *   <li>Al revisar el borrador: si falta el resultado o una igualdad «a op b = c» es falsa, se pide
 *       reescribir (verificación con herramienta, como en T1, Kang et al. 2025).</li>
 *   <li>Último recurso: si la reescritura sigue sin el número correcto, se añade el resultado exacto.</li>
 * </ol>
 * Los modelos pequeños calculan peor de lo que parece y, sobre todo, no detectan sus propios errores
 * aritméticos («validation gap», Bertolazzi et al. 2025): por eso la comprobación es externa y determinista.
 */
public final class MathGrounding {
    private MathGrounding() { }

    private static final Pattern CUE = Pattern.compile(
            "^(?:salve[,:]?\\s+)?(?:y\\s+)?(?:cuanto (?:es|son|da|sale|seria|serian)|calcula(?:me)?|calcular|resuelve|"
            + "dime cuanto (?:es|son|da)|cual es el resultado de|resultado de|haz la cuenta(?: de)?|opera)\\s*:?\\s+(.+)$");
    /** Una cuenta sin pregunta: «345*12», «2^10». Solo símbolos, para no confundir frases. */
    private static final Pattern BARE = Pattern.compile("^[0-9.,()\\s]+[-+*/^x×÷%][0-9.,()\\s+*/^x×÷%-]+$");
    private static final Pattern EQUATION = Pattern.compile(
            "(?<![0-9.,])([0-9]+(?:[.,][0-9]+)*)\\s*([-+×x*/÷])\\s*([0-9]+(?:[.,][0-9]+)*)\\s*=\\s*(-?[0-9]+(?:[.,][0-9]+)*)(?![0-9])");
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(?:[.,][0-9]+)*");

    /** La cuenta que pide Bryan en este mensaje, ya calculada; null si no pide ninguna. */
    public static ExactCalculator.Result requested(String input) {
        if (input == null || input.length() > 200 || input.indexOf('\n') >= 0) return null;
        String t = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceFirst("^[¿¡\\s]+", "").replaceFirst("[?!.\\s]+$", "").trim();
        Matcher cue = CUE.matcher(t);
        String expr = cue.matches() ? cue.group(1) : BARE.matcher(t).matches() ? t : null;
        if (expr == null) return null;
        expr = expr.replaceFirst("^(?:el resultado de|la cuenta|el)\\s+", "").trim();
        return ExactCalculator.evaluate(expr);
    }

    /** Bloque de contexto para el modelo; vacío si no hay cuenta. */
    public static String promptBlock(String input) {
        ExactCalculator.Result r = requested(input);
        if (r == null) return "";
        return " | CALCULO_EXACTO (hecho con calculadora, no lo rehagas ni lo redondees): "
                + r.expression + " = " + r.spanish()
                + ". Da este resultado tal cual y, si ayuda, explica en una frase cómo se obtiene.";
    }

    /** Corrección para la reescritura, o null si las cuentas del borrador están bien. */
    public static String problem(String input, String draft) {
        if (draft == null || draft.trim().isEmpty()) return null;
        ExactCalculator.Result r = requested(input);
        if (r != null && !mentions(draft, r.value))
            return "el resultado exacto de " + r.expression + " es " + r.spanish()
                    + " (calculado): dilo así, sin otro número como resultado";
        String wrong = falseEquation(draft);
        return wrong == null ? null : "escribiste " + wrong + ", y es falso: corrige esa cuenta";
    }

    /** Último recurso tras la reescritura: garantiza que el número correcto llega a Bryan. */
    public static String ensureResult(String input, String reply) {
        ExactCalculator.Result r = requested(input);
        if (r == null || reply == null || mentions(reply, r.value)) return reply;
        return reply.trim() + (reply.trim().isEmpty() ? "" : "\n\n") + "Resultado exacto: " + r.expression + " = " + r.spanish() + ".";
    }

    /** ¿Aparece el valor en el texto, aunque sea redondeado a los decimales que escribió? */
    static boolean mentions(String text, BigDecimal value) {
        Matcher m = NUMBER.matcher(text);
        while (m.find()) {
            String raw = m.group();
            boolean negative = raw.startsWith("-");
            BigDecimal said;
            try { said = ExactCalculator.parseNumber(negative ? raw.substring(1) : raw); }
            catch (NumberFormatException skip) { continue; }
            if (negative) said = said.negate();
            if (sameAsWritten(said, value)) return true;
        }
        return false;
    }

    /** 397,8 cuenta como 397,80 y 0,33 como 0,3333…; 400 no cuenta como 397,8. */
    static boolean sameAsWritten(BigDecimal said, BigDecimal exact) {
        int decimals = decimalsOf(said);
        // Redondear está bien con 2 o más decimales (0,33 por 0,333…); con menos, cambia la respuesta (397 no es 397,8).
        if (decimals < 2 && decimalsOf(exact) > decimals) return false;
        return exact.setScale(decimals, RoundingMode.HALF_UP).compareTo(said.setScale(decimals, RoundingMode.HALF_UP)) == 0;
    }

    private static int decimalsOf(BigDecimal v) { return Math.max(0, v.stripTrailingZeros().scale()); }

    /** Primera igualdad «a op b = c» falsa del borrador, o null. */
    static String falseEquation(String draft) {
        Matcher m = EQUATION.matcher(draft);
        while (m.find()) {
            ExactCalculator.Result r = ExactCalculator.evaluate(m.group(1) + " " + m.group(2) + " " + m.group(3));
            if (r == null) continue;
            String rhs = m.group(4);
            boolean negative = rhs.startsWith("-");
            BigDecimal said;
            try { said = ExactCalculator.parseNumber(negative ? rhs.substring(1) : rhs); }
            catch (NumberFormatException skip) { continue; }
            if (negative) said = said.negate();
            if (!sameAsWritten(said, r.value)) return m.group().trim() + " (es " + r.spanish() + ")";
        }
        return null;
    }
}
