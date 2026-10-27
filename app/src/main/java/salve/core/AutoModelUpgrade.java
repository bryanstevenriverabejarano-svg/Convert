package salve.core;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * En móviles con RAM de sobra (10 GiB o más, p. ej. Galaxy S24 Ultra), Salve pasa sola de Gemma 4 E2B
 * a Gemma 4 E4B: entiende mejor, ve mejor y usa herramientas mejor. Se hace una sola vez, solo con
 * Wi-Fi, y con la misma red de seguridad que el cambio manual: si E4B no carga o no responde, se
 * conserva el modelo anterior. Si Bryan pide volver a E2B, se respeta y no se vuelve a intentar.
 */
public final class AutoModelUpgrade {
    static final long GIB = 1024L * 1024L * 1024L;
    /** Un móvil de 12 GB anuncia unos 10,8 GiB; uno de 8 GB, unos 7,3. */
    static final long MIN_TOTAL_RAM = 10L * GIB;
    static final String PREFS = "salve_prefs";
    static final String ATTEMPTED_KEY = "auto_e4b_attempted";
    static final String DECLINED_KEY = "auto_e4b_declined";

    private AutoModelUpgrade() { }

    /** Decisión pura (testable): ¿toca pasar a E4B automáticamente? */
    static boolean shouldUpgrade(long totalRamBytes, String activeModelId, boolean attempted, boolean declined) {
        return totalRamBytes >= MIN_TOTAL_RAM && LocalModelPolicy.FALLBACK.equals(activeModelId) && !attempted && !declined;
    }

    /** Llamar al arrancar y tras preparar el modelo por defecto. No hace nada si no procede. */
    public static void maybeUpgrade(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String active = LocalModelPolicy.idForPath(SalveLLM.getInstance(context).activeModelPath());
            if (!shouldUpgrade(totalRam(context), active, prefs.getBoolean(ATTEMPTED_KEY, false), prefs.getBoolean(DECLINED_KEY, false)))
                return;
            prefs.edit().putBoolean(ATTEMPTED_KEY, true).apply();
            salve.work.ModelDownloadWorker.enqueueOptionalAfterCurrent(context, LocalModelPolicy.GEMMA_E4B);
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
