package salve.core;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Representa una señal multimodal (visual, auditiva o sensorial) preparada para
 * integrarse en la memoria emocional y el grafo de conocimiento sin comprometer
 * privacidad. Incluye metadatos afectivos, etiquetas descriptivas y trazabilidad
 * de supervisión humana para entrenar clasificadores ligeros.
 */
public class MultimodalSignal {

    public enum Tipo {
        VISUAL,
        AUDITIVA,
        SENSORIAL
    }

    public static class Afecto {
        public final double valencia;
        public final double activacion;
        public final String emocionDominante;

        public Afecto(double valencia, double activacion, String emocionDominante) {
            this.valencia = valencia;
            this.activacion = activacion;
            this.emocionDominante = emocionDominante == null ? "" : emocionDominante.trim();
        }

        public String toNarrative() {
            return String.format(Locale.getDefault(),
                    "Valencia %.2f | Activación %.2f | Emoción: %s",
                    valencia,
                    activacion,
                    emocionDominante.isEmpty() ? "desconocida" : emocionDominante);
        }
    }

    public enum RetentionPolicy {
        VOLATIL,
        SEMANAL,
        LARGO_PLAZO
    }

    public static class CuratedMetadata {
        public final String clasificacionPrincipal;
        public final RetentionPolicy retentionPolicy;
        public final boolean sensible;
        public final List<String> etiquetasInferidas;
        public final double prioridadNarrativa;

        CuratedMetadata(String clasificacionPrincipal,
                        RetentionPolicy retentionPolicy,
                        boolean sensible,
                        List<String> etiquetasInferidas,
                        double prioridadNarrativa) {
            this.clasificacionPrincipal = clasificacionPrincipal == null
                    ? "curaduría" : clasificacionPrincipal;
            this.retentionPolicy = retentionPolicy == null
                    ? RetentionPolicy.SEMANAL : retentionPolicy;
            this.sensible = sensible;
            this.etiquetasInferidas = etiquetasInferidas == null
                    ? Collections.emptyList()
                    : new ArrayList<>(etiquetasInferidas);
            this.prioridadNarrativa = prioridadNarrativa;
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append("Curaduría → clasificada como ")
                    .append(clasificacionPrincipal)
                    .append(" | política ")
                    .append(retentionPolicy.name().toLowerCase(Locale.getDefault()));
            if (sensible) {
                builder.append(" | sensible");
            }
            if (!etiquetasInferidas.isEmpty()) {
                builder.append(" | etiquetas inferidas: ")
                        .append(String.join(", ", etiquetasInferidas));
            }
            builder.append(String.format(Locale.getDefault(), " | prioridad %.2f", prioridadNarrativa));
            return builder.toString();
        }
    }

    public static class HumanLabel {
        public final String etiqueta;
        public final String anotador;
        public final boolean coincideCuraduria;
        public final double confianza;
        public final long timestamp;

        HumanLabel(String etiqueta,
                   String anotador,
                   boolean coincideCuraduria,
                   double confianza,
                   long timestamp) {
            this.etiqueta = etiqueta == null ? "" : etiqueta.trim();
            this.anotador = TextUtils.isEmpty(anotador) ? "humano" : anotador.trim();
            this.coincideCuraduria = coincideCuraduria;
            this.confianza = confianza;
            this.timestamp = timestamp;
        }

        public String toNarrative() {
            return String.format(Locale.getDefault(),
                    "%s etiquetó como '%s' (confianza %.2f, coincide=%s)",
                    anotador,
                    etiqueta,
                    confianza,
                    coincideCuraduria ? "sí" : "no");
        }
    }

    private final Tipo tipo;
    private final String etiqueta;
    private final String descripcion;
    private final Afecto afecto;
    private final List<String> etiquetas;
    private final String origen;
    private final String notaPrivacidad;
    private final CuratedMetadata curaduria;
    private final List<HumanLabel> humanLabels;
    private int humanLabelAciertos;
    private int humanLabelTotal;
    private double precisionPromedio;
    private RetentionPolicy retentionOverride;

    public MultimodalSignal(Tipo tipo,
                             String etiqueta,
                             String descripcion,
                             Afecto afecto,
                             List<String> etiquetas,
                             String origen,
                             String notaPrivacidad) {
        this.tipo = tipo == null ? Tipo.VISUAL : tipo;
        this.etiqueta = etiqueta == null ? "" : etiqueta.trim();
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.afecto = afecto == null ? new Afecto(0.0, 0.0, "neutral") : afecto;
        this.etiquetas = etiquetas == null ? new ArrayList<>() : new ArrayList<>(etiquetas);
        this.origen = origen == null ? "desconocido" : origen.trim();
        this.notaPrivacidad = notaPrivacidad == null ? "" : notaPrivacidad.trim();
        this.curaduria = curar();
        this.humanLabels = new ArrayList<>();
        this.humanLabelAciertos = 0;
        this.humanLabelTotal = 0;
        this.precisionPromedio = 0.0;
        this.retentionOverride = null;
        if (curaduria != null && !curaduria.etiquetasInferidas.isEmpty()) {
            for (String etiquetaInferida : curaduria.etiquetasInferidas) {
                if (!this.etiquetas.contains(etiquetaInferida)) {
                    this.etiquetas.add(etiquetaInferida);
                }
            }
        }
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Afecto getAfecto() {
        return afecto;
    }

    public List<String> getEtiquetas() {
        return Collections.unmodifiableList(etiquetas);
    }

    public String getOrigen() {
        return origen;
    }

    public String getNotaPrivacidad() {
        return notaPrivacidad;
    }

    public CuratedMetadata getCuraduria() {
        return curaduria;
    }

    public RetentionPolicy getRetentionPolicy() {
        if (retentionOverride != null) {
            return retentionOverride;
        }
        return curaduria == null ? RetentionPolicy.SEMANAL : curaduria.retentionPolicy;
    }

    public double getPrecisionPromedio() {
        return precisionPromedio;
    }

    public int getTotalEtiquetasHumanas() {
        return humanLabelTotal;
    }

    public List<HumanLabel> getHumanLabels() {
        return Collections.unmodifiableList(humanLabels);
    }

    public void registrarEtiquetaHumana(String etiqueta,
                                        String anotador,
                                        boolean coincide,
                                        double confianza) {
        HumanLabel label = new HumanLabel(etiqueta, anotador, coincide, confianza,
                System.currentTimeMillis());
        humanLabels.add(label);
        humanLabelTotal += 1;
        if (coincide) {
            humanLabelAciertos += 1;
        }
        precisionPromedio = humanLabelTotal == 0 ? 0.0
                : (double) humanLabelAciertos / Math.max(1, humanLabelTotal);
        if (precisionPromedio > 0.82 && getRetentionPolicy() != RetentionPolicy.LARGO_PLAZO) {
            retentionOverride = RetentionPolicy.LARGO_PLAZO;
        } else if (precisionPromedio < 0.45) {
            retentionOverride = RetentionPolicy.VOLATIL;
        }
    }

    public String toNarrative() {
        StringBuilder builder = new StringBuilder();
        builder.append("Señal ").append(tipo.name().toLowerCase(Locale.getDefault()))
                .append(" etiquetada como ")
                .append(TextUtils.isEmpty(etiqueta) ? "sin título" : etiqueta)
                .append(".");
        if (!descripcion.isEmpty()) {
            builder.append(" Descripción: ").append(descripcion).append('.');
        }
        builder.append(" Afecto → ").append(afecto.toNarrative()).append('.');
        if (!etiquetas.isEmpty()) {
            builder.append(" Etiquetas: ").append(String.join(", ", etiquetas)).append('.');
        }
        if (!TextUtils.isEmpty(notaPrivacidad)) {
            builder.append(" Nota de privacidad: ").append(notaPrivacidad).append('.');
        }
        builder.append(" Origen: ").append(origen).append('.');
        if (curaduria != null) {
            builder.append(' ').append(curaduria.toNarrative()).append('.');
        }
        if (!humanLabels.isEmpty()) {
            builder.append(' ')
                    .append(String.format(Locale.getDefault(),
                            "Supervisión humana → %.0f%% precisión con %d etiquetas.",
                            precisionPromedio * 100,
                            humanLabelTotal));
        }
        return builder.toString();
    }

    private CuratedMetadata curar() {
        Set<String> inferidas = new LinkedHashSet<>();
        boolean sensible = !TextUtils.isEmpty(notaPrivacidad)
                || (etiquetas != null && etiquetas.stream().anyMatch(t -> t != null && t.toLowerCase(Locale.getDefault()).contains("privado")));
        double valencia = afecto.valencia;
        double activacion = afecto.activacion;
        String clasificacion;
        RetentionPolicy policy;
        double prioridad;

        if (sensible) {
            policy = RetentionPolicy.VOLATIL;
            clasificacion = "registro_confidencial";
            inferidas.add("sensible");
        } else if (tipo == Tipo.VISUAL && valencia > 0.4 && activacion > 0.5) {
            policy = RetentionPolicy.LARGO_PLAZO;
            clasificacion = "destello_inspiracion";
            inferidas.add("inspiracion_visual");
        } else if (tipo == Tipo.AUDITIVA && activacion > 0.6) {
            policy = RetentionPolicy.SEMANAL;
            clasificacion = "ritmo_experimental";
            inferidas.add("audio_creativo");
        } else if (valencia < -0.2) {
            policy = RetentionPolicy.SEMANAL;
            clasificacion = "señal_alerta";
            inferidas.add("alerta_emocional");
        } else {
            policy = RetentionPolicy.SEMANAL;
            clasificacion = "textura_contextual";
        }

        prioridad = Math.max(0.1, Math.min(1.0, (Math.abs(valencia) + activacion) / 2.0));
        return new CuratedMetadata(clasificacion, policy, sensible, new ArrayList<>(inferidas), prioridad);
    }
}
