package salve.core

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessageContent
import android.util.Log
import kotlinx.coroutines.runBlocking

/**
 * Pequeño cliente síncrono para el motor MLC.
 *
 * Envuelve las funciones `suspend` del motor en `runBlocking`
 * para poder llamarlo desde Java/Kotlin normal.
 */
object BasicLocalLlm {

    // Engine mutable — permite reintentar creación si la primera vez falla.
    // (by lazy cachea permanentemente los fallos, impidiendo recuperación.)
    private var engine: MLCEngine? = null

    @Volatile
    private var initialized: Boolean = false

    @JvmStatic
    fun isInitialized(): Boolean = initialized

    /**
     * Resetea el estado interno para permitir una re-inicialización completa.
     * Llamar antes de init() cuando se quiera recargar el modelo.
     */
    @JvmStatic
    @Synchronized
    fun reset() {
        Log.i(TAG, "reset() — liberando motor MLC para permitir recarga")
        initialized = false
        val current = engine
        if (current != null) {
            try {
                current.unload()
            } catch (_: Exception) { }
        }
        engine = null
    }

    /**
     * Inicializa el motor con el modelo ya descargado.
     *
     * @param modelPath Ruta al directorio del modelo (la carpeta del modelo en /Android/data/.../files).
     * @param modelLib  Nombre de la librería del modelo (el `model_lib` de mlc-chat-config.json).
     */
    @JvmStatic
    @Synchronized
    fun init(modelPath: String, modelLib: String) {
        if (initialized) return

        runBlocking {
            // Crear engine si no existe (retryable — no cacheamos fallos)
            var engineInstance = engine
            if (engineInstance == null) {
                engineInstance = try {
                    MLCEngine()
                } catch (e: Throwable) {
                    Log.e(TAG, "No se pudo crear MLCEngine. Runtime no disponible.", e)
                    initialized = false
                    throw IllegalStateException("No se pudo cargar el runtime MLC", e)
                }
                engine = engineInstance
            }

            // Por si hubiese algo cargado antes
            try {
                engineInstance.unload()
            } catch (_: Exception) {
                // ignoramos errores al descargar lo anterior
            }

            // Igual que hace AppViewModel.ChatState.mainReloadChat
            try {
                engineInstance.reload(modelPath, modelLib)
                Log.i(TAG, "MLC runtime cargado con modelPath=$modelPath modelLib=$modelLib")
                initialized = true
            } catch (e: Throwable) {
                Log.e(TAG, "No se pudo recargar el modelo MLC. Modelo no disponible.", e)
                initialized = false
                throw IllegalStateException("No se pudo cargar el modelo MLC", e)
            }
        }
    }

    /**
     * Hace una llamada “simple” al chat local y devuelve el texto completo del assistant
     * (sin streaming hacia fuera).
     *
     * @param messages Historial de mensajes en formato OpenAI (role/user/assistant).
     */
    @JvmStatic
    @Synchronized
    fun simpleChat(messages: List<ChatCompletionMessage>): String {
        if (!initialized) {
            throw IllegalStateException("El modelo MLC no está inicializado")
        }

        val currentEngine = engine
            ?: throw IllegalStateException("El motor MLC no está creado")

        return runBlocking {
            val responses = try {
                currentEngine.chat.completions.create(
                    messages = messages,
                    stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                )
            } catch (e: Exception) {
                throw IllegalStateException("Falló la inferencia MLC", e)
            }

            val sb = StringBuilder()
            var finishReasonLength = false

            for (res in responses) {
                for (choice in res.choices) {
                    choice.delta.content?.let { content ->
                        sb.append(content.asText())
                    }
                    choice.finish_reason?.let { finishReason ->
                        if (finishReason == "length") {
                            finishReasonLength = true
                        }
                    }
                }
            }

            if (finishReasonLength) {
                sb.append(" [output truncated due to context length limit...]")
            }

            val finalText = sb.toString()
            if (finalText.isBlank()) {
                Log.w(TAG, "El motor MLC devolvió una respuesta vacía.")
            }
            finalText
        }
    }

    /**
     * Helper para casos sencillos: un solo prompt → una respuesta.
     */
    @JvmStatic
    fun chatSinglePrompt(prompt: String): String {
        val msg = ChatCompletionMessage(
            role = OpenAIProtocol.ChatCompletionRole.user,
            content = ChatCompletionMessageContent(text = prompt)
        )
        return simpleChat(listOf(msg))
    }

    private const val TAG = "BasicLocalLlm"
}
