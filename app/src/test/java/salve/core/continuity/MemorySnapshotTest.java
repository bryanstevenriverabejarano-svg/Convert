package salve.core.continuity;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemorySnapshotTest {
    static byte[] db(int version) {
        byte[] b = new byte[4096];
        byte[] magic = "SQLite format 3\0".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(magic, 0, b, 0, magic.length);
        b[60] = (byte) (version >>> 24); b[61] = (byte) (version >>> 16); b[62] = (byte) (version >>> 8); b[63] = (byte) version;
        return b;
    }

    static Map<String, byte[]> mind(int version) {
        Map<String, byte[]> m = new LinkedHashMap<>();
        m.put(MemorySnapshot.DATABASE, db(version));
        m.put("nobackup/mind/self_model.json", "{\"narrative\":\"Soy Salve\"}".getBytes(StandardCharsets.UTF_8));
        m.put("files/sketch/learned.json", "[]".getBytes(StandardCharsets.UTF_8));
        return m;
    }

    @Test public void roundTripKeepsEveryFileIntact() throws Exception {
        MemorySnapshot.Contents c = MemorySnapshot.unpack(MemorySnapshot.pack(mind(8), 99, 8), 4, 8);
        assertEquals(99, c.createdAt); assertEquals(8, c.schema);
        assertArrayEquals(mind(8).get("nobackup/mind/self_model.json"), c.entries.get("nobackup/mind/self_model.json"));
        assertArrayEquals(db(8), c.entries.get(MemorySnapshot.DATABASE));
        assertEquals(8, MemorySnapshot.userVersion(db(8)));
    }

    @Test public void refusesSchemasTheAppCannotOpen() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(MemorySnapshot.pack(mind(9), 1, 9), 4, 8));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(MemorySnapshot.pack(mind(3), 1, 3), 4, 8));
        // El manifiesto no puede mentir sobre la versión.
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(MemorySnapshot.pack(mind(8), 1, 7), 4, 8));
    }

    @Test public void onlyWhitelistedNamesNoTraversal() throws Exception {
        assertFalse(MemorySnapshot.allowed("nobackup/../../shared_prefs/x.json"));
        assertFalse(MemorySnapshot.allowed("nobackup/private-family.enc"));
        assertFalse(MemorySnapshot.allowed("db/salventes.db"));
        assertTrue(MemorySnapshot.allowed("nobackup/autonomy/goals.json"));
        Map<String, byte[]> evil = mind(8); evil.put("nobackup/mind/../../../x.json", new byte[2]);
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.pack(evil, 1, 8));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(rawZip("../evil.json", "{}".getBytes()), 4, 8));
    }

    @Test public void detectsTamperingAndDamage() throws Exception {
        Map<String, byte[]> m = mind(8);
        byte[] good = MemorySnapshot.pack(m, 1, 8);
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(new byte[]{1, 2, 3}, 4, 8));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(rawZip(MemorySnapshot.DATABASE, db(8)), 4, 8));   // sin manifiesto
        Map<String, byte[]> broken = mind(8); broken.put("nobackup/mind/lessons.json", "{roto".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(MemorySnapshot.pack(broken, 1, 8), 4, 8));
        Map<String, byte[]> notSqlite = mind(8); notSqlite.put(MemorySnapshot.DATABASE, new byte[200]);
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(MemorySnapshot.pack(notSqlite, 1, 8), 4, 8));
        assertNotNull(MemorySnapshot.unpack(good, 4, 8));
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.pack(new LinkedHashMap<>(), 1, 8));
    }

    @Test public void zipBombIsStopped() throws Exception {
        byte[] huge = new byte[2 * 1024 * 1024];   // ceros: se comprime muchísimo (mismos límites, a escala)
        assertThrows(IllegalArgumentException.class, () -> MemorySnapshot.unpack(rawZip("nobackup/mind/x.json", huge), 4, 8, 1024 * 1024, 4 * 1024 * 1024));
    }

    static byte[] rawZip(String name, byte[] data) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) { zip.putNextEntry(new ZipEntry(name)); zip.write(data); zip.closeEntry(); }
        return bytes.toByteArray();
    }
}
