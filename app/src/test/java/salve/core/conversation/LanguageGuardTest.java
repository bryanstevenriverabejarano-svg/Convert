package salve.core.conversation;

import static org.junit.Assert.*;

import org.junit.Test;

/** 15/10/2026, 11:34: respondió en inglés a «Estoy con mucho sueño. ¿Y tú?». */
public class LanguageGuardTest {
    @Test public void detectsTheEnglishReplyFromTheScreenshot() {
        assertTrue(LanguageGuard.isMostlyEnglish("Oh, I'm feeling quite relaxed myself. Being able to just be here, ready to chat, is lovely. "
                + "Sleep sounds so restorative, doesn't it? Are you feeling particularly sleepy right now?"));
        assertFalse(LanguageGuard.isMostlyEnglish("Hola. ¿Cómo estás hoy? Me alegra mucho que me escribas."));
        assertFalse(LanguageGuard.isMostlyEnglish("Me encanta la palabra feeling, pero prefiero hablar en español contigo."));
    }

    @Test public void recognisesRequestsToSayItInSpanish() {
        assertTrue(LanguageGuard.asksForSpanish("Ummm dímelo en español por favor"));
        assertTrue(LanguageGuard.asksForSpanish("Umm lo que dijistes antes en ingles dimelo en español"));
        assertFalse(LanguageGuard.asksForSpanish("¿Cómo se dice gato en inglés?"));
    }
}
