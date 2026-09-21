package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import dalvik.system.DexFile;

/**
 * CodeAnalyzerEnhanced:
 *  - Descubre dinámicamente clases anotadas @CoreComponent.
 *  - Inspecciona métodos y genera AnalysisReport.
 *  - Soporta timeout para no bloquear.
 */
public class CodeAnalyzerEnhanced {
    private static final String TAG = "CodeAnalyzerEnh";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CodeAnalyzerEnhanced(Context context) {
        this.context = context;
    }

    /** Ejecuta el análisis con timeout y devuelve reports. */
    public List<AnalysisReport> analyzeWithTimeout(long timeout, TimeUnit unit) {
        Future<List<AnalysisReport>> future = executor.submit(this::performAnalysis);
        try {
            return future.get(timeout, unit);
        } catch (TimeoutException te) {
            future.cancel(true);
            Log.w(TAG, "Análisis abortado por timeout");
        } catch (InterruptedException | ExecutionException e) {
            Log.e(TAG, "Error durante análisis", e);
        }
        return Collections.emptyList();
    }

    /** An explicit source allowlist replaces discovery when no classes carry @CoreComponent. */
    public List<AnalysisReport> analyzeSourceTargets(List<String> classNames) {
        if (classNames == null || classNames.size() > AutoImprovementSourceSnapshot.MAX_FILES) {
            throw new IllegalArgumentException("Lista de fuentes inválida");
        }
        List<Class<?>> classes = new ArrayList<>();
        for (String name : classNames) {
            if (Thread.currentThread().isInterrupted()) break;
            if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("Clase inválida");
            try {
                classes.add(Class.forName("salve.core." + name, false, context.getClassLoader()));
            } catch (ClassNotFoundException | LinkageError error) {
                Log.w(TAG, "El APK no contiene una clase analizable para la fuente " + name);
            }
        }
        return inspect(classes);
    }

    /** Lógica de detección e inspección. */
    private List<AnalysisReport> performAnalysis() {
        return inspect(discoverCoreClasses());
    }

    private List<AnalysisReport> inspect(List<Class<?>> classes) {
        List<AnalysisReport> reports = new ArrayList<>();
        for (Class<?> cls : classes) {
            final Method[] methods;
            try { methods = cls.getDeclaredMethods(); }
            catch (LinkageError missingDependency) {
                Log.w(TAG, "Faltan dependencias para inspeccionar " + cls.getSimpleName());
                continue;
            }
            AnalysisReport report = new AnalysisReport(cls.getSimpleName());
            for (Method m : methods) {
                int params = m.getParameterCount();
                IssueLevel level = (params > 4) ? IssueLevel.WARNING : IssueLevel.INFO;
                String suggestion = (params > 4)
                        ? "Demasiados parámetros, considera refactorizar"
                        : "OK";
                report.addIssue(new MethodIssue(m.getName(), params, level, suggestion));
            }
            Log.i(TAG, report.toString());
            reports.add(report);
        }
        return reports;
    }

    /** Escanea el APK y retorna clases anotadas @CoreComponent. */
    private List<Class<?>> discoverCoreClasses() {
        List<Class<?>> classes = new ArrayList<>();
        try {
            DexFile dex = new DexFile(context.getPackageCodePath());
            Enumeration<String> entries = dex.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement();
                if (name.startsWith("salve.core")) {
                    Class<?> cls = Class.forName(name);
                    if (cls.isAnnotationPresent(CoreComponent.class)) {
                        classes.add(cls);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error descubriendo clases core", e);
        }
        return classes;
    }
}
