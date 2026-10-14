package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import salve.core.mind.AdapterManifest;

/**
 * Carga el adaptador LoRA personal si existe y corresponde al modelo GGUF activo.
 *
 * <p>Ubicación: {@code Android/data/<paquete>/files/adapters/salve-lora.gguf} y
 * {@code salve-lora.json}. Se copia desde el PC (USB o {@code adb push}) tras entrenarlo con
 * {@code scripts/mind_lora/}. Es la única vía en la que la experiencia de Salve cambia pesos
 * del modelo y no solo el contenido del prompt.</p>
 */
final class PersonalAdapter {
    private static final String TAG = "Salve/Adapter";
    private PersonalAdapter() {}

    static File directory(Context context) {
        File external = context.getExternalFilesDir("adapters");
        return external != null ? external : new File(context.getFilesDir(), "adapters");
    }

    /** @return descripción del resultado (para logs y estado). */
    static String applyIfPresent(Context context, String modelPath) {
        File dir = directory(context);
        File adapter = new File(dir, "salve-lora.gguf");
        File manifestFile = new File(dir, "salve-lora.json");
        if (!adapter.isFile() || !manifestFile.isFile()) return "Sin adaptador personal.";
        try {
            AdapterManifest manifest = AdapterManifest.parse(
                    new String(Files.readAllBytes(manifestFile.toPath()), StandardCharsets.UTF_8));
            String rejection = manifest.rejectionFor(modelPath, adapter);
            if (rejection != null) { Log.w(TAG, rejection); return "Adaptador no aplicado: " + rejection; }
            boolean applied = GgufLlm.applyAdapter(adapter.getAbsolutePath(), manifest.scale);
            String result = applied ? "Adaptador personal activo (escala " + manifest.scale
                    + (manifest.trainedOn == null ? "" : ", entrenado " + manifest.trainedOn) + ")."
                    : "llama.cpp rechazó el adaptador; el modelo base sigue activo.";
            Log.i(TAG, result);
            return result;
        } catch (RuntimeException failure) {
            Log.w(TAG, "Adaptador ignorado: " + failure.getClass().getSimpleName());
            return "Adaptador ignorado: descripción inválida.";
        } catch (Exception failure) {
            return "Adaptador ignorado: no se pudo leer.";
        }
    }
}
