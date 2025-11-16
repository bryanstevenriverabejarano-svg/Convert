package salve.data.util;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Helper estático para subir eventos a tu nube (Namecheap).
 * Se puede llamar desde cualquier parte del proyecto (core, workers, etc.)
 */
public final class CloudLogger {

    // === Config ===
    private static final String NUBE_ENDPOINT = "https://arzenit.com/salve_data.php";
    private static final String NUBE_SECRET   = "pon_aqui_tu_clave_larga"; // <-- cámbialo igual que en el PHP

    private CloudLogger() {}

    /** Enviar un JSON (String) al servidor. */
    private static void enviar(String jsonPayload) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(NUBE_ENDPOINT);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("X-Salve-Token", NUBE_SECRET);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int code = conn.getResponseCode();
                Log.d("CloudLogger", "Respuesta servidor: " + code);
            } catch (Exception e) {
                Log.e("CloudLogger", "Error enviando a la nube", e);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
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
