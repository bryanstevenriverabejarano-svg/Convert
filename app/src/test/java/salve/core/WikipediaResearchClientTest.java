package salve.core;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikipediaResearchClientTest {
    private final WikipediaResearchClient client = new WikipediaResearchClient();

    @Test
    public void extractsAtMostThreeHttpsUrls() {
        List<String> urls = client.extractUrls("Lee https://a.org/uno, https://b.org/dos "
                + "https://c.org/tres y https://d.org/cuatro");
        assertEquals(3, urls.size());
        assertEquals("https://a.org/uno", urls.get(0));
    }

    @Test
    public void removesExecutableMarkupAndLimitsText() {
        String html = "<style>secreto</style><script>alert(1)</script><p>Contenido útil</p>";
        String text = WikipediaResearchClient.extractText(html, "text/html");
        assertEquals("Contenido útil", text);
        assertFalse(text.contains("alert"));

        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 3_000; i++) longText.append('a');
        assertEquals(WikipediaResearchClient.MAX_EXTRACT_CHARS,
                WikipediaResearchClient.extractText(longText.toString(), "text/plain").length());
    }

    @Test
    public void ignoresHttpAndPrivateUrlsInQuestions() {
        List<String> urls = client.extractUrls("http://public.org/no https://127.0.0.1/no https://ok.org/si");
        assertEquals(1, urls.size());
        assertTrue(urls.get(0).contains("ok.org"));
    }

    @Test public void cancellationAndInvalidDestinationAreReportedBeforeNetwork() {
        assertEquals(WikipediaResearchClient.Status.CANCELLED,
                client.researchResult("https://example.org/", () -> true).status);
        assertEquals(WikipediaResearchClient.Status.ERROR,
                client.researchResult("https://127.0.0.1/private", () -> false).status);
        assertEquals(WikipediaResearchClient.Status.ERROR,
                client.researchResult("http://example.org/unsupported", () -> false).status);
    }

    @Test public void unclosedExecutableMarkupIsNotPresentedAsVisibleText() {
        assertEquals("Texto", WikipediaResearchClient.extractText("<p>Texto</p><script>neverRun()", "text/html"));
    }
}
