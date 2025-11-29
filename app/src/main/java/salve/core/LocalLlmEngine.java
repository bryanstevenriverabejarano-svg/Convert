package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * Motor local de modelos de lenguaje para Salve.
 *
 * Recibe la ruta a un archivo .gguf y un prompt completo,
 * y delega la inferencia en el runtime real (por ahora FallbackEngine).
 */
public class LocalLlmEngine {

    private static final String TAG = "LocalLlmEngine";

    private static LocalLlmEngine instance;
    private final Context appContext;

    private LocalLlmEngine(Context context) {
        this.appContext = context.getApplicationContext();
        Log.d(TAG, "LocalLlmEngine inicializado.");
        // TODO: inicializar aquí el runtime real (cargar libs nativas, etc.)
    }

    public static synchronized LocalLlmEngine getInstance(Context context) {
        if (instance == null) {
            instance = new LocalLlmEngine(context);
        }
        return instance;
    }

    /**
     * Ejecuta un modelo local a partir de su archivo .gguf.
     *
     * @param modelPath  Ruta completa al modelo (.gguf) en almacenamiento interno.
     * @param fullPrompt Prompt completo (system + usuario).
     */
    public String generateSync(String modelPath, String fullPrompt) {
        if (TextUtils.isEmpty(modelPath)) {
            Log.e(TAG, "generateSync() recibido con modelPath vacío");
            return "/* LocalLlmEngine: modelPath requerido */";
        }
        if (fullPrompt == null) {
            Log.w(TAG, "generateSync() recibió prompt nulo, se asume cadena vacía");
            fullPrompt = "";
        }

        Log.d(TAG, "generateSync() llamado. modelPath=" + modelPath +
                ", prompt.length=" + fullPrompt.length());

        File f = new File(modelPath);
        if (!f.exists()) {
            return "/* LocalLlmEngine: modelo no encontrado en " + modelPath + " */";
        }

        if (engineNoInicializado()) {
            // De momento solo vista previa
            return FallbackEngine.buildPreview(modelPath, fullPrompt);
        }

        // TODO: aquí llamar al runtime real (MLC / llama.cpp / etc.)
        return FallbackEngine.buildPreview(modelPath, fullPrompt);
    }

    private boolean engineNoInicializado() {
        // Cambia a false cuando inicialices el runtime real
        return true;
    }
}
