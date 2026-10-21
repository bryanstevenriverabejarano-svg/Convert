package salve.core.agent;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict, bounded arithmetic task from the user's literal request.
 * A model cannot broaden the expression or choose new operands in a tool call.
 */
public final class AgentArithmeticTask {
    private static final Pattern GOAL = Pattern.compile(
            "(?iu)^\\s*¿?\\s*(?:calcula|cu[aá]nto\\s+es)\\s+"
                    + "([+-]?[0-9]{1,7})\\s*([+*×-])\\s*([+-]?[0-9]{1,7})\\s*\\??\\s*$");
    private static final Pattern ANSWER = Pattern.compile(
            "(?iu)^\\s*(?:(?:el\\s+resultado\\s+es|resultado:|es)\\s*)?"
                    + "([+-]?[0-9]{1,13})\\.?\\s*$");
    public final long first, second;
    public final char operator;

    private AgentArithmeticTask(long first, char operator, long second) {
        this.first = first; this.operator = operator; this.second = second;
    }

    public static AgentArithmeticTask parse(String goal) {
        if (goal == null || goal.length() > 96) return null;
        Matcher match = GOAL.matcher(goal);
        if (!match.matches()) return null;
        try {
            long a = Long.parseLong(match.group(1));
            long b = Long.parseLong(match.group(3));
            if (Math.abs(a) > 1_000_000L || Math.abs(b) > 1_000_000L) return null;
            char operator = match.group(2).charAt(0);
            return new AgentArithmeticTask(a, operator == '×' ? '*' : operator, b);
        } catch (NumberFormatException invalid) { return null; }
    }

    /** The local executor uses Java checked arithmetic. */
    public long calculateLocally() {
        switch (operator) {
            case '+': return Math.addExact(first, second);
            case '-': return Math.subtractExact(first, second);
            case '*': return Math.multiplyExact(first, second);
            default: throw new IllegalArgumentException("Operador no admitido");
        }
    }

    /** Independent oracle: mathematically exact BigInteger arithmetic. */
    public BigInteger referenceResult() {
        BigInteger a = BigInteger.valueOf(first), b = BigInteger.valueOf(second);
        switch (operator) {
            case '+': return a.add(b);
            case '-': return a.subtract(b);
            case '*': return a.multiply(b);
            default: throw new IllegalArgumentException("Operador no admitido");
        }
    }

    /** The LLM must give a single machine-checkable value, not an explanation. */
    public boolean matchesAnswer(String text) {
        if (text == null || text.length() > 96) return false;
        Matcher match = ANSWER.matcher(text);
        if (!match.matches()) return false;
        try { return referenceResult().equals(new BigInteger(match.group(1))); }
        catch (NumberFormatException invalid) { return false; }
    }
}
