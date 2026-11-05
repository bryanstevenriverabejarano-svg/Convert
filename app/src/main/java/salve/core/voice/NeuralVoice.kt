package salve.core.voice

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Voz neuronal de Salve: una voz de mujer en español de España (Piper «sharvard», hablante F) que
 * se genera en el propio móvil con sherpa-onnx, sin internet. Suena mucho más humana que las voces
 * de Android. El motor y los datos de espeak van en el APK; el modelo (77 MB) se descarga aparte.
 * Salve la usa a través de su propio motor de texto a voz ([salve.services.NeuralTtsService]), así
 * que todo lo demás (coche, interrupciones, avatar) sigue igual.
 */
object NeuralVoice {
    const val ID = "Voz neuronal de Salve"
    const val MODEL_FILE = "vits-piper-es_ES-sharvard-medium-2306b73.onnx"
    const val VOICE_NAME = "salve-neural-es-ES-mujer"
    /** Hablantes del modelo sharvard: {"M": 0, "F": 1}. */
    const val FEMALE_SPEAKER = 1
    private const val DATA_VERSION = "sherpa-1.13.8 sharvard-medium 1"
    private const val PREFS = "salve_prefs"
    private const val ENABLED_KEY = "neural_voice_enabled"
    private const val TAG = "NeuralVoice"

    @Volatile private var engine: OfflineTts? = null
    private val readyListeners = CopyOnWriteArrayList<Runnable>()

    /** ¿Lleva este APK el motor y los datos? (las compilaciones locales sin la CI no). */
    @JvmStatic fun packaged(context: Context): Boolean = try {
        context.assets.open("neural-voice/tokens.txt").close(); true
    } catch (missing: Exception) { false }

    @JvmStatic fun modelFile(context: Context): File = File(salve.core.ModelStore.dir(context), MODEL_FILE)

    /** Lista para hablar: motor en el APK y modelo descargado. */
    @JvmStatic fun ready(context: Context): Boolean = packaged(context) && modelFile(context).isFile

    /** Bryan la pidió: activada por defecto en cuanto esté lista; «usa la voz de Android» la apaga. */
    @JvmStatic fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED_KEY, true)

    @JvmStatic fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED_KEY, on).apply()
        notifyChanged()
    }

    /** Se usará en la próxima frase: lista y activada. */
    @JvmStatic fun active(context: Context): Boolean = enabled(context) && ready(context)

    /** Avisan a Salve para que cambie de motor sin reiniciar la app (descarga terminada, voz cambiada). */
    @JvmStatic fun addChangeListener(listener: Runnable) { readyListeners.add(listener) }
    @JvmStatic fun removeChangeListener(listener: Runnable) { readyListeners.remove(listener) }
    @JvmStatic fun notifyChanged() { for (listener in readyListeners) try { listener.run() } catch (e: RuntimeException) { Log.w(TAG, "Aviso de voz fallido", e) } }

    /** El motor (se crea la primera vez, tarda ~1 s). */
    @JvmStatic @Synchronized fun engine(context: Context): OfflineTts {
        engine?.let { return it }
        val data = prepareData(context)
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = modelFile(context).absolutePath,
                    tokens = File(data, "tokens.txt").absolutePath,
                    dataDir = File(data, "espeak-ng-data").absolutePath,
                ),
                numThreads = 2,
                provider = "cpu",
            ),
        )
        return OfflineTts(config = config).also { engine = it }
    }

    @JvmStatic @Synchronized fun release() {
        engine?.release()
        engine = null
    }

    /** espeak necesita rutas de verdad: se copian una vez de assets a la carpeta de la app. */
    private fun prepareData(context: Context): File {
        val dir = File(context.noBackupFilesDir, "neural-voice")
        val marker = File(dir, "version.txt")
        if (marker.isFile && marker.readText() == DATA_VERSION) return dir
        dir.deleteRecursively()
        dir.mkdirs()
        val assets = context.assets
        val files = assets.open("neural-voice/espeak-files.txt").bufferedReader().readLines().filter { it.isNotBlank() } + "tokens.txt"
        for (name in files) {
            require(!name.contains("..")) { "Ruta de datos de voz inválida" }
            val target = File(dir, name)
            target.parentFile?.mkdirs()
            assets.open("neural-voice/$name").use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        marker.writeText(DATA_VERSION)
        return dir
    }

    /** Puro (pruebas JVM): PCM float [-1, 1] a 16 bits little-endian, recortando lo que se sale. */
    @JvmStatic fun toPcm16(samples: FloatArray): ByteArray {
        val out = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = (samples[i].coerceIn(-1f, 1f) * 32767f).toInt()
            out[2 * i] = (v and 0xff).toByte()
            out[2 * i + 1] = ((v shr 8) and 0xff).toByte()
        }
        return out
    }
}
