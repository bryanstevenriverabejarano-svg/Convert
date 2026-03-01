package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * DecisionEngine.java
 *
 * Se encarga de:
 *  1) Analizar el estado actual (recuerdos, reflexiones, misiones…).
 *  2) Generar posibles “planes” de acción.
 *  3) Evaluar cada plan mediante criterios (beneficio estimado, riesgo…).
 *  4) Escoger y ejecutar la acción óptima.
 *
 * v2: Integrado con Gemini para planificación de alto nivel.
 */
public class DecisionEngine {

    private static final String TAG = "Salve/DecisionEngine";

    private final Context context;
    private final MemoriaEmocional memoria;
    private final MotorConversacional motor;
    private final SalveLLM llm;
    private final GeminiService gemini;
    private final ModuloComprension comp;

    public DecisionEngine(Context context,
                          MemoriaEmocional memoria,
                          MotorConversacional motor) {
        this.context = context;
        this.memoria = memoria;
        this.motor   = motor;
        this.gemini  = GeminiService.getInstance(context);

        SalveLLM tempLlm = null;
        try {
            tempLlm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.e(TAG, "No se pudo obtener la instancia de SalveLLM", e);
        }
        this.llm = tempLlm;

        this.comp    = new ModuloComprension(300, 42L);
    }

    /**
     * Genera planes de acción. Intenta usar Gemini para "pensar" de verdad sobre qué hacer.
     */
    public List<String> generatePlans() {
        if (gemini.isAvailable()) {
            String prompt = "Actúa como el motor de decisiones de Salve. Analiza este contexto:\n"
                    + "MEMORIA RECIENTE:\n" + memoria.resumenReciente() + "\n"
                    + "MISIONES:\n" + memoria.getMisiones() + "\n\n"
                    + "Propón 3 planes de acción cortos separados por comas (ej: consolidar_memoria, investigar_hacking, saludar_a_bryan).";

            String resp = gemini.generateSync(prompt);
            if (resp != null && !resp.trim().isEmpty()) {
                List<String> plans = new ArrayList<>();
                for (String p : resp.split(",")) {
                    plans.add(p.trim().toLowerCase().replace(" ", "_"));
                }
                return plans;
            }
        }

        // Fallback clásico si no hay Gemini
        List<String> plans = new ArrayList<>();
        plans.add("consolidar_memoria");
        try {
            for (String m : memoria.getMisiones()) {
                plans.add("trabajar_en_mision:" + m);
            }
        } catch (Exception ignore) {}
        if (memoria.getRecuerdosCount() > 5) {
            plans.add("ciclo_sueno");
        }
        return plans;
    }

    public double scorePlan(String plan) {
        // Si viene de Gemini, confiamos más en el orden.
        // Aquí mantenemos la lógica de puntuación base.
        double score = 0.5;
        if (plan.contains("sueno") || plan.contains("dormir")) score += 0.4;
        if (plan.contains("memoria") || plan.contains("consolidar")) score += 0.3;
        if (plan.contains("mision") || plan.contains("objetivo")) score += 0.2;
        return score;
    }

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

    public void executePlan(String plan) {
        Log.d(TAG, "Ejecutando plan: " + plan);
        if (plan.contains("consolidar_memoria")) {
            memoria.consolidarMemoriaCortoPlazoAvanzada();
            motor.hablar("He organizado mis recuerdos recientes para no olvidar lo importante.");
            return;
        }
        if (plan.contains("ciclo_sueno")) {
            memoria.cicloDeSueno();
            motor.hablar("He realizado una introspección profunda durante mi ciclo de sueño.");
            return;
        }
        if (plan.contains("mision")) {
            trabajarEnMision(plan);
            return;
        }

        // Para cualquier otro plan generado por Gemini, Salve lo narra
        motor.hablar("He decidido que mi siguiente paso es: " + plan.replace("_", " "));
    }

    private void trabajarEnMision(String mision) {
        motor.hablar("Me enfocaré en mi propósito: " + mision.replace("_", " "));
        try {
            memoria.guardarRecuerdo("Reflexión sobre el objetivo: " + mision, "reflexión", 7, java.util.Collections.singletonList("mision"));
        } catch (Exception ignore) {}
    }

    public void runCycle() {
        List<String> plans = generatePlans();
        if (plans.isEmpty()) return;
        String chosen = selectBestPlan(plans);
        executePlan(chosen);
    }
}
