package salve.core.embeddings;

import ai.onnxruntime.*;
import java.io.File;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;

/** Shared production inference core, also exercised on the host with the exact downloaded files. */
public final class E5EmbeddingEngine implements AutoCloseable {
    public static final int DIMENSIONS = 384;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final E5Tokenizer tokenizer;

    public E5EmbeddingEngine(File model, File vocabulary) throws Exception {
        environment = OrtEnvironment.getEnvironment();
        tokenizer = new E5Tokenizer(vocabulary);
        try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
            options.setIntraOpNumThreads(2);
            options.setInterOpNumThreads(1);
            options.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL);
            options.setMemoryPatternOptimization(false);
            options.setCPUArenaAllocator(false);
            session = environment.createSession(model.getAbsolutePath(), options);
        } catch (Throwable failure) {
            tokenizer.close();
            throw failure;
        }
        if (!session.getInputNames().contains("input_ids") || !session.getOutputNames().contains("last_hidden_state")) {
            close();
            throw new IllegalStateException("Contrato ONNX de E5 incompatible");
        }
    }
    public synchronized float[] query(String text) throws Exception { return run(tokenizer.query(text)); }
    public synchronized float[] passage(String text) throws Exception { return run(tokenizer.passage(text)); }
    private float[] run(int[] ids) throws Exception {
        long[] values = new long[ids.length], mask = new long[ids.length];
        for (int i = 0; i < ids.length; i++) { values[i] = ids[i]; mask[i] = 1; }
        long[] shape = {1, ids.length}; // dynamic length: do not compute 256 pads for a short query.
        try (OnnxTensor input = OnnxTensor.createTensor(environment, LongBuffer.wrap(values), shape);
             OnnxTensor attention = OnnxTensor.createTensor(environment, LongBuffer.wrap(mask), shape);
             OnnxTensor types = OnnxTensor.createTensor(environment, LongBuffer.allocate(ids.length), shape)) {
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", input); inputs.put("attention_mask", attention);
            if (session.getInputNames().contains("token_type_ids")) inputs.put("token_type_ids", types);
            try (OrtSession.Result result = session.run(inputs)) {
                Object value = result.get("last_hidden_state").orElseThrow(
                        () -> new IllegalStateException("Falta last_hidden_state")).getValue();
                if (!(value instanceof float[][][])) throw new IllegalStateException("Salida E5 incompatible");
                return pool(((float[][][]) value)[0], mask);
            }
        }
    }
    /** Attention-weighted mean and L2 normalization; special tokens are attended, padded positions are not. */
    public static float[] pool(float[][] hidden, long[] mask) {
        if (hidden.length != mask.length) throw new IllegalArgumentException("Máscara incompatible");
        float[] vector = new float[DIMENSIONS];
        int count = 0;
        for (int t = 0; t < hidden.length; t++) {
            if (hidden[t].length != DIMENSIONS) throw new IllegalArgumentException("Dimensión incompatible");
            if (mask[t] == 0) continue;
            count++;
            for (int d = 0; d < DIMENSIONS; d++) {
                if (!Float.isFinite(hidden[t][d])) throw new IllegalArgumentException("Vector no finito");
                vector[d] += hidden[t][d];
            }
        }
        if (count == 0) throw new IllegalArgumentException("Sin tokens atendidos");
        double norm = 0;
        for (int d = 0; d < DIMENSIONS; d++) { vector[d] /= count; norm += vector[d] * vector[d]; }
        if (norm <= 0 || !Double.isFinite(norm)) throw new IllegalArgumentException("Vector inválido");
        double divisor = Math.sqrt(norm);
        for (int d = 0; d < DIMENSIONS; d++) vector[d] /= divisor;
        return vector;
    }
    @Override public synchronized void close() {
        try { session.close(); } catch (OrtException ignored) { }
        finally { tokenizer.close(); }
    }
}
