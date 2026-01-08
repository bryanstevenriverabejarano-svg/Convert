package salve.core;

import android.content.Context;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;

/**
 * Utilidad para descomprimir modelos ZIP usando Zip4j con soporte Zip64.
 */
public final class ModelZipExtractor {

    private static final String TAG = "ModelZipExtractor";

    private static final int BUF = 256 * 1024;
    private static final long MIN_ZIP_BYTES = 1L << 20;
    private static final long MIN_MARGIN_BYTES = 50L << 20;
    private static final double MARGIN_RATIO = 0.05;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Listener {
        default void onProgress(int percent) {
        }

        void onComplete(File targetDir);

        void onError(Exception e);
    }

    private ModelZipExtractor() {
    }

    public static File extractSync(Context ctx, File zipFile, String prefixLower) throws Exception {
        validateZip(zipFile);

        File targetDir = ensureTargetDir(ctx, prefixLower);
        if (targetHasContent(targetDir)) {
            return targetDir;
        }

        ensureFreeSpace(zipFile, targetDir);

        ModelConsoleOverlay.show();
        ModelConsoleOverlay.log("📦 Descomprimiendo…");

        ZipFile zf = new ZipFile(zipFile);
        zf.setBufferSize(BUF);
        zf.setRunInThread(false);
        try {
            zf.extractAll(targetDir.getAbsolutePath());
            Log.d(TAG, "Modelo descomprimido en: " + targetDir.getAbsolutePath());
            ModelConsoleOverlay.log("✅ Modelo descomprimido: " + prefixLower);
            ModelConsoleOverlay.hideDelayed(1200);
            return targetDir;
        } catch (ZipException e) {
            Log.e(TAG, "Error extrayendo ZIP: " + e.getMessage(), e);
            ModelConsoleOverlay.log("⚠ Error al descomprimir: " + e.getMessage());
            ModelConsoleOverlay.hideDelayed(2500);
            throw e;
        }
    }

    public static void extractAsync(Context ctx, File zipFile, String prefixLower, Listener cb) {
        EXECUTOR.execute(() -> {
            try {
                validateZip(zipFile);

                File targetDir = ensureTargetDir(ctx, prefixLower);
                if (targetHasContent(targetDir)) {
                    if (cb != null) {
                        cb.onComplete(targetDir);
                    }
                    return;
                }

                ensureFreeSpace(zipFile, targetDir);

                ModelConsoleOverlay.show();
                ModelConsoleOverlay.log("📦 Descomprimiendo…");

                ZipFile zf = new ZipFile(zipFile);
                zf.setBufferSize(BUF);
                zf.setRunInThread(true);
                ProgressMonitor pm = zf.getProgressMonitor();
                zf.extractAll(targetDir.getAbsolutePath());

                while (pm.getState() == ProgressMonitor.State.BUSY) {
                    if (cb != null) {
                        cb.onProgress(pm.getPercentDone());
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (pm.getResult() == ProgressMonitor.Result.ERROR) {
                    throw new RuntimeException("Error extrayendo ZIP", pm.getException());
                }

                if (cb != null) {
                    cb.onComplete(targetDir);
                }
                ModelConsoleOverlay.log("✅ Modelo descomprimido: " + prefixLower);
                ModelConsoleOverlay.hideDelayed(1200);
            } catch (Exception e) {
                if (cb != null) {
                    cb.onError(e);
                }
                ModelConsoleOverlay.log("⚠ Error al descomprimir: " + e.getMessage());
                ModelConsoleOverlay.hideDelayed(2500);
            }
        });
    }

    private static void validateZip(File zipFile) {
        if (zipFile == null || !zipFile.exists()) {
            throw new IllegalArgumentException("ZIP inválido o inexistente");
        }
        if (zipFile.length() < MIN_ZIP_BYTES) {
            throw new IllegalStateException("Archivo ZIP demasiado pequeño: " + zipFile.length());
        }
    }

    private static File ensureTargetDir(Context ctx, String prefixLower) {
        File modelsDir = ModelStore.dir(ctx);
        File targetDir = new File(modelsDir, prefixLower);
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("No se pudo crear carpeta de modelo: " + targetDir.getAbsolutePath());
        }
        return targetDir;
    }

    private static boolean targetHasContent(File targetDir) {
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return false;
        }
        File[] existing = targetDir.listFiles();
        return existing != null && existing.length > 0;
    }

    private static void ensureFreeSpace(File zipFile, File targetDir) {
        long zipSize = zipFile.length();
        long estimatedExtractSize = zipSize * 2;
        long margin = Math.max(MIN_MARGIN_BYTES, (long) (estimatedExtractSize * MARGIN_RATIO));
        long required = estimatedExtractSize + margin;
        long free = getAvailableBytes(targetDir);
        if (free < required) {
            throw new RuntimeException(String.format(Locale.US,
                    "Espacio insuficiente: libre=%d bytes, requerido=%d bytes (+%d margen)",
                    free, estimatedExtractSize, margin));
        }
    }

    private static long getAvailableBytes(File path) {
        try {
            StatFs stat = new StatFs(path.getAbsolutePath());
            return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
        } catch (Throwable t) {
            return Long.MAX_VALUE;
        }
    }
}
