package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agent_tool_stats")
public class AgentToolStatEntity {
    @PrimaryKey @NonNull public String tool = "";
    public long successes, failures, elapsedMs, updatedAt;
}
