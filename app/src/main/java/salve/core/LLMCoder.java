package salve.core;

import android.content.Context;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import salve.core.conversation.ConversationModelRouter;

/** Genera propuestas con el proveedor elegido por el usuario; nunca ejecuta código. */
public class LLMCoder {
    private static LLMCoder instance;
    private final BooleanSupplier localOnly;
    private final Function<String, ModelResult> cloud;
    private final Function<String, ModelResult> local;

    private LLMCoder(Context ctx) {
        Context context = ctx.getApplicationContext();
        this.localOnly = () -> SalveLLM.getInstance(context).isLocalOnly();
        this.cloud = prompt -> GeminiService.getInstance(context).generateResultSync(prompt, null);
        this.local = prompt -> SalveLLM.getInstance(context)
                .generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
    }

    /** Inyección de proveedores para comprobar rutas y fallos sin una red ni un modelo. */
    LLMCoder(BooleanSupplier localOnly, Function<String, ModelResult> cloud,
             Function<String, ModelResult> local) {
        this.localOnly = localOnly;
        this.cloud = cloud;
        this.local = local;
    }

    public static synchronized LLMCoder getInstance(Context ctx) {
        if (instance == null) instance = new LLMCoder(ctx);
        return instance;
    }

    public String generateCode(String description, String language) {
        String prompt = "Eres un experto programador. Genera un fragmento de código en "
                + language + " para la siguiente tarea:\n" + description + "\n"
                + "Responde ÚNICAMENTE con el código, sin explicaciones.";
        return generate(prompt, "// No se pudo generar el código. Verifica el proveedor seleccionado.");
    }

    /**
     * Propone un diff. Sin el código fuente exacto, no se considera aplicable ni
     * validado hasta que el ejecutor externo lo compruebe contra una revisión.
     */
    public String generateFix(String issueDescription, String className) {
        String sourcePath = "app/src/main/java/salve/core/" + className + ".java";
        String prompt = "Como asistente de mejora continua, corrige el siguiente problema en la clase "
                + className + ":\n" + issueDescription + "\n"
                + "La ruta exacta del archivo es " + sourcePath + ".\n"
                + "Devuelve exclusivamente un unified diff aplicable con git apply. "
                + "El diff solo puede modificar esa ruta, debe incluir contexto suficiente y "
                + "no debe contener bloques Markdown ni explicaciones. "
                + "Si no dispones del código necesario para un diff fiel, responde "
                + "// No se pudo generar la corrección: falta el código fuente exacto.";
        return generate(prompt, "// No se pudo generar la corrección.");
    }

    private String generate(String prompt, String failure) {
        final boolean useLocalOnly;
        try {
            useLocalOnly = localOnly.getAsBoolean();
        } catch (RuntimeException unavailablePolicy) {
            // No enviar datos si no se ha podido determinar el modo elegido.
            return failure;
        }
        ModelResult result = ConversationModelRouter.generate(false, useLocalOnly, false,
                cloud == null ? null : () -> cloud.apply(prompt),
                local == null ? null : () -> local.apply(prompt));
        if (!result.isSuccess()) return failure;
        String code = cleanCode(result.getText());
        return code.isEmpty() ? failure : code;
    }

    private String cleanCode(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?s)```[a-zA-Z]*\\n?(.*?)\\n?```", "$1").trim();
    }
}
