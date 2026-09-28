package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import salve.core.tasks.ResearchTask;

@Entity(tableName = "agent_research_tasks")
public class ResearchTaskEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String question = "";
    @NonNull public String status = "QUEUED";
    @NonNull public String owner = "";
    public String receipt, error;
    public int attempts;
    public long createdAt, updatedAt, leaseUntil, nextAttemptAt;

    public ResearchTask snapshot() {
        ResearchTask t = new ResearchTask();
        t.id = id; t.question = question; t.status = status; t.owner = owner;
        t.receipt = receipt; t.error = error; t.attempts = attempts;
        t.createdAt = createdAt; t.updatedAt = updatedAt;
        t.leaseUntil = leaseUntil; t.nextAttemptAt = nextAttemptAt;
        return t;
    }
}
