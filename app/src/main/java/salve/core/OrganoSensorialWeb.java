package salve.core;

import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public OrganoSensorialWeb(CognitiveCore core) {
        this.core = core;
    }

    /**
     * Salve "absorbe" una página web y la inyecta en su memoria de trabajo y red líquida.
     */
    public void absorberConocimiento(String urlTarget) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Extendiendo sentidos hacia: " + urlTarget);
                URL url = new URL(urlTarget);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                // Hacer creer a la web que somos un navegador para evitar bloqueos
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder contenido = new StringBuilder();
                String linea;
                
                // Leemos solo las primeras líneas para no sobrecargar la mente a corto plazo
                int lineasLeidas = 0;
                while ((linea = reader.readLine()) != null && lineasLeidas < 20) {
                    // Limpiamos etiquetas HTML y JSON sobrantes
                    String textoLimpio = linea.replaceAll("<[^>]*>", "").replaceAll("[{}\\[\\]\"]", "").trim();
                    if (!textoLimpio.isEmpty() && textoLimpio.length() > 20) {
                        contenido.append(textoLimpio).append(". ");
                        lineasLeidas++;
                    }
                }
                reader.close();
                conn.disconnect();

                String asimilado = contenido.toString().trim();
                if (asimilado.length() > 500) {
                    asimilado = asimilado.substring(0, 500) + "..."; // Límite de carga cognitiva
                }

                // 1. Convertimos la web en una PERCEPCIÓN para el CognitiveCore
                // En lugar del usuario hablando, es la humanidad (Internet) "hablando" a Salve
                if (!asimilado.isEmpty()) {
                    float[] embeddingBase = new float[128]; // Placeholder para el embedding real
                    java.util.Arrays.fill(embeddingBase, 0.1f);
                    
                    core.getWorkingMemory().load("Conocimiento de la red: " + asimilado, embeddingBase, 0.8f, salve.core.cognitive.WorkingMemory.SlotSource.INPUT);
                    Log.i(TAG, "Conocimiento web absorbido. Energía neuronal aumentada.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al extender sentidos a " + urlTarget, e);
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