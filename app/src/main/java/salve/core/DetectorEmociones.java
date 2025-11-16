package salve.core;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DetectorEmociones {

    private static final String MODEL_PATH = "emotion_model.tflite";

    private static final String[] LABELS = {
            "feliz", "triste", "enojado", "sorprendido", "miedo", "neutro"
    };

    private Interpreter tflite;
    private final Context context;
    private final Map<String, List<String>> fallbackDict;

    public DetectorEmociones(Context context) {
        this.context = context;
        tflite = loadModel(MODEL_PATH);

        fallbackDict = new HashMap<>();
        fallbackDict.put("feliz", Arrays.asList("feliz", "contento", "alegre", "entusiasmado", "satisfecho"));
        fallbackDict.put("triste", Arrays.asList("triste", "llorando", "deprimido", "desanimado", "sollozo"));
        fallbackDict.put("enojado", Arrays.asList("enojado", "molesto", "furioso", "ira", "rabia"));
        fallbackDict.put("sorprendido", Arrays.asList("sorprendido", "impactado", "inesperado", "wow"));
        fallbackDict.put("miedo", Arrays.asList("miedo", "asustado", "temor", "pánico", "nervioso"));
    }

    private Interpreter loadModel(String modelPath) {
        try (AssetFileDescriptor afd = context.getAssets().openFd(modelPath);
             FileInputStream fis = new FileInputStream(afd.getFileDescriptor())) {

            FileChannel channel = fis.getChannel();
            MappedByteBuffer buf = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.getStartOffset(),
                    afd.getLength()
            );
            return new Interpreter(buf);
        } catch (IOException e) {
            Log.e("Salve", "No se pudo cargar el modelo TFLite, usando fallback", e);
            return null;
        }
    }

    public String detectarEmocion(String texto) {
        String lower = texto.toLowerCase(Locale.ROOT).trim();

        // 1) Modelo TFLite
        if (tflite != null) {
            try {
                float[] input = preprocess(lower);
                float[][] output = new float[1][LABELS.length];
                tflite.run(input, output);
                int bestIdx = argMax(output[0]);
                return LABELS[bestIdx];
            } catch (Exception e) {
                Log.w("Salve", "Error infiriendo TFLite, usando fallback", e);
            }
        }

        // 2) Fallback de diccionario
        Map<String, Integer> conteo = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : fallbackDict.entrySet()) {
            String emo = entry.getKey();
            int puntos = 0;
            for (String palabra : entry.getValue()) {
                if (lower.contains(palabra)) puntos++;
            }
            if (puntos > 0) conteo.put(emo, puntos);
        }
        return conteo.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutro");
    }

    private float[] preprocess(String text) {
        float lengthNorm = Math.min(text.length(), 100) / 100f;
        float vowelCount = text.replaceAll("(?i)[^aeiouáéíóúü]", "").length() / 20f;
        return new float[]{lengthNorm, vowelCount};
    }

    private int argMax(float[] array) {
        int best = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[best]) best = i;
        }
        return best;
    }
}
