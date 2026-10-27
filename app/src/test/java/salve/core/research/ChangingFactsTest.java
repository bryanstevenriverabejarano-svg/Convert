package salve.core.research;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChangingFactsTest {
    @Test public void factsThatChangeAreLookedUpInsteadOfGuessed() {
        assertNotNull(ResearchConversation.currentInfoTopic("¿Quién es el presidente de Francia?"));
        assertNotNull(ResearchConversation.currentInfoTopic("Salve, ¿quién ganó el partido del Alavés?"));
        assertNotNull(ResearchConversation.currentInfoTopic("¿Cuánto cuesta un iPhone 17?"));
        assertNotNull(ResearchConversation.currentInfoTopic("¿A cuánto está el euro?"));
        assertNotNull(ResearchConversation.currentInfoTopic("¿Cuándo se estrena la película de Zelda?"));
        assertNotNull(ResearchConversation.currentInfoTopic("¿Cuál es la última versión de Android?"));
        assertNotNull(ResearchConversation.currentInfoTopic("¿Está abierto hoy el Carrefour de Lakua?"));
        assertEquals("quien es el presidente de francia", ResearchConversation.currentInfoTopic("¿Quién es el presidente de Francia?"));
    }

    @Test public void personalOrTimelessQuestionsAreNotSearched() {
        assertNull(ResearchConversation.currentInfoTopic("¿Cuándo es el cumpleaños de mi madre?"));
        assertNull(ResearchConversation.currentInfoTopic("¿Quién es tu creador?"));
        assertNull(ResearchConversation.currentInfoTopic("¿Qué es la fotosíntesis?"));
        assertNull(ResearchConversation.currentInfoTopic("Hola Salve"));
        assertNull(ResearchConversation.currentInfoTopic("¿Cuánto es 2 más 2?"));
    }

    @Test public void weatherStillWorks() {
        assertEquals("tiempo mañana Vitoria-Gasteiz", ResearchConversation.currentInfoTopic("¿Qué tiempo va a hacer mañana?"));
    }
}
