package salve.services

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import salve.core.voice.NeuralVoice
import salve.core.voice.SpeechSentences
import java.util.Locale

/**
 * Motor de texto a voz propio de Salve con su voz neuronal. Salve crea su TextToSpeech con este
 * motor (su propio paquete), así que listeners, foco de audio del coche e interrupciones no cambian.
 */
class NeuralTtsService : TextToSpeechService() {
    @Volatile private var stopped = false

    private fun spanish(lang: String?) = lang != null && (lang.equals("spa", true) || lang.equals("es", true))

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int = when {
        !spanish(lang) || !NeuralVoice.ready(this) -> TextToSpeech.LANG_NOT_SUPPORTED
        country.equals("ESP", true) || country.equals("ES", true) -> TextToSpeech.LANG_COUNTRY_AVAILABLE
        else -> TextToSpeech.LANG_AVAILABLE
    }

    override fun onGetLanguage(): Array<String> = arrayOf("spa", "ESP", "")

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
        onIsLanguageAvailable(lang, country, variant)

    override fun onGetVoices(): List<Voice> = if (!NeuralVoice.ready(this)) emptyList() else listOf(
        Voice(NeuralVoice.VOICE_NAME, Locale("es", "ES"), Voice.QUALITY_VERY_HIGH, Voice.LATENCY_NORMAL, false, emptySet()))

    override fun onIsValidVoiceName(voiceName: String?): Int =
        if (voiceName == NeuralVoice.VOICE_NAME && NeuralVoice.ready(this)) TextToSpeech.SUCCESS else TextToSpeech.ERROR

    override fun onLoadVoice(voiceName: String?): Int = onIsValidVoiceName(voiceName)

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? =
        if (spanish(lang)) NeuralVoice.VOICE_NAME else null

    override fun onStop() { stopped = true }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        stopped = false
        val text = request.charSequenceText?.toString().orEmpty()
        val engine = try { NeuralVoice.engine(this) } catch (e: Exception) {
            Log.e(TAG, "La voz neuronal no pudo arrancar", e)
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
            return
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Este APK no trae el motor de la voz neuronal", e)
            callback.error(TextToSpeech.ERROR_SYNTHESIS)
            return
        }
        if (callback.start(engine.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return
        if (text.isBlank()) { callback.done(); return }
        // Android da la velocidad en % (100 = normal); Piper la entiende como factor.
        val speed = (request.speechRate / 100f).coerceIn(0.6f, 1.6f)
        // Salve parte el texto en frases y pide al motor una cada vez: el motor solo devuelve audio y
        // nunca ejecuta código de Salve a mitad de generar. Antes el motor llamaba a Salve por cada
        // trozo y, si algo fallaba ahí, Android cerraba la app entera («JNI NewFloatArray called
        // with pending exception»). Entre frase y frase se comprueba si hay que parar.
        var failure: Throwable? = null
        try {
            var frames = 0
            for (piece in SpeechSentences.split(text)) {
                if (stopped) break
                val audio = NeuralVoice.synthesize(this, piece.text, speed)
                if (stopped) break
                // Qué frase suena ahora: el avatar mueve la boca con ella.
                callback.rangeStart(frames, piece.start, piece.end)
                val pcm = NeuralVoice.toPcm16(audio)
                var offset = 0
                while (offset < pcm.size && !stopped) {
                    val size = minOf(callback.maxBufferSize, pcm.size - offset)
                    if (callback.audioAvailable(pcm, offset, size) != TextToSpeech.SUCCESS) { stopped = true; break }
                    offset += size
                }
                frames += audio.size
            }
        } catch (e: Exception) {
            failure = e
        }
        val error = failure
        if (error == null) {
            if (!stopped) callback.done()
            return
        }
        Log.e(TAG, "Fallo generando la voz", error)
        recordFailure(error)
        try { callback.error(TextToSpeech.ERROR_SYNTHESIS) } catch (ignored: RuntimeException) { }
    }

    /** Deja el fallo de la voz en el registro de ejecución, para saber qué lo provocó. */
    private fun recordFailure(error: Throwable) {
        try {
            val journal = salve.core.trace.ExecutionTrace.journal(this)
            val id = journal.begin("VOZ", "Fallo al generar la voz (Salve sigue abierta)")
            journal.append(id, "ERROR DE LA VOZ", Log.getStackTraceString(error).take(6000))
            journal.finish(id, "FALLO")
        } catch (ignored: RuntimeException) { }
    }

    private companion object { const val TAG = "NeuralTtsService" }
}
