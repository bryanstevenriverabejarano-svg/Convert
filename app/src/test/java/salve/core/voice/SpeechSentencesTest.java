package salve.core.voice;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class SpeechSentencesTest {
    @Test public void splitsSentencesAndKeepsTheirPlaceInTheText() {
        String text = "Hola, Bryan. Mi memoria está bien: tengo 1.240 recuerdos y ningún duplicado. ¿Quieres que la revise?";
        List<SpeechSentences.Piece> pieces = SpeechSentences.split(text);
        assertEquals(2, pieces.size());
        assertEquals("Hola, Bryan. Mi memoria está bien: tengo 1.240 recuerdos y ningún duplicado.", pieces.get(0).text);
        assertEquals("¿Quieres que la revise?", pieces.get(1).text);
        for (SpeechSentences.Piece p : pieces) assertEquals(p.text, text.substring(p.start, p.end));
    }

    @Test public void shortGreetingJoinsTheNextSentenceButNotAcrossParagraphs() {
        List<SpeechSentences.Piece> pieces = SpeechSentences.split("¡Hola! Hoy hace un día precioso en Vitoria-Gasteiz.");
        assertEquals(1, pieces.size());
        pieces = SpeechSentences.split("Título\n\nÉrase una vez una niña que dibujaba con epiciclos.");
        assertEquals("Título", pieces.get(0).text);
    }

    @Test public void longSentenceIsCutAtACommaSoTheFirstPartSoundsSooner() {
        StringBuilder text = new StringBuilder("Había una vez un pueblo pequeño");
        for (int i = 0; i < 12; i++) text.append(", con casas de piedra y tejados rojos");
        text.append('.');
        List<SpeechSentences.Piece> pieces = SpeechSentences.split(text.toString());
        assertTrue(pieces.size() > 1);
        for (SpeechSentences.Piece p : pieces) {
            assertTrue(p.text.length() <= SpeechSentences.MAX_CHARS);
            assertFalse(p.text.startsWith(","));
        }
        assertTrue(pieces.get(0).text.endsWith(","));
    }

    @Test public void numbersAndAbbreviationsDoNotEndASentence() {
        assertEquals(1, SpeechSentences.split("Uso Gemma 4 con versión 3.5 del motor y va bien hoy.").size());
        assertEquals(1, SpeechSentences.split("Te lo dijo el Sr. pérez ayer por la tarde en casa.").size());
    }

    @Test public void nothingToSayGivesNoPieces() {
        assertTrue(SpeechSentences.split(null).isEmpty());
        assertTrue(SpeechSentences.split("   \n ").isEmpty());
        assertEquals("Vale.", SpeechSentences.split("Vale.").get(0).text);
    }
}
