package salve.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Representa un experimento creativo dentro de la agenda de investigación
 * autónoma de Salve. Un experimento captura la pregunta de investigación,
 * la hipótesis inicial, el método sugerido y los recursos a emplear.
 *
 * La clase está diseñada para ser inmutable en sus atributos básicos pero
 * permite actualizar el estado del flujo (pendiente, en curso, completado).
 */
public class ExperimentoCreativo {

    public enum Estado {
        PENDIENTE,
        EN_CURSO,
        COMPLETADO,
        BLOQUEADO
    }

    private final String id;
    private final String tema;
    private final String pregunta;
    private final String hipotesis;
    private final List<String> pasosMetodo;
    private final List<String> recursos;
    private final int prioridad;
    private Estado estado;
    private String notasSeguimiento;

    public ExperimentoCreativo(String tema,
                               String pregunta,
                               String hipotesis,
                               List<String> pasosMetodo,
                               List<String> recursos,
                               int prioridad) {
        this.id = UUID.randomUUID().toString();
        this.tema = tema == null ? "" : tema.trim();
        this.pregunta = pregunta == null ? "" : pregunta.trim();
        this.hipotesis = hipotesis == null ? "" : hipotesis.trim();
        this.pasosMetodo = pasosMetodo == null
                ? new ArrayList<>()
                : new ArrayList<>(pasosMetodo);
        this.recursos = recursos == null
                ? new ArrayList<>()
                : new ArrayList<>(recursos);
        this.prioridad = Math.max(1, prioridad);
        this.estado = Estado.PENDIENTE;
        this.notasSeguimiento = "";
    }

    public String getId() {
        return id;
    }

    public String getTema() {
        return tema;
    }

    public String getPregunta() {
        return pregunta;
    }

    public String getHipotesis() {
        return hipotesis;
    }

    public List<String> getPasosMetodo() {
        return Collections.unmodifiableList(pasosMetodo);
    }

    public List<String> getRecursos() {
        return Collections.unmodifiableList(recursos);
    }

    public int getPrioridad() {
        return prioridad;
    }

    public Estado getEstado() {
        return estado;
    }

    public void actualizarEstado(Estado nuevoEstado) {
        if (nuevoEstado != null) {
            this.estado = nuevoEstado;
        }
    }

    public String getNotasSeguimiento() {
        return notasSeguimiento;
    }

    public void registrarNota(String nota) {
        if (nota == null || nota.trim().isEmpty()) {
            return;
        }
        if (notasSeguimiento == null || notasSeguimiento.isEmpty()) {
            notasSeguimiento = nota.trim();
        } else {
            notasSeguimiento = notasSeguimiento + "\n" + nota.trim();
        }
    }

    public String descripcionDetallada() {
        StringBuilder builder = new StringBuilder();
        builder.append("Tema: ").append(tema).append('\n');
        builder.append("Pregunta: ").append(pregunta).append('\n');
        builder.append("Hipótesis: ").append(hipotesis).append('\n');
        builder.append("Pasos propuestos: ").append(pasosMetodo.isEmpty() ? "(sin definir)" : String.join(" → ", pasosMetodo)).append('\n');
        builder.append("Recursos sugeridos: ").append(recursos.isEmpty() ? "(sin recursos)" : String.join(", ", recursos)).append('\n');
        builder.append("Prioridad: ").append(prioridad).append('\n');
        builder.append("Estado actual: ").append(estado.name());
        if (notasSeguimiento != null && !notasSeguimiento.isEmpty()) {
            builder.append('\n').append("Notas: ").append(notasSeguimiento);
        }
        return builder.toString();
    }
}
