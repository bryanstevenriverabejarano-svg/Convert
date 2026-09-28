package salve.data.sync;

import com.google.gson.JsonObject;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.Test;
import salve.core.memory.ConversationMemoryGrounding;
import salve.data.db.*;
import static org.junit.Assert.*;

public class CloudMemoryImporterTest {
    @Test public void restoredOldHistoryIsAvailableToNewConversationAndModel() {
        Store store = new Store();
        store.importer.ingest(event("user_message", "Te conocí en Madrid", 1000), 9000);
        store.importer.ingest(event("user_message", "Mi nombre es Bryan", 2000), 10000);
        store.importer.ingest(event("user_message", "Esta es la conversación actual", 8000), 11000);
        for (int model = 0; model < 3; model++) {
            ConversationMemoryGrounding memory = new ConversationMemoryGrounding(store.memories, null, null);
            assertTrue(memory.retrieve("¿Cuál es tu primer recuerdo conmigo?").getDirectAnswer().contains("Madrid"));
            assertTrue(memory.retrieve("¿Quién soy yo?").getContext().contains("Bryan"));
            assertTrue(memory.retrieve("¿Quién soy yo?").getContext().contains("pcloud_restaurado"));
        }
    }

    @Test public void replayAndLocalDuplicateDoNotCreateExtraMemories() {
        Store store = new Store();
        String payload = event("memory", "Un recuerdo íntegro", 1234);
        assertTrue(store.importer.ingest(payload, 9000));
        assertFalse(store.importer.ingest(payload, 9000));
        assertEquals(1, store.rows.size());
        assertEquals(1234, store.rows.get(0).timestamp);
    }

    @Test public void fullProfileSerializationKeepsValueTagsAndDate() {
        Store store = new Store();
        RecuerdoEntity saved = new RecuerdoEntity();
        saved.frase = "Mi nombre es Bryan"; saved.etiquetas = "[\"hecho_usuario\",\"profile:name\"]";
        saved.timestamp = 3000; saved.emocion = "neutral"; saved.intensidad = 7;
        store.importer.ingest(CloudMemoryImporter.serialize(saved), 9999);
        assertEquals(saved.frase, store.profile("profile:name").frase);
        assertEquals(3000, store.profile("profile:name").timestamp);
    }

    @Test public void olderProfileCannotReplaceNewerOrResurrectDeletedValue() {
        Store store = new Store();
        store.importer.ingest(event("user_message", "Me llamo Bryan", 3000), 3000);
        store.importer.ingest(event("user_message", "Me llamo NombreViejo", 1000), 1000);
        assertTrue(store.profile("profile:name").frase.contains("Bryan"));
        assertTrue(store.importer.ingest("{\"type\":\"profile_delete\",\"category\":\"name\",\"time_ms\":4000}", 4000));
        assertFalse(store.importer.ingest(event("user_message", "Me llamo Bryan", 3000), 3000));
        assertFalse(store.importer.ingest(event("user_message", "Me llamo Bryan", 4000), 4000));
        assertNull(store.profile("profile:name"));
        assertTrue(store.importer.ingest(event("user_message", "Me llamo Bryan de nuevo", 5000), 5000));
        assertTrue(store.profile("profile:name").frase.contains("de nuevo"));
    }

    @Test public void oldCategoryOnlyLogAndAssistantTextCannotInventUserIdentity() {
        Store store = new Store();
        assertFalse(store.importer.ingest(event("memoria_perfil", "name", 1000), 1000));
        assertFalse(store.importer.ingest(event("assistant_message", "Me llamo Inventado", 1000), 1000));
        assertNull(store.profile("profile:name"));
    }

    @Test public void legacyDuplicateCannotTurnConfigurationIntoASharedMemory() {
        Store store = new Store();
        store.importer.ingest(event("memoria_manual", "Manifiesto instalado", 1001), 1001);
        String full = "{\"type\":\"memory\",\"content\":\"Manifiesto instalado\",\"time_ms\":1000,\"tags\":[\"manifiesto\"]}";
        store.importer.ingest(full, 1000);
        assertEquals(1, store.rows.size());
        assertTrue(store.rows.get(0).etiquetas.contains("manifiesto"));
        assertFalse(store.importer.ingest(event("memoria_manual", "Manifiesto instalado", 1001), 1001));
    }

    @Test public void localJournalRepairDoesNotClaimARemoteDownload() {
        Store store = new Store();
        store.importer.ingest(event("memory", "Un dato local", 1000), 1000, false);
        assertFalse(store.rows.get(0).etiquetas.contains("pcloud"));
        assertTrue(store.rows.get(0).etiquetas.contains("diario_local"));
    }

    @Test public void partialRestoreDoesNotClaimThatLocalRowIsFirstEverMemory() {
        Store store = new Store();
        store.importer.ingest(event("user_message", "Registro actual", 5000), 5000);
        ConversationMemoryGrounding.Result result = new ConversationMemoryGrounding(store.memories, null, null)
                .retrieve("primer recuerdo conmigo").withCloudStatus("pCloud pendiente", false);
        assertEquals(ConversationMemoryGrounding.Status.PARTIAL, result.getStatus());
        assertTrue(result.getDirectAnswer().contains("disponibles ahora"));
        assertTrue(result.getDirectAnswer().contains("pCloud pendiente"));
    }

    @Test public void malformedJournalEntryCannotDeleteAnExistingLegacyRecord() {
        Store store = new Store();
        store.importer.ingest(event("memoria_manual", "Recuerdo intacto", 1001), 1001);
        String bad = "{\"type\":\"memory\",\"content\":\"Recuerdo intacto\",\"time_ms\":1000,\"tags\":{}}";
        try { store.importer.ingest(bad, 1000); fail("Invalid tags accepted"); }
        catch (IllegalArgumentException expected) { }
        assertEquals(1, store.rows.size());
        assertEquals("Recuerdo intacto", store.rows.get(0).frase);
        try { store.importer.ingest("[]", 1000); fail("Invalid root accepted"); }
        catch (IllegalArgumentException expected) { }
        try { store.importer.ingest("{\"type\":\"memory\",\"time_ms\":{}}", 1000); fail("Invalid date accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDateDoesNotBecomeTheEarliestMemory() {
        new Store().importer.ingest(event("memory", "Sin fecha", -1), 2000);
    }

    private static String event(String type, String text, long time) {
        JsonObject event = new JsonObject(); event.addProperty("type", type);
        event.addProperty("content", text); event.addProperty("time_ms", time);
        return event.toString();
    }

    private static final class Store {
        final List<RecuerdoEntity> rows = new ArrayList<>();
        final Map<String, MemorySyncStateEntity> revisions = new HashMap<>();
        int nextId = 1;
        RecuerdoEntity profile(String tag) {
            return rows.stream().filter(r -> r.etiquetas.contains("\"" + tag + "\""))
                    .max(Comparator.comparingLong(r -> r.timestamp)).orElse(null);
        }
        final RecuerdoDao memories = (RecuerdoDao) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{RecuerdoDao.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "insertRecuerdo": {
                            RecuerdoEntity row = (RecuerdoEntity) args[0]; row.id = nextId++; rows.add(row); return null;
                        }
                        case "countExact": return (int) rows.stream().filter(r -> r.timestamp == (Long) args[0] && r.frase.equals(args[1])).count();
                        case "canonicalNear": return (int) rows.stream().filter(r -> Math.abs(r.timestamp - (Long) args[0]) <= 5000 && r.frase.equals(args[1])
                                && !r.etiquetas.contains("memoria_manual") && !r.etiquetas.contains("memoria_auto")).count();
                        case "deleteLegacyCopies": {
                            int before = rows.size(); rows.removeIf(r -> Math.abs(r.timestamp - (Long) args[0]) <= 5000 && r.frase.equals(args[1])
                                    && (r.etiquetas.contains("memoria_manual") || r.etiquetas.contains("memoria_auto")));
                            return before - rows.size();
                        }
                        case "ultimoPorEtiqueta": return profile((String) args[0]);
                        case "eliminarPorEtiqueta": {
                            int before = rows.size(); rows.removeIf(r -> r.etiquetas.contains("\"" + args[0] + "\"")); return before - rows.size();
                        }
                        case "reemplazarPorEtiqueta": {
                            rows.removeIf(r -> r.etiquetas.contains("\"" + args[0] + "\""));
                            RecuerdoEntity row = (RecuerdoEntity) args[1]; row.id = nextId++; rows.add(row); return null;
                        }
                        case "primerRecuerdo": case "primerRecuerdoCompartido":
                            return rows.stream().min(Comparator.comparingLong(r -> r.timestamp)).orElse(null);
                        default: throw new AssertionError(method.getName());
                    }
                });
        final MemorySyncStateDao states = (MemorySyncStateDao) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{MemorySyncStateDao.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("get")) return revisions.get(args[0]);
                    if (method.getName().equals("put")) { MemorySyncStateEntity state = (MemorySyncStateEntity) args[0]; revisions.put(state.key, state); return null; }
                    throw new AssertionError(method.getName());
                });
        final CloudMemoryImporter importer = new CloudMemoryImporter(memories, states);
    }
}
