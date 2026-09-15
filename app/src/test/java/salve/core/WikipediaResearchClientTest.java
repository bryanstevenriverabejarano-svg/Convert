package salve.core;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class WikipediaResearchClientTest {
    @Test
    public void groundedContextPreservesTitlesUrlsAndCitationNumbers() {
        WikipediaResearchClient.ResearchResult result =
                new WikipediaResearchClient.ResearchResult("energia solar", Arrays.asList(
                        new WikipediaResearchClient.Source(
                                "Energía solar", "https://es.wikipedia.org/?curid=1", "Extracto uno"),
                        new WikipediaResearchClient.Source(
                                "Panel solar", "https://es.wikipedia.org/?curid=2", "Extracto dos")
                ));

        String context = result.asGroundedContext();
        assertTrue(context.contains("[1] Energía solar"));
        assertTrue(context.contains("[2] Panel solar"));
        assertTrue(context.contains("https://es.wikipedia.org/?curid=1"));
    }
}
