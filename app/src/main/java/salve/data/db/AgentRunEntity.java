package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agent_runs")
public class AgentRunEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String status = "QUEUED", owner = "", journal = "";
    public long createdAt, updatedAt, leaseUntil;
}
