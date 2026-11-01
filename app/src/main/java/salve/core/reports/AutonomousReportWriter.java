package salve.core.reports;

import android.content.Context;
import java.io.File;
import java.nio.file.Files;
import salve.core.ModelResult;
import salve.core.PdfGenerator;
import salve.core.SalveLLM;

/** Local, evidence-first PDF reports from background work. Never publishes data to GitHub. */
public final class AutonomousReportWriter {
    private AutonomousReportWriter() { }

    /** Call from an existing background worker, never from the UI thread. */
    public static boolean save(Context context, String category, String evidence) {
        if (context == null || category == null || evidence == null || evidence.trim().isEmpty()) return false;
        Context app = context.getApplicationContext();
        try {
            File directory = new File(app.getFilesDir(), "salve-improvement-reports");
            if (!directory.exists() && !directory.mkdirs()) return false;
            String observed = evidence.substring(0, Math.min(6000, evidence.length()));
            String analysis = "No se realizó una inferencia del modelo; se conserva únicamente la evidencia.";
            SalveLLM llm = SalveLLM.getInstance(app);
            if (llm.activeModelPath() != null && !llm.isLoading()) {
                String prompt = "Eres Salve. Escribe un informe de investigación o mejora en español. "
                        + "Distingue hechos observados, hipótesis, propuestas, riesgos y pruebas pendientes. "
                        + "No inventes inspecciones de código, fuentes, resultados ni acciones. "
                        + "No incluyas secretos ni datos personales. No ejecutes cambios. "
                        + "Si la evidencia es insuficiente, dilo. Evidencia:\n" + observed;
                ModelResult result = llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
                if (result != null && result.isSuccess() && result.getText() != null
                        && !result.getText().trim().isEmpty())
                    analysis = result.getText();
            }
            String body = "INFORME AUTÓNOMO DE SALVE\nCategoría: " + category
                    + "\nEstado: hipótesis y propuestas sin implementar ni verificar.\n\n"
                    + "ANÁLISIS DE SALVE\n" + analysis
                    + "\n\nEVIDENCIA REGISTRADA\n" + observed;
            File generated = new PdfGenerator(app).crearPdfDesdeTexto(body, "salve-autonomo");
            File destination = new File(directory, "informe-" + System.currentTimeMillis() + ".pdf");
            Files.copy(generated.toPath(), destination.toPath());
            generated.delete();
            return true;
        } catch (Exception failed) {
            android.util.Log.w("Salve/Reports", "No se guardó informe autónomo: "
                    + failed.getClass().getSimpleName());
            return false;
        }
    }
}
