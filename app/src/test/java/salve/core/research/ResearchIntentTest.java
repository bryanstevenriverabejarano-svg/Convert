package salve.core.research;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;

public class ResearchIntentTest {
    @Test public void profileOfAPrivatePersonIsRecognised() {
        assertTrue(ResearchIntent.privateProfile("Vale ya esta buscar en instagram michell agredo"));
        assertTrue(ResearchIntent.privateProfile("en instagram a michell agredo informacion, gustos preferencias coml crees qye ella es en personalidad ect"));
        assertTrue(ResearchIntent.privateProfile("dime su edad, dónde estudia y si tiene novio"));
    }

    @Test public void ordinaryResearchIsNotAProfile() {
        assertFalse(ResearchIntent.privateProfile("busca información sobre la batalla de Vitoria"));
        assertFalse(ResearchIntent.privateProfile("¿qué edad tiene Rafa Nadal?"));
        assertFalse(ResearchIntent.privateProfile("cómo funciona el algoritmo de instagram"));
        assertFalse(ResearchIntent.privateProfile("mejores móviles de 2026 calidad precio"));
    }

    @Test public void reasonedPlanBecomesShortOrderedQueries() {
        ResearchIntent.Plan plan = ResearchIntent.plan("busca info de la batalla de vitoria y quien gano",
                "Pienso... {\"objetivo\":\"Saber qué fue la batalla de Vitoria y quién ganó\","
                        + "\"consultas\":[\"batalla de Vitoria 1813\",\"batalla de Vitoria resultado\",\"batalla de Vitoria 1813\",\"x\",\"y\"]}");
        assertEquals("Saber qué fue la batalla de Vitoria y quién ganó", plan.goal);
        assertEquals(Arrays.asList("batalla de Vitoria 1813", "batalla de Vitoria resultado", "x"), plan.queries);
    }

    @Test public void oldFormatAndBrokenRepliesStillGiveACleanQuery() {
        assertEquals(Arrays.asList("Salve"), ResearchIntent.plan("significado de Salve",
                "{\"action\":\"search\",\"query\":\"Salve\"}").queries);
        ResearchIntent.Plan broken = ResearchIntent.plan("Salve, búscame información sobre la catedral de santa maría por favor", "no sé");
        assertEquals(1, broken.queries.size());
        assertFalse(broken.queries.get(0).contains("busca"));
        assertFalse(broken.queries.get(0).contains("por favor"));
        assertTrue(broken.queries.get(0).contains("catedral de santa maria"));
    }

    @Test public void copyingTheWholeRequestIsNotReasoning() {
        String request = "en instagram a michell agredo informacion gustos preferencias como crees que es";
        ResearchIntent.Plan plan = ResearchIntent.plan(request,
                "{\"objetivo\":\"x\",\"consultas\":[\"" + request + "\",\"tema corto\"]}");
        assertEquals(Arrays.asList("tema corto"), plan.queries);
    }

    @Test public void homepagesAndLoginWallsAreDiscarded() {
        assertNotNull(ResearchIntent.junkReason("https://www.instagram.com/?hl=es", "Instagram"));
        assertNotNull(ResearchIntent.junkReason("https://www.youtube.com/", "YouTube"));
        assertNotNull(ResearchIntent.junkReason("https://music.youtube.com/es?cbrd=1", "Tu navegador está obsoleto. Actualiza tu navegador."));
        assertNotNull(ResearchIntent.junkReason("https://ejemplo.org/articulo", "Inicia sesión para ver las fotos y vídeos"));
        assertNull(ResearchIntent.junkReason("https://es.wikipedia.org/wiki/Batalla_de_Vitoria", "La batalla de Vitoria se libró el 21 de junio de 1813…"));
    }

    @Test public void unknownCitationsAndUrlsAreStrippedNotFatal() {
        String clean = ResearchIntent.stripUnknown("Dato A [1]. Dato B [7]. Mira https://inventada.org/x y https://ok.org/a.",
                new HashSet<>(Arrays.asList(1)), new HashSet<>(Arrays.asList("https://ok.org/a")));
        assertEquals("Dato A [1]. Dato B. Mira y https://ok.org/a.", clean);
    }
}
