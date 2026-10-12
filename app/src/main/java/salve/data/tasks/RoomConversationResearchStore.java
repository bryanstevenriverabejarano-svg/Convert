package salve.data.tasks;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import com.google.gson.Gson;
import salve.core.tasks.ResearchReceipt;
import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoEntity;
import salve.data.db.SyncEventEntity;
import salve.data.sync.CloudMemoryImporter;

/** Ordinary chat research joins the same indexed memory and optional cloud outbox as tasks. */
public final class RoomConversationResearchStore {
    public static final String TAG = "busqueda_conversacional";
    private final MemoriaDatabase db;
    private final BooleanSupplier syncEnabled;
    public RoomConversationResearchStore(MemoriaDatabase db, BooleanSupplier syncEnabled) {
        this.db = db; this.syncEnabled = syncEnabled;
    }

    public boolean archive(String turnId, String question, ResearchReceipt receipt, long now,
                           BooleanSupplier stopped) {
        UUID.fromString(turnId);
        if (question == null || question.trim().isEmpty() || question.length() > 2048)
            throw new IllegalArgumentException("Tema inválido");
        receipt.validate();
        String tag = "research-turn:" + turnId;
        try {
            return db.runInTransaction(() -> {
                check(stopped);
                if (db.recuerdoDao().ultimoPorEtiqueta(tag) != null) return true;
                RecuerdoEntity memory = new RecuerdoEntity();
                memory.frase = "Investigación pública solicitada: " + question.trim()
                        + "\nResultado " + receipt.status + " (síntesis o extractos, no hecho personal confirmado):\n"
                        + receipt.answer;
                memory.emocion = "no evaluada"; memory.intensidad = 5; memory.timestamp = now;
                memory.etiquetas = new Gson().toJson(Arrays.asList("investigacion_publica", "fuentes_externas", TAG, tag));
                db.recuerdoDao().insertRecuerdo(memory);
                if (syncEnabled.getAsBoolean()) {
                    SyncEventEntity event = new SyncEventEntity();
                    event.payload = CloudMemoryImporter.serialize(memory); event.createdAt = now;
                    db.syncEventDao().insert(event);
                }
                check(stopped); // Cancelled output rolls back memory and outbox together.
                return true;
            });
        } catch (CancellationException cancelled) { return false; }
    }

    public RecuerdoEntity latest() { return db.recuerdoDao().ultimoPorEtiqueta(TAG); }

    private static void check(BooleanSupplier stopped) {
        if (Thread.currentThread().isInterrupted() || stopped.getAsBoolean()) throw new CancellationException();
    }
}
