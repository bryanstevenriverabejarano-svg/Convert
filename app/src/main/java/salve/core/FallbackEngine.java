package salve.core;

import android.text.TextUtils;

/**
 * Generador de respuestas sintéticas cuando el motor local aún no está disponible.
 * Mantiene separado el texto de depuración que usamos en {@link LocalLlmEngine}
 * para que el resto del código pueda seguir evolucionando sin depender del
 * backend nativo todavía.
 */
final class FallbackEngine {

    private FallbackEngine() {
        // Utility class
    }

    static String buildPreview(String modelId, String prompt) {
        final String safeModel = TextUtils.isEmpty(modelId) ? "<sin_modelo>" : modelId;
        final String userBlock = extractUserSegment(prompt);
        return "/* LocalLlmEngine aún no está conectado a un modelo GGUF real. */\n" +
                "Modelo solicitado: " + safeModel + "\n\n" +
                "Último mensaje del usuario:\n" + userBlock + "\n" +
                "(Esta es una respuesta simulada para verificar el flujo de extremo a extremo.)";
    }

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
}
