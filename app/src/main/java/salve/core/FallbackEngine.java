package salve.core;

import android.content.Context;

import com.salve.salve.core.LocalLlmEngine;

/**
 * Motor de respaldo para desarrollo.
 * No infiere de verdad: devuelve una respuesta amable usando el USER del prompt.
 * Sirve para comprobar el cableado (que todo llama a generate()).
 */
public class FallbackEngine implements LocalLlmEngine {

    private boolean ready = true;
    private String lastModelPath = null;

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public void loadIfNeeded(Context ctx, String modelPath) {
        // No carga nada real, solo recuerda la ruta (para logs futuros).
        this.lastModelPath = modelPath;
        this.ready = true;
    }

    @Override
    public String generate(String prompt, int maxTokens, float temperature, int topK, float topP) {
        // Extrae el bloque USER (si existe) para simular una respuesta útil.
        String user = prompt;
        int i = prompt.lastIndexOf("<<USER>>");
        if (i >= 0) {
            user = prompt.substring(i).replace("<<USER>>", "")
                    .replace("<</USER>>", "")
                    .replace("<<ASSISTANT>>", "")
                    .trim();
        }
        String modelo = (lastModelPath == null) ? "(modelo no especificado)" : lastModelPath;
        return "Estoy usando el flujo de LLM (modo prueba). Modelo: " + modelo +
                "\n\nPedido del usuario:\n" + user +
                "\n\nRespuesta (borrador): puedo desarrollar esto cuando conectemos el motor real. 😊";
    }
}
