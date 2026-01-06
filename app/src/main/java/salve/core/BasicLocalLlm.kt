package salve.core

import ai.mlc.mlcllm.MLCEngine
import ai.mlc.mlcllm.OpenAIProtocol
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessage
import ai.mlc.mlcllm.OpenAIProtocol.ChatCompletionMessageContent
import kotlinx.coroutines.runBlocking

/**
 * Pequeño cliente síncrono para el motor MLC.
 *
 * Envuelve las funciones `suspend` del motor en `runBlocking`
 * para poder llamarlo desde Java/Kotlin normal.
 */
object BasicLocalLlm {

    private val engine = MLCEngine()

    @Volatile
    private var initialized: Boolean = false

    @JvmStatic
    fun isInitialized(): Boolean = initialized

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
            // Por si hubiese algo cargado antes
            try {
                engine.unload()
            } catch (_: Exception) {
                // ignoramos errores al descargar lo anterior
            }

            // Igual que hace AppViewModel.ChatState.mainReloadChat
            try {
                engine.reload(modelPath, modelLib)
                initialized = true
            } catch (e: Exception) {
                initialized = false
                throw e
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
    fun simpleChat(messages: List<ChatCompletionMessage>): String {
        if (!initialized) {
            return "El modelo local aún no está inicializado en BasicLocalLlm."
        }

        return runBlocking {
            val responses = try {
                engine.chat.completions.create(
                    messages = messages,
                    stream_options = OpenAIProtocol.StreamOptions(include_usage = true)
                )
            } catch (e: Exception) {
                return@runBlocking "No se pudo generar respuesta con el motor MLC: ${e.message}"
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

            sb.toString()
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
}
