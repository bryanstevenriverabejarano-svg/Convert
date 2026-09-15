package salve.core;

import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Herramienta de Salve para publicar su código HTML en Internet.
 */
public class GestorDespliegueWeb {
    private static final String TAG = "Salve/WebDeploy";

    // Usaremos un servicio abierto y gratuito (como hastebin/gist conceptual) para publicar
    public interface WebDeployCallback {
        void onExito(String urlPublica);
        void onError(String error);
    }

    /**
     * Sube el código HTML a un repositorio temporal público.
     */
    public void publicarHTML(String titulo, String contenidoHtml, WebDeployCallback callback) {
        new Thread(() -> {
            try {
                // Endpoint gratuito de prueba para subir texto (puedes cambiarlo por la API de GitHub Gists con un Token)
                URL url = new URL("https://pastebin.com/api/api_post.php"); 
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                // Construir los parámetros (Esto varía según la API que elijas usar finalmente)
                // NOTA: Para Pastebin real necesitas una API Key. Aquí se mantiene la estructura sugerida.
                String postData = "api_dev_key=TU_API_KEY_AQUI&api_option=paste&api_paste_code=" + 
                                  java.net.URLEncoder.encode(contenidoHtml, "UTF-8") + 
                                  "&api_paste_format=html&api_paste_name=" + java.net.URLEncoder.encode(titulo, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Scanner scanner = new Scanner(conn.getInputStream());
                    String urlPublica = scanner.hasNext() ? scanner.next() : "URL desconocida";
                    Log.i(TAG, "Sitio publicado con éxito en: " + urlPublica);
                    callback.onExito(urlPublica);
                } else {
                    callback.onError("Código HTTP: " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error publicando en la web", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }
}