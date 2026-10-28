package salve.core.evaluation;

import android.content.Context;
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
    public static void record(Context context, MindRuntime mind, boolean practiceRan,
                              BooleanSupplier stopped) {
        if (stopped != null && stopped.getAsBoolean()) return;
        JsonObject exam = lastExam(context);
        JsonObject report = SleepImprovementCycle.assess(exam, mind.skills().history(),
                mind.skills().decisions(), practiceRan, System.currentTimeMillis());
        if (stopped != null && stopped.getAsBoolean()) return;
        File target = reportPath(context);
        File dir = target.getParentFile();
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        AtomicFile file = new AtomicFile(target);
        FileOutputStream stream = null;
        try {
            stream = file.startWrite();
            stream.write(report.toString().getBytes(StandardCharsets.UTF_8));
            if (stopped != null && stopped.getAsBoolean()) { file.failWrite(stream); return; }
            file.finishWrite(stream);
        } catch (Exception failed) {
            if (stream != null) file.failWrite(stream);
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
        return SleepImprovementCycle.describe(last(context));
    }
}
