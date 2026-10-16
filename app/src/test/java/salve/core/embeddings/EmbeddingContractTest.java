package salve.core.embeddings;

import static org.junit.Assert.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class EmbeddingContractTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void downloadPairIsNotReadyUntilBothVerifiedArtifactsAreCommitted() throws Exception {
        EmbeddingArtifacts files = new EmbeddingArtifacts(temporary.getRoot());
        assertFalse(files.installed());
        assertTrue(files.directory.mkdirs());
        try (RandomAccessFile model = new RandomAccessFile(files.model, "rw")) { model.setLength(EmbeddingArtifacts.MODEL_BYTES); }
        try { files.markVerified(); fail("incomplete pair"); } catch (java.io.IOException expected) { }
        try (RandomAccessFile tokenizer = new RandomAccessFile(files.tokenizer, "rw")) { tokenizer.setLength(EmbeddingArtifacts.TOKENIZER_BYTES); }
        assertFalse(files.installed());
        files.markVerified(); // represents the successful transfer boundary; loading still checks hashes.
        assertTrue(files.installed());
        try { files.verify(); fail("size alone is not integrity"); } catch (java.io.IOException expected) { }
        assertFalse(files.installed());
    }
    @Test public void wrongContractCannotReuseAReadyPair() throws Exception {
        EmbeddingArtifacts files = new EmbeddingArtifacts(temporary.getRoot());
        assertTrue(files.directory.mkdirs());
        try (RandomAccessFile m = new RandomAccessFile(files.model, "rw")) { m.setLength(EmbeddingArtifacts.MODEL_BYTES); }
        try (RandomAccessFile t = new RandomAccessFile(files.tokenizer, "rw")) { t.setLength(EmbeddingArtifacts.TOKENIZER_BYTES); }
        Files.write(new File(files.directory, "ready").toPath(), "gte-small".getBytes(StandardCharsets.UTF_8));
        assertFalse(files.installed());
    }
    @Test public void poolingUsesAttentionAndNormalizesInsteadOfTreatingTokenIdZeroAsPadding() {
        float[][] hidden = new float[3][384];
        hidden[0][0] = 3; hidden[1][1] = 4; hidden[2][2] = 10000;
        float[] vector = E5EmbeddingEngine.pool(hidden, new long[]{1, 1, 0});
        assertEquals(.6, vector[0], 1e-6); assertEquals(.8, vector[1], 1e-6); assertEquals(0, vector[2], 0);
    }
    @Test public void badModelOutputFailsClosed() {
        float[][] hidden = new float[1][384]; hidden[0][2] = Float.NaN;
        try { E5EmbeddingEngine.pool(hidden, new long[]{1}); fail(); } catch (IllegalArgumentException expected) { }
        try { E5EmbeddingEngine.pool(new float[1][384], new long[]{0}); fail(); } catch (IllegalArgumentException expected) { }
    }
    @Test public void ramAdmissionPreservesAndroidThresholdAndSupportsResidentEngine() {
        long m = EmbeddingMemoryPolicy.MIB;
        assertTrue(EmbeddingMemoryPolicy.allows(2723*m, 11080*m, 300*m, false, false));
        assertFalse(EmbeddingMemoryPolicy.allows(600*m, 11080*m, 300*m, false, false));
        assertTrue(EmbeddingMemoryPolicy.allows(600*m, 11080*m, 300*m, false, true));
        assertFalse(EmbeddingMemoryPolicy.allows(900*m, 11080*m, 1000*m, false, true));
        assertFalse(EmbeddingMemoryPolicy.allows(4000*m, 11080*m, 300*m, true, false));
        assertFalse(EmbeddingMemoryPolicy.allows(0, 0, 0, false, false));
    }
}
