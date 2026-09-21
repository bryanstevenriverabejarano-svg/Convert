package salve.core;

import android.content.Context;
import android.util.Log;
import salve.work.ModelDownloadWorker;

/** Uses the same pinned, verified catalog and unique worker as the user interface. */
public class BuscadorDescargadorModelos {
    public enum Status { REQUESTED, BLOCKED, ERROR }
    public static final class Result {
        public final Status status;
        public final String message;
        private Result(Status status, String message) { this.status = status; this.message = message; }
    }
    private final Runnable enqueue;
    public BuscadorDescargadorModelos(Context context) {
        Context application = context.getApplicationContext();
        this.enqueue = () -> ModelDownloadWorker.enqueue(application);
    }
    BuscadorDescargadorModelos(Runnable enqueue) { this.enqueue = enqueue; }

    public static boolean esSolicitudExplicita(String input) {
        if (input == null) return false;
        String value = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(java.util.Locale.ROOT).trim()
                .replaceFirst("^salve[, ]+", "").replaceAll("^[¿¡]+|[.!?]+$", "").trim();
        return value.matches("(?:descarga|descargar) (?:lo que necesites|un llm|un modelo|el modelo|gemma 4|gemma 4 e2b)")
                || value.equals("busca un nuevo cerebro");
    }

    public void buscarYDescargarModeloAutonomo(String razon) { buscarYDescargarModelo(razon, false); }

    public void buscarYDescargarModelo(String razon, boolean aprobacionHumanaExplicita) {
        Result result = solicitarDescarga(razon, aprobacionHumanaExplicita);
        Log.i("Salve/BuscadorModelos", result.message);
    }

    public Result solicitarDescarga(String razon, boolean aprobacionHumanaExplicita) {
        ObjectiveGovernance.Assessment assessment = ObjectiveGovernance.assess(
                razon, ObjectiveGovernance.Impact.SENSITIVE_DATA, aprobacionHumanaExplicita, true);
        if (!assessment.isAllowed()) {
            return new Result(Status.BLOCKED, "No solicité la descarga. " + assessment.reason);
        }
        // No invented URLs, partial parameter shards, or duplicated concurrent downloads.
        try {
            enqueue.run();
            return new Result(Status.REQUESTED, "He solicitado a Android descargar o reanudar Gemma 4 E2B del catálogo verificado. "
                    + "Esperará una conexión sin coste medido y espacio suficiente. La descarga y su activación aún no están confirmadas; "
                    + "puedes ver el progreso y pausarla en la notificación o en IA y cámara.");
        } catch (RuntimeException error) {
            return new Result(Status.ERROR, "Android no aceptó la solicitud de descarga. Abre IA y cámara para revisar el estado.");
        }
    }
}
