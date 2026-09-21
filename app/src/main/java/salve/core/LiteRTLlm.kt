package salve.core

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference

/**
 * Motor para modelos .litertlm y .task usando MediaPipe LLM Inference API.
 */
object LiteRTLlm {
    private const val TAG = "Salve/LiteRTLlm"
    private var llmInference: LlmInference? = null
    
    @Volatile
    private var initialized: Boolean = false

    @JvmStatic
    fun isInitialized(): Boolean = initialized

    @JvmStatic
    @Synchronized
    fun reset() {
        Log.i(TAG, "Liberando motor LiteRT")
        val previous = llmInference
        llmInference = null
        initialized = false
        previous?.close()
    }

    @JvmStatic
    @Synchronized
    fun init(context: Context, modelPath: String) {
        if (initialized) return
        
        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            initialized = true
            Log.i(TAG, "Motor LiteRT inicializado con éxito: $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando LiteRT LLM: ${e.message}", e)
            initialized = false
            throw IllegalStateException("No se pudo cargar el modelo MediaPipe", e)
        } catch (e: LinkageError) {
            initialized = false
            throw IllegalStateException("Runtime MediaPipe incompatible con el dispositivo", e)
        }
    }

    @JvmStatic
    @Synchronized
    fun generate(prompt: String): String {
        if (!initialized || llmInference == null) {
            throw IllegalStateException("El modelo MediaPipe no está inicializado")
        }
        
        return try {
            llmInference!!.generateResponse(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Error en generación LiteRT: ${e.message}")
            throw IllegalStateException("Falló la inferencia MediaPipe", e)
        }
    }
}
