package salve.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * En móviles con RAM de sobra (10 GiB o más, p. ej. Galaxy S24 Ultra), Salve descarga y activa sola,
 * una vez y con Wi-Fi, Gemma 4 E4B sin censura. Si no carga o no responde, se conserva el modelo
 * anterior. El Gemma 4 E4B normal ya no se descarga: solo los dos sin censura (E2B y E4B).
 */
public final class AutoModelUpgrade {
    static final long GIB = 1024L * 1024L * 1024L;
    /** Un móvil de 12 GB anuncia unos 10,8 GiB; uno de 8 GB, unos 7,3. */
    static final long MIN_TOTAL_RAM = 10L * GIB;
    static final String PREFS = "salve_prefs";
    static final String ATTEMPTED_KEY = "auto_e4b_attempted";
    static final String DECLINED_KEY = "auto_e4b_declined";
    /** Una sola vez: Bryan pidió que el modelo sin censura se descargue y active solo. */
    static final String UNCENSORED_KEY = "auto_uncensored_attempted";
    static final String VISION_KEY = "auto_uncensored_vision_requested_at";
    static final String VOICE_KEY = "auto_neural_voice_requested_at";

    private AutoModelUpgrade() { }

    /** ¿Toca descargar y activar Gemma 4 E4B sin censura sola? Una vez, en móviles con RAM de sobra. */
    static boolean shouldGetUncensored(long totalRamBytes, String activeModelId, boolean attempted) {
        return totalRamBytes >= MIN_TOTAL_RAM && !LocalModelPolicy.isUncensored(activeModelId) && !attempted;
    }

    /** Llamar al arrancar y tras preparar el modelo por defecto. No hace nada si no procede. */
    public static void maybeUpgrade(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String active = LocalModelPolicy.idForPath(SalveLLM.getInstance(context).activeModelPath());
            if (shouldGetUncensored(totalRam(context), active, prefs.getBoolean(UNCENSORED_KEY, false))) {
                prefs.edit().putBoolean(UNCENSORED_KEY, true).putBoolean(ATTEMPTED_KEY, true).apply();
                salve.work.ModelDownloadWorker.enqueueOptionalAfterCurrent(context, LocalModelPolicy.UNCENSORED);
            }
            // Voz neuronal (Bryan la pidió): se descarga sola si el APK trae el motor y no la ha desactivado.
            if (salve.core.voice.NeuralVoice.packaged(context) && salve.core.voice.NeuralVoice.enabled(context)
                    && !salve.core.voice.NeuralVoice.modelFile(context).isFile()
                    && System.currentTimeMillis() - prefs.getLong(VOICE_KEY, 0L) > 12L * 60 * 60 * 1000) {
                prefs.edit().putLong(VOICE_KEY, System.currentTimeMillis()).apply();
                salve.work.ModelDownloadWorker.enqueueOptionalAfterCurrent(context, salve.core.voice.NeuralVoice.ID);
            }
            // Ya no se descarga el Gemma 4 E4B normal: solo los dos sin censura.
            // Modelo sin censura activo pero sin ojos (instalado antes de la visión, o la descarga falló):
            // se pide su visión, como mucho una vez cada 12 horas.
            String vision = LocalModelPolicy.visionFor(active);
            if (vision != null && !new java.io.File(ModelStore.dir(context), LocalModelPolicy.visionFileFor(active)).isFile()
                    && System.currentTimeMillis() - prefs.getLong(VISION_KEY, 0L) > 12L * 60 * 60 * 1000) {
                prefs.edit().putLong(VISION_KEY, System.currentTimeMillis()).apply();
                salve.work.ModelDownloadWorker.enqueueOptionalAfterCurrent(context, vision);
            }
        } catch (RuntimeException unavailable) { /* se intentará en el próximo arranque */ }
    }

    /** Bryan eligió E2B a mano: no se le vuelve a cambiar el modelo por su cuenta. */
    public static void declined(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(DECLINED_KEY, true).apply();
    }

    static long totalRam(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return 0;
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        return info.totalMem;
    }
}
