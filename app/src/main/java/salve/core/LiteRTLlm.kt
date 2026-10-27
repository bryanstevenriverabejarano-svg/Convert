package salve.core

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
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
    /**
     * Decodificación especulativa activa. Con ella NO se usa el formato JSON forzado: LiteRT-LM 0.17/0.18
     * rompe la salida cuando se combinan (issue google-ai-edge/LiteRT-LM#3944). El JSON se pide en el
     * prompt y lo valida CognitiveTurn, que ya repara o recupera el texto si llega mal.
     */
    @Volatile @JvmStatic var speculativeActive = false
        private set

    /**
     * Pensar antes de responder (modo pensamiento de Gemma 4) en las llamadas de ESTE hilo. Lo activa
     * el motor solo para preguntas difíciles; lo pensado va al canal «thought» y nunca al texto público.
     */
    private val thinkingHere = ThreadLocal<Boolean>()

    @JvmStatic fun setThinking(enabled: Boolean) { if (enabled) thinkingHere.set(true) else thinkingHere.remove() }
    @JvmStatic fun isThinking(): Boolean = thinkingHere.get() == true

    @OptIn(ExperimentalApi::class)
    private fun setSpeculative(enabled: Boolean): Boolean = try {
        ExperimentalFlags.enableSpeculativeDecoding = enabled
        speculativeActive = enabled
        true
    } catch (unsupported: Throwable) {
        if (unsupported is VirtualMachineError) throw unsupported
        speculativeActive = false
        false
    }

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
        speculativeActive = false
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
            fun loadAll(): Throwable? {
                tokens = maxTokens
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
                return lastError
            }
            // Decodificación especulativa (MTP de Gemma 4): ~1,6× en E2B y ~2,2× en E4B según Google.
            // Si el motor no la admite con este modelo o dispositivo, se carga igual que antes sin ella.
            val mtp = setSpeculative(true)
            var lastError = loadAll()
            if (lastError != null && mtp) {
                Log.w(TAG, "Decodificación especulativa no disponible; se carga sin ella", lastError)
                setSpeculative(false)
                lastError = loadAll()
            }
            if (speculativeActive) backendName += " · MTP"
            lastError?.let { throw it }
            contextTokens = tokens
        }
        loadedPath = modelPath
        visionEnabled = withVision
        initialized = true
        Log.i(TAG, "runtime=$backendName vision=$visionEnabled context=$contextTokens")
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generate(prompt: String, structured: Boolean = false, repair: Boolean = false, creative: Boolean = false): String =
        generateStreaming(prompt, structured, repair, creative, null)

    /**
     * Igual que [generate], pero entrega cada fragmento según llega (palabra a palabra) a
     * [onChunk]. Devuelve el texto completo. El motor .task antiguo no emite flujo: se entrega
     * entero al final.
     */
    @JvmStatic @Synchronized
    fun generateStreaming(prompt: String, structured: Boolean, repair: Boolean, creative: Boolean,
                          onChunk: java.util.function.Consumer<String>?): String {
        check(initialized) { "El modelo local no está inicializado" }
        legacy?.let { model -> return model.generateResponse(prompt).also { onChunk?.accept(it) } }
        return respond(Contents.of(prompt), structured, repair, creative && !structured, onChunk)
    }

    @JvmStatic @JvmOverloads @Synchronized
    fun generateImage(prompt: String, jpeg: ByteArray, structured: Boolean = false, repair: Boolean = false): String {
        check(initialized && visionEnabled && engine != null) { "No hay un motor visual local cargado" }
        return respond(Contents.of(Content.ImageBytes(jpeg), Content.Text(prompt)), structured, repair, false, null)
    }

    /** Read text only. Private channels and tool arguments never enter chat or TTS. */
    /** Si el pensamiento llegara mezclado con el texto (sin canal), se quita: lo pensado no se dice. */
    @JvmStatic
    fun stripThoughts(text: String): String = text
        .replace(Regex("(?s)<think>.*?</think>"), "")
        .replace(Regex("(?s)<\\|channel>thought.*?<channel\\|>"), "")

    @JvmStatic
    fun publicText(message: Message): String = message.contents.contents
        .filterIsInstance<Content.Text>().joinToString("") { it.text }

    /** Salida máxima de un texto creativo libre (cuento, escena): ~350 palabras caben con holgura. */
    const val CREATIVE_OUTPUT_TOKENS = 1024

    // Kept independently testable: both initialization AND each send must opt into constraints.
    // Creative text runs without the JSON grammar (it may use paragraphs) and with more room and
    // temperature; a 512-token JSON cut a bedtime story in half (15/10/2026).
    @JvmStatic @JvmOverloads
    fun conversationConfig(structured: Boolean, repair: Boolean = false, creative: Boolean = false,
                           speculative: Boolean = speculativeActive, thinking: Boolean = isThinking()): ConversationConfig =
        conversationConfigFor(structured, repair, creative, speculative, thinking && structured && !repair)

    /** Pensamiento solo en turnos estructurados (no en reparaciones de formato ni cuentos). */
    @JvmStatic fun thinks(structured: Boolean, repair: Boolean, thinking: Boolean = isThinking()): Boolean =
        thinking && structured && !repair

    /**
     * MTP sí o no en esta conversación (LiteRT-LM 0.18 lo permite por conversación). Las respuestas
     * normales van con MTP y sin JSON forzado (rápidas); la reparación de formato va sin MTP y con el
     * JSON forzado, la combinación que funciona bien: así el fallo #3944 no cuesta respuestas.
     */
    @JvmStatic fun useMtp(speculative: Boolean, repair: Boolean): Boolean = speculative && !repair

    private fun conversationConfigFor(structured: Boolean, repair: Boolean, creative: Boolean, speculative: Boolean,
                                      think: Boolean = false): ConversationConfig {
        val mtp = useMtp(speculative, repair)
        // Pensando, el JSON tampoco se fuerza: la restricción se aplicaría también al pensamiento.
        val constrained = structured && !mtp && !think
        return ConversationConfig(
        automaticToolCalling = false,
        samplerConfig = if (creative && !structured) SamplerConfig(topK = 64, topP = 0.95, temperature = 0.8)
            else SamplerConfig(topK = 40, topP = 0.9, temperature = 0.3),
        maxOutputToken = if (repair) 256 else if (creative && !structured) CREATIVE_OUTPUT_TOKENS
            else if (think) 512 + salve.core.reasoning.HardQuestion.THINKING_BUDGET_TOKENS else 512,
        thinkingConfig = ThinkingConfig(enableThinking = think,
            thinkingTokenBudget = if (think) salve.core.reasoning.HardQuestion.THINKING_BUDGET_TOKENS else -1),
        extraContext = mapOf("enable_thinking" to think), enableResponseFormat = constrained,
        enableSpeculativeDecoding = if (speculative) mtp else null)
    }


    @JvmStatic @JvmOverloads
    fun responseFormat(structured: Boolean, repair: Boolean = false, speculative: Boolean = speculativeActive): ResponseFormat? =
        if (structured && !useMtp(speculative, repair) && !thinks(structured, repair)) ResponseFormat.json(CognitiveOutputConstraints.jsonSchema()) else null

    private fun respond(contents: Contents, structured: Boolean, repair: Boolean, creative: Boolean,
                        onChunk: java.util.function.Consumer<String>?): String {
        val activeEngine = checkNotNull(engine) { "El runtime LiteRT-LM no está cargado" }
        // The application supplies the history; do not keep a second session or execute native tool calls.
        val config = conversationConfig(structured, repair, creative)
        val format = responseFormat(structured, repair)
        activeEngine.createConversation(config).use { conversation ->
            val done = CountDownLatch(1)
            val error = AtomicReference<Throwable?>()
            val text = StringBuffer()
            val lastTokenAt = java.util.concurrent.atomic.AtomicLong(0L)
            val firstTokenAt = java.util.concurrent.atomic.AtomicLong(0L)
            val sentAt = System.currentTimeMillis()
            conversation.sendMessageAsync(contents, object : MessageCallback {
                override fun onMessage(message: Message) {
                    if (message.toolCalls.isNotEmpty()) {
                        error.compareAndSet(null, IllegalStateException("La respuesta requiere una herramienta no conectada"))
                    }
                    val chunk = publicText(message)
                    lastTokenAt.set(System.currentTimeMillis())
                    if (chunk.isEmpty()) return
                    firstTokenAt.compareAndSet(0L, System.currentTimeMillis())
                    text.append(chunk)
                    // A slow UI/TTS consumer must never break generation.
                    try { onChunk?.accept(chunk) } catch (consumer: RuntimeException) { Log.w(TAG, "Fallo al mostrar el flujo", consumer) }
                }
                override fun onDone() { done.countDown() }
                override fun onError(throwable: Throwable) { error.set(throwable); done.countDown() }
            }, responseFormat = format)
            val startedAt = System.currentTimeMillis()
            try {
                // No fixed deadline: wait while tokens keep arriving; stop a stalled engine.
                while (!done.await(1, TimeUnit.SECONDS)) {
                    val verdict = GenerationWatchdog.check(System.currentTimeMillis(), startedAt, lastTokenAt.get(), repair, creative)
                    if (verdict == GenerationWatchdog.Verdict.RUNNING) continue
                    conversation.cancelProcess()
                    if (GenerationWatchdog.keepPartial(structured, text.length)) {
                        Log.w(TAG, "Generación detenida ($verdict); se entrega el texto escrito")
                        return text.toString().trim()
                    }
                    throw java.util.concurrent.TimeoutException("El modelo local dejó de responder ($verdict)")
                }
            } catch (interrupted: InterruptedException) {
                conversation.cancelProcess()
                Thread.currentThread().interrupt()
                throw interrupted
            }
            error.get()?.let { throw IllegalStateException("Falló la generación LiteRT-LM", it) }
            val finished = stripThoughts(text.toString()).trim()
            // Velocidad medida en el propio móvil: lectura del prompt (1ª palabra) y escritura (palabras/s).
            // Cuántos tokens leyó y escribió: dice si la primera palabra tarda por un prompt demasiado largo.
            val tokenCount = try { conversation.getTokenCount() } catch (unavailable: Exception) { -1 }
            val speed = GenerationSpeed.measure(sentAt, firstTokenAt.get(), System.currentTimeMillis(), finished,
                backendName + (if (repair) " · reparación" else "")).withTokens(tokenCount)
            GenerationSpeed.record(speed)
            salve.core.trace.ExecutionTrace.note("VELOCIDAD", speed.describe())
            return finished.also { check(it.isNotEmpty()) { "El modelo no devolvió una respuesta pública" } }
        }
    }
}
