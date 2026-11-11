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
import salve.core.voice.VoicePlaybackJournal
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Motor de texto a voz propio de Salve con su voz neuronal. Salve crea su TextToSpeech con este
 * motor (su propio paquete), así que listeners, foco de audio del coche e interrupciones no cambian.
 */
class NeuralTtsService : TextToSpeechService() {
    @Volatile private var stopped = false
    @Volatile private var currentTrace: String? = null
    private val utteranceCounter = AtomicLong(0L)

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

    override fun onStop() {
        stopped = true
        currentTrace?.let { VoicePlaybackJournal.event(it, "ENGINE_STOP_REQUESTED", 0) }
    }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        val text = request.charSequenceText?.toString().orEmpty()
        val trace = "neural_" + utteranceCounter.incrementAndGet()
        currentTrace = trace
        stopped = false
        VoicePlaybackJournal.begin(this, trace, "síntesis neuronal", text.length)
        try {
            val engine = try {
                NeuralVoice.engine(this)
            } catch (e: Exception) {
                Log.e(TAG, "La voz neuronal no pudo arrancar", e)
                VoicePlaybackJournal.event(trace, "ENGINE_START_ERROR", 0)
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
                VoicePlaybackJournal.finish(trace, "ERROR")
                return
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Este APK no trae el motor de la voz neuronal", e)
                VoicePlaybackJournal.event(trace, "ENGINE_LINK_ERROR", 0)
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
                VoicePlaybackJournal.finish(trace, "ERROR")
                return
            }
            val result = callback.start(engine.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1)
            if (result != TextToSpeech.SUCCESS) {
                VoicePlaybackJournal.event(trace, "CALLBACK_START_REJECTED", result.toLong())
                VoicePlaybackJournal.finish(trace, "ERROR")
                return
            }
            VoicePlaybackJournal.event(trace, "CALLBACK_STARTED", engine.sampleRate().toLong())
            if (text.isBlank()) {
                val end = callback.done()
                VoicePlaybackJournal.event(trace, "CALLBACK_DONE", end.toLong())
                VoicePlaybackJournal.finish(trace, if (end == TextToSpeech.SUCCESS) "COMPLETADA" else "ERROR")
                return
            }
            val speed = (request.speechRate / 100f).coerceIn(0.6f, 1.6f)
            var failure: Throwable? = null
            var rejectedOutput: Int? = null
            var frames = 0
            var piecesDone = 0
            try {
                for (piece in SpeechSentences.split(text)) {
                    if (stopped) break
                    VoicePlaybackJournal.event(trace, "PIECE_STARTED", piecesDone.toLong())
                    val audio = NeuralVoice.synthesize(this, piece.text, speed)
                    if (stopped) break
                    callback.rangeStart(frames, piece.start, piece.end)
                    val pcm = NeuralVoice.toPcm16(audio)
                    var offset = 0
                    while (offset < pcm.size && !stopped) {
                        val size = minOf(callback.maxBufferSize, pcm.size - offset)
                        if (size <= 0) {
                            rejectedOutput = TextToSpeech.ERROR_OUTPUT
                            break
                        }
                        val accepted = callback.audioAvailable(pcm, offset, size)
                        if (accepted != TextToSpeech.SUCCESS) {
                            rejectedOutput = accepted
                            break
                        }
                        offset += size
                    }
                    if (rejectedOutput != null || stopped) break
                    frames += audio.size
                    piecesDone++
                    VoicePlaybackJournal.event(trace, "PIECE_WRITTEN", frames.toLong())
                }
            } catch (e: Exception) {
                failure = e
            } catch (e: LinkageError) {
                failure = e
            }
            when {
                stopped -> {
                    VoicePlaybackJournal.event(trace, "SYNTH_INTERRUPTED", piecesDone.toLong())
                    VoicePlaybackJournal.finish(trace, "INTERRUMPIDA")
                }
                rejectedOutput != null -> {
                    // Before: silently returned without callback.done() OR callback.error().
                    // Android could leave the chat text complete but cut the audio abruptly.
                    VoicePlaybackJournal.event(trace, "OUTPUT_BUFFER_REJECTED", rejectedOutput!!.toLong())
                    try { callback.error(TextToSpeech.ERROR_OUTPUT) }
                    catch (ignored: RuntimeException) { }
                    VoicePlaybackJournal.finish(trace, "ERROR")
                }
                failure != null -> {
                    Log.e(TAG, "Fallo generando la voz", failure)
                    VoicePlaybackJournal.event(trace, "SYNTH_EXCEPTION", piecesDone.toLong())
                    recordFailure(failure)
                    try { callback.error(TextToSpeech.ERROR_SYNTHESIS) }
                    catch (ignored: RuntimeException) { }
                    VoicePlaybackJournal.finish(trace, "ERROR")
                }
                else -> {
                    val end = callback.done()
                    VoicePlaybackJournal.event(trace, "CALLBACK_DONE", end.toLong())
                    VoicePlaybackJournal.finish(trace, if (end == TextToSpeech.SUCCESS) "COMPLETADA" else "ERROR")
                }
            }
        } finally {
            if (currentTrace == trace) currentTrace = null
        }
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
