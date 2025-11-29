// DetectorEmociones.java (versión segura sin crashear)

package salve.core;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DetectorEmociones {

    private static final String TAG = "DetectorEmociones";
    private static final String MODEL_ASSET_NAME = "emotion_model.tflite";

    private final boolean modeloDisponible;
    private Interpreter interpreter;

    public DetectorEmociones(Context context) {
        Interpreter tmp = null;
        boolean disponible = false;

        try {
            MappedByteBuffer buffer = loadModel(context);
            tmp = new Interpreter(buffer);
            disponible = true;
            Log.d(TAG, "Modelo de emociones cargado correctamente.");
        } catch (IOException e) {
            // AQUÍ ES DONDE ANTES TE REVENTABA TODO
            Log.e(TAG, "No se pudo cargar el modelo TFLite de emociones. Usando modo dummy.", e);
        }

        this.interpreter = tmp;
        this.modeloDisponible = disponible;
    }

    private MappedByteBuffer loadModel(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_ASSET_NAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Analiza el texto y devuelve la emoción.
     * Si el modelo no está disponible, devolvemos "neutro" como fallback.
     */
    public String detectarEmocion(String texto) {
        if (!modeloDisponible || interpreter == null) {
            // Fallback seguro: no rompemos el flujo, pero informamos en logs.
            Log.w(TAG, "Modelo de emociones no disponible. Devolviendo 'neutro'.");
            return "neutro";
        }

        // TODO: aquí va tu lógica real de inferencia con TFLite.
        // De momento puedes seguir usando algo simple o lo que ya tengas.
        // Ejemplo placeholder:
        return "neutro";
    }

    public void cerrar() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
