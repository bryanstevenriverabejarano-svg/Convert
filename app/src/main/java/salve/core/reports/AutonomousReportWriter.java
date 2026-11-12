package salve.core.reports;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import salve.core.ModelResult;
import salve.core.PdfGenerator;
import salve.core.SalveLLM;

/** Local, evidence-first PDF reports from background work. Never publishes data to GitHub. */
public final class AutonomousReportWriter {
    /** La biblioteca que muestra la pestaña Informes del Registro de ejecución. */
    public static final String DIRECTORY = "salve-improvement-reports";

    private AutonomousReportWriter() { }

    /** Call from an existing background worker, never from the UI thread. */
    public static boolean save(Context context, String category, String evidence) {
        if (context == null || category == null || evidence == null || evidence.trim().isEmpty()) return false;
        Context app = context.getApplicationContext();
        String observed = evidence.substring(0, Math.min(6000, evidence.length()));
        String analysis = "No se realizó una inferencia del modelo; se conserva únicamente la evidencia.";
        try {
            SalveLLM llm = SalveLLM.getInstance(app);
            if (llm.activeModelPath() != null && !llm.isLoading()) {
                String prompt = "Eres Salve. Escribe un informe de investigación o mejora en español. "
                        + "Distingue hechos observados, hipótesis, propuestas, riesgos y pruebas pendientes. "
                        + "No inventes inspecciones de código, fuentes, resultados ni acciones. "
                        + "No incluyas secretos ni datos personales. No ejecutes cambios. "
                        + "Si la evidencia es insuficiente, dilo. "
                        + "Termina con una última línea exacta: «" + salve.core.selfpr.EvolutionSignals.IMPROVEMENT_LINE
                        + " <una mejora concreta para tu propio código que se desprenda de esto, o «ninguna»>». "
                        + "Evidencia:\n" + observed;
                ModelResult result = llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
                if (result != null && result.isSuccess() && result.getText() != null
                        && !result.getText().trim().isEmpty())
                    analysis = result.getText();
            }
        } catch (Exception failed) {
            android.util.Log.w("Salve/Reports", "Informe sin análisis del modelo: "
                    + failed.getClass().getSimpleName());
        }
        String body = "INFORME AUTÓNOMO DE SALVE\nCategoría: " + category
                + "\nEstado: hipótesis y propuestas sin implementar ni verificar.\n\n"
                + "ANÁLISIS DE SALVE\n" + analysis
                + "\n\nEVIDENCIA REGISTRADA\n" + observed;
        return saveText(app, body) != null;
    }

    /**
     * Guarda un texto ya redactado como PDF en la biblioteca de informes, sin otra inferencia.
     * Devuelve el archivo, o null si no se pudo escribir.
     */
    public static File saveText(Context context, String body) {
        if (context == null || body == null || body.trim().isEmpty()) return null;
        Context app = context.getApplicationContext();
        try {
            File directory = new File(app.getFilesDir(), DIRECTORY);
            if (!directory.exists() && !directory.mkdirs()) return null;
            File generated = new PdfGenerator(app).crearPdfDesdeTexto(body, "salve-autonomo");
            File destination = new File(directory, "informe-" + System.currentTimeMillis() + ".pdf");
            Files.copy(generated.toPath(), destination.toPath());
            generated.delete();
            // Su ficha en la biblioteca: título, tipo y resumen (si falla, el PDF sigue ahí).
            ReportLibrary.writeMeta(ReportLibrary.describe(destination, body, System.currentTimeMillis()));
            return destination;
        } catch (Exception failed) {
            android.util.Log.w("Salve/Reports", "No se guardó el informe: "
                    + failed.getClass().getSimpleName());
            return null;
        }
    }
}
