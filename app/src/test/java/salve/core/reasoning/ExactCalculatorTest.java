package salve.core.reasoning;

import static org.junit.Assert.*;

import org.junit.Test;

public class ExactCalculatorTest {
    private static String calc(String text) {
        ExactCalculator.Result r = ExactCalculator.evaluate(text);
        return r == null ? null : r.spanish();
    }

    @Test public void symbolsAndPrecedence() {
        assertEquals("4140", calc("345*12"));
        assertEquals("12", calc("(3 + 4) * 12 / 7"));
        assertEquals("14", calc("2 + 3 * 4"));
        assertEquals("512", calc("2^3^2"));
        assertEquals("-6", calc("-2 * 3"));
        assertEquals("0,3333333333", calc("1/3"));
    }

    @Test public void spanishWordsAndPercentages() {
        assertEquals("397,8", calc("17% de 2.340"));
        assertEquals("397,8", calc("el 17 por ciento de 2340"));
        assertEquals("205", calc("200 más 10% de 50"));
        assertEquals("12", calc("(3 más 4) por 12 entre 7"));
        assertEquals("1024", calc("2 elevado a 10"));
        assertEquals("49", calc("7 al cuadrado"));
        assertEquals("1,4142135624", calc("raíz cuadrada de 2"));
        assertEquals("12", calc("raiz de 144"));
        assertEquals("2,5", calc("5 dividido entre 2"));
        assertEquals("7,5", calc("2,5 x 3"));
    }

    @Test public void unclearOrUnsafeTextIsNotGuessed() {
        assertNull(calc("7"));
        assertNull(calc("la edad de mi abuela más 3"));
        assertNull(calc("5 / 0"));
        assertNull(calc("(2 + 3"));
        assertNull(calc("raíz de -4"));
        assertNull(calc("2 ^ 100000"));
        assertNull(calc(""));
    }
}
