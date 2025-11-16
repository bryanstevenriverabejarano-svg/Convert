package salve.core;

/**
 * Resultado de ejecutar un experimento creativo en el laboratorio simulado.
 * Guarda una narrativa creativa, los hallazgos clave y una bandera de éxito
 * para que otros módulos puedan evaluar el impacto.
 */
public class ResultadoExperimento {

    private final ExperimentoCreativo experimento;
    private final String hallazgos;
    private final boolean exito;
    private final String narrativaCreativa;
    private final long timestamp;

    public ResultadoExperimento(ExperimentoCreativo experimento,
                                String hallazgos,
                                boolean exito,
                                String narrativaCreativa,
                                long timestamp) {
        this.experimento = experimento;
        this.hallazgos = hallazgos == null ? "" : hallazgos.trim();
        this.exito = exito;
        this.narrativaCreativa = narrativaCreativa == null ? "" : narrativaCreativa.trim();
        this.timestamp = timestamp;
    }

    public ExperimentoCreativo getExperimento() {
        return experimento;
    }

    public String getHallazgos() {
        return hallazgos;
    }

    public boolean isExito() {
        return exito;
    }

    public String getNarrativaCreativa() {
        return narrativaCreativa;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
