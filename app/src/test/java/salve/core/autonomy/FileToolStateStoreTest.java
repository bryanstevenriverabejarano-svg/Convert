package salve.core.autonomy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class FileToolStateStoreTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void absentStateAndUnicodeSurviveAtomicReplacement() throws Exception {
        File file = new File(folder.getRoot(), "autonomy/tools.json");
        FileToolStateStore store = new FileToolStateStore(file);
        assertNull(store.read());
        store.write("{\"nota\":\"estrategia comprobada: óptima\"}");
        assertEquals("{\"nota\":\"estrategia comprobada: óptima\"}", new FileToolStateStore(file).read());
        store.write("{\"revision\":2}");
        assertEquals("{\"revision\":2}", store.read());
        assertArrayEquals(new String[] {"tools.json"}, file.getParentFile().list());
    }

    @Test public void utf8ByteLimitPreservesPreviousState() throws Exception {
        File file = folder.newFile();
        FileToolStateStore store = new FileToolStateStore(file);
        store.write("previous");
        String unicode = repeat("é", FileToolStateStore.MAX_BYTES / 2 + 1);
        expectIo(() -> store.write(unicode));
        assertEquals("previous", store.read());
    }

    @Test public void exactLimitRoundTrips() throws Exception {
        FileToolStateStore store = new FileToolStateStore(folder.newFile());
        String value = repeat("x", FileToolStateStore.MAX_BYTES);
        store.write(value);
        assertEquals(value, store.read());
    }

    @Test public void oversizedExistingStateIsNotTruncated() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), new byte[FileToolStateStore.MAX_BYTES + 1]);
        expectIo(() -> new FileToolStateStore(file).read());
        assertEquals(FileToolStateStore.MAX_BYTES + 1, Files.size(file.toPath()));
    }

    @Test public void invalidUtf8AndUnpairedSurrogateAreRejected() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), new byte[] {(byte) 0xc3, (byte) 0x28});
        FileToolStateStore store = new FileToolStateStore(file);
        expectIo(store::read);
        store.write("previous");
        expectIo(() -> store.write("\ud800"));
        assertEquals("previous", store.read());
    }

    @Test public void nullWriteDoesNotReplaceState() throws Exception {
        FileToolStateStore store = new FileToolStateStore(folder.newFile());
        store.write("previous");
        expectIo(() -> store.write(null));
        assertEquals("previous", store.read());
    }

    @Test public void nonDirectoryParentIsPreserved() throws Exception {
        File parent = folder.newFile();
        Files.write(parent.toPath(), "previous".getBytes(StandardCharsets.UTF_8));
        expectIo(() -> new FileToolStateStore(new File(parent, "tools.json")).write("next"));
        assertEquals("previous", new String(Files.readAllBytes(parent.toPath()), StandardCharsets.UTF_8));
    }

    @Test public void failedAtomicReplacementCleansTemporaryAndPreservesDirectory() throws Exception {
        File destination = folder.newFolder("tools.json");
        File nested = new File(destination, "keep");
        Files.write(nested.toPath(), "previous".getBytes(StandardCharsets.UTF_8));
        expectIo(() -> new FileToolStateStore(destination).write("next"));
        assertEquals("previous", new String(Files.readAllBytes(nested.toPath()), StandardCharsets.UTF_8));
        assertArrayEquals(new String[] {"tools.json"}, folder.getRoot().list());
    }

    private static String repeat(String value, int count) {
        StringBuilder out = new StringBuilder(count * value.length());
        for (int i = 0; i < count; i++) out.append(value);
        return out.toString();
    }

    private static void expectIo(Throwing action) throws Exception {
        try { action.run(); fail("Expected storage failure"); }
        catch (IOException expected) { }
    }

    private interface Throwing { void run() throws Exception; }
}
