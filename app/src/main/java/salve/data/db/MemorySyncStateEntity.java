package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Import receipts and profile revisions survive restarts and model changes. */
@Entity(tableName = "memory_sync_state")
public class MemorySyncStateEntity {
    @PrimaryKey @NonNull public String key = "";
    public long updatedAt;
    public boolean deleted;

    public MemorySyncStateEntity() { }
    public static MemorySyncStateEntity of(String key, long time, boolean deleted) {
        MemorySyncStateEntity state = new MemorySyncStateEntity();
        state.key = key; state.updatedAt = time; state.deleted = deleted;
        return state;
    }
}
