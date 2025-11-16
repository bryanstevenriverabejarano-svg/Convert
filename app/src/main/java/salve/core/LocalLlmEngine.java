package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * Motor local de modelos de lenguaje para Salve.
 *
 * Esta clase es un "wrapper" sencillo alrededor del motor real
 * (llama.cpp, gguf, etc.). De momento solo es un stub para que
 * todo compile y podamos integrar el motor más adelante.
 */
public class LocalLlmEngine {

    private static final String TAG = "LocalLlmEngine";

    // Singleton
    private static LocalLlmEngine instance;

    private final Context appContext;

    /**
     * Constructor privado. Usa getInstance() para obtener la instancia.
     */
    private LocalLlmEngine(Context context) {
        this.appContext = context.getApplicationContext();
        Log.d(TAG, "LocalLlmEngine inicializado.");
        // TODO: inicializar aquí el motor real (cargar libs nativas, etc.)
    }

    /**
     * Devuelve la instancia única del motor local.
     */
    public static synchronized LocalLlmEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LocalLlmEngine(context);
        }
        return instance;
    }

    /**
     * Punto de entrada síncrono que usa SalveLLM.
     *
     * @param modelId    ID del modelo (por ejemplo, "qwen2.5-3b-instruct")
     * @param fullPrompt Prompt completo (system + usuario)
     * @return Respuesta generada por el modelo local.
     */
    public String generateSync(String modelId, String fullPrompt) {
        if (TextUtils.isEmpty(modelId)) {
            Log.e(TAG, "generateSync() recibido con modelId vacío");
            return "/* LocalLlmEngine: modelId requerido */";
        }
        if (fullPrompt == null) {
            Log.w(TAG, "generateSync() recibió prompt nulo, se asume cadena vacía");
            fullPrompt = "";
        }

        Log.d(TAG, "generateSync() llamado. modelId=" + modelId
                + ", prompt.length=" + fullPrompt.length());

        if (engineNoInicializado()) {
            return buildStubResponse(modelId);
        }

        // ⚠️ Stub temporal: aquí todavía no está conectado el motor real.
        // Cuando tengamos el runtime GGUF, sustituiremos este bloque.
        return buildStubResponse(modelId);
    }

    private boolean engineNoInicializado() {
        // TODO: reemplazar cuando se inicie el motor real (gestionar handles, threads, etc.)
        return true;
    }

    private String buildStubResponse(String modelId) {
        return "/* LocalLlmEngine aún no está conectado a un modelo GGUF real. " +
                "modelId=" + modelId + " */";
    }
}
