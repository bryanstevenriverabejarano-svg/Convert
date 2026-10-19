package salve.core.mind;

import static org.junit.Assert.*;
import static salve.core.mind.SelfFeatures.Feature.*;

import org.junit.Test;

public class SelfFeaturesTest {
    @Test public void recognisesQuestionsAboutHerOwnFunctions() {
        assertEquals(NODES, SelfFeatures.detect("Nodos"));
        assertEquals(NODES, SelfFeatures.detect("Me pregunto por tus nodos"));
        assertEquals(NODES, SelfFeatures.detect("¿Qué es la constelación?"));
        assertEquals(WEB_SEARCH, SelfFeatures.detect("¿Cómo buscas en internet?"));
        assertEquals(PHONE_ACTIONS, SelfFeatures.detect("¿Puedes llamar a alguien?"));
        assertEquals(PRIVACY_CHECK, SelfFeatures.detect("¿Cómo sabes que soy yo?"));
        assertEquals(OVERVIEW, SelfFeatures.detect("¿Qué puedes hacer?"));
        assertNull(SelfFeatures.detect("Hola, ¿qué tal?"));
        assertNull(SelfFeatures.detect(null));
    }

    @Test public void onlyAClearYesAcceptsAnOffer() {
        for (String yes : new String[]{"sí", "Sí, ábrela", "vale", "claro", "muéstramelos", "Salve, sí"})
            assertTrue(yes, SelfFeatures.accepts(yes));
        for (String other : new String[]{"sí, pero antes dime otra cosa", "¿qué es?", "nodos", "no"})
            assertFalse(other, SelfFeatures.accepts(other));
        assertTrue(SelfFeatures.declines("ahora no"));
    }

    @Test public void offerIsAppendedOnceAndOnlyForScreensSheHas() {
        String offer = SelfFeatures.offer(NODES, false);
        assertTrue(SelfFeatures.withOffer("Tengo dos nodos.", offer).endsWith(offer));
        String already = "Tengo dos nodos. " + offer;
        assertEquals(already, SelfFeatures.withOffer(already, offer));
        assertNull(SelfFeatures.offer(WEB_SEARCH, true));
        assertNull("sin biometría no hay verificación que ofrecer", SelfFeatures.offer(PRIVACY_CHECK, false));
    }

    @Test public void promptCarriesFactsAndLiveState() {
        String block = SelfFeatures.promptBlock(NODES, "2 nodos registrados", "¿…?");
        assertTrue(block.contains("constelación"));
        assertTrue(block.contains("2 nodos registrados"));
    }
}
