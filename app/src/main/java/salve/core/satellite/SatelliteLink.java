package salve.core.satellite;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * La conexión de un satélite con la Salve del dueño, a través de su nodo. El satélite llama hacia
 * fuera por HTTPS al nodo (un servidor con dirección fija en internet), así que funciona estén donde
 * estén los dos móviles: en la misma wifi o en países distintos. Se presenta, late cada poco y, si el
 * nodo le asigna un trabajo que ofrece, lo recoge.
 *
 * <p>No abre ningún puerto en el móvil ni acepta conexiones entrantes: solo sale. Reusa el enlace de
 * emparejamiento del dueño. Borrador: ya se registra y late; ejecutar de verdad cada trabajo (cómputo,
 * pantalla, sensores) es el siguiente paso y exige su permiso en el satélite.</p>
 */
public final class SatelliteLink {
    private static final String TAG = "Salve/Satelite";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false)
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).callTimeout(40, TimeUnit.SECONDS)
            .dns(host -> {
                // El nodo puede estar en internet o en la LAN; nunca en direcciones no enrutables peligrosas.
                List<InetAddress> addresses = Dns.SYSTEM.lookup(host);
                for (InetAddress ip : addresses)
                    if (ip.isAnyLocalAddress() || ip.isMulticastAddress())
                        throw new UnknownHostException("Destino de nodo no admitido");
                return addresses;
            }).build();

    private final SatelliteStore store;
    private final SatelliteCapabilities caps;

    public SatelliteLink(Context context) {
        this.store = new SatelliteStore(context);
        this.caps = SatelliteCapabilities.all(); // los tres usos que eligió el dueño
    }

    /** Se presenta ante el nodo. Devuelve true si el nodo lo aceptó. Nunca lanza. */
    public boolean register() {
        String endpoint = store.parentEndpoint();
        if (endpoint == null) return false;
        JsonObject body = caps.registration(store.satelliteId(), android.os.Build.MODEL);
        JsonObject reply = post(endpoint + SatelliteProtocol.REGISTER, body);
        boolean ok = reply != null;
        Log.i(TAG, ok ? "Registrado en «" + store.parentName() + "»." : "El nodo no respondió al registro.");
        return ok;
    }

    /**
     * Un latido: «sigo aquí y disponible». Si el nodo asigna un trabajo que el satélite ofrece, se
     * devuelve; por ahora el llamador decide qué hacer con él (la ejecución real es el siguiente paso).
     */
    public SatelliteProtocol.Assignment heartbeat(boolean available) {
        String endpoint = store.parentEndpoint();
        if (endpoint == null) return null;
        JsonObject reply = post(endpoint + SatelliteProtocol.HEARTBEAT,
                SatelliteProtocol.heartbeat(store.satelliteId(), available, caps));
        SatelliteProtocol.Assignment task = SatelliteProtocol.parseAssignment(reply);
        if (task != null && !SatelliteProtocol.allowed(task, caps)) {
            Log.i(TAG, "Trabajo «" + task.kind + "» ignorado: esa capacidad no está activada.");
            return null;
        }
        return task;
    }

    private JsonObject post(String url, JsonObject payload) {
        Request request = new Request.Builder().url(url)
                .post(RequestBody.create(payload.toString(), JSON))
                .header("User-Agent", "Salve-Satelite/1.0").build();
        try (Response response = CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            ResponseBody rb = response.body();
            if (rb == null) return null;
            byte[] bytes = rb.bytes();
            if (bytes.length == 0 || bytes.length > 262144) return bytes.length == 0 ? new JsonObject() : null;
            return JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException offline) {
            Log.i(TAG, "Nodo no alcanzable ahora: " + offline.getClass().getSimpleName());
            return null;
        } catch (RuntimeException bad) {
            Log.w(TAG, "Respuesta del nodo no válida", bad);
            return null;
        }
    }
}
