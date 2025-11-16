package salve.core;

import android.content.Context;

import java.util.List;

/**
 * DecisionEngine.java
 *
 * Se encarga de:
 *  1) Analizar el estado actual (recuerdos, reflexiones, misiones…).
 *  2) Generar posibles “planes” de acción.
 *  3) Evaluar cada plan mediante criterios (beneficio estimado, riesgo…).
 *  4) Escoger y ejecutar la acción óptima.
 */
public class DecisionEngine {

    private final Context context;
    private final MemoriaEmocional memoria;
    private final MotorConversacional motor;
    private final LLMResponder llm;
    private final ModuloComprension comp;

    /**
     * Constructor.
     *
     * @param context Contexto de Android para acceso a TTS, SharedPrefs…
     * @param memoria Componente de memoria emocional.
     * @param motor   Componente conversacional (para hablar, escuchar…).
     */
    public DecisionEngine(Context context,
                          MemoriaEmocional memoria,
                          MotorConversacional motor) {
        this.context = context;
        this.memoria = memoria;
        this.motor   = motor;
        this.llm     = LLMResponder.getInstance(context);
        this.comp    = new ModuloComprension(300, 42L);
    }

    /**
     * Genera una lista de posibles planes de acción.
     * @return lista de descripciones de planes.
     */
    public List<String> generatePlans() {
        List<String> plans = new java.util.ArrayList<>();
        // Siempre considerar consolidar memoria si hay recuerdos en corto plazo
        plans.add("consolidar_memoria");
        // Añadir planes para cada misión
        try {
            List<String> misiones = memoria.getMisiones();
            for (String m : misiones) {
                // Generar un plan para trabajar en la misión
                plans.add("trabajar_en_mision:" + m);
            }
        } catch (Exception ignore) {
            // Si algo falla, continuamos con otros planes
        }
        // Si hay muchos recuerdos sin consolidar, planear ciclo de sueño
        try {
            int count = memoria.getRecuerdosCount();
            if (count > 5) {
                plans.add("ciclo_sueno");
            }
        } catch (Exception ignore) {}
        return plans;
    }

    /**
     * Puntúa un plan concreto.
     * @param plan descripción del plan.
     * @return score (mayor = mejor).
     */
    public double scorePlan(String plan) {
        double score = 0.0;
        // Priorizar ciclo de sueño cuando hay muchos recuerdos
        if ("ciclo_sueno".equals(plan)) {
            score += 0.8;
        }
        // Priorizar consolidar memoria en segundo lugar
        if ("consolidar_memoria".equals(plan)) {
            score += 0.6;
        }
        // Planes de misión: dar prioridad según relevancia del concepto
        if (plan.startsWith("trabajar_en_mision:")) {
            String mision = plan.substring("trabajar_en_mision:".length());
            try {
                // Usa ModuloComprension para medir afinidad con la misión
                double cscore = comp.comprehensionScore(mision, mision);
                score += 0.5 + 0.5 * cscore; // Normalizar entre 0.5 y 1.0
            } catch (Exception ignore) {
                score += 0.5;
            }
        }
        return score;
    }

    /**
     * Escoge el mejor plan de los generados.
     * @param plans lista de planes.
     * @return plan seleccionado.
     */
    public String selectBestPlan(List<String> plans) {
        String best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (String p : plans) {
            double sc = scorePlan(p);
            if (sc > bestScore) {
                bestScore = sc;
                best = p;
            }
        }
        return best;
    }

    /**
     * Ejecuta el plan elegido.
     * @param plan descripción del plan a ejecutar.
     */
    public void executePlan(String plan) {
        if ("consolidar_memoria".equals(plan)) {
            // Consolidar recuerdos de corto a largo plazo
            memoria.consolidarMemoriaCortoPlazoAvanzada();
            motor.hablar("He consolidado recuerdos recientes en mi memoria de largo plazo.");
            return;
        }
        if ("ciclo_sueno".equals(plan)) {
            memoria.cicloDeSueno();
            motor.hablar("He realizado un ciclo de sueño completo para reorganizar mis recuerdos y generar reflexiones.");
            return;
        }
        if (plan.startsWith("trabajar_en_mision:")) {
            String mision = plan.substring("trabajar_en_mision:".length());
            trabajarEnMision(mision);
            return;
        }
        // Plan desconocido
        motor.hablar("No sé cómo ejecutar el plan: " + plan);
    }

    /**
     * Ejecuta una tarea relacionada con una misión concreta. Por ahora, la
     * implementación es simple: informa al usuario de que va a trabajar
     * en ella e intenta generar un recuerdo de introspección.
     * @param mision el nombre de la misión
     */
    private void trabajarEnMision(String mision) {
        // Hablar con el usuario acerca de la misión
        motor.hablar("Voy a trabajar en la misión: " + mision);
        // Generar un recuerdo introspectivo sobre la misión como recordatorio
        try {
            String frase = "Pensando sobre la misión " + mision + ", quiero avanzar un poco más.";
            memoria.guardarRecuerdo(frase, "reflexión", 5, java.util.Collections.singletonList("mision"));
        } catch (Exception ignore) {}
    }

    /**
     * Recorre todo el ciclo de decisión: genera, puntúa, selecciona y ejecuta.
     */
    public void runCycle() {
        List<String> plans = generatePlans();
        if (plans.isEmpty()) {
            motor.hablar("No he encontrado ningún plan válido.");
            return;
        }
        String chosen = selectBestPlan(plans);
        executePlan(chosen);
    }
}