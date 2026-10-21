package salve.core.mind;

import static org.junit.Assert.*;
import org.junit.Test;

public class SelfFunctionsTest {
    @Test public void offeredWhenBryanAsksInHisOwnWords() {
        assertTrue(SelfFunctions.offered("Que tal tu memoria?"));
        assertTrue(SelfFunctions.offered("ponte a prueba a ver qué nota sacas"));
        assertTrue(SelfFunctions.offered("enséñame cómo piensas"));
        assertTrue(SelfFunctions.offered("qué crees que deberías mejorar"));
    }

    @Test public void notWhenHeAsksForAMemoryOrTalksOfSomethingElse() {
        assertFalse(SelfFunctions.offered("busca en tu memoria lo de navegar"));
        assertFalse(SelfFunctions.offered("¿Recuerdas que hablamos de navegar?"));
        assertFalse(SelfFunctions.offered("Dibuja un gorila"));
        assertFalse(SelfFunctions.offered("hola, ¿cómo estás?"));
    }

    @Test public void actionsAreNormalized() {
        assertEquals("estado_memoria", SelfFunctions.action("estado_memoria"));
        assertEquals("estado_memoria", SelfFunctions.action("estado de la memoria"));
        assertEquals("examen", SelfFunctions.action("Examen"));
        assertEquals("mapa_mente", SelfFunctions.action("mapa mente"));
        assertEquals("propuestas_mejora", SelfFunctions.action("propuestas de mejora"));
        assertNull(SelfFunctions.action("borrar todo"));
        assertTrue("cabe en el catálogo", SelfFunctions.description().length() < 200);
    }
}
