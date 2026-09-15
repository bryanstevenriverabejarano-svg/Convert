package salve.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import salve.core.ReconocimientoFacial;

/**
 * VideoAnalysisManager - Maneja la captura de frames usando CameraX ImageAnalysis.
 * Combina el buffer de frames multimodales (para Gemini) con el reconocimiento facial.
 */
public class VideoAnalysisManager {
    private static final String TAG = "Salve/VideoAnalysis";
    private static final int MAX_FRAMES = 5;
    private static final long FRAME_INTERVAL_MS = 1000; // ~1 fps

    // ===== Preferencias Reconocimiento Facial =====
    private static final String PREFS_NAME = "salve_face_prefs";
    private static final String KEY_OWNER_VERIFIED = "owner_verified";
    private static final String KEY_OWNER_SNAPSHOT = "owner_snapshot_path";

    private static VideoAnalysisManager instance;
    private final LinkedList<Bitmap> frameBuffer = new LinkedList<>();
    private long lastFrameTimestamp = 0;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    private Context context;
    private ReconocimientoFacial reconocimiento;
    
    // Estado de Reconocimiento
    public enum Mode { NORMAL, VERIFY, ENROLL }
    private Mode currentMode = Mode.NORMAL;

    private VideoAnalysisManager() {}

    public static synchronized VideoAnalysisManager getInstance() {
        if (instance == null) {
            instance = new VideoAnalysisManager();
        }
        return instance;
    }

    public void startAnalysis(Context context, LifecycleOwner lifecycleOwner) {
        this.context = context.getApplicationContext();
        if (this.reconocimiento == null) {
            this.reconocimiento = new ReconocimientoFacial(this.context);
        }

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(analysisExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA; // Cambiado a frontal para reconocimiento facial

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis);

                Log.d(TAG, "CameraX ImageAnalysis iniciado. Modo: " + currentMode);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error iniciando CameraX", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    private void analyzeImage(@NonNull ImageProxy image) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTimestamp >= FRAME_INTERVAL_MS) {
            lastFrameTimestamp = currentTime;

            Bitmap bitmap = imageToBitmap(image);
            if (bitmap != null) {
                // Rotar bitmap si es necesario (CameraX suele entregar rotado según el sensor)
                int rotationDegrees = image.getImageInfo().getRotationDegrees();
                if (rotationDegrees != 0) {
                    bitmap = rotateBitmap(bitmap, rotationDegrees);
                }

                synchronized (frameBuffer) {
                    if (frameBuffer.size() >= MAX_FRAMES) {
                        Bitmap removed = frameBuffer.removeFirst();
                        removed.recycle();
                    }
                    frameBuffer.addLast(bitmap);
                }
                
                // --- Procesar Reconocimiento Facial si es necesario ---
                procesarReconocimientoFacial(bitmap);
                
                // Log.d(TAG, "Frame capturado. Buffer size: " + frameBuffer.size());
            }
        }
        image.close();
    }

    // ===== Funciones Integradas de Reconocimiento Facial =====

    public void setMode(Mode mode) {
        this.currentMode = mode;
        Log.d(TAG, "Cambiando modo de cámara a: " + mode.name());
    }

    private void procesarReconocimientoFacial(Bitmap bitmap) {
        if (currentMode == Mode.NORMAL || reconocimiento == null) return;

        if (currentMode == Mode.ENROLL) {
            // Guardar snapshot + fijar patrón
            guardarSnapshotYFijarPatron(bitmap, true);
            Log.i(TAG, "Rostro base registrado. Volviendo a modo NORMAL.");
            currentMode = Mode.NORMAL;
            return;
        }

        if (currentMode == Mode.VERIFY) {
            if (!isOwnerVerified()) {
                reconocimiento.verificarRostro(bitmap, esValido -> {
                    if (esValido) {
                        Log.i(TAG, "Primera verificación correcta. Marcando owner_verified.");
                        guardarSnapshotYFijarPatron(bitmap, true);
                    } else {
                        Log.w(TAG, "Rostro no reconocido durante primera verificación.");
                    }
                    currentMode = Mode.NORMAL;
                });
            } else {
                reconocimiento.verificarRostro(bitmap, esValido -> {
                    if (esValido) {
                        Log.d(TAG, "Rostro autorizado (owner ya verificado previamente).");
                    } else {
                        Log.w(TAG, "Rostro NO coincide con owner verificado. Protocolo de seguridad.");
                    }
                    currentMode = Mode.NORMAL;
                });
            }
        }
    }

    private void guardarSnapshotYFijarPatron(Bitmap bmp, boolean markVerified) {
        try {
            if (context == null) return;
            // 1) Guardar snapshot en almacenamiento interno
            File out = new File(context.getFilesDir(), "owner_face.jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            }

            // 2) Intentar informar al motor
            try {
                reconocimiento.getClass()
                        .getMethod("establecerRostroBase", Bitmap.class)
                        .invoke(reconocimiento, bmp);
                Log.d(TAG, "establecerRostroBase(Bitmap) invocado con éxito.");
            } catch (Exception e) {
                Log.d(TAG, "ReconocimientoFacial.establecerRostroBase no existe o falló.");
            }

            // 3) Marcar preferencias
            SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_OWNER_SNAPSHOT, out.getAbsolutePath())
                    .putBoolean(KEY_OWNER_VERIFIED, markVerified)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando snapshot/fijando patrón", e);
        }
    }

    private boolean isOwnerVerified() {
        if (context == null) return false;
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_OWNER_VERIFIED, false);
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        // Implementación simplificada usando toBitmap() disponible en CameraX 1.3+
        try {
            return image.toBitmap();
        } catch (Exception e) {
            Log.e(TAG, "Error convirtiendo ImageProxy a Bitmap", e);
            return null;
        }
    }

    private Bitmap rotateBitmap(Bitmap source, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public List<Bitmap> getRecentFrames() {
        synchronized (frameBuffer) {
            return new ArrayList<>(frameBuffer);
        }
    }

    public void clearBuffer() {
        synchronized (frameBuffer) {
            for (Bitmap bmp : frameBuffer) {
                bmp.recycle();
            }
            frameBuffer.clear();
        }
    }
}
