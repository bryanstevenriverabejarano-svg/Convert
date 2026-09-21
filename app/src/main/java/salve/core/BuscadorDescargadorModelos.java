package salve.core;

import android.content.Context;
import android.util.Log;
import salve.work.ModelDownloadWorker;

/** Uses the same pinned, verified catalog and unique worker as the user interface. */
public class BuscadorDescargadorModelos {
    private final Context context;
    public BuscadorDescargadorModelos(Context context) { this.context = context.getApplicationContext(); }

    public void buscarYDescargarModeloAutonomo(String razon) { buscarYDescargarModelo(razon, false); }

    public void buscarYDescargarModelo(String razon, boolean aprobacionHumanaExplicita) {
        ObjectiveGovernance.Assessment assessment = ObjectiveGovernance.assess(
                razon, ObjectiveGovernance.Impact.SENSITIVE_DATA, aprobacionHumanaExplicita, true);
        if (!assessment.isAllowed()) {
            Log.i("Salve/BuscadorModelos", assessment.reason);
            return;
        }
        // No invented URLs, partial parameter shards, or duplicated concurrent downloads.
        ModelDownloadWorker.enqueue(context);
    }
}
