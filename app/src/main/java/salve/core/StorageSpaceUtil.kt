package salve.core

import android.os.StatFs
import java.io.File
import kotlin.math.max

/**
 * Utilidad centralizada para calcular espacio libre y márgenes de seguridad.
 *
 * Regla: 2x tamaño del ZIP + margen adicional (50 MB o 5%).
 */
object StorageSpaceUtil {
    private const val MIN_MARGIN_BYTES: Long = 50L shl 20
    private const val MARGIN_RATIO = 0.05

    data class SpaceRequirement(
        val zipSizeBytes: Long,
        val estimatedExtractBytes: Long,
        val marginBytes: Long,
        val requiredBytes: Long,
        val availableBytes: Long
    )

    fun calculateRequirement(zipSizeBytes: Long, targetDir: File): SpaceRequirement {
        val estimatedExtractBytes = zipSizeBytes * 2
        val marginBytes = max(MIN_MARGIN_BYTES, (estimatedExtractBytes * MARGIN_RATIO).toLong())
        val requiredBytes = estimatedExtractBytes + marginBytes
        val availableBytes = getAvailableBytes(targetDir)
        return SpaceRequirement(
            zipSizeBytes = zipSizeBytes,
            estimatedExtractBytes = estimatedExtractBytes,
            marginBytes = marginBytes,
            requiredBytes = requiredBytes,
            availableBytes = availableBytes
        )
    }

    fun hasEnoughSpace(zipSizeBytes: Long, targetDir: File): Boolean {
        val requirement = calculateRequirement(zipSizeBytes, targetDir)
        return requirement.availableBytes >= requirement.requiredBytes
    }

    private fun getAvailableBytes(path: File): Long {
        return try {
            val stat = StatFs(path.absolutePath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (t: Throwable) {
            Long.MAX_VALUE
        }
    }
}
