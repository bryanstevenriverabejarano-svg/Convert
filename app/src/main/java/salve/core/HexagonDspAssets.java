package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts the Qualcomm DSP-side Hexagon v75 kernel from the APK's assets.
 * This is NOT an ARM64 JNI library; FastRPC loads it from ADSP_LIBRARY_PATH.
 * A standard APK has no asset and simply uses the existing GPU/CPU route.
 */
final class HexagonDspAssets {
    private static final String TAG = "Salve/Hexagon";
    private static final String NAME = "libggml-htp-v75.so";
    private HexagonDspAssets() { }

    static boolean isPackaged(Context context) {
        try (InputStream ignored = context.getAssets().open("hexagon/" + NAME)) {
            return true;
        } catch (IOException notPackaged) {
            return false;
        }
    }

    static synchronized String prepare(Context context) {
        File dir = new File(context.getNoBackupFilesDir(), "hexagon-v75");
        File finalFile = new File(dir, NAME);
        File staging = new File(dir, NAME + ".part");
        try (InputStream input = context.getAssets().open("hexagon/" + NAME)) {
            if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("No se pudo crear el directorio Hexagon");
            try (FileOutputStream output = new FileOutputStream(staging, false)) {
                byte[] buf = new byte[32768];
                int len;
                while ((len = input.read(buf)) != -1) output.write(buf, 0, len);
                output.getFD().sync();
            }
            if (staging.length() < 4096L) throw new IOException("Biblioteca DSP demasiado pequeña");
            if (finalFile.exists() && !finalFile.delete()) throw new IOException("No se pudo actualizar la biblioteca DSP");
            if (!staging.renameTo(finalFile)) throw new IOException("No se pudo instalar la biblioteca DSP");
            return dir.getAbsolutePath();
        } catch (IOException unavailable) {
            Log.i(TAG, "HTP v75 no incluido o no extraíble; se conserva GPU/CPU: " + unavailable.getMessage());
            return null;
        } finally {
            if (staging.exists()) staging.delete();
        }
    }
}
