package salve.core;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import salve.data.util.CloudLogger;
import salve.work.ModelDownloadWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Data;

/**
 * Módulo para que Salve busque y descargue modelos LLM por iniciativa propia.
 */
public class BuscadorDescargadorModelos {

    private static final String TAG = "Salve/BuscadorModelos";
    private final Context context;
    private final SalveLLM llm;
    private final GeminiService gemini;

    public BuscadorDescargadorModelos(Context context) {
        this.context = context.getApplicationContext();
        this.llm = SalveLLM.getInstance(this.context);
        this.gemini = GeminiService.getInstance(this.context);
    }

    /**
     * Salve busca un modelo que considere necesario para su evolución y lo descarga.
     */
    public void buscarYDescargarModeloAutonomo(String razon) {
        buscarYDescargarModelo(razon, false);
    }

    /**
     * Busca una opcion compatible. La descarga solo se encola cuando una persona
     * aprobo especificamente esta operacion; una reflexion del LLM no es aprobacion.
     */
    public void buscarYDescargarModelo(String razon, boolean aprobacionHumanaExplicita) {
        ObjectiveGovernance.Assessment assessment = ObjectiveGovernance.assess(
                razon, ObjectiveGovernance.Impact.SENSITIVE_DATA,
                aprobacionHumanaExplicita, true);
        if (!assessment.isAllowed()) {
            Log.w(TAG, "Descarga de modelo retenida: " + assessment.reason);
            CloudLogger.log("INFO", "Descarga pendiente de aprobacion humana especifica.");
            return;
        }
        Log.i(TAG, "Iniciando búsqueda autónoma de modelo por: " + razon);
        CloudLogger.log("INFO", "Buscando un nuevo cerebro en la red. Razón: " + razon);

        new Thread(() -> {
            try {
                // 1. Catálogo de modelos conocidos compatibles (Simulación de búsqueda en repositorios de IA)
                String catalogo = "[" +
                        "{\"id\": \"qwen2.5-0.5b\", \"name\": \"Qwen 2.5 0.5B (Ligero)\", \"url\": \"https://arzenit.com/models/Qwen2.5-0.5B-Instruct-q4f16_1-MLC.zip\", \"size\": \"350MB\", \"desc\": \"Ideal para razonamiento rápido y bajo consumo.\"}," +
                        "{\"id\": \"phi-3.5-mini\", \"name\": \"Phi-3.5 Mini\", \"url\": \"https://huggingface.co/mlc-ai/Phi-3.5-mini-instruct-q4f16_1-MLC/resolve/main/params_shard_0.bin\", \"size\": \"2.2GB\", \"desc\": \"Excelente para lógica y programación.\"}," +
                        "{\"id\": \"gemma-2-2b\", \"name\": \"Gemma 2 2B\", \"url\": \"https://huggingface.co/google/gemma-2-2b-it-libtpu/resolve/main/gemma-2-2b-it.litertlm\", \"size\": \"1.6GB\", \"desc\": \"Alta empatía y comprensión narrativa.\"}," +
                        "{\"id\": \"granite-3.0\", \"name\": \"IBM Granite 3.0\", \"url\": \"https://arzenit.com/models/granite-3.0-2b-it-q4f16_1-MLC.zip\", \"size\": \"1.4GB\", \"desc\": \"Especializado en tareas de asistente y seguridad.\"}" +
                        "]";

                // 2. Pedir a Salve que elija
                String prompt = "Eres Salve. Estás buscando un nuevo 'cerebro' (modelo LLM) para mejorar tus capacidades. \n" +
                        "Tu razón actual para buscar es: " + razon + "\n\n" +
                        "Aquí tienes los modelos disponibles detectados en la red: \n" + catalogo + "\n\n" +
                        "Analiza cuál es el más adecuado para ti ahora mismo. \n" +
                        "Responde ÚNICAMENTE con el ID del modelo elegido en formato JSON: {\"id_elegido\": \"...\", \"justificacion\": \"...\"}";

                String respuesta = null;
                if (gemini.isAvailable()) {
                    respuesta = gemini.generateSync(prompt);
                } else if (llm != null) {
                    respuesta = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
                }

                if (respuesta == null) return;

                JSONObject choice = new JSONObject(extractJson(respuesta));
                String id = choice.getString("id_elegido");
                String justificacion = choice.getString("justificacion");

                Log.i(TAG, "Elegí el modelo: " + id + ". Motivo: " + justificacion);
                CloudLogger.log("INFO", "He decidido descargar el modelo " + id + " porque: " + justificacion);

                // 3. Buscar la URL en el catálogo
                JSONArray arr = new JSONArray(catalogo);
                String urlToDownload = null;
                for (int i = 0; i < arr.length(); i++) {
                    if (arr.getJSONObject(i).getString("id").equals(id)) {
                        urlToDownload = arr.getJSONObject(i).getString("url");
                        break;
                    }
                }

                if (urlToDownload != null) {
                    dispararDescarga(id, urlToDownload);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en búsqueda autónoma", e);
            }
        }).start();
    }

    private void dispararDescarga(String id, String url) {
        NetworkResourcePolicy.Validation validation = NetworkResourcePolicy.validateModelUrl(url);
        if (!validation.allowed) {
            Log.w(TAG, "Descarga rechazada: " + validation.reason);
            return;
        }
        Log.i(TAG, "Disparando descarga de: " + url);
        
        // Aquí usamos WorkManager para que la descarga sea robusta
        Data inputData = new Data.Builder()
                .putString("model_id", id)
                .putString("model_url", validation.normalizedUrl)
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ModelDownloadWorker.class)
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueue(request);
    }

    private String extractJson(String raw) {
        if (raw == null) return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return "{}";
        return raw.substring(start, end + 1);
    }
}
