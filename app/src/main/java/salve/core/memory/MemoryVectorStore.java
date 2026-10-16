package salve.core.memory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Índice vectorial local de recuerdos (búsqueda por significado).
 *
 * <p>Guarda un vector cuantizado a 8 bits por recuerdo (~390 bytes con 384 dimensiones) y busca
 * por fuerza bruta, que para decenas de miles de recuerdos tarda milisegundos en un móvil.
 * Es un índice derivado: si se pierde, se reconstruye desde Room. Un recuerdo borrado debe
 * eliminarse también aquí ({@link #remove} o {@link #retainOnly}).</p>
 */
public final class MemoryVectorStore {
    public static final int MAX_ENTRIES = 50_000;
    private static final int MAGIC = 0x53565831; // "SVX1"

    public static final class Hit {
        public final int id;
        public final double score;
        Hit(int id, double score) { this.id = id; this.score = score; }
    }

    private static final class Entry {
        final float scale; final byte[] values;
        Entry(float scale, byte[] values) { this.scale = scale; this.values = values; }
    }

    private final LinkedHashMap<Integer, Entry> entries = new LinkedHashMap<>();
    private int dim = -1;
    private boolean dirty;

    public synchronized int size() { return entries.size(); }
    public synchronized boolean contains(int id) { return entries.containsKey(id); }
    public synchronized boolean isDirty() { return dirty; }

    public synchronized void put(int id, float[] vector) {
        if (vector == null || vector.length == 0) throw new IllegalArgumentException("Vector vacío.");
        if (dim < 0) dim = vector.length;
        if (vector.length != dim) throw new IllegalArgumentException("Dimensión distinta: " + vector.length + " != " + dim);
        float[] normalized = normalize(vector);
        float max = 0;
        for (float v : normalized) max = Math.max(max, Math.abs(v));
        float scale = max == 0 ? 1f : max / 127f;
        byte[] q = new byte[dim];
        for (int i = 0; i < dim; i++) q[i] = (byte) Math.round(normalized[i] / scale);
        entries.remove(id);
        entries.put(id, new Entry(scale, q));
        while (entries.size() > MAX_ENTRIES) {
            Iterator<Integer> it = entries.keySet().iterator();
            it.next();
            it.remove();
        }
        dirty = true;
    }

    public synchronized boolean remove(int id) {
        boolean removed = entries.remove(id) != null;
        if (removed) dirty = true;
        return removed;
    }

    /** Elimina vectores de recuerdos que ya no existen. Devuelve cuántos se borraron. */
    public synchronized int retainOnly(Set<Integer> existing) {
        int before = entries.size();
        entries.keySet().retainAll(existing);
        int removed = before - entries.size();
        if (removed > 0) dirty = true;
        return removed;
    }

    public synchronized List<Integer> ids() { return new ArrayList<>(entries.keySet()); }

    /**
     * Bounded candidates for hybrid retrieval, not confirmed facts. E5 is trained for relative
     * ranking: a cutoff fitted to eight calibration phrases rejects valid unseen paraphrases.
     */
    public List<Hit> candidates(float[] query, int k) {
        return nearest(query, Math.min(k, 16), -1.000001);
    }

    /** Los k recuerdos más cercanos con similitud coseno >= minScore, de mayor a menor. */
    public synchronized List<Hit> nearest(float[] query, int k, double minScore) {
        List<Hit> out = new ArrayList<>();
        if (query == null || query.length != dim || k <= 0) return out;
        float[] q = normalize(query);
        double[] scores = new double[k];
        int[] ids = new int[k];
        int count = 0;
        for (Map.Entry<Integer, Entry> e : entries.entrySet()) {
            Entry entry = e.getValue();
            double dot = 0, norm = 0;
            for (int i = 0; i < dim; i++) {
                double v = entry.values[i] * entry.scale;
                dot += q[i] * v;
                norm += v * v;
            }
            double score = norm == 0 ? 0 : dot / Math.sqrt(norm);
            if (score < minScore) continue;
            if (count < k) {
                int pos = count++;
                while (pos > 0 && scores[pos - 1] < score) { scores[pos] = scores[pos - 1]; ids[pos] = ids[pos - 1]; pos--; }
                scores[pos] = score; ids[pos] = e.getKey();
            } else if (score > scores[k - 1]) {
                int pos = k - 1;
                while (pos > 0 && scores[pos - 1] < score) { scores[pos] = scores[pos - 1]; ids[pos] = ids[pos - 1]; pos--; }
                scores[pos] = score; ids[pos] = e.getKey();
            }
        }
        for (int i = 0; i < count; i++) out.add(new Hit(ids[i], scores[i]));
        return out;
    }

    public synchronized void save(File file) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Almacenamiento no disponible.");
        File temporary = new File(parent, file.getName() + ".pending");
        try {
            try (FileOutputStream fos = new FileOutputStream(temporary);
                 DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos))) {
                out.writeInt(MAGIC);
                out.writeInt(Math.max(dim, 0));
                out.writeInt(entries.size());
                for (Map.Entry<Integer, Entry> e : entries.entrySet()) {
                    out.writeInt(e.getKey());
                    out.writeFloat(e.getValue().scale);
                    out.write(e.getValue().values);
                }
                out.flush();
                fos.getFD().sync();
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            dirty = false;
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    /** Carga un índice guardado. Un archivo dañado se ignora: el índice se reconstruye después. */
    public static MemoryVectorStore load(File file) {
        MemoryVectorStore store = new MemoryVectorStore();
        if (file == null || !file.isFile()) return store;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (in.readInt() != MAGIC) return new MemoryVectorStore();
            int dim = in.readInt();
            int count = in.readInt();
            if (dim <= 0 || dim > 4096 || count < 0 || count > MAX_ENTRIES) return new MemoryVectorStore();
            store.dim = dim;
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                float scale = in.readFloat();
                byte[] values = new byte[dim];
                in.readFully(values);
                store.entries.put(id, new Entry(scale, values));
            }
            store.dirty = false;
            return store;
        } catch (IOException | RuntimeException damaged) {
            return new MemoryVectorStore();
        }
    }

    private static float[] normalize(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        float norm = (float) Math.sqrt(sum);
        float[] out = new float[v.length];
        if (norm == 0) return out;
        for (int i = 0; i < v.length; i++) out[i] = v[i] / norm;
        return out;
    }
}
