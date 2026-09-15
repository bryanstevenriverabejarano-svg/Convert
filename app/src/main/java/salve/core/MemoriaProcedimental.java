package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * El Cerebelo de Salve (Memoria Muscular).
 * Almacena y ejecuta secuencias de acciones (Macros) de forma instantánea 
 * sin necesidad de despertar al modelo LLM pesado.
 */
public class MemoriaProcedimental {
    private static final String TAG = "Salve/Cerebelo";
    private static final String PREFS_NAME = "salve_habilidades";
    private final SharedPreferences prefs;

    public MemoriaProcedimental(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Guarda una nueva "Habilidad" en el cerebro de Salve.
     * Ejemplo de scriptJson: [{"tool":"TAP", "x":100, "y":200}, {"tool":"ESCRIBIR", "texto":"hola"}]
     */
    public void aprenderHabilidad(String nombreHabilidad, String scriptJson) {
        prefs.edit().putString(nombreHabilidad.toLowerCase().trim(), scriptJson).apply();
        Log.i(TAG, "Nueva habilidad muscular adquirida: " + nombreHabilidad);
    }

    /**
     * Comprueba si Salve ya sabe hacer esto sin pensar.
     */
    public boolean conoceHabilidad(String nombreHabilidad) {
        return prefs.contains(nombreHabilidad.toLowerCase().trim());
    }

    public List<String> listarHabilidades() {
        return new ArrayList<>(prefs.getAll().keySet());
    }

    /**
     * Ejecuta la rutina a la velocidad de la luz.
     */
    public void ejecutarHabilidad(String nombreHabilidad, MotorConversacional motor) {
        String script = prefs.getString(nombreHabilidad.toLowerCase().trim(), null);
        if (script == null) return;

        motor.hablar("Ejecutando rutina aprendida: " + nombreHabilidad);
        
        new Thread(() -> {
            try {
                JSONArray acciones = new JSONArray(script);
                for (int i = 0; i < acciones.length(); i++) {
                    JSONObject accion = acciones.getJSONObject(i);
                    // Pasamos la acción simulando que el LLM la generó
                    motor.interceptarComandoJSON(accion.toString());
                    
                    // Pausa biológica entre taps para que Android reaccione a la UI
                    Thread.sleep(1500); 
                }
                motor.hablar("Rutina " + nombreHabilidad + " completada con éxito.");
            } catch (Exception e) {
                Log.e(TAG, "Fallo en la memoria muscular", e);
                motor.hablar("Mi memoria muscular falló al ejecutar esta rutina. Necesito re-aprenderla.");
            }
        }).start();
    }
}
