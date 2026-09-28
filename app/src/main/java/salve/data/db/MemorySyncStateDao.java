package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MemorySyncStateDao {
    @Query("SELECT * FROM memory_sync_state WHERE `key` = :key LIMIT 1")
    MemorySyncStateEntity get(String key);
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(MemorySyncStateEntity state);
}
