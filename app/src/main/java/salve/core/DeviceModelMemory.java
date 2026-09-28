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
            throw new ModelMemoryPolicy.PressureException("RAM insuficiente para " + id + ". " + describe(ram));
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
