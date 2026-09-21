package salve.services;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import salve.core.sensors.SensorSnapshot;
import salve.core.sensors.SessionPolicy;

/** Explicit, temporary hardware observations. Constructing this class reads no sensors. */
public final class SistemaSensorial implements SensorEventListener {
    private static final String TAG = "Salve/Sensores";
    private final Context context;
    private final SensorManager sensorManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SessionPolicy session = new SessionPolicy();
    private final SensorSnapshot snapshot = new SensorSnapshot();
    private Runnable expiration;
    private boolean observing;
    private long generation;

    public SistemaSensorial(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
    }

    /** Called only after an explicit session request. Existing sessions are not extended. */
    public synchronized String iniciarSesion(long durationMs) {
        if (durationMs <= 0L || durationMs > SessionPolicy.MAX_DURATION_MS) {
            return "Elige una sesión de sensores de entre 1 y 60 segundos.";
        }
        long now = SystemClock.elapsedRealtime();
        if (session.isActive(now)) {
            return "La sesión de sensores ya está activa; quedan "
                    + secondsRemaining(now) + " segundos. No amplié su duración.";
        }
        detenerSesion();
        session.start(now, durationMs);
        observing = true;
        register(Sensor.TYPE_LIGHT, SensorSnapshot.Reading.LIGHT);
        register(Sensor.TYPE_ACCELEROMETER, SensorSnapshot.Reading.ACCELERATION);
        register(Sensor.TYPE_PROXIMITY, SensorSnapshot.Reading.PROXIMITY);
        register(Sensor.TYPE_AMBIENT_TEMPERATURE, SensorSnapshot.Reading.AMBIENT_TEMPERATURE);
        readBattery(now);

        final long requestedGeneration = generation;
        expiration = () -> expireSession(requestedGeneration);
        if (!mainHandler.postDelayed(expiration, session.remainingMs(SystemClock.elapsedRealtime()))) {
            detenerSesion();
            return "No pude programar el cierre automático; los sensores siguen desactivados.";
        }
        return "Sensores activos durante " + ((durationMs + 999L) / 1000L)
                + " segundos: luz, aceleración, proximidad, temperatura ambiente y batería,"
                + " según disponibilidad. Puedes decir «desactiva sensores» para detenerlos.";
    }

    private void register(int type, SensorSnapshot.Reading reading) {
        boolean registered = false;
        if (sensorManager != null) {
            try {
                Sensor sensor = sensorManager.getDefaultSensor(type);
                registered = sensor != null && sensorManager.registerListener(
                        this, sensor, SensorManager.SENSOR_DELAY_NORMAL, mainHandler);
            } catch (RuntimeException error) {
                Log.w(TAG, "No se pudo registrar un sensor: " + error.getClass().getSimpleName());
            }
        }
        snapshot.setAvailable(reading, registered);
    }

    private synchronized void expireSession(long requestedGeneration) {
        if (requestedGeneration != generation || !observing) return;
        long remaining = session.remainingMs(SystemClock.elapsedRealtime());
        if (remaining > 0L && mainHandler.postDelayed(expiration, remaining)) return;
        detenerSesion();
    }

    /** Safe from the UI, conversation executor or expiration callback; clears all samples. */
    public synchronized void detenerSesion() {
        generation++;
        observing = false;
        session.stop();
        if (expiration != null) mainHandler.removeCallbacks(expiration);
        expiration = null;
        if (sensorManager != null) {
            try { sensorManager.unregisterListener(this); }
            catch (RuntimeException error) {
                Log.w(TAG, "No se pudo liberar un listener: " + error.getClass().getSimpleName());
            }
        }
        snapshot.clear();
    }

    public synchronized boolean isActive() {
        if (session.isActive(SystemClock.elapsedRealtime())) return true;
        if (observing) detenerSesion();
        return false;
    }

    @Override
    public synchronized void onSensorChanged(SensorEvent event) {
        if (!isActive() || event == null || event.sensor == null || event.values == null) return;
        long now = SystemClock.elapsedRealtime();
        // SensorEvent.timestamp uses the elapsed-realtime clock in nanoseconds.
        long sampledAt = event.timestamp / 1_000_000L;
        if (!session.acceptsSample(sampledAt, now)) return;
        SensorSnapshot.Reading reading;
        double value;
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LIGHT:
                reading = SensorSnapshot.Reading.LIGHT;
                value = firstValue(event);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                reading = SensorSnapshot.Reading.ACCELERATION;
                value = event.values.length < 3 ? Double.NaN
                        : Math.sqrt((double) event.values[0] * event.values[0]
                        + (double) event.values[1] * event.values[1]
                        + (double) event.values[2] * event.values[2]);
                break;
            case Sensor.TYPE_PROXIMITY:
                reading = SensorSnapshot.Reading.PROXIMITY;
                value = firstValue(event);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                reading = SensorSnapshot.Reading.AMBIENT_TEMPERATURE;
                value = firstValue(event);
                break;
            default:
                return;
        }
        snapshot.record(reading, value, sampledAt);
    }

    private static double firstValue(SensorEvent event) {
        return event.values.length == 0 ? Double.NaN : event.values[0];
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    public synchronized String obtenerEstadoFisico() {
        if (!isActive()) {
            return "Los sensores están desactivados. Puedes decir «activa sensores»"
                    + " para una sesión temporal de 30 segundos.";
        }
        long now = SystemClock.elapsedRealtime();
        readBattery(now);
        return "Sesión de sensores: quedan " + secondsRemaining(now) + " segundos. "
                + snapshot.describe(now);
    }

    private long secondsRemaining(long now) {
        return (session.remainingMs(now) + 999L) / 1000L;
    }

    private void readBattery(long now) {
        try {
            Intent state = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            snapshot.setAvailable(SensorSnapshot.Reading.BATTERY, state != null);
            if (state != null) {
                snapshot.recordBattery(state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1),
                        state.getIntExtra(BatteryManager.EXTRA_SCALE, -1), now);
            }
        } catch (RuntimeException error) {
            snapshot.setAvailable(SensorSnapshot.Reading.BATTERY, false);
            Log.w(TAG, "No se pudo consultar la batería: " + error.getClass().getSimpleName());
        }
    }

    /** Compatibility for explicit torch controls only; sensor sessions never call this method. */
    public void alterarLuzFisica(boolean encender) {
        try {
            CameraManager cameras = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameras == null) return;
            for (String id : cameras.getCameraIdList()) {
                if (Boolean.TRUE.equals(cameras.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    cameras.setTorchMode(id, encender);
                    return;
                }
            }
        } catch (Exception error) {
            Log.w(TAG, "No se pudo controlar la linterna: " + error.getClass().getSimpleName());
        }
    }
}
