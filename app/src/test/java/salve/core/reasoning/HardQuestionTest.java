package salve.core.reasoning;

import static org.junit.Assert.*;

import org.junit.Test;

public class HardQuestionTest {
    @Test public void thinksOnPuzzlesPlansAndWordProblems() {
        assertTrue(HardQuestion.needsThinking("Resuelve este acertijo: tres cajas mal etiquetadas…"));
        assertTrue(HardQuestion.needsThinking("Razona paso a paso por qué el cielo es azul"));
        assertTrue(HardQuestion.needsThinking("¿Qué me conviene más, alquilar o comprar piso en Vitoria?"));
        assertTrue(HardQuestion.needsThinking("Si un tren sale a las 9 y va a 120 km/h, ¿cuánto tarda en recorrer 300 km?"));
        assertTrue(HardQuestion.needsThinking("Hazme un plan para preparar el examen en dos semanas"));
    }

    @Test public void answersFastOtherwise() {
        assertFalse(HardQuestion.needsThinking("Hola Salve"));
        assertFalse(HardQuestion.needsThinking("¿Qué tal estás hoy?"));
        assertFalse(HardQuestion.needsThinking("¿Cuánto es 17% de 2340?"));
        assertFalse(HardQuestion.needsThinking("Tengo 3 hermanos y 2 perros"));
        assertFalse(HardQuestion.needsThinking(null));
    }
}
