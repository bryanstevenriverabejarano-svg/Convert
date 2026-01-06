package ai.mlc.mlcllm

/**
 * Estructuras ligeras compatibles con el protocolo OpenAI
 * utilizadas por la integración local de MLC.
 */
object OpenAIProtocol {

    /** Roles permitidos en mensajes de chat. */
    enum class ChatCompletionRole { user, assistant, system }

    /** Contenido del mensaje. */
    data class ChatCompletionMessageContent(val text: String? = null) {
        fun asText(): String = text.orEmpty()
    }

    /** Mensaje individual de chat. */
    data class ChatCompletionMessage(
        val role: ChatCompletionRole,
        val content: ChatCompletionMessageContent
    )

    /** Opciones de streaming. */
    data class StreamOptions(val include_usage: Boolean = false)

    /** Delta incremental de una respuesta. */
    data class ChatCompletionChoiceDelta(
        val content: ChatCompletionMessageContent? = null
    )

    /** Opción de respuesta dentro de un chunk. */
    data class ChatCompletionChoice(
        val delta: ChatCompletionChoiceDelta = ChatCompletionChoiceDelta(),
        val finish_reason: String? = null
    )

    /** Chunk de respuesta de chat. */
    data class ChatCompletionChunk(
        val choices: List<ChatCompletionChoice>
    )
}
