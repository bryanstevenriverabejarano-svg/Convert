package salve.core;

import java.util.Map;

/**
 * Representa un paso de un "plan" de acciones que el servicio de accesibilidad debe ejecutar.
 */
public class PlanStep {

    public final ActionType action;            // Qué acción ejecutar
    public final Map<String,String> params;    // Parámetros (por ejemplo: paquete, id, texto…)

    /**
     * @param action  Tipo de acción (enum)
     * @param params  Parámetros para esa acción
     */
    public PlanStep(ActionType action, Map<String,String> params) {
        this.action = action;
        this.params = params;
    }
}