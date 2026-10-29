package salve.core.cognition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;

/** No text from a model is ever a tool authorization. Routing only filters offered tools. */
public class CapabilityPlannerTest {
    private static Map<String, CognitiveTurn.Tool> all() {
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        for (String key : new String[] {"memory.lookup", "web.search", "runtime.status",
                "sketch.draw", "AVATAR_LIST", "AVATAR_WEAR", "AVATAR_CREATE", "cloud.sync",
                "evidence.inspect", "SOLVE_CHALLENGE", "TAP", "ESCRIBIR"}) {
            tools.put(key, new CognitiveTurn.Tool("{}: " + key, args -> new CognitiveTurn.Receipt("ok")));
        }
        return tools;
    }
    private static String first(Map<String, CognitiveTurn.Tool> tools) {
        return tools.keySet().iterator().next();
    }

    @Test public void explicitWardrobeActionBeatsGenericMemoryOrWeb() {
        Map<String, CognitiveTurn.Tool> tools = all();
        assertEquals("AVATAR_CREATE", first(CapabilityPlanner.rank(tools, "Diseña un vestido nuevo")));
        assertEquals("AVATAR_WEAR", first(CapabilityPlanner.rank(tools, "Ponte el pijama guardado")));
        assertEquals("AVATAR_LIST", first(CapabilityPlanner.rank(tools, "Muéstrame el armario")));
        assertEquals("sketch.draw", first(CapabilityPlanner.rank(tools, "Dibuja un vestido")));
    }

    @Test public void combinedTasksCanExposeTwoSourcesWithoutChangingPermissions() {
        Map<String, CognitiveTurn.Tool> tools = all();
        tools.remove("evidence.inspect");
        Map<String, CognitiveTurn.Tool> ranked = CapabilityPlanner.rank(tools,
                "Busca en internet lo que hablamos antes y comprueba la fuente");
        Iterator<String> names = ranked.keySet().iterator();
        assertEquals("web.search", names.next());
        assertEquals("memory.lookup", names.next());
        assertTrue(CognitivePrompt.limitTools(ranked).containsKey("web.search"));
        assertTrue(CognitivePrompt.limitTools(ranked).containsKey("memory.lookup"));
    }

    @Test public void privateEvidenceIsAlwaysFirstOnlyIfOffered() {
        Map<String, CognitiveTurn.Tool> verified = all();
        assertEquals("evidence.inspect",
                first(CapabilityPlanner.rank(verified, "Qué aprendiste mientras dormías?")));
        verified.remove("evidence.inspect");
        assertFalse(CapabilityPlanner.rank(verified, "Qué aprendiste mientras dormías?")
                .containsKey("evidence.inspect"));
    }

    @Test public void offeredEvidenceCannotHijackUnrelatedModelActions() {
        Map<String, CognitiveTurn.Tool> tools = all();
        assertEquals("AVATAR_CREATE", first(CapabilityPlanner.rank(tools, "Diseña un vestido nuevo")));
        assertEquals("runtime.status", first(CapabilityPlanner.rank(tools, "Cómo está tu RAM?")));
        assertEquals("web.search", first(CapabilityPlanner.rank(tools, "Busca noticias de IA")));
        assertEquals("sketch.draw", first(CapabilityPlanner.rank(tools, "Dibuja la luna")));
        assertEquals("evidence.inspect", first(CapabilityPlanner.rank(tools, "¿Y el registro?")));
        assertEquals("evidence.inspect", first(CapabilityPlanner.rank(tools, "Muéstrame ambas")));
    }

    @Test public void greetingsAreUnchangedAndMissingCapabilitiesNeverInvented() {
        Map<String, CognitiveTurn.Tool> tools = all();
        assertEquals(tools.keySet(), CapabilityPlanner.rank(tools, "hola").keySet());
        tools.remove("AVATAR_CREATE");
        Map<String, CognitiveTurn.Tool> ranked = CapabilityPlanner.rank(tools, "Diseña un vestido");
        assertFalse(ranked.containsKey("AVATAR_CREATE"));
        assertEquals(tools.size(), ranked.size());
    }

    @Test public void adaptiveCatalogShowsMoreExistingCapabilitiesWithoutGrantingAny() {
        Map<String, CognitiveTurn.Tool> offered = new LinkedHashMap<>();
        for (int i = 0; i < 12; i++) {
            String key = "tool_" + i;
            offered.put(key, new CognitiveTurn.Tool("{}: " + key + " " + "x".repeat(84),
                    args -> new CognitiveTurn.Receipt("ok")));
        }
        Map<String, CognitiveTurn.Tool> compact = CognitivePrompt.limitTools(offered);
        Map<String, CognitiveTurn.Tool> small = CognitivePrompt.limitTools(offered, 3000);
        Map<String, CognitiveTurn.Tool> large = CognitivePrompt.limitTools(offered, 9500);
        assertEquals(compact.keySet(), small.keySet());
        assertTrue(large.size() > compact.size());
        assertTrue(offered.keySet().containsAll(large.keySet()));
        assertFalse(large.containsKey("not_authorized"));
    }

    @Test public void statusSyncAndPhysicalActionRemainExplicit() {
        Map<String, CognitiveTurn.Tool> tools = all();
        tools.remove("evidence.inspect");
        assertEquals("runtime.status", first(CapabilityPlanner.rank(tools, "Cómo está tu RAM?")));
        assertEquals("cloud.sync", first(CapabilityPlanner.rank(tools, "Sincroniza con pCloud")));
        assertEquals("TAP", first(CapabilityPlanner.rank(tools, "Toca el botón")));
        assertEquals("ESCRIBIR", first(CapabilityPlanner.rank(tools, "Escribe hola en WhatsApp")));
    }
}
