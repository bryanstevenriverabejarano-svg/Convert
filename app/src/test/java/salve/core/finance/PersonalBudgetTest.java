package salve.core.finance;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;
import static salve.core.finance.PersonalBudget.Category.*;

public class PersonalBudgetTest {
    private static BigDecimal n(String number) { return new BigDecimal(number); }
    private static void number(String expected, BigDecimal actual) { assertEquals(0, n(expected).compareTo(actual)); }
    private static PersonalBudget salary(String amount) { return new PersonalBudget().withMonthlyIncome(n(amount), "EUR"); }
    private static PersonalBudget usual() {
        return salary("2000").withExpense("Alquiler", n("700"), NEED, "EUR")
                .withExpense("Comida", n("250"), NEED, "EUR").withExpense("Ocio", n("150"), WANT, "EUR");
    }

    @Test public void exactMonthlyProjectionSeparatesRecordedExpensesAndSavings() {
        PersonalBudget.Summary result = usual().summary();
        number("950", result.needs); number("150", result.wants); number("1100", result.totalExpenses);
        number("900", result.remainder); number("400", result.targetSavings); number("400", result.feasibleSavings);
        number("500", result.remainingAfterSavings); number("0", result.deficit);
        assertTrue(result.provisional);
    }

    @Test public void unknownIncomeNeverLooksLikeZeroSalaryOrSpendableMoney() {
        PersonalBudget budget = new PersonalBudget().withExpense("Alquiler", n("700"), NEED, "EUR");
        assertNull(budget.monthlyIncome);
        number("700", budget.summary().needs);
        assertNull(budget.summary().remainder); assertNull(budget.summary().feasibleSavings);
        assertNull(budget.summary().deficit); assertNull(budget.summary().referenceSavings20);
    }

    @Test public void repeatedNamedExpenseUpdatesInsteadOfDuplicatingAndDoesNotMutateOriginal() {
        PersonalBudget original = usual();
        PersonalBudget updated = original.withExpense("  ALQUILER  ", n("720"), NEED, "EUR");
        assertEquals(3, updated.expenses.size()); number("970", updated.summary().needs);
        number("950", original.summary().needs);
        assertThrows(UnsupportedOperationException.class, () -> updated.expenses.clear());
    }

    @Test public void updateMayChangeCategoryWithoutDoubleCounting() {
        PersonalBudget updated = usual().withExpense("Ocio", n("150"), NEED, "EUR");
        number("1100", updated.summary().needs); number("0", updated.summary().wants);
    }

    @Test public void compatibilitySpacesAndRepeatedSpacesDoNotCreateDuplicateExpenseNames() {
        PersonalBudget budget = salary("2000").withExpense("Seguro del coche", n("35"), NEED, "EUR")
                .withExpense("\u00a0SEGURO   DEL COCHE\u00a0", n("40"), NEED, "EUR");
        assertEquals(1, budget.expenses.size()); number("40", budget.summary().needs);
    }

    @Test public void deficitPreventsImpossibleSavingsAndRemainsVisible() {
        PersonalBudget.Summary result = salary("1000").withExpense("Vivienda", n("1100"), NEED, "EUR").summary();
        number("-100", result.remainder); number("100", result.deficit);
        number("200", result.targetSavings); number("0", result.feasibleSavings);
        number("-100", result.remainingAfterSavings);
    }

    @Test public void positiveRemainderCapsSavingsWithoutInventingExtraFunds() {
        PersonalBudget.Summary result = salary("1000").withExpense("Vivienda", n("950"), NEED, "EUR").summary();
        number("200", result.targetSavings); number("50", result.feasibleSavings);
        number("0", result.remainingAfterSavings);
    }

    @Test public void referenceRuleDoesNotOverrideActualNeedsOrUserSavingsTarget() {
        PersonalBudget.Summary result = salary("2000").withExpense("Necesidades", n("1400"), NEED, "EUR")
                .withSavingsTargetPercent(n("5")).summary();
        number("1400", result.needs); number("1000", result.referenceNeeds50);
        number("600", result.referenceWants30); number("400", result.referenceSavings20);
        number("100", result.targetSavings); number("100", result.feasibleSavings);
    }

    @Test public void zeroIncomeIsAnExplicitValueAndStillDetectsDeficit() {
        PersonalBudget.Summary result = salary("0").withExpense("Luz", n("10"), NEED, "EUR").summary();
        number("0", result.feasibleSavings); number("10", result.deficit); number("-10", result.remainder);
    }

    @Test public void onlyExplicitCompletenessMakesProjectionNonprovisional() {
        PersonalBudget complete = usual().withExpensesComplete(true);
        assertFalse(complete.summary().provisional);
        assertTrue(complete.withExpense("Agua", n("20"), NEED, "EUR").summary().provisional);
        assertFalse(complete.withExpense("Comida", n("270"), NEED, "EUR").summary().provisional);
        assertTrue(complete.withoutExpense("Comida").summary().provisional);
        assertTrue(complete.withoutAllExpenses().summary().provisional);
        assertTrue(salary("2000").summary().provisional);
    }

    @Test public void deletingExpenseAndAllExpensesPreservesSalaryAndSavingConfiguration() {
        PersonalBudget budget = usual().withSavingsTargetPercent(n("15"));
        PersonalBudget removed = budget.withoutExpense("ALQUILER");
        assertEquals(2, removed.expenses.size()); number("400", removed.summary().totalExpenses);
        assertSame(removed, removed.withoutExpense("No existe"));
        PersonalBudget cleared = removed.withoutAllExpenses();
        assertTrue(cleared.expenses.isEmpty()); number("2000", cleared.monthlyIncome);
        number("15", cleared.savingsTargetPercent);
        assertNull(new PersonalBudget().currency);
    }

    @Test public void forgettingSalaryDoesNotEraseExpensesOrInventZeroIncome() {
        PersonalBudget forgotten = usual().withoutIncome();
        assertNull(forgotten.monthlyIncome); assertNull(forgotten.summary().feasibleSavings);
        assertEquals("EUR", forgotten.currency); assertEquals(3, forgotten.expenses.size());
        number("1100", forgotten.summary().totalExpenses);
    }

    @Test public void currencyFractionsAndRoundingNeverOverspend() {
        PersonalBudget.Summary tiny = salary("0.03").withSavingsTargetPercent(n("50")).summary();
        number("0.01", tiny.feasibleSavings); number("0.02", tiny.remainingAfterSavings);
        PersonalBudget yen = new PersonalBudget().withMonthlyIncome(n("101"), "JPY");
        number("20", yen.summary().feasibleSavings);
        assertThrows(IllegalArgumentException.class, () -> yen.withExpense("Comida", n("1.5"), NEED, "JPY"));
        PersonalBudget dinars = new PersonalBudget().withMonthlyIncome(n("1.001"), "KWD");
        number("0.200", dinars.summary().feasibleSavings);
    }

    @Test public void decimalAmountsAreExact() {
        PersonalBudget.Summary result = salary("0.30").withExpense("A", n("0.10"), NEED, "EUR")
                .withExpense("B", n("0.20"), WANT, "EUR").summary();
        number("0", result.remainder); number("0", result.feasibleSavings); number("0", result.deficit);
    }

    @Test public void rejectsMixedInvalidAndUnspecifiedCurrency() {
        assertThrows(IllegalArgumentException.class, () -> salary("2000").withMonthlyIncome(n("2000"), "USD"));
        assertThrows(IllegalArgumentException.class, () -> usual().withExpense("Viajes", n("100"), WANT, "USD"));
        for (String currency : new String[] {null, "", "$", "ZZZ", "XXX", "EURUSD"}) {
            assertThrows(IllegalArgumentException.class, () -> new PersonalBudget().withMonthlyIncome(n("1"), currency));
        }
        assertEquals("EUR", new PersonalBudget().withMonthlyIncome(n("1"), " eur ").currency);
    }

    @Test public void boundsRejectNegativeHugeAmountsAndInvalidSavingPercent() {
        for (String amount : new String[] {"-0.01", "1000000000001", "0.001", "1E-100", "1E100"})
            assertThrows(IllegalArgumentException.class, () -> salary(amount));
        assertThrows(IllegalArgumentException.class, () -> new PersonalBudget().withMonthlyIncome(null, "EUR"));
        for (String percent : new String[] {"-1", "101", "20.001", "1E-100"})
            assertThrows(IllegalArgumentException.class, () -> usual().withSavingsTargetPercent(n(percent)));
        number("0", usual().withSavingsTargetPercent(n("0")).summary().feasibleSavings);
        number("900", usual().withSavingsTargetPercent(n("100")).summary().feasibleSavings);
    }

    @Test public void namesAndCollectionSizeAreBounded() {
        for (String name : new String[] {null, "", "  ", "a\nb", "x".repeat(81)})
            assertThrows(IllegalArgumentException.class, () -> salary("2000").withExpense(name, n("1"), NEED, "EUR"));
        PersonalBudget budget = salary("2000");
        for (int i = 0; i < 200; i++) budget = budget.withExpense("Gasto " + i, n("1"), NEED, "EUR");
        PersonalBudget full = budget;
        assertThrows(IllegalArgumentException.class, () -> full.withExpense("Extra", n("1"), NEED, "EUR"));
        assertEquals(200, full.withExpense("Gasto 0", n("2"), NEED, "EUR").expenses.size());
    }

    @Test public void strictJsonRoundTripsEmptyAndPopulatedBudgetsExactly() {
        for (PersonalBudget original : new PersonalBudget[] {new PersonalBudget(), usual().withExpensesComplete(true),
                new PersonalBudget().withExpense("Internet \\\"casa\\\"", n("29.95"), NEED, "EUR")}) {
            PersonalBudget restored = PersonalBudget.fromJson(original.toJson());
            assertEquals(original.toJson(), restored.toJson());
            assertEquals(original.expensesComplete, restored.expensesComplete);
        }
    }

    @Test public void persistenceRejectsAmbiguousMissingUnknownAndTrailingData() {
        String good = usual().toJson();
        for (String bad : new String[] {null, "", "{}", "[]", good + "{}", good + " false", good.replace("\"version\":1", "\"version\":2"),
                good.replace("\"version\":1", "\"version\":1,\"version\":1"), good.replace("\"version\":1", "\"unknown\":1"),
                good.replace("\"monthlyIncome\":2000.00", "\"monthlyIncome\":\"2000\""),
                good.replace("\"monthlyIncome\":2000.00", "\"monthlyIncome\":2e3"),
                good.replace("\"monthlyIncome\":2000.00", "\"monthlyIncome\":-1"),
                good.replace("\"expensesComplete\":false", "\"expensesComplete\":\"false\""),
                good.replace("\"category\":\"NEED\"", "\"category\":\"UNKNOWN\""),
                good.replace("\"currency\":\"EUR\"", "\"currency\":null"), " ".repeat(65537)}) {
            assertThrows("Accepted malformed schema: " + bad, IllegalArgumentException.class, () -> PersonalBudget.fromJson(bad));
        }
    }

    @Test public void persistenceRejectsDuplicateExpenseNamesAndExpenseFields() {
        String good = usual().toJson();
        assertThrows(IllegalArgumentException.class, () -> PersonalBudget.fromJson(good.replace("\"Comida\"", "\"ALQUILER\"")));
        assertThrows(IllegalArgumentException.class, () -> PersonalBudget.fromJson(good.replace("\"name\":\"Ocio\"", "\"name\":\"Ocio\",\"name\":\"Ocio\"")));
        assertThrows(IllegalArgumentException.class, () -> PersonalBudget.fromJson(good.replace("\"name\":\"Ocio\",", "")));
    }
}
