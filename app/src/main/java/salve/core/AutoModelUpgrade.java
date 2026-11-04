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
            // Ya no se descarga el Gemma 4 E4B normal: solo los dos sin censura.
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
