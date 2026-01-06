package ai.mlc.mlcllm

import android.util.Log

/**
 * Implementación ligera y local del motor MLC4J.
 *
 * Esta clase mantiene la API utilizada por BasicLocalLlm sin depender
 * del artefacto remoto no disponible.
 */
class MLCEngine {

    private var currentModelPath: String? = null
    private var currentModelLib: String? = null

    /** Punto de entrada para llamadas de chat. */
    val chat: Chat = Chat()

    /** Descarga cualquier modelo cargado en memoria. */
    @Synchronized
    fun unload() {
        Log.d(TAG, "Descargando modelo local cargado (si existía).")
        currentModelPath = null
        currentModelLib = null
    }

    /**
    * Carga o recarga el modelo indicado.
    * En esta implementación local se almacena la referencia para validar usos posteriores.
    */
    @Synchronized
    fun reload(modelPath: String, modelLib: String) {
        require(modelPath.isNotBlank()) { "modelPath no puede estar vacío" }
        require(modelLib.isNotBlank()) { "modelLib no puede estar vacío" }

        currentModelPath = modelPath
        currentModelLib = modelLib

        Log.d(TAG, "Modelo local preparado en $modelPath con librería $modelLib")
    }

    inner class Chat {
        val completions: Completions = Completions()
    }

    inner class Completions {
        /**
         * Genera una respuesta simulada compatible con la API de streaming esperada.
         */
        fun create(
            messages: List<OpenAIProtocol.ChatCompletionMessage>,
            stream_options: OpenAIProtocol.StreamOptions? = null
        ): Iterable<OpenAIProtocol.ChatCompletionChunk> {
            val responseText = buildResponse(messages, stream_options)
            val chunk = OpenAIProtocol.ChatCompletionChunk(
                choices = listOf(
                    OpenAIProtocol.ChatCompletionChoice(
                        delta = OpenAIProtocol.ChatCompletionChoiceDelta(
                            content = OpenAIProtocol.ChatCompletionMessageContent(text = responseText)
                        ),
                        finish_reason = "stop"
                    )
                )
            )
            return listOf(chunk)
        }
    }

    private fun buildResponse(
        messages: List<OpenAIProtocol.ChatCompletionMessage>,
        stream_options: OpenAIProtocol.StreamOptions?
    ): String {
        val lastUserMessage = messages.lastOrNull { it.role == OpenAIProtocol.ChatCompletionRole.user }
        val userText = lastUserMessage?.content?.asText()?.takeIf { it.isNotBlank() }
        val modelInfo = currentModelLib?.let { "[$it]" } ?: "[modelo no inicializado]"
        val streamInfo = if (stream_options?.include_usage == true) " (stream:usage)" else ""

        return listOfNotNull(modelInfo, userText).joinToString(" ") + streamInfo
    }

    companion object {
        private const val TAG = "MLC4J/StubEngine"
    }
}
