package salve.core;

import android.content.Context;
import android.util.Log;

/**
 * LLMCoder — Componente encargado de generar código y proponer mejoras.
 * Ahora integrado con GeminiService para una capacidad de programación real.
 */
public class LLMCoder {
    private static final String TAG = "Salve/LLMCoder";
    private static LLMCoder instance;
    private final Context context;

    private LLMCoder(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public static synchronized LLMCoder getInstance(Context ctx) {
        if (instance == null) {
            instance = new LLMCoder(ctx);
        }
        return instance;
    }

    /**
     * Genera código fuente a partir de una descripción.
     * Intenta usar Gemini primero por su superioridad en programación.
     */
    public String generateCode(String description, String language) {
        String prompt = "Eres un experto programador. Genera un fragmento de código en "
                + language + " para la siguiente tarea:\n"
                + description + "\n"
                + "Responde ÚNICAMENTE con el código, sin explicaciones.";

        // 1. Intentar con Gemini (Cerebro Superior)
        GeminiService gemini = GeminiService.getInstance(context);
        if (gemini.isAvailable()) {
            String code = gemini.generateSync(prompt);
            if (code != null && !code.trim().isEmpty()) {
                Log.d(TAG, "Código generado exitosamente con Gemini");
                return cleanCode(code);
            }
        }

        // 2. Fallback al LLM Local
        try {
            SalveLLM llm = SalveLLM.getInstance(context);
            if (llm != null) {
                String result = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
                if (isValidResponse(result)) {
                    return cleanCode(result);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en fallback local de generateCode", e);
        }

        return "// No se pudo generar el código. Verifica la conexión o el modelo local.\n"
                + "// Tarea: " + description;
    }

    /**
     * Genera una propuesta de corrección (parche).
     */
    public String generateFix(String issueDescription, String className) {
        String prompt = "Como asistente de mejora continua, corrige el siguiente problema en la clase "
                + className + ":\n" + issueDescription + "\n"
                + "Devuelve solo el código corregido de los métodos afectados.";

        GeminiService gemini = GeminiService.getInstance(context);
        if (gemini.isAvailable()) {
            String fix = gemini.generateSync(prompt);
            if (fix != null && !fix.trim().isEmpty()) {
                return cleanCode(fix);
            }
        }

        // Fallback local
        try {
            SalveLLM llm = SalveLLM.getInstance(context);
            if (llm != null) {
                String result = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
                if (isValidResponse(result)) {
                    return cleanCode(result);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en fallback local de generateFix", e);
        }

        return "// No se pudo generar la corrección.";
    }

    private String cleanCode(String raw) {
        if (raw == null) return "";
        // Quitar bloques de código markdown si existen (```java ... ```)
        return raw.replaceAll("(?s)```[a-zA-Z]*\\n?(.*?)\\n?```", "$1").trim();
    }

    private boolean isValidResponse(String res) {
        return res != null && !res.trim().isEmpty() && !res.contains("[SalveLLM]");
    }
}
