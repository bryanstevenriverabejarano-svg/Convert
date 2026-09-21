package salve.core.goals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class GoalFileStoreTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void absentJournalAndUnicodeRoundTrip() throws Exception {
        File file = new File(folder.getRoot(), "autonomy/goals.json");
        GoalFileStore store = new GoalFileStore(file);
        assertNull(store.read());
        store.write("{\"nota\":\"Quién es Salve: hipótesis revisable\"}");
        assertEquals("{\"nota\":\"Quién es Salve: hipótesis revisable\"}", new GoalFileStore(file).read());
        assertFalse(new File(file.getParentFile(), "goals.json.pending").exists());
    }

    @Test public void oversizedWritePreservesPreviousJournal() throws Exception {
        File file = folder.newFile();
        GoalFileStore store = new GoalFileStore(file);
        store.write("previous");
        try { store.write(new String(new char[128_001])); fail("Should reject oversize"); }
        catch (IOException expected) { assertEquals("previous", store.read()); }
    }

    @Test public void malformedUtf8IsNotSilentlyReplaced() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), new byte[] {(byte) 0xc3, (byte) 0x28});
        try { new GoalFileStore(file).read(); fail("Should reject malformed UTF-8"); }
        catch (IOException expected) { assertEquals(2, Files.size(file.toPath())); }
    }

    @Test public void nonDirectoryParentFailsWithoutTouchingFile() throws Exception {
        File parent = folder.newFile();
        Files.write(parent.toPath(), "preserved".getBytes(StandardCharsets.UTF_8));
        try { new GoalFileStore(new File(parent, "goals.json")).write("new"); fail("Should fail"); }
        catch (IOException expected) {
            assertEquals("preserved", new String(Files.readAllBytes(parent.toPath()), StandardCharsets.UTF_8));
        }
    }

    @Test public void oversizeExistingJournalIsNotTruncated() throws Exception {
        File file = folder.newFile();
        Files.write(file.toPath(), new byte[128_001]);
        try { new GoalFileStore(file).read(); fail("Should reject oversize file"); }
        catch (IOException expected) { assertEquals(128_001, Files.size(file.toPath())); }
    }
}
