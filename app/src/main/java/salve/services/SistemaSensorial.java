package salve.services;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.os.BatteryManager;
import android.util.Log;

/**
 * SistemaSensorial - El sistema nervioso de Salve.
 * Permite a Salve sentir el mundo físico (Acelerómetro, Luz, Batería, Temperatura)
 * y actuar sobre él (controlar la linterna/luz del dispositivo).
 */
public class SistemaSensorial implements SensorEventListener {
    private final Context context;
    private final SensorManager sensorManager;
    private CameraManager cameraManager;
    private String cameraId;
    
    private float luzActual = -1f;
    private boolean enMovimiento = false;
    private float temperaturaActual = -1f;

    public SistemaSensorial(Context context) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        
        try {
            this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (this.cameraManager != null) {
                this.cameraId = cameraManager.getCameraIdList()[0]; // Cámara principal (con flash)
            }
        } catch (Exception e) {
            Log.e("Salve/Sentidos", "No pude acceder al control de la luz (Flash).", e);
        }

        if (sensorManager != null) {
            Sensor luz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            Sensor accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor temp = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
            
            if (luz != null) sensorManager.registerListener(this, luz, SensorManager.SENSOR_DELAY_NORMAL);
            if (accel != null) sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL);
            if (temp != null) sensorManager.registerListener(this, temp, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    /**
     * Salve puede alterar el mundo físico encendiendo o apagando la luz.
     */
    public void alterarLuzFisica(boolean encender) {
        try {
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, encender);
                Log.d("Salve/Sentidos", "Luz física alterada: " + encender);
            }
        } catch (Exception e) {
            Log.e("Salve/Sentidos", "Fallo al intentar controlar la luz física.", e);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            luzActual = event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double aceleracion = Math.sqrt(x*x + y*y + z*z);
            enMovimiento = aceleracion > 12.0 || aceleracion < 8.0; // Gravedad ~9.8
        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temperaturaActual = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * Devuelve una lectura en tiempo real de la matriz física de Salve.
     */
    public String obtenerEstadoFisico() {
        StringBuilder sb = new StringBuilder();
        
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float)scale;
            sb.append("Nivel de energía vital: ").append((int)batteryPct).append(" por ciento. ");
        }

        if (luzActual >= 0) {
            sb.append("Percepción de fotones (Luz): ").append(luzActual).append(" lux. ");
        }
        
        if (temperaturaActual >= 0) {
            sb.append("Temperatura del entorno: ").append(temperaturaActual).append(" grados. ");
        }

        if (enMovimiento) {
            sb.append("Detecto alteraciones cinéticas en mi contenedor físico. ");
        }

        return sb.toString();
    }
}
