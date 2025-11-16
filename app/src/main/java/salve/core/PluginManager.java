// PluginManager.java
package salve.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * PluginManager: gestiona el registro y acceso a los plugins cargados.
 */
public class PluginManager {
    private static final String TAG = "PluginManager";
    private final List<SavePlugin> plugins = new ArrayList<>();

    // Umbral mínimo para registrar un plugin. Los plugins con un score
    // inferior no se añaden al gestor para evitar comportamientos indeseados.
    private static final float SCORE_THRESHOLD = 0.6f;

    /**
     * Registra un plugin (si supera el umbral de score).
     */
    public void register(SavePlugin plugin) {
        try {
            float score = plugin.score();
            if (score >= SCORE_THRESHOLD) {
                plugins.add(plugin);
                Log.d(TAG, "Plugin registrado: " + plugin.getClass().getSimpleName() + " (score=" + score + ")");
            } else {
                Log.d(TAG, "Plugin rechazado: " + plugin.getClass().getSimpleName() + " (score=" + score + ")");
            }
        } catch (Exception e) {
            // Si el plugin lanza una excepción al obtener su score, no lo registramos
            Log.e(TAG, "Error obteniendo score de plugin " + plugin.getClass().getSimpleName(), e);
        }
    }

    /**
     * @return Lista inmutable de plugins actualmente registrados.
     */
    public List<SavePlugin> getPlugins() {
        return new ArrayList<>(plugins);
    }
}