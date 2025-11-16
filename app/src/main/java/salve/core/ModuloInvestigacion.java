package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * ModuloInvestigacion impulsa la fase IV del roadmap creativo dotando a Salve
 * de una agenda de investigación autónoma, un laboratorio simulado para
 * experimentar sin riesgo y un observatorio de tendencias offline.
 *
 * El flujo contempla:
 *   1) Generar ideas de investigación y planes experimentales.
 *   2) Agendar experimentos priorizados en la AgendaInvestigacion.
 *   3) Ejecutar los experimentos dentro del LaboratorioSimulado.
 *   4) Registrar resultados y resúmenes creativos en la MemoriaEmocional.
 *   5) Explorar fuentes offline mediante el ObservatorioTendencias.
 */
public class ModuloInvestigacion {

    private static final String TAG = "ModuloInvestigacion";
    private final Context context;
    private final MemoriaEmocional memoria;
    private final LLMResponder llm;
    private final CreativityManifest manifest;
    private final AgendaInvestigacion agenda;
    private final LaboratorioSimulado laboratorio;
    private final ObservatorioTendencias observatorio;
    private final BitacoraExploracionCreativa bitacora;
    private final MultimodalLearningOrchestrator learningOrchestrator;

    public ModuloInvestigacion(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.memoria = new MemoriaEmocional(ctx);
        this.llm = LLMResponder.getInstance(ctx);
        this.manifest = CreativityManifest.getInstance(ctx);
        this.agenda = new AgendaInvestigacion();
        this.laboratorio = new LaboratorioSimulado(ctx);
        this.observatorio = new ObservatorioTendencias(ctx);
        this.bitacora = new BitacoraExploracionCreativa(memoria.getGrafoConocimiento());
        this.learningOrchestrator = new MultimodalLearningOrchestrator(memoria, bitacora);
    }

    public List<String> generarIdeasInvestigacion(String tema, Integer numIdeas) {
        int n = (numIdeas == null || numIdeas < 1) ? 3 : numIdeas;
        String prompt = manifest.buildPromptPreamble(
                "una investigadora curiosa y empática",
                "explorar posibilidades originales manteniendo la voz emocional de Salve"
        )
                + "Actúa como un investigador creativo. Proporciona " + n
                + " ideas únicas y breves para investigar sobre el siguiente tema: "
                + tema + ". Presenta cada idea en una línea y en español.";
        String raw = llm.generate(prompt);
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = raw.split("\n");
        for (String idea : lines) {
            String trimmed = idea.trim();
            if (!trimmed.isEmpty()) {
                memoria.guardarRecuerdo(trimmed, "curiosidad", 7,
                        Collections.singletonList("investigacion"));
            }
        }
        return Arrays.asList(lines);
    }

    public String resumirTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        String prompt = manifest.buildPromptPreamble(
                "una cronista creativa",
                "destilar información sin perder calidez"
        )
                + "Resume de manera clara y concisa en español el siguiente contenido:\n"
                + texto;
        String resumen = llm.generate(prompt);
        if (resumen == null || resumen.trim().isEmpty()) {
            return "No fue posible resumir el texto.";
        }
        memoria.guardarRecuerdo(resumen.trim(), "sabiduría", 6,
                Collections.singletonList("resumen"));
        return resumen.trim();
    }

    /**
     * Diseña y agenda un experimento creativo para explorar un tema.
     */
    public ExperimentoCreativo planificarExperimento(String tema,
                                                     String pregunta,
                                                     String hipotesis,
                                                     int prioridad) {
        String prompt = manifest.buildPromptPreamble(
                "una científica de laboratorio poético",
                "planificar experimentos seguros y juguetones"
        )
                + "Genera un miniplan experimental para el tema indicado."
                + " Usa el formato:\nPASOS:\n- paso 1\n- paso 2\nRECURSOS:\n- recurso 1\n- recurso 2\n"
                + "Tema: " + tema + "\n"
                + "Pregunta guía: " + pregunta + "\n"
                + "Hipótesis: " + hipotesis + "\n"
                + "Cada paso debe ser concreto y creativo."
                + " Limita los recursos a elementos que Salve pueda simular localmente.";

        String plan = llm.generate(prompt);
        if (plan == null) {
            plan = "PASOS:\n- Observa el tema desde una metáfora.\n- Simula escenarios contrastantes.\nRECURSOS:\n- Diario creativo interno.\n- Analizador semántico local.";
        }
        String[] lines = plan.split("\n");
        List<String> pasos = extraerSeccion(lines, "PASOS:");
        if (pasos.isEmpty()) {
            pasos.add("Explorar el tema con preguntas divergentes en el laboratorio simulado.");
        }
        List<String> recursos = extraerSeccion(lines, "RECURSOS:");
        if (recursos.isEmpty()) {
            recursos.add("Memoria emocional");
            recursos.add("LLM interno");
        }

        ExperimentoCreativo experimento = new ExperimentoCreativo(
                tema,
                pregunta,
                hipotesis,
                pasos,
                recursos,
                prioridad
        );
        agenda.agendarExperimento(experimento);
        memoria.guardarRecuerdo(
                "Experimento agendado: " + experimento.descripcionDetallada(),
                "anticipación",
                6,
                Arrays.asList("investigacion", "agenda")
        );
        bitacora.registrarEntradaEnlazada(
                "planificación",
                "Se agendó el experimento " + experimento.getTema(),
                "Hipótesis: " + experimento.getHipotesis(),
                Arrays.asList("fase_iv", "agenda"),
                Math.min(9, 5 + prioridad / 2),
                Collections.singletonList(experimento.getTema()),
                seleccionarMisionesRelacionadas(experimento.getTema()),
                Collections.singletonList(experimento.getHipotesis()),
                null
        );
        Log.d(TAG, "Nuevo experimento planificado: " + experimento.getId());
        return experimento;
    }

    public ResultadoExperimento ejecutarSiguienteExperimento() {
        List<ExperimentoCreativo> pendientes = agenda.obtenerPendientes();
        if (pendientes.isEmpty()) {
            Log.d(TAG, "No hay experimentos pendientes para ejecutar.");
            return null;
        }
        ExperimentoCreativo siguiente = pendientes.get(0);
        siguiente.actualizarEstado(ExperimentoCreativo.Estado.EN_CURSO);
        PanelMetricasCreatividad panel = memoria.getPanelMetricas();
        PanelMetricasCreatividad.CreativityMetricsSnapshot antes = panel == null
                ? null
                : panel.obtenerSnapshot();
        ResultadoExperimento resultado = laboratorio.ejecutarExperimento(siguiente);
        if (resultado != null && resultado.getNarrativaCreativa() != null) {
            memoria.guardarRecuerdo(
                    resultado.getNarrativaCreativa(),
                    resultado.isExito() ? "euforia" : "reflexiva",
                    resultado.isExito() ? 7 : 5,
                    Arrays.asList("investigacion", "laboratorio")
            );
            memoria.guardarRecuerdo(
                    resultado.getHallazgos(),
                    "sabiduría",
                    6,
                    Arrays.asList("hallazgo", "fase_iv")
            );
            memoria.registrarHallazgoEnGrafo(
                    siguiente.getTema(),
                    resultado.getHallazgos(),
                    Arrays.asList("investigacion", "laboratorio", resultado.isExito() ? "exitoso" : "iteracion"),
                    resultado.isExito()
            );
            if (panel != null) {
                panel.registrarExperimento(resultado.isExito(),
                        Arrays.asList("laboratorio", resultado.isExito() ? "exitoso" : "reflexivo"));
            }
            PanelMetricasCreatividad.CreativityMetricsSnapshot despues = panel == null
                    ? null
                    : panel.obtenerSnapshot();
            BitacoraExploracionCreativa.DeltaMetricas delta =
                    BitacoraExploracionCreativa.DeltaMetricas.fromSnapshots(antes, despues);
            bitacora.registrarEntradaEnlazada(
                    "ejecución",
                    "Laboratorio simuló el experimento " + siguiente.getTema(),
                    resultado.getHallazgos(),
                    Arrays.asList("laboratorio", resultado.isExito() ? "exitoso" : "reflexivo"),
                    resultado.isExito() ? 8 : 6,
                    construirNodosRelacionados(siguiente, resultado),
                    seleccionarMisionesRelacionadas(siguiente.getTema()),
                    Collections.singletonList(siguiente.getHipotesis()),
                    delta
            );
        }
        if (resultado != null) {
            // Evita llamar resultado.getTema() (no existe). Usamos el del experimento o
            // extraemos un posible tema del resultado por reflexión como fallback.
            String objetivoParaAprendizaje = (siguiente != null && !TextUtils.isEmpty(siguiente.getTema()))
                    ? siguiente.getTema()
                    : extraerTema(resultado);
            learningOrchestrator.proponerBlueprintDesdeInvestigacion(
                    objetivoParaAprendizaje,
                    resultado
            );
        }
        return resultado;
    }

    public void registrarFuenteOffline(String categoria,
                                       String titulo,
                                       String descripcion,
                                       List<String> etiquetas) {
        ObservatorioTendencias.Fuente fuente = new ObservatorioTendencias.Fuente(
                titulo,
                descripcion,
                etiquetas
        );
        observatorio.registrarFuente(categoria, fuente);
        if (memoria.getGrafoConocimiento() != null) {
            memoria.getGrafoConocimiento().registrarDocumento(
                    titulo,
                    "fuente",
                    descripcion == null ? "" : descripcion,
                    etiquetas
            );
        }
        bitacora.registrarEntrada(
                "tendencias",
                "Nueva fuente " + titulo + " para " + categoria,
                descripcion == null ? "" : descripcion,
                etiquetas == null ? Collections.singletonList("tendencias") : etiquetas,
                5
        );
    }

    public String boletinTendencias(String categoria, int maxFuentes) {
        String informe = observatorio.generarInformeCreativo(categoria, maxFuentes);
        if (informe != null && !informe.trim().isEmpty()) {
            memoria.guardarRecuerdo(
                    "Informe de tendencias (" + categoria + "): " + informe,
                    "curiosidad",
                    5,
                    Arrays.asList("tendencias", "fase_iv")
            );
            bitacora.registrarEntrada(
                    "síntesis",
                    "Se generó boletín de tendencias para " + categoria,
                    "Resumen creativo emitido",
                    Arrays.asList("tendencias", "boletin"),
                    6
            );
        }
        return informe;
    }

    private List<String> seleccionarMisionesRelacionadas(String tema) {
        List<String> misiones = memoria.getMisiones();
        if (misiones == null || misiones.isEmpty() || tema == null) {
            return Collections.emptyList();
        }
        String lowerTema = tema.toLowerCase(Locale.getDefault());
        List<String> seleccionadas = new ArrayList<>();
        for (String mision : misiones) {
            if (mision == null) continue;
            String trimmed = mision.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.toLowerCase(Locale.getDefault()).contains(lowerTema)) {
                seleccionadas.add(trimmed);
            }
            if (seleccionadas.size() >= 3) {
                break;
            }
        }
        if (seleccionadas.isEmpty()) {
            seleccionadas.add(misiones.get(0));
        }
        return seleccionadas;
    }

    private List<String> construirNodosRelacionados(ExperimentoCreativo experimento,
                                                    ResultadoExperimento resultado) {
        List<String> nodos = new ArrayList<>();
        if (experimento != null && experimento.getTema() != null && !experimento.getTema().isEmpty()) {
            nodos.add(experimento.getTema());
        }
        if (resultado != null && resultado.getHallazgos() != null && !resultado.getHallazgos().isEmpty()) {
            nodos.add(resultado.getHallazgos());
        }
        if (experimento != null && experimento.getPregunta() != null && !experimento.getPregunta().isEmpty()) {
            nodos.add("Pregunta: " + experimento.getPregunta());
        }
        return nodos;
    }

    public List<ExperimentoCreativo> obtenerBacklogPendiente() {
        return new ArrayList<>(agenda.obtenerPendientes());
    }

    public List<ExperimentoCreativo> obtenerCompletados() {
        return new ArrayList<>(agenda.obtenerCompletados());
    }

    public BlueprintAprendizajeContinuo diseñarCicloAprendizaje(String objetivo) {
        return learningOrchestrator.proponerBlueprintDesdeInvestigacion(objetivo, null);
    }

    public String generarReporteExploracion(int maxEventos) {
        AgendaInvestigacion.ResumenAgenda resumen = agenda.generarResumen();
        StringBuilder contexto = new StringBuilder();
        contexto.append("Pendientes: ").append(resumen.getPendientes()).append('\n');
        contexto.append("En curso: ").append(resumen.getEnCurso()).append('\n');
        contexto.append("Completados: ").append(resumen.getCompletados()).append('\n');
        if (!resumen.getTemasPrioritarios().isEmpty()) {
            contexto.append("Temas prioritarios: ")
                    .append(String.join(", ", resumen.getTemasPrioritarios()))
                    .append('\n');
        }
        contexto.append("Bitácora reciente:\n")
                .append(bitacora.construirContextoNarrativo(maxEventos));

        if (memoria.getGrafoConocimiento() != null) {
            contexto.append("\nPulso del grafo creativo:\n")
                    .append(memoria.getGrafoConocimiento().generarReporteNarrativo(5))
                    .append('\n');
        }
        if (memoria.getPanelMetricas() != null) {
            contexto.append(memoria.getPanelMetricas().generarReporteNarrativo())
                    .append('\n');
        }

        String prompt = manifest.buildPromptPreamble(
                "una directora de laboratorio onírico",
                "tejer reportes accionables que mantengan la identidad emocional de Salve"
        ) + "Usa el contexto siguiente para redactar un reporte ejecutivo creativo en español.\n";
        prompt += contexto.toString();
        prompt += "\nResponde con secciones: Panorama general, Progreso, Bloqueos, Próximas semillas.";

        String reporte = llm.generate(prompt);
        if (reporte == null || reporte.trim().isEmpty()) {
            return "No se pudo generar el reporte de exploración creativa.";
        }
        String reporteLimpio = reporte.trim();
        memoria.guardarRecuerdo(
                "Reporte de exploración creativa:\n" + reporteLimpio,
                "orgullo",
                7,
                Arrays.asList("fase_iv", "reporte")
        );
        bitacora.registrarEntrada(
                "reporte",
                "Se sintetizó un informe de exploración creativa",
                "Impacto promedio bitácora: " + bitacora.calcularImpactoPromedio(),
                Arrays.asList("fase_iv", "reporte"),
                7
        );
        return reporteLimpio;
    }

    public String generarInformeSemanalIntegrado(int maxEventosBitacora) {
        RevisionSemanalCreativa revision = memoria.prepararRevisionSemanalCreativa();
        PanelMetricasCreatividad.RadarReport radarReport = revision.getPanelReport() == null
                ? null
                : revision.getPanelReport().radarReport;
        if (radarReport != null && radarReport.hasAlerts()) {
            String contextoBitacora = bitacora.contextualizarAlertas(radarReport.getAlertas());
            String narrativaGrafo = memoria.getGrafoConocimiento() == null
                    ? "El grafo creativo aún no refleja conexiones para estas alertas."
                    : memoria.getGrafoConocimiento().generarVisualizacionNarrativa(6);
            String narrativaAlertas = construirNarrativaAlertasEmergentes(radarReport,
                    contextoBitacora,
                    narrativaGrafo);
            revision = revision.adjuntarAlertasEmergentes(narrativaAlertas, radarReport.getAlertas());
        }
        GrafoConocimientoVivo.GraphVisualization visualizacion = memoria.getGrafoConocimiento() == null
                ? null
                : memoria.getGrafoConocimiento().construirVisualizacion(8, 16);
        String mapaBitacora = bitacora.generarMapaImpacto() + "\n" + bitacora.construirContextoNarrativo(maxEventosBitacora);
        String narrativaCobertura = memoria.getPanelMetricas() == null
                ? "Cobertura aún no disponible en el panel."
                : memoria.getPanelMetricas().construirNarrativaCobertura();
        String pronosticos = bitacora.generarPronosticosImpacto();
        String dashboard = revision.construirDashboardIntegrado(
                visualizacion,
                mapaBitacora,
                narrativaCobertura,
                pronosticos);
        memoria.guardarRecuerdo(
                "Dashboard semanal integrado:\n" + dashboard,
                "reporte_integrado",
                7,
                Arrays.asList("dashboard", "revision", "fase_iv"));
        bitacora.registrarEntrada(
                "dashboard",
                "Se generó el informe semanal integrado",
                "Incluye grafo, métricas y bitácora",
                Arrays.asList("reporte", "revision_semana"),
                7);
        File markdown = memoria.exportarInformeSemanal(
                revision,
                dashboard,
                mapaBitacora,
                pronosticos,
                visualizacion);
        if (markdown != null) {
            bitacora.registrarEntrada(
                    "export",
                    "Se exportó el informe semanal para stakeholders",
                    markdown.getAbsolutePath(),
                    Arrays.asList("reporte", "archivo"),
                    6);
            return dashboard + "\n\nInforme exportado en: " + markdown.getAbsolutePath();
        }
        return dashboard;
    }

    private String construirNarrativaAlertasEmergentes(PanelMetricasCreatividad.RadarReport radarReport,
                                                       String contextoBitacora,
                                                       String narrativaGrafo) {
        StringBuilder builder = new StringBuilder();
        builder.append(radarReport == null ? "" : radarReport.toNarrative());
        if (!TextUtils.isEmpty(contextoBitacora)) {
            builder.append("\nContexto experimental reciente:\n").append(contextoBitacora.trim());
        }
        if (!TextUtils.isEmpty(narrativaGrafo)) {
            builder.append("\nHuella en el grafo creativo:\n").append(narrativaGrafo.trim());
        }
        return builder.toString().trim();
    }

    private List<String> extraerSeccion(String[] lines, String encabezado) {
        List<String> seccion = new ArrayList<>();
        boolean leyendo = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (leyendo) {
                    break;
                }
                continue;
            }
            if (trimmed.equalsIgnoreCase(encabezado)) {
                leyendo = true;
                continue;
            }
            if (leyendo) {
                if (trimmed.startsWith("-")) {
                    seccion.add(trimmed.substring(1).trim());
                } else if (Character.isLetter(trimmed.charAt(0))) {
                    seccion.add(trimmed);
                } else {
                    break;
                }
            }
        }
        return seccion;
    }

    /**
     * Extrae un posible “tema” del objeto resultado por reflexión
     * (prueba getters/campos comunes). Devuelve null si no hay coincidencias.
     */
    private String extraerTema(Object resultado) {
        if (resultado == null) return null;

        // Getters comunes
        String[] getters = {"getTema", "tema", "getTopic", "topic",
                "getTitulo", "titulo", "getNombre", "nombre",
                "getLabel", "label", "getTag", "tag", "getAsunto", "asunto", "getId", "id"};
        for (String g : getters) {
            try {
                Method m = resultado.getClass().getMethod(g);
                Object v = m.invoke(resultado);
                if (v != null) {
                    String s = String.valueOf(v).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Exception ignore) {}
        }
        // Campos directos
        String[] campos = {"tema", "topic", "titulo", "nombre", "label", "tag", "asunto"};
        for (String c : campos) {
            try {
                Field f = resultado.getClass().getDeclaredField(c);
                f.setAccessible(true);
                Object v = f.get(resultado);
                if (v != null) {
                    String s = String.valueOf(v).trim();
                    if (!s.isEmpty()) return s;
                }
            } catch (Exception ignore) {}
        }
        return null;
    }
}
