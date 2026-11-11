package salve.core.cognition;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ActionToolExpectationTest {
    private static Map<String, CognitiveTurn.Tool> tools(String... names) {
        Map<String, CognitiveTurn.Tool> map = new LinkedHashMap<>();
        for (String name : names) map.put(name,
                new CognitiveTurn.Tool("verified tool", args -> new CognitiveTurn.Receipt("OK")));
        return map;
    }

    @Test public void recognizesExplicitExistingToolsForBothModes() {
        assertEquals("device.control", ActionToolExpectation.requiredTool(
                "enciende la linterna", tools("device.control", "web.search")));
        assertEquals("device.setting", ActionToolExpectation.requiredTool(
                "activa el bluetooth", tools("device.setting")));
        assertEquals("self.command", ActionToolExpectation.requiredTool(
                "abre tu biblioteca de informes", tools("self.command")));
        assertEquals("device.open_app", ActionToolExpectation.requiredTool(
                "Salve, ¿puedes abrirme Spotify, por favor?", tools("device.open_app")));
        assertEquals("whatsapp.draft", ActionToolExpectation.requiredTool(
                "escribe un WhatsApp a mamá diciendo que voy tarde", tools("whatsapp.draft")));
        assertEquals("sketch.draw", ActionToolExpectation.requiredTool(
                "dibújame una luna", tools("sketch.draw")));
        assertEquals("web.search", ActionToolExpectation.requiredTool(
                "busca en internet nuevos artículos de robótica", tools("web.search")));
    }

    @Test public void fallbackOnlyUsesExactLowRiskValidatedArgs() {
        assertEquals("TORCH_ON",
                ActionToolExpectation.directFallbackArguments(
                        "enciende la linterna", "device.control").get("accion").getAsString());
        assertEquals("PAUSE",
                ActionToolExpectation.directFallbackArguments(
                        "pausa la música", "device.media_control").get("action").getAsString());
        assertEquals("OPEN_JOURNAL",
                ActionToolExpectation.directFallbackArguments(
                        "abre tu biblioteca de informes", "self.command").get("id").getAsString());
        assertNull(ActionToolExpectation.directFallbackArguments("activa el wifi", "device.setting"));
        assertNull(ActionToolExpectation.directFallbackArguments(
                "dile a mamá por whatsapp que la quiero", "whatsapp.draft"));
        assertNull(ActionToolExpectation.directFallbackArguments(
                "abre Spotify", "device.open_app"));
        assertNull(ActionToolExpectation.directFallbackArguments(
                "abre tu PR", "self.command"));
    }

    @Test public void doesNotExpandPermissionOrActOnNegationsAndOrdinaryChat() {
        assertNull(ActionToolExpectation.requiredTool(
                "enciende la linterna", tools("web.search")));
        assertNull(ActionToolExpectation.requiredTool(
                "no enciendas la linterna", tools("device.control")));
        assertNull(ActionToolExpectation.requiredTool(
                "explícame cómo encender la linterna", tools("device.control")));
        assertNull(ActionToolExpectation.requiredTool(
                "¿qué es Spotify?", tools("device.open_app")));
        assertNull(ActionToolExpectation.requiredTool(
                "¿cómo estás?", tools("web.search", "sketch.draw")));
    }
}
