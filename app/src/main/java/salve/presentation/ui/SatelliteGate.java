package salve.presentation.ui;

import android.app.Activity;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import salve.core.satellite.InstallRole;
import salve.core.satellite.SatelliteStore;

/**
 * El arranque del modo satélite, en una sola puerta. Solo actúa en una instalación nueva sin dueño
 * (una copia recién compilada del repositorio público): ofrece ser la Salve del dueño o unirse a una
 * Salve existente como satélite. La Salve de Bryan, que ya tiene memoria, entra como dueña y no ve nada.
 *
 * <p>No modifica el motor, ni la memoria, ni el sistema de nodos de cómputo, ni los Salventes: solo
 * decide, al abrir, qué es esta instalación, y lo guarda. Borrador: aún no restringe lo que un
 * satélite puede hacer; de momento solo lo registra y lo avisa.</p>
 */
final class SatelliteGate {
    private SatelliteGate() { }

    static void onStart(Activity host) {
        if (host == null || host.isFinishing()) return;
        SatelliteStore store = new SatelliteStore(host);
        switch (store.role()) {
            case SIN_RECLAMAR:
                askNewInstall(host, store);
                break;
            case SATELITE:
                Toast.makeText(host, "Este móvil es un satélite de «" + store.parentName()
                        + "». Sirve a esa Salve; no lleva una Salve propia.", Toast.LENGTH_LONG).show();
                break;
            default: // DUENO: la Salve del dueño, sin cambios.
        }
    }

    /** La elección de la primera vez. Sin botón de cancelar: hay que decidir qué es esta copia. */
    private static void askNewInstall(Activity host, SatelliteStore store) {
        new AlertDialog.Builder(host)
                .setCancelable(false)
                .setTitle("¿Qué es esta Salve?")
                .setMessage("Esta instalación no tiene dueño todavía.\n\n"
                        + "• Si es tuya, puedes ser su dueño: será una Salve independiente, con su propia memoria.\n\n"
                        + "• O puede unirse a una Salve que ya existe, como satélite: la ayuda con cómputo, pantalla y "
                        + "sensores, pero la que piensa y recuerda sigue siendo la Salve del dueño.")
                .setPositiveButton("Ser dueño", (d, w) -> {
                    store.claimOwner();
                    Toast.makeText(host, "Esta es ahora tu Salve.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Unirme como satélite", (d, w) -> joinAsSatellite(host, store))
                .show();
    }

    /** Lee el QR salve://nodo que muestra el dueño y se une a esa Salve. Reusa el emparejamiento de nodos. */
    private static void joinAsSatellite(Activity host, SatelliteStore store) {
        final android.widget.EditText field = new android.widget.EditText(host);
        field.setHint("salve://nodo?v=1&n=…&u=https://…&t=…");
        new AlertDialog.Builder(host)
                .setTitle("Unirse a una Salve")
                .setMessage("Pega el enlace de emparejamiento que te muestre el dueño de la Salve "
                        + "(el mismo QR salve://nodo que usan los nodos).")
                .setView(field)
                .setPositiveButton("Unirme", (d, w) -> {
                    try {
                        salve.core.compute.NodePairing p = salve.core.compute.NodePairing.fromQr(field.getText().toString());
                        store.becomeSatelliteOf(p.name, p.endpoint);
                        Toast.makeText(host, "Unido a «" + p.name + "» como satélite.", Toast.LENGTH_LONG).show();
                    } catch (IllegalArgumentException bad) {
                        Toast.makeText(host, bad.getMessage(), Toast.LENGTH_LONG).show();
                        askNewInstall(host, store); // vuelve a la elección
                    }
                })
                .setNegativeButton("Atrás", (d, w) -> askNewInstall(host, store))
                .show();
    }
}
