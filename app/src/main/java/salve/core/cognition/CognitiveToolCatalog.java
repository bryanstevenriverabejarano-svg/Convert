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
        if (salve.core.schema.Abstraction.offered(safeInput))
            add(tools, salve.core.schema.Abstraction.TOOL, salve.core.schema.Abstraction.toolDescription(), dispatch, "concepto");
        if (verifiedCreator && salve.core.mind.SelfCode.offered(safeInput))
            add(tools, salve.core.mind.SelfCode.TOOL, salve.core.mind.SelfCode.description(), dispatch, "pregunta");
        if (verifiedCreator && salve.core.mind.SelfFunctions.offered(safeInput))
            add(tools, salve.core.mind.SelfFunctions.TOOL, salve.core.mind.SelfFunctions.description(), dispatch, "accion");

        String family = AutonomousToolCommand.offeredFamilyFor(safeInput);
        if (!family.isEmpty()) add(tools, "SOLVE_CHALLENGE",
                "{challenge:{schema:1,id:'consulta',family:'" + family + "',input:"
                + ToolRegistry.inputTemplate(family)
                + "}}; enteros explícitos, <=14 elementos, capacidad<=100",
                dispatch, "challenge");

        // Both explicit user intent and a connected accessibility service are
        // necessary to *propose* these calls; the Android executor requires
        // independent per-action approval before dispatching any side effect.
        if (accessibilityReady && request.matches("(?s).*(pantalla|teclado|pulsa|toca |escribe |whatsapp).*")) {
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
