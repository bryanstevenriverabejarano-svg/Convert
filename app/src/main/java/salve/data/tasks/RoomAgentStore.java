package salve.data.tasks;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import salve.core.agent.AgentEngine;
import salve.core.agent.AgentPolicy;
import salve.core.agent.AgentRun;
import salve.data.db.*;
import salve.data.sync.CloudMemoryImporter;

/** Journal, statistics and final memory/outbox updates share Room transactions. */
public final class RoomAgentStore implements AgentEngine.Store {
    private final MemoriaDatabase db;
    private final AgentDao dao;
    private final BooleanSupplier syncEnabled;
    public RoomAgentStore(MemoriaDatabase db, BooleanSupplier syncEnabled) {
        this.db = db; this.dao = db.agentDao(); this.syncEnabled = syncEnabled;
    }
    public AgentRun create(String goal, boolean code, String bridge, long now) {
        if (goal == null || goal.trim().isEmpty() || goal.length() > 2048) throw new IllegalArgumentException("Indica un objetivo de hasta 2048 caracteres.");
        return db.runInTransaction(() -> {
            if (dao.pendingCount() >= 20) throw new IllegalArgumentException("Hay 20 planes pendientes; termina o cancela alguno.");
            dao.prune();
            AgentRun run = new AgentRun(); run.id = UUID.randomUUID().toString(); run.goal = goal.trim();
            run.codeAllowed = code; run.bridge = bridge == null ? "" : bridge; run.createdAt = run.updatedAt = now;
            AgentRunEntity row = new AgentRunEntity(); row.id = run.id; row.createdAt = row.updatedAt = now; row.journal = run.encode();
            dao.insert(row); return run;
        });
    }
    @Override public AgentRun get(String id) { return snapshot(dao.get(id)); }
    private AgentRun snapshot(AgentRunEntity row) {
        if (row == null) return null;
        AgentRun run = AgentRun.decode(row.journal);
        run.status = row.status; run.owner = row.owner; run.leaseUntil = row.leaseUntil; run.updatedAt = row.updatedAt;
        return run;
    }
    public boolean paused() { return db.researchTaskDao().paused() != 0; }
    public void pause(boolean pause, long now) {
        db.runInTransaction(() -> {
            AgentControlEntity control = new AgentControlEntity(); control.paused = pause; db.researchTaskDao().control(control);
            if (pause) { dao.fence(now); db.researchTaskDao().fenceRunning(now); }
        });
    }
    public boolean cancel(String id, long now) { return dao.cancel(id, now) == 1; }
    public boolean resume(String id, long now) {
        return db.runInTransaction(() -> {
            if (dao.resume(id, now) != 1) return false;
            AgentRun run = get(id); run.attempts = 0; dao.resetJournal(id, run.encode()); return true;
        });
    }
    public List<AgentRun> recent() { List<AgentRun> result = new ArrayList<>(); for (AgentRunEntity row : dao.recent(50)) result.add(snapshot(row)); return result; }
    public List<AgentRun> pending() { List<AgentRun> result = new ArrayList<>(); for (AgentRunEntity row : dao.pending()) result.add(snapshot(row)); return result; }
    @Override public AgentRun claim(String id, String owner, long now) {
        return db.runInTransaction(() -> {
            if (dao.claim(id, owner, now, now + AgentEngine.LEASE_MS) != 1) return null;
            AgentRun run = get(id);
            if (run.attempts >= 8) {
                run.status = "BLOCKED"; run.error = "Ocho interrupciones o intentos; revisa el plan antes de reanudarlo.";
                dao.save(id, owner, run.encode(), run.status, now, 0); return null;
            }
            run.attempts++; dao.save(id, owner, run.encode(), "RUNNING", now, now + AgentEngine.LEASE_MS); return run;
        });
    }
    @Override public boolean owns(String id, String owner) {
        AgentRun run = get(id); return run != null && run.status.equals("RUNNING") && run.owner.equals(owner) && !paused();
    }
    @Override public boolean save(AgentRun run, long now) {
        return db.runInTransaction(() -> {
            AgentRun previous = get(run.id);
            if (!owns(run.id, run.owner)) return false;
            if (dao.save(run.id, run.owner, run.encode(), "RUNNING", now, now + AgentEngine.LEASE_MS) != 1) return false;
            for (int i = previous.observations.size(); i < run.observations.size(); i++) {
                AgentRun.Observation o = run.observations.get(i);
                if (!AgentPolicy.TOOLS.contains(o.tool)) continue;
                AgentToolStatEntity stat = dao.stat(o.tool);
                if (stat == null) { stat = new AgentToolStatEntity(); stat.tool = o.tool; }
                if (o.useful()) stat.successes++; else stat.failures++;
                stat.elapsedMs += o.elapsedMs; stat.updatedAt = now; dao.putStat(stat);
            }
            return true;
        });
    }
    @Override public boolean finish(AgentRun run, long now) {
        return db.runInTransaction(() -> {
            if (!owns(run.id, run.owner) || !run.terminal()) return false;
            if (dao.save(run.id, run.owner, run.encode(), run.status, now, 0) != 1) return false;
            if (!run.answer.isEmpty() && (run.status.equals("SUCCEEDED") || run.status.equals("PARTIAL"))) {
                RecuerdoEntity memory = new RecuerdoEntity();
                StringBuilder text = new StringBuilder("Resultado de plan solicitado: ").append(run.goal)
                        .append("\nEstado: ").append(run.status).append("\n").append(run.answer);
                for (AgentRun.Observation observation : run.observations) if (run.evidence.contains(observation.id))
                    text.append("\nFuente ").append(observation.id).append(" (").append(observation.tool).append("): ")
                            .append(String.join(" ", observation.sources));
                memory.frase = text.toString(); memory.timestamp = now; memory.emocion = "no evaluada"; memory.intensidad = 5;
                memory.etiquetas = new Gson().toJson(Arrays.asList("sintesis_agente", "plan:" + run.id));
                db.recuerdoDao().insertRecuerdo(memory);
                if (syncEnabled.getAsBoolean()) {
                    SyncEventEntity event = new SyncEventEntity(); event.createdAt = now; event.payload = CloudMemoryImporter.serialize(memory);
                    db.syncEventDao().insert(event);
                }
            }
            return true;
        });
    }
    @Override public void release(String id, String owner, long now) { dao.release(id, owner, now); }
    public String statistics() { return new Gson().toJson(dao.stats()); }
    public AgentSubscriptionEntity subscribe(String goal, String event, long interval, String bridge, long now) {
        if (!Arrays.asList("TIMER", "APP_OPEN", "CHARGING").contains(event) || interval < 3_600_000L || interval > 720L * 3_600_000L
                || goal == null || goal.trim().isEmpty() || goal.length() > 2048) throw new IllegalArgumentException("Vigilancia inválida.");
        return db.runInTransaction(() -> {
            if (dao.subscriptionCount() >= 5) throw new IllegalArgumentException("Ya hay cinco vigilancias activas.");
            AgentSubscriptionEntity row = new AgentSubscriptionEntity(); row.id = UUID.randomUUID().toString();
            row.goal = goal.trim(); row.event = event; row.intervalMs = interval; row.bridge = bridge == null ? "" : bridge;
            row.nextAt = event.equals("TIMER") ? now + interval : now; dao.subscribe(row); return row;
        });
    }
    public List<AgentSubscriptionEntity> subscriptions() { return dao.subscriptions(); }
    public boolean unsubscribe(String id) { return dao.unsubscribe(id) == 1; }
    public List<AgentRun> signal(String event, long now) {
        return db.runInTransaction(() -> {
            List<AgentRun> created = new ArrayList<>();
            if (paused()) return created;
            for (AgentSubscriptionEntity s : dao.subscriptions()) if (s.event.equals(event) && s.nextAt <= now && dao.pendingCount() < 20) {
                if (dao.advance(s.id, s.nextAt, now + s.intervalMs) == 1) created.add(create(s.goal, false, s.bridge, now));
            }
            return created;
        });
    }
}
