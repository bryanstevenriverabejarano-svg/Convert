package salve.core.autonomy;

import com.google.gson.JsonObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ToolRegistryTest {
    @Test public void catalogContainsOnlyExecutableFamiliesAndAllowsNoMutation() {
        for (String family : ToolRegistry.families()) {
            JsonObject metadata = ToolRegistry.describe(family);
            assertEquals(ToolKernels.strategies(family).size(), metadata.getAsJsonArray("strategies").size());
            assertTrue(metadata.get("available").getAsBoolean());
            assertTrue(metadata.getAsJsonArray("externalPermissions").isEmpty());
            metadata.addProperty("available", false);
            assertTrue(ToolRegistry.describe(family).get("available").getAsBoolean());
        }
        assertThrows(UnsupportedOperationException.class, () -> ToolRegistry.families().add("shell"));
        assertThrows(IllegalArgumentException.class, () -> ToolRegistry.describe("shell"));
        assertThrows(IllegalArgumentException.class, () -> ToolRegistry.inputTemplate("cloud_admin"));
    }

    @Test public void latencyIsUnknownUntilMeasuredAndCapabilitiesHaveLimits() {
        JsonObject metadata = ToolRegistry.describe("route");
        assertTrue(metadata.get("estimatedLatencyMs").isJsonNull());
        assertEquals(0, metadata.get("estimatedProviderCost").getAsInt());
        assertFalse(metadata.get("limits").getAsString().isEmpty());
        assertFalse(metadata.get("risks").getAsString().isEmpty());
    }
}
