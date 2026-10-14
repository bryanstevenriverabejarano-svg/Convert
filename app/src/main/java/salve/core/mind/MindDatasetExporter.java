package salve.core.mind;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Exporta lo consolidado para entrenar un adaptador LoRA personal fuera del móvil.
 *
 * <p>Solo sale conocimiento con procedencia: la narrativa vigente, creencias no contradichas con
 * confianza suficiente y pensamientos con fuentes, más los episodios citados. El script de
 * {@code scripts/mind_lora} genera ejemplos de conversación a partir de esto; el móvil no entrena.</p>
 */
public final class MindDatasetExporter {
    public static final double MIN_CONFIDENCE = 0.6;
    private static final Gson GSON = new Gson();

    private MindDatasetExporter() {}

    /** Una línea JSON por elemento (JSONL). */
    public static String knowledge(SelfModelJournal journal) {
        StringBuilder out = new StringBuilder();
        JsonObject narrative = new JsonObject();
        narrative.addProperty("type", "narrative");
        narrative.addProperty("text", journal.narrative());
        out.append(GSON.toJson(narrative)).append('\n');
        for (SelfModel.Belief b : journal.beliefs()) {
            if (b.confidence < MIN_CONFIDENCE || !b.contradictions.isEmpty() || b.sources.isEmpty()) continue;
            JsonObject o = new JsonObject();
            o.addProperty("type", "belief");
            o.addProperty("about", b.about);
            o.addProperty("text", b.statement);
            o.addProperty("confidence", Math.round(b.confidence * 100) / 100.0);
            o.add("sources", GSON.toJsonTree(b.sources));
            out.append(GSON.toJson(o)).append('\n');
        }
        for (SelfModel.Thought t : journal.thoughts()) {
            if (t.sources.isEmpty()) continue;
            JsonObject o = new JsonObject();
            o.addProperty("type", "thought");
            o.addProperty("text", t.text);
            o.add("sources", GSON.toJsonTree(t.sources));
            out.append(GSON.toJson(o)).append('\n');
        }
        return out.toString();
    }

    public static String episodes(List<Episode> episodes) {
        StringBuilder out = new StringBuilder();
        for (Episode e : episodes) {
            JsonObject o = new JsonObject();
            o.addProperty("id", e.id);
            o.addProperty("at", e.at);
            o.addProperty("text", e.text);
            out.append(GSON.toJson(o)).append('\n');
        }
        return out.toString();
    }
}
