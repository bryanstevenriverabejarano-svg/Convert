package salve.core.memory;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;

public class SemanticMemoryTest {
    @Test public void nearestReturnsMostSimilarFirstAboveThreshold() {
        MemoryVectorStore store = new MemoryVectorStore();
        store.put(1, new float[]{1, 0, 0});
        store.put(2, new float[]{0.9f, 0.1f, 0});
        store.put(3, new float[]{0, 1, 0});
        List<MemoryVectorStore.Hit> hits = store.nearest(new float[]{1, 0, 0}, 5, 0.5);
        assertEquals(2, hits.size());
        assertEquals(1, hits.get(0).id);
        assertEquals(2, hits.get(1).id);
        assertEquals(1.0, hits.get(0).score, 0.01);
    }

    @Test public void quantizationKeepsRankingOnRealisticVectors() {
        java.util.Random random = new java.util.Random(7);
        MemoryVectorStore store = new MemoryVectorStore();
        float[][] vectors = new float[200][384];
        for (int i = 0; i < vectors.length; i++) {
            for (int d = 0; d < 384; d++) vectors[i][d] = (float) random.nextGaussian();
            store.put(i, vectors[i]);
        }
        float[] query = vectors[42].clone();
        for (int d = 0; d < 384; d++) query[d] += 0.3f * (float) random.nextGaussian();
        assertEquals(42, store.nearest(query, 1, -1).get(0).id);
    }

    @Test public void persistsAndReloads() throws Exception {
        File dir = Files.createTempDirectory("salve-vectors").toFile();
        File file = new File(dir, "v.bin");
        MemoryVectorStore store = new MemoryVectorStore();
        store.put(5, new float[]{0.2f, 0.8f});
        store.put(9, new float[]{0.9f, 0.1f});
        store.save(file);
        assertFalse(store.isDirty());
        MemoryVectorStore loaded = MemoryVectorStore.load(file);
        assertEquals(2, loaded.size());
        assertEquals(9, loaded.nearest(new float[]{1, 0}, 1, 0).get(0).id);
    }

    @Test public void damagedFileStartsEmptyInsteadOfCrashing() throws Exception {
        File file = File.createTempFile("salve", ".bin");
        Files.write(file.toPath(), new byte[]{1, 2, 3});
        assertEquals(0, MemoryVectorStore.load(file).size());
    }

    @Test public void deletedMemoriesStopBeingRecallable() {
        MemoryVectorStore store = new MemoryVectorStore();
        store.put(1, new float[]{1, 0});
        store.put(2, new float[]{1, 0});
        assertEquals(1, store.retainOnly(new HashSet<>(Collections.singletonList(2))));
        assertEquals(Collections.singletonList(2), store.ids());
    }

    @Test(expected = IllegalArgumentException.class)
    public void dimensionMismatchIsRejected() {
        MemoryVectorStore store = new MemoryVectorStore();
        store.put(1, new float[]{1, 0});
        store.put(2, new float[]{1, 0, 0});
    }

    @Test public void reciprocalRankFusionRewardsAgreement() {
        List<Integer> fused = HybridFusion.rrf(3, Arrays.asList(1, 2, 3), Arrays.asList(3, 4));
        assertEquals(Integer.valueOf(3), fused.get(0));
        assertEquals(3, fused.size());
        assertEquals(Arrays.asList(1, 2), HybridFusion.rrf(2, Arrays.asList(1, 2), null));
    }

    @Test public void groundingAddsParaphrasedMemoryThatWordsMiss() {
        Map<Integer, RecuerdoEntity> rows = new LinkedHashMap<>();
        rows.put(4, record(4, "Las ventas de agosto me quitan el sueño", "[\"hecho_usuario\"]"));
        rows.put(6, record(6, "Investigación pública sobre ventas", "[\"investigacion_publica\"]"));
        ConversationMemoryGrounding grounding = new ConversationMemoryGrounding(dao(rows), null, null);
        grounding.setSemanticRecall((query, limit) -> Arrays.asList(4, 6));
        ConversationMemoryGrounding.Result result = grounding.retrieve("¿Qué me preocupaba del negocio?");
        assertTrue(result.getContext(), result.getContext().contains("recuerdos:4"));
        assertTrue(result.getContext().contains("recuerdo_por_significado"));
        assertTrue(result.getContext().contains("COINCIDENCIA_POR_SIGNIFICADO"));
    }

    @Test public void semanticFailureFallsBackToWordSearch() {
        Map<Integer, RecuerdoEntity> rows = new LinkedHashMap<>();
        rows.put(4, record(4, "Las ventas de agosto", "[\"hecho_usuario\"]"));
        ConversationMemoryGrounding grounding = new ConversationMemoryGrounding(dao(rows), null, null);
        grounding.setSemanticRecall((query, limit) -> { throw new IllegalStateException("onnx"); });
        ConversationMemoryGrounding.Result result = grounding.retrieve("¿Qué me preocupaba del negocio?");
        assertNotEquals(ConversationMemoryGrounding.Status.ERROR, result.getStatus());
        assertFalse(result.getContext().contains("recuerdo_por_significado"));
    }

    private static RecuerdoEntity record(int id, String text, String tags) {
        RecuerdoEntity r = new RecuerdoEntity();
        r.id = id;
        r.frase = text;
        r.etiquetas = tags;
        r.timestamp = 1_700_000_000_000L + id;
        return r;
    }

    /** DAO mínimo: la búsqueda por palabras no encuentra nada; porIds devuelve filas reales. */
    private static RecuerdoDao dao(Map<Integer, RecuerdoEntity> rows) {
        return (RecuerdoDao) Proxy.newProxyInstance(RecuerdoDao.class.getClassLoader(), new Class<?>[]{RecuerdoDao.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "buscarIndice": return new ArrayList<RecuerdoEntity>();
                        case "porIds": {
                            List<RecuerdoEntity> out = new ArrayList<>();
                            for (Object id : (List<?>) args[0]) if (rows.containsKey(id)) out.add(rows.get(id));
                            return out;
                        }
                        case "hashCode": return 1;
                        case "equals": return proxy == args[0];
                        case "toString": return "dao";
                        default: return null;
                    }
                });
    }
}
