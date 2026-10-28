package salve.core.evaluation;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import salve.avatar.AvatarState;
import salve.work.SleepFocusPolicy;
import android.util.AtomicFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.BooleanSupplier;
import salve.core.MindRuntime;

/** Android adapter: read the existing real exam and skill journal; persist only safe summary metadata. */
public final class SleepImprovementRuntime {
    private static final int MAX_BYTES = 100_000;
    private SleepImprovementRuntime() { }

    private static File reportPath(Context context) {
        return new File(new File(context.getApplicationContext().getNoBackupFilesDir(), "mind"),
                "sleep-improvement.json");
    }

    public static JsonObject last(Context context) {
        File path = reportPath(context);
        if (!path.isFile() || path.length() > MAX_BYTES) return null;
        try {
            return JsonParser.parseString(new String(new AtomicFile(path).readFully(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception invalid) { return null; }
    }

    /** Existing exam: no model re-execution, fabricated grades or network traffic. */
    public static JsonObject lastExam(Context context) {
        File path = new File(new File(context.getApplicationContext().getFilesDir(), "exam"), "ultimo.json");
        if (!path.isFile() || path.length() > MAX_BYTES) return null;
        try {
            return JsonParser.parseString(new String(Files.readAllBytes(path.toPath()),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception invalid) { return null; }
    }

    /** Called only inside the existing sleep gate, after its verified local challenge. */
    public static boolean record(Context context, MindRuntime mind, boolean practiceRan,
                                 BooleanSupplier stopped) {
        if (stopped != null && stopped.getAsBoolean()) return false;
        JsonObject exam = lastExam(context);
        JsonObject report = SleepImprovementCycle.assess(exam, mind.skills().history(),
                mind.skills().decisions(), practiceRan, System.currentTimeMillis());
        if (stopped != null && stopped.getAsBoolean()) return false;
        File target = reportPath(context);
        File dir = target.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) return false;
        AtomicFile file = new AtomicFile(target);
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(report.toString().getBytes(StandardCharsets.UTF_8));
            if (stopped != null && stopped.getAsBoolean()) { file.failWrite(stream); return false; }
            file.finishWrite(stream);
            return true;
        } catch (Exception failed) {
            if (stream != null) file.failWrite(stream);
            return false;
        }
    }

    /** Selected at night, but revalidated against the same exam at explicit patch-request time. */
    public static JsonObject eligibleCandidate(Context context, JsonObject exam) {
        JsonObject report = last(context);
        if (report == null) return null;
        try {
            long now = System.currentTimeMillis();
            long assessed = report.get("evaluated_at").getAsLong();
            long examined = exam.get("at").getAsLong();
            if (assessed < 0 || now < assessed || now - assessed > SleepImprovementCycle.MAX_EXAM_AGE_MS
                    || examined < 0 || now < examined || now - examined > SleepImprovementCycle.MAX_EXAM_AGE_MS)
                return null;
            return SleepImprovementCycle.candidateFor(report, exam);
        } catch (RuntimeException invalid) { return null; }
    }

    public static String status(Context context) {
        JsonObject report = last(context);
        if (report != null) return SleepImprovementCycle.describe(report);
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences("salve_sleep_focus", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("enabled", false))
            return "Todavía no tengo un informe de mejora nocturna guardado. Para crear uno debo entrar en modo sueño y completar un ciclo; la animación por sí sola no demuestra aprendizaje.";
        AvatarState avatar = AvatarState.decode(app.getSharedPreferences("salve_avatar", Context.MODE_PRIVATE)
                .getString("state", null));
        if (!SleepFocusPolicy.active(true, avatar))
            return "El modo sueño está solicitado, pero mi personaje ya no está dormido. No tengo un ciclo nocturno completado.";
        BatteryManager battery = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
        if (battery != null && !battery.isCharging())
            return "Estoy en modo sueño, pero aún no hay informe. El trabajo nocturno está esperando a que conectes el móvil al cargador y Android permita ejecutarlo; la animación de dormir no significa que el modelo esté trabajando.";
        return "Estoy en modo sueño y todavía no he guardado ningún ciclo completo. Android puede aplazarlo si hay otras tareas, ahorro de batería o el modelo está ocupado. Consulta el Registro de ejecución para ver los intentos.";
    }
}
