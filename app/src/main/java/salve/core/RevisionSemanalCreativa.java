package salve.core;

import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Representa la rutina de revisión semanal colaborativa entre humanos y
 * Salve. Almacena la agenda, los compromisos y el pulso creativo para ser
 * persistido en {@link MemoriaEmocional}.
 */
public class RevisionSemanalCreativa {

    private final long programadaPara;
    private final String agendaNarrativa;
    private final List<String> compromisos;
    private final String facilitador;
    private final String resumenAutomatico;
    private final String variacionNarrativa;
    private final List<String> recomendacionesPrioritarias;
    private final PanelMetricasCreatividad.PanelSemanalReport panelReport;
    private final String alertasEmergentesNarrativa;
    private final List<PanelMetricasCreatividad.RadarAlert> radarAlerts;
    private final String aprendizajeNarrativa;
    private final PanelMetricasCreatividad.AprendizajeSnapshot aprendizajeSnapshot;

    public RevisionSemanalCreativa(long programadaPara,
                                   String agendaNarrativa,
                                   List<String> compromisos,
                                   String facilitador,
                                   String resumenAutomatico,
                                   String variacionNarrativa,
                                   List<String> recomendacionesPrioritarias,
                                   PanelMetricasCreatividad.PanelSemanalReport panelReport,
                                   String alertasEmergentesNarrativa,
                                   List<PanelMetricasCreatividad.RadarAlert> radarAlerts) {
        this.programadaPara = programadaPara;
        this.agendaNarrativa = agendaNarrativa == null ? "" : agendaNarrativa.trim();
        this.compromisos = compromisos == null ? new ArrayList<>() : new ArrayList<>(compromisos);
        this.facilitador = TextUtils.isEmpty(facilitador) ? "Círculo creativo" : facilitador;
        this.resumenAutomatico = resumenAutomatico == null ? "" : resumenAutomatico.trim();
        this.variacionNarrativa = variacionNarrativa == null ? "" : variacionNarrativa.trim();
        this.recomendacionesPrioritarias = recomendacionesPrioritarias == null
                ? new ArrayList<>()
                : new ArrayList<>(recomendacionesPrioritarias);
        this.panelReport = panelReport;
        this.alertasEmergentesNarrativa = alertasEmergentesNarrativa == null
                ? ""
                : alertasEmergentesNarrativa.trim();
        this.radarAlerts = radarAlerts == null
                ? new ArrayList<>()
                : new ArrayList<>(radarAlerts);
        this.aprendizajeNarrativa = panelReport == null ? "" : panelReport.aprendizajeNarrativa;
        this.aprendizajeSnapshot = panelReport == null ? null : panelReport.aprendizajeSnapshot;
    }

    public long getProgramadaPara() {
        return programadaPara;
    }

    public String getAgendaNarrativa() {
        return agendaNarrativa;
    }

    public List<String> getCompromisos() {
        return new ArrayList<>(compromisos);
    }

    public String getResumenAutomatico() {
        return resumenAutomatico;
    }

    public String getVariacionNarrativa() {
        return variacionNarrativa;
    }

    public List<String> getRecomendacionesPrioritarias() {
        return new ArrayList<>(recomendacionesPrioritarias);
    }

    public PanelMetricasCreatividad.PanelSemanalReport getPanelReport() {
        return panelReport;
    }

    public String getAlertasEmergentesNarrativa() {
        return alertasEmergentesNarrativa;
    }

    public List<PanelMetricasCreatividad.RadarAlert> getRadarAlerts() {
        return new ArrayList<>(radarAlerts);
    }

    public String getAprendizajeNarrativa() {
        return aprendizajeNarrativa;
    }

    public PanelMetricasCreatividad.AprendizajeSnapshot getAprendizajeSnapshot() {
        return aprendizajeSnapshot;
    }

    public String construirDashboardIntegrado(GrafoConocimientoVivo.GraphVisualization visualizacion,
                                               String mapaBitacora,
                                               String narrativaCobertura,
                                               String pronosticos) {
        StringBuilder builder = new StringBuilder();
        builder.append("Informe semanal creativo unificado\n");
        builder.append("Facilitador: ").append(facilitador).append(" | Fecha: ")
                .append(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date(programadaPara))).append('\n');
        if (!resumenAutomatico.isEmpty()) {
            builder.append("Resumen panel: ").append(resumenAutomatico).append('\n');
        }
        if (panelReport != null) {
            builder.append(panelReport.variacionNarrativa).append('\n');
            if (!TextUtils.isEmpty(panelReport.coberturaNarrativa)) {
                builder.append(panelReport.coberturaNarrativa).append('\n');
            }
            if (!TextUtils.isEmpty(aprendizajeNarrativa)) {
                builder.append(aprendizajeNarrativa).append('\n');
            }
            if (!panelReport.recomendaciones.isEmpty()) {
                builder.append("Recomendaciones métricas:");
                int idx = 1;
                for (String recomendacion : panelReport.recomendaciones) {
                    builder.append('\n').append(idx++).append(") ").append(recomendacion);
                }
                builder.append('\n');
            }
        }
        if (!TextUtils.isEmpty(narrativaCobertura)) {
            builder.append(narrativaCobertura).append('\n');
        }
        if (!radarAlerts.isEmpty() || !TextUtils.isEmpty(alertasEmergentesNarrativa)) {
            builder.append("Alertas emergentes:\n");
            if (!TextUtils.isEmpty(alertasEmergentesNarrativa)) {
                builder.append(alertasEmergentesNarrativa).append('\n');
            }
            if (!radarAlerts.isEmpty()) {
                int idx = 1;
                for (PanelMetricasCreatividad.RadarAlert alerta : radarAlerts) {
                    builder.append(idx++).append(") ").append(alerta.toNarrative()).append('\n');
                }
            }
        }
        if (visualizacion != null) {
            builder.append("Visualización del grafo creativo:\n");
            builder.append(visualizacion.toAscii()).append('\n');
            builder.append("Snapshot JSON: ").append(visualizacion.toJson()).append('\n');
        }
        if (!TextUtils.isEmpty(mapaBitacora)) {
            builder.append("Mapa de experimentos:\n").append(mapaBitacora).append('\n');
        }
        if (!TextUtils.isEmpty(pronosticos)) {
            builder.append("Pronósticos experimentales:\n").append(pronosticos).append('\n');
        }
        if (!variacionNarrativa.isEmpty()) {
            builder.append("Variación narrativa: ").append(variacionNarrativa).append('\n');
        }
        if (!compromisos.isEmpty()) {
            builder.append("Compromisos vigentes:");
            int idx = 1;
            for (String compromiso : compromisos) {
                builder.append('\n').append(idx++).append(") ").append(compromiso);
            }
            builder.append('\n');
        }
        builder.append("Agenda colaborativa:\n").append(agendaNarrativa);
        return builder.toString();
    }

    public String toNarrative() {
        SimpleDateFormat format = new SimpleDateFormat("EEEE d 'de' MMMM 'a las' HH:mm", Locale.getDefault());
        StringBuilder builder = new StringBuilder();
        builder.append("Revisión semanal programada con ")
                .append(facilitador)
                .append(" para ")
                .append(format.format(new Date(programadaPara)))
                .append('.');
        if (!agendaNarrativa.isEmpty()) {
            builder.append("\nAgenda creativa:\n").append(agendaNarrativa);
        }
        if (!compromisos.isEmpty()) {
            builder.append("\nCompromisos compartidos:");
            int index = 1;
            for (String compromiso : compromisos) {
                builder.append('\n').append(index++).append(") ").append(compromiso);
            }
        }
        if (!resumenAutomatico.isEmpty()) {
            builder.append("\nResumen automático: ").append(resumenAutomatico);
        }
        if (!variacionNarrativa.isEmpty()) {
            builder.append("\nVariación semanal: ").append(variacionNarrativa);
        }
        if (!recomendacionesPrioritarias.isEmpty()) {
            builder.append("\nRecomendaciones prioritarias:");
            int idx = 1;
            for (String recomendacion : recomendacionesPrioritarias) {
                builder.append('\n').append(idx++).append(") ").append(recomendacion);
            }
        }
        if (!radarAlerts.isEmpty() || !TextUtils.isEmpty(alertasEmergentesNarrativa)) {
            builder.append("\nAlertas emergentes: ");
            builder.append(alertasEmergentesNarrativa.isEmpty()
                    ? "El radar no registró anomalías narrativas."
                    : alertasEmergentesNarrativa);
            if (!radarAlerts.isEmpty()) {
                int idx = 1;
                for (PanelMetricasCreatividad.RadarAlert alerta : radarAlerts) {
                    builder.append("\n  ").append(idx++).append(") ").append(alerta.getDimension())
                            .append(" → ").append(alerta.getSeverity());
                }
            }
        }
        builder.append("\nInvitar a Salve a traer ejemplos poéticos y métricas actualizadas.");
        return builder.toString();
    }

    public RevisionSemanalCreativa adjuntarAlertasEmergentes(String narrativa,
                                                             List<PanelMetricasCreatividad.RadarAlert> alertas) {
        return new RevisionSemanalCreativa(
                programadaPara,
                agendaNarrativa,
                compromisos,
                facilitador,
                resumenAutomatico,
                variacionNarrativa,
                recomendacionesPrioritarias,
                panelReport,
                narrativa,
                alertas
        );
    }
}
