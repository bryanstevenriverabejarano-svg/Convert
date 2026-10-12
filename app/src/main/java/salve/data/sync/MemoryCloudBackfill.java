package salve.data.sync;

import java.util.List;
import java.util.function.BooleanSupplier;
import salve.data.db.*;

/** Restartable Room-to-outbox repair for memories written while sync was disabled. */
public final class MemoryCloudBackfill {
    private static final String CURSOR = "outbox_memory_cursor_v1";
    private MemoryCloudBackfill() { }
    public static boolean enqueueBatch(MemoriaDatabase db, int limit, BooleanSupplier enabled) {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("Lote de memoria inválido");
        final long[] after = {0};
        db.runInTransaction(() -> {
            MemorySyncStateEntity cursor = db.memorySyncStateDao().get(CURSOR);
            after[0] = cursor == null ? 0 : cursor.updatedAt;
            List<RecuerdoEntity> rows = db.recuerdoDao().pageForCloud(after[0], limit);
            for (RecuerdoEntity row : rows) {
                if (!enabled.getAsBoolean() || Thread.currentThread().isInterrupted()) break;
                if (row.etiquetas == null || !row.etiquetas.contains("\"pcloud\"")) {
                    String payload = CloudMemoryImporter.serialize(row);
                    if (db.syncEventDao().countExact(row.timestamp, payload) == 0) {
                        SyncEventEntity event = new SyncEventEntity();
                        event.payload = payload; event.createdAt = row.timestamp; event.tries = 0;
                        db.syncEventDao().insert(event);
                    }
                }
                after[0] = row.id;
                db.memorySyncStateDao().put(MemorySyncStateEntity.of(CURSOR, row.id, false));
            }
        });
        return !db.recuerdoDao().pageForCloud(after[0], 1).isEmpty();
    }
}
