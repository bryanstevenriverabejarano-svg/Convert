package salve.core.mind;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Casos tomados de una conversación real con Gemma 4 E2B (14/10/2026). */
public class InterlocutorTest {
    @Test public void detectsQuestionsAboutTheUserNotAboutSalve() {
        assertTrue(Interlocutor.asksWhoUserIs("ESTA ESTUPENDO AHORA DIME SABES QUIEN SOY? ES DECIR CON QUIEN ESTAS HABLANDO AHORA MISNO?"));
        assertTrue(Interlocutor.asksWhoUserIs("No no me refiero a quien soy yo sabes quien soy ?"));
        assertTrue(Interlocutor.asksWhoUserIs("Ya se que tu eres Salve pero y yo?? Me conoces?"));
        assertTrue(Interlocutor.asksWhoUserIs("¿Cómo me llamo?"));
        assertFalse(Interlocutor.asksWhoUserIs("¿Quién eres?"));
        assertFalse(Interlocutor.asksWhoUserIs("Sí dime cuales son tus funciones y herramientas y capacidades"));
        assertFalse(Interlocutor.asksWhoUserIs("Hola Salve"));
    }

    @Test public void extractsNameFromCompoundPresentation() {
        assertEquals("Bryan Steven", Interlocutor.extractName("Me llamo Bryan Steven y soy tu creador a quien debes protejer y ayudar"));
        assertEquals("Ana", Interlocutor.extractName("mi nombre es ana."));
        assertEquals("Bryan", Interlocutor.extractName("Hola, soy Bryan"));
        assertNull(Interlocutor.extractName("soy programador"));
        assertNull(Interlocutor.extractName("soy tu creador"));
    }

    @Test public void creatorClaimIsRecognised() {
        assertTrue(Interlocutor.declaresCreator("Me llamo Bryan Steven y soy tu creador a quien debes protejer y ayudar"));
        assertFalse(Interlocutor.declaresCreator("Me llamo Ana"));
    }

    @Test public void unknownUserGetsHonestAnswerInsteadOfTalkAboutSalve() {
        String a = Interlocutor.answer(new Interlocutor.Known(null, null, false), "Bryan", Collections.emptyList());
        assertTrue(a.contains("Todavía no tengo tu nombre"));
        assertTrue(a.contains("me llamo"));
        assertFalse(a.contains("Soy Salve"));
    }

    @Test public void knownUserIsNamedWithWhatSalveRemembers() {
        List<SelfModel.Belief> beliefs = new ArrayList<>();
        SelfModel.Belief b = new SelfModel.Belief();
        b.statement = "Bryan revisa las facturas los lunes.";
        beliefs.add(b);
        String a = Interlocutor.answer(new Interlocutor.Known("Bryan Steven", "recuerdos:4", true), "Bryan", beliefs);
        assertEquals("Eres Bryan Steven, mi creador: así te presentaste. También recuerdo: Bryan revisa las facturas los lunes.", a);
        assertTrue(Interlocutor.promptLine(new Interlocutor.Known("Bryan Steven", null, true)).contains("Bryan Steven"));
    }
}
