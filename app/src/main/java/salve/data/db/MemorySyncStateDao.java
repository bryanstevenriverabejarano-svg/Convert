package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MemorySyncStateDao {
    @Query("SELECT * FROM memory_sync_state WHERE `key` = :key LIMIT 1")
    MemorySyncStateEntity get(String key);
    @Query("SELECT * FROM memory_sync_state WHERE deleted = 1 AND `key` LIKE 'profile:%'")
    java.util.List<MemorySyncStateEntity> deletedProfiles();
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void put(MemorySyncStateEntity state);
}
