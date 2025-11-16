package salve.core;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ai.onnxruntime.*;

/**
 * EmbeddingsIndex administra un índice local de vectores de texto para
 * realizar búsquedas semánticas. Utiliza ONNX Runtime para ejecutar un
 * modelo de embeddings en el dispositivo. Este código carga un modelo
 * (por defecto gte-small-int8) desde la carpeta assets/embeddings, y
 * proporciona utilidades para obtener los vectores, normalizarlos y
 * serializarlos en BLOBs para almacenarlos en SQLite.
 */
public class EmbeddingsIndex implements AutoCloseable {
    private final OrtEnvironment env;
    private final OrtSession session;
    private final WordPiece tokenizer;
    private final int dim = 384;      // dimensión de los embeddings
    private final int maxLen = 256;   // longitud máxima del tokenizador

    public EmbeddingsIndex(Context ctx) throws Exception {
        env = OrtEnvironment.getEnvironment();
        File m = new File(ctx.getFilesDir(), "embeddings/gte-small-int8.onnx");
        if (!m.exists()) {
            copyAsset(ctx, "embeddings/gte-small-int8.onnx", m);
        }
        session = env.createSession(m.getAbsolutePath(), new OrtSession.SessionOptions());
        InputStream vocabStream = ctx.getAssets().open("embeddings/vocab.txt");
        tokenizer = new WordPiece(vocabStream);
    }

    public float[] embed(String text) throws Exception {
        if (text == null) {
            return new float[0];
        }
        int[] ids = tokenizer.tokenizeToIds(text, maxLen);
        long[] shape = new long[]{1, maxLen};

        LongBuffer idsBuf = LongBuffer.allocate(maxLen);
        for (int id : ids) idsBuf.put((long) id);
        idsBuf.rewind();
        OnnxTensor inputIds = OnnxTensor.createTensor(env, idsBuf, shape);

        LongBuffer att = LongBuffer.allocate(maxLen);
        int padId = 0;
        for (int id : ids) att.put(id == padId ? 0L : 1L);
        att.rewind();
        OnnxTensor attMask = OnnxTensor.createTensor(env, att, shape);

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("input_ids", inputIds);
        inputs.put("attention_mask", attMask);

        try (OrtSession.Result r = session.run(inputs)) {
            float[] vec = extract(r);
            l2norm(vec);
            return vec;
        }
    }

    // Extrae el vector embebido de la salida de la red.
    private float[] extract(OrtSession.Result r) throws Exception {
        String[] names = new String[]{"sentence_embedding", "pooled_output", "last_hidden_state"};
        for (String name : names) {
            try {
                // get() devuelve Optional<OnnxValue>:contentReference[oaicite:1]{index=1}
                Optional<OnnxValue> optional = r.get(name);
                if (optional == null || !optional.isPresent()) continue;
                OnnxValue v = optional.get();

                if (v.getValue() instanceof float[][]) {
                    float[][] arr = (float[][]) v.getValue();
                    return arr[0];
                }
                if (v.getValue() instanceof float[][][]) {
                    float[][][] arr = (float[][][]) v.getValue();
                    float[] mean = new float[dim];
                    int count = 0;
                    for (int t = 0; t < arr[0].length; t++) {
                        for (int h = 0; h < dim; h++) {
                            mean[h] += arr[0][t][h];
                        }
                        count++;
                    }
                    for (int h = 0; h < dim; h++) {
                        mean[h] /= Math.max(1, count);
                    }
                    return mean;
                }
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("No se pudo extraer el embedding de la salida");
    }

    private void l2norm(float[] v) {
        double sum = 0.0;
        for (float x : v) sum += x * x;
        float norm = (float) Math.sqrt(sum) + 1e-8f;
        for (int i = 0; i < v.length; i++) v[i] /= norm;
    }

    public static float cosine(float[] a, float[] b) {
        float s = 0.0f;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    public static byte[] toBlob(float[] v) {
        ByteBuffer bb = ByteBuffer.allocate(v.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float x : v) bb.putFloat(x);
        return bb.array();
    }

    public static float[] fromBlob(byte[] blob) {
        FloatBuffer fb = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer();
        float[] v = new float[fb.remaining()];
        fb.get(v);
        return v;
    }

    private static void copyAsset(Context ctx, String asset, File dst) throws IOException {
        dst.getParentFile().mkdirs();
        try (InputStream in = ctx.getAssets().open(asset);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception ignored) {
        }
    }
}
