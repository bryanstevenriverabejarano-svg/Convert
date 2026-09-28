package salve.data.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import salve.core.tasks.ResearchReceipt;
import salve.core.tasks.ResearchTask;
import salve.core.tasks.ResearchTaskEngine;
import salve.data.db.AgentControlEntity;
import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoEntity;
import salve.data.db.ResearchTaskDao;
import salve.data.db.ResearchTaskEntity;
import salve.data.db.SyncEventEntity;
import salve.data.sync.CloudMemoryImporter;

/** Transactions fence stale workers and atomically archive result + optional cloud outbox. */
public final class RoomResearchTaskStore implements ResearchTaskEngine.Store {
    private final MemoriaDatabase db;
    private final ResearchTaskDao dao;
    private final BooleanSupplier syncEnabled;
    public RoomResearchTaskStore(MemoriaDatabase db, BooleanSupplier syncEnabled) {
        this.db = db; this.dao = db.researchTaskDao(); this.syncEnabled = syncEnabled;
    }
    public ResearchTask create(String question, long now) {
        if (question == null || question.trim().isEmpty() || question.length() > 2048)
            throw new IllegalArgumentException("Indica un tema de hasta 2048 caracteres.");
        return db.runInTransaction(() -> {
            if (dao.pendingCount() >= 20) throw new IllegalArgumentException("Hay 20 tareas pendientes; termina o cancela alguna.");
            dao.prune();
            ResearchTaskEntity entity = new ResearchTaskEntity();
            entity.id = UUID.randomUUID().toString(); entity.question = question.trim();
            entity.createdAt = now; entity.updatedAt = now;
            dao.insert(entity);
            return entity.snapshot();
        });
    }
    @Override public ResearchTask get(String id) {
        ResearchTaskEntity entity = dao.get(id);
        return entity == null ? null : entity.snapshot();
    }
    public List<ResearchTask> recent(int limit) {
        List<ResearchTask> tasks = new ArrayList<>();
        for (ResearchTaskEntity entity : dao.recent(Math.min(50, limit))) tasks.add(entity.snapshot());
        return tasks;
    }
    public List<ResearchTask> pending() {
        List<ResearchTask> tasks = new ArrayList<>();
        for (ResearchTaskEntity entity : dao.pending()) tasks.add(entity.snapshot());
        return tasks;
    }
    public boolean paused() { return dao.paused() != 0; }
    public void pause(boolean pause, long now) {
        db.runInTransaction(() -> {
            AgentControlEntity control = new AgentControlEntity(); control.paused = pause;
            dao.control(control);
            if (pause) dao.fenceRunning(now);
        });
    }
    public boolean cancel(String id, long now) { return dao.cancel(id, now) == 1; }
    public boolean resume(String id, long now) { return dao.resume(id, now) == 1; }
    @Override public ResearchTask claim(String id, String owner, long now) {
        return db.runInTransaction(() -> {
            if (paused()) return null;
            dao.expireExhausted(id, now);
            return dao.claim(id, owner, now, now + ResearchTaskEngine.LEASE_MS) == 1 ? get(id) : null;
        });
    }
    @Override public boolean owns(String id, String owner) {
        ResearchTask task = get(id);
        return task != null && !task.terminal() && owner.equals(task.owner) && !paused();
    }
    @Override public boolean stage(String id, String owner, String receipt, long now) {
        ResearchReceipt.decode(receipt);
        return dao.stage(id, owner, receipt, now, now + ResearchTaskEngine.LEASE_MS) == 1;
    }
    @Override public boolean finish(String id, String owner, long now) {
        return db.runInTransaction(() -> {
            ResearchTask task = get(id);
            if (task == null || paused() || !owner.equals(task.owner)
                    || !("STAGED".equals(task.status) || "RUNNING".equals(task.status))) return false;
            ResearchReceipt receipt = ResearchReceipt.decode(task.receipt);
            if (dao.complete(id, owner, receipt.completionStatus(), now) != 1) return false;
            RecuerdoEntity memory = new RecuerdoEntity();
            memory.frase = "Investigación pública solicitada: " + task.question
                    + "\nResultado " + receipt.status + " (síntesis o extractos de fuentes, no hecho personal confirmado):\n"
                    + receipt.answer;
            memory.emocion = "no evaluada"; memory.intensidad = 5;
            memory.etiquetas = "[\"investigacion_publica\",\"fuentes_externas\",\"task:" + id + "\"]";
            memory.timestamp = now;
            db.recuerdoDao().insertRecuerdo(memory);
            if (syncEnabled.getAsBoolean()) {
                SyncEventEntity event = new SyncEventEntity();
                event.payload = CloudMemoryImporter.serialize(memory); event.createdAt = now;
                db.syncEventDao().insert(event);
            }
            return true;
        });
    }
    @Override public void release(String id, String owner, String error, long now, boolean retryable) {
        db.runInTransaction(() -> {
            ResearchTask task = get(id);
            if (task == null || !owner.equals(task.owner)) return;
            boolean retry = retryable && (task.receipt != null || task.attempts < ResearchTaskEngine.MAX_ATTEMPTS);
            dao.release(id, owner, retry ? "QUEUED" : "FAILED", error, now,
                    now + Math.min(8, 1L << Math.min(3, task.attempts)) * 60_000L);
        });
    }
}
