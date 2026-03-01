package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * GeminiService — Integración con Google Gemini AI.
 * Permite a Salve tener un "cerebro superior" en la nube para razonamiento complejo,
 * programación y análisis profundo de recuerdos.
 */
public class GeminiService {
    private static final String TAG = "Salve/Gemini";
    private static final String PREFS_NAME = "gemini_prefs";
    private static final String KEY_API_KEY = "gemini_api_key";

    private static GeminiService instance;
    private final Context context;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private GenerativeModelFutures model;

    private GeminiService(Context context) {
        this.context = context.getApplicationContext();
        initModel();
    }

    public static synchronized GeminiService getInstance(Context context) {
        if (instance == null) {
            instance = new GeminiService(context);
        }
        return instance;
    }

    private void initModel() {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "Gemini API Key no configurada. Usa setApiKey() para habilitar.");
            return;
        }

        GenerativeModel gm = new GenerativeModel(
            "gemini-1.5-flash", // O "gemini-1.5-pro" para más capacidad
            apiKey
        );
        this.model = GenerativeModelFutures.from(gm);
    }

    public void setApiKey(String apiKey) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_KEY, apiKey).apply();
        initModel();
    }

    public String getApiKey() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_API_KEY, null);
    }

    public boolean isAvailable() {
        return model != null;
    }

    /**
     * Genera una respuesta síncrona (bloqueante) usando Gemini.
     */
    public String generateSync(String prompt) {
        if (model == null) return null;

        try {
            Content content = new Content.Builder()
                    .addText(prompt)
                    .build();

            ListenableFuture<GenerateContentResponse> future = model.generateContent(content);
            GenerateContentResponse response = future.get(); // Bloqueante
            return response.getText();
        } catch (Exception e) {
            Log.e(TAG, "Error generating content with Gemini", e);
            return null;
        }
    }
}
