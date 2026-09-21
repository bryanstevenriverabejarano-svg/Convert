package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    private static final int MAX_ISSUES_PER_RUN = 3;
    private final Context context;
    private final CodeAnalyzerEnhanced analyzer;
    private final LLMCoder coder;
    private final MemoriaEmocional memoria;
    private final CreativityManifest manifest;
    private final AutoTestGenerator testGenerator;
    private final ValidationSandbox validationSandbox;
    private final ConsejoEticoCreativo consejoEtico;
    private final MultimodalLearningOrchestrator learningOrchestrator;
    private final AutoImprovementProposalStore proposalStore;

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
        this.proposalStore = new AutoImprovementProposalStore(ctx);
        this.learningOrchestrator = new MultimodalLearningOrchestrator(
                this.memoria,
                new BitacoraExploracionCreativa(this.memoria.getGrafoConocimiento()));
    }

    /**
     * Ejecuta el flujo de auto mejora: analiza el código de Salve, genera
     * parches sugeridos para cada issue de nivel WARNING y guarda estas
     * sugerencias como recuerdos en la memoria emocional. Se utiliza la
     * fuente exacta de una revisión y una lista acotada de clases instaladas.
     */
    public String autoImprove() {
        int detectedIssues = 0;
        int queuedProposals = 0;
        try {
            final AutoImprovementSourceSnapshot snapshot;
            try {
                snapshot = AutoImprovementSourceSnapshot.read(new File(context.getFilesDir(), AutoImprovementSourceSnapshot.RELATIVE_PATH));
            } catch (IOException missingSource) {
                Log.w(TAG, "Auto-mejora no iniciada: falta una instantánea válida del código fuente.");
                return "No pude iniciar la auto-mejora: necesito una instantánea válida del código fuente exportada desde Git. Aún no he generado un parche.";
            }
            List<AnalysisReport> reports = analyzer.analyzeSourceTargets(snapshot.classNames());
            if (reports.isEmpty()) {
                Log.w(TAG, "Auto-mejora sin objetivos: ninguna fuente corresponde a una clase analizable del APK.");
                return "No encontré clases instaladas que pueda analizar entre las fuentes suministradas. Aún no he generado un parche.";
            }
            analysisLoop:
            for (AnalysisReport report : reports) {
                for (MethodIssue issue : report.getIssues()) {
                    if (issue.getLevel() != IssueLevel.WARNING) {
                        continue;
                    }
                    if (detectedIssues == MAX_ISSUES_PER_RUN || Thread.currentThread().isInterrupted()) break analysisLoop;
                    detectedIssues++;
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

                    String diagnosis = describeIssue(issue, report) + "\nFuente: revisión " + snapshot.revision
                            + "; SHA-256 " + snapshot.source(report.getClassName()).sha256
                            + ". La inspección de firmas procede del APK y puede diferir de esta revisión.";
                    String fix = coder.generateFix(describeIssue(issue, report), report.getClassName(), snapshot);
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

                    if (hasFix) {
                        try {
                            AutoImprovementProposalStore.Proposal proposal =
                                    new AutoImprovementProposalStore.Proposal(
                                            System.currentTimeMillis(),
                                            report.getClassName(),
                                            "app/src/main/java/salve/core/" + report.getClassName() + ".java",
                                            diagnosis,
                                            fix,
                                            suite == null ? "" : suite.getCode(),
                                            validation.success,
                                            validation.getExecutionResult().wasAttempted(),
                                            validation.getExecutionResult().wasSuccessful(),
                                            deliberacion.aprobada,
                                            snapshot.revision,
                                            snapshot.source(report.getClassName()).sha256
                                    );
                            proposalStore.enqueue(proposal);
                            queuedProposals++;
                            session.addArtifact(
                                    AutoImprovementSession.Stage.REVIEW,
                                    "Bandeja para PR automático",
                                    proposal.isReadyForPullRequest()
                                            ? "Lista para que el ejecutor externo abra una rama y un PR."
                                            : "Pendiente de validación completa en el ejecutor externo.",
                                    true
                            );
                        } catch (Exception proposalError) {
                            Log.e(TAG, "No se pudo encolar la propuesta de PR", proposalError);
                            session.addArtifact(
                                    AutoImprovementSession.Stage.REVIEW,
                                    "Bandeja para PR automático",
                                    "No se pudo persistir el artefacto para el ejecutor externo.",
                                    false
                            );
                        }
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
            return "La auto-mejora se interrumpió por un error. Propuestas guardadas: " + queuedProposals + ". No se ha modificado el APK.";
        }
        if (Thread.currentThread().isInterrupted()) return "La auto-mejora fue cancelada. Propuestas guardadas: " + queuedProposals + ". No se ha modificado el APK.";
        if (queuedProposals > 0) return "He guardado " + queuedProposals + " propuesta(s) con fuente identificada. El ejecutor externo aún debe aplicar y probar los parches; no se ha modificado el APK.";
        return detectedIssues == 0
                ? "La inspección de firmas no detectó métodos con más de cuatro parámetros en las clases suministradas. Esto no demuestra que el código esté libre de otros problemas."
                : "Detecté " + detectedIssues + " posible(s) problema(s), pero no obtuve un parche utilizable. No se ha modificado el APK.";
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
