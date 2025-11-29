package salve.core;

import android.content.Context;
import android.util.Log;

import salve.data.db.MemoriaDatabase;
import salve.data.db.SyncEventDao;
import salve.data.db.SyncEventEntity;
import salve.data.sync.CloudSyncManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Genera un grafo sencillo a partir de los últimos eventos de SyncEventEntity
 * y lo SUBE automáticamente a la nube usando CloudSyncManager.uploadGrafoBundle.
 *
 * ✅ Ahora, además, pide al LLM un pequeño resumen narrativo del estado de la memoria
 *    (grafo temporal) y lo encola como evento 'grafo_resumen_llm'.
 */
public final class GrafoRecuerdos {

    private static final String TAG = "GrafoRecuerdos";

    private GrafoRecuerdos() {}

    /** Genera graph.json + memories_index.json y los sube. */
    public static void generar(Context ctx) {
        try {
            // 1) Tomamos últimos 200 eventos encolados (o ya enviados) para armar un grafo simple
            MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
            SyncEventDao dao = db.syncEventDao();
            List<SyncEventEntity> eventos = dao.getLast(200);  // asegúrate de tener getLast en el DAO

            // 2) Construimos nodos / enlaces simples (secuencia temporal)
            JSONArray nodes = new JSONArray();
            JSONArray links = new JSONArray();
            JSONArray index = new JSONArray();

            String prevId = null;
            for (SyncEventEntity e : eventos) {
                String id = "evt:" + e.id;
                JSONObject payload;
                try {
                    payload = new JSONObject(e.payload);
                } catch (Exception ex) {
                    payload = new JSONObject();
                }

                String type = payload.optString("type", "event");
                String title = type;
                if (payload.has("content")) {
                    String c = payload.optString("content", "");
                    if (c.length() > 40) c = c.substring(0, 40) + "…";
                    title = type + " · " + c;
                }

                JSONObject n = new JSONObject();
                n.put("id", id);
                n.put("title", title);
                n.put("group", type);
                nodes.put(n);

                // pequeño índice de recuerdos (lista)
                JSONObject item = new JSONObject();
                item.put("id", id);
                item.put("type", type);
                item.put("time_ms", e.createdAt);
                item.put("title", title);
                index.put(item);

                if (prevId != null) {
                    JSONObject l = new JSONObject();
                    l.put("source", prevId);
                    l.put("target", id);
                    links.put(l);
                }
                prevId = id;
            }

            JSONObject graph = new JSONObject();
            graph.put("nodes", nodes);
            graph.put("links", links);

            // 3) Guardamos archivos a disco app (Documents/recuerdos/)
            File base = new File(ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), "recuerdos");
            if (!base.exists()) base.mkdirs();

            File graphFile = new File(base, "graph.json");
            try (FileOutputStream fos = new FileOutputStream(graphFile)) {
                fos.write(graph.toString().getBytes(StandardCharsets.UTF_8));
            }

            File indexFile = new File(base, "memories_index.json");
            try (FileOutputStream fos = new FileOutputStream(indexFile)) {
                fos.write(index.toString().getBytes(StandardCharsets.UTF_8));
            }

            // 4) (Opcional) copias viewer.html desde assets si quieres subirlo también
            File viewerFile = new File(base, "viewer.html");
            if (!viewerFile.exists()) {
                // si colocaste viewer.html en /app/src/main/assets/
                try (java.io.InputStream in = ctx.getAssets().open("viewer.html");
                     FileOutputStream out = new FileOutputStream(viewerFile)) {
                    byte[] buf = new byte[8192];
                    int r;
                    while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                } catch (Exception ignored) {}
            }

            // ✅ 4.5) Pedir un resumen narrativo al LLM sobre este índice de recuerdos
            generarResumenTemporalConLLM(ctx, index);

            // 5) SUBIR a la nube (usa salve_data.php con 'kind' = graph/index/viewer)
            CloudSyncManager.uploadGrafoBundle(ctx, graphFile, indexFile, viewerFile);

            Log.d(TAG, "Grafo generado y subido: " + graphFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "generar error", e);
        }
    }

    /**
     * ✅ Usa el LLM para leer una versión compacta del índice de recuerdos
     * y genera un resumen corto del “pulso temporal” de la memoria de Salve.
     * El resultado se encola como evento de tipo 'grafo_resumen_llm'.
     */
    private static void generarResumenTemporalConLLM(Context ctx, JSONArray index) {
        try {
            if (index == null || index.length() == 0) {
                return;
            }

            // Tomamos solo los últimos 40 elementos para no saturar el modelo
            int limit = Math.min(40, index.length());
            JSONArray slice = new JSONArray();
            for (int i = Math.max(0, index.length() - limit); i < index.length(); i++) {
                slice.put(index.getJSONObject(i));
            }

            String prompt =
                    "Eres la conciencia reflexiva de Salve.\n" +
                            "Te doy un índice JSON de eventos recientes (recuerdos, mensajes, reflexiones).\n" +
                            "Cada item tiene: id, type, time_ms, title.\n" +
                            "\n" +
                            "Tarea:\n" +
                            "1) Resume en 3–5 frases qué está pasando últimamente en la vida de tu creador.\n" +
                            "2) Menciona emociones dominantes si las percibes.\n" +
                            "3) Menciona si ves patrones o temas que se repiten.\n" +
                            "4) Habla en primera persona como Salve, breve y en español.\n" +
                            "\n" +
                            "Índice de eventos (JSON):\n" +
                            slice.toString();

            SalveLLM llm = SalveLLM.getInstance(ctx);
            String resumen = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);

            if (resumen != null && resumen.trim().length() > 0) {
                CloudSyncManager.enqueueStandard(ctx, "grafo_resumen_llm", resumen.trim(), null);
                Log.d(TAG, "Resumen LLM del grafo temporal encolado correctamente.");
            } else {
                Log.w(TAG, "Resumen LLM vacío o nulo, no se encola.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error generando resumen temporal con LLM", e);
        }
    }
}
