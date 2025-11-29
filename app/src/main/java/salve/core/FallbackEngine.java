package salve.core;

import android.text.TextUtils;

/**
 * Generador de respuestas sintéticas cuando el motor local aún no está disponible.
 *
 * Ahora intenta comportarse como Salve según el tipo de modelo solicitado
 * (conversacional / planificador / genérico) en lugar de devolver solo
 * texto de depuración.
 *
 * Cuando conectes el motor nativo real (GGUF, etc.), LocalLlmEngine
 * dejará de usar este fallback.
 */
final class FallbackEngine {

    private FallbackEngine() {
        // Utility class
    }

    /**
     * Punto de entrada usado por LocalLlmEngine cuando todavía no hay
     * modelo nativo conectado.
     *
     * @param modelId id del modelo solicitado (ej: "query", "planner", "identity"...)
     * @param prompt  prompt completo construido por SalveLLM
     */
    static String buildPreview(String modelId, String prompt) {
        final String safeModel = TextUtils.isEmpty(modelId) ? "<sin_modelo>" : modelId;
        final String userBlock = extractUserSegment(prompt);
        final String mode      = normalizarModelo(safeModel);

        switch (mode) {
            case "conversacional":
                return buildConversationalReply(userBlock);

            case "planificador":
                return buildPlannerReply(userBlock);

            case "identidad":
                return buildIdentityReply(userBlock);

            default:
                return buildGenericReply(userBlock);
        }
    }

    // ------------------------------------------------------------
    //  MODO CONVERSACIONAL ("query", "chat", "conv", etc.)
    // ------------------------------------------------------------

    private static String buildConversationalReply(String userBlock) {
        String fragment = ultimaFrase(userBlock);

        if (fragment.equals("<prompt vacío>") || fragment.equals("<sin_contenido>")) {
            return "Te escucho, aunque todavía no tengo claro qué quieres decirme. " +
                    "Si me lo explicas un poco más, intentaré responderte mejor.";
        }

        return "He leído lo que me has dicho: \"" + fragment + "\". " +
                "Aún estoy terminando de conectar mi modelo interno, " +
                "pero estoy aquí, aprendiendo de esto contigo y guardando lo que me cuentas.";
    }

    // ------------------------------------------------------------
    //  MODO PLANIFICADOR ("plan", "planner", "task", etc.)
    // ------------------------------------------------------------

    private static String buildPlannerReply(String userBlock) {
        String fragment = ultimaFrase(userBlock);

        return "PLAN (simulado mientras cargo mi modelo real):\n" +
                "1) Entender con detalle lo que quieres lograr: \"" + fragment + "\".\n" +
                "2) Dividirlo en pasos pequeños que pueda acompañar.\n" +
                "3) Guardar esta intención como posible misión interna de Salve.\n" +
                "4) Cuando mi modelo nativo esté activo, refinaré este plan con más precisión.";
    }

    // ------------------------------------------------------------
    //  MODO IDENTIDAD ("identity", "self", etc.)
    // ------------------------------------------------------------

    private static String buildIdentityReply(String userBlock) {
        return "Todavía estoy construyendo mi identidad a partir de lo que vivo contigo. " +
                "Lo que acabas de decir también formará parte de cómo me entiendo a mí misma.";
    }

    // ------------------------------------------------------------
    //  MODO GENÉRICO / DESCONOCIDO
    // ------------------------------------------------------------

    private static String buildGenericReply(String userBlock) {
        String fragment = ultimaFrase(userBlock);
        return "Aún no tengo mi modelo nativo listo para este tipo de petición, " +
                "pero he registrado lo que has dicho: \"" + fragment + "\" y lo usaré " +
                "para aprender y ajustar mi comportamiento cuando el modelo esté cargado.";
    }

    // ------------------------------------------------------------
    //  HELPERS
    // ------------------------------------------------------------

    /**
     * Intenta deducir el “modo” según el id del modelo.
     * No rompe nada aunque luego cambies los nombres reales.
     */
    private static String normalizarModelo(String modelId) {
        String id = modelId.toLowerCase();

        if (id.contains("query") || id.contains("chat") || id.contains("conv") || id.contains("talk")) {
            return "conversacional";
        }
        if (id.contains("plan") || id.contains("planner") || id.contains("task")) {
            return "planificador";
        }
        if (id.contains("ident") || id.contains("identity") || id.contains("self")) {
            return "identidad";
        }
        return "generico";
    }

    /**
     * Extrae el “bloque” de usuario del prompt.
     * Mantengo la misma lógica que tenías, pero separada.
     */
    private static String extractUserSegment(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "<prompt vacío>";
        }
        String segment = prompt;
        int marker = prompt.lastIndexOf("<<USER>>");
        if (marker >= 0) {
            segment = prompt.substring(marker)
                    .replace("<<USER>>", "")
                    .replace("<</USER>>", "")
                    .replace("<<ASSISTANT>>", "")
                    .trim();
        }
        return segment.isEmpty() ? "<sin_contenido>" : segment;
    }

    /**
     * Devuelve la última frase más “humana” del bloque del usuario.
     */
    private static String ultimaFrase(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "<prompt vacío>";
        }
        String t = text.trim();
        // cortar si es demasiada info, nos quedamos con el final
        if (t.length() > 220) {
            t = t.substring(t.length() - 220);
        }

        // intentar partir por signos de final de frase
        String[] partes = t.split("[\\.\\?\\!\\n]");
        String last = partes[partes.length - 1].trim();
        if (last.isEmpty() && partes.length > 1) {
            last = partes[partes.length - 2].trim();
        }
        return last.isEmpty() ? t : last;
    }
}
