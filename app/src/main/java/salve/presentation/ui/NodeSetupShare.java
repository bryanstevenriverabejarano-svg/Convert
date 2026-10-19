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
    private static final String[] SCRIPTS = {"salve_node_setup.py", "salve_compute_node.py", "salve_private_location.py", "requirements-family.txt", "PRIVATE_FAMILY_NETWORK.md"};
    static final String GUIDE = "Red privada de Salve\n\n"
            + "Sigue PRIVATE_FAMILY_NETWORK.md (adjunto) para configurar WireGuard propio en Linux, crear tus certificados TLS e iniciar el nodo.\n"
            + "Instala las dependencias con: python -m pip install -r requirements-family.txt\n"
            + "En la app abre Nodos de cómputo > Red privada y mapa familiar. Importa tu perfil WireGuard y CA, autoriza la VPN y conecta.\n"
            + "Registra cada nodo por su IPv4 privada y token. Selecciona Familia para supervisarlo.\n"
            + "Cada propietario elige si comparte ubicación estática o GPS puntual. No se descubre ni instala ningún nodo automáticamente.";

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
