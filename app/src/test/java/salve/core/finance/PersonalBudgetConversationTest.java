package salve.core.finance;

import java.math.BigDecimal;
import org.junit.Test;
import static org.junit.Assert.*;

public class PersonalBudgetConversationTest {
    private final PersonalBudgetConversation conversation = new PersonalBudgetConversation();
    private PersonalBudget budget = new PersonalBudget();

    private PersonalBudgetConversation.Response say(String input) {
        assertTrue("Private route must catch: " + input, conversation.handles(input));
        PersonalBudgetConversation.Response response = conversation.handle(input, budget);
        if (response.nextBudget != null) budget = response.nextBudget;
        if (response.clearAll) budget = new PersonalBudget();
        return response;
    }
    private void amount(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
    private void unchanged(String input) {
        String before = budget.toJson();
        PersonalBudgetConversation.Response response = conversation.handle(input, budget);
        assertFalse(input, response.changed);
        assertNull(input, response.nextBudget);
        assertFalse(input, response.clearAll);
        assertEquals(before, budget.toJson());
    }

    @Test public void recordsExplicitNetMonthlySalaryAndExactDecimalComma() {
        PersonalBudgetConversation.Response response = say("Mi sueldo es 1800,25 euros netos al mes.");
        assertTrue(response.changed);
        amount("1800.25", budget.monthlyIncome);
        assertEquals("EUR", budget.currency);
        assertFalse(budget.expensesComplete);
    }

    @Test public void netMonthlyQualifiersCanPrecedeAmount() {
        say("mi salario mensual neto es 1900 EUR");
        amount("1900", budget.monthlyIncome);
    }

    @Test public void missingSalaryDetailsAreOnlyEphemeralUntilClarified() {
        assertFalse(say("cobro 1800").changed);
        assertNull(budget.monthlyIncome);
        assertTrue(conversation.hasPendingClarification());
        assertFalse(say("EUR").changed);
        assertNull(budget.monthlyIncome);
        assertTrue(say("netos al mes").changed);
        amount("1800", budget.monthlyIncome);
        assertFalse(conversation.hasPendingClarification());
    }

    @Test public void changingSubjectOrCancellingDiscardsPendingDetails() {
        say("cobro 1800");
        assertFalse(conversation.handles("hablemos de cine"));
        // Routing is a pure query; the caller cancels pending state when the topic changes.
        assertTrue(conversation.hasPendingClarification());
        conversation.resetPending();
        assertFalse(conversation.hasPendingClarification());
        unchanged("EUR netos al mes");
        say("cobro 1800");
        assertFalse(say("cancela").changed);
        assertFalse(conversation.hasPendingClarification());
        assertNull(budget.monthlyIncome);
    }

    @Test public void doesNotConvertGrossWeeklyAnnualOrOneTimeAmounts() {
        for (String input : new String[]{"mi sueldo es 1800 EUR brutos al mes",
                "cobro 1800 EUR netos al ano", "cobro 450 EUR netos por semana",
                "pago 650 EUR de alquiler este mes", "gasto 20 EUR en comida hoy"}) unchanged(input);
        assertNull(budget.monthlyIncome);
        assertTrue(budget.expenses.isEmpty());
    }

    @Test public void repeatedExpenseReplacesRatherThanDoubleCounts() {
        say("pago 650 euros de alquiler al mes");
        say("gasto 200 euros en comida al mes");
        say("pago 700 EUR de alquiler al mes");
        assertEquals(2, budget.expenses.size());
        amount("900", budget.summary().totalExpenses);
        amount("900", budget.summary().needs);
        amount("0", budget.summary().wants);
    }

    @Test public void missingExpenseFrequencyIsClarifiedBeforeWriting() {
        assertFalse(say("pago 650 EUR de alquiler").changed);
        assertTrue(budget.expenses.isEmpty());
        say("al mes");
        amount("650", budget.summary().totalExpenses);
    }

    @Test public void establishedCurrencyCarriesForwardButMonthlyPeriodIsNotInvented() {
        say("cobro 1800 EUR netos al mes");
        assertFalse(say("pago 650 de alquiler").changed);
        assertTrue(budget.expenses.isEmpty());
        PersonalBudgetConversation.Response response = say("mensual");
        assertTrue(response.reply.contains("650 EUR"));
        amount("650", budget.summary().totalExpenses);
        unchanged("gasto 30 pesos en cine al mes");
    }

    @Test public void affirmativeClarificationOnlyCompletesExplicitMissingDetails() {
        say("mi sueldo es 1800 EUR");
        assertTrue(say("Sí, netos al mes").changed);
        amount("1800", budget.monthlyIncome);
        unchanged("si mi sueldo es 2000 EUR netos al mes");
    }

    @Test public void userCanReviewNamedExpensesBeforeCorrectingThem() {
        say("pago 650 EUR de alquiler al mes");
        say("gasto 200 EUR en comida al mes");
        PersonalBudgetConversation.Response response = say("¿Qué gastos tengo?");
        assertFalse(response.changed);
        assertTrue(response.reply.contains("alquiler: 650 EUR"));
        assertTrue(response.reply.contains("comida: 200 EUR"));
        assertTrue(response.reply.contains("Total registrado: 850 EUR"));
    }

    @Test public void userClassifiesAmbiguousExpenseBeforeWriting() {
        assertFalse(say("gasto 60 EUR en baile al mes").changed);
        assertTrue(budget.expenses.isEmpty());
        say("ocio");
        assertEquals("baile", budget.expenses.get(0).name);
        assertEquals(PersonalBudget.Category.WANT, budget.expenses.get(0).category);
        amount("60", budget.summary().wants);
    }

    @Test public void acceptsExplicitCurrenciesButDoesNotMixOrGuessThem() {
        unchanged("cobro 1800 pesos netos al mes");
        unchanged("cobro 1800 dolares netos al mes");
        say("cobro 1800 USD netos al mes");
        unchanged("pago 650 EUR de alquiler al mes");
        unchanged("cobro 1900 USD EUR netos al mes");
        amount("1800", budget.monthlyIncome);
        assertEquals("USD", budget.currency);
    }

    @Test public void explicitColombianPesosAndDollarsAreRecognized() {
        say("recibo 2500000 pesos colombianos netos al mes");
        assertEquals("COP", budget.currency);
        say("borra mi presupuesto");
        say("cobro 2000 dolares estadounidenses netos al mes");
        assertEquals("USD", budget.currency);
    }

    @Test public void questionsNegationHypothesesQuotesAndCompanyDataNeverMutate() {
        say("mi sueldo es 1800 EUR netos al mes");
        for (String input : new String[]{"¿mi sueldo es 2000 EUR netos al mes?",
                "mi sueldo no es 2000 EUR netos al mes", "si mi sueldo es 2000 EUR netos al mes",
                "imagina que cobro 2000 EUR netos al mes", "mi empresa paga 2000 EUR netos al mes",
                "mi hermano cobra 2000 EUR netos al mes", "\"cobro 2000 EUR netos al mes\"",
                "cobro 2000 EUR netos al mes, por ejemplo", "no borres mi presupuesto",
                "cobro 2000 EUR netos al mes y pago 650 EUR de alquiler al mes"}) unchanged(input);
        amount("1800", budget.monthlyIncome);
    }

    @Test public void estimatesRemainProvisionalUntilUserExplicitlyCompletesExpenses() {
        say("cobro 1800 EUR netos al mes");
        say("pago 650 EUR de alquiler al mes");
        say("gasto 200 EUR en comida al mes");
        PersonalBudgetConversation.Response partial = say("¿Cuánto puedo gastar?");
        assertFalse(partial.changed);
        assertTrue(partial.reply.contains("provisionales"));
        assertTrue(partial.reply.contains("590 EUR"));
        assertTrue(partial.reply.contains("no indican cuánto puedes gastar con seguridad"));
        say("esos son todos mis gastos");
        assertTrue(budget.expensesComplete);
        PersonalBudgetConversation.Response complete = say("cómo va mi presupuesto");
        assertTrue(complete.reply.contains("no el saldo disponible de hoy"));
        say("gasto 15 EUR en cine al mes");
        assertFalse(budget.expensesComplete);
    }

    @Test public void savingsTargetIsUserControlledAndBounded() {
        say("quiero ahorrar el 25,5%");
        amount("25.5", budget.savingsTargetPercent);
        unchanged("quiero ahorrar el 101%");
        unchanged("quiero ahorrar el -5%");
        say("me gustaría ahorrar el 10%");
        amount("10", budget.savingsTargetPercent);
    }

    @Test public void addressingSalveWorksForDeclarationsAndQuestions() {
        say("Salve, mi sueldo es 1800 EUR netos al mes");
        amount("1800", budget.monthlyIncome);
        assertTrue(say("Salve, ¿cuánto puedo ahorrar?").reply.contains("Proyección mensual"));
    }

    @Test public void percentageReferenceDoesNotOverrideRealExpensesOrChosenGoal() {
        say("cobro 1800 EUR netos al mes");
        say("pago 1100 EUR de alquiler al mes");
        say("quiero ahorrar el 10%");
        String reply = say("distribuye mi dinero").reply;
        assertTrue(reply.contains("necesidades 900 EUR, ocio 540 EUR y ahorro 360 EUR"));
        assertTrue(reply.contains("10%: 180 EUR"));
        amount("1100", budget.summary().needs);
        amount("10", budget.savingsTargetPercent);
    }

    @Test public void deficitDoesNotTurnIntoAFalseSpendingAllowance() {
        say("cobro 500 EUR netos al mes");
        say("pago 650 EUR de alquiler al mes");
        PersonalBudgetConversation.Response response = say("cuánto puedo ahorrar");
        assertTrue(response.reply.contains("superan el ingreso en 150 EUR"));
        assertTrue(response.reply.contains("no hay margen estimado de ahorro"));
        assertFalse(response.reply.contains("quedarían -"));
    }

    @Test public void missingIncomeRemainsUnknownAndZeroIncomeIsExplicit() {
        say("pago 650 EUR de alquiler al mes");
        assertTrue(say("mi presupuesto").reply.contains("Falta tu ingreso neto mensual"));
        say("mi sueldo es 0 EUR netos al mes");
        amount("0", budget.monthlyIncome);
        assertTrue(say("mi presupuesto").reply.contains("superan el ingreso en 650 EUR"));
    }

    @Test public void individualAndWholeBudgetDeletionAreExplicit() {
        say("cobro 1800 EUR netos al mes");
        say("pago 650 EUR de alquiler al mes");
        say("olvida mi sueldo");
        assertNull(budget.monthlyIncome);
        assertEquals(1, budget.expenses.size());
        say("borra el gasto alquiler");
        assertTrue(budget.expenses.isEmpty());
        assertFalse(say("borra el gasto alquiler").changed);
        assertTrue(say("borra mi presupuesto").clearAll);
        assertNull(budget.currency);
    }

    @Test public void malformedAmountAndMultipleExpensesDoNotPartiallySave() {
        for (String input : new String[]{"cobro 1.800 EUR netos al mes", "cobro -1800 EUR netos al mes",
                "cobro 1e3 EUR netos al mes", "cobro 1000000000001 EUR netos al mes",
                "gasto 20 EUR en cine y 30 EUR en comida al mes",
                "gasto 10,123 EUR en cine al mes"}) unchanged(input);
    }

    @Test public void directExpenseDeclarationsAndUserTypingSlashesAreSupported() {
        say("mi alquiler es 650 EUR al mes");
        say("mi internet es 35 EUR al mes");
        say("mi sue/ldo es 1800 EUR ne/tos al mes");
        amount("685", budget.summary().totalExpenses);
        amount("1800", budget.monthlyIncome);
    }

    @Test public void restoringFromDiskNeedsNoDialogueHistory() {
        say("cobro 1800 EUR netos al mes");
        say("pago 650 EUR de alquiler al mes");
        PersonalBudget restored = PersonalBudget.fromJson(budget.toJson());
        PersonalBudgetConversation fresh = new PersonalBudgetConversation();
        assertFalse(fresh.hasPendingClarification());
        String reply = fresh.handle("cuánto puedo ahorrar", restored).reply;
        assertTrue(reply.contains("360 EUR"));
        assertTrue(reply.contains("790 EUR"));
    }
}
