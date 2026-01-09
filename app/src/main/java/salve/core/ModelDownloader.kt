package salve.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * ModelDownloader descarga modelos definidos en `assets/config/models.json` a:
 *   `/Android/data/<pkg>/files/models/<filename>`.
 *
 * La clase soporta reanudación mediante encabezados Range, redirecciones 30x,
 * verificación de checksum SHA‑256 en streaming, y comprobación de espacio libre
 * antes de iniciar la descarga. Además, emite eventos de progreso a través de un
 * [kotlinx.coroutines.flow.Flow] para que la interfaz pueda reaccionar en
 * tiempo real.
 */
class ModelDownloader(
    private val client: OkHttpClient = NetworkModule.provideOkHttpClient(),
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {

    /**
     * Descarga todos los modelos indicados en el JSON proporcionado. Devuelve
     * un [Flow] que emite eventos de tipo [DownloadEvent] durante el proceso.
     *
     * @param context Contexto de Android utilizado para resolver rutas.
     * @param jsonStream Flujo de entrada del fichero JSON de configuración.
     */
    fun downloadAll(context: Context, jsonStream: InputStream): Flow<DownloadEvent> =
        flow {
            val items = loadItems(jsonStream)
            val total = items.size

            // Asegura que la carpeta de modelos exista antes de empezar.
            val base = modelsDir(context)
            if (!base.exists() && !base.mkdirs()) {
                throw RuntimeException("No se pudo crear carpeta: ${base.absolutePath}")
            }

            // Recorre cada elemento y descarga si fuera necesario.
            for ((index, item) in items.withIndex()) {
                // Notifica inicio de la descarga de un modelo.
                emit(DownloadEvent.Started(item.id, index + 1, total))
                try {
                    // Llama a downloadOne y repropaga eventos de progreso.
                    val file = downloadOne(context, item, index + 1, total) { event ->
                        emit(event)
                    }
                    // Notifica que el modelo actual se ha descargado completamente.
                    emit(DownloadEvent.Completed(item.id, file))
                } catch (e: Exception) {
                    // Notifica error en la descarga de este modelo.
                    emit(DownloadEvent.Error(item.id, e))
                }
            }
            // Notifica que todas las descargas han terminado.
            emit(DownloadEvent.AllDone)
        }.flowOn(Dispatchers.IO)

    /**
     * Verifica de forma síncrona si faltan archivos por descargar o si están
     * corruptos según el SHA‑256. Devuelve `true` si detecta que falta
     * alguno, y `false` cuando todo está presente y válido. Se expone
     * públicamente una versión estática para compatibilidad con código Java.
     */
    suspend fun precheckAll(context: Context, jsonStream: InputStream): Boolean {
        return try {
            val items = loadItems(jsonStream)
            for (item in items) {
                val out = outputFor(context, item.filename)
                // Considera que falta si no existe o es demasiado pequeño.
                if (!out.exists() || out.length() <= MIN_FILE_BYTES) return true
                // Si se especifica checksum, verifica que coincida.
                if (!item.sha256.isNullOrBlank()) {
                    val got = sha256Hex(out)
                    if (!equalsHex(item.sha256, got)) return true
                } else if (item.sizeBytes > 0 && out.length() != item.sizeBytes) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.w(TAG, "precheckAll error: ${e.message}")
            true
        }
    }

    /**
     * Descarga un único archivo de modelo. Emite eventos de progreso a través
     * del callback proporcionado. Devuelve el archivo final una vez
     * completada la descarga y validación del checksum.
     *
     * @param context Contexto de Android utilizado para resolver rutas.
     * @param item Información del modelo a descargar.
     * @param index Índice del modelo actual (para eventos de progreso).
     * @param total Número total de modelos (para eventos de progreso).
     * @param emit Callback de emisión de eventos de progreso.
     */
    private suspend fun downloadOne(
        context: Context,
        item: ModelItem,
        index: Int,
        total: Int,
        emit: suspend (DownloadEvent) -> Unit
    ): File {
        val out = outputFor(context, item.filename)
        val expectedSize = item.sizeBytes

        // Si existe y parece válido, omite la descarga.
        if (out.exists() && out.length() > MIN_FILE_BYTES) {
            if (!item.sha256.isNullOrBlank()) {
                val got = sha256Hex(out)
                if (equalsHex(item.sha256, got)) {
                    return out
                }
            } else if (expectedSize > 0 && out.length() == expectedSize) {
                return out
            }
        }

        // Comprueba espacio libre antes de descargar.
        ensureFreeSpace(context, expectedSize, out)

        val existing = if (out.exists()) out.length() else 0L
        val requestBuilder = Request.Builder().url(item.url)
        if (existing > 0) {
            requestBuilder.header("Range", "bytes=$existing-")
        }
        val request = requestBuilder.build()

        client.newCall(request).execute().use { response ->
            // Si el servidor responde 416 (rango no válido), reinicia.
            if (response.code == HTTP_REQUESTED_RANGE_NOT_SATISFIABLE) {
                if (out.exists()) {
                    out.delete()
                }
                return downloadOne(context, item.copy(), index, total, emit)
            }
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP ${response.code} al descargar ${item.id}")
            }

            val body = response.body ?: throw RuntimeException("Respuesta sin cuerpo")
            val totalBytes = computeTotalBytes(existing, body, response)
            val digestBody = body as? DigestingResponseBody
            val buffer = ByteArray(bufferSize)
            var written = existing
            val startTime = System.nanoTime()

            body.source().use { source ->
                val output = if (existing > 0) java.io.FileOutputStream(out, true) else java.io.FileOutputStream(out)
                output.buffered(bufferSize).use { sink ->
                    while (true) {
                        val read = source.read(buffer)
                        if (read == -1) break
                        sink.write(buffer, 0, read)
                        written += read
                        val elapsedSec = max(1e-6, (System.nanoTime() - startTime) / 1_000_000_000.0)
                        val mbps = (written / (1024.0 * 1024.0)) / elapsedSec
                        emit(
                            DownloadEvent.Progress(
                                id = item.id,
                                index = index,
                                total = total,
                                bytes = written,
                                totalBytes = totalBytes,
                                mbPerSec = mbps
                            )
                        )
                    }
                }
            }

            val digest = digestBody?.digestHex
            if (!item.sha256.isNullOrBlank() && digest != null && !equalsHex(item.sha256, digest)) {
                if (DELETE_ON_BAD_CHECKSUM) {
                    out.delete()
                }
                throw RuntimeException("Checksum inválido para ${item.id}")
            }
            return out
        }
    }

    /**
     * Calcula el tamaño total esperado de la descarga teniendo en cuenta un
     * posible reanudado (código 206) o un cuerpo de longitud desconocida.
     */
    private fun computeTotalBytes(existing: Long, body: ResponseBody, response: Response): Long {
        val length = body.contentLength()
        return if (response.code == 206 && length > 0) existing + length else length
    }

    /**
     * Verifica si hay espacio suficiente para descargar el archivo. Lanza
     * RuntimeException si no se cumple.
     */
    private fun ensureFreeSpace(context: Context, expectedSize: Long, out: File) {
        val sizeToCheck = if (expectedSize > 0) expectedSize else out.length()
        val requirement = StorageSpaceUtil.calculateRequirement(sizeToCheck, out.parentFile ?: modelsDir(context))
        if (requirement.availableBytes < requirement.requiredBytes) {
            throw RuntimeException(
                String.format(
                    Locale.US,
                    "Espacio insuficiente: libre=%d bytes, requerido=%d bytes (+%d margen)",
                    requirement.availableBytes,
                    requirement.estimatedExtractBytes,
                    requirement.marginBytes
                )
            )
        }
    }

    /**
     * Analiza el JSON de configuración y devuelve una lista de [ModelItem].
     */
    private fun loadItems(jsonStream: InputStream): List<ModelItem> {
        val raw = jsonStream.readBytes()
        val root = JSONObject(String(raw))
        val items = root.getJSONArray("items")
        return (0 until items.length()).map { idx ->
            val it = items.getJSONObject(idx)
            val id = it.getString("id")
            val url = it.getString("url")
            val filename = it.optString("filename").takeIf { name -> name.isNotBlank() }
                ?: fileNameFromUrl(url, id)
            val sizeBytes = it.optLong("sizeBytes", -1L)
            val sha256 = optStringOrNull(it, "sha256")
            ModelItem(id, url, filename, sizeBytes, sha256)
        }
    }

    /**
     * Obtiene una cadena opcional de un [JSONObject] si existe.
     */
    private fun optStringOrNull(obj: JSONObject, key: String): String? {
        return if (obj.has(key)) obj.optString(key, null) else null
    }

    /**
     * Devuelve el directorio donde se almacenan los modelos.
     */
    private fun modelsDir(context: Context): File = ModelStore.dir(context)

    /**
     * Devuelve el archivo de salida para un nombre de fichero dado.
     */
    private fun outputFor(context: Context, fileNameFromUrl: String): File {
        return File(modelsDir(context), fileNameFromUrl)
    }

    /**
     * Extrae un nombre de fichero a partir de una URL. Si la URL no contiene
     * nombre de archivo, usa el `fallbackId` con extensión `.zip`.
     */
    private fun fileNameFromUrl(url: String, fallbackId: String): String {
        return try {
            val path = URL(url).path
            val name = path.substringAfterLast('/')
            if (name.isBlank()) "$fallbackId.zip" else name
        } catch (e: Exception) {
            "$fallbackId.zip"
        }
    }

    /**
     * Calcula el SHA‑256 de un archivo y lo devuelve en hexadecimal.
     */
    private fun sha256Hex(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(bufferSize)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Compara dos cadenas hexadecimales ignorando mayúsculas y espacios.
     */
    private fun equalsHex(expected: String, actual: String): Boolean {
        return expected.trim().equals(actual.trim(), ignoreCase = true)
    }

    /**
     * Información de un modelo: id, URL, nombre de archivo, tamaño y checksum opcional.
     */
    data class ModelItem(
        val id: String,
        val url: String,
        val filename: String,
        val sizeBytes: Long,
        val sha256: String?
    )

    /**
     * Representa los distintos eventos que se pueden producir durante la
     * descarga de modelos.
     */
    sealed class DownloadEvent {
        /** Inicio de la descarga de un modelo. */
        data class Started(val id: String, val index: Int, val total: Int) : DownloadEvent()
        /** Progreso de descarga. */
        data class Progress(
            val id: String,
            val index: Int,
            val total: Int,
            val bytes: Long,
            val totalBytes: Long,
            val mbPerSec: Double
        ) : DownloadEvent()
        /** Descarga completada de un modelo. */
        data class Completed(val id: String, val file: File) : DownloadEvent()
        /** Error durante la descarga. */
        data class Error(val id: String, val error: Exception) : DownloadEvent()
        /** Todas las descargas han finalizado. */
        data object AllDone : DownloadEvent()
    }

    companion object {
        private const val TAG = "ModelDownloader"
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024
        private const val MIN_FILE_BYTES = 1L shl 20
        private const val HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416
        private const val DELETE_ON_BAD_CHECKSUM = true

        /**
         * Versión estática de [precheckAll] para compatibilidad con código Java.
         */
        @JvmStatic
        fun precheckAll(context: Context, jsonStream: InputStream): Boolean {
            return runBlocking { ModelDownloader().precheckAll(context, jsonStream) }
        }
    }
}

/**
 * Proporciona instancias de [OkHttpClient] con configuración predeterminada.
 */
object NetworkModule {
    private const val CONNECT_TIMEOUT_MS = 25_000L
    private const val READ_TIMEOUT_MS = 25_000L

    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(ChecksumInterceptor())
            .build()
    }
}

/**
 * Interceptor que calcula el SHA‑256 mientras se lee el cuerpo de la respuesta.
 */
private class ChecksumInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        val body = response.body ?: return response
        val digest = MessageDigest.getInstance("SHA-256")
        val digestingBody = DigestingResponseBody(body, digest)
        return response.newBuilder().body(digestingBody).build()
    }
}

/**
 * Cuerpo de respuesta que calcula el digest del contenido a medida que se lee.
 */
private class DigestingResponseBody(
    private val delegate: ResponseBody,
    private val digest: MessageDigest
) : ResponseBody() {
    private val bufferedSource by lazy { source(delegate.source()).buffer() }
    var digestHex: String? = null
        private set

    override fun contentType() = delegate.contentType()
    override fun contentLength() = delegate.contentLength()
    override fun source(): okio.BufferedSource = bufferedSource

    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            override fun read(sink: Buffer, byteCount: Long): Long {
                val read = super.read(sink, byteCount)
                if (read > 0) {
                    val buffer = sink.clone()
                    val bytes = buffer.readByteArray(read)
                    digest.update(bytes)
                }
                if (read == -1L) {
                    digestHex = digest.digest().joinToString("") { "%02x".format(it) }
                }
                return read
            }
        }
    }
}