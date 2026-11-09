package salve.core.cognition;

import static org.junit.Assert.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import salve.core.ModelResult;
import salve.core.sensors.DeviceSenses;

/** Linterna, volumen y vibración: el modelo elige usar el mando; la frase del usuario fija cuál y cuánto. */
public class DeviceControlIntentTest {
    private static DeviceControlIntent.Action action(String input) {
        DeviceControlIntent.Command command = DeviceControlIntent.parse(input);
        return command == null ? null : command.action;
    }

    @Test public void recognizesOnlyExplicitPhysicalControls() {
        assertEquals(DeviceControlIntent.Action.TORCH_ON, action("Salve, enciende la linterna"));
        assertEquals(DeviceControlIntent.Action.TORCH_OFF, action("Apaga la linterna, por favor"));
        assertEquals(DeviceControlIntent.Action.VOLUME_UP, action("Sube el volumen"));
        assertEquals(DeviceControlIntent.Action.VOLUME_DOWN, action("baja un poco el volumen"));
        assertEquals(DeviceControlIntent.Action.VOLUME_MUTE, action("Silencia el móvil"));
        assertEquals(DeviceControlIntent.Action.VIBRATE, action("Vibra"));
        DeviceControlIntent.Command set = DeviceControlIntent.parse("Pon el volumen al 40%");
        assertEquals(DeviceControlIntent.Action.VOLUME_SET, set.action);
        assertEquals(40, set.percent);
        assertNull(DeviceControlIntent.parse("Pon el volumen al 250 por ciento"));
        assertNull(DeviceControlIntent.parse("No enciendas la linterna"));
        assertNull(DeviceControlIntent.parse("¿Por qué vibra tanto el móvil cuando llaman?"));
        assertNull(DeviceControlIntent.parse("Cuéntame un cuento sobre una linterna mágica"));
        assertNull(DeviceControlIntent.parse(null));
    }

    @Test public void theModelCannotInvertOrInventAControl() {
        assertEquals(DeviceControlIntent.Action.TORCH_OFF,
                DeviceControlIntent.verified("Apaga la linterna", "torch_off").action);
        reject("Apaga la linterna", "TORCH_ON");
        reject("Baja el volumen", "VOLUME_UP");
        reject("Cuéntame un cuento", "VIBRATE");
        reject("Sube el volumen", null);
    }

    @Test public void volumeTargetsStayInsideTheRealRange() {
        DeviceControlIntent.Command up = DeviceControlIntent.parse("sube el volumen");
        DeviceControlIntent.Command down = DeviceControlIntent.parse("baja el volumen");
        assertEquals(7, DeviceControlIntent.targetVolume(up, 5, 15));
        assertEquals(15, DeviceControlIntent.targetVolume(up, 14, 15));
        assertEquals(0, DeviceControlIntent.targetVolume(down, 1, 15));
        assertEquals(6, DeviceControlIntent.targetVolume(DeviceControlIntent.parse("pon el volumen al 40 por ciento"), 12, 15));
        assertEquals(0, DeviceControlIntent.targetVolume(DeviceControlIntent.parse("quita el sonido"), 9, 15));
        assertEquals(0, DeviceControlIntent.targetVolume(up, 3, 0));
    }

    @Test public void catalogOffersControlsAndSensesOnlyToTheVerifiedCreatorWhoAsked() {
        String request = "Enciende la linterna";
        AtomicReference<String> executed = new AtomicReference<>();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(request, false, true, true, (name, args) -> {
            executed.set(name + ":" + DeviceControlIntent.verified(request, args.get("accion").getAsString()).action);
            return new CognitiveTurn.Receipt("LINTERNA: Android aceptó encenderla.");
        });
        assertTrue(tools.containsKey(DeviceControlIntent.TOOL));
        assertEquals(DeviceControlIntent.TOOL, CognitivePrompt.prioritize(tools, request).keySet().iterator().next());
        assertFalse("sin identidad verificada no hay mandos", CognitiveToolCatalog.build(request, false, false, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey(DeviceControlIntent.TOOL));
        assertFalse("ni en un turno que no los pide", CognitiveToolCatalog.build("Cuéntame un cuento", false, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey(DeviceControlIntent.TOOL));

        String call = "{\"mode\":\"tool\",\"message\":null,\"action\":"
                + "{\"name\":\"device.control\",\"arguments\":{\"accion\":\"TORCH_ON\"}}}";
        String answer = "{\"mode\":\"respond\",\"message\":\"Linterna encendida.\",\"action\":null}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(receipts.size() == 0 ? call : answer, 1), tools, () -> false);
        assertEquals("device.control:TORCH_ON", executed.get());
        assertFalse(result.handedOff);
        assertEquals("completed", result.receipts().get(0).getAsJsonObject().get("status").getAsString());

        String question = "¿Cuánta batería te queda?";
        Map<String, CognitiveTurn.Tool> senses = CognitiveToolCatalog.build(question, false, true, true,
                (name, args) -> new CognitiveTurn.Receipt("ENERGIA: Batería: 82 %."));
        assertTrue(senses.containsKey(DeviceSenses.TOOL));
        assertTrue(CognitivePrompt.limitTools(CognitivePrompt.prioritize(senses, question), 3000).containsKey(DeviceSenses.TOOL));
        assertFalse(CognitiveToolCatalog.build(question, false, false, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey(DeviceSenses.TOOL));
        // El grupo es opcional; cualquier otro argumento se rechaza antes de medir.
        assertEquals("completed", senseStatus(senses, "{}"));
        assertEquals("completed", senseStatus(senses, "{\"grupo\":\"ENERGIA\"}"));
        assertEquals("failed", senseStatus(senses, "{\"orden\":\"borra todo\"}"));
    }

    private static String senseStatus(Map<String, CognitiveTurn.Tool> tools, String arguments) {
        String call = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"device.sense\",\"arguments\":" + arguments + "}}";
        String answer = "{\"mode\":\"respond\",\"message\":\"Hecho.\",\"action\":null}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(receipts.size() == 0 ? call : answer, 1), tools, () -> false);
        return result.receipts().get(0).getAsJsonObject().get("status").getAsString();
    }

    private static void reject(String input, String proposed) {
        try { DeviceControlIntent.verified(input, proposed); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}
