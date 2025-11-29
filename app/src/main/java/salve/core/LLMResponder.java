package salve.core;

import android.content.Context;

import salve.core.SalveLLM;

/**
 * Stub de LLMResponder para que el proyecto compile.
 * Más adelante reemplázalo por tu implementación real.
 */
public class LLMResponder {
    private static LLMResponder instance;
    // Se mantiene una referencia al contexto por si en el futuro es necesario
    // acceder a preferencias o a recursos. De momento no se utiliza.
    private final Context context;

    private LLMResponder(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    public static LLMResponder getInstance(Context ctx) {
        if (instance == null) instance = new LLMResponder(ctx);
        return instance;
    }

    /**
     * Genera una respuesta a partir de un prompt. Este método implementa
     * un motor muy simple sin dependencias externas, con algunas
     * heurísticas básicas para tareas comunes. Si no reconoce la tarea,
     * delega en SalveLLM (si está disponible).
     *
     * @param prompt Texto que describe la tarea a ejecutar.
     * @return respuesta generada o comentario indicativo
     */
    public String generate(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "/* LLM interno: prompt vacío */";
        }
        String lower = prompt.toLowerCase();
        // Heurísticas básicas de generación de código o respuestas
        // 1. Operaciones aritméticas
        if (lower.contains("suma") || lower.contains("sumar")) {
            return "public int sumar(int a, int b) {\n    return a + b;\n}";
        }
        if (lower.contains("resta") || lower.contains("restar")) {
            return "public int restar(int a, int b) {\n    return a - b;\n}";
        }
        if (lower.contains("multiplica") || lower.contains("multiplicar")) {
            return "public int multiplicar(int a, int b) {\n    return a * b;\n}";
        }
        if (lower.contains("divid") || lower.contains("división") || lower.contains("dividir")) {
            return "public int dividir(int a, int b) {\n    if (b == 0) throw new IllegalArgumentException(\"división por cero\");\n    return a / b;\n}";
        }
        // 2. Mostrar por consola
        if (lower.contains("imprimir") || lower.contains("mostrar")) {
            return "System.out.println(\"" + prompt.replace("\"", "\\\"") + "\");";
        }

        // 3. Si no se detecta ningún patrón conocido,
        //    intentamos delegar en el nuevo gestor de modelos.
        try {
            SalveLLM llm = SalveLLM.getInstance(context);
            if (llm != null) {
                return llm.generate(prompt, SalveLLM.Role.CONVERSACIONAL);
            } else {
                return "/* LLM interno: SalveLLM no está disponible en este momento */";
            }
        } catch (Exception e) {
            // No propagamos la excepción para no romper la app;
            // devolvemos un mensaje seguro.
            return "/* LLM interno: error al inicializar SalveLLM: " +
                    (e.getMessage() == null ? "desconocido" : e.getMessage()) +
                    " */";
        }
    }
}
