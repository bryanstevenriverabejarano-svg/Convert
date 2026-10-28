package salve.core.satellite;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.gson.JsonObject;

/**
 * El trabajo de sensores de un satélite: dar una lectura (ubicación, cámara o micrófono) a la Salve
 * del dueño. Regla firme: <b>nunca captura nada sin permiso</b>. Si el permiso del móvil no está
 * concedido, devuelve «permiso_requerido» y no lee nada; no abre la cámara ni el micrófono por su
 * cuenta. La ubicación se da solo como última posición conocida, sin encender un seguimiento en vivo.
 */
public final class SatelliteSensors {
    private static final String TAG = "Salve/Satelite";

    private SatelliteSensors() { }

    public static JsonObject execute(Context context, SatelliteProtocol.Assignment task) {
        String sensor = SatelliteProtocol.sensorKind(task == null ? null : task.params);
        switch (sensor) {
            case "location": return location(context);
            case "camera": case "mic":
                // Cámara y micrófono no se capturan nunca sin una confirmación explícita del satélite.
                return SatelliteProtocol.simpleOutput("permiso_requerido",
                        "Este satélite no abre " + (sensor.equals("camera") ? "la cámara" : "el micrófono") + " sin permiso.");
            default:
                return SatelliteProtocol.simpleOutput("error", "Sensor no reconocido.");
        }
    }

    private static JsonObject location(Context context) {
        boolean granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!granted) return SatelliteProtocol.simpleOutput("permiso_requerido", "Sin permiso de ubicación en este móvil.");
        try {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (manager == null) return SatelliteProtocol.simpleOutput("error", "Este móvil no tiene servicio de ubicación.");
            Location best = null;
            for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
                try {
                    Location last = manager.getLastKnownLocation(provider);
                    if (last != null && (best == null || last.getTime() > best.getTime())) best = last;
                } catch (SecurityException | IllegalArgumentException skip) { /* proveedor no disponible */ }
            }
            if (best == null) return SatelliteProtocol.simpleOutput("sin_dato", "No hay una posición reciente guardada.");
            JsonObject out = new JsonObject();
            out.addProperty("status", "ok");
            out.addProperty("lat", best.getLatitude());
            out.addProperty("lon", best.getLongitude());
            out.addProperty("accuracy_m", best.hasAccuracy() ? best.getAccuracy() : -1f);
            out.addProperty("age_ms", System.currentTimeMillis() - best.getTime());
            return out;
        } catch (RuntimeException failure) {
            Log.w(TAG, "No se pudo leer la ubicación", failure);
            return SatelliteProtocol.simpleOutput("error", "No se pudo leer la ubicación.");
        }
    }
}
