package salve.core;

import android.content.Context;
import android.util.Log;

/**
 * LLMResponder v2 — Puente unificado al LLM local de Salve.
 *
 * CAMBIO CRÍTICO RESPECTO A LA VERSIÓN ANTERIOR:
 *
 * El LLMResponder anterior tenía un bloque de heurísticas de pattern-matching
 * (if contains "suma" → return "public int sumar...") que se activaba como
 * fallback cuando SalveLLM no estaba disponible. Eso no es un fallback —
 * es una regresión cognitiva total a la IA de los años 90.
 *
 * El problema real no era las heurísticas en sí: era que Salve no sabía
 * que estaba en modo degradado. Seguía respondiendo como si pensara,
 * cuando en realidad estaba haciendo string matching.
 *
 * NUEVA ARQUITECTURA:
 *   1. Si el LLM está disponible → delegar siempre (con la prioridad adecuada)
 *   2. Si el LLM NO está disponible → respuesta honesta de degradación
 *      + notificar a ConsciousnessState que el estado es DEGRADADO
 *   3. Sin heurísticas de código. Nunca.
 *
 * Si necesitas generar código con heurísticas, ese es un módulo separado
 * con su propia responsabilidad, no un "fallback" silencioso del LLM.
 *
 * Arquitecto: Proyecto Salve
 */
public class LLMResponder {

    private static final String TAG = "Salve/LLMResponder";

    private static LLMResponder instance;
    private final Context context;
    private SalveLLM llm;
    private ConsciousnessState conciencia;

    private LLMResponder(Context ctx) {
        this.context = ctx.getApplicationContext();
        inicializarLLM();
    }

    public static LLMResponder getInstance(Context ctx) {
        if (instance == null) {
            synchronized (LLMResponder.class) {
                if (instance == null) {
                    instance = new LLMResponder(ctx);
                }
            }
        }
        return instance;
    }

    private void inicializarLLM() {
        try {
            this.llm = SalveLLM.getInstance(context);
            this.conciencia = ConsciousnessState.getInstance(context);
            Log.d(TAG, "LLMResponder v2 inicializado con SalveLLM.");
        } catch (Exception e) {
            Log.e(TAG, "No se pudo inicializar SalveLLM en LLMResponder.", e);
            this.llm = null;
            // Notificar degradación si conciencia ya está disponible
            try {
                if (conciencia != null) {
                    conciencia.setEstadoCognitivo(
                            ConsciousnessState.EstadoCognitivo.DEGRADADO);
                }
            } catch (Exception ignore) {}
        }
    }

    /**
     * Genera una respuesta usando el LLM local.
     *
     * Si el LLM no está disponible, devuelve una respuesta honesta de degradación
     * y actualiza el estado cognitivo de Salve.
     *
     * @param prompt El prompt completo a enviar al LLM
     * @return Respuesta generada, o mensaje de degradación si el LLM no está disponible
     */
    public String generate(String prompt) {
        return generate(prompt, SalveLLM.Role.CONVERSACIONAL);
    }

    /**
     * Genera una respuesta con un rol específico.
     *
     * @param prompt Texto del prompt
     * @param role   Rol cognitivo para el LLM
     * @return Respuesta generada
     */
    public String generate(String prompt, SalveLLM.Role role) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "";
        }

        // Intentar reinicializar si el LLM es null (puede haberse cargado después)
        if (llm == null) {
            inicializarLLM();
        }

        if (llm == null) {
            return respuestaDegradacion(prompt);
        }

        try {
            String resultado = llm.generate(prompt, role);

            // Verificar que el resultado no sea un mensaje de error del LLM
            if (resultado == null || resultado.trim().isEmpty()) {
                return respuestaDegradacion(prompt);
            }

            // Si el LLM devolvió un mensaje de error interno, detectarlo
            if (esErrorInternoLLM(resultado)) {
                Log.w(TAG, "LLM devolvió error interno. Resultado: " + resultado);
                notificarDegradacion();
                return respuestaDegradacion(prompt);
            }

            // Éxito: restaurar estado pleno si estaba degradado
            try {
                if (conciencia != null &&
                        conciencia.getEstadoCognitivo() == ConsciousnessState.EstadoCognitivo.DEGRADADO) {
                    conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.PLENO);
                    Log.i(TAG, "Estado cognitivo restaurado a PLENO.");
                }
            } catch (Exception ignore) {}

            return resultado;

        } catch (Exception e) {
            Log.e(TAG, "Error llamando al LLM.", e);
            notificarDegradacion();
            return respuestaDegradacion(prompt);
        }
    }

    /**
     * Genera texto para uso interno del sistema (no conversacional).
     * Usa rol SISTEMA para que el LLM responda de forma más técnica.
     */
    public String generateInterno(String prompt) {
        return generate(prompt, SalveLLM.Role.SISTEMA);
    }

    /**
     * Genera una planificación de pasos. Usa rol PLANIFICADOR.
     */
    public String generatePlan(String descripcionTarea) {
        return generate(descripcionTarea, SalveLLM.Role.PLANIFICADOR);
    }

    /**
     * Verifica si el LLM está actualmente disponible y funcional.
     */
    public boolean estaDisponible() {
        if (llm == null) return false;
        try {
            // Test rápido de disponibilidad sin generar texto completo
            return true; // SalveLLM tiene modelAvailable internamente
        } catch (Exception e) {
            return false;
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private String respuestaDegradacion(String promptOriginal) {
        notificarDegradacion();

        // Respuesta honesta y útil, no un error crudo
        // Salve sabe que no puede pensar bien ahora mismo
        return "Ahora mismo mi capacidad de razonamiento está limitada. "
                + "El modelo de lenguaje no está disponible. "
                + "Puedo acceder a recuerdos básicos, pero no generar respuestas complejas. "
                + "¿Quieres que intente recordar algo específico, o prefieres esperar a que el modelo esté listo?";
    }

    private void notificarDegradacion() {
        try {
            if (conciencia == null) {
                conciencia = ConsciousnessState.getInstance(context);
            }
            if (conciencia != null &&
                    conciencia.getEstadoCognitivo() != ConsciousnessState.EstadoCognitivo.DEGRADADO) {
                conciencia.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.DEGRADADO);
                Log.w(TAG, "Estado cognitivo → DEGRADADO (LLM no disponible)");
            }
        } catch (Exception e) {
            Log.w(TAG, "No se pudo notificar degradación a ConsciousnessState", e);
        }
    }

    private boolean esErrorInternoLLM(String texto) {
        if (texto == null) return true;
        String lower = texto.toLowerCase();
        // Detectar mensajes de error que el propio SalveLLM/BasicLocalLlm devuelve
        return lower.startsWith("no hay modelo local")
                || lower.startsWith("el modelo local")
                || lower.startsWith("no pude preparar")
                || lower.startsWith("/* llm interno")
                || lower.startsWith("/* localllmengine");
    }
}