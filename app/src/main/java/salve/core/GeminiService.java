package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Gemini text/image inference. Configured credentials do not imply a verified connection. */
public class GeminiService {
    private static final String TAG = "Salve/Gemini";
    private static final String PREFS_NAME = "gemini_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";
    private static final String KEY_MODEL = "gemini_model";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static GeminiService instance;
    private final SharedPreferences preferences;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false).build();

    private GeminiService(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized GeminiService getInstance(Context context) {
        if (instance == null) instance = new GeminiService(context);
        return instance;
    }

    public synchronized void configure(String apiKey, String modelName) {
        String model = GeminiProtocol.modelName(modelName);
        preferences.edit().putString(KEY_API_KEY, apiKey == null ? "" : apiKey.trim())
                .putString(KEY_MODEL, model).apply();
    }

    public synchronized void setApiKey(String apiKey) { configure(apiKey, getModelName()); }
    public synchronized String getApiKey() { return preferences.getString(KEY_API_KEY, ""); }
    public synchronized String getModelName() {
        return GeminiProtocol.modelName(preferences.getString(KEY_MODEL, GeminiProtocol.DEFAULT_MODEL));
    }
    /** Configuration only; use generateResultSync to verify real inference. */
    public synchronized boolean isAvailable() { return !getApiKey().trim().isEmpty(); }

    public String generateSync(String prompt) { return generateSync(prompt, null); }
    public String generateSync(String prompt, List<Bitmap> frames) {
        ModelResult result = generateResultSync(prompt, frames);
        return result.isSuccess() ? result.getText() : null;
    }

    public ModelResult generateResultSync(String prompt, List<Bitmap> frames) {
        return generateResultSync(prompt, frames, null);
    }

    /** Per-request specialist; does not change the conversational model or its saved configuration. */
    public ModelResult generateResultSync(String prompt, List<Bitmap> frames, String modelOverride) {
        long startedAt = System.nanoTime();
        String key;
        String model;
        synchronized (this) {
            key = getApiKey();
            model = modelOverride == null || modelOverride.trim().isEmpty()
                    ? getModelName() : GeminiProtocol.modelName(modelOverride);
        }
        if (key.trim().isEmpty()) return ModelResult.failure(ModelResult.Status.UNAVAILABLE,
                "Configura una clave API en IA y cámara", 0L);
        if (Thread.currentThread().isInterrupted()) return ModelResult.failure(
                ModelResult.Status.CANCELLED, "Turno cancelado", 0L);
        try {
            List<String> images = new ArrayList<>();
            if (frames != null) {
                if (frames.size() > 5) throw new IllegalArgumentException("Demasiadas imágenes");
                for (Bitmap frame : frames) images.add(encodeImage(frame));
            }
            Request request = new Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
                    .header("x-goog-api-key", key)
                    .post(RequestBody.create(GeminiProtocol.request(prompt, images), JSON)).build();
            try (Response response = client.newCall(request).execute()) {
                String body = response.isSuccessful() && response.body() != null ? response.body().string() : "";
                ModelResult result = GeminiProtocol.response(response.code(), body, elapsed(startedAt));
                Log.i(TAG, "provider=gemini model=" + model + " status=" + result.getStatus()
                        + " images=" + images.size() + " latency_ms=" + result.getLatencyMillis());
                return result;
            }
        } catch (InterruptedIOException e) {
            return ModelResult.failure(Thread.currentThread().isInterrupted()
                    ? ModelResult.Status.CANCELLED : ModelResult.Status.TIMEOUT,
                    "La petición a Gemini no terminó a tiempo", elapsed(startedAt));
        } catch (IOException e) {
            return ModelResult.failure(ModelResult.Status.ERROR, "No se pudo conectar con Gemini", elapsed(startedAt));
        } catch (RuntimeException e) {
            return ModelResult.failure(ModelResult.Status.ERROR, "No se pudo preparar la consulta o la imagen", elapsed(startedAt));
        }
    }

    private String encodeImage(Bitmap image) {
        if (image == null || image.isRecycled()) throw new IllegalArgumentException("Imagen no disponible");
        float scale = Math.min(1f, 1024f / Math.max(image.getWidth(), image.getHeight()));
        Bitmap scaled = scale < 1f ? Bitmap.createScaledBitmap(image,
                Math.max(1, Math.round(image.getWidth() * scale)),
                Math.max(1, Math.round(image.getHeight() * scale)), true) : image;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 85, bytes)) {
                throw new IllegalArgumentException("No se pudo codificar la imagen");
            }
            return Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP);
        } finally {
            if (scaled != image) scaled.recycle();
        }
    }

    private long elapsed(long start) { return (System.nanoTime() - start) / 1_000_000L; }
}
