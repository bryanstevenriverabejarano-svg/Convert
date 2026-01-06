package ai.mlc.mlcllm;

import android.util.Log;

/**
 * JSONFFIEngine (STUB)
 *
 * Versión simplificada sin dependencia de org.apache.tvm.
 * Solo existe para que el proyecto compile. Toda la lógica real
 * de conversación la manejamos ahora con mlc4j / Salve.
 */
public class JSONFFIEngine {

    public interface KotlinFunction {
        void invoke(String arg);
    }

    public JSONFFIEngine() {
        Log.d("JSONFFIEngine", "Stub JSONFFIEngine inicializado (sin backend TVM).");
    }

    // No-op: no hace nada, solo evita errores de compilación
    public void initBackgroundEngine(KotlinFunction callback) { }

    public void reload(String engineConfigJSONStr) { }

    public void chatCompletion(String requestJSONStr, String requestId) { }

    public void runBackgroundLoop() { }

    public void runBackgroundStreamBackLoop() { }

    public void exitBackgroundLoop() { }

    public void unload() { }

    public void reset() { }
}
