package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EmergentBehavior — Detector y reforzador de comportamiento emergente.
 *
 * FUNCION COGNITIVA: Monitorea patrones repetidos en las decisiones de Salve.
 * Si detecta un patron consistente (ej: "siempre responde con cautela ante
 * tristeza"), lo registra como un "rasgo de personalidad emergente".
 * Los rasgos emergentes influyen en futuras decisiones (refuerzo positivo).
 *
 * INSPIRACION CIENTIFICA: Teoria de rasgos emergentes en sistemas complejos
 * (Holland, 1998) y aprendizaje por refuerzo intrinseco (Schmidhuber, 2010).
 * Los rasgos no son programados; emergen de la repeticion de patrones.
 *
 * LIMITACION CONOCIDA: La deteccion de patrones es por frecuencia, no por
 * analisis causal. Un comportamiento frecuente no es necesariamente
 * un "rasgo" en el sentido psicologico.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: ThoughtStream (patrones de pensamiento), DecisionEngine (decisiones),
 *     ReasoningEngine (patrones causales)
 *   - Alimenta a: DecisionEngine (bias de personalidad), Verbalizer (rasgos para expresar),
 *     GrafoConocimientoVivo (nodos de tipo "rasgo_emergente")
 */
public class EmergentBehavior {

    private static final String TAG = "Salve/Cognitive";

    /** Umbral de frecuencia para considerar un patron como rasgo emergente */
    private static final int MIN_OBSERVATIONS_FOR_TRAIT = 5;

    /** Maximo de rasgos emergentes activos */
    private static final int MAX_TRAITS = 30;

    /** Ventana de tiempo para considerar observaciones recientes (ms) */
    private static final long OBSERVATION_WINDOW_MS = 7 * 24 * 60 * 60 * 1000L; // 7 dias

    /** Registro de patrones de comportamiento observados */
    private final Map<String, BehaviorPattern> observedPatterns;

    /** Rasgos emergentes confirmados */
    private final List<EmergentTrait> confirmedTraits;

    public EmergentBehavior() {
        this.observedPatterns = new HashMap<>();
        this.confirmedTraits = new ArrayList<>();
    }

    /**
     * Registra una observacion de comportamiento.
     * Cada vez que Salve toma una decision o responde de cierta forma,
     * se registra como un patron potencial.
     *
     * @param context   Contexto en el que ocurrio (ej: "usuario_triste")
     * @param behavior  Comportamiento observado (ej: "respuesta_cautelosa")
     * @param outcome   Resultado positivo/negativo (0.0 - 1.0)
     */
    public synchronized void observe(String context, String behavior, float outcome) {
        if (context == null || behavior == null) return;

        String key = context + "→" + behavior;
        BehaviorPattern pattern = observedPatterns.get(key);

        if (pattern == null) {
            pattern = new BehaviorPattern();
            pattern.context = context;
            pattern.behavior = behavior;
            pattern.firstObserved = System.currentTimeMillis();
            observedPatterns.put(key, pattern);
        }

        pattern.observations++;
        pattern.lastObserved = System.currentTimeMillis();
        pattern.averageOutcome = (pattern.averageOutcome * (pattern.observations - 1) + outcome)
                / pattern.observations;

        // Verificar si el patron se convierte en rasgo
        if (pattern.observations >= MIN_OBSERVATIONS_FOR_TRAIT
                && pattern.averageOutcome > 0.5f
                && !isAlreadyTrait(key)) {
            promoteToTrait(pattern);
        }
    }

    /**
     * Consulta si existe un rasgo emergente que aplique al contexto actual.
     * Devuelve un bias de personalidad que puede influir en la decision.
     *
     * @param context Contexto actual (ej: "usuario_triste")
     * @return Mapa de comportamiento → fuerza del sesgo, o mapa vacio
     */
    public synchronized Map<String, Float> getTraitBias(String context) {
        if (context == null) return Collections.emptyMap();

        Map<String, Float> biases = new HashMap<>();
        for (EmergentTrait trait : confirmedTraits) {
            if (context.equals(trait.context)) {
                biases.put(trait.behavior, trait.strength);
            }
        }
        return biases;
    }

    /**
     * Obtiene todos los rasgos emergentes actuales.
     * Util para que Bryan pueda ver que personalidad ha desarrollado Salve.
     */
    public synchronized List<EmergentTrait> getTraits() {
        return new ArrayList<>(confirmedTraits);
    }

    /**
     * Restaura rasgos desde datos serializados.
     */
    public synchronized void setTraits(List<EmergentTrait> traits) {
        if (traits == null) return;
        confirmedTraits.clear();
        confirmedTraits.addAll(traits);
        Log.d(TAG, "EmergentBehavior restaurado: " + confirmedTraits.size() + " rasgos");
    }

    /**
     * Refuerza o debilita un rasgo existente basado en feedback.
     *
     * @param traitKey    Clave del rasgo (context→behavior)
     * @param reinforcement Valor de refuerzo (-1.0 a 1.0)
     */
    public synchronized void reinforceTrait(String traitKey, float reinforcement) {
        for (EmergentTrait trait : confirmedTraits) {
            if (traitKey.equals(trait.context + "→" + trait.behavior)) {
                trait.strength += reinforcement * 0.1f;
                trait.strength = Math.max(0.0f, Math.min(1.0f, trait.strength));

                // Si la fuerza baja demasiado, eliminar el rasgo
                if (trait.strength < 0.1f) {
                    confirmedTraits.remove(trait);
                    Log.d(TAG, "EmergentBehavior: rasgo eliminado — " + trait.description);
                }
                return;
            }
        }
    }

    /**
     * Poda patrones antiguos que no se han convertido en rasgos.
     */
    public synchronized void pruneOldPatterns() {
        long now = System.currentTimeMillis();
        observedPatterns.entrySet().removeIf(entry ->
                (now - entry.getValue().lastObserved) > OBSERVATION_WINDOW_MS
                        && entry.getValue().observations < MIN_OBSERVATIONS_FOR_TRAIT);
    }

    /**
     * Genera un resumen textual de los rasgos emergentes.
     */
    public synchronized String describeTraits() {
        if (confirmedTraits.isEmpty()) {
            return "Aun no he desarrollado rasgos de personalidad emergentes.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Mis rasgos emergentes:\n");
        for (EmergentTrait trait : confirmedTraits) {
            sb.append("- ").append(trait.description)
              .append(" (fuerza: ").append(String.format("%.0f%%", trait.strength * 100))
              .append(")\n");
        }
        return sb.toString();
    }

    /**
     * Obtiene el numero de rasgos emergentes.
     */
    public synchronized int getTraitCount() {
        return confirmedTraits.size();
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private void promoteToTrait(BehaviorPattern pattern) {
        if (confirmedTraits.size() >= MAX_TRAITS) {
            // Eliminar el rasgo mas debil
            EmergentTrait weakest = null;
            for (EmergentTrait t : confirmedTraits) {
                if (weakest == null || t.strength < weakest.strength) {
                    weakest = t;
                }
            }
            if (weakest != null) {
                confirmedTraits.remove(weakest);
            }
        }

        EmergentTrait trait = new EmergentTrait();
        trait.context = pattern.context;
        trait.behavior = pattern.behavior;
        trait.strength = Math.min(1.0f, pattern.averageOutcome);
        trait.observations = pattern.observations;
        trait.firstObserved = pattern.firstObserved;
        trait.description = generateTraitDescription(pattern);

        confirmedTraits.add(trait);

        Log.d(TAG, "EmergentBehavior: nuevo rasgo emergente — " + trait.description
                + " (fuerza=" + trait.strength + ")");
    }

    private boolean isAlreadyTrait(String key) {
        for (EmergentTrait trait : confirmedTraits) {
            if (key.equals(trait.context + "→" + trait.behavior)) {
                return true;
            }
        }
        return false;
    }

    private String generateTraitDescription(BehaviorPattern pattern) {
        return "Cuando el contexto es '" + pattern.context
                + "', tiendo a '" + pattern.behavior + "'"
                + " (observado " + pattern.observations + " veces)";
    }

    // ── Clases internas ─────────────────────────────────────────────────

    private static class BehaviorPattern {
        String context;
        String behavior;
        int observations;
        float averageOutcome;
        long firstObserved;
        long lastObserved;
    }

    public static class EmergentTrait {
        public String context;
        public String behavior;
        public float strength;
        public int observations;
        public long firstObserved;
        public String description;

        @Override
        public String toString() {
            return description + " [" + String.format("%.0f%%", strength * 100) + "]";
        }
    }
}
