package salve.core.visual;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentationResult;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter;
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions;

import java.nio.FloatBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Separa el sujeto del fondo en una foto, en el propio móvil (segmentación de sujetos de ML Kit).
 * Devuelve, por píxel, la confianza de que sea el sujeto (0..1). Si el módulo aún no se ha descargado,
 * no hay sujeto o algo falla, devuelve null y el dibujo se calca como antes. Nunca en el hilo principal.
 */
public final class SubjectCutout {
    private static final String TAG = "Salve/Recorte";
    private static volatile SubjectSegmenter segmenter;

    private SubjectCutout() { }

    private static SubjectSegmenter client() {
        if (segmenter == null) {
            synchronized (SubjectCutout.class) {
                if (segmenter == null) segmenter = SubjectSegmentation.getClient(
                        new SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build());
            }
        }
        return segmenter;
    }

    /** Máscara del sujeto del tamaño del bitmap (ancho × alto), o null. */
    public static float[] mask(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() < 16 || bitmap.getHeight() < 16) return null;
        try {
            SubjectSegmentationResult result = Tasks.await(client().process(InputImage.fromBitmap(bitmap, 0)), 8, TimeUnit.SECONDS);
            FloatBuffer buffer = result == null ? null : result.getForegroundConfidenceMask();
            if (buffer == null) return null;
            int n = bitmap.getWidth() * bitmap.getHeight();
            buffer.rewind();
            if (buffer.remaining() < n) return null;
            float[] mask = new float[n];
            buffer.get(mask, 0, n);
            return mask;
        } catch (Exception | LinkageError unavailable) {
            Log.i(TAG, "Sin recorte de sujeto: " + unavailable.getClass().getSimpleName());
            return null;
        }
    }
}
