package salve.data.salvente;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SalventeDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) void insert(SalventeProfileEntity profile);
    @Query("SELECT * FROM salvente_profiles ORDER BY created_at") List<SalventeProfileEntity> all();
    @Query("SELECT * FROM salvente_profiles WHERE id = :id") SalventeProfileEntity get(String id);
    @Query("SELECT name FROM salvente_profiles ORDER BY created_at") List<String> names();
    @Query("SELECT COUNT(*) FROM salvente_profiles WHERE id = :id") int countId(String id);
    @Query("UPDATE salvente_profiles SET state = :state, updated_at = :now WHERE id = :id") int setState(String id, String state, long now);
    @Query("DELETE FROM salvente_profiles WHERE id = :id") int delete(String id);

    @Insert void insertMemory(SalventeMemoryEntity memory);
    @Query("SELECT * FROM salvente_memories WHERE salvente_id = :id ORDER BY created_at DESC, id DESC LIMIT :limit")
    List<SalventeMemoryEntity> memories(String id, int limit);
    @Query("SELECT COUNT(*) FROM salvente_memories WHERE salvente_id = :id") int memoryCount(String id);
    @Query("DELETE FROM salvente_memories WHERE salvente_id = :id AND id NOT IN "
            + "(SELECT id FROM salvente_memories WHERE salvente_id = :id ORDER BY created_at DESC, id DESC LIMIT :keep)")
    int trimMemories(String id, int keep);
}
