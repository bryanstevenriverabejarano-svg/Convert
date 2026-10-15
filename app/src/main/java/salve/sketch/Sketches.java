package salve.sketch;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Punto de acceso Android: carga el repertorio una vez, prepara las transformadas fuera del hilo
 * principal y publica en el escenario el dibujo que Salve está trazando ahora.
 */
public final class Sketches {
    private static final String TAG = "SalveSketch";
    private static volatile SketchLibrary library;
    private static final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "salve-sketch");
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });
    private static final Map<String, SketchAnimation> ready = new LinkedHashMap<String, SketchAnimation>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, SketchAnimation> eldest) { return size() > 24; }
    };
    private static final java.util.Set<String> pending = new java.util.HashSet<>();

    private Sketches() { }

    /** Bloquea la primera vez (≈0,3 MB de JSON); llámese desde un hilo de fondo o tras {@link #preload}. */
    public static SketchLibrary library(Context context) {
        SketchLibrary current = library;
        if (current != null || context == null) return current;
        synchronized (Sketches.class) {
            if (library != null) return library;
            try (InputStream input = context.getApplicationContext().getAssets().open("sketch/library.json")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[16384]; int n;
                while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
                library = SketchLibrary.parse(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            } catch (Exception | OutOfMemoryError error) {
                Log.e(TAG, "No se pudo cargar el repertorio de trazos", error);
            }
            return library;
        }
    }

    /** Sólo devuelve la biblioteca si ya está cargada (seguro en el hilo principal). */
    public static SketchLibrary loaded() { return library; }

    public static void preload(Context context) {
        Context app = context.getApplicationContext();
        worker.execute(() -> {
            if (library(app) != null) main.post(() -> { for (Runnable l : new ArrayList<>(listeners)) l.run(); });
        });
    }

    /** Animación lista para dibujar, o null mientras se calcula (se encola sola). */
    public static SketchAnimation animation(SketchScene scene) {
        if (scene == null) return null;
        synchronized (ready) {
            SketchAnimation done = ready.get(scene.key);
            if (done != null || !pending.add(scene.key)) return done;
        }
        worker.execute(() -> {
            SketchAnimation made = null;
            try { made = new SketchAnimation(scene); }
            catch (RuntimeException | OutOfMemoryError error) { Log.w(TAG, "Escena no dibujable: " + scene.key, error); }
            synchronized (ready) {
                pending.remove(scene.key);
                if (made != null) ready.put(scene.key, made);
            }
        });
        return null;
    }

    // ───────────── escenario ─────────────

    /** Lo que se ve en el escenario: escena y momento (uptime) en que empezó. */
    public static final class Showing {
        public final SketchScene scene;
        public final long startedAt;
        Showing(SketchScene scene, long startedAt) { this.scene = scene; this.startedAt = startedAt; }
    }

    private static volatile Showing showing;
    private static final List<Runnable> listeners = new ArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());

    /** Salve empieza a trazar esta escena en su espacio astral (cualquier hilo). */
    public static void show(SketchScene scene) {
        if (scene == null) return;
        animation(scene);
        main.post(() -> {
            Showing current = showing;
            if (current != null && current.scene.key.equals(scene.key)
                    && android.os.SystemClock.uptimeMillis() - current.startedAt < 15000) return;
            showing = new Showing(scene, android.os.SystemClock.uptimeMillis());
            salve.avatar.AstralBody.get().setDrawing(scene.title, 12f);
            for (Runnable l : new ArrayList<>(listeners)) l.run();
        });
    }

    public static Showing showing() { return showing; }
    public static void clearShowing() { showing = null; }
    public static void addListener(Runnable l) { if (!listeners.contains(l)) listeners.add(l); }
    public static void removeListener(Runnable l) { listeners.remove(l); }
}
