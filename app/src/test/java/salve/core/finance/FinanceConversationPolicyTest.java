package salve.core.finance;

import org.junit.Test;
import static org.junit.Assert.*;

public class FinanceConversationPolicyTest {
    @Test public void protectsIncomeExpenseAndSavingsDeclarationsBeforeParsing() {
        for (String input : new String[] {
                "Mi sueldo es 2000 euros netos al mes", "Mi suel/do es dos mil euros",
                "Guarda mi nómina de 1.200,50 EUR", "cobro 3000 USD", "gano dos mil euros",
                "Pago alquiler 700 al mes", "gasto 250 en supermercado", "ahorro 200 euros",
                "Mis gastos son alquiler, luz y comida", "mi sueldo está pendiente de confirmar",
                "Mi hipoteca es 800", "Quiero ahorrar el 20%", "quiero ahorrar más",
                "Objetivo de ahorro: 20%", "Ingreso mensual: 2000 EUR", "saldo disponible: 250 EUR",
                "Mi empresa me paga 2000 euros al mes", "Me gustaría ahorrar el 25%",
                "recibo 1800 EUR netos al mes", "mi seguro es 50 EUR al mes",
                "mi internet es 35 EUR al mes", "mi transporte es 60 EUR al mes",
                "Tengo 500 euros", "Tengo una deuda de 2500", "Me quedan 350 EUR"
        }) assertTrue(input, FinanceConversationPolicy.isPrivateBudgetInput(input));
    }

    @Test public void protectsQueriesUpdatesDeletionAndCompleteness() {
        for (String input : new String[] {
                "¿Cuánto puedo gastar?", "¿cuánto puedo ahorrar?", "cuánto dinero me queda",
                "¿Puedo gastar 50 euros?", "Enséñame mi presupuesto", "Mi sueldo ahora es 2500",
                "borra el gasto supermercado", "olvida mi sueldo", "borra mi presupuesto",
                "Esos son todos mis gastos", "esos son todos los gastos", "gastos completos",
                "cambia la moneda a EUR", "reparte mi dinero", "resumen de presupuesto",
                "¿Qué gastos tengo?", "¿Cuánto gasto?"
        }) assertTrue(input, FinanceConversationPolicy.isPrivateBudgetInput(input));
    }

    @Test public void incompleteUnsupportedAndQuotedPersonalDataDoesNotLeakToModel() {
        for (String input : new String[] {
                "Mi sueldo es algo irregular", "Mi sueldo = NaN", "Mi salario es -2000",
                "Si mi sueldo fuera 2000", "El usuario dijo: mi salario son 2300 euros",
                "Re: ingreso mensual: 2000 EUR; saldo disponible: 250 EUR",
                "Mi IBAN es ES0000000000000000000000", "Guarda mi tarjeta de crédito"
        }) assertTrue(input, FinanceConversationPolicy.isPrivateBudgetInput(input));
    }

    @Test public void leavesEducationalBusinessAndUnrelatedConversationsOnGeneralPath() {
        for (String input : new String[] {
                null, "", "Hola Salve", "gano puntos en ajedrez", "gasto tiempo pensando",
                "gano 10 puntos en ajedrez", "gasto 2 horas pensando", "recibo 50 mensajes al día",
                "Mi teléfono es S24", "Mi internet no funciona", "Tengo 2 hijos", "Me quedan 3 vidas",
                "Crea una cama", "¿Qué es un sueldo neto?", "Explícame la regla 50/30/20",
                "¿Cómo organizo una empresa?", "¿Cómo calculo el margen de mi negocio?",
                "¿Cómo reducir los gastos de mi empresa?", "¿Qué es mi presupuesto empresarial?",
                "¿Qué significa flujo de caja?", "¿Cómo pagar impuestos en España?",
                "¿Cómo calculo el sueldo de mis empleados con 20 horas semanales?",
                "Mi empresa factura 10000 al mes, ¿qué margen tiene?"
        }) assertFalse(String.valueOf(input), FinanceConversationPolicy.isPrivateBudgetInput(input));
    }

    @Test public void personalAmountsRemainPrivateInsideMixedBusinessQuestion() {
        assertTrue(FinanceConversationPolicy.isPrivateBudgetInput(
                "Mi empresa factura 10000, pero mi sueldo es 2000. ¿Cuánto puedo ahorrar?"));
    }

    @Test public void educationalContextCoversBusinessAndUncertaintyWithoutLoadingAmounts() {
        String context = FinanceConversationPolicy.contextFor("¿Cómo administro una empresa?");
        assertFalse(context.isEmpty());
        assertTrue(context.contains("beneficio y efectivo disponible no son lo mismo"));
        assertTrue(context.contains("país y período fiscal"));
        assertTrue(context.contains("fuente oficial con fecha realmente consultada"));
        assertTrue(context.contains("50/30/20 es una referencia adaptable"));
        assertEquals(context, FinanceConversationPolicy.contextFor("Tengo 2700 de sueldo"));
        assertFalse(context.contains("2700"));
        assertEquals("", FinanceConversationPolicy.contextFor("Hola, ¿cómo estás?"));
    }
}
