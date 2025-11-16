package salve.core;

import java.util.List;

/**
 * PromptBuilder es un utilitario para construir los mensajes que se enviarán
 * a los modelos de lenguaje. Combina instrucciones de sistema, fragmentos
 * de contexto recuperados de la memoria y la entrada del usuario en un
 * formato coherente.
 *
 * Este archivo sirve como plantilla inicial. Si lo deseas, puedes extender
 * su funcionalidad para añadir herramientas y otros bloques de información.
 */
public class PromptBuilder {
    /**
     * Crea un prompt para el LLM combinando las distintas partes.
     *
     * @param system Instrucciones del sistema que definen la personalidad y reglas.
     * @param context Fragmentos relevantes de la memoria o reflexiones.
     * @param tools Descripción de las herramientas disponibles (en formato libre).
     * @param user Mensaje actual del usuario.
     * @return Un prompt de texto listo para enviar al LLM.
     */
    public static String build(String system,
                               List<String> context,
                               String tools,
                               String user) {
        StringBuilder sb = new StringBuilder();
        if (system != null && !system.isEmpty()) {
            sb.append("[SYSTEM]\n").append(system).append("\n\n");
        }
        if (tools != null && !tools.isEmpty()) {
            sb.append("[HERRAMIENTAS]\n").append(tools).append("\n\n");
        }
        if (context != null && !context.isEmpty()) {
            sb.append("[CONTEXTO]\n");
            for (String line : context) {
                sb.append("- ").append(line).append("\n");
            }
            sb.append("\n");
        }
        if (user != null) {
            sb.append("[USUARIO]\n").append(user);
        }
        return sb.toString();
    }
}
