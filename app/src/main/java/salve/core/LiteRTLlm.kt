package salve.core

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import com.google.ai.edge.litertlm.ResponseFormat
import salve.core.cognition.CognitiveOutputConstraints
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ThinkingConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** .litertlm uses LiteRT-LM. Existing .task imports retain their MediaPipe runtime. */
object LiteRTLlm {
    private const val TAG = "Salve/LiteRTLlm"
    private var engine: Engine? = null
    private var legacy: LlmInference? = null
    private var loadedPath: String? = null
    private var visionEnabled = false
    @Volatile private var initialized = false
    @Volatile private var backendName = "sin cargar"
    @Volatile private var contextTokens = 4096

    @JvmStatic fun isInitialized(): Boolean = initialized
    @JvmStatic fun getBackendName(): String = backendName
    /** Tokens de contexto con los que se cargó el motor (entrada + imagen + salida). */
    @JvmStatic fun getContextTokens(): Int = contextTokens
    @JvmStatic fun isVisionEnabled(): Boolean = initialized && visionEnabled
    @JvmStatic fun isLoaded(path: String?): Boolean = initialized && path != null && path == loadedPath

    @JvmStatic @Synchronized
    fun reset() {
        initialized = false
        loadedPath = null
        visionEnabled = false
        backendName = "sin cargar"
        contextTokens = 4096
        val oldEngine = engine
        val oldLegacy = legacy
        engine = null
        legacy = null
        try { oldEngine?.close() } finally { oldLegacy?.close() }
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun init(context: Context, modelPath: String, withVision: Boolean = false, maxTokens: Int = 4096) {
        if (initialized && loadedPath == modelPath && (!withVision || visionEnabled) && maxTokens <= contextTokens) return
        reset()
        if (modelPath.endsWith(".task", ignoreCase = true)) {
            check(!withVision) { "El modelo .task importado solo admite texto" }
            legacy = LlmInference.createFromOptions(context,
                // tasks-genai 0.10.33 no ofrece temperatura en LlmInferenceOptions (se configura por
                // sesión); llamarla rompía la compilación de main desde la PR #133.
                LlmInference.LlmInferenceOptions.builder().setModelPath(modelPath).setMaxTokens(1024).build())
            backendName = "MediaPipe (.task)"
        } else {
            require(modelPath.endsWith(".litertlm", ignoreCase = true)) { "Formato local no compatible" }
            var tokens = maxTokens
            fun load(backend: Backend, vision: Backend?): Engine {
                val candidate = Engine(EngineConfig(modelPath = modelPath, backend = backend,
                    visionBackend = vision,
                    maxNumTokens = tokens, maxNumImages = 1, cacheDir = context.cacheDir.absolutePath))
                try { candidate.initialize(); return candidate }
                catch (error: Exception) { runCatching { candidate.close() }; throw error }
                catch (error: LinkageError) { runCatching { candidate.close() }; throw error }
            }
            // La visión por GPU no está disponible en todos los móviles; antes solo se intentaba
            // GPU y la foto fallaba sin alternativa. Se prueba también el codificador visual en CPU.
            val attempts = mutableListOf<Triple<String, () -> Backend, (() -> Backend)?>>(
                Triple("LiteRT-LM GPU", { Backend.GPU() }, if (withVision) ({ Backend.GPU() }) else null))
            if (withVision) attempts += Triple("LiteRT-LM GPU · visión CPU", { Backend.GPU() }, { Backend.CPU(threadCount = 4) })
            attempts += Triple("LiteRT-LM CPU", { Backend.CPU(threadCount = 4) },
                if (withVision) ({ Backend.CPU(threadCount = 4) }) else null)
            var lastError: Throwable? = null
            for ((name, text, vision) in attempts) {
                try {
                    engine = load(text(), vision?.invoke())
                    backendName = name
                    lastError = null
                    break
                } catch (error: Exception) {
                    Log.w(TAG, "$name no disponible", error)
                    lastError = error
                }
            }
            if (lastError != null && tokens > 4096) {
                // Si el contexto ampliado no cabe o el modelo no lo admite, se vuelve a 4.096.
                Log.w(TAG, "Contexto de $tokens tokens no disponible; reintentando con 4096")
                tokens = 4096
                for ((name, text, vision) in attempts) {
                    try {
                        engine = load(text(), vision?.invoke())
                        backendName = name
                        lastError = null
                        break
                    } catch (error: Exception) {
                        Log.w(TAG, "$name no disponible", error)
                        lastError = error
                    }
                }
            }
            lastError?.let { throw it }
            contextTokens = tokens
        }
        loadedPath = modelPath
        visionEnabled = withVision
        initialized = true
        Log.i(TAG, "runtime=$backendName vision=$visionEnabled context=$contextTokens")
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generate(prompt: String, structured: Boolean = false, repair: Boolean = false, creative: Boolean = false): String {
        check(initialized) { "El modelo local no está inicializado" }
        legacy?.let { return it.generateResponse(prompt) }
        return respond(Contents.of(prompt), structured, repair, creative && !structured)
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generateImage(prompt: String, jpeg: ByteArray, structured: Boolean = false, repair: Boolean = false): String {
        check(initialized && visionEnabled && engine != null) { "No hay un motor visual local cargado" }
        return respond(Contents.of(Content.ImageBytes(jpeg), Content.Text(prompt)), structured, repair, false)
    }

    /** Read text only. Private channels and tool arguments never enter chat or TTS. */
    @JvmStatic
    fun publicText(message: Message): String = message.contents.contents
        .filterIsInstance<Content.Text>().joinToString("") { it.text }

    /** Salida máxima de un texto creativo libre (cuento, escena): ~350 palabras caben con holgura. */
    const val CREATIVE_OUTPUT_TOKENS = 1024

    // Kept independently testable: both initialization AND each send must opt into constraints.
    // Creative text runs without the JSON grammar (it may use paragraphs) and with more room and
    // temperature; a 512-token JSON cut a bedtime story in half (15/10/2026).
    @JvmStatic @JvmOverloads
    fun conversationConfig(structured: Boolean, repair: Boolean = false, creative: Boolean = false): ConversationConfig = ConversationConfig(
        automaticToolCalling = false,
        samplerConfig = if (creative && !structured) SamplerConfig(topK = 64, topP = 0.95, temperature = 0.8)
            else SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3),
        maxOutputToken = if (repair) 256 else if (creative && !structured) CREATIVE_OUTPUT_TOKENS else 512,
        thinkingConfig = ThinkingConfig(enableThinking = false),
        extraContext = mapOf("enable_thinking" to false), enableResponseFormat = structured)

    /** Segundos de espera: un cuento de 1024 tokens necesita más que una respuesta breve. */
    @JvmStatic
    fun timeoutSeconds(repair: Boolean, creative: Boolean): Long = if (repair) 30 else if (creative) 180 else 90

    @JvmStatic
    fun responseFormat(structured: Boolean): ResponseFormat? =
        if (structured) ResponseFormat.json(CognitiveOutputConstraints.jsonSchema()) else null

    private fun respond(contents: Contents, structured: Boolean, repair: Boolean, creative: Boolean): String {
        val activeEngine = checkNotNull(engine) { "El runtime LiteRT-LM no está cargado" }
        // The application supplies the history; do not keep a second session or execute native tool calls.
        val config = conversationConfig(structured, repair, creative)
        val format = responseFormat(structured)
        activeEngine.createConversation(config).use { conversation ->
            val done = CountDownLatch(1)
            val error = AtomicReference<Throwable?>()
            val text = StringBuffer()
            conversation.sendMessageAsync(contents, object : MessageCallback {
                override fun onMessage(message: Message) {
                    if (message.toolCalls.isNotEmpty()) {
                        error.compareAndSet(null, IllegalStateException("La respuesta requiere una herramienta no conectada"))
                    }
                    text.append(publicText(message))
                }
                override fun onDone() { done.countDown() }
                override fun onError(throwable: Throwable) { error.set(throwable); done.countDown() }
            }, responseFormat = format)
            try {
                if (!done.await(timeoutSeconds(repair, creative), TimeUnit.SECONDS)) {
                    conversation.cancelProcess()
                    throw java.util.concurrent.TimeoutException("El modelo local agotó el tiempo de respuesta")
                }
            } catch (interrupted: InterruptedException) {
                conversation.cancelProcess()
                Thread.currentThread().interrupt()
                throw interrupted
            }
            error.get()?.let { throw IllegalStateException("Falló la generación LiteRT-LM", it) }
            return text.toString().trim().also { check(it.isNotEmpty()) { "El modelo no devolvió una respuesta pública" } }
        }
    }
}
