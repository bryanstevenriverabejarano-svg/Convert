package salve.core.research;

import com.google.gson.*;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.atomic.*;
import salve.core.ModelResult;
import salve.core.WikipediaResearchClient;
import static org.junit.Assert.*;
import static salve.core.research.PublicResearchCoordinator.*;

/** Deterministic protocol/transport fixtures, not a claim that a real model always reasons correctly. */
public class GuidedPublicResearchTest {
    private static final String PLAN = "{\"objective\":\"Comparar información sobre coladas\",\"subject\":\"colada\",\"person\":false,"
            + "\"goals\":[\"definición\",\"contrastar\"],\"queries\":[\"colada definición\",\"colada significado\"]}";
    private static final String TEXT = "La colada es el lavado de la ropa.";
    private static ModelResult ok(String text) { return ModelResult.success(text, 0); }
    private static WikipediaResearchClient.Page page(String url, String text) { return new WikipediaResearchClient.Page(url, text); }
    private static WikipediaResearchClient.ResearchBatch batch(WikipediaResearchClient.Page... pages) {
        return new WikipediaResearchClient.ResearchBatch(WikipediaResearchClient.Status.COMPLETE, Arrays.asList(pages), 0);
    }
    private static String evidence(String quote) {
        JsonObject value = new JsonObject(); value.addProperty("relevant", true); value.addProperty("identity", "not_applicable");
        value.addProperty("identity_quote", ""); JsonArray quotes = new JsonArray(); quotes.add(quote); value.add("quotes", quotes); return value.toString();
    }
    @Test public void journalSeesAppliedPlanBeforeNetworkAndValidatedNotesBeforeNextPage() {
        List<String> events = new ArrayList<>();
        PublicResearchCoordinator.Observer observer = new PublicResearchCoordinator.Observer() {
            @Override public void onNote(String kind, String text) { events.add(kind + "\n" + text); }
        };
        SourceProvider provider = new SourceProvider() {
            @Override public WikipediaResearchClient.ResearchBatch read(String q, java.util.function.BooleanSupplier s) { throw new AssertionError(); }
            @Override public WikipediaResearchClient.ResearchBatch read(String q, java.util.function.BooleanSupplier s,
                    java.util.function.Consumer<WikipediaResearchClient.Page> consumer) {
                assertTrue(events.get(0).startsWith("PLAN DE BÚSQUEDA APLICADO"));
                WikipediaResearchClient.Page a = page("https://example.org/a", TEXT);
                consumer.accept(a);
                assertTrue(events.stream().anyMatch(event -> event.equals("NOTA VALIDADA — VALORACIÓN DEL MODELO\n"
                        + a.url + "\n" + evidence(TEXT))));
                return batch(a);
            }
        };
        Result result = new PublicResearchCoordinator(provider, (phase, prompt) -> phase == Phase.QUERY ? ok(PLAN)
                : phase == Phase.ANALYZE ? ok(evidence(TEXT)) : ok("Es el lavado de ropa [1]."), 5000, observer)
                .runConversation("Busca colada", () -> false);
        assertEquals(Status.ANSWERED, result.status);
        assertFalse(events.stream().anyMatch(event -> event.contains("Devuelve SOLO JSON")));
    }
    @Test public void understandsBeforeAnySearchAndAnalyzesEachLinkBeforeTheNext() {
        List<String> order = new ArrayList<>(); AtomicInteger reads = new AtomicInteger();
        SourceProvider provider = new SourceProvider() {
            @Override public WikipediaResearchClient.ResearchBatch read(String query, java.util.function.BooleanSupplier stop) { throw new AssertionError("Use streaming adapter"); }
            @Override public WikipediaResearchClient.ResearchBatch read(String query, java.util.function.BooleanSupplier stop,
                    java.util.function.Consumer<WikipediaResearchClient.Page> consumer) {
                order.add("search");
                WikipediaResearchClient.Page a = page("https://example.org/a", TEXT), b = page("https://example.net/b", "La colada también puede ser lava.");
                order.add("fetch-a"); consumer.accept(a); order.add("fetch-b"); consumer.accept(b); reads.incrementAndGet(); return batch(a,b);
            }
        };
        Result result = new PublicResearchCoordinator(provider, (phase, prompt) -> {
            order.add(phase.name());
            if (phase == Phase.QUERY) { assertEquals(0, reads.get()); return ok(PLAN); }
            if (phase == Phase.ANALYZE) return ok(evidence(prompt.contains("example.org/a") ? TEXT : "La colada también puede ser lava."));
            assertTrue(prompt.contains("contradicciones")); assertTrue(prompt.contains("example.net"));
            return ok("El término tiene usos distintos [1] [2].");
        }, 5000).runConversation("Busca qué es una colada y contrasta significados", () -> false);
        assertEquals(Arrays.asList("QUERY", "search", "fetch-a", "ANALYZE", "fetch-b", "ANALYZE"), order.subList(0,6));
        assertEquals(Status.ANSWERED, result.status); assertEquals(2,result.sources.size());
        assertEquals(4,result.modelCalls); // Duplicate URLs from the second query are not analyzed twice.
    }
    @Test public void noisyPersonRequestBecomesNamedQueriesNotWholeConversation() {
        String request = "busca en instagram a mireya prado informaxion, gustos preferencias coml crees qye ella es en personalidad ect";
        ResearchIntent fallback = ResearchIntent.fallback(request);
        assertTrue(fallback.person); assertEquals("mireya prado", fallback.subject);
        assertEquals("site:instagram.com \"mireya prado\"", fallback.queries.get(0));
        assertFalse(fallback.queries.get(0).contains("personalidad"));
        assertTrue(fallback.goals.stream().anyMatch(goal -> goal.contains("gustos")));
    }
    @Test public void rejectsInventedNamesSitesDuplicateKeysAndExecutablePlans() {
        String plan = "{\"objective\":\"Información pública\",\"subject\":\"Mireya Prado\",\"person\":true,"
                + "\"goals\":[\"estudios\"],\"queries\":[\"site:instagram.com Mireya Prado estudios\"]}";
        assertEquals("Mireya Prado", ResearchIntent.parse(plan,"Busca en Instagram a Mireya Prado estudios").subject);
        for (String invalid : Arrays.asList(plan.replace("Mireya", "Otra"), plan.replace("site:instagram.com", "https://inventada.org/"),
                plan.replace("{\"objective\":", "{\"person\":false,\"objective\":"), "{\"action\":\"shell\"}")) {
            try { ResearchIntent.parse(invalid,"Busca en Instagram a Mireya Prado estudios"); fail("Invalid plan accepted"); }
            catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void homepagesAccessWallsAndIrrelevantEvidenceAreDiscarded() {
        AtomicInteger analyzed = new AtomicInteger();
        Result result = new PublicResearchCoordinator((q,s) -> batch(page("https://www.instagram.com/?hl=es","Comunidad segura"),
                new WikipediaResearchClient.Page("https://music.youtube.com/watch?v=a", "Tu navegador está obsoleto",200,20,"Tu navegador está obsoleto"),
                page("https://example.org/a", "Otra persona estudió piano.")), (phase,prompt) -> {
                    if (phase == Phase.QUERY) return ok(PLAN);
                    assertEquals(Phase.ANALYZE, phase); analyzed.incrementAndGet();
                    return ok("{\"relevant\":false,\"identity\":\"different\",\"identity_quote\":\"\",\"quotes\":[]}");
                }, 4000).runConversation("colada", () -> false);
        assertEquals(Status.NO_SOURCES,result.status); assertTrue(result.sources.isEmpty()); assertEquals(1,analyzed.get());
    }
    @Test public void fabricatedQuotesCannotReachSynthesis() {
        Result result = new PublicResearchCoordinator((q,s) -> batch(page("https://example.org/a",TEXT)),
                (phase,prompt) -> {
                    if (phase == Phase.QUERY) return ok(PLAN);
                    assertEquals(Phase.ANALYZE,phase);
                    return ok(evidence("Tiene treinta años y estudia física."));
                }, 4000).runConversation("colada", () -> false);
        assertEquals(Status.MODEL_FAILED,result.status); assertTrue(result.sources.isEmpty());
    }
    @Test public void ambiguousIdentityDoesNotProduceAPersonProfile() {
        String plan = "{\"objective\":\"Información pública\",\"subject\":\"Mireya Prado\",\"person\":true,"
                + "\"goals\":[\"estudios\"],\"queries\":[\"Mireya Prado estudios\"]}";
        Result result = new PublicResearchCoordinator((q,s) -> batch(page("https://example.org/mireya", "Mireya Prado estudió música.")),
                (phase,prompt) -> {
                    if (phase == Phase.QUERY) return ok(plan);
                    assertEquals(Phase.ANALYZE,phase);
                    return ok(evidence("Mireya Prado estudió música.").replace("not_applicable","unclear"));
                }, 4000).runConversation("Busca información de Mireya Prado estudios", () -> false);
        assertEquals(Status.PARTIAL,result.status); assertTrue(result.answer.contains("@usuario"));
        assertFalse(result.answer.contains("estudió música"));
    }
    @Test public void invalidSynthesisGetsOneRepairWithOnlyKnownEvidence() {
        AtomicInteger finals = new AtomicInteger();
        Result result = new PublicResearchCoordinator((q,s) -> batch(page("https://example.org/a",TEXT)),
                (phase,prompt) -> ok(phase == Phase.QUERY ? PLAN : phase == Phase.ANALYZE ? evidence(TEXT)
                        : finals.incrementAndGet() == 1 ? "Inventado [99]." : "Es el lavado de ropa [1]."), 4000)
                .runConversation("colada", () -> false);
        assertEquals(2,finals.get()); assertEquals(Status.ANSWERED,result.status);
        assertTrue(result.answer.contains("[1]")); assertFalse(result.answer.contains("[99]"));
    }
    @Test public void cancellationAfterIntentPreventsNetworkAndCancellationAfterPagePreventsNextInference() {
        AtomicBoolean cancel = new AtomicBoolean(); AtomicInteger calls = new AtomicInteger();
        Result result = new PublicResearchCoordinator((q,s) -> { fail("No network after cancellation"); return null; },
                (phase,prompt) -> { calls.incrementAndGet(); cancel.set(true); return ok(PLAN); },4000).runConversation("colada",cancel::get);
        assertEquals(Status.CANCELLED,result.status); assertEquals(1,calls.get()); assertEquals(0,result.searches);
    }
    @Test public void smallContextBudgetsAndManyPagesStillReserveTheAnswer() {
        AtomicInteger reads = new AtomicInteger(); AtomicInteger finals = new AtomicInteger();
        Result result = new PublicResearchCoordinator((q,s) -> {
            int n = reads.incrementAndGet(); return batch(page("https://example.org/a"+n,TEXT),page("https://example.net/b"+n,TEXT),page("https://example.com/c"+n,TEXT));
        }, (phase,prompt) -> {
            assertTrue(prompt.length() <= 2000);
            if (phase == Phase.QUERY) return ok(PLAN.replace("\"colada significado\"", "\"colada significado\",\"colada usos\""));
            if (phase == Phase.ANALYZE) return ok(evidence(TEXT));
            finals.incrementAndGet(); return ok("Lavado de la ropa [1].");
        },2000).runConversation("colada",()->false);
        assertTrue(result.modelCalls <= MAX_MODEL_CALLS); assertEquals(6,result.sources.size()); assertEquals(1,finals.get());
        assertEquals(Status.PARTIAL,result.status);
    }
    @Test public void originalDirectUrlIsPreservedInsteadOfInventingAReplacement() {
        ResearchIntent intent = ResearchIntent.parse(PLAN,"Compara estudios en https://example.org/Perfil?Idioma=ES");
        assertEquals(Collections.singletonList("https://example.org/Perfil?Idioma=ES"),intent.queries);
    }
}
