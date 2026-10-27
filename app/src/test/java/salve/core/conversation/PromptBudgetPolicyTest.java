package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

public class PromptBudgetPolicyTest {
    @Test public void casualShortTurnsReadHalfTheContext() {
        assertEquals(PromptBudgetPolicy.LEAN_CHARS, PromptBudgetPolicy.forTurn(10500, false, false, false, false, false, "Hola Salve".length()));
    }

    @Test public void turnsThatNeedContextKeepTheFullBudget() {
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, true, false, false, false, false, 20));   // memoria
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, false, true, false, false, false, 20));   // verificación
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, false, false, true, false, false, 20));   // «¿recuerdas…?»
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, false, false, false, true, false, 20));   // foto
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, false, false, false, false, true, 20));   // cuento
        assertEquals(10500, PromptBudgetPolicy.forTurn(10500, false, false, false, false, false, 400)); // mensaje largo
    }

    @Test public void smallerBudgetsAreNeverRaised() {
        assertEquals(3200, PromptBudgetPolicy.forTurn(3200, false, false, false, false, false, 10));
    }
}
