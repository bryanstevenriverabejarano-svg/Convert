package salve.core.embeddings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;

/** An immutable, coupled model/tokenizer/pooling contract. No English fallback or mixed vectors. */
public final class EmbeddingArtifacts {
    public static final String REVISION = "614241f622f53c4eeff9890bdc4f31cfecc418b3";
    public static final String MODEL_SHA = "dd476dd0c2514e9b9be83aeb3853fac0763e0bdf4a71645407587d77c48a2d88";
    public static final String TOKENIZER_SHA = "cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865";
    public static final String ID = "e5-small-dd476dd0-cfc8146a-mean-prefix256-v1";
    private static final String BASE = "https://huggingface.co/intfloat/multilingual-e5-small/resolve/" + REVISION + "/";
    public static final String MODEL_URL = BASE + "onnx/model_qint8_avx512_vnni.onnx";
    public static final String TOKENIZER_URL = BASE + "sentencepiece.bpe.model";
    public static final long MODEL_BYTES = 118346824L;
    public static final long TOKENIZER_BYTES = 5069051L;
    public static final long TOTAL_BYTES = MODEL_BYTES + TOKENIZER_BYTES;
    public final File directory, model, tokenizer;
    private final File ready;

    public EmbeddingArtifacts(File root) {
        directory = new File(root, ID);
        model = new File(directory, "model.onnx");
        tokenizer = new File(directory, "sentencepiece.bpe.model");
        ready = new File(directory, "ready");
    }
    public boolean installed() {
        try {
            return model.length() == MODEL_BYTES && tokenizer.length() == TOKENIZER_BYTES && ready.isFile()
                    && ready.length() < 200 && ID.equals(new String(Files.readAllBytes(ready.toPath()), StandardCharsets.UTF_8));
        } catch (IOException failure) { return false; }
    }
    public void verify() throws Exception {
        if (!installed()) throw new IOException("Descarga de memoria en español pendiente o incompleta");
        try {
            verifyFile(model, MODEL_BYTES, MODEL_SHA);
            verifyFile(tokenizer, TOKENIZER_BYTES, TOKENIZER_SHA);
        } catch (Exception corrupt) {
            if (!(corrupt instanceof java.io.InterruptedIOException)) Files.deleteIfExists(ready.toPath());
            throw corrupt;
        }
    }
    /** Call only after BOTH VerifiedModelFile downloads passed length and SHA-256 checks. */
    public void markVerified() throws IOException {
        if (model.length() != MODEL_BYTES || tokenizer.length() != TOKENIZER_BYTES)
            throw new IOException("Pareja de embeddings incompleta");
        File pending = new File(directory, "ready.pending");
        Files.write(pending.toPath(), ID.getBytes(StandardCharsets.UTF_8));
        Files.move(pending.toPath(), ready.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    private static void verifyFile(File file, long size, String expected) throws Exception {
        if (!file.isFile() || file.length() != size) throw new IOException("Tamaño del artefacto incorrecto");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException();
                digest.update(buffer, 0, n);
            }
        }
        StringBuilder hash = new StringBuilder();
        for (byte b : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        if (!expected.equals(hash.toString())) throw new IOException("SHA-256 de embeddings incorrecto");
    }
}
