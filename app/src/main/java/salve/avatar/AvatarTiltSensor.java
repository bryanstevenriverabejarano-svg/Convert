package salve.avatar;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Giroscopio para el relieve 3D: encendido solo mientras alguna vista muestra a Salve.
 * No guarda lecturas ni las envía a ningún sitio: cada muestra mueve su punto de vista y se descarta.
 * Sin giroscopio (o si Android no lo entrega) la figura queda de frente, como antes.
 */
final class AvatarTiltSensor implements SensorEventListener {
    private static final AvatarTiltSensor INSTANCE = new AvatarTiltSensor();
    private final AstralParallax parallax = new AstralParallax();
    private SensorManager manager;
    private int users;
    private long lastNanos;

    private AvatarTiltSensor() { }

    static synchronized void acquire(Context context) {
        if (INSTANCE.users++ > 0 || context == null) return;
        try {
            SensorManager sensors = (SensorManager) context.getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            Sensor gyroscope = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscope == null) return;
            INSTANCE.lastNanos = 0;
            INSTANCE.parallax.reset();
            if (sensors.registerListener(INSTANCE, gyroscope, SensorManager.SENSOR_DELAY_GAME)) INSTANCE.manager = sensors;
        } catch (RuntimeException error) {
            Log.w("SalveRelieve", "Sin giroscopio para el relieve: " + error.getClass().getSimpleName());
        }
    }

    static synchronized void release() {
        if (INSTANCE.users == 0 || --INSTANCE.users > 0) return;
        if (INSTANCE.manager != null) {
            try { INSTANCE.manager.unregisterListener(INSTANCE); }
            catch (RuntimeException error) { Log.w("SalveRelieve", "No se pudo soltar el giroscopio"); }
        }
        INSTANCE.manager = null;
        INSTANCE.parallax.reset();
        AstralBody.get().lookFrom(0, 0);
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 2) return;
        long previous = lastNanos;
        lastNanos = event.timestamp;
        if (previous == 0 || event.timestamp <= previous) return;
        parallax.rotate(event.values[0], event.values[1], (event.timestamp - previous) / 1e9f);
        AstralBody.get().lookFrom(parallax.yaw(), parallax.pitch());
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
