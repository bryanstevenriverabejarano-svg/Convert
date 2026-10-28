package salve.core.evaluation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import salve.core.autonomy.BlindSkillTransferExperiment;

/**
 * Low-frequency, local-only counterfactual-style generalization probe.
 * The oracle and final holdout belong to BlindSkillTransferExperiment, not
 * the model. No code, learned tool or weight is installed by this worker.
 */
public final class NightTransferTrial {
    public static final long COOLDOWN_MS = 7L * 24 * 60 * 60 * 1000L;
    private static final String PREFS = "salve_night_transfer";
    private static final String KEY_LAST = "last_attempt";
    private NightTransferTrial() { }

    public static boolean due(long previous, long now) {
        return now > 0 && previous >= 0 && previous <= now
                && (previous == 0 || now - previous >= COOLDOWN_MS);
    }

    public static String runIfDue(Context context, BooleanSupplier stopped) {
        Context app = context.getApplicationContext();
        BooleanSupplier cancel = stopped == null ? () -> false : stopped;
        if (cancel.getAsBoolean()) return "INTERRUMPIDO";
        SharedPreferences pref = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        synchronized (NightTransferTrial.class) {
            if (!due(pref.getLong(KEY_LAST, 0), now)) return "EN_ESPERA_SEMANAL";
            // Claim the budget before the expensive work, including crashes/restarts.
            if (!pref.edit().putLong(KEY_LAST, now).commit()) return "SIN_PRESUPUESTO";
        }
        try {
            BlindSkillTransferExperiment.Result measured =
                    BlindSkillTransferExperiment.runNewChallenge(cancel);
            if (cancel.getAsBoolean()) return "INTERRUMPIDO";
            JsonObject record = new JsonObject();
            record.addProperty("schema", 1);
            record.addProperty("at", now);
            record.addProperty("status", measured.status);
            record.addProperty("attempts", measured.attempts);
            record.addProperty("final_correct", measured.finalCorrect);
            record.addProperty("final_total", BlindSkillTransferExperiment.FINAL_CASES);
            record.addProperty("baseline_correct", measured.baselineCorrect);
            record.addProperty("skill_installed", false);
            record.addProperty("model_weights_updated", false);
            if (!save(app, record, cancel)) return "SIN_PERSISTENCIA";
            return "MEDIDO_" + measured.status;
        } catch (java.util.concurrent.CancellationException interrupted) {
            return "INTERRUMPIDO";
        } catch (Exception failure) {
            return "FALLO_CONTROLADO"; // no exception text or source prompt in reports
        }
    }

    public static String status(Context context) {
        JsonObject last = last(context);
        if (last == null) return "";
        try {
            if (last.get("schema").getAsInt() != 1) return "";
            int correct = last.get("final_correct").getAsInt();
            int total = last.get("final_total").getAsInt();
            int baseline = last.get("baseline_correct").getAsInt();
            if (total != BlindSkillTransferExperiment.FINAL_CASES || correct < 0
                    || correct > total || baseline < 0 || baseline > total) return "";
            String verdict = last.get("status").getAsString();
            if (!verdict.matches("(TRANSFER_VERIFIED|TRANSFER_FAILED|DEVELOPMENT_FAILED)")) return "";
            return "Ensayo semanal de transferencia con oráculo independiente: " + verdict
                    + ", " + correct + "/" + total + " aciertos (referencia "
                    + baseline + "/" + total + "). No instalé habilidades ni modifiqué Gemma.";
        } catch (RuntimeException broken) { return ""; }
    }

    private static File location(Context app) {
        return new File(new File(app.getNoBackupFilesDir(), "mind"), "night-transfer.json");
    }

    private static JsonObject last(Context context) {
        File file = location(context.getApplicationContext());
        if (!file.isFile() || file.length() > 4096) return null;
        try {
            return JsonParser.parseString(new String(new AtomicFile(file).readFully(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception invalid) { return null; }
    }

    private static boolean save(Context app, JsonObject report, BooleanSupplier stopped) {
        File dest = location(app);
        if (!dest.getParentFile().isDirectory() && !dest.getParentFile().mkdirs()) return false;
        AtomicFile atom = new AtomicFile(dest);
        FileOutputStream out = null;
        try {
            out = atom.startWrite();
            out.write(report.toString().getBytes(StandardCharsets.UTF_8));
            if (stopped.getAsBoolean()) { atom.failWrite(out); return false; }
            atom.finishWrite(out);
            return true;
        } catch (Exception failed) {
            if (out != null) atom.failWrite(out);
            return false;
        }
    }
}
