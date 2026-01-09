package salve.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.progress.ProgressMonitor
import java.io.File
import java.util.Locale

/**
 * Utilidad para descomprimir modelos ZIP usando Zip4j con soporte Zip64.
 */
class ModelZipExtractor(
    private val bufferSize: Int = DEFAULT_BUFFER_SIZE
) {
    suspend fun extract(context: Context, zipFile: File, prefixLower: String): File {
        return withContext(Dispatchers.IO) {
            extractInternal(context, zipFile, prefixLower)
        }
    }

    fun extractWithProgress(context: Context, zipFile: File, prefixLower: String): Flow<ExtractProgress> {
        return channelFlow {
            withContext(Dispatchers.IO) {
                try {
                    validateZip(zipFile)
                    val targetDir = ensureTargetDir(context, prefixLower)
                    if (targetHasContent(targetDir)) {
                        trySend(ExtractProgress.Completed(targetDir))
                        return@withContext
                    }

                    ensureFreeSpace(zipFile, targetDir)

                    ModelConsoleOverlay.show()
                    ModelConsoleOverlay.log("📦 Descomprimiendo…")

                    val zf = ZipFile(zipFile)
                    zf.setBufferSize(bufferSize)
                    zf.setRunInThread(true)
                    val pm = zf.progressMonitor
                    zf.extractAll(targetDir.absolutePath)

                    while (pm.state == ProgressMonitor.State.BUSY) {
                        trySend(ExtractProgress.Progress(pm.percentDone))
                        delay(200)
                    }

                    if (pm.result == ProgressMonitor.Result.ERROR) {
                        throw RuntimeException("Error extrayendo ZIP", pm.exception)
                    }

                    trySend(ExtractProgress.Completed(targetDir))
                    ModelConsoleOverlay.log("✅ Modelo descomprimido: $prefixLower")
                    ModelConsoleOverlay.hideDelayed(1200)
                } catch (e: Exception) {
                    trySend(ExtractProgress.Error(e))
                    ModelConsoleOverlay.log("⚠ Error al descomprimir: ${e.message}")
                    ModelConsoleOverlay.hideDelayed(2500)
                }
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun extractInternal(context: Context, zipFile: File, prefixLower: String): File {
        validateZip(zipFile)

        val targetDir = ensureTargetDir(context, prefixLower)
        if (targetHasContent(targetDir)) {
            return targetDir
        }

        ensureFreeSpace(zipFile, targetDir)

        ModelConsoleOverlay.show()
        ModelConsoleOverlay.log("📦 Descomprimiendo…")

        val zf = ZipFile(zipFile)
        zf.setBufferSize(bufferSize)
        zf.setRunInThread(false)
        return try {
            zf.extractAll(targetDir.absolutePath)
            Log.d(TAG, "Modelo descomprimido en: ${targetDir.absolutePath}")
            ModelConsoleOverlay.log("✅ Modelo descomprimido: $prefixLower")
            ModelConsoleOverlay.hideDelayed(1200)
            targetDir
        } catch (e: ZipException) {
            Log.e(TAG, "Error extrayendo ZIP: ${e.message}", e)
            ModelConsoleOverlay.log("⚠ Error al descomprimir: ${e.message}")
            ModelConsoleOverlay.hideDelayed(2500)
            throw e
        }
    }

    private fun validateZip(zipFile: File?) {
        require(!(zipFile == null || !zipFile.exists())) { "ZIP inválido o inexistente" }
        check(zipFile.length() >= MIN_ZIP_BYTES) { "Archivo ZIP demasiado pequeño: ${zipFile.length()}" }
    }

    private fun ensureTargetDir(context: Context, prefixLower: String): File {
        val modelsDir = ModelStore.dir(context)
        val targetDir = File(modelsDir, prefixLower)
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw RuntimeException("No se pudo crear carpeta de modelo: ${targetDir.absolutePath}")
        }
        return targetDir
    }

    private fun targetHasContent(targetDir: File): Boolean {
        if (!targetDir.exists() || !targetDir.isDirectory) {
            return false
        }
        val existing = targetDir.listFiles()
        return existing != null && existing.isNotEmpty()
    }

    private fun ensureFreeSpace(zipFile: File, targetDir: File) {
        val requirement = StorageSpaceUtil.calculateRequirement(zipFile.length(), targetDir)
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

    sealed class ExtractProgress {
        data class Progress(val percent: Int) : ExtractProgress()
        data class Completed(val targetDir: File) : ExtractProgress()
        data class Error(val error: Exception) : ExtractProgress()
    }

    companion object {
        private const val TAG = "ModelZipExtractor"
        private const val DEFAULT_BUFFER_SIZE = 256 * 1024
        private const val MIN_ZIP_BYTES = 1L shl 20

        @JvmStatic
        @JvmOverloads
        fun extractSync(context: Context, zipFile: File, prefixLower: String, bufferSize: Int = DEFAULT_BUFFER_SIZE): File {
            return kotlinx.coroutines.runBlocking {
                ModelZipExtractor(bufferSize).extract(context, zipFile, prefixLower)
            }
        }
    }
}
