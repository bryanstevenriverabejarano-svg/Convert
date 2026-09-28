package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agent_subscriptions")
public class AgentSubscriptionEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String goal = "", event = "TIMER", bridge = "";
    public boolean enabled = true;
    public long intervalMs, nextAt;
}
