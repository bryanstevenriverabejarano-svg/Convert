package salve.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agent_control")
public class AgentControlEntity {
    @PrimaryKey public int id = 1;
    public boolean paused;
}
