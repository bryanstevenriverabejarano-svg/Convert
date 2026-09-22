package salve.core.conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.Test;

public class SessionLocationTest {
    private final MutableClock clock = new MutableClock();

    @Test public void usesDeclarationWithSourceAndTimeWithoutPretendingToReadGps() {
        ConversationSession session = session();
        session.addUser("Estoy en Bogotá");
        String context = session.relevantLocationContext("¿Dónde estoy?");
        assertTrue(context.contains("«Bogotá»"));
        assertTrue(context.contains("declaración directa del usuario"));
        assertTrue(context.contains("2026-09-21T12:00:00Z"));
        assertTrue(context.contains("no una lectura GPS"));
        assertTrue(context.contains("ni confirma que el usuario siga allí"));
    }

    @Test public void latestDeclarationReplacesPreviousOne() {
        ConversationSession session = session();
        session.addUser("Mi ubicación es Madrid");
        clock.advance(30_000L);
        session.addUser("No, ahora estoy en Quito.");
        String context = session.relevantLocationContext("¿Qué hora es aquí?");
        assertFalse(context.contains("Madrid"));
        assertTrue(context.contains("«Quito»"));
        assertTrue(context.contains("12:00:30Z"));
    }

    @Test public void survivesTranscriptEvictionButClearAndNewSessionForgetIt() {
        ConversationSession session = new ConversationSession(2, 300, clock);
        session.addUser("Ahora estoy en Buenos Aires");
        session.addAssistant("Entendido");
        session.addUser("Otra pregunta");
        assertFalse(session.asPromptTranscript().contains("Buenos Aires"));
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("Buenos Aires"));
        session.clear();
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("no hay una ubicación actual"));
        assertTrue(session().relevantLocationContext("¿Dónde estoy?").contains("no hay una ubicación actual"));
    }

    @Test public void expiresAtTwoHoursWithoutFurtherUserMessages() {
        ConversationSession session = session();
        session.addUser("Estoy en casa");
        clock.advance(SessionLocation.TTL_MILLIS - 1L);
        assertTrue(session.relevantLocationContext("mi ubicación").contains("«casa»"));
        clock.advance(1L);
        assertTrue(session.relevantLocationContext("mi ubicación").contains("no hay una ubicación actual"));
    }

    @Test public void clockCorrectionBackwardsDoesNotExtendValidity() {
        ConversationSession session = session();
        session.addUser("Estoy en Lima");
        clock.advance(-1L);
        assertFalse(session.relevantLocationContext("¿Dónde estoy?").contains("Lima"));
    }

    @Test public void assistantCannotCreateOrReplaceLocation() {
        ConversationSession session = session();
        session.addUser("Estoy en Bogotá");
        session.addAssistant("Estoy en Madrid");
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("«Bogotá»"));
        assertFalse(session.relevantLocationContext("¿Dónde estoy?").contains("Madrid"));
    }

    @Test public void excludesResidencesStatesQuestionsQuotesAndConditionalClaims() {
        for (String input : new String[]{"Vivo en Madrid", "Soy de Colombia", "Estoy en desacuerdo contigo",
                "Estoy en proceso de aprender", "Estoy en el proceso de aprender", "Estoy en una situación difícil", "Estoy en deuda",
                "Estoy en contra", "¿Estoy en Quito?", "Si estoy en París, dime la hora",
                "Él dice que estoy en Lima", "\"Estoy en Londres\"", "No estoy en Quito",
                "Estoy en Lima y quiero estudiar"}) {
            ConversationSession session = session();
            session.addUser(input);
            assertTrue(input, session.relevantLocationContext("¿Dónde estoy?")
                    .contains("no hay una ubicación actual"));
        }
    }

    @Test public void ignoresUnrelatedQueriesAndDoesNotInferCityTimezone() {
        ConversationSession session = session();
        session.addUser("Mi ubicación actual es San José");
        assertEquals("", session.relevantLocationContext("Ayúdame a programar una suma"));
        String context = session.relevantLocationContext("¿Qué hora es allí?");
        assertTrue(context.contains("«San José»"));
        assertTrue(context.contains("No determina por sí solo la zona horaria"));
        assertFalse(context.contains("America/"));
    }

    @Test public void canReadLeadingLocationSentenceBeforeQuestion() {
        ConversationSession session = session();
        session.addUser("Estoy en Bogotá, Colombia. ¿Qué hora es?");
        assertTrue(session.relevantLocationContext("¿Qué hora es aquí?").contains("«Bogotá, Colombia»"));
        session.addUser("Estoy en Quito, ¿qué hora es allí?");
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("«Quito»"));
    }

    @Test public void explicitDenialInvalidatesOnlyThePreviouslyDeclaredPlace() {
        ConversationSession session = session();
        session.addUser("Estoy en Bogotá");
        session.addUser("No estoy en Madrid");
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("«Bogotá»"));
        session.addUser("Ya no estoy en Bogotá.");
        assertTrue(session.relevantLocationContext("¿Dónde estoy?").contains("no hay una ubicación actual"));
    }

    private ConversationSession session() { return new ConversationSession(16, 8000, clock); }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-09-21T12:00:00Z");
        void advance(long millis) { instant = instant.plusMillis(millis); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant, zone); }
        @Override public Instant instant() { return instant; }
    }
}
