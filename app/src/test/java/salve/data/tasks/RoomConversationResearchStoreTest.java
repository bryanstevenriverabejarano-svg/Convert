package salve.data.tasks;

import android.app.Application;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.memory.MemoryProvenance;
import salve.core.tasks.ResearchReceiptTest;
import salve.data.db.MemoriaDatabase;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class RoomConversationResearchStoreTest {
    private Context context;
    private MemoriaDatabase db;
    private RoomConversationResearchStore store;
    private boolean sync = true;
    private final String turn = UUID.randomUUID().toString();
    @Before public void setup() { context = RuntimeEnvironment.getApplication(); context.deleteDatabase("conversation-research-test"); open(); }
    private void open() {
        db = MemoriaDatabase.builder(context, "conversation-research-test").allowMainThreadQueries().build();
        store = new RoomConversationResearchStore(db, () -> sync);
    }
    @After public void close() { db.close(); context.deleteDatabase("conversation-research-test"); }
    private boolean archive() { return store.archive(turn, "Origen de Salve", ResearchReceiptTest.evidence(), 123, () -> false); }

    @Test public void ordinaryResearchSurvivesRestartAndKeepsSourceAttribution() {
        assertTrue(archive()); db.close(); open();
        assertTrue(store.latest().frase.contains("https://example.org/topic"));
        assertTrue(store.latest().frase.contains("Origen de Salve"));
        assertEquals(123, store.latest().timestamp);
        assertFalse(MemoryProvenance.isPersonalCandidate(store.latest()));
        assertNull(db.recuerdoDao().primerRecuerdoCompartido());
        assertEquals(1, db.recuerdoDao().buscarIndice("\"origen\"", false, 4).size());
        assertEquals(0, db.recuerdoDao().buscarIndice("\"origen\"", true, 4).size());
    }
    @Test public void retryDoesNotDuplicateMemoryOrCloudEvent() {
        archive(); archive();
        assertEquals(1, db.recuerdoDao().filtrarRecuerdos("Investigación pública").size());
        assertEquals(1, db.syncEventDao().getPending(20).size());
    }
    @Test public void localModeArchivesWithoutCloudUpload() {
        sync = false; archive(); assertNotNull(store.latest());
        assertEquals(0, db.syncEventDao().getPending(20).size());
    }
    @Test public void cloudFailureRollsBackBothWritesAndAllowsRetry() {
        db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER reject_conversation_outbox BEFORE INSERT ON sync_events BEGIN SELECT RAISE(ABORT, 'test disk failure'); END");
        assertThrows(RuntimeException.class, this::archive); assertNull(store.latest());
        db.getOpenHelper().getWritableDatabase().execSQL("DROP TRIGGER reject_conversation_outbox");
        assertTrue(archive()); assertEquals(1, db.syncEventDao().getPending(20).size());
    }
    @Test public void cancelledTurnCannotArchiveLateResults() {
        assertFalse(store.archive(turn, "Salve", ResearchReceiptTest.evidence(), 123, () -> true));
        assertNull(store.latest()); assertEquals(0, db.syncEventDao().getPending(20).size());
    }
    @Test public void cancellationDuringCommitRollsBackMemoryAndOutbox() {
        AtomicInteger checks = new AtomicInteger();
        assertFalse(store.archive(turn, "Salve", ResearchReceiptTest.evidence(), 123, () -> checks.incrementAndGet() == 2));
        assertNull(store.latest()); assertEquals(0, db.syncEventDao().getPending(20).size());
    }
    @Test public void noSourcesCannotBeArchived() {
        salve.core.tasks.ResearchReceipt receipt = ResearchReceiptTest.evidence(); receipt.sources.clear();
        assertThrows(IllegalArgumentException.class, () -> store.archive(turn, "Salve", receipt, 123, () -> false));
        assertNull(store.latest());
    }
}
