package salve.core.visual;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import salve.data.db.*;
import static org.junit.Assert.*;

public class VisualMemoryGraphTest {
    @Test public void twoPhotosOfNamedPersonShareNodeAndCorrectionPreservesOtherPhoto() {
        Graph graph = new Graph();
        VisualMemoryRecord first = VisualMemoryRecord.create("Una foto", "Bryan", "yo", "única persona", false);
        VisualMemoryRecord second = VisualMemoryRecord.create("Otra foto", "Bryan", "yo", "izquierda", false);
        graph.save(first); graph.save(second);
        long bryan = graph.nodes.get(VisualMemoryRecord.personKey("Bryan")).id;
        long firstPhoto = graph.nodes.get("foto:" + first.id).id;
        long secondPhoto = graph.nodes.get("foto:" + second.id).id;
        assertEquals(3, graph.nodes.size()); assertEquals(2, graph.edges.size());
        graph.save(first.identify("Diego", "hermano", "única persona"));
        assertEquals(2, graph.edges.size());
        assertTrue(graph.edges.stream().anyMatch(e -> e.origenId == bryan && e.destinoId == secondPhoto));
        assertFalse(graph.edges.stream().anyMatch(e -> e.origenId == bryan && e.destinoId == firstPhoto));
        assertEquals(2, graph.memories.size());
        assertTrue(graph.memories.get("visual:" + first.id).frase.contains("Diego"));
    }

    @Test public void analysisAloneNeverCreatesPersonNodeAndRemovalOnlyDeletesPhotoIdentityEdge() {
        Graph graph = new Graph();
        VisualMemoryRecord photo = VisualMemoryRecord.create("Soy yo", "", "", "", false).withAnalysis("Se parece a Bryan");
        graph.save(photo); assertEquals(1, graph.nodes.size()); assertTrue(graph.edges.isEmpty());
        photo = photo.identify("Bryan", "yo", "centro"); graph.save(photo);
        assertEquals(1, graph.edges.size());
        graph.save(photo.identify("", "", "")); assertTrue(graph.edges.isEmpty());
        assertTrue(graph.nodes.containsKey("foto:" + photo.id)); assertEquals(1, graph.memories.size());
    }

    private static final class Graph {
        final Map<String, KnowledgeNodeEntity> nodes = new HashMap<>();
        final Map<String, RecuerdoEntity> memories = new HashMap<>();
        final List<KnowledgeRelationEntity> edges = new ArrayList<>();
        long next = 1;
        final KnowledgeNodeDao nodeDao = (KnowledgeNodeDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{KnowledgeNodeDao.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "findByEtiqueta": return nodes.get(args[0]);
                        case "insert": {
                            KnowledgeNodeEntity n = (KnowledgeNodeEntity) args[0]; n.id = next++;
                            nodes.put(n.etiqueta, n); return n.id;
                        }
                        case "update": {
                            KnowledgeNodeEntity n = (KnowledgeNodeEntity) args[0]; nodes.put(n.etiqueta, n); return null;
                        }
                        default: throw new AssertionError(method.getName());
                    }
                });
        final KnowledgeRelationDao relationDao = (KnowledgeRelationDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{KnowledgeRelationDao.class}, (proxy, method, args) -> {
                    if (method.getName().equals("deleteVisualIdentity")) {
                        edges.removeIf(e -> e.destinoId == (Long) args[0] && e.tipoRelacion.equals(args[1])); return null;
                    }
                    if (method.getName().equals("insert")) { edges.add((KnowledgeRelationEntity) args[0]); return next++; }
                    throw new AssertionError(method.getName());
                });
        final RecuerdoDao memoryDao = (RecuerdoDao) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{RecuerdoDao.class}, (proxy, method, args) -> {
                    if (method.getName().equals("reemplazarPorEtiqueta")) { memories.put((String) args[0], (RecuerdoEntity) args[1]); return null; }
                    throw new AssertionError(method.getName());
                });
        void save(VisualMemoryRecord record) { VisualMemoryGraph.index(memoryDao, nodeDao, relationDao, record); }
    }
}
