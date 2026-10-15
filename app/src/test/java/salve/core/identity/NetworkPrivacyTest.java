package salve.core.identity;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/** H1 del informe: una sola política antes de enviar nada a internet por iniciativa propia. */
public class NetworkPrivacyTest {
    @Test public void personalQuestionsStayOnThePhone() {
        assertFalse(NetworkPrivacy.safeForNetwork("¿Qué tratamiento recibió mi hermano por su diagnóstico?", null));
        assertFalse(NetworkPrivacy.safeForNetwork("cuánto gana de sueldo mi pareja", null));
        assertFalse(NetworkPrivacy.safeForNetwork("¿Qué siente Bryan cuando trabaja?", null));
        assertFalse(NetworkPrivacy.safeForNetwork("teléfono 612345678 de quién es", null));
        assertFalse(NetworkPrivacy.safeForNetwork("¿Qué opina Lucía de esto?", Arrays.asList("Lucía Pérez")));
    }

    @Test public void worldQuestionsCanBeSearched() {
        assertTrue(NetworkPrivacy.safeForNetwork("¿Por qué los pulpos tienen tres corazones?", null));
        assertTrue(NetworkPrivacy.safeForNetwork("quién ganó el último mundial de fútbol", Arrays.asList("Bryan")));
        assertFalse(NetworkPrivacy.safeForNetwork("", null));
    }
}
