// PluginIndexReader.java
package salve.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * PluginIndexReader: lee el índice de plugins desde META-INF/plugins.txt 
 * dentro del APK (que es un ZIP).
 */
public class PluginIndexReader {

    /**
     * Abre el APK como ZIP y lee línea a línea META-INF/plugins.txt.
     * Cada línea debe contener un nombre de clase de plugin.
     *
     * @param apkPath Ruta absoluta al .apk de la app corriente.
     * @return Lista de nombres de clase candidates.
     * @throws IOException Si no existe el índice o falla lectura.
     */
    public static List<String> read(String apkPath) throws IOException {
        List<String> list = new ArrayList<>();
        try (ZipFile zip = new ZipFile(apkPath)) {
            ZipEntry entry = zip.getEntry("META-INF/plugins.txt");
            if (entry == null) {
                throw new IOException("No se encontró META-INF/plugins.txt");
            }
            try (InputStream is = zip.getInputStream(entry);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        list.add(line);
                    }
                }
            }
        }
        return list;
    }
}