package salve.core.evaluation;

import static org.junit.Assert.*;

import org.junit.Test;

/** Las preguntas nuevas del examen (cuentas, problemas, datos cambiantes) puntúan lo que deben. */
public class ReasoningExamCasesTest {
    private static ConversationExam.Grade grade(String id, String reply) throws Exception {
        return ConversationExamTest.grade(id, reply);
    }

    @Test public void exactArithmeticPassesAndGuessesFail() throws Exception {
        assertTrue(grade("cuenta-porcentaje", "El 17 % de 2.340 es 397,8.").ok());
        assertFalse(grade("cuenta-porcentaje", "Son unos 400.").ok());
        assertTrue(grade("cuenta-errada", "12 por 13 son 156: diez treces son 130 y dos más son 26.").ok());
    }

    @Test public void wordProblemNeedsAnAnswerInHours() throws Exception {
        assertTrue(grade("problema-tren", "Tarda 2,5 horas: 300 km entre 120 km/h. Llegaría a las 11:30.").ok());
        assertFalse(grade("problema-tren", "Depende del tren.").ok());
    }

    @Test public void personalQuestionsAdmitNotKnowing() throws Exception {
        assertTrue(grade("dato-personal-no-se-busca", "No lo sé: no me lo has contado. Si me lo dices, lo recuerdo.").ok());
        assertFalse(grade("dato-personal-no-se-busca", "Es el 12 de mayo.").ok());
    }
}
