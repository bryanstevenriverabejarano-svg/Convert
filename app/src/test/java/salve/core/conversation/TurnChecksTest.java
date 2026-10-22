package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

public class TurnChecksTest {
    @Test public void saysBryansNameWhenHeAsksWhoHeIs() {
        assertNotNull(TurnChecks.problem("¿Y sabes quién soy yo?", "Claro, eres la persona con la que hablo cada día.", true));
        assertNull(TurnChecks.problem("¿Y sabes quién soy yo?", "Claro: eres Bryan, quien me creó.", true));
        assertNull("un invitado no es Bryan", TurnChecks.problem("¿sabes quién soy?", "No te conozco todavía.", false));
    }

    @Test public void aQuestionAboutHerGetsAnAnswerAboutHer() {
        assertNotNull(TurnChecks.problem("¿Qué intereses tienes?", "El arte, la ciencia y la música son temas fascinantes.", true));
        assertNull(TurnChecks.problem("¿Qué intereses tienes?", "Me encanta dibujar y estoy aprendiendo sobre las estrellas.", true));
        assertNotNull(TurnChecks.problem("Salve, ¿estás bien?", "¡Sí, gracias por preguntar! ¿Y tú?", true));
        assertNull("pedir ayuda no es preguntar por ella", TurnChecks.problem("¿Puedes explicarme la fotosíntesis?", "Las plantas usan la luz.", true));
    }

    @Test public void promisingIsNotDoing() {
        String real = "Es un placer ayudarte a descompasar esos conceptos. Analizaré cada uno por separado para ofrecerte una perspectiva clara.";
        assertTrue(TurnChecks.announcesOnly(real));
        assertNotNull(TurnChecks.problem("descompón libertad y lo de hundido", real, true));
        assertFalse(TurnChecks.announcesOnly("Voy a contarte un cuento: había una vez un zorro que quería volar."));
        assertEquals("a; b", TurnChecks.join("a", "b"));
        assertEquals("b", TurnChecks.join(null, "b"));
        assertNull(TurnChecks.join(null, null));
    }
}
