package salve.presentation.ui;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import salve.core.satellite.SatelliteAutoJoin;
import salve.core.satellite.SatelliteStore;

/**
 * El arranque del modo satélite, en una sola puerta y sin preguntar nada.
 *
 * <p>La Salve de Bryan (ya con memoria o verificación previa) es dueña: no pasa nada, arranca normal.
 * Una copia recién compilada del repositorio, sin dueño, se instala <b>sola como satélite</b>: no
 * nace como una Salve independiente, sino que busca en la red local una Salve a la que unirse. No hay
 * cartel ni elección.</p>
 *
 * <p>No modifica el motor, ni la memoria, ni el sistema de nodos de cómputo, ni los Salventes.
 * Borrador: marca el rol y lanza la búsqueda; todavía no restringe lo que un satélite ejecuta.</p>
 */
final class SatelliteGate {
    private static final String TAG = "Salve/Satelite";

    private SatelliteGate() { }

    static void onStart(Activity host) {
        if (host == null || host.isFinishing()) return;
        SatelliteStore store = new SatelliteStore(host);
        if (store.role().fullSalve()) return; // la Salve del dueño: sin cambios.

        // Satélite, automáticamente. Si ya sabe a qué Salve sirve, se presenta; si no, la busca sola.
        if (store.parentEndpoint() != null) {
            Log.i(TAG, "Satélite de «" + store.parentName() + "»; registrándose.");
            final android.content.Context app = host.getApplicationContext();
            new Thread(() -> new salve.core.satellite.SatelliteLink(app).register(), "salve-satelite-registro").start();
            return;
        }
        Toast.makeText(host, "Este móvil es un satélite: buscando una Salve a la que unirse…", Toast.LENGTH_LONG).show();
        new SatelliteAutoJoin(host).startIfPending();
    }
}
