package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Su yo (narrativa, creencias, pensamientos) debe llegar al prompt aunque crezca la lista de lo
 * que sabe hacer. Cuando la lista se lo comió, respondía «Soy Salve. Soy Salve.».
 */
public class MindRuntimeSelfModelTest {
    private static final String HEAD = "INTERLOCUTOR: Bryan Steven Rivera Bejarano, tu creador (es su teléfono). "
            + "Háblale de tú, en masculino.\nFamilia que me has presentado: tu madre Vivina, tu hermana Ana, tu padre Luis.\n";

    @Test public void selfKeepsGuaranteedRoomInTheConversationPrompt() {
        int[] asked = new int[1];
        String out = MindRuntime.composeSelfModel(HEAD, budget -> {
            asked[0] = budget;
            return "YO_PERSISTENTE v9\nNarrativa: Me formo mi identidad a partir de mi interacción con Bryan.";
        }, 1700);
        assertTrue("su yo recibió solo " + asked[0], asked[0] >= MindRuntime.MIN_SELF_CHARS);
        assertTrue(out.contains("Me formo mi identidad"));
        assertTrue(out.contains("Capacidades reales"));
        assertTrue(out.length() <= 1700);
    }

    @Test public void fullCapabilitiesOnlyWhenTheyFitBesideHerSelf() {
        String roomy = MindRuntime.composeSelfModel(HEAD, b -> "yo", 4000);
        assertTrue(roomy.contains(MindRuntime.CAPABILITIES));
        String tight = MindRuntime.composeSelfModel(HEAD, b -> "yo", 1700);
        assertTrue(tight.contains(MindRuntime.CAPABILITIES_SHORT));
        assertFalse(tight.contains(MindRuntime.CAPABILITIES));
    }

    @Test public void shortCapabilitiesStayShort() {
        assertTrue(MindRuntime.CAPABILITIES_SHORT.length() < 450);
    }
}
