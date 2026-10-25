package salve.data.sync;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import salve.data.db.*;
import salve.data.db.SyncEventDao;
import salve.data.db.SyncEventEntity;
import salve.core.visual.VisualMemoryRecord;
import salve.core.visual.VisualMemoryRepository;
import salve.core.visual.VisualMemoryStore;

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
    public static void configurePCloud(Context ctx, String accessToken, String apiHost, java.util.function.BooleanSupplier cancelled) {
        PCloudProvider.configureOAuth(ctx, accessToken, apiHost, cancelled);
    }

    public static boolean isPCloudConfigured(Context ctx) {
        return ctx != null && new PCloudProvider(ctx).isConfigured();
    }

    /** Cambio hecho por Bryan: pausar aquí impide que Salve la reactive sola. */
    public static void setEnabled(Context ctx, boolean enabled) { setEnabled(ctx, enabled, true); }

    public static void setEnabled(Context ctx, boolean enabled, boolean byUser) {
        if (ctx == null) return;
        android.content.SharedPreferences.Editor e = ctx.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(CLOUD_SYNC_ENABLED, enabled);
        if (byUser) e.putBoolean("cloud_sync_paused_by_user", !enabled);
        e.apply();
        if (enabled) {
            requestRestore(ctx);
            try { CloudAutoSync.schedulePeriodic(ctx); } catch (RuntimeException ignored) { }
        }
    }

    public static boolean pausedByUser(Context ctx) {
        return ctx.getSharedPreferences(PRIVACY_PREFS, Context.MODE_PRIVATE).getBoolean("cloud_sync_paused_by_user", false);
    }

    // ── Lo que aprende fuera de los recuerdos: dibujos, su diario de dibujo y sus acciones ──

    /** {archivo local relativo, ruta en pCloud}; base "files" o "nobackup". */
    static final String BIOGRAPHY = "mind/biography.json";
    static final String PEOPLE = "mind/people.json";
    static final String[][] KNOWLEDGE = {
            {"files", "sketch/learned.json", ROOT + "/sketch/learned.json"},
            {"files", "sketch/journal.json", ROOT + "/sketch/journal.json"},
            {"nobackup", "mind/actions.json", ROOT + "/mind/actions.json"},
            {"nobackup", "mind/lessons.json", ROOT + "/mind/lessons.json"},
            {"nobackup", "mind/skills.json", ROOT + "/mind/skills.json"},
            {"nobackup", BIOGRAPHY, ROOT + "/" + BIOGRAPHY},
            {"nobackup", PEOPLE, ROOT + "/" + PEOPLE},
            {"nobackup", "mind/milestones.json", ROOT + "/mind/milestones.json"},
            {"nobackup", "mind/stories.json", ROOT + "/mind/stories.json"},
            {"nobackup", "mind/concepts.json", ROOT + "/mind/concepts.json"},
    };

    /**
     * Sube los que cambiaron y, en una instalación nueva, descarga los que faltan. Fuera del hilo
     * principal. @return cuántos archivos movió
     */
    public static int syncKnowledge(Context ctx) {
        if (!isEnabled(ctx) || !isPCloudConfigured(ctx)) return 0;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE);
        PCloudProvider provider = new PCloudProvider(ctx);
        int moved = 0;
        for (String[] k : KNOWLEDGE) {
            File local = new File("files".equals(k[0]) ? ctx.getFilesDir() : ctx.getNoBackupFilesDir(), k[1]);
            try {
                if (BIOGRAPHY.equals(k[1])) {
                    // Su biografía no se pisa al reinstalar: primero se une con la copia de la nube y
                    // después se sube la unión (si no, la instalación nueva borraría su historia).
                    salve.core.MindRuntime mind = salve.core.MindRuntime.get(ctx);
                    byte[] remote = provider.downloadBounded(k[2], 64 * 1024);
                    if (remote != null && remote.length > 0) mind.biography().mergeFrom(new String(remote, StandardCharsets.UTF_8));
                    mind.anchorBiographyToMemories(true);
                }
                if (PEOPLE.equals(k[1])) {
                    // Igual con quién es habitual en su vida: se une con la nube antes de subir.
                    byte[] remote = provider.downloadBounded(k[2], 256 * 1024);
                    if (remote != null && remote.length > 0)
                        salve.core.MindRuntime.get(ctx).people().mergeFrom(new String(remote, StandardCharsets.UTF_8));
                }
                if (local.isFile()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(local.toPath());
                    String hash = Integer.toHexString(java.util.Arrays.hashCode(bytes));
                    if (hash.equals(prefs.getString("hash:" + k[2], null))) continue;
                    if (provider.uploadJson(k[2], new String(bytes, StandardCharsets.UTF_8))) {
                        prefs.edit().putString("hash:" + k[2], hash).apply();
                        moved++;
                    }
                } else {
                    byte[] remote = provider.downloadBounded(k[2], 4 * 1024 * 1024);
                    if (remote == null || remote.length == 0) continue;
                    com.google.gson.JsonParser.parseString(new String(remote, StandardCharsets.UTF_8)); // sólo JSON válido
                    File dir = local.getParentFile();
                    if (dir != null && !dir.isDirectory() && !dir.mkdirs()) continue;
                    File tmp = new File(dir, local.getName() + ".tmp");
                    java.nio.file.Files.write(tmp.toPath(), remote);
                    java.nio.file.Files.move(tmp.toPath(), local.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    prefs.edit().putString("hash:" + k[2], Integer.toHexString(java.util.Arrays.hashCode(remote))).apply();
                    moved++;
                }
            } catch (Exception skip) { /* uno que falle no frena los demás */ }
        }
        moved += syncLibrary(ctx, provider, prefs);
        return moved;
    }

    /**
     * El texto entero de sus cuentos (inventados y leídos) va en files/library: el índice
     * (mind/stories.json) ya se sincroniza, y sin esto, al reinstalar sabría los títulos pero no
     * podría contarlos. Sube los que no estén subidos y baja los que falten, con los nombres que
     * dice el índice (y sus versiones anteriores).
     */
    static int syncLibrary(Context ctx, PCloudProvider provider, android.content.SharedPreferences prefs) {
        File index = new File(ctx.getNoBackupFilesDir(), "mind/stories.json");
        if (!index.isFile()) return 0;
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        try {
            com.google.gson.JsonArray list = com.google.gson.JsonParser.parseString(
                    new String(java.nio.file.Files.readAllBytes(index.toPath()), StandardCharsets.UTF_8)).getAsJsonArray();
            for (com.google.gson.JsonElement e : list) {
                com.google.gson.JsonObject o = e.getAsJsonObject();
                if (o.has("file") && o.get("file").isJsonPrimitive()) names.add(o.get("file").getAsString());
                if (o.has("earlier") && o.get("earlier").isJsonArray())
                    for (com.google.gson.JsonElement f : o.getAsJsonArray("earlier")) if (f.isJsonPrimitive()) names.add(f.getAsString());
            }
        } catch (Exception unreadable) { return 0; }
        File dir = new File(ctx.getFilesDir(), "library");
        int moved = 0;
        for (String name : names) {
            if (!name.matches("[a-z0-9-]{1,60}\\.txt")) continue; // solo nombres que pone ella: nada de rutas
            File local = new File(dir, name);
            String remote = ROOT + "/library/" + name;
            try {
                if (local.isFile()) {
                    if (prefs.getBoolean("up:" + remote, false)) continue;
                    if (provider.uploadFile(remote, local)) { prefs.edit().putBoolean("up:" + remote, true).apply(); moved++; }
                } else {
                    byte[] bytes = provider.downloadBounded(remote, 4 * 1024 * 1024);
                    if (bytes == null || bytes.length == 0) continue;
                    if (!dir.isDirectory() && !dir.mkdirs()) continue;
                    File tmp = new File(dir, name + ".tmp");
                    java.nio.file.Files.write(tmp.toPath(), bytes);
                    java.nio.file.Files.move(tmp.toPath(), local.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    prefs.edit().putBoolean("up:" + remote, true).apply();
                    moved++;
                }
            } catch (Exception skip) { /* el siguiente */ }
        }
        return moved;
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

    /** Stable payload plus a transactional deduplication check also covers photos captured offline. */
    public static void enqueueVisual(Context ctx, VisualMemoryRecord record) {
        if (!isEnabled(ctx)) return;
        com.google.gson.JsonObject event = new com.google.gson.JsonObject();
        event.addProperty("type", "visual_memory");
        event.addProperty("time_ms", record.createdAt);
        event.addProperty("content", record.memoryText());
        event.add("visual_record", new com.google.gson.Gson().toJsonTree(record));
        String payload = event.toString();
        MemoriaDatabase database = MemoriaDatabase.getInstance(ctx);
        database.runInTransaction(() -> {
            SyncEventDao dao = database.syncEventDao();
            if (dao.countExact(record.createdAt, payload) > 0) return;
            SyncEventEntity entry = new SyncEventEntity();
            entry.payload = payload; entry.createdAt = record.createdAt; entry.tries = 0;
            dao.insert(entry);
        });
    }

    public static void enqueueArchivedPhotos(Context ctx) {
        if (!isEnabled(ctx)) return;
        for (VisualMemoryRecord record : new VisualMemoryRepository(ctx).store.list()) enqueueVisual(ctx, record);
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
        long deadline = android.os.SystemClock.elapsedRealtime() + 60000L;
        try {
            SyncEventDao dao = MemoriaDatabase.getInstance(ctx).syncEventDao();
            List<SyncEventEntity> batch = dao.getPending(maxBatch);
            for (SyncEventEntity e : batch) {
                if (!isEnabled(ctx) || Thread.currentThread().isInterrupted()
                        || android.os.SystemClock.elapsedRealtime() >= deadline) break;
                String path = ROOT + "/events/" + e.createdAt + "-" + e.id + ".json";
                if (uploadPhotoAttachment(ctx, provider, e.payload) && provider.uploadJson(path, e.payload)) {
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

    private static VisualMemoryRecord visualRecord(String payload) {
        com.google.gson.JsonElement root = com.google.gson.JsonParser.parseString(payload);
        if (!root.isJsonObject()) throw new IllegalArgumentException("Evento inválido");
        com.google.gson.JsonObject event = root.getAsJsonObject();
        if (!event.has("type")) return null;
        if (!event.get("type").isJsonPrimitive()) throw new IllegalArgumentException("Tipo de evento inválido");
        if (!"visual_memory".equals(event.get("type").getAsString())) return null;
        return VisualMemoryStore.validated(new com.google.gson.Gson().fromJson(event.get("visual_record"), VisualMemoryRecord.class));
    }

    /** A visual event is acknowledged only after both its image and metadata are verified remotely. */
    private static boolean uploadPhotoAttachment(Context context, PCloudProvider provider, String payload) {
        try {
            VisualMemoryRecord record = visualRecord(payload);
            return record == null || provider.uploadFile(ROOT + "/images/" + record.id + ".jpg",
                    new VisualMemoryRepository(context).store.imageFile(record.id));
        } catch (Exception invalid) { Log.w(TAG, "Adjunto visual pendiente o inválido"); return false; }
    }

    public static boolean hasPending(Context context) {
        return isEnabled(context) && !MemoriaDatabase.getInstance(context).syncEventDao().getPending(1).isEmpty();
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

    private static final String RESTORE_PREFS = "salve_memory_restore";
    private static final Object RESTORE_LOCK = new Object();
    private static final Object LOCAL_INDEX_LOCK = new Object();

    public static String memoryStatus(Context ctx) {
        if (!isPCloudConfigured(ctx)) return "pCloud sin conectar; consulto los recuerdos disponibles en este móvil.";
        if (!isEnabled(ctx)) return "pCloud en pausa; consulto la copia disponible en este móvil.";
        return ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE).getString("status",
                "Recuperación de pCloud pendiente; la historia disponible puede estar incompleta.");
    }

    public static boolean memoryRestoreComplete(Context ctx) {
        return isEnabled(ctx) && isPCloudConfigured(ctx)
                && ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE).getBoolean("complete", false);
    }

    public static void requestRestore(Context ctx) {
        if (!isEnabled(ctx) || !isPCloudConfigured(ctx)) return;
        restoreStatus(ctx, "Recuperación de pCloud solicitada; esperando conexión o el lote en curso.", false);
        SyncWorker.enqueueWhenOnline(ctx.getApplicationContext());
    }

    /**
     * Recuperación pedida por el usuario: se ejecuta ya, por lotes, contando el progreso, en vez
     * de esperar a un trabajo de WorkManager que puede estar aplazado por reintentos anteriores.
     * Llamar fuera del hilo principal. Devuelve un resumen honesto (incluido el error real).
     */
    public static String restoreNow(Context ctx, java.util.function.BooleanSupplier cancelled,
                                    java.util.function.Consumer<String> progress) {
        if (!isPCloudConfigured(ctx)) return "pCloud no está conectada en esta instalación.";
        if (!isEnabled(ctx)) return "La sincronización con pCloud está en pausa.";
        try { indexLocalJournal(ctx); } catch (RuntimeException ignored) { }
        int restored = 0;
        RestoreBatch batch = null;
        for (int round = 0; round < 80; round++) {
            if (cancelled != null && cancelled.getAsBoolean()) break;
            batch = restoreMemoryBatch(ctx, 150);
            restored += batch.restored;
            if (progress != null && batch.more) progress.accept(memoryStatus(ctx));
            if (batch.retry || !batch.more) break;
        }
        boolean mind = restoreMind(ctx);
        // Reinicia el trabajo en segundo plano sin el retraso acumulado de reintentos antiguos.
        try { SyncWorker.restartNow(ctx.getApplicationContext()); } catch (RuntimeException ignored) { }
        String status = memoryStatus(ctx) + (mind ? " También he recuperado mi mente (lo que pensaba, creía y había aprendido)." : "");
        return (restored > 0 ? "He traído " + restored + " recuerdos nuevos de pCloud. "
                : batch != null && !batch.retry ? "No había recuerdos nuevos que traer de pCloud. " : "")
                + status;
    }

    private static final String MIND_REMOTE = ROOT + "/mind/salve_mind_v1.json";

    /** Sube su mente (yo persistente y juego) si cambió desde la última copia. Fuera del hilo principal. */
    public static boolean uploadMind(Context ctx) {
        if (!isEnabled(ctx) || !isPCloudConfigured(ctx)) return false;
        String snapshot = salve.core.MindRuntime.get(ctx).exportMindSnapshot();
        if (snapshot == null) return false;
        String hash = Integer.toHexString(snapshot.hashCode());
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE);
        if (hash.equals(prefs.getString("mind_hash", null))) return true;
        boolean ok = new PCloudProvider(ctx).uploadJson(MIND_REMOTE, snapshot);
        if (ok) prefs.edit().putString("mind_hash", hash).apply();
        return ok;
    }

    /** En una instalación nueva (o con una copia más reciente en pCloud) recupera su mente. */
    public static boolean restoreMind(Context ctx) { return restoreMind(ctx, true); }

    /** Sin forzar, comprueba como mucho cada 12 h (evita descargar la copia en cada sincronización). */
    public static boolean restoreMind(Context ctx, boolean force) {
        if (!isEnabled(ctx) || !isPCloudConfigured(ctx)) return false;
        android.content.SharedPreferences prefs = ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("mind_checked_at", 0L) < 12L * 60 * 60 * 1000) return false;
        prefs.edit().putLong("mind_checked_at", now).apply();
        try {
            byte[] bytes = new PCloudProvider(ctx).downloadBounded(MIND_REMOTE, 2 * 1024 * 1024);
            return bytes != null && salve.core.MindRuntime.get(ctx).importMindSnapshot(new String(bytes, StandardCharsets.UTF_8));
        } catch (RuntimeException unavailable) { return false; }
    }

    private static void restoreStatus(Context ctx, String status, boolean complete) {
        ctx.getSharedPreferences(RESTORE_PREFS, Context.MODE_PRIVATE).edit()
                .putString("status", status).putBoolean("complete", complete).apply();
    }

    public static final class RestoreBatch {
        public final int restored;
        public final boolean retry, more;
        RestoreBatch(int restored, boolean retry) { this(restored, retry, false); }
        RestoreBatch(int restored, boolean retry, boolean more) { this.restored = restored; this.retry = retry; this.more = more; }
    }

    /** Restartable batches, oldest first. Import receipts avoid downloading the same journal again. */
    public static RestoreBatch restoreMemoryBatch(Context ctx, int maxEvents) {
        if (!isEnabled(ctx) || !isPCloudConfigured(ctx) || maxEvents <= 0) return new RestoreBatch(0, false);
        synchronized (RESTORE_LOCK) {
            int restored = 0, scanned = 0, pending = 0, errors = 0, invalid = 0, verified = 0;
            long deadline = android.os.SystemClock.elapsedRealtime() + 90000L;
            restoreStatus(ctx, "Recuperando recuerdos de pCloud…", false);
            try {
                PCloudProvider provider = new PCloudProvider(ctx);
                MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
                CloudMemoryImporter importer = new CloudMemoryImporter(db.recuerdoDao(), db.memorySyncStateDao());
                List<String> files = new java.util.ArrayList<>(provider.listFilesChecked(ROOT + "/events"));
                files.removeIf(path -> !path.endsWith(".json") || timestampFromEventPath(path) <= 0);
                files.sort(java.util.Comparator.comparingLong(CloudSyncManager::timestampFromEventPath).thenComparing(path -> path));
                for (String remote : files) {
                    MemorySyncStateEntity receipt = db.memorySyncStateDao().get("remote:" + remote);
                    if (receipt != null) { if (receipt.deleted) invalid++; else verified++; continue; }
                    if (scanned >= maxEvents || android.os.SystemClock.elapsedRealtime() >= deadline || Thread.currentThread().isInterrupted() || !isEnabled(ctx)) { pending++; continue; }
                    scanned++;
                    byte[] bytes = provider.downloadBounded(remote, 256 * 1024);
                    if (bytes == null) { errors++; continue; }
                    String payload = new String(bytes, StandardCharsets.UTF_8);
                    long createdAt = timestampFromEventPath(remote);
                    try {
                        VisualMemoryRecord photo = visualRecord(payload);
                        if (photo != null) {
                            VisualMemoryRepository archive = new VisualMemoryRepository(ctx);
                            byte[] image = archive.store.imageFile(photo.id).isFile() ? null
                                    : provider.downloadBounded(ROOT + "/images/" + photo.id + ".jpg", VisualMemoryStore.MAX_IMAGE_BYTES);
                            if (image == null && !archive.store.imageFile(photo.id).isFile()) { errors++; continue; }
                            archive.restore(photo, image);
                        }
                        db.runInTransaction(() -> {
                            importer.ingest(payload, createdAt);
                            if (db.syncEventDao().countExact(createdAt, payload) == 0) {
                                SyncEventEntity event = new SyncEventEntity();
                                event.payload = payload; event.createdAt = createdAt; event.tries = -1;
                                db.syncEventDao().insert(event);
                            }
                            db.memorySyncStateDao().put(MemorySyncStateEntity.of("remote:" + remote, createdAt, false));
                        });
                        restored++; verified++;
                    } catch (com.google.gson.JsonParseException | IllegalArgumentException invalidData) {
                        // Retain a rejection receipt without letting one malformed file block older siblings.
                        db.memorySyncStateDao().put(MemorySyncStateEntity.of("remote:" + remote, createdAt, true));
                        invalid++;
                    } catch (Exception unavailable) { errors++; }
                }
                boolean complete = pending == 0 && errors == 0 && invalid == 0;
                restoreStatus(ctx, "pCloud: " + verified + " de " + files.size() + " eventos recuperados. "
                        + (complete ? "Historial de eventos disponible hasta la última sincronización."
                        : pending + " pendientes; " + errors + " sin descargar; " + invalid + " no legibles. La historia puede estar incompleta."), complete);
                return new RestoreBatch(restored, errors > 0, pending > 0);
            } catch (Exception failure) {
                restoreStatus(ctx, "No pude recuperar pCloud. " + PCloudProvider.safeFailure(failure)
                        + " Los recuerdos locales se conservan.", false);
                return new RestoreBatch(restored, true);
            }
        }
    }

    /** Repair journals downloaded by older app versions, including while offline. */
    public static void indexLocalJournal(Context ctx) {
        synchronized (LOCAL_INDEX_LOCK) {
            MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
            MemorySyncStateEntity cursor = db.memorySyncStateDao().get("local_cursor_v1");
            long after = cursor == null ? 0 : cursor.updatedAt;
            CloudMemoryImporter importer = new CloudMemoryImporter(db.recuerdoDao(), db.memorySyncStateDao());
            while (!Thread.currentThread().isInterrupted()) {
                List<SyncEventEntity> page = db.syncEventDao().pageAfter(after, 200);
                if (page.isEmpty()) return;
                for (SyncEventEntity event : page) {
                    db.runInTransaction(() -> {
                        try { importer.ingest(event.payload, event.createdAt, false); }
                        catch (com.google.gson.JsonParseException | IllegalArgumentException invalidData) {
                            Log.w(TAG, "Evento antiguo no legible; se conserva el original");
                        }
                        db.memorySyncStateDao().put(MemorySyncStateEntity.of("local_cursor_v1", event.id, false));
                    });
                    after = event.id;
                }
            }
        }
    }

    public static int restoreEvents(Context ctx, int maxEvents) {
        indexLocalJournal(ctx);
        return restoreMemoryBatch(ctx, maxEvents).restored;
    }

    /** Full profile text and original date, with idempotent outbox insertion. Call off the UI thread. */
    public static void enqueueMemory(Context ctx, RecuerdoEntity record) {
        if (!isEnabled(ctx)) return;
        enqueueStable(ctx, CloudMemoryImporter.serialize(record), record.timestamp);
    }

    public static void enqueueProfileDeletion(Context ctx, String category, long time) {
        if (!isEnabled(ctx)) return;
        com.google.gson.JsonObject event = new com.google.gson.JsonObject();
        event.addProperty("type", "profile_delete"); event.addProperty("category", category);
        event.addProperty("time_ms", time);
        enqueueStable(ctx, event.toString(), time);
    }

    private static void enqueueStable(Context ctx, String payload, long time) {
        MemoriaDatabase db = MemoriaDatabase.getInstance(ctx);
        db.runInTransaction(() -> {
            if (db.syncEventDao().countExact(time, payload) > 0) return;
            SyncEventEntity event = new SyncEventEntity();
            event.payload = payload; event.createdAt = time; event.tries = 0;
            db.syncEventDao().insert(event);
        });
        try { SyncWorker.enqueueWhenOnline(ctx); }
        catch (RuntimeException unavailable) { Log.w(TAG, "Evento conservado; sincronización pendiente de programar"); }
    }

    public static void enqueueExistingProfiles(Context ctx) {
        if (!isEnabled(ctx)) return;
        for (RecuerdoEntity profile : MemoriaDatabase.getInstance(ctx).recuerdoDao().perfiles()) {
            // Remote records already exist in pCloud; no upload loop.
            if (profile.etiquetas == null || !profile.etiquetas.contains("\"pcloud\"")) enqueueMemory(ctx, profile);
        }
        for (MemorySyncStateEntity deletion : MemoriaDatabase.getInstance(ctx).memorySyncStateDao().deletedProfiles())
            enqueueProfileDeletion(ctx, deletion.key.substring("profile:".length()), deletion.updatedAt);
    }

    public static boolean enqueueExistingMemories(Context ctx) {
        return isEnabled(ctx) && MemoryCloudBackfill.enqueueBatch(MemoriaDatabase.getInstance(ctx), 200, () -> isEnabled(ctx));
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
