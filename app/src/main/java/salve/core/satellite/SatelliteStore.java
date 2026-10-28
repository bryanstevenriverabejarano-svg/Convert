package salve.core.satellite;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/**
 * Guarda y resuelve el rol de esta instalación (ver {@link InstallRole}). Decide sola, al arrancar,
 * leyendo tres señales locales: si ya se reclamó como dueña, si ya está unida a una Salve, y si hay
 * datos de un uso anterior (memoria o recuerdos). Nunca sale a la red ni toca el sistema de nodos.
 */
public final class SatelliteStore {
    private static final String PREFS = "salve_satellite";
    private static final String KEY_OWNER = "claimed_owner";
    private static final String KEY_PARENT_ENDPOINT = "parent_endpoint";
    private static final String KEY_PARENT_NAME = "parent_name";

    private final Context app;

    public SatelliteStore(Context context) { this.app = context.getApplicationContext(); }

    /** El rol actual, decidido con las señales locales (por defecto satélite, sin preguntar nada). */
    public InstallRole role() {
        return InstallRole.decide(prefs().getBoolean(KEY_OWNER, false), hasExistingData());
    }

    /** Un satélite que todavía no ha encontrado a qué Salve unirse (busca sola en la red). */
    public boolean satellitePendingParent() {
        return role() == InstallRole.SATELITE && parentEndpoint() == null;
    }

    /** Esta instalación se declara la Salve del dueño (elección explícita en el arranque). */
    public void claimOwner() {
        prefs().edit().putBoolean(KEY_OWNER, true).remove(KEY_PARENT_ENDPOINT).remove(KEY_PARENT_NAME).apply();
    }

    /**
     * Esta instalación se une como satélite a la Salve indicada (por ejemplo, tras leer el QR
     * {@code salve://nodo?…} que muestra el dueño). Guarda a quién sirve; no arranca una Salve propia.
     */
    public void becomeSatelliteOf(String parentName, String parentEndpoint) {
        String endpoint = salve.core.compute.ComputeNode.endpoint(parentEndpoint); // valida https, sin ruta ni credenciales
        prefs().edit().putBoolean(KEY_OWNER, false)
                .putString(KEY_PARENT_ENDPOINT, endpoint)
                .putString(KEY_PARENT_NAME, parentName == null ? "" : parentName.trim())
                .apply();
    }

    /** La Salve a la que sirve este satélite, o null si no es un satélite. */
    public String parentEndpoint() {
        String value = prefs().getString(KEY_PARENT_ENDPOINT, null);
        return value == null || value.isEmpty() ? null : value;
    }

    public String parentName() { return prefs().getString(KEY_PARENT_NAME, ""); }

    /** Vuelve a un estado sin decidir (para rehacer la elección del arranque). */
    public void reset() {
        prefs().edit().remove(KEY_OWNER).remove(KEY_PARENT_ENDPOINT).remove(KEY_PARENT_NAME).apply();
    }

    /**
     * ¿Hay rastro de un uso anterior? Protege la Salve de Bryan de antes de esta función: si ya
     * tiene memoria, recuerdos o una verificación guardada, es su dueña y no se le pregunta nada.
     */
    private boolean hasExistingData() {
        try {
            if (app.getDatabasePath("memoria.db").exists()) return true;
            if (new File(app.getFilesDir(), "causal_memory.tsv").exists()) return true;
            if (app.getSharedPreferences("salve_identity", Context.MODE_PRIVATE).getLong("verified_at", 0L) > 0L) return true;
        } catch (RuntimeException unavailable) { /* en duda, no se asume nada */ }
        return false;
    }

    private SharedPreferences prefs() { return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
}
