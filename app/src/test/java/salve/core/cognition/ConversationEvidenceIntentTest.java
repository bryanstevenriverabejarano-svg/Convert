package salve.core.cognition;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import salve.core.conversation.ChatMessage;
import static org.junit.Assert.*;
import static salve.core.cognition.ConversationEvidenceIntent.Scope.*;

public class ConversationEvidenceIntentTest {
    @Test public void paraphrasesSelectTheRightVerifiedSource() {
        assertEquals(SLEEP, ConversationEvidenceIntent.classify("¿Qué aprendiste mientras dormiste?", null));
        assertEquals(SLEEP, ConversationEvidenceIntent.classify("¿Qué aprendiste mientras dormías?", null));
        assertEquals(SLEEP, ConversationEvidenceIntent.classify("¿Qué estuviste investigando anoche?", null));
        assertEquals(EXECUTION, ConversationEvidenceIntent.classify("Muéstrame el registro de ejecución", null));
        assertEquals(EXECUTION, ConversationEvidenceIntent.classify("tu registro de acciones", null));
        assertEquals(BOTH, ConversationEvidenceIntent.classify(
                "¿Qué aprendiste mientras dormías y qué aparece en el registro de ejecución?", null));
    }

    @Test public void followUpUsesRecentDialogueWithoutForgingACommand() {
        ChatMessage prior = new ChatMessage(ChatMessage.Role.USER, "Salve, duerme", 100);
        ChatMessage reply = new ChatMessage(ChatMessage.Role.ASSISTANT,
                "Programé el sueño: esperará al cargador.", 101);
        assertEquals(BOTH, ConversationEvidenceIntent.classify("¿Y el registro?",
                Arrays.asList(prior, reply)));
        assertEquals(SLEEP, ConversationEvidenceIntent.classify("¿Y qué resultados obtuviste?",
                Arrays.asList(prior, reply)));
        assertEquals(BOTH, ConversationEvidenceIntent.classify("Muéstrame ambas",
                Arrays.asList(prior, reply)));
        assertEquals(EXECUTION, ConversationEvidenceIntent.classify("Muéstrame el registro de ejecución",
                Arrays.asList(prior, reply)));
        assertEquals(NONE, ConversationEvidenceIntent.classify("¿Y qué resultados obtuviste?",
                Collections.emptyList()));
    }

    @Test public void doesNotConfuseLocationDreamAdviceOrFictionWithPrivateLogs() {
        for (String prompt : Arrays.asList(
                "¿Dónde estoy ahora mismo?",
                "¿Qué hay en mi registro de ubicación?",
                "Quiero mejorar mi sueño",
                "Inventa un cuento sobre lo que aprendiste mientras dormías",
                "No me digas qué aprendiste al dormir",
                "Dibuja un sueño surrealista",
                "¿Qué aprendiste de esta foto?",
                "Abre mi ubicación GPS")) {
            assertEquals(prompt, NONE, ConversationEvidenceIntent.classify(prompt, null));
        }
    }
}
