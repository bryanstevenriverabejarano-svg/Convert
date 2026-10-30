package salve.data.sync;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import salve.core.autonomy.FileToolStateStore;

public class SkillCloudArchiveTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    static final String TOOLS = "{\"schema\":1,\"revision\":0,\"tools\":{},\"regressions\":{},\"receipts\":[]}";
    static final String INDUCTIVE = "{\"schema\":1,\"skills\":{\"sort_numbers\":"
            + "{\"program\":[\"sort\"],\"training\":3,\"holdout\":3}}}";

    @Test public void reusesRealToolAndInductiveDecodersAndBindsFilenameToDigest() {
        for (String kind : Arrays.asList(SkillCloudArchive.TOOLS, SkillCloudArchive.INDUCTIVE)) {
            String payload = kind.equals(SkillCloudArchive.TOOLS) ? TOOLS : INDUCTIVE;
            String packed = SkillCloudArchive.pack(kind, payload, 123456789L);
            String remote = SkillCloudArchive.ROOT + "/" + kind + "-123456789-"
                    + SkillCloudArchive.sha256(payload) + ".json";
            SkillCloudArchive.Snapshot validated = SkillCloudArchive.unpack(packed, remote);
            assertEquals(kind, validated.kind);
            assertEquals(payload, validated.payload);
            assertEquals(remote, validated.remotePath());
            assertTrue(SkillCloudArchive.recognizedPath(remote));
            assertThrows(IllegalArgumentException.class,
                    () -> SkillCloudArchive.unpack(packed, remote.replace("123456789-", "123456788-")));
            assertThrows(IllegalArgumentException.class,
                    () -> SkillCloudArchive.unpack(packed.replace("sort_numbers", "other_skill"), remote));
        }
    }

    @Test public void rejectsTamperingScriptsUnknownSkillsAndOversizedData() {
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.pack("javascript", "{}", 1));
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE,
                        "{\"schema\":1,\"skills\":{\"evil\":{\"program\":[\"shell\"],"
                        + "\"training\":3,\"holdout\":3}}}", 1));
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.pack(SkillCloudArchive.TOOLS,
                        "{\"schema\":1,\"revision\":0,\"tools\":{\"evil\":{}},"
                        + "\"regressions\":{},\"receipts\":[]}", 1));
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.unpack("{\"schema\":99,\"payload\":\"x\"}", null));
        assertFalse(SkillCloudArchive.recognizedPath(
                "/Salve/autonomy/verified/../tools-10-" + "0".repeat(64) + ".json"));
        assertFalse(SkillCloudArchive.recognizedPath("/Salve/autonomy/verified/tools.js"));
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.pack(SkillCloudArchive.TOOLS, "x".repeat(600_000), 1));
    }

    @Test public void cloudWorkerStagesButNeverInstallsOrReplacesInstalledSkills() throws Exception {
        File root = temp.newFolder("autonomy");
        File active = new File(root, "inductive-skills.json");
        File inbox = new File(new File(root, "cloud-inbox"), active.getName());
        String envelope = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE, INDUCTIVE, 555L);
        String path = SkillCloudArchive.unpack(envelope, null).remotePath();
        assertTrue(SkillCloudArchive.stageIfMissing(active, inbox, envelope, path));
        assertTrue(inbox.isFile());
        assertFalse(active.exists());
        assertFalse(SkillCloudArchive.stageIfMissing(active, inbox, envelope, path));
        assertFalse(SkillCloudArchive.installIfApproved(active, inbox, SkillCloudArchive.INDUCTIVE, false));
        assertFalse(active.exists());
        assertTrue(SkillCloudArchive.installIfApproved(active, inbox, SkillCloudArchive.INDUCTIVE, true));
        assertEquals(INDUCTIVE, new FileToolStateStore(active).read());
        assertTrue(inbox.isFile());
        assertFalse(SkillCloudArchive.installIfApproved(active, inbox, SkillCloudArchive.INDUCTIVE, true));
        // Remote changes never overwrite a locally verified catalog.
        String changed = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE,
                "{\"schema\":1,\"skills\":{}}", 556L);
        assertFalse(SkillCloudArchive.stageIfMissing(active, inbox, changed,
                SkillCloudArchive.unpack(changed, null).remotePath()));
        assertEquals(INDUCTIVE, Files.readString(active.toPath(), StandardCharsets.UTF_8));
    }

    @Test public void corruptStagedContentNeverActivatesAndMissingLocalIsNotOverwritten() throws Exception {
        File root = temp.newFolder("autonomy");
        File active = new File(root, "tools.json");
        File inbox = new File(new File(root, "cloud-inbox"), active.getName());
        String envelope = SkillCloudArchive.pack(SkillCloudArchive.TOOLS, TOOLS, 9876L);
        String path = SkillCloudArchive.unpack(envelope, null).remotePath();
        assertFalse(SkillCloudArchive.stageIfMissing(active, inbox, envelope,
                path.replace("tools-", "inductive-")));
        assertFalse(inbox.exists());
        assertTrue(SkillCloudArchive.stageIfMissing(active, inbox, envelope, path));
        new FileToolStateStore(inbox).write(envelope.replace("revision", "tampered"));
        assertThrows(IllegalArgumentException.class,
                () -> SkillCloudArchive.installIfApproved(active, inbox, SkillCloudArchive.TOOLS, true));
        assertFalse(active.exists());
        File existing = new File(root, "inductive-skills.json");
        new FileToolStateStore(existing).write(INDUCTIVE);
        assertFalse(SkillCloudArchive.stageIfMissing(existing,
                new File(new File(root, "cloud-inbox"), "inductive-skills.json"),
                SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE, INDUCTIVE, 55L),
                SkillCloudArchive.ROOT + "/inductive-55-" + SkillCloudArchive.sha256(INDUCTIVE) + ".json"));
    }

    @Test public void choosesNewestRecognizedArchiveWithoutImportingAnyContent() {
        String digest = "a".repeat(64);
        String older = SkillCloudArchive.ROOT + "/tools-100-" + digest + ".json";
        String newer = SkillCloudArchive.ROOT + "/tools-200-" + digest + ".json";
        List<String> paths = SkillCloudArchive.candidates(SkillCloudArchive.TOOLS,
                Arrays.asList(newer, older, "/Salve/autonomy/verified/evil.js",
                        SkillCloudArchive.ROOT + "/inductive-300-" + digest + ".json"));
        assertEquals(Arrays.asList(newer, older), paths);
        assertTrue(SkillCloudArchive.candidates(SkillCloudArchive.TOOLS, null).isEmpty());
    }
}
