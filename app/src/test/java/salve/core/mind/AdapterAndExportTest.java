package salve.core.mind;

import static org.junit.Assert.*;
import static salve.core.mind.MindTestSupport.list;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import org.junit.Test;

public class AdapterAndExportTest {
    @Test public void adapterMustMatchBaseModelAndHash() throws Exception {
        File dir = Files.createTempDirectory("salve-adapter").toFile();
        File adapter = new File(dir, "salve-lora.gguf");
        Files.write(adapter.toPath(), "GGUF-test".getBytes("UTF-8"));
        String sha = AdapterManifest.sha256(adapter);
        AdapterManifest manifest = AdapterManifest.parse("{\"base\":\"Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf\",\"scale\":0.8,\"sha256\":\"" + sha + "\"}");
        assertNull(manifest.rejectionFor("/data/models/Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf", adapter));
        assertNotNull(manifest.rejectionFor("/data/models/otro.gguf", adapter));
        Files.write(adapter.toPath(), "alterado".getBytes("UTF-8"));
        assertTrue(manifest.rejectionFor("/data/models/Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf", adapter).contains("SHA-256"));
        assertEquals(0.8f, manifest.scale, 1e-6);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidHashIsRejected() {
        AdapterManifest.parse("{\"base\":\"x.gguf\",\"sha256\":\"no\"}");
    }

    @Test public void exportKeepsOnlySourcedConfidentKnowledge() {
        SelfModelJournal journal = MindTestSupport.journal(new MindTestSupport.MemoryStore(), new MindTestSupport.Clock());
        journal.apply(new MindPatch("sueño")
                .belief("bryan", "Bryan revisa facturas los lunes", 0.9, list("recuerdos:1", "recuerdos:2"))
                .belief("bryan", "Quizá le guste el jazz", 0.3, list("recuerdos:3"))
                .thought("Pensamiento sin fuente", Collections.emptyList())
                .thought("Pensamiento con fuente", list("recuerdos:1")));
        String jsonl = MindDatasetExporter.knowledge(journal);
        assertTrue(jsonl.contains("\"type\":\"narrative\""));
        assertTrue(jsonl.contains("Bryan revisa facturas los lunes"));
        assertFalse(jsonl.contains("jazz"));
        assertFalse(jsonl.contains("Pensamiento sin fuente"));
        assertTrue(jsonl.contains("Pensamiento con fuente"));
    }
}
