package salve.core.visual;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import salve.data.db.MemoriaDatabase;
import salve.data.sync.CloudSyncManager;

/** Connects private photo files, searchable memory, the knowledge graph and the existing outbox. */
public final class VisualMemoryRepository {
    private static final Object LOCK = new Object();
    public static final String EDGE = VisualMemoryGraph.EDGE;
    private final Context context;
    public final VisualMemoryStore store;

    public VisualMemoryRepository(Context context) {
        this.context = context.getApplicationContext();
        store = new VisualMemoryStore(new File(this.context.getFilesDir(), "visual_memory"));
    }

    public VisualMemoryRecord capture(Bitmap photo, String question, String name,
                                      String relationship, String position, boolean localOnly) throws IOException {
        int longest = Math.max(photo.getWidth(), photo.getHeight());
        Bitmap scaled = longest <= 1600 ? photo : Bitmap.createScaledBitmap(photo,
                Math.max(1, photo.getWidth() * 1600 / longest), Math.max(1, photo.getHeight() * 1600 / longest), true);
        byte[] bytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new IOException("No se pudo guardar la foto");
            bytes = output.toByteArray();
        } finally { if (scaled != photo) scaled.recycle(); }
        return persist(VisualMemoryRecord.create(question, name, relationship, position, localOnly), bytes, true);
    }

    public VisualMemoryRecord update(VisualMemoryRecord record) throws IOException { return persist(record, null, true); }

    /** Restoring an event never re-enqueues it. Older revisions cannot undo an identity correction. */
    public VisualMemoryRecord restore(VisualMemoryRecord record, byte[] image) throws IOException {
        if (image != null) {
            if (image.length > VisualMemoryStore.MAX_IMAGE_BYTES) throw new IOException("Foto remota demasiado grande");
            BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(image, 0, image.length, bounds);
            if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outWidth > 1600 || bounds.outHeight > 1600)
                throw new IOException("Imagen remota inválida");
        }
        return persist(VisualMemoryStore.validated(record), image, false);
    }

    public Bitmap loadImage(VisualMemoryRecord record) throws IOException {
        File file = store.imageFile(record.id);
        if (!file.isFile() || file.length() > VisualMemoryStore.MAX_IMAGE_BYTES) throw new IOException("No está disponible la foto original");
        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth < 1 || bounds.outHeight < 1 || bounds.outWidth > 1600 || bounds.outHeight > 1600)
            throw new IOException("La foto no se puede abrir");
        Bitmap result = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (result == null) throw new IOException("La foto no se puede abrir");
        return result;
    }

    private VisualMemoryRecord persist(VisualMemoryRecord record, byte[] jpeg, boolean enqueue) throws IOException {
        synchronized (LOCK) {
            VisualMemoryRecord saved = store.save(record, jpeg);
            MemoriaDatabase database = MemoriaDatabase.getInstance(context);
            database.runInTransaction(() -> VisualMemoryGraph.index(database.recuerdoDao(),
                    database.knowledgeNodeDao(), database.knowledgeRelationDao(), saved));
            if (enqueue) {
                CloudSyncManager.enqueueVisual(context, saved);
                if (CloudSyncManager.isEnabled(context)) salve.data.sync.SyncWorker.enqueueWhenOnline(context);
                salve.core.GrafoRecuerdos.generarVisual(context);
            }
            return saved;
        }
    }

}
