package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface AgentDao {
    @Insert void insert(AgentRunEntity row);
    @Query("SELECT * FROM agent_runs WHERE id=:id") AgentRunEntity get(String id);
    @Query("SELECT * FROM agent_runs ORDER BY createdAt DESC, id DESC LIMIT :limit") List<AgentRunEntity> recent(int limit);
    @Query("SELECT * FROM agent_runs WHERE status IN ('QUEUED','RUNNING') ORDER BY createdAt ASC LIMIT 20") List<AgentRunEntity> pending();
    @Query("SELECT COUNT(*) FROM agent_runs WHERE status IN ('QUEUED','RUNNING')") int pendingCount();
    @Query("UPDATE agent_runs SET owner=:owner, status='RUNNING', leaseUntil=:until, updatedAt=:now WHERE id=:id AND status IN ('QUEUED','RUNNING') AND (owner='' OR leaseUntil<=:now) AND NOT EXISTS(SELECT 1 FROM agent_control WHERE id=1 AND paused=1)")
    int claim(String id, String owner, long now, long until);
    @Query("UPDATE agent_runs SET journal=:journal, status=:status, updatedAt=:now, leaseUntil=:until WHERE id=:id AND owner=:owner AND status='RUNNING' AND NOT EXISTS(SELECT 1 FROM agent_control WHERE id=1 AND paused=1)")
    int save(String id, String owner, String journal, String status, long now, long until);
    @Query("UPDATE agent_runs SET status='QUEUED', owner='', leaseUntil=0, updatedAt=:now WHERE id=:id AND owner=:owner AND status='RUNNING'") int release(String id, String owner, long now);
    @Query("UPDATE agent_runs SET status='CANCELLED', owner='', leaseUntil=0, updatedAt=:now WHERE id=:id AND status IN ('QUEUED','RUNNING','BLOCKED')") int cancel(String id, long now);
    @Query("UPDATE agent_runs SET status='QUEUED', owner='', leaseUntil=0, updatedAt=:now WHERE id=:id AND status IN ('CANCELLED','BLOCKED')") int resume(String id, long now);
    @Query("UPDATE agent_runs SET journal=:journal WHERE id=:id AND status='QUEUED' AND owner=''") void resetJournal(String id, String journal);
    @Query("UPDATE agent_runs SET status='QUEUED', owner='', leaseUntil=0, updatedAt=:now WHERE status='RUNNING'") void fence(long now);
    @Query("DELETE FROM agent_runs WHERE status NOT IN ('QUEUED','RUNNING') AND id NOT IN(SELECT id FROM agent_runs ORDER BY createdAt DESC LIMIT 50)") void prune();
    @Insert void subscribe(AgentSubscriptionEntity row);
    @Query("SELECT * FROM agent_subscriptions WHERE enabled=1 ORDER BY nextAt ASC LIMIT 5") List<AgentSubscriptionEntity> subscriptions();
    @Query("SELECT COUNT(*) FROM agent_subscriptions WHERE enabled=1") int subscriptionCount();
    @Query("DELETE FROM agent_subscriptions WHERE id=:id") int unsubscribe(String id);
    @Query("UPDATE agent_subscriptions SET nextAt=:next WHERE id=:id AND nextAt=:previous AND enabled=1") int advance(String id, long previous, long next);
    @Query("SELECT * FROM agent_tool_stats WHERE tool=:tool") AgentToolStatEntity stat(String tool);
    @Query("SELECT * FROM agent_tool_stats ORDER BY tool") List<AgentToolStatEntity> stats();
    @Insert(onConflict = OnConflictStrategy.REPLACE) void putStat(AgentToolStatEntity stat);
}
