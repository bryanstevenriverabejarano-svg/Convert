package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * AutoImprovementManager coordina la generación de mejoras de código
 * utilizando análisis estático y un generador de código basado en LLM.
 * Su objetivo es identificar problemas en el propio código de Salve
 * (por ejemplo, métodos con demasiados parámetros) y crear
 * sugerencias de parches que se almacenan en la memoria emocional para
 * revisión posterior. Este flujo provee un primer paso hacia la
 * auto‑modificación supervisada.
 */
public class AutoImprovementManager {

    private static final String TAG = "AutoImproveMgr";
    private final Context context;
    private final CodeAnalyzerEnhanced analyzer;
    private final LLMCoder coder;
    private final MemoriaEmocional memoria;
    private final CreativityManifest manifest;
    private final AutoTestGenerator testGenerator;
    private final ValidationSandbox validationSandbox;
    private final ConsejoEticoCreativo consejoEtico;
    private final MultimodalLearningOrchestrator learningOrchestrator;

    /**
     * Construye un gestor de auto mejora.
     * @param ctx contexto de la aplicación
     */
    public AutoImprovementManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.analyzer = new CodeAnalyzerEnhanced(ctx);
        this.coder = LLMCoder.getInstance(ctx);
        this.memoria = new MemoriaEmocional(ctx);
        this.manifest = CreativityManifest.getInstance(ctx);
        this.testGenerator = new AutoTestGenerator(ctx);
        this.validationSandbox = new ValidationSandbox(ctx, GradleSandboxTestExecutor.createDefault(ctx));
        this.consejoEtico = new ConsejoEticoCreativo(ctx);
        this.learningOrchestrator = new MultimodalLearningOrchestrator(
                this.memoria,
                new BitacoraExploracionCreativa(this.memoria.getGrafoConocimiento()));
    }

    /**
     * Ejecuta el flujo de auto mejora: analiza el código de Salve, genera
     * parches sugeridos para cada issue de nivel WARNING y guarda estas
     * sugerencias como recuerdos en la memoria emocional. Se utiliza un
     * timeout de 5 segundos para el análisis.
     */
    public void autoImprove() {
        try {
            List<AnalysisReport> reports = analyzer.analyzeWithTimeout(5, TimeUnit.SECONDS);
            for (AnalysisReport report : reports) {
                for (MethodIssue issue : report.getIssues()) {
                    if (issue.getLevel() != IssueLevel.WARNING) {
                        continue;
                    }
                    AutoImprovementSession session = new AutoImprovementSession(
                            issue.getSuggestion() + " en " + report.getClassName());
                    session.addArtifact(
                            AutoImprovementSession.Stage.ANALYSIS,
                            "Diagnóstico creativo",
                            describeIssue(issue, report),
                            true
                    );

                    String designGuidance = manifest.craftDesignGuidance(issue.getSuggestion());
                    session.addArtifact(
                            AutoImprovementSession.Stage.DESIGN,
                            "Diseño previo",
                            designGuidance,
                            true
                    );

                    String fix = coder.generateFix(issue.getSuggestion(), report.getClassName());
                    boolean hasFix = isActionableFix(fix);
                    session.addArtifact(
                            AutoImprovementSession.Stage.GENERATION,
                            "Patch propuesto",
                            fix,
                            hasFix
                    );

                    AutoTestGenerator.GeneratedTestSuite suite =
                            testGenerator.generateSuite(issue, report.getClassName(), fix);
                    session.addArtifact(
                            AutoImprovementSession.Stage.TEST_GENERATION,
                            "Suite de pruebas creativas",
                            suite.toNarrative(),
                            suite.isActionable()
                    );

                    ValidationSandbox.ValidationReport validation =
                            validationSandbox.execute(hasFix, suite, report.getClassName());
                    session.addArtifact(
                            AutoImprovementSession.Stage.VALIDATION,
                            "Validación automática",
                            validation.toNarrative(),
                            validation.success
                    );

                    if (validation.hasRuntimeExecution()) {
                        session.addArtifact(
                                AutoImprovementSession.Stage.TEST_EXECUTION,
                                "Ejecución en sandbox",
                                validation.getExecutionResult().toNarrative(),
                                validation.getExecutionResult().wasSuccessful()
                        );
                    }

                    PanelMetricasCreatividad.RadarReport radarReport = null;
                    if (memoria.getPanelMetricas() != null) {
                        radarReport = memoria.getPanelMetricas().evaluarDerivaCreativa();
                        if (radarReport == null) {
                            radarReport = PanelMetricasCreatividad.RadarReport.sinDatos();
                        }
                        session.addArtifact(
                                AutoImprovementSession.Stage.RADAR_MONITORING,
                                "Radar de deriva creativa",
                                radarReport.toNarrative(),
                                !radarReport.hasCriticalAlerts()
                        );
                    }

                    ConsejoEticoCreativo.Deliberacion deliberacion = consejoEtico.deliberar(
                            describeIssue(issue, report),
                            fix,
                            validation,
                            suite,
                            radarReport
                    );
                    session.addArtifact(
                            AutoImprovementSession.Stage.ETHICAL_REVIEW,
                            "Consejo ético creativo",
                            deliberacion.toNarrative(),
                            deliberacion.aprobada
                    );

                    String review = buildReviewSummary(session, suite, validation, deliberacion, radarReport);
                    session.addArtifact(
                            AutoImprovementSession.Stage.REVIEW,
                            "Resumen para humanos",
                            review,
                            true
                    );

                    BlueprintAprendizajeContinuo blueprint = learningOrchestrator
                            .proponerBlueprintDesdeAutoMejora(report.getClassName(), validation, radarReport);
                    if (blueprint != null) {
                        session.addArtifact(
                                AutoImprovementSession.Stage.COGNITIVE_EXPANSION,
                                "Blueprint de aprendizaje continuo",
                                blueprint.toNarrativa(),
                                true
                        );
                    }

                    memoria.guardarRecuerdo(
                            session.toNarrative(manifest),
                            "auto_mejora",
                            hasFix ? 6 : 3,
                            Arrays.asList("auto_codigo", "pipeline_auto")
                    );
                    if (memoria.getPanelMetricas() != null) {
                        memoria.getPanelMetricas().registrarCicloAutoMejora(validation.success);
                        boolean sandboxEjecutado = validation.getExecutionResult().wasAttempted();
                        boolean sandboxExitoso = validation.getExecutionResult().wasSuccessful();
                        memoria.getPanelMetricas().registrarValidacionAutomatica(
                                sandboxEjecutado,
                                sandboxEjecutado && sandboxExitoso
                        );
                        memoria.getPanelMetricas().registrarRevisionEtica(deliberacion.aprobada);
                    }
                    Log.d(TAG, "Auto-mejora creativa registrada para " + report.getClassName());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en autoImprove", e);
        }
    }

    private String describeIssue(MethodIssue issue, AnalysisReport report) {
        return "Clase analizada: " + report.getClassName() + '\n'
                + "Método: " + issue.getMethodName() + '\n'
                + "Nivel: " + issue.getLevel() + '\n'
                + "Detalle: " + issue.getDescription();
    }

    private boolean isActionableFix(String fix) {
        if (fix == null) {
            return false;
        }
        String trimmed = fix.trim();
        return !trimmed.isEmpty() && !trimmed.startsWith("// No se pudo");
    }

    private String buildReviewSummary(AutoImprovementSession session,
                                      AutoTestGenerator.GeneratedTestSuite suite,
                                      ValidationSandbox.ValidationReport validation,
                                      ConsejoEticoCreativo.Deliberacion deliberacion,
                                      PanelMetricasCreatividad.RadarReport radarReport) {
        StringBuilder builder = new StringBuilder();
        builder.append("Checklist final: ");
        builder.append(validation.success && validation.getExecutionResult().wasSuccessful()
                ? "sin bloqueos"
                : "requiere ayuda humana");
        builder.append(". Recordar compartir el parche con el círculo creativo para validación conjunta.");
        if (session.hasFailures()) {
            builder.append(" Algunas etapas necesitan refuerzo.");
        }
        if (suite == null || !suite.isActionable()) {
            builder.append(" La generación de pruebas requiere apoyo humano.");
        }
        builder.append("\nEstado de validación: ")
                .append(validation.message);
        if (validation.hasRuntimeExecution()) {
            builder.append("\nSandbox: ")
                    .append(validation.getExecutionResult().getSummary());
        }
        if (radarReport != null) {
            builder.append("\nRadar creativo: ").append(radarReport.getResumen());
            if (radarReport.hasCriticalAlerts()) {
                builder.append(" → se requiere revisión humana reforzada antes del despliegue.");
            }
        }
        builder.append("\nConsejo ético: ")
                .append(deliberacion.aprobada ? "sin objeciones" : "revisar recomendaciones");
        builder.append('\n');
        builder.append(manifest.toChecklistNarrative());
        return builder.toString();
    }
}
