package salve.services;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import salve.core.ReconocimientoFacial;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * CamaraService con verificación bajo demanda y alta/registro del rostro propietario.
 *
 * • No mantiene la cámara activa todo el tiempo: solo se enciende cuando se pide (acción VERIFY_ONE_SHOT o ENROLL_OWNER)
 * • La primera vez que verifique correctamente, marca al usuario como "owner_verified" y guarda un snapshot (y, si existe, fija el patrón en ReconocimientoFacial)
 * • En siguientes llamadas, si ya está verificado, el servicio hace una verificación rápida (one-shot) y se apaga solo.
 * • Throttling de frames para no quemar CPU.
 */
@SuppressWarnings("deprecation") // Usamos Camera API antigua por compatibilidad
public class CamaraService extends Service implements Camera.PreviewCallback {

    // ===== API pública (acciones) =====
    // Antes: BuildConfig.APPLICATION_ID + ".action."
    // Quitamos BuildConfig para evitar el error y usamos el applicationId fijo de tu app.
    private static final String ACTION_PREFIX = "com.salve.app.action.";
    public static final String ACTION_VERIFY_ONE_SHOT = ACTION_PREFIX + "VERIFY_ONE_SHOT";   // Verifica y apaga
    public static final String ACTION_ENROLL_OWNER    = ACTION_PREFIX + "ENROLL_OWNER";      // Fuerza alta del rostro base
    public static final String ACTION_STOP            = ACTION_PREFIX + "STOP";              // Apagar si estuviera corriendo

    // ===== Notificación (FGS) =====
    private static final String TAG   = "Salve/CamaraService";
    private static final String CH_ID = "salve_camera";
    private static final int    NOTIF_ID = 99;

    // ===== Preferencias =====
    private static final String PREFS_NAME = "salve_face_prefs";
    private static final String KEY_OWNER_VERIFIED = "owner_verified";      // boolean
    private static final String KEY_OWNER_SNAPSHOT = "owner_snapshot_path"; // String

    // ===== Cámara =====
    private Camera camera;
    private SurfaceTexture dummyTexture;
    private ReconocimientoFacial reconocimiento;

    // ===== Estado de ejecución =====
    private enum Mode { VERIFY, ENROLL }
    private Mode currentMode = Mode.VERIFY;

    // Throttling
    private long lastProcessTimeMs = 0L;
    private static final long PROCESS_EVERY_MS = 600; // procesa ~1.6 fps

    @Override
    public void onCreate() {
        super.onCreate();
        ensureChannel();
        reconocimiento = new ReconocimientoFacial(this);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_ENROLL_OWNER.equals(action)) {
            currentMode = Mode.ENROLL;
        } else {
            currentMode = Mode.VERIFY; // por defecto one-shot verify
        }

        // Subir a FOREGROUND **antes** de tocar la cámara
        Notification notif = new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle("Cámara activa")
                .setContentText(currentMode == Mode.ENROLL ? "Registrando rostro base" : "Verificando rostro (one-shot)")
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, notif);

        iniciarCamara();
        return START_NOT_STICKY; // no queremos relanzar solos si el sistema nos mata
    }

    // ===== Helpers públicos para lanzar acciones desde cualquier Context =====
    public static void requestOneShotVerify(Context ctx) {
        Intent i = new Intent(ctx, CamaraService.class).setAction(ACTION_VERIFY_ONE_SHOT);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
    }

    public static void requestEnrollOwner(Context ctx) {
        Intent i = new Intent(ctx, CamaraService.class).setAction(ACTION_ENROLL_OWNER);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i); else ctx.startService(i);
    }

    public static void requestStop(Context ctx) {
        Intent i = new Intent(ctx, CamaraService.class).setAction(ACTION_STOP);
        ctx.startService(i);
    }

    // ===== Cámara =====
    @SuppressLint("MissingPermission")
    private void iniciarCamara() {
        try {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Permiso CAMERA no concedido. Deteniendo servicio.");
                stopSelf();
                return;
            }

            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            Camera.Parameters params = camera.getParameters();

            Camera.Size best = null;
            for (Camera.Size s : params.getSupportedPreviewSizes()) {
                if (s.width <= 1280 && s.height <= 720) {
                    if (best == null || (s.width * s.height) > (best.width * best.height)) best = s;
                }
            }
            if (best != null) {
                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewSize(best.width, best.height);
                camera.setParameters(params);
            }

            if (dummyTexture == null) dummyTexture = new SurfaceTexture(0);
            camera.setPreviewTexture(dummyTexture);

            camera.setPreviewCallback(this);
            camera.startPreview();
            Log.d(TAG, "Cámara frontal iniciada. Modo=" + currentMode);
        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar la cámara: " + e.getMessage(), e);
            stopSelf();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera cam) {
        try {
            // Throttle
            long now = System.currentTimeMillis();
            if (now - lastProcessTimeMs < PROCESS_EVERY_MS) return;
            lastProcessTimeMs = now;

            if (cam == null) return;
            Camera.Size size = cam.getParameters().getPreviewSize();
            Bitmap bitmap = convertirYUV420AABitmap(data, size.width, size.height);

            if (currentMode == Mode.ENROLL) {
                // Guardar snapshot + intentar fijar patrón en ReconocimientoFacial
                guardarSnapshotYFijarPatron(bitmap, /*markVerified=*/true);
                Log.i(TAG, "Rostro base registrado. Apagando cámara.");
                stopSelf();
                return;
            }

            // VERIFY one-shot
            if (!isOwnerVerified()) {
                // Si aún no hay owner verificado, al primer match correcto: marcar y guardar
                reconocimiento.verificarRostro(bitmap, esValido -> {
                    if (esValido) {
                        Log.i(TAG, "Primera verificación correcta. Marcando owner_verified y guardando patrón.");
                        guardarSnapshotYFijarPatron(bitmap, /*markVerified=*/true);
                    } else {
                        Log.w(TAG, "Rostro no reconocido durante primera verificación.");
                    }
                    stopSelf(); // siempre one-shot
                });
            } else {
                // Ya verificado anteriormente: comprobación rápida y apagar
                reconocimiento.verificarRostro(bitmap, esValido -> {
                    if (esValido) {
                        Log.d(TAG, "Rostro autorizado (owner ya verificado previamente).");
                    } else {
                        Log.w(TAG, "Rostro NO coincide con owner verificado. Protocolo de seguridad.");
                        // TODO: acción de seguridad (bloqueo/alerta)
                    }
                    stopSelf();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "onPreviewFrame error: " + e.getMessage(), e);
            stopSelf();
        }
    }

    private Bitmap convertirYUV420AABitmap(byte[] data, int width, int height) {
        android.graphics.YuvImage yuv = new android.graphics.YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new android.graphics.Rect(0, 0, width, height), 90, out);
        byte[] bytes = out.toByteArray();
        return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void guardarSnapshotYFijarPatron(Bitmap bmp, boolean markVerified) {
        try {
            // 1) Guardar snapshot en almacenamiento interno
            File out = new File(getFilesDir(), "owner_face.jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos);
            }

            // 2) Intentar informar al motor (si expone API). Lo intentamos por reflexión para no romper si no existe
            try {
                reconocimiento.getClass()
                        .getMethod("establecerRostroBase", Bitmap.class)
                        .invoke(reconocimiento, bmp);
                Log.d(TAG, "establecerRostroBase(Bitmap) invocado con éxito.");
            } catch (NoSuchMethodException nsme) {
                // fallback: si ReconocimientoFacial no tiene ese método, asumimos que usa almacenamiento propio o ADNVisual
                Log.d(TAG, "ReconocimientoFacial.establecerRostroBase(Bitmap) no existe; se conserva snapshot local.");
            } catch (Throwable t) {
                Log.w(TAG, "No se pudo fijar patrón en ReconocimientoFacial: " + t);
            }

            // 3) Marcar preferencias
            SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            sp.edit()
                    .putString(KEY_OWNER_SNAPSHOT, out.getAbsolutePath())
                    .putBoolean(KEY_OWNER_VERIFIED, markVerified)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Error guardando snapshot/fijando patrón: " + e.getMessage(), e);
        }
    }

    private boolean isOwnerVerified() {
        SharedPreferences sp = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sp.getBoolean(KEY_OWNER_VERIFIED, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (camera != null) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            if (dummyTexture != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) dummyTexture.release();
                dummyTexture = null;
            }
        } catch (Exception ignore) {}
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null && nm.getNotificationChannel(CH_ID) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_ID, "Salve cámara", NotificationManager.IMPORTANCE_LOW
                );
                ch.setDescription("Uso de cámara bajo demanda");
                nm.createNotificationChannel(ch);
            }
        }
    }
}
