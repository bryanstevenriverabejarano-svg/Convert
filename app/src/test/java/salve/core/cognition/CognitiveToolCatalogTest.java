package salve.core.cognition;

import com.google.gson.JsonObject;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** The catalog is a controller-owned allowlist, never a model-selected permission. */
public class CognitiveToolCatalogTest {
    private static final CognitiveToolCatalog.Dispatch NO_OP = (name, args) ->
            new CognitiveTurn.Receipt("ok");

    @Test public void photoTurnCannotExposeExecutableTools() {
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                "Escribe hola por WhatsApp y sincroniza mi nube", true, true, true, NO_OP);
        assertTrue(tools.isEmpty());
    }

    @Test public void nonCreatorDoesNotSeePrivateMemoryOrCloud() {
        Map<String, CognitiveTurn.Tool> guest = CognitiveToolCatalog.build(
                "sincroniza tus recuerdos", false, false, false, NO_OP);
        assertFalse(guest.containsKey("memory.lookup"));
        assertFalse(guest.containsKey("cloud.sync"));
        assertTrue(guest.containsKey("runtime.status"));
        assertTrue(guest.containsKey("web.search"));
        Map<String, CognitiveTurn.Tool> creator = CognitiveToolCatalog.build(
                "sincroniza tus recuerdos", false, true, false, NO_OP);
        assertTrue(creator.containsKey("memory.lookup"));
        assertTrue(creator.containsKey("cloud.sync"));
    }

    @Test public void sensitiveToolsRequireBothOriginalIntentAndServiceAvailability() {
        assertFalse(CognitiveToolCatalog.build("toca la pantalla",
                false, true, false, NO_OP).containsKey("TAP"));
        assertFalse(CognitiveToolCatalog.build("hola",
                false, true, true, NO_OP).containsKey("TAP"));
        Map<String, CognitiveTurn.Tool> allowed = CognitiveToolCatalog.build(
                "toca la pantalla", false, true, true, NO_OP);
        assertTrue(allowed.containsKey("TAP"));
        assertTrue(allowed.containsKey("ESCRIBIR"));
        assertFalse(allowed.containsKey("DEPLOY_WEB"));
        assertTrue(CognitiveToolCatalog.build("publica mi página web",
                false, true, false, NO_OP).containsKey("DEPLOY_WEB"));
    }

    @Test public void invalidArgumentKeysNeverReachAndroidExecutor() {
        AtomicInteger calls = new AtomicInteger();
        CognitiveToolCatalog.Dispatch counted = (name, args) -> {
            calls.incrementAndGet();
            return new CognitiveTurn.Receipt("ejecutado");
        };
        Map<String, CognitiveTurn.Tool> allowed = CognitiveToolCatalog.build(
                "toca la pantalla", false, true, true, counted);
        String bad = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"TAP\",\"arguments\":{\"x\":1,\"y\":2,\"hidden\":\"danger\"}}}";
        CognitiveTurn.run((receipts, repair, left) -> {
            return salve.core.ModelResult.success(bad, 0);
        }, allowed, () -> false);
        assertEquals(0, calls.get());
    }

    @Test public void noToolEscalationThroughActionText() {
        Map<String, CognitiveTurn.Tool> normal = CognitiveToolCatalog.build(
                "Hola", false, true, false, NO_OP);
        assertFalse(normal.containsKey("TAP"));
        assertFalse(normal.containsKey("ESCRIBIR"));
        assertFalse(normal.containsKey("DEPLOY_WEB"));
        assertFalse(normal.containsKey("AVATAR_CREATE"));
        assertFalse(normal.containsKey("SOLVE_CHALLENGE"));
        assertTrue(normal.containsKey("memory.lookup"));
    }

    @Test public void wardrobeStillSupportsSelectionBySavedId() {
        Map<String, CognitiveTurn.Tool> catalog = CognitiveToolCatalog.build(
                "viste tu avatar", false, true, false, NO_OP);
        assertTrue(catalog.containsKey("AVATAR_LIST"));
        assertTrue(catalog.containsKey("AVATAR_WEAR"));
        assertTrue(catalog.containsKey("AVATAR_CREATE"));
        // The dispatch preserves the existing AvatarDesignRequest alternative of id/name.
        // A strict validator in the actual Android executor remains authoritative.
        assertTrue(catalog.get("AVATAR_WEAR").description.contains("id:string"));
    }

    @Test public void catalogNeverExecutesUntilCognitiveTurnAuthorizesCall() {
        AtomicInteger calls = new AtomicInteger();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(
                "investiga", false, true, false, (name, args) -> {
                    calls.incrementAndGet();
                    return new CognitiveTurn.Receipt("ok");
                });
        assertEquals(0, calls.get());
        assertTrue(tools.containsKey("web.search"));
        assertTrue(CognitivePrompt.limitTools(tools).size() <= tools.size());
        assertEquals(0, calls.get());
    }
}
