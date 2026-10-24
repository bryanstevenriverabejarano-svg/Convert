package salve.core.synergy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** A candidate for a reviewed source experiment, never an executable patch. */
public final class SynergyEvolution {
    private SynergyEvolution() {}
    private static final Map<String,String> TARGETS = new LinkedHashMap<>();
    static {
        TARGETS.put("no_devuelve_pregunta","app/src/main/java/salve/core/conversation/TurnChecks.java");
        TARGETS.put("no_anuncia_sin_hacer","app/src/main/java/salve/core/conversation/TurnChecks.java");
        TARGETS.put("no_repite","app/src/main/java/salve/core/conversation/ReplyReviewer.java");
        TARGETS.put("no_niega_memoria","app/src/main/java/salve/core/conversation/MemoryRecall.java");
        TARGETS.put("no_inventa_recuerdo","app/src/main/java/salve/core/conversation/MemoryRecall.java");
        TARGETS.put("usa","app/src/main/java/salve/core/cognition/CognitiveToolCatalog.java");
        TARGETS.put("consulta_limpia","app/src/main/java/salve/core/mind/CuriosityCycle.java");
    }
    public static JsonObject candidate(JsonObject exam) {
        if (exam==null||!exam.has("cases")||!exam.get("cases").isJsonArray()) return null;
        Map<String,Integer> failures=new LinkedHashMap<>();
        JsonArray cases=exam.getAsJsonArray("cases");
        for (JsonElement entry:cases) {
            if (!entry.isJsonObject()) continue;
            JsonObject c=entry.getAsJsonObject();
            if (!c.has("failed")||!c.get("failed").isJsonArray()) continue;
            for (JsonElement value:c.getAsJsonArray("failed")) {
                if (!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()) continue;
                String key=value.getAsString().split(":",2)[0].trim();
                if (TARGETS.containsKey(key)) failures.put(key,failures.getOrDefault(key,0)+1);
            }
        }
        String issue=null; int hits=0;
        for (Map.Entry<String,Integer> e:failures.entrySet())
            if (e.getValue()>hits) { issue=e.getKey(); hits=e.getValue(); }
        if (issue==null||hits<2) return null;
        JsonObject plan=new JsonObject();
        plan.addProperty("schema",1);
        plan.addProperty("stage","PROPOSAL_ONLY");
        plan.addProperty("issue_id",issue);
        plan.addProperty("observed_failures",hits);
        plan.addProperty("source_module_candidate",TARGETS.get(issue));
        plan.addProperty("hypothesis","A targeted code change may reduce this observed failure.");
        plan.addProperty("required_test","Compare fixed exam cases plus unseen regression cases with previous build.");
        plan.addProperty("runner","scripts/auto_improvement_runner.py");
        plan.addProperty("sandbox_and_user_review_required",true);
        plan.addProperty("signed_update_automatic",false);
        plan.addProperty("rollback_required",true);
        return plan;
    }
    public static String describe(JsonObject exam) {
        JsonObject plan=candidate(exam);
        if (plan==null) return "\nEVOLUCIÓN ESTRUCTURAL: faltan fallos repetidos del examen para proponer cambios de código.";
        return "\nEVOLUCIÓN ESTRUCTURAL: identifiqué "+plan.get("observed_failures").getAsInt()+
                " fallos del tipo "+plan.get("issue_id").getAsString()+
                ". Módulo a revisar: "+plan.get("source_module_candidate").getAsString()+
                ". Hipótesis y pruebas pendientes; no he modificado ni instalado código. "+
                "Una propuesta de parche debe superar pruebas aisladas, comparación y tu revisión.";
    }
}
