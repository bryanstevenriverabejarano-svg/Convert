package salve.data.tasks;

import android.app.Application;
import android.content.Context;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.conversation.GroundedConversationPrompt;
import salve.core.memory.ConversationMemoryGrounding;
import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoEntity;
import salve.data.sync.CloudMemoryImporter;
import static org.junit.Assert.*;

/** Real Room/SQLite triggers and import transactions; no mocked search results. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class IndexedMemoryIntegrationTest {
    private Context context;
    private MemoriaDatabase db;
    @Before public void setup() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("memory-index-test"); open();
    }
    private void open() { db = MemoriaDatabase.builder(context, "memory-index-test").allowMainThreadQueries().build(); }
    @After public void cleanup() { if (db != null) db.close(); context.deleteDatabase("memory-index-test"); }
    private RecuerdoEntity add(String text, long time, String tags) {
        RecuerdoEntity record = new RecuerdoEntity();
        record.frase = text; record.timestamp = time; record.etiquetas = tags;
        db.recuerdoDao().insertRecuerdo(record); return record;
    }
    private ConversationMemoryGrounding.Result recall(String query) {
        return new ConversationMemoryGrounding(db.recuerdoDao(), db.knowledgeNodeDao(), db.knowledgeRelationDao()).retrieve(query);
    }
    private boolean indexed(String term) { return !db.recuerdoDao().buscarIndice("\"" + term + "\"", false, 4).isEmpty(); }
    private void ingest(String event) {
        db.runInTransaction(() -> new CloudMemoryImporter(db.recuerdoDao(), db.memorySyncStateDao()).ingest(event, 1000));
    }
    @Test public void accentsCaseAndShortNamesUseUnicodeWordMatching() {
        add("Ana diseñó el proyecto ORIÓN junto al mar", 1000, "[]");
        String context = recall("¿Recuerdas Ana y el proyecto orion?").getContext();
        assertTrue(context.contains("ORIÓN")); assertTrue(indexed("orion")); assertTrue(indexed("ANA"));
        assertTrue(recall("mar").hasEvidence());
        assertFalse(indexed("rio")); // No substring match inside ORIÓN.
    }
    @Test public void fullTopicFindsOldMemoryBuriedBelowRecentSingleWordMatches() {
        add("Proyecto telescopio presupuesto aprobado", 1000, "[\"pcloud\",\"user_message\"]");
        for (int i = 0; i < 100; i++) {
            add("Proyecto distinto " + i, 2000 + i, "[]");
            add("Presupuesto distinto " + i, 3000 + i, "[]");
            add("Telescopio distinto " + i, 4000 + i, "[]");
        }
        String context = recall("Proyecto telescopio presupuesto").getContext();
        assertTrue(context.contains("presupuesto aprobado"));
        assertTrue(context.indexOf("presupuesto aprobado") < context.indexOf("distinto"));
        assertTrue(context.contains("pcloud_restaurado")); assertTrue(context.length() <= 3200);
    }
    @Test public void controlledVocabularyFindsOriginalWordingWithoutRewritingIt() {
        add("Compré el teléfono Samsung para Salve", 1000, "[\"hecho_usuario\"]");
        String context = recall("¿Recuerdas mi móvil Samsung?").getContext();
        assertTrue(context.contains("Compré el teléfono Samsung"));
        assertTrue(context.contains("BUSQUEDA_AMPLIADA")); assertTrue(context.contains("declaracion_usuario"));
        assertFalse(context.contains("Compré el móvil"));
    }
    @Test public void lexicalMatchBeatsMoreRecentVocabularyExpansion() {
        add("Móvil Samsung elegido", 1000, "[]");
        add("Teléfono Samsung alternativo", 9000, "[]");
        String context = recall("móvil Samsung").getContext();
        assertTrue(context.indexOf("elegido") < context.indexOf("alternativo"));
    }
    @Test public void longMemoryIncludesRelevantVerbatimWindowAndMarksOmissions() {
        add(String.join("", Collections.nCopies(700, "Introducción. "))
                + "El telescopio Orion costó 42 euros", 1000, "[]");
        String context = recall("telescopio Orion").getContext();
        assertTrue(context.contains("costó 42 euros")); assertTrue(context.contains("…"));
        assertTrue(context.length() <= 3200);
    }
    @Test public void researchAndConfigurationDoNotBecomeSharedMemories() {
        add("Investigación viaje Tokio", 500, "[\"investigacion_publica\",\"fuentes_externas\"]");
        add("Manifiesto viaje Tokio", 9000, "[\"manifiesto\"]");
        add("Te conté mi viaje Tokio", 1000, "[\"user_message\",\"pcloud\"]");
        add("Estudio posterior viaje Tokio", 8000, "[\"fuentes_externas\"]");
        for (String query : new String[]{"tu primer recuerdo conmigo", "tu último recuerdo conmigo"})
            assertTrue(recall(query).getDirectAnswer().contains("Te conté mi viaje Tokio"));
        String personal = recall("¿Qué te dije de mi viaje Tokio?").getContext();
        assertTrue(personal.contains("Te conté mi viaje"));
        assertFalse(personal.contains("Investigación viaje")); assertFalse(personal.contains("Estudio posterior"));
        assertFalse(personal.contains("Manifiesto viaje"));
    }
    @Test public void noSharedRecordDoesNotClaimWholeDatabaseEmpty() {
        add("Investigación Saturno", 1000, "[\"fuentes_externas\"]");
        assertFalse(recall("tu primer recuerdo conmigo").getContext().contains("MEMORIA_VACIA"));
        assertTrue(recall("Saturno").getContext().contains("investigacion_publica"));
        assertTrue(recall("tu primer recuerdo").getDirectAnswer().contains("investigación pública"));
    }
    @Test public void conflictingRecordsRemainSeparateDatedEvidence() {
        add("Proyecto telescopio aprobado", 1000, "[\"hecho_usuario\"]");
        add("Proyecto telescopio cancelado", 2000, "[\"hecho_usuario\"]");
        String context = recall("proyecto telescopio").getContext();
        assertTrue(context.contains("aprobado")); assertTrue(context.contains("cancelado"));
        assertTrue(context.contains("1970-01-01T00:00:01Z"));
        assertTrue(context.contains("1970-01-01T00:00:02Z")); assertTrue(context.contains("contradicciones"));
    }
    @Test public void profileCorrectionRemovesObsoleteIndexEntries() {
        ingest("{\"type\":\"profile\",\"category\":\"residence\",\"content\":\"Vivo en Quito\",\"time_ms\":1000}");
        ingest("{\"type\":\"profile\",\"category\":\"residence\",\"content\":\"Ahora vivo en Lima\",\"time_ms\":2000}");
        assertFalse(indexed("Quito")); assertTrue(indexed("Lima"));
        assertTrue(recall("¿Dónde vivo?").getContext().contains("Lima"));
        assertFalse(recall("¿Dónde vivo?").getContext().contains("Quito"));
    }
    @Test public void deletionSurvivesRestartAndOldCloudReplayWithoutIndexResurrection() {
        String old = "{\"type\":\"profile\",\"category\":\"name\",\"content\":\"Me llamo Ana\",\"time_ms\":1000}";
        ingest(old);
        ingest("{\"type\":\"profile_delete\",\"category\":\"name\",\"time_ms\":2000}");
        db.close(); open(); ingest(old);
        assertFalse(indexed("Ana")); assertFalse(recall("quién soy").hasEvidence());
        assertTrue(db.memorySyncStateDao().get("profile:name").deleted);
    }
    @Test public void rollbackRestoresCanonicalMemoryAndIndexTogether() {
        add("Telescopio único", 1000, "[\"remove_me\"]");
        assertThrows(IllegalStateException.class, () -> db.runInTransaction(() -> {
            db.recuerdoDao().eliminarPorEtiqueta("remove_me");
            add("Microscopio nuevo", 2000, "[]"); throw new IllegalStateException("injected");
        }));
        assertTrue(indexed("telescopio")); assertFalse(indexed("microscopio"));
        db.getOpenHelper().getWritableDatabase().execSQL("INSERT INTO recuerdos_fts(recuerdos_fts) VALUES ('integrity-check')");
    }
    @Test public void ordinarySqlUpdatesAreReflectedByRoomTriggers() {
        add("Telescopio antiguo", 1000, "[]");
        db.getOpenHelper().getWritableDatabase().execSQL("UPDATE recuerdos SET frase='Microscopio corregido'");
        assertFalse(indexed("telescopio")); assertTrue(indexed("microscopio"));
    }
    @Test public void cloudImportRetainsOriginalDateAndIsSearchableAfterRestart() {
        RecuerdoEntity record = new RecuerdoEntity(); record.frase = "Celebramos el proyecto Orión";
        record.timestamp = 1234; record.etiquetas = "[\"hecho_usuario\"]";
        ingest(CloudMemoryImporter.serialize(record)); db.close(); open();
        String context = recall("proyecto orion").getContext();
        assertTrue(context.contains("Celebramos")); assertTrue(context.contains("pcloud_restaurado"));
        assertTrue(context.contains("1970-01-01T00:00:01.234Z"));
        assertEquals(0, db.syncEventDao().getPending(10).size());
    }
    @Test public void independentProviderSessionsGetSamePersistentEvidence() {
        add("Proyecto Orion aprobado por Bryan", 1000, "[\"pcloud\",\"hecho_usuario\"]");
        String first = recall("proyecto Orion").getContext();
        db.close(); open(); String second = recall("proyecto Orion").getContext();
        assertEquals(first, second);
        for (String provider : new String[]{"Dolphin 8B", "Dolphin 3B", "Gemma"}) {
            String prompt = GroundedConversationPrompt.build("Proveedor: " + provider, Collections.emptyList(),
                    "proyecto Orion", second, "", "", 10500);
            assertTrue(prompt.contains("aprobado por Bryan"));
        }
    }
    @Test public void literalUserOperatorsCannotExecuteSqlOrBecomeFtsSyntax() {
        add("Proyecto Orion", 1000, "[]");
        ConversationMemoryGrounding.Result result = recall("Proyecto \" OR *; DROP TABLE recuerdos --");
        assertTrue(result.hasEvidence());
        assertNotEquals(ConversationMemoryGrounding.Status.ERROR, result.getStatus());
        assertTrue(indexed("Orion"));
    }
    @Test public void partialCloudRestoreStillQualifiesFirstMemoryAnswer() {
        add("Primer registro disponible", 1000, "[\"pcloud\",\"user_message\"]");
        ConversationMemoryGrounding.Result result = recall("tu primer recuerdo conmigo")
                .withCloudStatus("Restauración pendiente", false);
        assertEquals(ConversationMemoryGrounding.Status.PARTIAL, result.getStatus());
        assertTrue(result.getDirectAnswer().startsWith("Entre los registros disponibles"));
        assertTrue(result.getDirectAnswer().contains("Restauración pendiente"));
    }
}
