package salve.core;

import android.app.ActivityManager;
import android.content.Context;
import android.os.SystemClock;
import java.util.Locale;
import java.util.function.BooleanSupplier;

public final class DeviceModelMemory {
    private DeviceModelMemory() { }
    public static ModelMemoryPolicy.Snapshot read(Context context) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            if (manager == null) throw new IllegalStateException("Sin información de RAM");
            manager.getMemoryInfo(info);
            return new ModelMemoryPolicy.Snapshot(info.availMem, info.totalMem, info.threshold, info.lowMemory);
        } catch (RuntimeException unavailable) { return new ModelMemoryPolicy.Snapshot(0, 0, 0, true); }
    }
    public static String describe(ModelMemoryPolicy.Snapshot ram) {
        if (!ram.known()) return "RAM disponible: no se pudo medir";
        return String.format(Locale.getDefault(), "RAM disponible: %.2f GiB de %.2f GiB%s",
                ram.available / (double)(1024 * ModelMemoryPolicy.MIB),
                ram.total / (double)(1024 * ModelMemoryPolicy.MIB), ram.low ? " · memoria baja" : "");
    }
    public static void require(Context context, String path, boolean loaded) {
        String id = LocalModelPolicy.idForPath(path);
        if (id == null) return; // imported models have no known budget
        ModelMemoryPolicy.Snapshot ram = read(context);
        if (!ModelMemoryPolicy.canLoad(id, ram, loaded))
            throw new ModelMemoryPolicy.PressureException(explain(id, ram, loaded));
    }
    /**
     * Contexto de LiteRT-LM que cabe ahora. Lanza {@link ModelMemoryPolicy.PressureException} con
     * explicación si no cabe ni el estándar. Para modelos importados sin presupuesto conocido: 4.096.
     */
    public static int liteRtContextFor(Context context, String path, boolean weightsResident, boolean vision) {
        String id = LocalModelPolicy.idForPath(path);
        if (id == null) return ModelMemoryPolicy.LITERT_STANDARD;
        ModelMemoryPolicy.Snapshot ram = read(context);
        int tokens = ModelMemoryPolicy.admitLiteRt(id, ram, weightsResident, vision);
        if (tokens == 0) throw new ModelMemoryPolicy.PressureException((ram.known() ? "RAM insuficiente para " : "No se pudo comprobar la RAM para ")
                + id + (vision ? " con visión" : "") + ". " + describe(ram) + String.format(Locale.getDefault(),
                ". Estimación mínima %s: %.2f GiB (con reserva para Android). Cierra alguna app abierta y pulsa «Intentar de nuevo».",
                weightsResident ? "adicional con el modelo ya cargado" : "para iniciar",
                ModelMemoryPolicy.requiredLiteRt(id, weightsResident, ram.threshold, vision, ModelMemoryPolicy.LITERT_STANDARD)
                        / (double) (1024 * ModelMemoryPolicy.MIB)));
        return tokens;
    }

    /** true si cabe el modelo con visión (para cargarla de antemano y no recargar al llegar una foto). */
    public static boolean fitsLiteRtVision(Context context, String path, boolean weightsResident) {
        String id = LocalModelPolicy.idForPath(path);
        if (id == null) return false;
        return ModelMemoryPolicy.selectLiteRtContext(id, read(context), weightsResident, true) != 0;
    }

    public static int contextFor(Context context, String path, boolean loaded) {
        String id = LocalModelPolicy.idForPath(path);
        if (id == null) return GgufContextPolicy.STANDARD;
        ModelMemoryPolicy.Snapshot ram = read(context);
        int tokens = ModelMemoryPolicy.selectContext(id, ram, loaded);
        if (tokens == 0) throw new ModelMemoryPolicy.PressureException(explain(id, ram, loaded));
        return tokens;
    }
    public static String explain(String id, ModelMemoryPolicy.Snapshot ram, boolean loaded) {
        boolean dolphin = LocalModelPolicy.PRIMARY.equals(id) || LocalModelPolicy.LIGHT.equals(id);
        int smallest = dolphin ? GgufContextPolicy.COMPACT : GgufContextPolicy.STANDARD;
        return (ram.known() ? "RAM insuficiente para " : "No se pudo comprobar la RAM para ") + id + ". " + describe(ram)
                + String.format(Locale.getDefault(), ". Estimación mínima %s: %.2f GiB (%scon reserva para Android).",
                loaded ? "adicional con los pesos cargados" : "para iniciar",
                ModelMemoryPolicy.required(id, loaded, ram.threshold, smallest) / (double)(1024 * ModelMemoryPolicy.MIB),
                dolphin ? "contexto 2048, " : "");
    }
    /** llama.cpp invokes this on native threads. Throttle Android queries and latch pressure. */
    public static final class Watch implements BooleanSupplier {
        private final Context context;
        private final BooleanSupplier cancelled;
        private long checkedAt;
        private volatile boolean pressure;
        public Watch(Context context, BooleanSupplier cancelled) { this.context = context; this.cancelled = cancelled; }
        public boolean hadPressure() { return pressure; }
        @Override public synchronized boolean getAsBoolean() {
            if (cancelled.getAsBoolean()) return true;
            long now = SystemClock.elapsedRealtime();
            if (!pressure && (checkedAt == 0 || now - checkedAt >= 1000)) {
                checkedAt = now;
                pressure = ModelMemoryPolicy.underPressure(read(context));
            }
            return pressure;
        }
    }
}
