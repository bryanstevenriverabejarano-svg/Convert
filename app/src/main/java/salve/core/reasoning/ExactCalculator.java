package salve.core.reasoning;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cálculo exacto en lenguaje natural: «cuánto es 17% de 2.340», «(3 + 4) por 12 entre 7»,
 * «raíz cuadrada de 2», «2 elevado a 10». Es el «razonamiento con programa» (PAL / razonamiento
 * con herramientas): el modelo pequeño no hace la cuenta; la hace Java y el modelo la explica.
 *
 * <p>Seguro por diseño: si queda una sola palabra que no entiende, devuelve null en vez de adivinar.
 * Sin red, sin eval, sin reflexión: un analizador descendente recursivo con BigDecimal.</p>
 */
public final class ExactCalculator {
    private static final MathContext MC = MathContext.DECIMAL64;
    private static final int MAX_INPUT = 160, MAX_TOKENS = 64, MAX_POW = 400;

    private ExactCalculator() { }

    /** Resultado de una expresión ya extraída; {@link #expression} es la forma canónica («17/100*2340»). */
    public static final class Result {
        public final String expression;
        public final BigDecimal value;
        Result(String expression, BigDecimal value) { this.expression = expression; this.value = value; }

        /** Formato español: coma decimal, hasta 10 decimales, sin ceros de sobra. */
        public String spanish() { return format(value); }
    }

    /** Evalúa una expresión en español o con símbolos; null si no es una cuenta clara. */
    public static Result evaluate(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty() || t.length() > MAX_INPUT) return null;
        List<String> tokens = tokenize(words(t));
        if (tokens == null || tokens.isEmpty() || tokens.size() > MAX_TOKENS) return null;
        int numbers = 0, ops = 0;
        for (String k : tokens) {
            if (Character.isDigit(k.charAt(0))) numbers++;
            else if ("+-*/^".contains(k) || k.equals("sqrt")) ops++;
        }
        if (numbers == 0 || ops == 0) return null; // «7» solo no es una cuenta
        Parser p = new Parser(tokens);
        try {
            BigDecimal v = p.expression();
            if (p.pos != tokens.size()) return null;
            return new Result(String.join("", tokens), v.round(MC));
        } catch (ArithmeticException | IllegalStateException | NumberFormatException invalid) {
            return null;
        }
    }

    /** «2.340» (miles) → 2340; «2,5» → 2.5; «1.5» → 1.5. */
    public static BigDecimal parseNumber(String raw) {
        String s = raw.trim();
        if (s.matches("[0-9]{1,3}(?:\\.[0-9]{3})+(?:,[0-9]+)?")) s = s.replace(".", "").replace(',', '.');
        else if (s.matches("[0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]+)?") && s.indexOf('.') >= 0) s = s.replace(",", "");
        else s = s.replace(',', '.');
        if (!s.matches("[0-9]+(?:\\.[0-9]+)?")) throw new NumberFormatException(raw);
        return new BigDecimal(s);
    }

    public static String format(BigDecimal value) {
        BigDecimal v = value.setScale(Math.min(10, Math.max(0, value.scale())), RoundingMode.HALF_EVEN).stripTrailingZeros();
        if (v.compareTo(BigDecimal.ZERO) == 0) return "0";
        String plain = v.abs().compareTo(new BigDecimal("1e21")) >= 0 ? v.round(new MathContext(12)).toString() : v.toPlainString();
        return plain.replace('.', ',');
    }

    // ── Palabras → símbolos ─────────────────────────────────────────────

    static String words(String text) {
        String t = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        t = t.replace('×', '*').replace('·', '*').replace('÷', '/').replace('−', '-').replace('—', '-');
        t = t.replaceAll("(?<=[0-9])\\s*x\\s*(?=[0-9(])", "*");
        t = t.replaceAll("\\bpor ciento\\b", "%");
        t = t.replaceAll("%\\s*(?:de|del)\\b", "%*");
        t = t.replaceAll("\\bel\\s+(?=[0-9])", "");
        t = t.replaceAll("\\braiz cuadrada (?:de|del)\\b", " sqrt ");
        t = t.replaceAll("\\braiz de\\b", " sqrt ");
        t = t.replaceAll("\\bal cuadrado\\b", "^2").replaceAll("\\bal cubo\\b", "^3");
        t = t.replaceAll("\\belevado (?:a|al)(?: la)?\\b", "^");
        t = t.replaceAll("\\b(?:multiplicado por|por)\\b", "*");
        t = t.replaceAll("\\b(?:dividido (?:entre|por|para)|entre)\\b", "/");
        t = t.replaceAll("\\bmas\\b", "+").replaceAll("\\bmenos\\b", "-");
        t = t.replaceAll("\\babre parentesis\\b", "(").replaceAll("\\bcierra parentesis\\b", ")");
        return t;
    }

    static List<String> tokenize(String t) {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < t.length()) {
            char c = t.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < t.length() && (Character.isDigit(t.charAt(j))
                        || ((t.charAt(j) == '.' || t.charAt(j) == ',') && j + 1 < t.length() && Character.isDigit(t.charAt(j + 1))))) j++;
                out.add(parseNumber(t.substring(i, j)).toPlainString());
                i = j;
                continue;
            }
            if ("+-*/^()%".indexOf(c) >= 0) {
                if (c == '%') { out.add("/"); out.add("100"); }
                else out.add(String.valueOf(c));
                i++;
                continue;
            }
            if (t.startsWith("sqrt", i)) { out.add("sqrt"); i += 4; continue; }
            return null; // palabra desconocida: no se adivina
        }
        // «10%*50» queda «10/100*50»: correcto por asociatividad izquierda.
        return out;
    }

    // ── Analizador ─────────────────────────────────────────────────────

    private static final class Parser {
        final List<String> k;
        int pos;
        Parser(List<String> tokens) { this.k = tokens; }

        String peek() { return pos < k.size() ? k.get(pos) : null; }
        boolean eat(String s) { if (s.equals(peek())) { pos++; return true; } return false; }

        BigDecimal expression() {
            BigDecimal v = term();
            while (true) {
                if (eat("+")) v = v.add(term(), MC);
                else if (eat("-")) v = v.subtract(term(), MC);
                else return v;
            }
        }

        BigDecimal term() {
            BigDecimal v = unary();
            while (true) {
                if (eat("*")) v = v.multiply(unary(), MC);
                else if (eat("/")) {
                    BigDecimal d = unary();
                    if (d.signum() == 0) throw new ArithmeticException("división por cero");
                    v = v.divide(d, MC);
                } else return v;
            }
        }

        BigDecimal unary() {
            if (eat("-")) return unary().negate();
            if (eat("+")) return unary();
            return power();
        }

        BigDecimal power() {
            BigDecimal base = primary();
            if (eat("^")) {
                BigDecimal exp = unary(); // derecha: 2^3^2 = 2^9
                return pow(base, exp);
            }
            return base;
        }

        BigDecimal primary() {
            String t = peek();
            if (t == null) throw new IllegalStateException("falta un número");
            if (eat("(")) {
                BigDecimal v = expression();
                if (!eat(")")) throw new IllegalStateException("falta cerrar paréntesis");
                return v;
            }
            if (eat("sqrt")) {
                BigDecimal v = power();
                if (v.signum() < 0) throw new ArithmeticException("raíz de negativo");
                return sqrt(v);
            }
            if (Character.isDigit(t.charAt(0))) { pos++; return new BigDecimal(t); }
            throw new IllegalStateException("símbolo inesperado " + t);
        }
    }

    static BigDecimal pow(BigDecimal base, BigDecimal exp) {
        if (exp.stripTrailingZeros().scale() <= 0 && exp.abs().compareTo(BigDecimal.valueOf(MAX_POW)) <= 0) {
            int n = exp.intValueExact();
            if (n >= 0) return base.pow(n, MC);
            if (base.signum() == 0) throw new ArithmeticException("cero a potencia negativa");
            return BigDecimal.ONE.divide(base.pow(-n, MC), MC);
        }
        double r = Math.pow(base.doubleValue(), exp.doubleValue());
        if (Double.isNaN(r) || Double.isInfinite(r)) throw new ArithmeticException("potencia fuera de rango");
        return new BigDecimal(r, MC);
    }

    /** Newton con BigDecimal (BigDecimal.sqrt solo existe desde Android 13). */
    static BigDecimal sqrt(BigDecimal v) {
        if (v.signum() == 0) return BigDecimal.ZERO;
        BigDecimal x = new BigDecimal(Math.sqrt(v.doubleValue()), MC);
        BigDecimal two = BigDecimal.valueOf(2);
        for (int i = 0; i < 6; i++) x = x.add(v.divide(x, MC), MC).divide(two, MC);
        return x;
    }
}
