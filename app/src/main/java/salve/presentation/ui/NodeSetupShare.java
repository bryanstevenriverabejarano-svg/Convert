package salve.presentation.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/** Envía al PC del dueño los scripts del nodo y una guía breve. Se ejecutan allí, a mano. */
final class NodeSetupShare {
    private static final String[] SCRIPTS = {"salve_private_network.py", "salve_node_setup.py", "salve_compute_node.py",
            "salve_private_location.py", "requirements-family.txt", "PRIVATE_FAMILY_NETWORK.md"};
    static final String GUIDE = "Red privada de Salve (sin servicios de terceros)\n\n"
            + "1. En el PC instala Python 3.10+, llama.cpp y WireGuard (wireguard.com, sin cuenta).\n"
            + "2. python -m pip install -r requirements-family.txt\n"
            + "3. Mide tu equipo y elige modelo: python salve_node_setup.py preparar --url https://10.77.0.2:8766\n"
            + "4. Crea la red privada: python salve_private_network.py crear --endpoint <IP del PC o de tu router>\n"
            + "   Genera certificados, claves WireGuard, el perfil del móvil y el comando exacto para arrancar el nodo.\n"
            + "5. Importa wg-nodo-salve.conf en WireGuard del PC y actívalo.\n"
            + "6. Pasa al móvil salve-movil.conf y ca.pem. En Salve: Nodos de cómputo → Red privada (VPN) → Configurar → Conectar.\n"
            + "7. Arranca llama-server y el nodo con el comando que muestra el paso 4, y escanea su QR en Añadir un nodo.\n\n"
            + "Detalles y modo manual en PRIVATE_FAMILY_NETWORK.md. El QR y salve-movil.conf son privados.";

    private NodeSetupShare() { }

    static void share(Activity activity) {
        try {
            File folder = new File(activity.getCacheDir(), "nodo");
            if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("sin carpeta");
            ArrayList<Uri> files = new ArrayList<>();
            for (String name : SCRIPTS) {
                File target = new File(folder, name);
                try (InputStream in = activity.getAssets().open("nodo/" + name); OutputStream out = new FileOutputStream(target)) {
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                }
                files.add(FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", target));
            }
            Intent send = new Intent(Intent.ACTION_SEND_MULTIPLE)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_SUBJECT, "Salve: scripts del nodo de cómputo")
                    .putExtra(Intent.EXTRA_TEXT, GUIDE)
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, "Enviar a mi PC"));
        } catch (IOException | RuntimeException error) {
            new androidx.appcompat.app.AlertDialog.Builder(activity).setTitle("Enviar script")
                    .setMessage("No se pudieron preparar los archivos. Los scripts están en la carpeta scripts/ del proyecto.")
                    .setPositiveButton("Cerrar", null).show();
        }
    }
}
