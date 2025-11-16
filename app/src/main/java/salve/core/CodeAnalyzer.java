package salve.core;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * CodeAnalyzer.java
 * -----------------
 * Explora las clases del paquete com.salve.salve.core mediante reflection
 * e identifica posibles puntos de mejora estructural.
 */
public class CodeAnalyzer {

    private static final String TAG = "CodeAnalyzer";
    private final Context context;

    /**
     * Constructor.
     *
     * @param context Contexto de la aplicación.
     */
    public CodeAnalyzer(Context context) {
        this.context = context;
    }

    /**
     * Inicia el análisis de las clases core.
     */
    public void analyze() {
        List<Class<?>> classes = discoverCoreClasses();
        for (Class<?> cls : classes) {
            inspectClass(cls);
        }
    }

    /**
     * Devuelve la lista de clases core a inspeccionar.
     * Actualmente enumeradas de forma manual.
     *
     * @return Listado de clases core.
     */
    private List<Class<?>> discoverCoreClasses() {
        List<Class<?>> classes = new ArrayList<>();
        try {
            classes.add(Class.forName("com.salve.salve.core.MotorConversacional"));
            classes.add(Class.forName("com.salve.salve.core.MemoriaEmocional"));
            classes.add(Class.forName("com.salve.salve.core.DetectorEmociones"));
            classes.add(Class.forName("com.salve.salve.core.ThinkWorker"));
            // Si añades nuevas clases core, inclúyelas aquí
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Clase core no encontrada durante el descubrimiento", e);
        }
        return classes;
    }

    /**
     * Inspecciona los métodos de la clase y emite recomendaciones:
     *  - Número de parámetros en la firma (> 4).
     *
     * @param cls Clase a inspeccionar.
     */
    private void inspectClass(Class<?> cls) {
        Log.i(TAG, "Analizando clase: " + cls.getSimpleName());
        Method[] methods = cls.getDeclaredMethods();
        for (Method method : methods) {
            StringBuilder sb = new StringBuilder();
            sb.append("  Método: ")
                    .append(method.getName())
                    .append(" (params=")
                    .append(method.getParameterCount())
                    .append(")");

            // Sugerencia de refactorización si hay demasiados parámetros
            if (method.getParameterCount() > 4) {
                sb.append(" ← demasiados parámetros, considera refactorizar");
            }

            Log.i(TAG, sb.toString());
        }
    }
}