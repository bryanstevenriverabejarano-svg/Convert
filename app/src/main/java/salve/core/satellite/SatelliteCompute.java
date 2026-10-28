package salve.core.satellite;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;

/**
 * El trabajo de cómputo de un satélite: corre un texto en su propio modelo local y devuelve la
 * respuesta. Presta cálculo a la Salve del dueño sin prestarle su identidad: usa el rol SALVENTE, un
 * contexto neutro, así que no mezcla recuerdos ni personalidad: solo hace la inferencia que se le pide.
 *
 * <p>Todo ocurre en el satélite; no sube el modelo a ningún sitio. Si el satélite no tiene un modelo
 * válido, lo dice en el resultado en vez de fallar en silencio.</p>
 */
public final class SatelliteCompute {
    private static final String TAG = "Salve/Satelite";

    private SatelliteCompute() { }

    /** Corre el trabajo de cómputo en el modelo local de este satélite y devuelve su resultado. */
    public static JsonObject execute(Context context, SatelliteProtocol.Assignment task) {
        String prompt = SatelliteProtocol.computePrompt(task == null ? null : task.params);
        if (prompt.isEmpty()) return SatelliteProtocol.computeOutput("error", "El trabajo no traía texto que inferir.", 0L);
        try {
            salve.core.ModelResult result = salve.core.SalveLLM.getInstance(context)
                    .generateLocalResult(prompt, salve.core.SalveLLM.Role.SALVENTE, null);
            String status = result.getStatus() == salve.core.ModelResult.Status.SUCCESS ? "ok"
                    : result.getStatus().name().toLowerCase(java.util.Locale.ROOT);
            return SatelliteProtocol.computeOutput(status, result.isSuccess() ? result.getText() : "", result.getLatencyMillis());
        } catch (RuntimeException | OutOfMemoryError error) {
            Log.w(TAG, "El trabajo de cómputo falló", error);
            return SatelliteProtocol.computeOutput("error", "", 0L);
        }
    }
}
