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
    private static final String[] SCRIPTS = {"salve_nodo_app.py", "salve_private_network.py", "salve_node_setup.py",
            "salve_compute_node.py", "salve_private_location.py", "salve_guardian.py", "salve_home.py", "requirements-family.txt", "PRIVATE_FAMILY_NETWORK.md"};
    static final String GUIDE = "Nodo de Salve con TLS mutuo (sin VPN ni programas extra)\n\n"
            + "Windows, lo más fácil: descarga SalveNodo.exe (artefacto «SalveNodo-windows» de GitHub Actions) y ábrelo con doble clic.\n"
            + "No necesita Python. Si quieres modelo local, pon llama-server.exe junto a SalveNodo.exe.\n\n"
            + "1. Responde las preguntas: nombre, IP del PC, IP pública o dominio DDNS (opcional), puerto (8443),\n"
            + "   ubicación fija opcional (latitud, longitud, ciudad, país) y el archivo .gguf del modelo.\n"
            + "2. Se abre un QR. En Salve: Nodos de cómputo → Añadir un nodo → Escanear QR → Guardar.\n"
            + "   (O pasa salve-perfil.json al móvil e impórtalo en Conexión segura.)\n"
            + "3. Deja la ventana abierta. Si Windows pregunta por el cortafuegos, permite redes privadas.\n"
            + "4. Fuera de casa: reenvía TCP 8443 del router a este PC y usa tu IP pública o DDNS.\n\n"
            + "Con Python (Windows, Linux o macOS): python -m pip install -r requirements-family.txt\n"
            + "y después python salve_nodo_app.py. Detalles en PRIVATE_FAMILY_NETWORK.md.\n"
            + "El QR y salve-perfil.json contienen la clave del móvil: no los compartas.";

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
