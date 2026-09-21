package salve.core;

import com.google.gson.Gson;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class AutoImprovementSourceSnapshotTest {
    private static final String SOURCE = "package salve.core;\r\nclass Example { /* ¿Qué tal? */ }\r\n";

    @Test public void preservesExactTextAndRevisionWithDefensiveClassList() {
        AutoImprovementSourceSnapshot snapshot = AutoImprovementSourceSnapshot.parse(json(SOURCE));
        assertEquals(SOURCE, snapshot.source("Example").code);
        assertEquals("a".repeat(40), snapshot.revision);
        snapshot.classNames().clear();
        assertEquals(Arrays.asList("Example"), snapshot.classNames());
        assertNull(snapshot.source("Missing"));
    }

    @Test public void rejectsCorruptionUnknownDuplicateAndTrailingFields() {
        String valid = json(SOURCE);
        for (String invalid : Arrays.asList(valid.replace("¿Qué", "Hola"),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1,\"schemaVersion\":1"),
                valid.replace("\"path\":", "\"extra\":\"x\",\"path\":"),
                valid.replace("\"path\":", "\"path\":\"duplicate\",\"path\":"),
                valid.replace("\"schemaVersion\":1", "\"schemaVersion\":1.0"),
                valid.replace("\"revision\":", "\"extra\":\"x\",\"revision\":"), valid + "{}")) {
            assertThrows(IllegalArgumentException.class, () -> AutoImprovementSourceSnapshot.parse(invalid));
        }
    }

    @Test public void rejectsUnsafePathsOversizeNullAndDuplicateFiles() {
        for (String path : Arrays.asList("../Example.java", "app/build.gradle", "app/src/main/java/salve/core/../Example.java")) {
            String invalid = json(SOURCE).replace("app/src/main/java/salve/core/Example.java", path);
            assertThrows(IllegalArgumentException.class, () -> AutoImprovementSourceSnapshot.parse(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> AutoImprovementSourceSnapshot.parse(json("x".repeat(8001))));
        assertThrows(IllegalArgumentException.class, () -> AutoImprovementSourceSnapshot.parse(json("a\0b")));
        Map<String, Object> root = fixture(SOURCE);
        Object file = ((java.util.List<?>) root.get("files")).get(0);
        root.put("files", Arrays.asList(file, file));
        assertThrows(IllegalArgumentException.class, () -> AutoImprovementSourceSnapshot.parse(new Gson().toJson(root)));
    }

    @Test public void fileReaderRejectsMalformedUtf8AndExcessiveBytes() throws Exception {
        File file = File.createTempFile("salve-source", ".json");
        try {
            Files.write(file.toPath(), new byte[]{(byte) 0xff});
            assertThrows(java.io.IOException.class, () -> AutoImprovementSourceSnapshot.read(file));
            Files.write(file.toPath(), new byte[AutoImprovementSourceSnapshot.MAX_BYTES + 1]);
            assertThrows(java.io.IOException.class, () -> AutoImprovementSourceSnapshot.read(file));
            Files.write(file.toPath(), json(SOURCE).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(SOURCE, AutoImprovementSourceSnapshot.read(file).source("Example").code);
        } finally { file.delete(); }
    }

    private static String json(String source) { return new Gson().toJson(fixture(source)); }

    private static Map<String, Object> fixture(String source) {
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("path", "app/src/main/java/salve/core/Example.java"); file.put("source", source);
        file.put("sha256", AutoImprovementSourceSnapshot.hash(source));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 1); root.put("revision", "a".repeat(40)); root.put("files", Arrays.asList(file));
        return root;
    }
}
