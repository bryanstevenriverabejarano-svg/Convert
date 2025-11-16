package salve.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Representa un plano de aprendizaje continuo que Salve puede auto-generar para
 * profundizar su infraestructura cognitiva. Define objetivos, modalidades,
 * nodos del grafo a reforzar y pasos de experimentación para guiar ciclos de
 * auto-entrenamiento multimodal.
 */
public class BlueprintAprendizajeContinuo {

    private final String id;
    private final String objetivo;
    private final List<String> modalidades;
    private final List<String> nodosClave;
    private final List<String> etapas;
    private final double confianza;
    private final boolean multimodal;
    private final boolean autoGenerado;
    private final long timestamp;
    private final String narrativa;

    private BlueprintAprendizajeContinuo(String id,
                                         String objetivo,
                                         List<String> modalidades,
                                         List<String> nodosClave,
                                         List<String> etapas,
                                         double confianza,
                                         boolean multimodal,
                                         boolean autoGenerado,
                                         long timestamp,
                                         String narrativa) {
        this.id = id == null ? UUID.randomUUID().toString() : id;
        this.objetivo = objetivo == null ? "Exploración cognitiva" : objetivo.trim();
        this.modalidades = modalidades == null ? new ArrayList<>() : new ArrayList<>(modalidades);
        this.nodosClave = nodosClave == null ? new ArrayList<>() : new ArrayList<>(nodosClave);
        this.etapas = etapas == null ? new ArrayList<>() : new ArrayList<>(etapas);
        this.confianza = Math.max(0.0, Math.min(1.0, confianza));
        this.multimodal = multimodal;
        this.autoGenerado = autoGenerado;
        this.timestamp = timestamp <= 0 ? System.currentTimeMillis() : timestamp;
        this.narrativa = narrativa == null ? "" : narrativa.trim();
    }

    public static BlueprintAprendizajeContinuo crear(String objetivo,
                                                     List<String> modalidades,
                                                     List<String> nodosClave,
                                                     List<String> etapas,
                                                     double confianza,
                                                     boolean multimodal,
                                                     boolean autoGenerado,
                                                     String narrativa) {
        return new BlueprintAprendizajeContinuo(null,
                objetivo,
                modalidades,
                nodosClave,
                etapas,
                confianza,
                multimodal,
                autoGenerado,
                System.currentTimeMillis(),
                narrativa);
    }

    public String getId() {
        return id;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public List<String> getModalidades() {
        return Collections.unmodifiableList(modalidades);
    }

    public List<String> getNodosClave() {
        return Collections.unmodifiableList(nodosClave);
    }

    public List<String> getEtapas() {
        return Collections.unmodifiableList(etapas);
    }

    public double getConfianza() {
        return confianza;
    }

    public boolean isMultimodal() {
        return multimodal;
    }

    public boolean isAutoGenerado() {
        return autoGenerado;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getImpactoCreativo() {
        return Math.max(5, Math.min(10, (int) Math.round(5 + confianza * 5)));
    }

    public List<String> getEtiquetasSugeridas() {
        List<String> etiquetas = new ArrayList<>();
        etiquetas.add("aprendizaje_continuo");
        etiquetas.add("infraestructura_cognitiva");
        if (multimodal) {
            etiquetas.add("multimodal");
        }
        etiquetas.addAll(modalidades);
        return etiquetas;
    }

    public String resumenCorto() {
        return String.format(Locale.getDefault(),
                "Blueprint '%s' → confianza %.2f | modalidades %s", objetivo, confianza,
                modalidades.isEmpty() ? "no definidas" : String.join(", ", modalidades));
    }

    public String toNarrativa() {
        StringBuilder builder = new StringBuilder();
        builder.append("Blueprint de aprendizaje continuo\n");
        builder.append("ID: ").append(id).append('\n');
        builder.append("Objetivo: ").append(objetivo).append('\n');
        builder.append("Modalidades: ").append(modalidades.isEmpty()
                ? "pendientes"
                : String.join(", ", modalidades)).append('\n');
        builder.append("Nodos del grafo a reforzar: ").append(nodosClave.isEmpty()
                ? "por descubrir"
                : String.join(", ", nodosClave)).append('\n');
        builder.append("Confianza del plan: ").append(String.format(Locale.getDefault(), "%.2f", confianza)).append('\n');
        builder.append("Auto-generado: ").append(autoGenerado ? "sí" : "no").append('\n');
        builder.append("Pasos sugeridos:");
        if (etapas.isEmpty()) {
            builder.append("\n- Mapear brechas en la memoria emocional\n- Diseñar micro-experimentos supervisados");
        } else {
            for (String etapa : etapas) {
                builder.append("\n- ").append(etapa);
            }
        }
        if (!narrativa.isEmpty()) {
            builder.append("\nNarrativa contextual: ").append(narrativa);
        }
        builder.append("\nGenerado el: ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(new Date(timestamp)));
        return builder.toString();
    }
}
