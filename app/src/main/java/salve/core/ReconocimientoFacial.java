package salve.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;
import java.util.function.Consumer;

public class ReconocimientoFacial {

    private final Context context;
    private ADNVisual adnVisual;
    private static final int TOLERANCIA_ADN = 50;

    public ReconocimientoFacial(Context context) {
        this.context = context;
        this.adnVisual = new ADNVisual();
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void verificarRostro(Bitmap imagenActual, Consumer<Boolean> resultadoCallback) {

        FaceDetectorOptions opciones = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .build();

        InputImage imagen = InputImage.fromBitmap(imagenActual, 0);
        FaceDetector detector = FaceDetection.getClient(opciones);

        detector.process(imagen)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        Face rostro = faces.get(0);
                        List<PointF> puntos = rostro.getAllLandmarks()
                                .stream().map(landmark -> landmark.getPosition()).toList();

                        String codigoNuevo = adnVisual.codificarRostro(puntos);

                        if (adnVisual.obtenerHistorial().isEmpty()) {
                            adnVisual.actualizarADN(codigoNuevo);
                            Log.d("Salve", "Primer rostro autorizado guardado.");
                            resultadoCallback.accept(true);
                        } else {
                            boolean coincide = adnVisual.esCoincidenciaAceptable(codigoNuevo, TOLERANCIA_ADN);
                            if (coincide) {
                                adnVisual.actualizarADN(codigoNuevo);
                                resultadoCallback.accept(true);
                            } else {
                                resultadoCallback.accept(false);
                            }
                        }
                    } else {
                        resultadoCallback.accept(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Salve", "Error en reconocimiento facial: " + e.getMessage());
                    resultadoCallback.accept(false);
                });
    }
}