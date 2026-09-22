package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Render only independently verified answers; a candidate is never presented as a solution. */
public final class AutonomousToolReply {
    private AutonomousToolReply() { }

    public static String summarize(JsonObject report) {
        if (report == null) return "El laboratorio no devolvió un informe válido.";
        String status = string(report, "status", "unverified");
        boolean verified = bool(report, "verified")
                && ("verified".equals(status) || "storage_error".equals(status));
        JsonArray attempts = report.has("attempts") && report.get("attempts").isJsonArray()
                ? report.getAsJsonArray("attempts") : new JsonArray();
        if (!verified) {
            String message;
            switch (status) {
                case "cancelled": message = "El cálculo fue cancelado."; break;
                case "budget_exhausted": message = "Alcancé el límite de cálculo."; break;
                case "storage_error": message = "No pude guardar el estado del laboratorio."; break;
                default: message = "No encontré una solución verificada."; break;
            }
            return message + " No presento el resultado candidato como correcto. Intentos: " + attempts.size() + ".";
        }
        JsonElement result = report.get("result");
        if (result == null || !result.isJsonObject()) return "El informe verificado no contiene un resultado utilizable.";
        String strategy = "no indicada";
        for (int i = attempts.size() - 1; i >= 0; i--) {
            if (attempts.get(i).isJsonObject() && bool(attempts.get(i).getAsJsonObject(), "passed")) {
                strategy = string(attempts.get(i).getAsJsonObject(), "strategy", strategy);
                break;
            }
        }
        return "Resultado verificado para los datos del reto: " + result
                + "\nEstrategia: " + bounded(strategy, 120) + ". Intentos: " + attempts.size() + "."
                + (bool(report, "adapted") ? " Adapté la estrategia tras comprobar una alternativa." : "")
                + ("storage_error".equals(status)
                        ? " No pude guardar la herramienta; la versión anterior permanece."
                        : bool(report, "promoted") ? " La herramienta pasó las comprobaciones de promoción y quedó registrada." : "");
    }

    private static boolean bool(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
                && value.getAsBoolean();
    }

    private static String string(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : fallback;
    }

    private static String bounded(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }
}
