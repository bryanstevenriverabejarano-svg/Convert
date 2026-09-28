package salve.core.agent;

import android.content.Context;
import java.util.function.BooleanSupplier;
import salve.core.WikipediaResearchClient;
import salve.core.memory.ConversationMemoryGrounding;
import salve.data.db.MemoriaDatabase;

/** Typed executors. The only local writes are the journal and the final memory transaction. */
public final class AndroidAgentTools implements AgentEngine.Tools {
    private final Context app;
    private final WikipediaResearchClient web = new WikipediaResearchClient();
    private final ConversationMemoryGrounding memory;
    private final java.util.function.Supplier<String> statistics;
    public AndroidAgentTools(Context context, java.util.function.Supplier<String> statistics) {
        app = context.getApplicationContext(); this.statistics = statistics;
        MemoriaDatabase db = MemoriaDatabase.getInstance(app);
        memory = new ConversationMemoryGrounding(db.recuerdoDao(), db.knowledgeNodeDao(), db.knowledgeRelationDao());
    }
    @Override public String capabilities(AgentRun run) {
        String metrics = statistics.get();
        return "memory.lookup: " + (run.codeAllowed ? "no autorizada" : "memoria local")
                + "; web.search: objetivo literal, Wikipedia/puente; web.read/api.get: URL pública autorizada; "
                + "browser.render: " + (run.bridge.isEmpty() ? "sin configurar" : "puente configurado, disponibilidad por comprobar")
                + "; code.python: " + (run.codeAllowed && !run.bridge.isEmpty() ? "autorizado, requiere sandbox disponible" : "no disponible")
                + ". Contadores observados (no prueban verdad): " + metrics.substring(0, Math.min(280, metrics.length()));
    }
    @Override public AgentRun.Observation execute(AgentRun run, AgentRun.Pending call, BooleanSupplier stopped) throws Exception {
        if (call.tool.equals("memory.lookup")) {
            ConversationMemoryGrounding.Result result = memory.retrieve(call.input);
            result = result.withCloudStatus(salve.data.sync.CloudSyncManager.memoryStatus(app),
                    salve.data.sync.CloudSyncManager.memoryRestoreComplete(app));
            return new AgentRun.Observation(call.id, call.tool, result.hasEvidence()
                    ? result.getStatus() == ConversationMemoryGrounding.Status.PARTIAL ? "PARTIAL" : "SUCCESS" : "ERROR",
                    result.getContext().isEmpty() ? "No se recuperó evidencia pertinente; no implica memoria vacía." : result.getContext());
        }
        if (call.tool.equals("browser.render") || call.tool.equals("code.python") || call.tool.equals("web.search") && !run.bridge.isEmpty()) {
            ToolBridgeConfig config = ToolBridgeConfig.load(app);
            if (config == null || !config.endpoint.equals(run.bridge)) {
                return new AgentRun.Observation(call.id, call.tool, "BLOCKED", "La configuración del puente falta o cambió; crea un plan con la configuración actual.");
            }
            try {
                AgentRun.Observation receipt = new ToolBridgeClient(config).execute(call, stopped);
                if (!call.tool.equals("web.search") || receipt.useful()) return receipt;
            } catch (java.io.IOException failed) { if (!call.tool.equals("web.search")) throw failed; }
            // A failed optional search provider falls back to the existing public discovery reader.
        }
        AgentRun.Observation observation = new AgentRun.Observation(call.id, call.tool, "SUCCESS", "");
        if (call.tool.equals("web.search")) {
            WikipediaResearchClient.ResearchBatch batch = web.researchResult(call.input, stopped);
            if (batch.pages.isEmpty()) return new AgentRun.Observation(call.id, call.tool, "ERROR", "No se recuperaron fuentes públicas.");
            observation.status = batch.status == WikipediaResearchClient.Status.COMPLETE ? "SUCCESS" : "PARTIAL";
            StringBuilder text = new StringBuilder("Descubrimiento público mediante Wikipedia/URLs explícitas:\n");
            for (WikipediaResearchClient.Page page : batch.pages) {
                if (page.text.length() >= WikipediaResearchClient.MAX_EXTRACT_CHARS) observation.status = "PARTIAL";
                observation.sources.add(page.url);
                text.append(page.url).append('\n').append(page.text.substring(0, Math.min(1400, page.text.length()))).append('\n');
                for (String url : page.links) if (observation.links.size() < 12 && !observation.links.contains(url)) observation.links.add(url);
            }
            observation.text = text.substring(0, Math.min(6000, text.length()));
        } else if (call.tool.equals("web.read") || call.tool.equals("api.get")) {
            WikipediaResearchClient.Page page = web.fetch(call.input, stopped);
            observation.status = page.text.length() >= WikipediaResearchClient.MAX_EXTRACT_CHARS ? "PARTIAL" : "SUCCESS";
            observation.text = page.text; observation.sources.add(page.url); observation.links.addAll(page.links);
        } else return new AgentRun.Observation(call.id, call.tool, "BLOCKED", "No hay ejecutor registrado.");
        return observation;
    }
}
