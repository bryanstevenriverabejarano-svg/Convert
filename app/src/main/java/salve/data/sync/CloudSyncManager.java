package salve.data.sync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import salve.data.db.MemoriaDatabase;
import salve.data.db.SyncEventDao;
import salve.data.db.SyncEventEntity;

/**
 * Offline-first synchronization coordinator.
 *
 * Local memories/events remain local. The sync table is an outbox plus local event journal:
 * successful pCloud uploads are marked synchronized, not deleted.
 */
public final class CloudSyncManager {
    private static final String TAG = "CloudSync";
    private static final String PRIVACY_PREFS = "salve_privacy";
    private static final String CLOUD_SYNC_ENABLED = "cloud_sync_enabled";
    private static final String ROOT = "/Salve";

    private CloudSyncManager() {}

    public static boolean isEnabled(Context ctx) {
        return ctx != null && ctx.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE)
                .getBoolean(CLOUD_SYNC_ENABLED, false);
    }

    /** OAuth completion should call configurePCloud once, then enable sync from the visible user action. */
    public static void configurePCloud(Context ctx, String accessToken, String apiHost) {
        PCloudProvider.configureOAuth(ctx, accessToken, apiHost);
    }

    public static boolean isPCloudConfigured(Context ctx) {
        return ctx != null && new PCloudProvider(ctx).isConfigured();
    }

    public static void setEnabled(Context ctx, boolean enabled) {
        if (ctx == null) return;
        ctx.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(CLOUD_SYNC_ENABLED, enabled).apply();
        if (enabled) SyncWorker.enqueueWhenOnline(ctx.getApplicationContext());
    }

    public static void enqueue(Context ctx, String jsonPayload) {
        if (!isEnabled(ctx) || jsonPayload == null || jsonPayload.trim().isEmpty()) return;
        Context app = ctx.getApplicationContext();
        new Thread(() -> {
            try {
                SyncEventDao dao = MemoriaDatabase.getInstance(app).syncEventDao();
                SyncEventEntity e = new SyncEventEntity();
                e.payload = jsonPayload;
                e.createdAt = System.currentTimeMillis();
                e.tries = 0;
                dao.insert(e);
                SyncWorker.enqueueWhenOnline(app);
            } catch (Exception ex) {
                Log.e(TAG, "enqueue error", ex);
            }
        }).start();
    }

    public static void enqueueStandard(Context ctx, String type, String content, Integer emotion) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("type", type);
            if (content != null) obj.put("content", content);
            if (emotion != null) obj.put("emotion", emotion);
            obj.put("time_ms", System.currentTimeMillis());
            enqueue(ctx, obj.toString());
        } catch (Exception e) {
            Log.e(TAG, "No se pudo construir evento cloud", e);
        }
    }

    /**
     * Sends pending events to pCloud. Success means pCloud accepted the file and its remote
     * checksum matched the local payload. The local journal entry is retained with tries=-1.
     */
    public static int flush(Context ctx, int maxBatch) {
        if (!isEnabled(ctx)) return 0;
        PCloudProvider provider = new PCloudProvider(ctx);
        if (!provider.isConfigured()) return 0;

        int sent = 0;
        try {
            SyncEventDao dao = MemoriaDatabase.getInstance(ctx).syncEventDao();
            List<SyncEventEntity> batch = dao.getPending(maxBatch);
            for (SyncEventEntity e : batch) {
                String path = ROOT + "/events/" + e.createdAt + "-" + e.id + ".json";
                if (provider.uploadJson(path, e.payload)) {
                    dao.markSynced(e.id);
                    sent++;
                } else {
                    dao.incTries(e.id);
                }
            }
            Log.d(TAG, "pCloud flush verificado: " + sent + " / " + batch.size());
        } catch (Exception ex) {
            Log.e(TAG, "flush error", ex);
        }
        return sent;
    }

    public static void uploadGrafoBundle(Context ctx) {
        if (!isEnabled(ctx)) return;
        File appBase = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "recuerdos");
        File legacyBase = new File(Environment.getExternalStorageDirectory(), "/Salve/recuerdos/");
        uploadGrafoBundle(ctx,
                firstExisting(new File(appBase, "graph.json"), new File(legacyBase, "graph.json")),
                firstExisting(new File(appBase, "memories_index.json"), new File(legacyBase, "memories_index.json")),
                firstExisting(new File(appBase, "viewer.html"), new File(legacyBase, "viewer.html")));
    }

    public static void uploadGrafoBundle(Context ctx, File graph, File index, File viewer) {
        if (!isEnabled(ctx)) return;
        PCloudProvider provider = new PCloudProvider(ctx);
        if (!provider.isConfigured()) return;
        if (graph != null && graph.isFile()) uploadAsync(provider, graph, ROOT + "/graphs/graph.json");
        if (index != null && index.isFile()) uploadAsync(provider, index, ROOT + "/memory/memories_index.json");
        if (viewer != null && viewer.isFile()) uploadAsync(provider, viewer, ROOT + "/graphs/viewer.html");
    }

    /** Restore synchronized event journal entries without duplicating existing local rows. */
    public static int restoreEvents(Context ctx, int maxEvents) {
        if (!isEnabled(ctx) || maxEvents <= 0) return 0;
        PCloudProvider provider = new PCloudProvider(ctx);
        if (!provider.isConfigured()) return 0;
        try {
            SyncEventDao dao = MemoriaDatabase.getInstance(ctx).syncEventDao();
            List<String> files = provider.listFiles(ROOT + "/events");
            int restored = 0;
            for (int i = Math.max(0, files.size() - maxEvents); i < files.size(); i++) {
                String remote = files.get(i);
                byte[] bytes = provider.download(remote);
                if (bytes == null) continue;
                String payload = new String(bytes, StandardCharsets.UTF_8);
                long createdAt = timestampFromEventPath(remote);
                if (createdAt <= 0) {
                    try { createdAt = new JSONObject(payload).optLong("time_ms", 0L); }
                    catch (Exception ignored) {}
                }
                if (createdAt <= 0) continue;
                if (dao.countExact(createdAt, payload) > 0) continue;
                SyncEventEntity e = new SyncEventEntity();
                e.payload = payload;
                e.createdAt = createdAt;
                e.tries = -1;
                dao.insert(e);
                restored++;
            }
            return restored;
        } catch (Exception e) {
            Log.e(TAG, "restoreEvents error", e);
            return 0;
        }
    }

    /** Restore graph/index/viewer plus event journal from pCloud. */
    public static int restoreCoreMemory(Context ctx, int maxEvents) {
        if (ctx == null) return 0;
        File base = new File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "recuerdos");
        int restored = restoreEvents(ctx, maxEvents);
        if (restoreTextArtifact(ctx, ROOT + "/graphs/graph.json", new File(base, "graph.json"))) restored++;
        if (restoreTextArtifact(ctx, ROOT + "/memory/memories_index.json", new File(base, "memories_index.json"))) restored++;
        if (restoreTextArtifact(ctx, ROOT + "/graphs/viewer.html", new File(base, "viewer.html"))) restored++;
        return restored;
    }

    /** Restore one text artifact into app-private external storage after an authenticated read. */
    public static boolean restoreTextArtifact(Context ctx, String remotePath, File localTarget) {
        if (!isEnabled(ctx) || localTarget == null) return false;
        PCloudProvider provider = new PCloudProvider(ctx);
        byte[] data = provider.download(remotePath);
        if (data == null) return false;
        try {
            File parent = localTarget.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) return false;
            try (FileOutputStream out = new FileOutputStream(localTarget)) {
                out.write(data);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "restore error", e);
            return false;
        }
    }

    static long timestampFromEventPath(String path) {
        if (path == null) return 0L;
        try {
            String name = path.substring(path.lastIndexOf('/') + 1);
            int dash = name.indexOf('-');
            if (dash <= 0) return 0L;
            return Long.parseLong(name.substring(0, dash));
        } catch (Exception e) {
            return 0L;
        }
    }

    private static void uploadAsync(PCloudProvider provider, File file, String remotePath) {
        new Thread(() -> {
            boolean ok = provider.uploadFile(remotePath, file);
            Log.d(TAG, "pCloud " + remotePath + ": " + (ok ? "VERIFIED" : "FAIL"));
        }).start();
    }

    private static File firstExisting(File a, File b) {
        if (a != null && a.exists()) return a;
        if (b != null && b.exists()) return b;
        return null;
    }
}
