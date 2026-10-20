package salve.data.continuity;

import android.app.Application;
import android.content.Context;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.continuity.MemorySnapshot;
import salve.data.db.MemoriaDatabase;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class SalveContinuityTest {
    Context context;

    @Before public void setup() { context = RuntimeEnvironment.getApplication(); }
    @After public void clean() { SalveContinuity.deleteTree(new File(context.getNoBackupFilesDir(), "continuity")); }

    static byte[] text(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    static byte[] sqlite(int version) {
        byte[] b = new byte[4096];
        byte[] magic = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, b, 0, magic.length);
        b[63] = (byte) version;
        return b;
    }

    @Test public void collectTakesAConsistentDatabaseSnapshotAndMindFiles() throws Exception {
        File mind = new File(context.getNoBackupFilesDir(), "mind"); mind.mkdirs();
        Files.write(new File(mind, "self_model.json").toPath(), text("{\"narrative\":\"Soy Salve\"}"));
        Files.write(new File(mind, "memory_vectors_x.bin").toPath(), new byte[]{1, 2});   // derivable: no viaja
        Files.write(new File(context.getNoBackupFilesDir(), "private-family.enc").toPath(), new byte[]{9});   // claves: nunca
        MemoriaDatabase.getInstance(context).getOpenHelper().getWritableDatabase();
        Map<String, byte[]> entries = new SalveContinuity(context).collectUnchecked();
        assertTrue(entries.containsKey(MemorySnapshot.DATABASE));
        assertEquals(SalveContinuity.SCHEMA, MemorySnapshot.userVersion(entries.get(MemorySnapshot.DATABASE)));
        assertEquals("{\"narrative\":\"Soy Salve\"}", new String(entries.get("nobackup/mind/self_model.json"), StandardCharsets.UTF_8));
        for (String name : entries.keySet()) assertTrue(name, MemorySnapshot.allowed(name));
        assertFalse(new File(context.getNoBackupFilesDir(), "continuity/instantanea.db").exists());
        // La instantánea se valida igual que una copia que vuelve del Guardián.
        assertNotNull(MemorySnapshot.unpack(MemorySnapshot.pack(entries, 1, SalveContinuity.SCHEMA), SalveContinuity.MIN_SCHEMA, SalveContinuity.SCHEMA));
    }

    @Test public void restoreIsStagedAndAppliedAtStartKeepingThePreviousMemory() throws Exception {
        File real = context.getDatabasePath("memoria.db");
        real.getParentFile().mkdirs();
        Files.write(real.toPath(), text("memoria vieja"));
        Files.write(new File(real.getPath() + "-wal").toPath(), text("wal viejo"));
        File mind = new File(context.getNoBackupFilesDir(), "mind"); mind.mkdirs();
        Files.write(new File(mind, "self_model.json").toPath(), text("{\"v\":\"vieja\"}"));
        Files.write(new File(mind, "memory_vectors_e5.bin").toPath(), new byte[]{1});

        Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
        entries.put(MemorySnapshot.DATABASE, sqlite(8));
        entries.put("nobackup/mind/self_model.json", text("{\"v\":\"restaurada\"}"));
        entries.put("files/sketch/learned.json", text("[]"));
        MemorySnapshot.Contents contents = MemorySnapshot.unpack(MemorySnapshot.pack(entries, 5, 8), 4, 8);
        SalveContinuity.stage(context, contents);
        assertTrue(SalveContinuity.pending(context));
        assertEquals("memoria vieja", read(real));   // nada cambia en caliente

        SalveContinuity.applyPending(context);
        assertFalse(SalveContinuity.pending(context));
        assertArrayEquals(sqlite(8), Files.readAllBytes(real.toPath()));
        assertFalse(new File(real.getPath() + "-wal").exists());
        assertEquals("{\"v\":\"restaurada\"}", read(new File(mind, "self_model.json")));
        assertEquals("[]", read(new File(context.getFilesDir(), "sketch/learned.json")));
        assertFalse(new File(mind, "memory_vectors_e5.bin").exists());
        File previous = new File(context.getNoBackupFilesDir(), "continuity/previous");
        assertEquals("memoria vieja", read(new File(previous, "db/memoria.db")));
        assertEquals("wal viejo", read(new File(previous, "db/memoria.db-wal")));
        assertEquals("{\"v\":\"vieja\"}", read(new File(previous, "nobackup/mind/self_model.json")));
        SalveContinuity.applyPending(context);   // sin nada pendiente no hace nada
        assertArrayEquals(sqlite(8), Files.readAllBytes(real.toPath()));
    }

    @Test public void markerWithForeignNamesNeverWritesOutsideTheWhitelist() throws Exception {
        File pending = new File(context.getNoBackupFilesDir(), "continuity/pending"); pending.mkdirs();
        File evil = new File(pending, "x.json"); Files.write(evil.toPath(), text("{}"));
        Files.write(new File(pending, "READY").toPath(), text("../../shared_prefs/x.json\nnobackup/../x.json\n"));
        SalveContinuity.applyPending(context);
        assertFalse(new File(context.getFilesDir().getParentFile(), "shared_prefs/x.json").exists());
        assertFalse(SalveContinuity.pending(context));
    }
}
