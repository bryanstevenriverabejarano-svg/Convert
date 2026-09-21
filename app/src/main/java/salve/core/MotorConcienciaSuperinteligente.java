package salve.core;

import android.content.Context;

/**
 * Entrada compatible para preparar propuestas sobre los objetivos de Salve.
 *
 * <p>El nombre se conserva por compatibilidad. Este componente no demuestra
 * superinteligencia ni experiencia subjetiva. Delega en el ciclo común de
 * objetivos, que conserva las propuestas para revisión y aplica sus límites
 * de pausa, frecuencia y cancelación. No modifica automáticamente la identidad
 * ni convierte una reflexión generada en un recuerdo de hechos.</p>
 */
public class MotorConcienciaSuperinteligente {

    private final Context context;

    /** El parámetro de memoria se conserva para los llamadores existentes. */
    public MotorConcienciaSuperinteligente(Context context, MemoriaEmocional memoria) {
        this.context = context.getApplicationContext();
    }

    /**
     * Ejecuta el ciclo común en el hilo del llamador, sin crear otro bucle.
     * Los llamadores periódicos respetan además la prioridad de la conversación.
     */
    public void ejecutarIntrospeccionProfunda() {
        GoalAutonomyRuntime.get(context).runCycle(
                () -> Thread.currentThread().isInterrupted());
    }

    /** Devuelve el resultado real del ciclo solicitado en la conversación. */
    public String prepararPropuesta() {
        return GoalAutonomyRuntime.get(context).respond("avanza tus objetivos");
    }
}
