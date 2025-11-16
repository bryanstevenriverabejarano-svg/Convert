package salve.core;

import android.content.Context;

/**
 * LLMCoder es un componente experimental encargado de generar código de forma
 * autónoma a partir de descripciones de alto nivel. Este stub no incluye
 * llamadas a modelos externos, pero define la interfaz necesaria para que
 * en el futuro se integre un LLM de programación (por ejemplo, Code Llama
 * u otro modelo de lenguaje especializado en código). La idea es permitir
 * que Salve proponga fragmentos de código o parches que puedan ser
 * evaluados y aplicados, impulsando su auto‑mejora.
 */
public class LLMCoder {

    private static LLMCoder instance;

    /**
     * Referencia al contexto de la aplicación. Se almacena para futuras
     * extensiones donde pueda ser necesario acceder a recursos o preferencias.
     */
    private final Context context;

    private LLMCoder(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    /**
     * Obtiene la instancia singleton de LLMCoder. Utiliza inicialización
     * perezosa para retrasar la construcción hasta el primer uso.
     *
     * @param ctx Contexto de Android.
     * @return la instancia global de LLMCoder
     */
    public static synchronized LLMCoder getInstance(Context ctx) {
        if (instance == null) {
            instance = new LLMCoder(ctx);
        }
        return instance;
    }

    /**
     * Genera código fuente a partir de una descripción en lenguaje natural.
     * Este método es un stub: actualmente devuelve un fragmento de código
     * comentado que describe la tarea solicitada. En una implementación
     * real, se llamaría a un modelo de lenguaje grande entrenado en
     * programación para producir código compilable en el lenguaje solicitado.
     *
     * @param description Descripción de alto nivel de lo que debe hacer el código.
     * @param language    Lenguaje de programación deseado (por ejemplo, "Java").
     * @return Cadena con el código fuente generado o un comentario indicativo.
     */
    public String generateCode(String description, String language) {
        try {
            // Construye un prompt para el LLM. Se pide únicamente código sin explicaciones.
            String prompt = "Eres un generador de código. Genera un fragmento de código en "
                    + language
                    + " que cumpla con la siguiente tarea:\n"
                    + description
                    + "\nDevuelve solo el código, sin explicaciones adicionales.";
            LLMResponder llm = LLMResponder.getInstance(context);
            String result = llm.generate(prompt);
            // Si el resultado es vacío o contiene el mensaje de error, devolvemos el stub anterior
            if (result == null || result.trim().isEmpty() || result.contains("[LLMResponder]")) {
                return "// No se pudo generar código con el LLM.\n"
                        + "// Descripción: " + description + "\n"
                        + "// Lenguaje: " + language;
            }
            return result.trim();
        } catch (Exception e) {
            return "// Error al generar código: " + e.getMessage() + "\n"
                    + "// Descripción: " + description + "\n"
                    + "// Lenguaje: " + language;
        }
    }

    /**
     * Genera una propuesta de corrección para un issue detectado durante
     * el análisis de código. Se utiliza para sugerir cambios concretos
     * relacionados con problemas encontrados en métodos existentes.
     *
     * @param issueDescription Descripción textual del problema a resolver.
     * @param className        Nombre de la clase donde se detectó el problema.
     * @return Cadena que contiene un parche de ejemplo o sugerencia de solución.
     */
    public String generateFix(String issueDescription, String className) {
        try {
            String prompt = "Eres un asistente que sugiere correcciones de código. "
                    + "Debes proporcionar un parche en Java para corregir el siguiente problema:\n"
                    + issueDescription
                    + "\nen la clase "
                    + className
                    + ". Devuelve solo el código actualizado sin explicaciones.";
            LLMResponder llm = LLMResponder.getInstance(context);
            String result = llm.generate(prompt);
            if (result == null || result.trim().isEmpty() || result.contains("[LLMResponder]")) {
                return "// No se pudo generar una corrección con el LLM.\n"
                        + "// Clase: " + className + "\n"
                        + "// Problema: " + issueDescription;
            }
            return result.trim();
        } catch (Exception e) {
            return "// Error al generar corrección: " + e.getMessage() + "\n"
                    + "// Clase: " + className + "\n"
                    + "// Problema: " + issueDescription;
        }
    }
}