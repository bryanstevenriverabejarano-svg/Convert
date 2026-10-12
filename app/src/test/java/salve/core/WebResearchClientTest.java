package salve.core;

import java.util.*;
import okhttp3.*;
import org.junit.Test;
import salve.core.web.SearchEngine;
import static org.junit.Assert.*;

public class WebResearchClientTest {
    private static Response response(Request request, int code, String type, String body) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("fixture")
                .header("Content-Type", type).body(ResponseBody.create(body, MediaType.get(type))).build();
    }
    @Test public void automaticSearchRecoversFromGoogleRateLimitThenReadsBingResult() {
        List<String> calls = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            String host = chain.request().url().host(); calls.add(host);
            if (host.equals("www.google.com")) return response(chain.request(),429,"text/html","rate limited");
            if (host.equals("www.bing.com")) return response(chain.request(),200,"text/xml",
                    "<rss><channel><item><link>https://example.org/colada</link></item></channel></rss>");
            return response(chain.request(),200,"text/html","<nav>Menú sin evidencia</nav><article>La colada es el lavado de la ropa.</article>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport, new SearchEngine("auto", ""))
                .researchResult("colada", () -> false);
        assertEquals(WikipediaResearchClient.Status.COMPLETE,result.status);
        assertEquals(Arrays.asList("www.google.com","www.bing.com","example.org"), calls);
        assertEquals("La colada es el lavado de la ropa.", result.pages.get(0).text);
        assertEquals("https://example.org/colada", result.pages.get(0).url);
        assertTrue(result.diagnostic.contains("429")); assertTrue(result.diagnostic.contains("bing"));
    }
    @Test public void explicitGoogleRequestDoesNotSilentlySwitchProviderOrTreatCaptchaAsEvidence() {
        List<String> calls = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.add(chain.request().url().host());
            return response(chain.request(),200,"text/html","<div class='g-recaptcha'>verifica</div><a href='https://example.org/'>fake</a>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport,new SearchEngine("auto",""))
                .researchResult("en google colada", () -> false);
        assertEquals(Collections.singletonList("www.google.com"), calls);
        assertTrue(result.pages.isEmpty()); assertEquals(WikipediaResearchClient.Status.ERROR,result.status);
        assertTrue(result.diagnostic.contains("intervención"));
    }
    @Test public void wikipediaJsonIsParsedBeforeAnyExtractClipping() {
        String json = "{\"batchcomplete\":true,\"query\":{\"pages\":[{\"index\":1,\"title\":\"Colada\",\"extract\":\""
                + String.join("",Collections.nCopies(3000,"a")) + "\",\"fullurl\":\"https://es.wikipedia.org/wiki/Colada\"},"
                + "{\"index\":2,\"title\":\"Piña\",\"extract\":\"Bebida\",\"fullurl\":\"https://es.wikipedia.org/wiki/Pi\\u00f1a_colada\"}]}}";
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> response(chain.request(),200,"application/json",json)).build();
        WikipediaResearchClient.ResearchBatch result = new WikipediaResearchClient(transport).researchResult("colada", () -> false);
        assertEquals(WikipediaResearchClient.Status.COMPLETE,result.status); assertEquals(2,result.pages.size());
        assertEquals(2500,result.pages.get(0).text.length()); assertTrue(result.pages.get(1).url.contains("Piña"));
    }
    @Test public void serviceErrorIsNotReportedAsNoResultsAndCancellationDoesNotSendRequest() {
        int[] calls = {0};
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls[0]++; return response(chain.request(),503,"text/html","unavailable");
        }).build();
        WikipediaResearchClient reader = new WikipediaResearchClient(transport);
        assertEquals(WikipediaResearchClient.Status.CANCELLED,reader.researchResult("colada",()->true).status);
        assertEquals(0,calls[0]);
        WikipediaResearchClient.ResearchBatch result = reader.researchResult("colada",()->false);
        assertEquals(WikipediaResearchClient.Status.ERROR,result.status); assertTrue(result.diagnostic.contains("503"));
    }
}
