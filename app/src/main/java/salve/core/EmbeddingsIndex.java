package salve.core;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import salve.core.embeddings.E5EmbeddingEngine;
import salve.core.embeddings.EmbeddingArtifacts;
import salve.core.embeddings.EmbeddingMemoryPolicy;

/** One lazy E5 session per process. No weights in the APK and no network from inference. */
public final class EmbeddingsIndex implements AutoCloseable {
    private static volatile EmbeddingsIndex instance;
    private final Context app;
    private final EmbeddingArtifacts artifacts;
    private E5EmbeddingEngine engine;
    private boolean verified;
    private final ScheduledExecutorService idle = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "salve-embedding-release"); thread.setDaemon(true); return thread;
    });
    private ScheduledFuture<?> release;

    public static EmbeddingArtifacts artifacts(Context context) {
        return new EmbeddingArtifacts(new File(context.getNoBackupFilesDir(), "embeddings"));
    }
    public static EmbeddingsIndex get(Context context) {
        if (instance == null) synchronized (EmbeddingsIndex.class) {
            if (instance == null) instance = new EmbeddingsIndex(context.getApplicationContext());
        }
        return instance;
    }
    private EmbeddingsIndex(Context context) {
        app = context; artifacts = artifacts(context);
        app.registerComponentCallbacks(new ComponentCallbacks2() {
            @Override public void onTrimMemory(int level) { if (level >= TRIM_MEMORY_RUNNING_LOW) idle.execute(EmbeddingsIndex.this::close); }
            @Override public void onLowMemory() { idle.execute(EmbeddingsIndex.this::close); }
            @Override public void onConfigurationChanged(Configuration configuration) { }
        });
    }
    /** Symmetric semantic similarity (prediction/surprise): E5 specifies query: on both sides. */
    public float[] embed(String text) throws Exception { return embedQuery(text); }
    public synchronized float[] embedQuery(String text) throws Exception { return execute(text, false); }
    public synchronized float[] embedPassage(String text) throws Exception { return execute(text, true); }
    private float[] execute(String text, boolean passage) throws Exception {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("La memoria semántica se ejecuta fuera de la interfaz");
        ModelMemoryPolicy.Snapshot ram = DeviceModelMemory.read(app);
        if (!EmbeddingMemoryPolicy.allows(ram.available, ram.total, ram.threshold, ram.low, engine != null)) {
            close(); throw new ModelMemoryPolicy.PressureException("Memoria semántica pausada por RAM; búsqueda por palabras disponible");
        }
        if (engine == null) {
            if (!verified) { artifacts.verify(); verified = true; }
            engine = new E5EmbeddingEngine(artifacts.model, artifacts.tokenizer);
        }
        if (release != null) release.cancel(false);
        try { return passage ? engine.passage(text) : engine.query(text); }
        finally { release = idle.schedule(this::close, 90, TimeUnit.SECONDS); }
    }
    /** Releases native memory; the lightweight shared adapter remains reusable. */
    @Override public synchronized void close() {
        if (release != null) { release.cancel(false); release = null; }
        if (engine != null) { engine.close(); engine = null; }
    }
    public static float cosine(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Vectores incompatibles");
        float sum = 0; for (int i = 0; i < a.length; i++) sum += a[i] * b[i]; return sum;
    }
    public static byte[] toBlob(float[] vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : vector) buffer.putFloat(value); return buffer.array();
    }
    public static float[] fromBlob(byte[] blob) {
        FloatBuffer buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] vector = new float[buffer.remaining()]; buffer.get(vector); return vector;
    }
}
