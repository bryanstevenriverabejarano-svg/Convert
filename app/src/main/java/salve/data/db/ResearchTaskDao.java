package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ResearchTaskDao {
    @Insert void insert(ResearchTaskEntity task);
    @Insert(onConflict = OnConflictStrategy.REPLACE) void control(AgentControlEntity control);
    @Query("SELECT * FROM agent_control WHERE id = 1") AgentControlEntity controlState();
    @Query("SELECT COUNT(*) FROM agent_control WHERE id = 1 AND paused = 1") int paused();
    @Query("SELECT * FROM agent_research_tasks WHERE id = :id") ResearchTaskEntity get(String id);
    @Query("SELECT * FROM agent_research_tasks ORDER BY createdAt DESC, id DESC LIMIT :limit") List<ResearchTaskEntity> recent(int limit);
    @Query("SELECT * FROM agent_research_tasks WHERE status IN ('QUEUED','RUNNING','STAGED') ORDER BY createdAt ASC LIMIT 20") List<ResearchTaskEntity> pending();
    @Query("SELECT COUNT(*) FROM agent_research_tasks WHERE status IN ('QUEUED','RUNNING','STAGED')") int pendingCount();
    @Query("DELETE FROM agent_research_tasks WHERE status IN ('SUCCEEDED','PARTIAL','FAILED','CANCELLED') AND id NOT IN (SELECT id FROM agent_research_tasks ORDER BY createdAt DESC, id DESC LIMIT 50)") void prune();
    @Query("UPDATE agent_research_tasks SET status = 'RUNNING', owner = :owner, leaseUntil = :until, updatedAt = :now, attempts = attempts + CASE WHEN receipt IS NULL THEN 1 ELSE 0 END WHERE id = :id AND status IN ('QUEUED','RUNNING','STAGED') AND nextAttemptAt <= :now AND (owner = '' OR leaseUntil <= :now) AND (receipt IS NOT NULL OR attempts < 3) AND NOT EXISTS (SELECT 1 FROM agent_control WHERE id = 1 AND paused = 1)")
    int claim(String id, String owner, long now, long until);
    @Query("UPDATE agent_research_tasks SET status = 'FAILED', owner = '', leaseUntil = 0, error = 'Presupuesto de intentos agotado; puedes reanudar la tarea.', updatedAt = :now WHERE id = :id AND status IN ('QUEUED','RUNNING') AND receipt IS NULL AND attempts >= 3 AND (owner = '' OR leaseUntil <= :now)")
    void expireExhausted(String id, long now);
    @Query("UPDATE agent_research_tasks SET status = 'STAGED', receipt = :receipt, updatedAt = :now, leaseUntil = :until, error = NULL WHERE id = :id AND owner = :owner AND status = 'RUNNING'")
    int stage(String id, String owner, String receipt, long now, long until);
    @Query("UPDATE agent_research_tasks SET status = :status, owner = '', leaseUntil = 0, updatedAt = :now, error = NULL WHERE id = :id AND owner = :owner AND status IN ('RUNNING','STAGED')")
    int complete(String id, String owner, String status, long now);
    @Query("UPDATE agent_research_tasks SET status = :status, owner = '', leaseUntil = 0, nextAttemptAt = :next, updatedAt = :now, error = :error WHERE id = :id AND owner = :owner AND status IN ('RUNNING','STAGED')")
    int release(String id, String owner, String status, String error, long now, long next);
    @Query("UPDATE agent_research_tasks SET status = 'CANCELLED', owner = '', leaseUntil = 0, error = NULL, updatedAt = :now WHERE id = :id AND status IN ('QUEUED','RUNNING','STAGED','FAILED')")
    int cancel(String id, long now);
    @Query("UPDATE agent_research_tasks SET status = 'QUEUED', owner = '', leaseUntil = 0, attempts = 0, nextAttemptAt = 0, error = NULL, updatedAt = :now WHERE id = :id AND status IN ('FAILED','CANCELLED')")
    int resume(String id, long now);
    @Query("UPDATE agent_research_tasks SET status = 'QUEUED', owner = '', leaseUntil = 0, updatedAt = :now WHERE status IN ('RUNNING','STAGED')")
    void fenceRunning(long now);
}
