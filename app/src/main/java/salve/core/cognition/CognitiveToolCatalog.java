package salve.core.cognition;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import salve.core.autonomy.AutonomousToolCommand;
import salve.core.autonomy.ToolRegistry;

/**
 * Declarative, controller-owned catalog of conversation tools.
 *
 * Neither the model nor an untrusted tool response can add capabilities.
 * This class creates an allowlist and verifies argument keys before handing
 * the selected call to the already-existing Android executor.
 */
public final class CognitiveToolCatalog {
    public interface Dispatch {
        CognitiveTurn.Receipt execute(String name, JsonObject validatedArguments);
    }

    private CognitiveToolCatalog() { }

    private static void add(Map<String, CognitiveTurn.Tool> map, String name,
            String description, Dispatch dispatch, String... fields) {
        map.put(name, new CognitiveTurn.Tool(description, args -> {
            CognitiveReply.fields(args, fields);
            // Defensive copy: the command may not mutate the model's arguments.
            return dispatch.execute(name, args.deepCopy());
        }));
    }

    /** Capability evaluation happens once per turn from controller-owned facts. */
    public static Map<String, CognitiveTurn.Tool> build(String input,
            boolean photoAnalysis, boolean verifiedCreator, boolean accessibilityReady,
            Dispatch dispatch) {
        return build(input, photoAnalysis, verifiedCreator, accessibilityReady, dispatch,
                ConversationEvidenceIntent.classify(input, null) != ConversationEvidenceIntent.Scope.NONE);
    }

    /** The controller may pass recent conversational context for elliptical follow-up requests. */
    public static Map<String, CognitiveTurn.Tool> build(String input,
            boolean photoAnalysis, boolean verifiedCreator, boolean accessibilityReady,
            Dispatch dispatch, boolean evidenceRelevant) {
        if (dispatch == null) throw new IllegalArgumentException("No hay ejecutor autorizado.");
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        if (photoAnalysis) return tools; // Photo-only requests never perform an action.
        String safeInput = input == null ? "" : input;
        String request = safeInput.toLowerCase(Locale.ROOT);

        // Read-only observability is not a new source of power. The controller must
        // identify relevance in THIS conversation and verify private access again in
        // the executor. A small local LLM selects sleep, execution or both.
        if (verifiedCreator && evidenceRelevant)
            add(tools, "evidence.inspect",
                    "{scope:'sleep'|'execution'|'both'}: consulta informes locales verificables de sueño/aprendizaje o Registro de ejecución; nunca inventes resultados",
                    dispatch, "scope");

        if (verifiedCreator) add(tools, "memory.lookup",
                "{query?:string}: recupera recuerdos; query opcional sobre el mismo tema de la conversación; ninguna consulta privada va a internet",
                dispatch, "query");
        add(tools, "web.search",
                "{query:string}: busca en internet un dato que no sabes (sin datos privados)",
                dispatch, "query");
        add(tools, "sketch.draw",
                "{motivos:string}: dibujas algo (si no lo conoces, buscas referencias), ej. 'arbol+luna'",
                dispatch, "motivos");
        // Private cloud synchronization requires both private-session access and
        // explicit intent in the current user turn, not the model's curiosity.
        if (verifiedCreator && request.matches("(?s).*(sincroniz|p ?cloud|la nube|sube mis recuerdos|restaura mi memoria).*"))
            add(tools, "cloud.sync",
                "{}: sincronizas tú misma con pCloud (traes recuerdos, fotos, dibujos y tu mente; subes lo nuevo)",
                dispatch);
        add(tools, "runtime.status", "{}: mide estado y RAM", dispatch);
        // Sus sentidos físicos: solo lectura y solo cuando el turno pregunta por algo medible.
        if (verifiedCreator && salve.core.sensors.DeviceSenses.offered(safeInput))
            // El grupo es opcional: un modelo pequeño puede omitirlo y el ejecutor lo deduce de la pregunta.
            tools.put(salve.core.sensors.DeviceSenses.TOOL, new CognitiveTurn.Tool(
                    salve.core.sensors.DeviceSenses.description(), args -> {
                        if (args.size() > 1 || args.size() == 1 && !args.has("grupo"))
                            throw new IllegalArgumentException("Campos incorrectos");
                        return dispatch.execute(salve.core.sensors.DeviceSenses.TOOL, args.deepCopy());
                    }));
        if (salve.core.schema.Abstraction.offered(safeInput))
            add(tools, salve.core.schema.Abstraction.TOOL, salve.core.schema.Abstraction.toolDescription(), dispatch, "concepto");
        if (verifiedCreator && salve.core.mind.SelfCode.offered(safeInput))
            add(tools, salve.core.mind.SelfCode.TOOL, salve.core.mind.SelfCode.description(), dispatch, "pregunta");
        // Leer su código real, línea a línea (solo lectura, sin red, la versión instalada).
        if (verifiedCreator && salve.core.mind.SelfCode.offered(safeInput))
            add(tools, salve.core.mind.SelfSource.TOOL, salve.core.mind.SelfSource.description(), dispatch, "archivo", "desde");
        // Guardar un PDF solo cuando Bryan lo pide en este turno; escribe en su propia biblioteca local.
        if (verifiedCreator && salve.core.reports.SelfReport.requested(safeInput))
            add(tools, salve.core.reports.SelfReport.TOOL, salve.core.reports.SelfReport.description(),
                    dispatch, "titulo", "contenido");
        if (verifiedCreator && salve.core.mind.SelfFunctions.offered(safeInput))
            add(tools, salve.core.mind.SelfFunctions.TOOL, salve.core.mind.SelfFunctions.description(), dispatch, "accion");

        String family = AutonomousToolCommand.offeredFamilyFor(safeInput);
        if (!family.isEmpty()) add(tools, "SOLVE_CHALLENGE",
                "{challenge:{schema:1,id:'consulta',family:'" + family + "',input:"
                + ToolRegistry.inputTemplate(family)
                + "}}; enteros explícitos, <=14 elementos, capacidad<=100",
                dispatch, "challenge");

        // A draft is a narrow, model-selected capability. The executor independently
        // checks the original request and resolves the contact in the existing UI;
        // opening a draft is NOT a verified delivery or permission to send it.
        boolean whatsappDraft = verifiedCreator && WhatsAppDraftIntent.offered(safeInput);
        if (whatsappDraft)
            add(tools, "whatsapp.draft",
                    "{contact:string,message:string}: prepara borrador de WhatsApp. contact debe figurar en USER_INPUT; message lo redactas tú de forma natural y fiel al objetivo del usuario, aunque no dicte palabras exactas. Si pide una excusa, propón una explicación prudente sin inventar hechos concretos. No uses datos de terceros no proporcionados. NUNCA envías; el usuario revisa destinatario y texto.",
                    dispatch, "contact", "message");

        // Existing phone adapters are reusable capabilities, not new executors.
        // The LLM selects a tool and its target; the controller checks both
        // against USER_INPUT before touching an external Android app.
        // Salve's own functions: the same recognizers that used to answer before the
        // model now only decide which of her functions the LLM may choose. Details
        // (on/off, which voice, which tab) are re-derived from USER_INPUT later.
        java.util.Set<SelfCommandIntent.Command> selfCommands = verifiedCreator && !whatsappDraft
                ? SelfCommandIntent.available(safeInput) : java.util.Collections.emptySet();
        if (!selfCommands.isEmpty())
            add(tools, SelfCommandIntent.TOOL, SelfCommandIntent.description(selfCommands), dispatch, "id");

        boolean deviceAction = false;
        if (verifiedCreator && !whatsappDraft) {
            if (MediaActionIntent.offered(safeInput)) {
                add(tools, "device.media_control",
                        "{action:'PLAY'|'PAUSE'|'NEXT'|'PREVIOUS'|'NOW_PLAYING'}: controla la sesión multimedia o consulta el título. El controlador verifica que la acción coincida con la petición.",
                        dispatch, "action");
                deviceAction = true;
            }
            // Wifi, Bluetooth, datos, modo avión, no molestar y brillo, mediante Shizuku.
            if (salve.core.devicecontrol.SystemSetting.offered(safeInput)) {
                add(tools, salve.core.devicecontrol.SystemSetting.TOOL,
                        salve.core.devicecontrol.SystemSetting.description(safeInput), dispatch, "accion");
                deviceAction = true;
            }
            // Linterna, volumen y vibración: el mando y su valor salen de la frase del usuario.
            if (DeviceControlIntent.offered(safeInput)) {
                add(tools, DeviceControlIntent.TOOL, DeviceControlIntent.description(safeInput), dispatch, "accion");
                deviceAction = true;
            }
            // «Abre el mapa de tu mente» is her own function, not an installed app.
            // Se mira lo que él pidió (no lo que se ofrece siempre: sus funciones de solo lectura).
            if (!SelfCommandIntent.offered(safeInput)
                    && DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP, safeInput)) {
                add(tools, "device.open_app",
                        "{target:string}: abre app instalada mencionada por el usuario. Nunca supone que el siguiente paso se haya completado.",
                        dispatch, "target");
                deviceAction = true;
            }
            if (DeviceActionIntent.offered(DeviceActionIntent.Kind.NAVIGATE, safeInput)) {
                add(tools, "device.navigate",
                        "{target:string}: solicita ruta en Maps al destino dicho por el usuario; abrir Maps no demuestra llegada.",
                        dispatch, "target");
                deviceAction = true;
            }
            if (DeviceActionIntent.offered(DeviceActionIntent.Kind.PLAY_MUSIC, safeInput)) {
                add(tools, "device.play_music",
                        "{target:string}: solicita reproducción de música mencionada; comprobar sonido queda pendiente.",
                        dispatch, "target");
                deviceAction = true;
            }
            if (DeviceActionIntent.offered(DeviceActionIntent.Kind.SEARCH_MUSIC, safeInput)) {
                add(tools, "device.search_music",
                        "{target:string}: busca música en Spotify sin afirmar que haya empezado a sonar.",
                        dispatch, "target");
                deviceAction = true;
            }
            if (DeviceActionIntent.offered(DeviceActionIntent.Kind.CALL, safeInput)) {
                add(tools, "device.call",
                        "{target:string}: abre revisión de llamada al contacto/número pedido; nunca llama sin confirmación.",
                        dispatch, "target");
                deviceAction = true;
            }
        }

        // A WhatsApp draft must NOT additionally offer arbitrary screen touches or
        // text injection. Those are separate high-risk, user-reviewed actions.
        if (!whatsappDraft && !deviceAction && accessibilityReady
                && request.matches("(?s).*(pantalla|teclado|pulsa|toca |escribe ).*")) {
            add(tools, "TAP", "{x:entero,y:entero}: propone toque; requiere revisión",
                    dispatch, "x", "y");
            add(tools, "ESCRIBIR",
                    "{texto:string}: propone escritura; requiere revisión", dispatch, "texto");
        }
        if (request.matches("(?s).*(publica|despliega).*(web|pagina|página).*"))
            add(tools, "DEPLOY_WEB",
                    "{codigo:string}: propone publicación; requiere confirmar acción",
                    dispatch, "codigo");
        if (request.matches("(?s).*(avatar|vestuario|ropa|vestido|pijama|armario).*")) {
            add(tools, "AVATAR_LIST", "{}: lista diseños locales", dispatch);
            // A design may be selected by name OR by an existing id.
            // AvatarDesignRequest.parse performs the strict alternative-key check.
            tools.put("AVATAR_WEAR", new CognitiveTurn.Tool(
                    "{name:string} o {id:string}: viste diseño existente",
                    args -> dispatch.execute("AVATAR_WEAR", args.deepCopy())));
            // Native AvatarDesignRequest continues to validate all fields, including
            // optional wear and immutable presets, before execution.
            add(tools, "AVATAR_CREATE",
                    "{name,template,color,pattern,wear:boolean}; template:core/kuro/shiro/original_dress/pajamas/explorer; "
                    + "color:ORIGINAL/TURQUOISE/LAVENDER/ROSE/BLUE/AMBER; pattern:NONE/STARS/STRIPES; composición local",
                    dispatch, "name", "template", "color", "pattern", "wear");
        }
        return tools;
    }
}
