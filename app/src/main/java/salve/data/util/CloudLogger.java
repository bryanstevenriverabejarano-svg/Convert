package salve.data.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;


/**
 * Helper estático para subir eventos a tu nube (Namecheap).
 * Se puede llamar desde cualquier parte del proyecto (core, workers, etc.)
 */
public final class CloudLogger {

    private static volatile Context appContext;

    private CloudLogger() {}

    public static void initialize(Context context) {
        appContext = context == null ? null : context.getApplicationContext();
    }

    private static boolean hasConsent() {
        Context context = appContext;
        return context != null
                && context.getSharedPreferences("salve_privacy", Context.MODE_PRIVATE)
                .getBoolean("cloud_sync_enabled", false);
    }

    /** Enviar un JSON (String) al servidor. */
    private static void enviar(String jsonPayload) {
        if (!hasConsent()) {
            Log.d("CloudLogger", "Evento no enviado: sincronización sin consentimiento.");
            return;
        }
        // Use the durable, consent-gated queue, with no separate network transport.
        salve.data.sync.CloudSyncManager.enqueue(appContext, jsonPayload);
    }

    /** Conveniencia: construir y enviar un JSON estándar. */
    public static void log(String tipo, String contenido, Integer emocion) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", tipo);
            if (contenido != null) obj.put("content", contenido);
            if (emocion != null) obj.put("emotion", emocion);
            obj.put("time_ms", System.currentTimeMillis());
            enviar(obj.toString());
        } catch (Exception e) {
            Log.e("CloudLogger", "Error creando JSON", e);
        }
    }

    /** Overload simple sin emoción. */
    public static void log(String tipo, String contenido) {
        log(tipo, contenido, null);
    }

    /** Overload mínimo: solo tipo. */
    public static void log(String tipo) {
        log(tipo, null, null);
    }
}
