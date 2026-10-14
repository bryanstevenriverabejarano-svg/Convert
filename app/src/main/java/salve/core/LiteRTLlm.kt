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

    @JvmStatic fun isInitialized(): Boolean = initialized
    @JvmStatic fun getBackendName(): String = backendName

    @JvmStatic @Synchronized
    fun reset() {
        initialized = false
        loadedPath = null
        visionEnabled = false
        backendName = "sin cargar"
        val oldEngine = engine
        val oldLegacy = legacy
        engine = null
        legacy = null
        try { oldEngine?.close() } finally { oldLegacy?.close() }
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun init(context: Context, modelPath: String, withVision: Boolean = false) {
        if (initialized && loadedPath == modelPath && (!withVision || visionEnabled)) return
        reset()
        if (modelPath.endsWith(".task", ignoreCase = true)) {
            check(!withVision) { "El modelo .task importado solo admite texto" }
            legacy = LlmInference.createFromOptions(context,
                LlmInference.LlmInferenceOptions.builder().setModelPath(modelPath).setMaxTokens(1024)
                    .setTemperature(0.3f).build())
            backendName = "MediaPipe (.task)"
        } else {
            require(modelPath.endsWith(".litertlm", ignoreCase = true)) { "Formato local no compatible" }
            fun load(backend: Backend): Engine {
                val candidate = Engine(EngineConfig(modelPath = modelPath, backend = backend,
                    visionBackend = if (withVision) Backend.GPU() else null,
                    maxNumTokens = 4096, maxNumImages = 1, cacheDir = context.cacheDir.absolutePath))
                try { candidate.initialize(); return candidate }
                catch (error: Exception) { runCatching { candidate.close() }; throw error }
                catch (error: LinkageError) { runCatching { candidate.close() }; throw error }
            }
            try {
                engine = load(Backend.GPU())
                backendName = "LiteRT-LM GPU"
            } catch (error: Exception) {
                Log.w(TAG, "GPU no disponible; probando CPU", error)
                engine = load(Backend.CPU(threadCount = 4))
                backendName = "LiteRT-LM CPU"
            }
        }
        loadedPath = modelPath
        visionEnabled = withVision
        initialized = true
        Log.i(TAG, "runtime=$backendName vision=$visionEnabled")
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generate(prompt: String, structured: Boolean = false, repair: Boolean = false): String {
        check(initialized) { "El modelo local no está inicializado" }
        legacy?.let { return it.generateResponse(prompt) }
        return respond(Contents.of(prompt), structured, repair)
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generateImage(prompt: String, jpeg: ByteArray, structured: Boolean = false, repair: Boolean = false): String {
        check(initialized && visionEnabled && engine != null) { "No hay un motor visual local cargado" }
        return respond(Contents.of(Content.ImageBytes(jpeg), Content.Text(prompt)), structured, repair)
    }

    /** Read text only. Private channels and tool arguments never enter chat or TTS. */
    @JvmStatic
    fun publicText(message: Message): String = message.contents.contents
        .filterIsInstance<Content.Text>().joinToString("") { it.text }

    // Kept independently testable: both initialization AND each send must opt into constraints.
    @JvmStatic @JvmOverloads
    fun conversationConfig(structured: Boolean, repair: Boolean = false): ConversationConfig = ConversationConfig(
        automaticToolCalling = false,
        samplerConfig = SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3),
        maxOutputToken = if (repair) 256 else 512, thinkingConfig = ThinkingConfig(enableThinking = false),
        extraContext = mapOf("enable_thinking" to false), enableResponseFormat = structured)

    @JvmStatic
    fun responseFormat(structured: Boolean): ResponseFormat? =
        if (structured) ResponseFormat.json(CognitiveOutputConstraints.jsonSchema()) else null

    private fun respond(contents: Contents, structured: Boolean, repair: Boolean): String {
        val activeEngine = checkNotNull(engine) { "El runtime LiteRT-LM no está cargado" }
        // The application supplies the history; do not keep a second session or execute native tool calls.
        val config = conversationConfig(structured, repair)
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
                if (!done.await(if (repair) 30 else 90, TimeUnit.SECONDS)) {
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
