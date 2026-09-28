package salve.data.tasks;

import android.app.Application;
import android.content.Context;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.tasks.ResearchReceipt;
import salve.core.tasks.ResearchReceiptTest;
import salve.core.tasks.ResearchTask;
import salve.core.tasks.ResearchTaskEngine;
import salve.data.db.MemoriaDatabase;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class RoomResearchTaskStoreTest {
    private Context context;
    private MemoriaDatabase db;
    private RoomResearchTaskStore store;
    private long now = 10_000;
    private boolean sync = true;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("durable-test"); open();
    }
    private void open() {
        db = MemoriaDatabase.builder(context, "durable-test").allowMainThreadQueries().build();
        store = new RoomResearchTaskStore(db, () -> sync);
    }
    @After public void close() { if (db != null) db.close(); context.deleteDatabase("durable-test"); }
    private ResearchTask create() { return store.create("Origen del nombre Salve", now); }
    private ResearchTaskEngine engine(ResearchTaskEngine.Researcher researcher) {
        return new ResearchTaskEngine(store, researcher, () -> now);
    }
    private int memories() { return db.recuerdoDao().filtrarRecuerdos("Investigación pública").size(); }
    @Test public void completedRunArchivesOnceAndEnqueuesExactlyOneCloudEvent() {
        ResearchTask task = create(); AtomicInteger reads = new AtomicInteger();
        ResearchTaskEngine runner = engine((q, stop) -> { reads.incrementAndGet(); return ResearchReceiptTest.evidence(); });
        assertEquals("SUCCEEDED", runner.run(task.id, () -> false).status);
        runner.run(task.id, () -> false);
        assertEquals(1, reads.get()); assertEquals(1, memories());
        assertEquals(1, db.syncEventDao().getPending(20).size());
        assertTrue(db.syncEventDao().getPending(20).get(0).payload.contains("fuentes_externas"));
        assertEquals(0, db.recuerdoDao().perfiles().size());
    }
    @Test public void processRestartAfterEvidenceDoesNotRepeatResearch() {
        ResearchTask task = create(); store.claim(task.id, "old-process", now);
        store.stage(task.id, "old-process", ResearchReceiptTest.evidence().encode(), now);
        db.close(); now += ResearchTaskEngine.LEASE_MS + 1; open();
        ResearchTask result = engine((q, stopped) -> { fail("The stored receipt must be reused"); return null; }).run(task.id, () -> false);
        assertEquals("SUCCEEDED", result.status); assertEquals(1, result.attempts); assertEquals(1, memories());
    }
    @Test public void competingWorkersAndStaleResultsAreFenced() {
        ResearchTask task = create(); assertNotNull(store.claim(task.id, "first", now));
        assertNull(store.claim(task.id, "second", now));
        now += ResearchTaskEngine.LEASE_MS + 1;
        assertNotNull(store.claim(task.id, "second", now));
        assertFalse(store.stage(task.id, "first", ResearchReceiptTest.evidence().encode(), now));
        assertFalse(store.finish(task.id, "first", now)); assertEquals(0, memories());
    }
    @Test public void cancellationDuringToolExecutionDiscardsLateOutput() {
        ResearchTask task = create();
        ResearchTask result = engine((q, stop) -> {
            store.cancel(task.id, now); assertTrue(stop.getAsBoolean()); return ResearchReceiptTest.evidence();
        }).run(task.id, () -> false);
        assertEquals("CANCELLED", result.status); assertNull(result.receipt); assertEquals(0, memories());
    }
    @Test public void pausePersistsAndResumeCannotAcceptAnOldOwner() {
        ResearchTask task = create(); store.claim(task.id, "old", now); store.pause(true, now);
        db.close(); open(); assertTrue(store.paused()); assertNull(store.claim(task.id, "new", now));
        store.pause(false, now); assertNotNull(store.claim(task.id, "new", now));
        assertFalse(store.stage(task.id, "old", ResearchReceiptTest.evidence().encode(), now));
    }
    @Test public void outboxFailureRollsBackMemoryAndCompletionTogether() {
        ResearchTask task = create(); store.claim(task.id, "worker", now);
        store.stage(task.id, "worker", ResearchReceiptTest.evidence().encode(), now);
        SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();
        sqlite.execSQL("CREATE TRIGGER reject_outbox BEFORE INSERT ON sync_events BEGIN SELECT RAISE(ABORT, 'test disk failure'); END");
        assertThrows(RuntimeException.class, () -> store.finish(task.id, "worker", now));
        assertEquals("STAGED", store.get(task.id).status); assertEquals(0, memories());
        sqlite.execSQL("DROP TRIGGER reject_outbox");
        assertTrue(store.finish(task.id, "worker", now)); assertEquals(1, memories());
    }
    @Test public void interruptedReadCanResumeWithinPersistentBudget() {
        ResearchTask task = create();
        assertEquals("QUEUED", engine((q, stop) -> { throw new ResearchTaskEngine.Unavailable(true); }).run(task.id, () -> false).status);
        assertNull(store.claim(task.id, "too-early", now));
        now += 10 * 60_000;
        assertEquals("SUCCEEDED", engine((q, stop) -> ResearchReceiptTest.evidence()).run(task.id, () -> false).status);
        assertEquals(2, store.get(task.id).attempts);
    }
    @Test public void repeatedProcessDeathExhaustsBudgetWithoutAnInfiniteLoop() {
        ResearchTask task = create();
        for (int i = 0; i < 3; i++) {
            assertNotNull(store.claim(task.id, "process-" + i, now));
            now += ResearchTaskEngine.LEASE_MS + 1;
        }
        assertNull(store.claim(task.id, "fourth", now));
        assertEquals("FAILED", store.get(task.id).status); assertEquals(3, store.get(task.id).attempts);
        assertTrue(store.resume(task.id, now)); assertNotNull(store.claim(task.id, "explicit-retry", now));
    }
    @Test public void noSourcesFailsWithoutInventingOrArchivingAResult() {
        ResearchTask task = create();
        assertEquals("FAILED", engine((q, stop) -> { throw new ResearchTaskEngine.Unavailable(false); }).run(task.id, () -> false).status);
        assertEquals(0, memories()); assertEquals(0, db.syncEventDao().getPending(10).size());
    }
    @Test public void sourcesWithoutModelAreArchivedAsPartial() {
        ResearchTask task = create();
        ResearchTask result = engine((q, stop) -> {
            ResearchReceipt receipt = ResearchReceiptTest.evidence(); receipt.status = "SOURCES_ONLY"; return receipt;
        }).run(task.id, () -> false);
        assertEquals("PARTIAL", result.status); assertEquals(1, memories());
    }
    @Test public void localOnlyArchiveDoesNotQueueCloudUpload() {
        sync = false; ResearchTask task = create();
        engine((q, stop) -> ResearchReceiptTest.evidence()).run(task.id, () -> false);
        assertEquals(1, memories()); assertEquals(0, db.syncEventDao().getPending(10).size());
    }
    @Test public void cancelAndResumePreservesEvidenceButInvalidatesOldCompletion() {
        ResearchTask task = create(); store.claim(task.id, "old", now);
        store.stage(task.id, "old", ResearchReceiptTest.evidence().encode(), now);
        store.cancel(task.id, now); store.resume(task.id, now);
        assertFalse(store.finish(task.id, "old", now));
        assertEquals("SUCCEEDED", engine((q, stop) -> { fail("Do not refetch"); return null; }).run(task.id, () -> false).status);
    }
    @Test public void queuedTaskCountAndTopicSizeAreBounded() {
        for (int i = 0; i < 20; i++) create();
        assertThrows(IllegalArgumentException.class, this::create);
        assertThrows(IllegalArgumentException.class, () -> store.create("x".repeat(2049), now));
    }
    @Test public void initialPauseIsInheritedButDoesNotOverwriteLaterUserChoice() {
        store.initializeControl(true); assertTrue(store.paused());
        store.pause(false, now); store.initializeControl(true); assertFalse(store.paused());
    }
}
