package salve.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Bitácora que almacena eventos relevantes de la exploración creativa durante
 * la fase IV. Permite registrar planificaciones, ejecuciones y síntesis de
 * hallazgos para generar reportes narrativos que mantengan trazabilidad.
 */
public class BitacoraExploracionCreativa {

    public static class EntradaBitacora {
        private final String tipo;
        private final String descripcion;
        private final String aprendizaje;
        private final List<String> etiquetas;
        private final int impactoCreativo;
        private final long timestamp;
        private final List<String> nodosConocimiento;
        private final List<String> misionesInfluyentes;
        private final List<String> hipotesisValidadas;
        private final DeltaMetricas deltaMetricas;

        public EntradaBitacora(String tipo,
                               String descripcion,
                               String aprendizaje,
                               List<String> etiquetas,
                               int impactoCreativo,
                               long timestamp,
                               List<String> nodosConocimiento,
                               List<String> misionesInfluyentes,
                               List<String> hipotesisValidadas,
                               DeltaMetricas deltaMetricas) {
            this.tipo = tipo == null ? "" : tipo.trim();
            this.descripcion = descripcion == null ? "" : descripcion.trim();
            this.aprendizaje = aprendizaje == null ? "" : aprendizaje.trim();
            this.etiquetas = etiquetas == null
                    ? new ArrayList<>()
                    : new ArrayList<>(etiquetas);
            this.impactoCreativo = Math.max(1, Math.min(10, impactoCreativo));
            this.timestamp = timestamp;
            this.nodosConocimiento = nodosConocimiento == null
                    ? new ArrayList<>()
                    : new ArrayList<>(nodosConocimiento);
            this.misionesInfluyentes = misionesInfluyentes == null
                    ? new ArrayList<>()
                    : new ArrayList<>(misionesInfluyentes);
            this.hipotesisValidadas = hipotesisValidadas == null
                    ? new ArrayList<>()
                    : new ArrayList<>(hipotesisValidadas);
            this.deltaMetricas = deltaMetricas;
        }

        public String getTipo() {
            return tipo;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public String getAprendizaje() {
            return aprendizaje;
        }

        public List<String> getEtiquetas() {
            return Collections.unmodifiableList(etiquetas);
        }

        public int getImpactoCreativo() {
            return impactoCreativo;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public List<String> getNodosConocimiento() {
            return Collections.unmodifiableList(nodosConocimiento);
        }

        public List<String> getMisionesInfluyentes() {
            return Collections.unmodifiableList(misionesInfluyentes);
        }

        public List<String> getHipotesisValidadas() {
            return Collections.unmodifiableList(hipotesisValidadas);
        }

        public DeltaMetricas getDeltaMetricas() {
            return deltaMetricas;
        }

        public String enFormatoLinea() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String fecha = format.format(new Date(timestamp));
            return "[" + fecha + "] " + tipo + " → " + descripcion
                    + (aprendizaje.isEmpty() ? "" : " | Aprendizaje: " + aprendizaje)
                    + " | Impacto: " + impactoCreativo
                    + (etiquetas.isEmpty() ? "" : " | Etiquetas: " + String.join(", ", etiquetas))
                    + (nodosConocimiento.isEmpty() ? "" : " | Nodos: " + String.join(", ", nodosConocimiento))
                    + (misionesInfluyentes.isEmpty() ? "" : " | Misiones: " + String.join(", ", misionesInfluyentes))
                    + (hipotesisValidadas.isEmpty() ? "" : " | Hipótesis: " + String.join(", ", hipotesisValidadas))
                    + (deltaMetricas == null ? "" : " | ΔMétricas: " + deltaMetricas.toCompactString());
        }
    }

    public static class DeltaMetricas {
        public final double deltaSatisfaccion;
        public final double deltaDiversidad;
        public final double deltaEstabilidad;
        public final double deltaEtica;

        public DeltaMetricas(double deltaSatisfaccion,
                             double deltaDiversidad,
                             double deltaEstabilidad,
                             double deltaEtica) {
            this.deltaSatisfaccion = deltaSatisfaccion;
            this.deltaDiversidad = deltaDiversidad;
            this.deltaEstabilidad = deltaEstabilidad;
            this.deltaEtica = deltaEtica;
        }

        public static DeltaMetricas fromSnapshots(PanelMetricasCreatividad.CreativityMetricsSnapshot previo,
                                                   PanelMetricasCreatividad.CreativityMetricsSnapshot actual) {
            if (previo == null || actual == null) {
                return null;
            }
            return new DeltaMetricas(
                    actual.satisfaccionPromedio - previo.satisfaccionPromedio,
                    actual.diversidadCreativa - previo.diversidadCreativa,
                    actual.estabilidadAutoMejora - previo.estabilidadAutoMejora,
                    actual.tasaAprobacionEtica - previo.tasaAprobacionEtica);
        }

        public String toCompactString() {
            return String.format(Locale.getDefault(),
                    "ΔSat %.2f | ΔDiv %.1f | ΔEst %.2f | ΔÉtica %.2f",
                    deltaSatisfaccion,
                    deltaDiversidad,
                    deltaEstabilidad,
                    deltaEtica);
        }
    }

    private final List<EntradaBitacora> entradas;
    private final GrafoConocimientoVivo grafo;

    public BitacoraExploracionCreativa() {
        this(null);
    }

    public BitacoraExploracionCreativa(GrafoConocimientoVivo grafo) {
        this.entradas = new ArrayList<>();
        this.grafo = grafo;
    }

    public synchronized void registrarEntrada(String tipo,
                                               String descripcion,
                                               String aprendizaje,
                                               List<String> etiquetas,
                                               int impactoCreativo) {
        EntradaBitacora entrada = new EntradaBitacora(
                tipo,
                descripcion,
                aprendizaje,
                etiquetas,
                impactoCreativo,
                System.currentTimeMillis(),
                null,
                null,
                null,
                null
        );
        entradas.add(entrada);
    }

    public synchronized void registrarEntradaEnlazada(String tipo,
                                                       String descripcion,
                                                       String aprendizaje,
                                                       List<String> etiquetas,
                                                       int impactoCreativo,
                                                       List<String> nodosConocimiento,
                                                       List<String> misionesInfluyentes,
                                                       List<String> hipotesis,
                                                       DeltaMetricas deltaMetricas) {
        EntradaBitacora entrada = new EntradaBitacora(
                tipo,
                descripcion,
                aprendizaje,
                etiquetas,
                impactoCreativo,
                System.currentTimeMillis(),
                nodosConocimiento,
                misionesInfluyentes,
                hipotesis,
                deltaMetricas
        );
        entradas.add(entrada);
        vincularConGrafo(entrada);
    }

    public synchronized List<EntradaBitacora> obtenerUltimas(int max) {
        if (entradas.isEmpty()) {
            return Collections.emptyList();
        }
        int limite = Math.max(1, max);
        int desde = Math.max(0, entradas.size() - limite);
        return new ArrayList<>(entradas.subList(desde, entradas.size()));
    }

    public synchronized int calcularImpactoPromedio() {
        if (entradas.isEmpty()) {
            return 0;
        }
        int suma = 0;
        for (EntradaBitacora entrada : entradas) {
            suma += entrada.getImpactoCreativo();
        }
        return Math.round((float) suma / entradas.size());
    }

    public synchronized String construirContextoNarrativo(int maxEventos) {
        List<EntradaBitacora> relevantes = obtenerUltimas(maxEventos);
        if (relevantes.isEmpty()) {
            return "No se registraron eventos recientes en la bitácora.";
        }
        StringBuilder builder = new StringBuilder();
        for (EntradaBitacora entrada : relevantes) {
            builder.append(entrada.enFormatoLinea()).append('\n');
        }
        return builder.toString();
    }

    public synchronized String generarMapaImpacto() {
        if (entradas.isEmpty()) {
            return "Sin experimentos registrados.";
        }
        java.util.Map<String, Integer> conteoPorTipo = new java.util.LinkedHashMap<>();
        double impactoAcumulado = 0.0;
        for (EntradaBitacora entrada : entradas) {
            conteoPorTipo.merge(entrada.getTipo(), 1, Integer::sum);
            impactoAcumulado += entrada.getImpactoCreativo();
        }
        double promedio = impactoAcumulado / Math.max(1, entradas.size());
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.getDefault(),
                "Eventos registrados: %d | Impacto promedio: %.2f",
                entradas.size(),
                promedio));
        builder.append('\n');
        builder.append("Distribución por tipo:");
        for (java.util.Map.Entry<String, Integer> entry : conteoPorTipo.entrySet()) {
            builder.append('\n')
                    .append("- ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" eventos");
        }
        builder.append('\n');
        builder.append("Últimos aprendizajes:");
        List<EntradaBitacora> ultimas = obtenerUltimas(3);
        int idx = 1;
        for (EntradaBitacora entrada : ultimas) {
            builder.append('\n')
                    .append(idx++)
                    .append(") ")
                    .append(entrada.getDescripcion())
                    .append(" → ")
                    .append(entrada.getAprendizaje());
        }
        return builder.toString();
    }

    public synchronized int totalEventos() {
        return entradas.size();
    }

    public synchronized String generarPronosticosImpacto() {
        if (entradas.size() < 2) {
            return "Aún no hay suficientes experimentos para proyectar pronósticos.";
        }
        double impactoPromedio = calcularImpactoPromedio();
        EntradaBitacora ultimo = entradas.get(entradas.size() - 1);
        double tendencia = ultimo.getImpactoCreativo() - impactoPromedio;
        String direccion = tendencia >= 0 ? "al alza" : "a la baja";
        java.util.Map<String, Integer> frecuenciaPorTipo = new java.util.LinkedHashMap<>();
        for (EntradaBitacora entrada : entradas) {
            frecuenciaPorTipo.merge(entrada.getTipo(), 1, Integer::sum);
        }
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(Locale.getDefault(),
                "Impacto promedio %.2f con tendencia %s (último evento %s).",
                impactoPromedio,
                direccion,
                ultimo.getTipo()));
        builder.append("\nFocos recomendados:");
        int idx = 1;
        for (java.util.Map.Entry<String, Integer> entry : frecuenciaPorTipo.entrySet()) {
            builder.append('\n')
                    .append(idx++)
                    .append(") ")
                    .append(entry.getKey())
                    .append(" → participación ")
                    .append(entry.getValue())
                    .append(" eventos");
        }
        return builder.toString();
    }

    public synchronized String contextualizarAlertas(List<PanelMetricasCreatividad.RadarAlert> alertas) {
        if (alertas == null || alertas.isEmpty()) {
            return "El radar no registró alertas experimentales esta semana.";
        }
        StringBuilder builder = new StringBuilder();
        for (PanelMetricasCreatividad.RadarAlert alerta : alertas) {
            builder.append("Alerta ").append(alerta.getDimension()).append(" → ")
                    .append(alerta.getSeverity()).append('\n');
            List<EntradaBitacora> relacionados = filtrarEntradasRelacionadas(alerta);
            if (relacionados.isEmpty()) {
                builder.append("  · Sin experimentos directamente vinculados en la bitácora.\n");
            } else {
                int idx = 1;
                for (EntradaBitacora entrada : relacionados) {
                    builder.append("  ").append(idx++).append(") ")
                            .append(entrada.enFormatoLinea()).append('\n');
                }
            }
        }
        return builder.toString().trim();
    }

    private void vincularConGrafo(EntradaBitacora entrada) {
        if (grafo == null || entrada == null) {
            return;
        }
        try {
            grafo.registrarResultadoExperimento(
                    entrada.getDescripcion(),
                    entrada.getNodosConocimiento(),
                    entrada.getMisionesInfluyentes(),
                    entrada.getHipotesisValidadas(),
                    entrada.getDeltaMetricas());
        } catch (Exception ignore) {
            // Evitar que fallas de grafo interrumpan el registro de la bitácora
        }
    }

    private List<EntradaBitacora> filtrarEntradasRelacionadas(PanelMetricasCreatividad.RadarAlert alerta) {
        List<EntradaBitacora> candidatos = new ArrayList<>();
        if (alerta == null) {
            return candidatos;
        }
        for (int i = entradas.size() - 1; i >= 0; i--) {
            EntradaBitacora entrada = entradas.get(i);
            if (coincideConAlerta(entrada, alerta)) {
                candidatos.add(entrada);
            }
            if (candidatos.size() >= 3) {
                break;
            }
        }
        if (candidatos.isEmpty()) {
            candidatos.addAll(obtenerUltimas(2));
        }
        candidatos.sort(Comparator.comparingLong(EntradaBitacora::getTimestamp).reversed());
        return candidatos.size() > 3 ? new ArrayList<>(candidatos.subList(0, 3)) : candidatos;
    }

    private boolean coincideConAlerta(EntradaBitacora entrada,
                                      PanelMetricasCreatividad.RadarAlert alerta) {
        if (entrada == null || alerta == null) {
            return false;
        }
        String dimension = alerta.getDimension();
        DeltaMetricas delta = entrada.getDeltaMetricas();
        if ("diversidad_creativa".equals(dimension)) {
            return delta != null && delta.deltaDiversidad < 0;
        }
        if ("satisfaccion_creativa".equals(dimension)) {
            return delta != null && delta.deltaSatisfaccion < 0;
        }
        if ("aprobacion_etica".equals(dimension)) {
            return delta != null && delta.deltaEtica < 0;
        }
        if ("aprobacion_humana".equals(dimension)) {
            return entrada.getEtiquetas().contains("revision")
                    || entrada.getEtiquetas().contains("revision_semana")
                    || !entrada.getMisionesInfluyentes().isEmpty();
        }
        if ("precision_multimodal".equals(dimension)) {
            return entrada.getEtiquetas().contains("multimodal")
                    || entrada.getEtiquetas().contains("curaduria")
                    || entrada.getEtiquetas().contains("señal");
        }
        return false;
    }
}
