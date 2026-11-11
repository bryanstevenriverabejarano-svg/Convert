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

    /**
     * Un adaptador nuevo es un cambio suyo como cualquier otro: se anota una vez en su linaje con la nota
     * del examen de ese momento, y el siguiente examen (también el que hace sola de noche) da el veredicto.
     * Si empeora, se quita el archivo y vuelve el modelo base.
     */
    private static void recordForVerdict(Context context, AdapterManifest manifest) {
        try {
            android.content.SharedPreferences prefs = context.getSharedPreferences("salve_adapter", Context.MODE_PRIVATE);
            if (manifest.sha256 == null || manifest.sha256.equals(prefs.getString("recorded", null))) return;
            com.google.gson.JsonObject exam = salve.core.evaluation.SleepImprovementRuntime.lastExam(context);
            com.google.gson.JsonObject entry = salve.core.selfpr.SelfLineage.entry(System.currentTimeMillis(),
                    salve.core.selfpr.SelfVerification.ADAPTER,
                    "adaptador entrenado" + (manifest.trainedOn == null ? "" : " el " + manifest.trainedOn),
                    "salve-lora.gguf", manifest.sha256,
                    salve.core.selfpr.SelfVerification.ADAPTER + ":" + manifest.sha256.substring(0, Math.min(12, manifest.sha256.length())),
                    salve.core.selfpr.SelfLineage.failedCases(exam));
            entry.addProperty("casos_examen", salve.core.selfpr.SelfLineage.totalCases(exam));
            salve.core.selfpr.SelfLineage.append(salve.core.selfpr.SelfVerification.lineageFile(context), entry);
            prefs.edit().putString("recorded", manifest.sha256).apply();
        } catch (Exception unsaved) { Log.w(TAG, "No pude anotar el adaptador en el linaje"); }
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
            if (applied) recordForVerdict(context, manifest);
            return result;
        } catch (RuntimeException failure) {
            Log.w(TAG, "Adaptador ignorado: " + failure.getClass().getSimpleName());
            return "Adaptador ignorado: descripción inválida.";
        } catch (Exception failure) {
            return "Adaptador ignorado: no se pudo leer.";
        }
    }
}
