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
    private final String braveKey; // clave del dueño; null si no hay
    public WebResearchClient(Context context) { this(context, ""); }
    public WebResearchClient(Context context, String question) {
        this.context = context.getApplicationContext(); selected = WebSearchSettings.load(this.context, question);
        braveKey = WebSearchKeyStore.braveKey(this.context);
    }
    WebResearchClient(okhttp3.OkHttpClient client, SearchEngine selected) { this(client, selected, null); }
    WebResearchClient(okhttp3.OkHttpClient client, SearchEngine selected, String braveKey) {
        super(client); context = null; this.selected = selected; this.braveKey = braveKey;
    }
    @Override public ResearchBatch researchResult(String question, BooleanSupplier stopped) {
        return researchResult(question, stopped, null);
    }
    @Override public ResearchBatch researchResult(String question, BooleanSupplier stopped, java.util.function.Consumer<Page> consumer) {
        if (question == null || question.trim().isEmpty() || question.length() > MAX_QUERY_CHARS || question.contains("://"))
            return deliverPages(super.researchResult(question, stopped), stopped, consumer);
        String explicit = SearchEngine.explicitEngine(question);
        SearchEngine engine = explicit == null ? selected : new SearchEngine(explicit, "");
        String query = SearchEngine.cleanQuery(question);
        if (engine.id.equals("wikipedia")) return deliverPages(super.researchResult(query, stopped), stopped, consumer);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        List<Page> pages = new ArrayList<>();
        List<String> attempts = new ArrayList<>();
        List<SearchEngine> choices = new ArrayList<>();
        if (engine.id.equals("auto")) {
            // Con clave propia, la API oficial va primero: no hay CAPTCHA ni HTML que adivinar.
            if (braveKey != null) choices.add(new SearchEngine("brave", ""));
            choices.add(new SearchEngine("google", "")); choices.add(new SearchEngine("bing", ""));
            choices.add(new SearchEngine("duckduckgo", ""));
        } else choices.add(engine);
        String lastUrl = "";
        try {
            for (SearchEngine candidate : choices) {
                checkSearch(stopped, deadline);
                lastUrl = candidate.url(query);
                try {
                    List<String> urls;
                    if (candidate.id.equals("brave")) {
                        if (braveKey == null) { attempts.add("brave: falta tu clave de la API (Buscadores web → Brave Search API)"); continue; }
                        Page api = readApiJson(BraveSearchApi.requestUrl(query), BraveSearchApi.KEY_HEADER, braveKey, stopped, deadline);
                        urls = BraveSearchApi.links(api.text);
                        if (urls.isEmpty()) { attempts.add("brave: sin resultados para esta consulta"); continue; }
                    } else {
                        Page document = readDocument(lastUrl, stopped, deadline);
                        if (SearchEngine.interactionRequired(document.url, document.text)) {
                            attempts.add(candidate.id + ": requiere intervención en el navegador"); continue;
                        }
                        urls = SearchEngine.links(document.text, document.url, salve.core.research.ResearchSourceQuality::candidate);
                        if (urls.isEmpty()) { attempts.add(candidate.id + ": no devolvió enlaces legibles; puede requerir JavaScript o consentimiento"); continue; }
                    }
                    int failures = 0;
                    for (String url : urls) {
                        checkSearch(stopped, deadline);
                        if (pages.size() == MAX_PAGES) break;
                        if (!salve.core.research.ResearchSourceQuality.candidate(url)) { failures++; continue; }
                        try {
                            Page page = readPage(url, stopped, deadline);
                            if (salve.core.research.ResearchSourceQuality.usable(page.url, page.title, page.text)) {
                                pages.add(page);
                                if (consumer != null) {
                                    long analysisStarted = System.nanoTime();
                                    consumer.accept(page); // Analyze now, before requesting the next link.
                                    deadline += System.nanoTime() - analysisStarted; // Model time is not network time.
                                } else if (context != null) SearchLive.reading(page.url, page.title);
                            }
                            else failures++;
                        }
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
                    boolean http = reason != null && reason.matches("La fuente pública devolvió HTTP [0-9]{3}\\.");
                    if (candidate.id.equals("brave") && http)
                        attempts.add(BraveSearchApi.failure(Integer.parseInt(reason.replaceAll("[^0-9]", ""))));
                    else attempts.add(candidate.id + ": " + (http ? reason : "fallo de conexión o tiempo agotado"));
                }
            }
            checkSearch(stopped, deadline);
            if (engine.id.equals("auto")) {
                ResearchBatch fallback = super.researchResult(query, stopped);
                if (!fallback.pages.isEmpty()) {
                    String info = "Buscadores generales sin resultados legibles; se consultó Wikipedia como respaldo. " + String.join("; ", attempts);
                    remember(lastUrl, info);
                    return deliverPages(new ResearchBatch(Status.PARTIAL, fallback.pages, attempts.size(), info), stopped, consumer);
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
