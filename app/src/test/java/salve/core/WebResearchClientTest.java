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

    @Test public void streamingReadsAndAnalysisInterleaveAndSkipPortalLinks() {
        List<String> order = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            String host = chain.request().url().host(); String path = chain.request().url().encodedPath();
            order.add("fetch:" + host + path);
            if (host.equals("www.google.com")) return response(chain.request(),200,"text/html",
                    "<a href='https://www.instagram.com/?hl=es'>Home</a>"
                    + "<a href='https://example.org/a'>A</a><a href='https://example.net/b'>B</a>");
            return response(chain.request(),200,"text/html","<article>Fuente pública sobre el tema.</article>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport,new SearchEngine("google",""))
                .researchResult("tema",()->false,page -> order.add("analyze:" + page.url));
        assertEquals(Arrays.asList("fetch:www.google.com/search", "fetch:example.org/a", "analyze:https://example.org/a",
                "fetch:example.net/b", "analyze:https://example.net/b"),order);
        assertEquals(2,result.pages.size());
    }
    @Test public void cancellingDuringPageAnalysisDoesNotFetchNextLink() {
        java.util.concurrent.atomic.AtomicBoolean cancel = new java.util.concurrent.atomic.AtomicBoolean();
        List<String> calls = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.add(chain.request().url().host());
            return response(chain.request(),200,"text/html",chain.request().url().host().equals("www.google.com")
                    ? "<a href='https://example.org/a'>A</a><a href='https://example.net/b'>B</a>"
                    : "<article>Fuente pública sobre el tema.</article>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport,new SearchEngine("google",""))
                .researchResult("tema",cancel::get,page -> cancel.set(true));
        assertEquals(WikipediaResearchClient.Status.CANCELLED,result.status);
        assertEquals(Arrays.asList("www.google.com","example.org"),calls);
    }
    @Test public void redirectedLoginPageCannotBecomeResearchEvidence() {
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            String host = chain.request().url().host();
            if (host.equals("www.google.com")) return response(chain.request(),200,"text/html","<a href='https://example.org/profile'>Profile</a>");
            return response(chain.request(),200,"text/html","<title>Acceso</title><main>Inicia sesión para continuar</main>");
        }).build();
        assertTrue(new WebResearchClient(transport,new SearchEngine("google",""))
                .researchResult("perfil",()->false).pages.isEmpty());
    }

    private static final String KEY = "BSAtestkey_0123456789abcdef";
    private static final String BRAVE_JSON = "{\"type\":\"search\",\"web\":{\"results\":["
            + "{\"title\":\"Colada\",\"url\":\"https://example.org/colada\",\"description\":\"x\"},"
            + "{\"title\":\"Local\",\"url\":\"https://192.168.1.1/admin\"}]}}";
    @Test public void automaticSearchUsesOwnersBraveKeyFirstWithoutScrapingGoogle() {
        List<String> calls = new ArrayList<>(); String[] header = {null};
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            String host = chain.request().url().host(); calls.add(host);
            if (host.equals("api.search.brave.com")) {
                header[0] = chain.request().header("X-Subscription-Token");
                return response(chain.request(),200,"application/json",BRAVE_JSON);
            }
            return response(chain.request(),200,"text/html","<article>La colada es el lavado de la ropa.</article>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport, new SearchEngine("auto", ""), KEY)
                .researchResult("colada", () -> false);
        assertEquals(WikipediaResearchClient.Status.COMPLETE, result.status);
        assertEquals(Arrays.asList("api.search.brave.com","example.org"), calls);
        assertEquals(KEY, header[0]);
        assertTrue(result.diagnostic.contains("brave"));
        assertFalse(result.diagnostic.contains(KEY));
    }
    @Test public void explicitBraveWithoutKeyExplainsWhatIsMissingAndSendsNothing() {
        int[] calls = {0};
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls[0]++; return response(chain.request(),200,"application/json",BRAVE_JSON);
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport, new SearchEngine("auto", ""))
                .researchResult("busca en brave colada", () -> false);
        assertEquals(0, calls[0]);
        assertEquals(WikipediaResearchClient.Status.ERROR, result.status);
        assertTrue(result.diagnostic.contains("falta tu clave"));
    }
    @Test public void rejectedKeyFallsBackInAutoModeAndIsNeverEchoed() {
        List<String> calls = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            String host = chain.request().url().host(); calls.add(host);
            if (host.equals("api.search.brave.com")) return response(chain.request(),401,"application/json","{\"error\":\"" + KEY + "\"}");
            if (host.equals("www.google.com")) return response(chain.request(),200,"text/html","<a href='/url?q=https%3A%2F%2Fexample.org%2Fa'>a</a>");
            return response(chain.request(),200,"text/html","<article>Texto público.</article>");
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport, new SearchEngine("auto", ""), KEY)
                .researchResult("colada", () -> false);
        assertEquals(Arrays.asList("api.search.brave.com","www.google.com","example.org"), calls);
        assertEquals(WikipediaResearchClient.Status.COMPLETE, result.status);
        assertTrue(result.diagnostic.contains("clave fue rechazada"));
        assertFalse(result.diagnostic.contains(KEY));
    }
    @Test public void credentialIsNotForwardedThroughRedirects() {
        List<String> calls = new ArrayList<>();
        OkHttpClient transport = new OkHttpClient.Builder().addInterceptor(chain -> {
            calls.add(chain.request().url().host());
            return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(302).message("moved")
                    .header("Location", "https://evil.example.org/steal").body(ResponseBody.create("", MediaType.get("text/plain"))).build();
        }).build();
        WikipediaResearchClient.ResearchBatch result = new WebResearchClient(transport, new SearchEngine("brave", ""), KEY)
                .researchResult("colada", () -> false);
        assertEquals(Collections.singletonList("api.search.brave.com"), calls);
        assertEquals(WikipediaResearchClient.Status.ERROR, result.status);
    }
}
