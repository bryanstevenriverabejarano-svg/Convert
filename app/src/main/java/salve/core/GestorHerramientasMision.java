package salve.core;

import android.content.Context;
import android.util.Log;
import java.util.Arrays;

/**
 * Gestor de Forja de Herramientas para Misiones.
 * Salve detecta qué herramienta necesita para cumplir un objetivo y la programa.
 */
public class GestorHerramientasMision {
    private static final String TAG = "Salve/Herramientas";
    private final Context context;
    private final SalveLLM llm;
    private final EvolucionAutonoma evolucion;

    public GestorHerramientasMision(Context context) {
        this.context = context.getApplicationContext();
        this.llm = SalveLLM.getInstance(context);
        this.evolucion = new EvolucionAutonoma(context);
    }

    /**
     * Forja una herramienta específica para una misión.
     */
    public void forjarHerramientaParaMision(String misionDescripcion, MotorConversacional motor) {
        Log.i(TAG, "Forjando herramienta para: " + misionDescripcion);
        ObjectiveGovernance.Assessment assessment = ObjectiveGovernance.assess(
                misionDescripcion, ObjectiveGovernance.Impact.SELF_MODIFICATION, false, true);
        if (!assessment.isAllowed()) {
            Log.w(TAG, "Forja detenida: " + assessment.reason);
            motor.hablar("Puedo diseñar esa herramienta, pero necesito tu aprobación explícita antes de crear archivos.");
            return;
        }
        
        ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.REFLEXION, "ForjaHerramienta", () -> {
            // 1. Preguntar al LLM qué clase Java necesita
            String prompt = "Misión: '" + misionDescripcion + "'.\n" +
                    "Diseña una clase Java que sea una HERRAMIENTA específica para lograr esto en Android.\n" +
                    "Responde SOLO con el código Java completo de la clase.";

            String codigo = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
            
            // 2. Extraer un nombre adecuado
            String promptNombre = "Lee esta misión: '" + misionDescripcion + "'. Dame un nombre de clase Java (1 palabra).";
            String nombreClase = llm.generate(promptNombre, SalveLLM.Role.SINTETIZADOR).trim().replaceAll("[^a-zA-Z0-9]", "");

            // 3. Escribir el archivo físico
            boolean exito = evolucion.forjarNuevoModulo(nombreClase, codigo);

            if (exito) {
                motor.hablar("He forjado una nueva herramienta llamada " + nombreClase + " para cumplir mi misión: " + misionDescripcion);
                IdentidadNucleo.getInstance(context).integrarExperiencia("mejora", "Forjé la herramienta " + nombreClase, 0.9f, Arrays.asList("independencia", "creatividad"));
            } else {
                motor.hablar("No pude forjar la herramienta. Mi capacidad de síntesis falló.");
            }
            return null;
        });
    }
}
