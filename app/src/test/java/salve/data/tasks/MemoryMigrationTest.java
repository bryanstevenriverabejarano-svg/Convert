package salve.data.tasks;

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.data.db.MemoriaDatabase;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class MemoryMigrationTest {
    private Context context;
    private MemoriaDatabase migrated;
    @Before public void setup() { context = RuntimeEnvironment.getApplication(); context.deleteDatabase("migration-test"); }
    @After public void cleanup() { if (migrated != null) migrated.close(); context.deleteDatabase("migration-test"); }
    private void legacy(int version) throws Exception {
        context.getDatabasePath("migration-test").getParentFile().mkdirs();
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("migration-test"), null);
             InputStream input = getClass().getResourceAsStream("/salve/memory-v4.sql")) {
            assertNotNull(input);
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            for (String statement : sql.split(";")) if (!statement.trim().isEmpty()) db.execSQL(statement);
            db.execSQL("INSERT INTO recuerdos VALUES (7, 'Mi nombre es Bryan', 'no evaluada', 7, '[\"profile:name\"]', '0101', 1234)");
            db.execSQL("INSERT INTO misiones VALUES (8, 'Conservar mi historia')");
            db.execSQL("INSERT INTO reflexiones VALUES (9, 'nota', 'Contenido conservado', 0.2, 'curiosidad', 'declarado', 0.3, 'pendiente', 1235)");
            db.execSQL("INSERT INTO plugins VALUES (10, 'local', '1', '/private/tool', 0.4, 1236)");
            db.execSQL("INSERT INTO sync_events VALUES (11, '{\"type\":\"user_message\"}', 1237, 2)");
            db.execSQL("INSERT INTO knowledge_nodes VALUES (12, 'Salve', 'tema', 'curiosidad', 'Resumen', '[]', 1, 1238)");
            db.execSQL("INSERT INTO knowledge_nodes VALUES (13, 'Bryan', 'tema', 'curiosidad', 'Resumen', '[]', 1, 1238)");
            db.execSQL("INSERT INTO knowledge_relations VALUES (14, 12, 13, 'conecta', 0.5, 'Asociación', 1239)");
            if (version == 5) {
                db.execSQL("CREATE TABLE memory_sync_state (`key` TEXT NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY(`key`))");
                db.execSQL("INSERT INTO memory_sync_state VALUES ('profile:residence', 9000, 1)");
            }
            db.setVersion(version);
        }
    }
    private void assertMigrated() {
        migrated = MemoriaDatabase.builder(context, "migration-test").allowMainThreadQueries().build();
        assertEquals("Mi nombre es Bryan", migrated.recuerdoDao().primerRecuerdo().frase);
        assertEquals(1234, migrated.recuerdoDao().primerRecuerdo().timestamp);
        assertEquals(7, migrated.recuerdoDao().primerRecuerdo().id);
        assertEquals(6, migrated.getOpenHelper().getWritableDatabase().getVersion());
        assertEquals(0, migrated.researchTaskDao().pendingCount());
        assertEquals(2, migrated.syncEventDao().getPending(1).get(0).tries);
        for (String table : new String[]{"misiones", "reflexiones", "plugins", "knowledge_relations"})
            try (Cursor rows = migrated.query("SELECT COUNT(*) FROM " + table, null)) {
                assertTrue(rows.moveToFirst()); assertEquals(1, rows.getInt(0));
            }
        assertEquals("Salve", migrated.knowledgeNodeDao().findById(12).etiqueta);
    }
    @Test public void migratesHistoricalFourThroughFiveToSixWithoutLoss() throws Exception { legacy(4); assertMigrated(); }
    @Test public void migratesFiveAndPreservesProfileDeletionRevision() throws Exception {
        legacy(5); assertMigrated();
        assertTrue(migrated.memorySyncStateDao().get("profile:residence").deleted);
        assertEquals(9000, migrated.memorySyncStateDao().get("profile:residence").updatedAt);
    }
    @Test public void unknownOlderVersionFailsWithoutErasingAnyRows() throws Exception {
        legacy(3); // Unsupported version marker, deliberately not claimed as a historical v3 schema.
        migrated = MemoriaDatabase.builder(context, "migration-test").allowMainThreadQueries().build();
        assertThrows(IllegalStateException.class, () -> migrated.getOpenHelper().getWritableDatabase());
        migrated.close(); migrated = null;
        try (SQLiteDatabase original = SQLiteDatabase.openDatabase(context.getDatabasePath("migration-test").getPath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor rows = original.rawQuery("SELECT frase FROM recuerdos WHERE id = 7", null)) {
            assertEquals(3, original.getVersion()); assertTrue(rows.moveToFirst()); assertEquals("Mi nombre es Bryan", rows.getString(0));
        }
    }
    @Test public void freshInstallCanOpenAllNewTables() {
        migrated = MemoriaDatabase.builder(context, "migration-test").allowMainThreadQueries().build();
        assertEquals(0, migrated.researchTaskDao().pendingCount()); assertEquals(0, migrated.researchTaskDao().paused());
    }
}
