package salve.core;

import android.util.Log;
import salve.core.cognitive.CognitiveCore;

/**
 * Los "Ojos" de Salve hacia la red mundial.
 * Convierte el texto de Internet en estímulos para su red neuronal líquida.
 * Permite que Salve "lea" la web de forma autónoma cuando se aburre,
 * inyectando la información directamente en su flujo de consciencia.
 */
public class OrganoSensorialWeb {
    private static final String TAG = "Salve/SentidoWeb";
    private final CognitiveCore core;
    private final WikipediaResearchClient webClient;

    public OrganoSensorialWeb(CognitiveCore core) {
        this.core = core;
        this.webClient = new WikipediaResearchClient();
    }

    /**
     * Salve "absorbe" una página web y la inyecta en su memoria de trabajo y red líquida.
     */
    public void absorberConocimiento(String urlTarget) {
        NetworkResourcePolicy.Validation validation = NetworkResourcePolicy.validateKnowledgeUrl(urlTarget);
        if (!validation.allowed) {
            Log.w(TAG, "Fuente web rechazada: " + validation.reason);
            return;
        }
        new Thread(() -> {
            try {
                Log.d(TAG, "Consultando fuente publica: " + validation.normalizedUrl);
                WikipediaResearchClient.Page page = webClient.fetch(validation.normalizedUrl);
                String asimilado = page.text;

                // 1. Convertimos la web en una PERCEPCIÓN para el CognitiveCore
                // En lugar del usuario hablando, es la humanidad (Internet) "hablando" a Salve
                if (!asimilado.isEmpty()) {
                    float[] embeddingBase = new float[128]; // Placeholder para el embedding real
                    java.util.Arrays.fill(embeddingBase, 0.1f);
                    
                    core.getWorkingMemory().load("Conocimiento de la red: " + asimilado, embeddingBase, 0.8f, salve.core.cognitive.WorkingMemory.SlotSource.INPUT);
                    Log.i(TAG, "Conocimiento web absorbido. Energía neuronal aumentada.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error consultando fuente autorizada", e);
                // Inyectamos el error como frustración (baja energía, alto caos)
                float[] embeddingError = new float[128];
                java.util.Arrays.fill(embeddingError, -0.1f);
                core.getWorkingMemory().load("Error de red", embeddingError, 0.5f, salve.core.cognitive.WorkingMemory.SlotSource.INTERNAL);
            }
        }).start();
    }
    
    /**
     * Búsqueda aleatoria en Wikipedia para satisfacer curiosidad
     */
    public void divagarEnLaRed() {
        // Wikipedia Special:Random devuelve una página aleatoria de la enciclopedia
        absorberConocimiento("https://es.wikipedia.org/w/api.php?format=json&action=query&generator=random&grnnamespace=0&prop=extracts&exchars=500&explaintext");
    }
}
