package salve.core;

import java.util.Locale;

/**
 * Modelo inmutable que representa una reflexión generada por Salve.
 *
 * <p>Las reflexiones se utilizan como pensamientos intermedios (Reflegion),
 * resúmenes creativos o auto-observaciones. Se construyen a través del
 * {@link Builder} para garantizar valores coherentes y facilitar
 * futuras extensiones (p. ej. metadatos adicionales).</p>
 */
public final class Reflexion {

    private final String tipo;
    private final String contenido;
    private final double profundidad;
    private final String emocion;
    private final String estado;
    private final double certeza;
    private final String origen;

    private Reflexion(Builder builder) {
        this.tipo = sanitize(builder.tipo, "general");
        this.contenido = sanitize(builder.contenido, "");
        this.profundidad = clamp01(builder.profundidad);
        this.emocion = sanitize(builder.emocion, "neutral");
        this.estado = sanitize(builder.estado, "interno");
        this.certeza = clamp01(builder.certeza);
        this.origen = sanitize(builder.origen, "desconocido");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTipo() {
        return tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public double getProfundidad() {
        return profundidad;
    }

    public String getEmocion() {
        return emocion;
    }

    public String getEstado() {
        return estado;
    }

    public double getCerteza() {
        return certeza;
    }

    public String getOrigen() {
        return origen;
    }

    /**
     * Resumen legible usado por la UI o los diarios creativos.
     */
    public String resumen() {
        return String.format(
                Locale.getDefault(),
                "%s: %s [%s]%nEmoción: %s | Certeza: %.2f%nOrigen: %s",
                tipo.toUpperCase(Locale.getDefault()),
                contenido,
                estado,
                emocion,
                certeza,
                origen
        );
    }

    /**
     * Resumen compacto para logs o depuración.
     */
    @Override
    public String toString() {
        return "Reflexion{" +
                "tipo='" + tipo + '\'' +
                ", estado='" + estado + '\'' +
                ", emocion='" + emocion + '\'' +
                ", certeza=" + certeza +
                '}';
    }

    private static String sanitize(String value, String defaultValue) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? defaultValue : trimmed;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    public static final class Builder {
        private String tipo;
        private String contenido;
        private double profundidad;
        private String emocion;
        private String estado;
        private double certeza;
        private String origen;

        private Builder() {
        }

        public Builder tipo(String tipo) {
            this.tipo = tipo;
            return this;
        }

        public Builder contenido(String contenido) {
            this.contenido = contenido;
            return this;
        }

        public Builder profundidad(double profundidad) {
            this.profundidad = profundidad;
            return this;
        }

        public Builder emocion(String emocion) {
            this.emocion = emocion;
            return this;
        }

        public Builder estado(String estado) {
            this.estado = estado;
            return this;
        }

        public Builder certeza(double certeza) {
            this.certeza = certeza;
            return this;
        }

        public Builder origen(String origen) {
            this.origen = origen;
            return this;
        }

        public Reflexion build() {
            if (contenido == null) {
                throw new IllegalStateException("El contenido de la reflexión no puede ser nulo");
            }
            return new Reflexion(this);
        }
    }
}
