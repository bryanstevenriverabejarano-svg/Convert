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

import salve.core.cognition.DeviceControlIntent;
import salve.core.sensors.DeviceSenses;
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
    public void alterarLuzFisica(boolean encender) { linterna(encender); }

    /** @return true si Android aceptó la orden en una cámara con flash. */
    private boolean linterna(boolean encender) {
        try {
            CameraManager cameras = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameras == null) return false;
            for (String id : cameras.getCameraIdList()) {
                if (Boolean.TRUE.equals(cameras.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                    cameras.setTorchMode(id, encender);
                    return true;
                }
            }
        } catch (Exception error) {
            Log.w(TAG, "No se pudo controlar la linterna: " + error.getClass().getSimpleName());
        }
        return false;
    }

    // ---------------------------------------------------------------- percepción completa (ráfaga)

    /** Lo que espera como mucho una ráfaga; el contador de pasos, que puede no contestar, menos. */
    private static final long BURST_MS = 700L, STEP_ONLY_MS = 250L;

    /**
     * Mide una vez con todos los sensores del grupo y devuelve el texto interpretado. Es una ráfaga:
     * registra, espera como mucho {@value #BURST_MS} ms, suelta los sensores y no conserva nada.
     * Independiente de las sesiones de {@link #iniciarSesion(long)}. No debe llamarse desde el hilo principal
     * (ahí solo devuelve lo que no necesita esperar).
     */
    public String percibir(DeviceSenses.Group requested) {
        DeviceSenses.Group group = requested == null ? DeviceSenses.Group.TODO : requested;
        DeviceSenses.Perception perception = new DeviceSenses.Perception();
        boolean all = group == DeviceSenses.Group.TODO;
        if (all || group == DeviceSenses.Group.ENTORNO || group == DeviceSenses.Group.MOVIMIENTO
                || group == DeviceSenses.Group.ORIENTACION) burst(perception, group);
        if (all || group == DeviceSenses.Group.ENERGIA) energy(perception);
        if (all || group == DeviceSenses.Group.SISTEMA) system(perception);
        if (all || group == DeviceSenses.Group.INVENTARIO) inventory(perception);
        return perception.render(group);
    }

    private void burst(DeviceSenses.Perception perception, DeviceSenses.Group group) {
        if (sensorManager == null) return;
        boolean all = group == DeviceSenses.Group.TODO;
        final java.util.Set<Integer> pending = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
        final Object lock = new Object();
        SensorEventListener listener = new SensorEventListener() {
            @Override public void onSensorChanged(SensorEvent event) {
                if (event == null || event.sensor == null || event.values == null) return;
                int type = event.sensor.getType();
                if (type == Sensor.TYPE_ROTATION_VECTOR) {
                    try {
                        float[] rotation = new float[9], angles = new float[3];
                        float[] vector = event.values.length > 4 ? java.util.Arrays.copyOf(event.values, 4) : event.values;
                        SensorManager.getRotationMatrixFromVector(rotation, vector);
                        SensorManager.getOrientation(rotation, angles);
                        perception.heading(true, (float) Math.toDegrees(angles[0]));
                    } catch (RuntimeException invalid) { return; }
                } else perception.sample(DeviceSenses.Kind.of(type), event.values);
                pending.remove(type);
                synchronized (lock) { lock.notifyAll(); }
            }
            @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
        };
        android.os.HandlerThread thread = new android.os.HandlerThread("salve-sentidos");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        try {
            for (DeviceSenses.Kind kind : DeviceSenses.Kind.values()) {
                if (!all && kind.group != group) continue;
                perception.present(kind, register(listener, kind.type, handler, pending));
            }
            if (all || group == DeviceSenses.Group.ORIENTACION)
                perception.heading(register(listener, Sensor.TYPE_ROTATION_VECTOR, handler, pending), Float.NaN);
            if (Looper.myLooper() != Looper.getMainLooper()) {
                long start = SystemClock.elapsedRealtime();
                synchronized (lock) {
                    while (!pending.isEmpty()) {
                        long waited = SystemClock.elapsedRealtime() - start;
                        boolean onlySteps = pending.size() == 1 && pending.contains(Sensor.TYPE_STEP_COUNTER);
                        long left = (onlySteps ? STEP_ONLY_MS : BURST_MS) - waited;
                        if (left <= 0) break;
                        try { lock.wait(left); }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                    }
                }
            }
        } finally {
            try { sensorManager.unregisterListener(listener); }
            catch (RuntimeException error) { Log.w(TAG, "No se pudo soltar la ráfaga: " + error.getClass().getSimpleName()); }
            thread.quitSafely();
        }
    }

    private boolean register(SensorEventListener listener, int type, Handler handler, java.util.Set<Integer> pending) {
        try {
            Sensor sensor = sensorManager.getDefaultSensor(type);
            if (sensor == null) return false;
            pending.add(type);
            if (sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME, handler)) return true;
            pending.remove(type);
        } catch (RuntimeException error) {
            pending.remove(type);
            Log.w(TAG, "Sensor no registrado en la ráfaga: " + error.getClass().getSimpleName());
        }
        return false;
    }

    private void energy(DeviceSenses.Perception perception) {
        try {
            Intent state = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (state == null) { perception.fact(DeviceSenses.Group.ENERGIA, "Batería: Android no entregó su estado."); return; }
            int level = state.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = state.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int plugged = state.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            int status = state.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            String source = plugged == BatteryManager.BATTERY_PLUGGED_AC ? "enchufado a la corriente"
                    : plugged == BatteryManager.BATTERY_PLUGGED_USB ? "conectado por USB"
                    : plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS ? "en carga inalámbrica" : "sin enchufar";
            String charge = status == BatteryManager.BATTERY_STATUS_FULL ? "llena"
                    : status == BatteryManager.BATTERY_STATUS_CHARGING ? "cargando" : "descargándose";
            if (level >= 0 && scale > 0) perception.fact(DeviceSenses.Group.ENERGIA,
                    "Batería: " + Math.round(level * 100f / scale) + " % (" + charge + ", " + source + ").");
            int tenths = state.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
            if (tenths != Integer.MIN_VALUE) perception.fact(DeviceSenses.Group.ENERGIA,
                    String.format(java.util.Locale.ROOT, "Temperatura de la batería: %.1f °C.", tenths / 10f));
            int millivolts = state.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            if (millivolts > 0) perception.fact(DeviceSenses.Group.ENERGIA,
                    String.format(java.util.Locale.ROOT, "Voltaje: %.2f V.", millivolts / 1000f));
            android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power != null && power.isPowerSaveMode()) perception.fact(DeviceSenses.Group.ENERGIA, "Ahorro de energía activado.");
        } catch (RuntimeException error) {
            Log.w(TAG, "No se pudo leer la energía: " + error.getClass().getSimpleName());
        }
    }

    private void system(DeviceSenses.Perception perception) {
        DeviceSenses.Group g = DeviceSenses.Group.SISTEMA;
        perception.fact(g, "Teléfono: " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                + ", Android " + android.os.Build.VERSION.RELEASE + ".");
        try {
            android.app.ActivityManager activity = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo memory = new android.app.ActivityManager.MemoryInfo();
            if (activity != null) {
                activity.getMemoryInfo(memory);
                perception.fact(g, String.format(java.util.Locale.ROOT, "RAM libre: %.1f de %.1f GB%s.",
                        memory.availMem / 1e9, memory.totalMem / 1e9, memory.lowMemory ? " (Android avisa de poca memoria)" : ""));
            }
        } catch (RuntimeException error) { Log.w(TAG, "RAM no disponible"); }
        try {
            android.os.StatFs disk = new android.os.StatFs(context.getFilesDir().getPath());
            perception.fact(g, String.format(java.util.Locale.ROOT, "Almacenamiento libre: %.1f de %.1f GB.",
                    disk.getAvailableBytes() / 1e9, disk.getTotalBytes() / 1e9));
        } catch (RuntimeException error) { Log.w(TAG, "Almacenamiento no disponible"); }
        try {
            android.net.ConnectivityManager net = (android.net.ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkCapabilities caps = net == null ? null : net.getNetworkCapabilities(net.getActiveNetwork());
            if (caps == null) perception.fact(g, "Red: sin conexión.");
            else perception.fact(g, "Red: " + (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ? "wifi"
                    : caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ? "datos móviles"
                    : caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) ? "cable" : "otra")
                    + (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) ? " con VPN" : "")
                    + (caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    ? ", con salida a internet." : ", sin salida a internet comprobada."));
        } catch (RuntimeException error) { Log.w(TAG, "Red no disponible"); }
        try {
            android.media.AudioManager audio = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audio != null) {
                int max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
                int mode = audio.getRingerMode();
                perception.fact(g, "Volumen multimedia: " + percent(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC), max)
                        + " %; timbre " + (mode == android.media.AudioManager.RINGER_MODE_SILENT ? "en silencio"
                        : mode == android.media.AudioManager.RINGER_MODE_VIBRATE ? "en vibración" : "con sonido") + ".");
            }
        } catch (RuntimeException error) { Log.w(TAG, "Volumen no disponible"); }
        try {
            android.content.ContentResolver settings = context.getContentResolver();
            int brightness = android.provider.Settings.System.getInt(settings, android.provider.Settings.System.SCREEN_BRIGHTNESS, -1);
            boolean automatic = android.provider.Settings.System.getInt(settings, android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE,
                    android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    == android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            if (brightness >= 0) perception.fact(g, "Brillo de pantalla: nivel " + brightness + " de 255 aprox."
                    + (automatic ? " (automático)" : "") + ".");
        } catch (RuntimeException error) { Log.w(TAG, "Brillo no disponible"); }
        try {
            android.os.PowerManager power = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power != null) {
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    int thermal = power.getCurrentThermalStatus();
                    perception.fact(g, "Calor: " + (thermal <= android.os.PowerManager.THERMAL_STATUS_NONE ? "sin estrés térmico"
                            : thermal == android.os.PowerManager.THERMAL_STATUS_LIGHT ? "estrés térmico ligero"
                            : thermal == android.os.PowerManager.THERMAL_STATUS_MODERATE ? "estrés térmico moderado"
                            : "estrés térmico alto: conviene dejarlo descansar") + ".");
                }
                perception.fact(g, power.isInteractive() ? "Pantalla encendida." : "Pantalla apagada.");
            }
        } catch (RuntimeException error) { Log.w(TAG, "Estado térmico no disponible"); }
        perception.fact(g, String.format(java.util.Locale.ROOT, "Encendido desde hace %.1f h.",
                SystemClock.elapsedRealtime() / 3_600_000.0));
    }

    private void inventory(DeviceSenses.Perception perception) {
        if (sensorManager == null) return;
        try {
            for (Sensor sensor : sensorManager.getSensorList(Sensor.TYPE_ALL)) perception.inventory(sensor.getName());
        } catch (RuntimeException error) { Log.w(TAG, "Inventario de sensores no disponible"); }
    }

    private static int percent(int value, int max) { return max <= 0 ? 0 : Math.round(value * 100f / max); }

    // ---------------------------------------------------------------- mandos físicos

    /**
     * Acciona un mando local y reversible (linterna, volumen, vibración) y devuelve lo que Android
     * informa después. El llamador ya verificó identidad y que la acción sale de la frase del usuario.
     */
    public String accionar(DeviceControlIntent.Command command) {
        if (command == null) return "MANDO_RECHAZADO: sin orden.";
        try {
            switch (command.action) {
                case TORCH_ON:
                case TORCH_OFF: {
                    boolean on = command.action == DeviceControlIntent.Action.TORCH_ON;
                    return linterna(on) ? "LINTERNA: Android aceptó " + (on ? "encenderla" : "apagarla") + "."
                            : "LINTERNA_SIN_CAMBIO: Android no me dejó usar el flash (¿la cámara está en uso?).";
                }
                case VIBRATE: {
                    android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator == null || !vibrator.hasVibrator()) return "VIBRACION_SIN_MOTOR: este teléfono no tiene vibrador.";
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(350L, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    return "VIBRACION: pedí una vibración de 0,35 s; en modo no molestar Android puede silenciarla.";
                }
                default: {
                    android.media.AudioManager audio = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    if (audio == null || audio.isVolumeFixed()) return "VOLUMEN_FIJO: este dispositivo no deja cambiar el volumen.";
                    int stream = android.media.AudioManager.STREAM_MUSIC, max = audio.getStreamMaxVolume(stream);
                    int before = audio.getStreamVolume(stream);
                    audio.setStreamVolume(stream, DeviceControlIntent.targetVolume(command, before, max),
                            android.media.AudioManager.FLAG_SHOW_UI);
                    int after = audio.getStreamVolume(stream);
                    return "VOLUMEN_REAL: multimedia antes " + percent(before, max) + " %, ahora " + percent(after, max) + " %"
                            + (after == before ? " (sin cambio: ya estaba en ese nivel o Android lo impidió)." : ".");
                }
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Mando no ejecutado: " + error.getClass().getSimpleName());
            return "MANDO_FALLIDO: Android rechazó la orden; no hay cambio confirmado.";
        }
    }
}
