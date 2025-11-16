package salve.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_events")
public class SyncEventEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String payload;   // JSON a enviar
    public long   createdAt; // millis
    public int    tries;     // intentos de envío
}
