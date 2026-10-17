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
    private static final String[] SCRIPTS = {"salve_node_setup.py", "salve_compute_node.py"};
    static final String GUIDE = "Guía para preparar tu PC como nodo de Salve\n\n"
            + "1. Instala Python 3.10+ y llama.cpp. Opcional: pip install qrcode zeroconf\n"
            + "2. Mide tu equipo y obtén la recomendación de modelo:\n"
            + "   python salve_node_setup.py preparar\n"
            + "3. Descarga un GGUF del tamaño recomendado y arráncalo solo en local:\n"
            + "   llama-server -m modelo.gguf --host 127.0.0.1 --port 8080\n"
            + "4. Publica el nodo con HTTPS válido. Con Tailscale en el PC y en el móvil:\n"
            + "   tailscale serve --bg --https=443 http://127.0.0.1:8766\n"
            + "5. Repite el paso 2 (detecta la URL de Tailscale o pásala con --url) y arranca el nodo:\n"
            + "   python salve_compute_node.py --token-file ~/.salve/node_token --model <modelo> --port 8766\n"
            + "6. En la app: Nodos de cómputo > Añadir un nodo > Escanear QR.\n"
            + "   Para que aparezca en \"Buscar en mi red\": python salve_node_setup.py anunciar --url <tu-url>\n\n"
            + "El QR incluye el token: no lo compartas. El anuncio de red no lo incluye.";

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
