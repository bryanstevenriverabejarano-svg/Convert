package salve.core;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.util.Log;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.memory.MemoryEvaluationProbe;
import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoEntity;

/**
 * Opt-in on-device Room/FTS integration test, using synthetic records in a
 * separate in-memory database. No production memories, cloud data or Gemma
 * output are accessed or exported. It verifies the real Android retrieval
 * path and latency plumbing, not a model-quality benchmark.
 *
 * ./gradlew :app:connectedDebugAndroidTest \\
 *   -Pandroid.testInstrumentationRunnerArguments.runMemoryEvidence=true \\
 *   -Pandroid.testInstrumentationRunnerArguments.class=salve.core.MemoryEvidenceDeviceTest
 */
public final class MemoryEvidenceDeviceTest {
    @Test public void syntheticRoomRetrievalProducesPrivateMeasuredReceipt() {
        assumeTrue("Memory evidence test requires explicit opt-in",
                "true".equals(InstrumentationRegistry.getArguments().getString("runMemoryEvidence")));
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MemoriaDatabase db = Room.inMemoryDatabaseBuilder(context, MemoriaDatabase.class)
                .allowMainThreadQueries().build();
        try {
            RecuerdoEntity row = new RecuerdoEntity();
            row.frase = "Proyecto telescopio lunar y sus lentes";
            row.timestamp = 1000L;
            row.etiquetas = "[]";
            db.recuerdoDao().insertRecuerdo(row);

            ConversationMemoryGrounding memory = new ConversationMemoryGrounding(db.recuerdoDao(),
                    db.knowledgeNodeDao(), db.knowledgeRelationDao());
            byte[] syntheticKey = new byte[32]; // test fixture only, never use for private memories.
            java.util.Arrays.fill(syntheticKey, (byte) 42);

            long begin = System.nanoTime();
            ConversationMemoryGrounding.Result hit = memory.retrieve("telescopio lunar");
            MemoryEvaluationProbe.Observation observation = MemoryEvaluationProbe.capture(
                    hit, System.nanoTime() - begin, syntheticKey);
            assertEquals("OK", observation.status);
            assertEquals(1, observation.retrieved.size());
            assertTrue(observation.retrieved.get(0).matches("[a-f0-9]{32}"));
            assertFalse(observation.toJson().toString().contains(row.frase));

            begin = System.nanoTime();
            MemoryEvaluationProbe.Observation absent = MemoryEvaluationProbe.capture(
                    memory.retrieve("zirconiaqui"), System.nanoTime() - begin, syntheticKey);
            assertEquals("EMPTY", absent.status);
            assertTrue(absent.retrieved.isEmpty());

            // Log only short metrics, never the synthetic (or real) source text.
            Log.i("Salve/MemoryEval", "room_fts=OK count=" + observation.retrieved.size()
                    + " read_ms=" + observation.durationMs
                    + " empty_status=" + absent.status + " empty_ms=" + absent.durationMs);
        } finally {
            db.close();
        }
    }
}
