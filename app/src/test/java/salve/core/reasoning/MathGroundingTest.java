package salve.core.reasoning;

import static org.junit.Assert.*;

import org.junit.Test;

public class MathGroundingTest {
    @Test public void onlyExplicitCalculationsAreGrounded() {
        assertEquals("397,8", MathGrounding.requested("¿Cuánto es el 17% de 2.340?").spanish());
        assertEquals("4140", MathGrounding.requested("calcula 345 por 12").spanish());
        assertEquals("4140", MathGrounding.requested("345*12").spanish());
        assertNull(MathGrounding.requested("¿cuánto es la distancia a la luna?"));
        assertNull(MathGrounding.requested("hola, ¿qué tal?"));
        assertNull(MathGrounding.requested("llama a mamá"));
        assertEquals("", MathGrounding.promptBlock("¿cómo estás?"));
        assertTrue(MathGrounding.promptBlock("cuánto es 2 elevado a 10").contains("= 1024"));
    }

    @Test public void draftWithoutTheExactResultIsCorrected() {
        String input = "¿cuánto es 17% de 2340?";
        assertNull(MathGrounding.problem(input, "El 17 % de 2.340 es 397,8."));
        assertNull(MathGrounding.problem(input, "Son 397,80 euros."));
        assertNotNull(MathGrounding.problem(input, "Aproximadamente 400."));
        assertNotNull(MathGrounding.problem(input, "Es 397."));
    }

    @Test public void falseEquationsInAnyDraftAreCaught() {
        assertNotNull(MathGrounding.problem("explícame la multiplicación", "Por ejemplo, 12 x 13 = 146."));
        assertNull(MathGrounding.problem("explícame la multiplicación", "Por ejemplo, 12 x 13 = 156."));
        assertNull(MathGrounding.problem("explícame la división", "1 / 3 = 0,33 aproximadamente."));
        assertNull(MathGrounding.problem("hola", "Hoy es 27-10-2026."));
    }

    @Test public void lastResortAppendsTheExactValue() {
        assertEquals("Es 397,8.", MathGrounding.ensureResult("¿cuánto es 17% de 2340?", "Es 397,8."));
        assertTrue(MathGrounding.ensureResult("¿cuánto es 17% de 2340?", "Unos 400.").endsWith("= 397,8."));
        assertEquals("Hola.", MathGrounding.ensureResult("hola", "Hola."));
    }
}
