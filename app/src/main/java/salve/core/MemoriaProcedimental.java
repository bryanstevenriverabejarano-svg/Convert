package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import salve.core.tools.VirtualToolRecipe;

/** Persisted declarative tools; each step awaits explicit approval and its actual result. */
public class MemoriaProcedimental {
    private static final String TAG = "Salve/Rutinas";
    private static final String PREFS_NAME = "salve_habilidades";
    private final SharedPreferences prefs;
    private final Object executionLock = new Object();
    private VirtualToolRecipe.Execution activeExecution;
    private String activeName;

    public MemoriaProcedimental(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Stores the full name and canonical, verified JSON; invalid legacy scripts are never executed. */
    public synchronized void aprenderHabilidad(String nombreHabilidad, String scriptJson) {
        String name = VirtualToolRecipe.normalizeName(nombreHabilidad);
        VirtualToolRecipe recipe = VirtualToolRecipe.parse(scriptJson);
        String previous = prefs.getString(name, null);
        if (!prefs.edit().putString(name, recipe.toJson()).commit()) {
            restorePreference(name, previous);
            throw new IllegalStateException("No pude guardar la rutina en el dispositivo.");
        }
        // Recipe names and writing payloads can contain user information: do not log them.
        Log.i(TAG, "Rutina validada y guardada; pasos=" + recipe.getSteps().size());
    }

    public boolean conoceHabilidad(String nombreHabilidad) {
        try {
            return prefs.contains(VirtualToolRecipe.normalizeName(nombreHabilidad));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public List<String> listarHabilidades() {
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getValue() instanceof String) names.add(entry.getKey());
        }
        Collections.sort(names);
        return names;
    }

    public synchronized boolean eliminarHabilidad(String nombreHabilidad) {
        String name = VirtualToolRecipe.normalizeName(nombreHabilidad);
        VirtualToolRecipe.Execution execution;
        synchronized (executionLock) {
            execution = name.equals(activeName) ? activeExecution : null;
        }
        if (execution != null) execution.cancel();
        String previous = prefs.getString(name, null);
        if (previous == null) return false;
        if (prefs.edit().remove(name).commit()) return true;
        restorePreference(name, previous);
        return false;
    }

    private void restorePreference(String name, String previous) {
        // commit() updates the in-memory map even when the disk write fails.
        SharedPreferences.Editor rollback = prefs.edit();
        if (previous == null) rollback.remove(name);
        else rollback.putString(name, previous);
        if (!rollback.commit()) Log.w(TAG, "No se pudo persistir la restauración de una rutina.");
    }

    public boolean rutinaEnCurso() {
        synchronized (executionLock) {
            return activeExecution != null;
        }
    }

    public boolean cancelarHabilidad() {
        VirtualToolRecipe.Execution execution;
        synchronized (executionLock) {
            execution = activeExecution;
        }
        if (execution == null) return false;
        execution.cancel();
        return true;
    }

    public void ejecutarHabilidad(String nombreHabilidad, MotorConversacional motor) {
        final String name;
        final VirtualToolRecipe recipe;
        try {
            name = VirtualToolRecipe.normalizeName(nombreHabilidad);
            String script = prefs.getString(name, null);
            if (script == null) {
                motor.hablar("No tengo guardada esa rutina.");
                return;
            }
            recipe = VirtualToolRecipe.parse(script);
        } catch (RuntimeException e) {
            motor.hablar("Esa rutina no es válida: " + e.getMessage()
                    + " Enséñamela de nuevo usando aplicaciones y elementos visibles, sin coordenadas inventadas.");
            return;
        }

        VirtualToolRecipe.Execution execution = new VirtualToolRecipe.Execution(recipe,
                new VirtualToolRecipe.StepExecutor() {
                    @Override public void execute(String stepJson, java.util.function.Consumer<Boolean> completed) {
                        motor.ejecutarPasoRutina(stepJson, completed);
                    }

                    @Override public void cancel() { motor.cancelarPasoRutina(); }
                }, outcome -> {
                    synchronized (executionLock) {
                        activeExecution = null;
                        activeName = null;
                    }
                    if (outcome == VirtualToolRecipe.Outcome.SUCCESS) {
                        motor.hablar("Rutina " + name + " completada: todos los pasos devolvieron un resultado correcto.");
                    } else if (outcome == VirtualToolRecipe.Outcome.CANCELLED) {
                        motor.hablar("He detenido la rutina. Los pasos ya realizados se mantienen.");
                    } else {
                        motor.hablar("He detenido la rutina porque un paso no pudo completarse. No ejecuté los pasos restantes.");
                    }
                });
        synchronized (executionLock) {
            if (activeExecution != null) {
                motor.hablar("Ya hay una rutina en curso. Complétala o di cancelar rutina antes de iniciar otra.");
                return;
            }
            activeExecution = execution;
            activeName = name;
        }
        motor.hablar("Preparando rutina " + name + ". Confirma cada paso en pantalla; puedes cancelar la rutina.");
        execution.start();
    }
}
