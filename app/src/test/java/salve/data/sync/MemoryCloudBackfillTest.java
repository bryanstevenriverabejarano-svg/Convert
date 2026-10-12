package salve.data.sync;

import android.app.Application;
import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.memory.ConversationMemoryGrounding;
import salve.data.db.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28,application=Application.class)
public class MemoryCloudBackfillTest {
    private Context context;
    private MemoriaDatabase db;
    @Before public void setup() { context=RuntimeEnvironment.getApplication(); context.deleteDatabase("memory-backfill-test"); open(); }
    private void open() { db=MemoriaDatabase.builder(context,"memory-backfill-test").allowMainThreadQueries().build(); }
    @After public void cleanup() { db.close(); context.deleteDatabase("memory-backfill-test"); }
    private void add(String text,long time,String tags) {
        RecuerdoEntity row=new RecuerdoEntity(); row.frase=text; row.timestamp=time; row.etiquetas=tags;
        db.recuerdoDao().insertRecuerdo(row);
    }
    @Test public void pausedMemoriesAreQueuedAfterResumeAndCursorSurvivesRestart() {
        add("Recuerdo local antiguo",1000,"[\"hecho_usuario\"]");
        add("Recuerdo ya recuperado",1500,"[\"pcloud\"]");
        add("Otro recuerdo durante la pausa",2000,"[]");
        assertTrue(MemoryCloudBackfill.enqueueBatch(db,2,()->false));
        assertTrue(db.syncEventDao().getPending(10).isEmpty());
        assertTrue(MemoryCloudBackfill.enqueueBatch(db,2,()->true));
        assertEquals(1,db.syncEventDao().getPending(10).size());
        db.close(); open();
        assertFalse(MemoryCloudBackfill.enqueueBatch(db,2,()->true));
        assertEquals(2,db.syncEventDao().getPending(10).size());
        assertFalse(MemoryCloudBackfill.enqueueBatch(db,2,()->true));
        assertEquals(2,db.syncEventDao().getPending(10).size());
        assertTrue(db.syncEventDao().getPending(10).get(0).payload.contains("1000"));
    }
    @Test public void uploadFailureTwentyTimesRemainsPendingAndDoesNotStarveNewItems() {
        add("primero",1000,"[]"); add("segundo",2000,"[]");
        MemoryCloudBackfill.enqueueBatch(db,200,()->true);
        long first=db.syncEventDao().getPending(1).get(0).id;
        for(int i=0;i<20;i++) db.syncEventDao().incTries(first);
        assertEquals(2,db.syncEventDao().getPending(10).size());
        assertNotEquals(first,db.syncEventDao().getPending(1).get(0).id);
        db.syncEventDao().markSynced(db.syncEventDao().getPending(1).get(0).id);
        assertEquals(first,db.syncEventDao().getPending(1).get(0).id);
    }
    @Test public void serializedMemoryCanRestoreToSameSharedEvidenceStoreWithoutDuplicates() {
        add("Conocimos el proyecto Orión",1000,"[\"hecho_usuario\"]");
        MemoryCloudBackfill.enqueueBatch(db,200,()->true);
        String payload=db.syncEventDao().getPending(1).get(0).payload;
        Context app=context;
        MemoriaDatabase recovered=MemoriaDatabase.builder(app,"memory-recovered-test").allowMainThreadQueries().build();
        try {
            recovered.runInTransaction(()->{
                CloudMemoryImporter importer=new CloudMemoryImporter(recovered.recuerdoDao(),recovered.memorySyncStateDao());
                assertTrue(importer.ingest(payload,9999)); assertFalse(importer.ingest(payload,9999));
            });
            assertEquals(1,recovered.recuerdoDao().pageForCloud(0,200).size());
            for(int model=0;model<3;model++) {
                ConversationMemoryGrounding memory=new ConversationMemoryGrounding(recovered.recuerdoDao(),null,null);
                assertTrue(memory.retrieve("¿Recuerdas el proyecto orion?").getContext().contains("Orión"));
                assertTrue(memory.retrieve("tu primer recuerdo conmigo").getDirectAnswer().contains("Orión"));
            }
        } finally { recovered.close(); context.deleteDatabase("memory-recovered-test"); }
    }
}
