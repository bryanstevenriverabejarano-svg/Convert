package salve.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SyncEventDao {
    @Insert
    long insert(SyncEventEntity e);

    /** Pending outbox entries. tries=-1 means synchronized and retained in the local journal. */
    @Query("SELECT * FROM sync_events WHERE tries >= 0 AND tries < 20 ORDER BY createdAt ASC LIMIT :limit")
    List<SyncEventEntity> getPending(int limit);

    @Query("SELECT * FROM sync_events ORDER BY createdAt DESC LIMIT :n")
    List<SyncEventEntity> getLast(int n);

    @Query("SELECT * FROM sync_events WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    List<SyncEventEntity> pageAfter(long afterId, int limit);

    /** Kept for explicit maintenance; cloud synchronization does not delete successful entries. */
    @Delete
    void delete(SyncEventEntity e);

    @Query("UPDATE sync_events SET tries = -1 WHERE id = :id")
    void markSynced(long id);

    @Query("UPDATE sync_events SET tries = tries + 1 WHERE id = :id")
    void incTries(long id);

    @Query("SELECT COUNT(*) FROM sync_events WHERE createdAt = :createdAt AND payload = :payload")
    int countExact(long createdAt, String payload);

    @Query("DELETE FROM sync_events WHERE tries >= :maxTries")
    void purgeFailed(int maxTries);
}
