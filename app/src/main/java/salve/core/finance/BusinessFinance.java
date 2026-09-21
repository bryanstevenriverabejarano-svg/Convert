package salve.core.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Single-product operating scenario. Decimal arithmetic; never model-generated numbers. */
public final class BusinessFinance {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final class Result {
        public final BigDecimal revenue, variableCosts, contributionPerUnit, operatingResult;
        /** Null when price is zero; no meaningful percentage can be calculated. */
        public final BigDecimal contributionPercent;
        /** Null when positive sales cannot recover costs; zero when no sales are needed. */
        public final BigDecimal breakEvenUnits;
        private Result(BigDecimal price, BigDecimal variable, BigDecimal fixed, long units) {
            BigDecimal quantity = BigDecimal.valueOf(units);
            revenue = price.multiply(quantity);
            variableCosts = variable.multiply(quantity);
            contributionPerUnit = price.subtract(variable);
            operatingResult = revenue.subtract(variableCosts).subtract(fixed);
            contributionPercent = price.signum() == 0 ? null
                    : contributionPerUnit.multiply(HUNDRED).divide(price, 2, RoundingMode.HALF_UP);
            breakEvenUnits = fixed.signum() == 0 ? BigDecimal.ZERO : contributionPerUnit.signum() <= 0 ? null
                    : fixed.divide(contributionPerUnit, 0, RoundingMode.CEILING);
        }
    }
    public static Result calculate(BigDecimal price, BigDecimal variable, BigDecimal fixed, long units) {
        validateAmount(price); validateAmount(variable); validateAmount(fixed);
        if (units < 0 || units > 1_000_000_000L) throw new IllegalArgumentException("Unidades fuera del límite: 0 a 1000000000.");
        return new Result(price, variable, fixed, units);
    }
    /** Comma or dot is a decimal separator; thousands grouping is deliberately unsupported. */
    public static BigDecimal parseAmount(String raw) {
        if (raw == null || !raw.trim().matches("[0-9]{1,13}([.,][0-9]{1,4})?"))
            throw new IllegalArgumentException("Usa importes positivos sin separadores de miles y con hasta 4 decimales.");
        BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
        validateAmount(value);
        return value;
    }
    public static long parseUnits(String raw) {
        if (raw == null || !raw.trim().matches("[0-9]{1,10}")) throw new IllegalArgumentException("Las unidades deben ser un número entero positivo o cero.");
        long value = Long.parseLong(raw.trim());
        if (value > 1_000_000_000L) throw new IllegalArgumentException("Máximo 1000000000 unidades.");
        return value;
    }
    private static void validateAmount(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0
                || value.scale() > 4 || value.scale() < 0)
            throw new IllegalArgumentException("Importe fuera de límites: 0 a 1000000000000, hasta 4 decimales.");
    }
    private BusinessFinance() { }
}
