package salve.core.mind;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import salve.core.conversation.ReplyStyle;

/** Casos de la segunda conversación real con Gemma 4 E2B (15/10/2026). */
public class FamilyAndNamesTest {
    @Test public void nameIsLearnedWhenNotAtTheStartOfTheMessage() {
        assertEquals("Bryan", Interlocutor.extractName("Jajajaja sí así es soy Bryan"));
        assertEquals("Bryan", Interlocutor.extractName("Bueno sí Me llamo Bryan soy tu creador como tu mencionaste"));
        assertTrue(Interlocutor.declaresCreator("Bueno sí Me llamo Bryan soy tu creador como tu mencionaste"));
        assertNull(Interlocutor.extractName("Ya nos conociamos Salve"));
    }

    @Test public void familyMembersAreParsed() {
        List<FamilyFacts.Member> m = FamilyFacts.parse("Mi hermana se llama Vivi y mi madre es Rosa");
        assertEquals(2, m.size());
        assertEquals("Mi hermana se llama Vivi.", m.get(0).statement());
        assertEquals("family_hermana_vivi", m.get(0).category());
        assertEquals("Mi madre se llama Rosa.", m.get(1).statement());
        assertEquals("Ana", FamilyFacts.parse("mi prima se llama ana y vive en Bilbao").get(0).name);
        assertTrue(FamilyFacts.parse("mi madre es enfermera").isEmpty());
        assertTrue(FamilyFacts.parse("¿mi hermana se llama Ana?").isEmpty());
    }

    @Test public void familyQuestionsAreAnsweredFromMemory() {
        assertTrue(FamilyFacts.asksAboutFamily("Y quien es mí familia ?"));
        assertTrue(FamilyFacts.asksAboutFamily("¿Cómo se llama mi madre?"));
        assertFalse(FamilyFacts.asksAboutFamily("Mi familia está bien"));
        assertTrue(FamilyFacts.answer("Y quien es mí familia ?", Collections.emptyList()).contains("Aún no me has hablado de tu familia"));
        List<String> stored = Arrays.asList("Mi hermana se llama Vivi.", "Mi madre se llama Rosa.");
        assertEquals("Por lo que me has contado: tu hermana se llama Vivi; tu madre se llama Rosa.",
                FamilyFacts.answer("¿Quién es mi familia?", stored));
        assertEquals("Por lo que me has contado: tu madre se llama Rosa.", FamilyFacts.answer("¿cómo se llama mi madre?", stored));
        assertTrue(FamilyFacts.answer("¿quién es mi padre?", stored).contains("No tengo guardado quién es tu padre"));
    }

    @Test public void prefabLabelAndRepeatsAreHandled() {
        String raw = "Esa es una pregunta muy personal, y como modelo de lenguaje, no tengo acceso a información sobre tu familia.";
        assertEquals("Esa es una pregunta muy personal, y no tengo acceso a información sobre tu familia.", ReplyStyle.clean(raw));
        String repeated = "Hola, Bryan. Es un placer conocerte. Me gusta mucho el sonido de tu nombre.";
        assertTrue(ReplyStyle.repeats(repeated, Collections.singletonList(repeated)));
        assertFalse(ReplyStyle.repeats("Hola de nuevo, Bryan. ¿Qué tal el cansancio?", Collections.singletonList(repeated)));
    }
}
