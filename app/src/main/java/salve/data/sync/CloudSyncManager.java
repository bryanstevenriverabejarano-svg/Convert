package salve.data.sync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import salve.data.db.MemoriaDatabase;
import salve.data.db.SyncEventDao;
import salve.data.db.SyncEventEntity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CloudSyncManager
 * Gestor de sincronización OFFLINE-FIRST:
 *  - enqueue(...) guarda eventos en Room (tabla sync_events)
 *  - flush(...) intenta enviarlos al servidor y limpia los enviados
 *
 * Además:
 *  - uploadGrafoBundle(...) sube graph.json, memories_index.json y viewer.html por multipart
 */
public final class CloudSyncManager {

    private static final String TAG = "CloudSync";

    // === Configura estos dos valores (mismos que en tu PHP) ===
    private static final String ENDPOINT = "https://arzenit.com/salve_data.php";
    private static final String SECRET   = "pon_aqui_tu_clave_larga";

    // timeouts
    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS    = 30000;

    private CloudSyncManager() {}

    // --------------------------------------------------------------------
    // ENCOLAR (OFFLINE) - EVENTOS JSON
    // --------------------------------------------------------------------

    /** Guarda directamente un JSON en la cola local (Room). */
    public static void enqueue(Context ctx, String jsonPayload) {
        try {
            MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
            SyncEventDao dao   = db.syncEventDao();

            SyncEventEntity e = new SyncEventEntity();
            e.payload   = jsonPayload;
            e.createdAt = System.currentTimeMillis();
            e.tries     = 0;

            dao.insert(e);
        } catch (Exception ex) {
            Log.e(TAG, "enqueue error", ex);
        }
    }

    /** Helper: construye un JSON estándar y lo encola. */
    public static void enqueueStandard(Context ctx, String type, String content, Integer emotion) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            if (content != null) obj.put("content", content);
            if (emotion != null) obj.put("emotion", emotion);
            obj.put("time_ms", System.currentTimeMillis());
            enqueue(ctx, obj.toString());
        } catch (Exception ignore) {}
    }

    // --------------------------------------------------------------------
    // FLUSH (ENVÍO PENDIENTES) - EVENTOS JSON
    // --------------------------------------------------------------------

    /**
     * Intenta enviar hasta maxBatch eventos pendientes.
     * Devuelve cuántos envió correctamente (2xx).
     */
    public static int flush(Context ctx, int maxBatch) {
        int sent = 0;
        try {
            MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
            SyncEventDao dao   = db.syncEventDao();

            List<SyncEventEntity> batch = dao.getPending(maxBatch);
            for (SyncEventEntity e : batch) {
                if (sendNow(e.payload)) {
                    dao.delete(e);       // éxito → borrar
                    sent++;
                } else {
                    dao.incTries(e.id);  // fallo → reintentar luego
                }
            }

            // Protección: purga los que fallaron demasiadas veces
            dao.purgeFailed(20);

            Log.d(TAG, "flush: enviados=" + sent + " / batch=" + batch.size());
        } catch (Exception ex) {
            Log.e(TAG, "flush error", ex);
        }
        return sent;
    }

    // --------------------------------------------------------------------
    // HTTP POST (JSON)
    // --------------------------------------------------------------------

    /** Envía ahora un JSON al endpoint. True si la respuesta es 2xx. */
    private static boolean sendNow(String jsonPayload) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("X-Salve-Token", SECRET);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = conn.getResponseCode();
            return (code >= 200 && code < 300);
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // --------------------------------------------------------------------
    // SUBIDA DE ARCHIVOS (MULTIPART) - GRAFO / INDEX / VIEWER
    // --------------------------------------------------------------------

    /**
     * Sube, si existen, los archivos del grafo buscando primero en:
     *   1) getExternalFilesDir(DIRECTORY_DOCUMENTS)/recuerdos/   (recomendado, Android 10+)
     *   2) /storage/emulated/0/Salve/recuerdos/                  (legacy)
     *
     * No bloquea el hilo principal: cada archivo se sube en un hilo.
     */
    public static void uploadGrafoBundle(Context ctx) {
        // Preferido (scoped storage friendly)
        File appBase = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "recuerdos");
        // Legacy (por compatibilidad si ya guardas ahí)
        File legacyBase = new File(Environment.getExternalStorageDirectory(), "/Salve/recuerdos/");

        File graph  = firstExisting(new File(appBase, "graph.json"),  new File(legacyBase, "graph.json"));
        File index  = firstExisting(new File(appBase, "memories_index.json"), new File(legacyBase, "memories_index.json"));
        File viewer = firstExisting(new File(appBase, "viewer.html"), new File(legacyBase, "viewer.html"));

        if (!existsAny(graph, index, viewer)) {
            Log.w(TAG, "uploadGrafoBundle: no hay archivos para subir en appBase ni legacyBase.");
            return;
        }

        if (graph  != null) uploadFileAsync(graph,  "graph");
        if (index  != null) uploadFileAsync(index,  "index");
        if (viewer != null) uploadFileAsync(viewer, "viewer");
    }

    /** Overload: por si ya tienes los File generados y quieres subir directo. */
    public static void uploadGrafoBundle(Context ctx, File graph, File index, File viewer) {
        if (!existsAny(graph, index, viewer)) {
            Log.w(TAG, "uploadGrafoBundle(ctx,files): no hay archivos para subir.");
            return;
        }
        if (graph  != null && graph.exists())  uploadFileAsync(graph,  "graph");
        if (index  != null && index.exists())  uploadFileAsync(index,  "index");
        if (viewer != null && viewer.exists()) uploadFileAsync(viewer, "viewer");
    }

    private static File firstExisting(File a, File b) {
        if (a != null && a.exists()) return a;
        if (b != null && b.exists()) return b;
        return null;
    }

    private static boolean existsAny(File... files) {
        if (files == null) return false;
        for (File f : files) if (f != null && f.exists()) return true;
        return false;
    }

    /** Lanza subida en background de un archivo (multipart). */
    private static void uploadFileAsync(File file, String kind) {
        new Thread(() -> {
            try {
                boolean ok = uploadFileMultipart(file, kind);
                Log.d(TAG, "upload " + kind + " (" + file.getAbsolutePath() + "): " + (ok ? "OK" : "FAIL"));
            } catch (Exception e) {
                Log.e(TAG, "upload error " + file.getAbsolutePath(), e);
            }
        }).start();
    }

    /**
     * Subida multipart/form-data al mismo ENDPOINT con header X-Salve-Token.
     * El servidor PHP debe aceptar:
     *   - $_POST['kind'] (graph | index | viewer)
     *   - $_FILES['file']
     */
    private static boolean uploadFileMultipart(File file, String kind) throws Exception {
        String boundary = "----SalveBoundary" + System.currentTimeMillis();
        HttpURLConnection conn = null;
        try {
            URL url = new URL(ENDPOINT);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("X-Salve-Token", SECRET);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (DataOutputStream out = new DataOutputStream(conn.getOutputStream());
                 FileInputStream fis = new FileInputStream(file)) {

                // Campo 'kind'
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"kind\"\r\n\r\n");
                out.writeBytes(kind + "\r\n");

                // Campo 'file'
                out.writeBytes("--" + boundary + "\r\n");
                out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n");
                out.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

                byte[] buf = new byte[8192];
                int len;
                while ((len = fis.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.writeBytes("\r\n--" + boundary + "--\r\n");
                out.flush();
            }

            int code = conn.getResponseCode();
            boolean ok = (code >= 200 && code < 300);

            // Log de respuesta para depuración
            InputStream is = ok ? conn.getInputStream() : conn.getErrorStream();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                    Log.d(TAG, "server says (" + kind + "): " + sb);
                }
            }
            return ok;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
