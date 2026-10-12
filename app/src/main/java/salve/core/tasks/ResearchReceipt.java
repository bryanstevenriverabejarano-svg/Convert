package salve.core.tasks;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import salve.core.NetworkResourcePolicy;
import salve.core.research.PublicResearchCoordinator;

/** Evidence checkpoint, distinct from autobiographical facts and executable instructions. */
public final class ResearchReceipt {
    public static final int MAX_BYTES = 64_000;
    public String status, answer;
    public List<Source> sources = new ArrayList<>();
    public List<String> providers = new ArrayList<>();
    /** Failed/cancelled reads never become learned facts; usable extracts keep their partial status. */
    public static ResearchReceipt fromResult(PublicResearchCoordinator.Result result, List<String> providers) {
        if (result == null || result.sources.isEmpty() || result.status == PublicResearchCoordinator.Status.CANCELLED)
            return null;
        ResearchReceipt receipt = new ResearchReceipt();
        receipt.status = result.status == PublicResearchCoordinator.Status.ANSWERED ? "ANSWERED"
                : result.status == PublicResearchCoordinator.Status.PARTIAL ? "PARTIAL" : "SOURCES_ONLY";
        receipt.answer = result.toConversationText();
        if (providers != null) receipt.providers.addAll(providers);
        for (PublicResearchCoordinator.Source source : result.sources)
            receipt.sources.add(new Source(source.url, source.excerpt));
        receipt.validate();
        return receipt;
    }
    public static final class Source {
        public String url, excerpt;
        public Source(String url, String excerpt) { this.url = url; this.excerpt = excerpt; }
    }
    public String encode() {
        validate();
        String json = new Gson().toJson(this);
        if (json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new IllegalArgumentException("Recibo demasiado grande");
        return json;
    }
    public static ResearchReceipt decode(String json) {
        if (json == null || json.length() > MAX_BYTES) throw new IllegalArgumentException("Recibo inválido");
        ResearchReceipt receipt = new Gson().fromJson(json, ResearchReceipt.class);
        if (receipt == null) throw new IllegalArgumentException("Recibo vacío");
        receipt.validate();
        return receipt;
    }
    public void validate() {
        if (!("ANSWERED".equals(status) || "PARTIAL".equals(status) || "SOURCES_ONLY".equals(status)))
            throw new IllegalArgumentException("Resultado no utilizable");
        if (answer == null || answer.trim().isEmpty() || answer.length() > 12_000
                || sources == null || sources.isEmpty() || sources.size() > 9
                || providers == null || providers.size() > 4)
            throw new IllegalArgumentException("Evidencia fuera de límites");
        for (Source source : sources) {
            if (source == null || source.url == null || source.url.length() > 500
                    || !NetworkResourcePolicy.validateKnowledgeUrl(source.url).allowed
                    || source.excerpt == null || source.excerpt.trim().isEmpty() || source.excerpt.length() > 2500)
                throw new IllegalArgumentException("Fuente inválida");
        }
        for (String provider : providers)
            if (provider == null || provider.length() > 240) throw new IllegalArgumentException("Proveedor inválido");
    }
    public String completionStatus() { return "ANSWERED".equals(status) ? "SUCCEEDED" : "PARTIAL"; }
}
