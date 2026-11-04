package salve.core;

import static org.junit.Assert.*;
import java.util.UUID;
import org.junit.Test;

public class LocalModelImportNameTest {
    private static final UUID ID = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

    @Test public void preservesQ40SuffixAfterImport() {
        String saved = LocalModelImportName.forDisplayName("Gemma-E2B-Q4_0.gguf", ID);
        assertTrue(saved.endsWith("-Q4_0.gguf"));
        assertTrue(GgufNpuPolicy.eligible("SM8650", "/files/" + saved));
    }
    @Test public void importDoesNotMisclassifyOtherQuantizationsAsNpu() {
        String saved = LocalModelImportName.forDisplayName("Dolphin-8B-Q4_K_M.gguf", ID);
        assertFalse(GgufNpuPolicy.eligible("SM8650", "/files/" + saved));
    }
    @Test public void unsafeProviderNameCannotLeaveModelsFolder() {
        String saved = LocalModelImportName.forDisplayName("../../other\\evil-Q4_0.gguf", ID);
        assertFalse(saved.contains("/"));
        assertFalse(saved.contains("\\"));
        assertTrue(saved.endsWith("-Q4_0.gguf"));
    }
    @Test public void rejectsUnrelatedFiles() {
        try {
            LocalModelImportName.forDisplayName("file.zip", ID);
            fail("Expected extension validation");
        } catch (IllegalArgumentException expected) { }
    }
}
