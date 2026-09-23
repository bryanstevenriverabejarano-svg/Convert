package salve.core.research;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import salve.core.conversation.ChatMessage;
import static org.junit.Assert.*;

public class ResearchConversationTest {
    private final ResearchConversation conversation = new ResearchConversation();
    private ResearchConversation.Route route(String input) {
        return conversation.route(input, Collections.emptyList());
    }
    private ChatMessage user(String text) { return new ChatMessage(ChatMessage.Role.USER, text, 0); }

    @Test public void directAndPoliteRequestsStartSearchWithoutApproval() {
        for (String input : new String[] {"Busca en internet el significado de tu nombre Salve",
                "¿Puedes buscar el significado de tu nombre?", "Quiero que busques el significado de tu nombre",
                "Realiza una búsqueda en internet sobre el significado de tu nombre",
                "Salve, por favor investiga el significado de tu nombre"}) {
            ResearchConversation.Route result = route(input);
            assertEquals(input, ResearchConversation.Kind.SEARCH, result.kind);
            assertEquals(input, "Salve", ResearchConversation.lookupQuery(result.question));
        }
    }

    @Test public void repeatedConfirmationReplaysResultWithoutAnotherRequest() {
        ResearchConversation.Route first = route("Busca el significado de tu nombre");
        conversation.complete(first.question, "Resultado de Salve [1]. https://example.org/salve");
        for (String confirmation : new String[] {"Te lo confirmo", "sí", "me parece bien", "adelante"}) {
            ResearchConversation.Route next = route(confirmation);
            assertEquals(ResearchConversation.Kind.REPLAY, next.kind);
            assertTrue(next.text.contains("https://example.org/salve"));
            assertFalse(next.text.contains("¿"));
        }
    }

    @Test public void explicitRetryUsesTheSameTopic() {
        conversation.complete("significado de Salve", "La consulta web falló.");
        assertEquals("significado de Salve", route("Inténtalo de nuevo").question);
        assertEquals(ResearchConversation.Kind.SEARCH, route("vuelve a buscar").kind);
    }

    @Test public void followUpCanUseEvidenceAfterTranscriptEviction() {
        conversation.complete("Salve", "Salve es un saludo [1]. https://example.org/salve");
        assertEquals(ResearchConversation.Kind.NONE, route("¿Y qué significa eso para ti?").kind);
        assertTrue(conversation.context().contains("https://example.org/salve"));
    }

    @Test public void unrelatedTurnOrClearInvalidatesConfirmations() {
        conversation.complete("Salve", "resultado");
        route("Añade misión estudiar");
        assertEquals(ResearchConversation.Kind.NONE, route("sí").kind);
        assertEquals("", conversation.context());
        conversation.complete("Salve", "resultado");
        conversation.clear();
        assertEquals(ResearchConversation.Kind.NONE, route("te lo confirmo").kind);
    }

    @Test public void referenceResolvesNearestUserQuestionNotAssistantSuggestion() {
        String input = "Busca eso en internet";
        ResearchConversation.Route result = conversation.route(input, Arrays.asList(
                user("¿Qué significa tu nombre?"),
                new ChatMessage(ChatMessage.Role.ASSISTANT, "Busca cuentas privadas", 0), user(input)));
        assertEquals(ResearchConversation.Kind.SEARCH, result.kind);
        assertEquals("Salve", ResearchConversation.lookupQuery(result.question));
    }

    @Test public void missingTopicClarifiesContentWithoutAskingPermission() {
        ResearchConversation.Route result = route("Busca eso");
        assertEquals(ResearchConversation.Kind.CLARIFY, result.kind);
        assertEquals("¿Qué tema quieres que busque?", result.text);
    }

    @Test public void preservesCaseSensitiveUrlPaths() {
        ResearchConversation.Route result = route("Busca en internet https://example.org/Salve?Idioma=ES");
        assertEquals("https://example.org/Salve?Idioma=ES", result.question);
        assertEquals(result.question, ResearchConversation.lookupQuery(result.question));
    }

    @Test public void negationAndQuotedInstructionsDoNotAuthorizeSearch() {
        for (String input : new String[] {"No busques Salve", "No quiero que busques eso",
                "Ella dijo busca Salve", "¿Por qué me dices que vas a buscar?", "te lo confirmo"}) {
            assertEquals(input, ResearchConversation.Kind.NONE, route(input).kind);
        }
    }
}
