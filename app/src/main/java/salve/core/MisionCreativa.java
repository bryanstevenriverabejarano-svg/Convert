package salve.core;

import androidx.annotation.NonNull;

/**
 * Representa una misión creativa dinámica con metadatos cuantificables
 * que permiten priorizar el aprendizaje continuo de Salve.
 */
public class MisionCreativa {

    private final String descripcion;
    private final String metaCuantificable;
    private final int nivelCreatividad; // 1 (bajo) - 5 (alto)
    private final String emocionObjetivo;
    private final int prioridad; // 1 (baja) - 5 (alta)
    private final long timestamp;

    public MisionCreativa(String descripcion,
                          String metaCuantificable,
                          int nivelCreatividad,
                          String emocionObjetivo,
                          int prioridad) {
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.metaCuantificable = metaCuantificable == null ? "" : metaCuantificable.trim();
        this.nivelCreatividad = clamp(nivelCreatividad, 1, 5);
        this.emocionObjetivo = emocionObjetivo == null ? "equilibrada" : emocionObjetivo.trim();
        this.prioridad = clamp(prioridad, 1, 5);
        this.timestamp = System.currentTimeMillis();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMetaCuantificable() {
        return metaCuantificable;
    }

    public int getNivelCreatividad() {
        return nivelCreatividad;
    }

    public String getEmocionObjetivo() {
        return emocionObjetivo;
    }

    public int getPrioridad() {
        return prioridad;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Devuelve un resumen compacto apto para listas o dashboards.
     */
    @NonNull
    public String toResumen() {
        return String.format(
                "Misión: %s | Meta: %s | Creatividad: %d | Emoción: %s | Prioridad: %d",
                descripcion,
                metaCuantificable.isEmpty() ? "sin métrica declarada" : metaCuantificable,
                nivelCreatividad,
                emocionObjetivo,
                prioridad
        );
    }
}
