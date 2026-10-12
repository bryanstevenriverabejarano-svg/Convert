package salve.core;

import android.content.Context;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import salve.core.web.*;

/** General public discovery plus actual page reads. No API key or private context in queries. */
public final class WebResearchClient extends WikipediaResearchClient {
    private final Context context;
    private final SearchEngine selected;
    public WebResearchClient(Context context) { this(context, ""); }
    public WebResearchClient(Context context, String question) {
        this.context = context.getApplicationContext(); selected = WebSearchSettings.load(this.context, question);
    }
    WebResearchClient(okhttp3.OkHttpClient client, SearchEngine selected) { super(client); context = null; this.selected = selected; }
    @Override public ResearchBatch researchResult(String question, BooleanSupplier stopped) {
        if (question == null || question.trim().isEmpty() || question.length() > MAX_QUERY_CHARS || question.contains("://"))
            return super.researchResult(question, stopped);
        String explicit = SearchEngine.explicitEngine(question);
        SearchEngine engine = explicit == null ? selected : new SearchEngine(explicit, "");
        String query = SearchEngine.cleanQuery(question);
        if (engine.id.equals("wikipedia")) return super.researchResult(query, stopped);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        List<Page> pages = new ArrayList<>();
        List<String> attempts = new ArrayList<>();
        List<SearchEngine> choices = engine.id.equals("auto") ? Arrays.asList(new SearchEngine("google", ""),
                new SearchEngine("bing", ""), new SearchEngine("duckduckgo", "")) : Collections.singletonList(engine);
        String lastUrl = "";
        try {
            for (SearchEngine candidate : choices) {
                checkSearch(stopped, deadline);
                lastUrl = candidate.url(query);
                try {
                    Page document = readDocument(lastUrl, stopped, deadline);
                    if (SearchEngine.interactionRequired(document.url, document.text)) {
                        attempts.add(candidate.id + ": requiere intervención en el navegador"); continue;
                    }
                    List<String> urls = SearchEngine.links(document.text, document.url);
                    if (urls.isEmpty()) { attempts.add(candidate.id + ": no devolvió enlaces legibles; puede requerir JavaScript o consentimiento"); continue; }
                    int failures = 0;
                    for (String url : urls) {
                        checkSearch(stopped, deadline);
                        if (pages.size() == MAX_PAGES) break;
                        try { Page page = readPage(url, stopped, deadline); if (!page.text.isEmpty()) pages.add(page); else failures++; }
                        catch (java.io.IOException failed) { failures++; }
                    }
                    if (!pages.isEmpty()) {
                        String info = "Búsqueda mediante " + candidate.id + "; " + pages.size() + " páginas leídas."
                                + (attempts.isEmpty() ? "" : " Intentos previos: " + String.join("; ", attempts));
                        remember(lastUrl, info);
                        return new ResearchBatch(failures == 0 ? Status.COMPLETE : Status.PARTIAL, pages, failures, info);
                    }
                    attempts.add(candidate.id + ": los resultados no pudieron leerse");
                } catch (java.io.IOException failed) {
                    // Our transport emits fixed messages, never credentials, response bodies or private context.
                    String reason = failed.getMessage();
                    attempts.add(candidate.id + ": " + (reason != null && reason.matches("La fuente pública devolvió HTTP [0-9]{3}\\.")
                            ? reason : "fallo de conexión o tiempo agotado"));
                }
            }
            checkSearch(stopped, deadline);
            if (engine.id.equals("auto")) {
                ResearchBatch fallback = super.researchResult(query, stopped);
                if (!fallback.pages.isEmpty()) {
                    String info = "Buscadores generales sin resultados legibles; se consultó Wikipedia como respaldo. " + String.join("; ", attempts);
                    remember(lastUrl, info);
                    return new ResearchBatch(Status.PARTIAL, fallback.pages, attempts.size(), info);
                }
            }
            String info = String.join("; ", attempts) + ". Abre IA y cámara → Buscadores web → Abrir última búsqueda para continuar en el navegador.";
            remember(lastUrl, info);
            return new ResearchBatch(Status.ERROR, pages, Math.max(1, attempts.size()), info);
        } catch (CancellationException cancelled) { return new ResearchBatch(Status.CANCELLED, pages, 0); }
        catch (SearchTimeout timeout) {
            String info = "Se agotó el tiempo de búsqueda. " + String.join("; ", attempts);
            remember(lastUrl, info); return new ResearchBatch(Status.BUDGET_EXHAUSTED, pages, attempts.size(), info);
        }
    }
    private void remember(String url, String diagnostic) { if (context != null) WebSearchSettings.lastSearch(context, url, diagnostic); }
    private static void checkSearch(BooleanSupplier stopped, long deadline) {
        if (Thread.currentThread().isInterrupted() || stopped != null && stopped.getAsBoolean()) throw new CancellationException();
        if (System.nanoTime() >= deadline) throw new SearchTimeout();
    }
    private static final class SearchTimeout extends RuntimeException { }
}
