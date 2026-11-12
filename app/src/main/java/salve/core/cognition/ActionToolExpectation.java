package salve.core.cognition;

import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import salve.core.devicecontrol.SystemSetting;
import salve.core.mind.SelfCode;
import salve.core.reports.SelfReport;

/**
 * A verified command must not silently become mere chat because a small LLM
 * selected respond instead of the existing, authorized tool.
 *
 * This class NEVER executes, authorizes, or invents a tool. It can only require
 * one that the controller already offered for this exact user turn. Requests with
 * no single well-supported action stay conversational.
 */
public final class ActionToolExpectation {
    private static final Pattern COMMAND = Pattern.compile(
            "^(?:salve )?(?:por favor )?(?:abre|abreme|abrir|activa|desactiva|enciende|apaga|"
            + "cambia|pon|ponme|muestra|muestrame|ensen[a-z]*|lee|leeme|"
            + "busca|buscame|investiga|investigame|dibuja|dibujame|pinta|"
            + "guarda|crea|haz|prepara|escribe|mandame|envia|"
            + "comprueba|revisa|reproduce|sincroniza|sube|baja|vibra|"
            + "dime|consulta|lleva|llevame|navega|viste|vistete)\\b.*");
    private static final Pattern NO_ACTION = Pattern.compile(
            "^(?:no|nunca|jamas|sin|imagina|supongamos|si |"
            + "que|como|por que|por cual|cuando|cual|"
            + "explica|hablame sobre)\\b.*");

    private ActionToolExpectation() { }

    public static String requiredTool(String input, Map<String, CognitiveTurn.Tool> offered) {
        if (input == null || offered == null || offered.isEmpty()) return null;
        String s = normalize(input);
        if (s.isEmpty() || s.length() > 300 || NO_ACTION.matcher(s).matches()) return null;

        // The recognizers below are already the allowlist shared by text and voice.
        if (offered.containsKey("whatsapp.draft") && WhatsAppDraftIntent.offered(input))
            return "whatsapp.draft";
        // Solo las frases exactas EXIGEN una herramienta. Lo entendido por significado se ofrece y lo
        // decide su LLM: si prefiere conversar, no se le fuerza ni se ejecuta por detrás.
        Set<SelfCommandIntent.Command> self = SelfCommandIntent.detected(input);
        if (offered.containsKey(SelfCommandIntent.TOOL) && self.size() == 1)
            return SelfCommandIntent.TOOL;
        if (offered.containsKey(SystemSetting.TOOL) && SystemSetting.offeredExact(input))
            return SystemSetting.TOOL;
        if (offered.containsKey(DeviceControlIntent.TOOL) && DeviceControlIntent.parseExact(input) != null)
            return DeviceControlIntent.TOOL;
        if (offered.containsKey("device.media_control") && MediaActionIntent.offeredExact(input))
            return "device.media_control";
        if (offered.containsKey("device.call")
                && DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.CALL, input))
            return "device.call";
        if (offered.containsKey("device.navigate")
                && DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.NAVIGATE, input))
            return "device.navigate";
        if (offered.containsKey("device.open_app")
                && DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.OPEN_APP, input))
            return "device.open_app";
        if (offered.containsKey("device.play_music")
                && DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.PLAY_MUSIC, input))
            return "device.play_music";
        if (offered.containsKey("device.search_music")
                && DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.SEARCH_MUSIC, input))
            return "device.search_music";

        // Additional tools retain their original per-operation verification.
        // This condition only prevents a false "done" response.
        if (!COMMAND.matcher(s).matches()) return null;
        if (offered.containsKey("cloud.sync") && s.matches(".*\\b(?:sincroniza|restaura|recupera|sube)\\b.*\\b(?:pcloud|nube|memoria|recuerdos)\\b.*"))
            return "cloud.sync";
        if (offered.containsKey(SelfReport.TOOL) && SelfReport.requested(input))
            return SelfReport.TOOL;
        if (offered.containsKey(SelfCode.TOOL) && SelfCode.offered(input)
                && s.matches(".*\\b(?:revisa|analiza|lee|investiga|comprueba)\\b.*"))
            return SelfCode.TOOL;
        if (offered.containsKey("evidence.inspect")
                && s.matches(".*\\b(?:registro de ejecucion|informe nocturno|informes de sueno)\\b.*"))
            return "evidence.inspect";
        if (offered.containsKey("memory.lookup")
                && s.matches(".*\\b(?:busca|consulta|recupera)\\b.*\\b(?:recuerdos|memoria)\\b.*"))
            return "memory.lookup";
        if (offered.containsKey("sketch.draw")
                && s.matches(".*\\b(?:dibuja|dibujame|pinta|pintame|ilustra|ilustrame)\\b.*"))
            return "sketch.draw";
        if (offered.containsKey("web.search")
                && s.matches(".*\\b(?:busca|buscame|investiga|investigame|consulta)\\b.*\\b(?:internet|web|google)\\b.*"))
            return "web.search";
        if (offered.containsKey("AVATAR_WEAR")
                && s.matches(".*\\b(?:ponte|vistete|equipate|cambia)\\b.*\\b(?:traje|ropa|vestido|pijama|vestuario)\\b.*"))
            return "AVATAR_WEAR";
        if (offered.containsKey("AVATAR_CREATE")
                && s.matches(".*\\b(?:crea|disena|fabrica|inventa)\\b.*\\b(?:traje|vestido|pijama|ropa|vestuario)\\b.*"))
            return "AVATAR_CREATE";
        if (offered.containsKey("AVATAR_LIST")
                && s.matches(".*\\b(?:muestra|muestrame|ensename|lista)\\b.*\\b(?:trajes|vestidos|ropa|vestuario)\\b.*"))
            return "AVATAR_LIST";
        if (offered.containsKey("TAP") && s.matches(".*\\b(?:toca|pulsa|haz clic)\\b.*"))
            return "TAP";
        if (offered.containsKey("ESCRIBIR") && s.matches(".*\\b(?:escribe|teclea|rellena)\\b.*\\b(?:pantalla|teclado|campo)\\b.*"))
            return "ESCRIBIR";
        if (offered.containsKey("DEPLOY_WEB")
                && s.matches(".*\\b(?:publica|despliega)\\b.*\\b(?:web|pagina)\\b.*"))
            return "DEPLOY_WEB";
        return null;
    }

    /**
     * A last-resort controller fallback ONLY for exact, reversible/read-only
     * commands with arguments derived from the literal user turn. The existing
     * executor still rechecks owner identity and every argument. Other tools
     * (messages, calls, shell settings, browsing, remote updates and arbitrary
     * app targets) must still be selected by the LLM or explicitly reviewed.
     */
    public static JsonObject directFallbackArguments(String input, String expectedTool) {
        if (input == null || expectedTool == null) return null;
        JsonObject args = new JsonObject();
        if (DeviceControlIntent.TOOL.equals(expectedTool)) {
            DeviceControlIntent.Command action = DeviceControlIntent.parseExact(input);
            if (action == null) return null;
            args.addProperty("accion", action.action.name());
            return args;
        }
        if ("device.media_control".equals(expectedTool)) {
            salve.core.voice.MusicControlCommand.Action action =
                    salve.core.voice.MusicControlCommand.parse(input);
            if (action == null || !MediaActionIntent.offeredExact(input)) return null;
            args.addProperty("action", action.name());
            return args;
        }
        if (SelfCommandIntent.TOOL.equals(expectedTool)) {
            Set<SelfCommandIntent.Command> detected = SelfCommandIntent.detected(input);
            if (detected.size() != 1) return null;
            SelfCommandIntent.Command action = detected.iterator().next();
            switch (action) {
                case MEMORY_HEALTH:
                case SYNERGY_STATUS:
                case MODEL_STATUS:
                case DOWNLOAD_STATUS:
                case SELF_PR_STATUS:
                case IMPROVEMENT_PROPOSALS:
                case MIND_MAP:
                case OPEN_JOURNAL:
                case BODY_ACTION:
                case CHESS_START:
                    args.addProperty("id", action.name());
                    return args;
                default: return null; // No auto-retry of sensitive or stateful modes.
            }
        }
        return null;
    }

    private static String normalize(String input) {
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
