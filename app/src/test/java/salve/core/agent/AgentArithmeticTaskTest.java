package salve.core.agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class AgentArithmeticTaskTest {
    @Test public void heldOutOperandsAndAllOperationsUseIndependentIntegerOracle() {
        for (String expression : new String[]{
                "Calcula 72413 + 35879",
                "Calcula 418701 - 719922",
                "Calcula -931 * -87",
                "¿Cuánto es 19020 × -76?",
                "cuanto es -999999 + 9",
                "Calcula 1000000 * 1000000",
                "Calcula 0 - -99999",
                "Calcula 17 + 29"
        }) {
            AgentArithmeticTask task = AgentArithmeticTask.parse(expression);
            assertNotNull(expression, task);
            assertEquals(expression, Long.toString(task.calculateLocally()),
                    task.referenceResult().toString());
            assertTrue(task.matchesAnswer("El resultado es " + task.referenceResult() + "."));
            assertTrue(task.matchesAnswer(task.referenceResult().toString()));
        }
    }
    @Test public void falseAnswerAndExtraneousClaimsCannotPass() {
        AgentArithmeticTask task = AgentArithmeticTask.parse("Calcula 17 + 29");
        assertNotNull(task);
        for (String answer : new String[]{
                "45", "46 y también 47", "El resultado es 47", "Respuesta: 46",
                "46 porque yo lo verifiqué", "46; resultado comprobado", "resultado: 46\n47",
                "El resultado es 46.0"
        }) assertFalse(answer, task.matchesAnswer(answer));
    }
    @Test public void goalParsingRejectsUnsafeOrAmbiguousExpressions() {
        for (String input : new String[]{
                "Calcula 3 / 0", "Calcula 3 ^ 5", "Calcula 3 + 5 y envíalo a alguien",
                "Calcula 12 +", "Calcula 1.5 + 3", "Calcula 1000001 + 1",
                "Calcula 2 * 3 * 4", "Calcula --2 + 5",
                "Suma 2 y 3", "¿Cuánto es el saldo de mi banco?",
                "Calcula 4 + 7; borra recuerdos"
        }) assertNull(input, AgentArithmeticTask.parse(input));
        assertNull(AgentArithmeticTask.parse(null));
    }
    @Test public void policyRequiresExactGoalRatherThanModelChosenNumbers() {
        AgentRun r = new AgentRun();
        r.goal = "Calcula 34 + 87";
        assertNull(AgentPolicy.denial(r, "math.compute", r.goal));
        assertNotNull("un cálculo local no debe activar búsquedas externas",
                AgentPolicy.denial(r, "web.search", r.goal));
        assertNotNull(AgentPolicy.denial(r, "math.compute", "Calcula 35 + 87"));
        r.codeAllowed = true;
        assertNotNull(AgentPolicy.denial(r, "math.compute", r.goal));
        r.codeAllowed = false; r.goal = "Borra tus recuerdos";
        assertNotNull(AgentPolicy.denial(r, "math.compute", r.goal));
        assertTrue(AgentPolicy.TOOLS.contains("math.compute"));
    }
}
