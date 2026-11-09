package salve.services;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

/**
 * Si Salve se cierra sola, al volver a abrirse apunta en su registro de ejecución por qué:
 * fallo de Java (con la pila), fallo nativo del motor (el «tombstone» de Android), falta de
 * memoria o bloqueo. Así Bryan puede pasar el motivo exacto en vez de adivinarlo.
 */
public final class CrashReport {
    private static final String TAG = "Salve/Cierre";
    private static final String FILE = "ultimo-cierre-java.txt";
    private static final String PREFS = "salve_cierres";
    private static final String SEEN = "ultimo_cierre_visto";
    private static final int MAX_TRACE = 12_000;

    private CrashReport() { }

    /** Guarda la pila de un fallo de Java antes de que Android cierre la app. */
    public static void install(Context context) {
        final File target = new File(context.getNoBackupFilesDir(), FILE);
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try (FileOutputStream out = new FileOutputStream(target)) {
                StringWriter stack = new StringWriter();
                error.printStackTrace(new PrintWriter(stack));
                String text = "Hilo: " + thread.getName() + "\n" + stack;
                out.write(clip(text).getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) { }
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    /** Al arrancar: si la vez anterior acabó mal, lo deja en el registro de ejecución. */
    public static void recordPrevious(Context context) {
        try {
            StringBuilder report = new StringBuilder();
            String why = null;
            File java = new File(context.getNoBackupFilesDir(), FILE);
            if (java.isFile()) {
                why = "FALLO DE JAVA";
                report.append(read(java));
                //noinspection ResultOfMethodCallIgnored
                java.delete();
            }
            if (Build.VERSION.SDK_INT >= 30) {
                String exit = lastBadExit(context);
                if (exit != null) {
                    if (why == null) why = exit.substring(0, exit.indexOf('\n') < 0 ? exit.length() : exit.indexOf('\n'));
                    if (report.length() > 0) report.append("\n\n");
                    report.append(exit);
                }
            }
            if (why == null) return;
            ExecutionJournal journal = ExecutionTrace.journal(context);
            String id = journal.begin("CIERRE INESPERADO", "Salve se cerró sola la vez anterior");
            journal.append(id, why, clip(report.toString()));
            journal.finish(id, "FALLO");
            Log.w(TAG, why + "\n" + report);
        } catch (RuntimeException unavailable) {
            Log.w(TAG, "No se pudo leer el motivo del último cierre", unavailable);
        }
    }

    @androidx.annotation.RequiresApi(30)
    private static String lastBadExit(Context context) {
        ActivityManager manager = context.getSystemService(ActivityManager.class);
        if (manager == null) return null;
        List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(context.getPackageName(), 0, 5);
        if (exits == null || exits.isEmpty()) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // La primera vez (recién instalada) se mira solo el último día: el cierre que hizo falta
        // diagnosticar suele ser justo el anterior a instalar esta versión.
        long seen = prefs.getLong(SEEN, System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        ApplicationExitInfo newest = exits.get(0);
        prefs.edit().putLong(SEEN, Math.max(seen, newest.getTimestamp())).apply();
        for (ApplicationExitInfo exit : exits) {
            if (exit.getTimestamp() <= seen) break;
            String reason = reason(exit.getReason());
            if (reason == null) continue;
            StringBuilder text = new StringBuilder(reason).append('\n')
                    .append(ExecutionJournal.date(exit.getTimestamp()))
                    .append(" · proceso ").append(exit.getProcessName())
                    .append(" · memoria usada ").append(exit.getPss() / 1024).append(" MB");
            if (exit.getDescription() != null) text.append("\nAndroid: ").append(exit.getDescription());
            if (exit.getReason() == ApplicationExitInfo.REASON_CRASH_NATIVE
                    || exit.getReason() == ApplicationExitInfo.REASON_ANR) {
                try (InputStream trace = exit.getTraceInputStream()) {
                    if (trace != null) text.append("\n\n").append(readHead(trace));
                } catch (Exception unreadable) {
                    text.append("\n(no se pudo leer la traza: ").append(unreadable.getClass().getSimpleName()).append(')');
                }
            }
            return text.toString();
        }
        return null;
    }

    @androidx.annotation.RequiresApi(30)
    private static String reason(int code) {
        switch (code) {
            case ApplicationExitInfo.REASON_CRASH: return "FALLO DE JAVA (Android)";
            case ApplicationExitInfo.REASON_CRASH_NATIVE: return "FALLO NATIVO (motor del modelo, voz u otra librería)";
            case ApplicationExitInfo.REASON_LOW_MEMORY: return "ANDROID LA CERRÓ POR FALTA DE MEMORIA";
            case ApplicationExitInfo.REASON_ANR: return "BLOQUEO (la app dejó de responder)";
            case ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE: return "ANDROID LA CERRÓ POR USO EXCESIVO";
            case ApplicationExitInfo.REASON_INITIALIZATION_FAILURE: return "FALLO AL ARRANCAR";
            case ApplicationExitInfo.REASON_SIGNALED: return "CERRADA POR SEÑAL DEL SISTEMA";
            default: return null; // salida normal, cerrada por el usuario o actualización
        }
    }

    private static String read(File file) {
        try (InputStream in = new java.io.FileInputStream(file)) { return readHead(in); }
        catch (Exception unreadable) { return "(no se pudo leer)"; }
    }

    private static String readHead(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while (out.size() < MAX_TRACE && (n = in.read(buffer)) > 0) out.write(buffer, 0, Math.min(n, MAX_TRACE - out.size()));
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static String clip(String text) {
        return text.length() <= MAX_TRACE ? text : text.substring(0, MAX_TRACE) + "\n[…recortado]";
    }
}
