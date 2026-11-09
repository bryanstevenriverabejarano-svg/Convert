package salve.core.devicecontrol;

import static org.junit.Assert.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import salve.core.ModelResult;
import salve.core.cognition.CognitivePrompt;
import salve.core.cognition.CognitiveToolCatalog;
import salve.core.cognition.CognitiveTurn;

/** Ajustes del sistema por Shizuku: lista cerrada, valor de la frase del usuario, nunca shell libre. */
public class SystemSettingTest {
    private static SystemSetting.Action action(String input) {
        SystemSetting.Command command = SystemSetting.parse(input);
        return command == null ? null : command.action;
    }

    @Test public void recognizesOnlyExplicitSettingOrders() {
        assertEquals(SystemSetting.Action.WIFI_ON, action("Salve, activa el wifi"));
        assertEquals(SystemSetting.Action.WIFI_OFF, action("Apaga el Wi-Fi"));
        assertEquals(SystemSetting.Action.BLUETOOTH_ON, action("enciende el bluetooth por favor"));
        assertEquals(SystemSetting.Action.BLUETOOTH_OFF, action("desconecta el Bluetooth"));
        assertEquals(SystemSetting.Action.MOBILE_DATA_OFF, action("quita los datos móviles"));
        assertEquals(SystemSetting.Action.AIRPLANE_ON, action("pon el modo avión"));
        assertEquals(SystemSetting.Action.DND_ON, action("activa el modo no molestar"));
        assertEquals(SystemSetting.Action.DND_OFF, action("desactiva no molestar"));
        assertEquals(SystemSetting.Action.BRIGHTNESS_AUTO_ON, action("activa el brillo automático"));
        SystemSetting.Command brightness = SystemSetting.parse("Pon el brillo al 40%");
        assertEquals(SystemSetting.Action.BRIGHTNESS_SET, brightness.action);
        assertEquals(40, brightness.value);
        assertEquals(100, SystemSetting.parse("sube el brillo al máximo").value);
        assertNull(SystemSetting.parse("pon el brillo al 300 por ciento"));
        assertNull("una negación no es una orden", SystemSetting.parse("No apagues el wifi"));
        assertNull(SystemSetting.parse("¿Tengo wifi?"));
        assertNull(SystemSetting.parse("¿Por qué el bluetooth gasta batería?"));
        assertNull(SystemSetting.parse("Cuéntame un cuento"));
        assertNull(SystemSetting.parse(null));
        assertEquals("«apaga» manda aunque la frase siga", SystemSetting.Action.WIFI_OFF, action("apaga el wifi y luego enciende el wifi"));
    }

    @Test public void everySettingMapsToFixedCommandsAndNothingElseRuns() {
        for (SystemSetting.Action a : SystemSetting.Action.values()) {
            String[][] commands = SystemSetting.commands(a.ordinal(), 50);
            assertNotNull(a.name(), commands);
            for (String[] argv : commands) {
                assertTrue(a.name(), argv[0].equals("svc") || argv[0].equals("cmd") || argv[0].equals("settings"));
                for (String arg : argv) assertTrue(a.name() + " " + arg, arg.matches("[a-z0-9_-]+"));
            }
            String[] verify = SystemSetting.verification(a.ordinal());
            assertEquals(a.name(), "settings", verify[0]);
            assertEquals("la comprobación solo lee", "get", verify[1]);
        }
        assertArrayEquals(new String[]{"svc", "wifi", "disable"}, SystemSetting.commands(SystemSetting.Action.WIFI_OFF.ordinal(), 0)[0]);
        String[][] brightness = SystemSetting.commands(SystemSetting.Action.BRIGHTNESS_SET.ordinal(), 40);
        assertArrayEquals(new String[]{"settings", "put", "system", "screen_brightness_mode", "0"}, brightness[0]);
        assertArrayEquals(new String[]{"settings", "put", "system", "screen_brightness", "102"}, brightness[1]);
        assertEquals("1", SystemSetting.commands(SystemSetting.Action.BRIGHTNESS_SET.ordinal(), 0)[1][4]);
        assertNull(SystemSetting.commands(SystemSetting.Action.BRIGHTNESS_SET.ordinal(), 101));
        assertNull(SystemSetting.commands(SystemSetting.Action.BRIGHTNESS_SET.ordinal(), -1));
        assertNull("un número de ajuste desconocido no ejecuta nada", SystemSetting.commands(999, 0));
        assertNull(SystemSetting.commands(-1, 0));
        assertNull(SystemSetting.verification(999));
    }

    @Test public void theModelCannotInvertOrSwapASetting() {
        assertEquals(SystemSetting.Action.WIFI_OFF, SystemSetting.verified("Apaga el wifi", "wifi_off").action);
        reject("Apaga el wifi", "WIFI_ON");
        reject("Apaga el wifi", "BLUETOOTH_OFF");
        reject("Cuéntame un cuento", "AIRPLANE_ON");
        reject("Activa el bluetooth", null);
    }

    @Test public void theReceiptSaysOnlyWhatTheSystemReported() {
        assertTrue(SystemSetting.explain(SystemSetting.Action.WIFI_ON, "1\n").startsWith("wifi: activado."));
        assertTrue(SystemSetting.explain(SystemSetting.Action.WIFI_ON, "0").startsWith("wifi: desactivado."));
        assertEquals("no molestar: desactivado.", SystemSetting.explain(SystemSetting.Action.DND_OFF, "0"));
        assertEquals("brillo: nivel 102 de 255 (≈ 40 %), en manual.", SystemSetting.explain(SystemSetting.Action.BRIGHTNESS_SET, "102"));
        assertTrue(SystemSetting.explain(SystemSetting.Action.BLUETOOTH_ON, "").contains("no hay cambio confirmado"));
        assertTrue(SystemSetting.explain(SystemSetting.Action.BLUETOOTH_ON, null).contains("no hay cambio confirmado"));
    }

    @Test public void catalogOffersItOnlyToTheVerifiedCreatorWhoAsked() {
        String request = "Activa el wifi";
        AtomicReference<String> executed = new AtomicReference<>();
        Map<String, CognitiveTurn.Tool> tools = CognitiveToolCatalog.build(request, false, true, true, (name, args) -> {
            executed.set(name + ":" + SystemSetting.verified(request, args.get("accion").getAsString()).action);
            return new CognitiveTurn.Receipt("AJUSTE_REAL: wifi: activado.");
        });
        assertTrue(tools.containsKey(SystemSetting.TOOL));
        assertEquals(SystemSetting.TOOL, CognitivePrompt.prioritize(tools, request).keySet().iterator().next());
        assertEquals(SystemSetting.TOOL, CognitivePrompt.prioritize(
                CognitiveToolCatalog.build("pon el wifi", false, true, true, (n, a) -> CognitiveTurn.Receipt.handoff()),
                "pon el wifi").keySet().iterator().next());
        assertFalse(CognitiveToolCatalog.build(request, false, false, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey(SystemSetting.TOOL));
        assertFalse(CognitiveToolCatalog.build("¿Tengo wifi?", false, true, true,
                (name, args) -> CognitiveTurn.Receipt.handoff()).containsKey(SystemSetting.TOOL));
        String call = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"device.setting\",\"arguments\":{\"accion\":\"WIFI_ON\"}}}";
        String answer = "{\"mode\":\"respond\",\"message\":\"Wifi activado.\",\"action\":null}";
        CognitiveTurn.Outcome result = CognitiveTurn.run(
                (receipts, repair, left) -> ModelResult.success(receipts.size() == 0 ? call : answer, 1), tools, () -> false);
        assertEquals("device.setting:WIFI_ON", executed.get());
        assertEquals("completed", result.receipts().get(0).getAsJsonObject().get("status").getAsString());
    }

    private static void reject(String input, String proposed) {
        try { SystemSetting.verified(input, proposed); fail(); }
        catch (IllegalArgumentException expected) { }
    }
}
