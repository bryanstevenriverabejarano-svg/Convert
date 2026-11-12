package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.JsonObject;
import java.util.function.BooleanSupplier;
import salve.core.selfpr.SelfExamPolicy;
import salve.core.selfpr.SelfLineage;
import salve.core.selfpr.SelfVerification;

/**
 * Salve se examina sola para dar el veredicto de un cambio suyo: proponer → instalar → medir → veredicto,
 * sin que Bryan tenga que pedir el examen. Lo llama el ciclo de sueño, dentro de su puerta.
 *
 * Usa el examen de siempre ({@link ConversationExamRunner}) en silencio. No abre PR, no revierte nada y no
 * toca la conversación: solo mide y deja el veredicto que {@link SelfVerification} guarda.
 */
public final class SelfExam {
    private static final String PREFS = "salve_self_exam";
    private SelfExam() { }

    /** @return lo que pasó, en una frase para el registro; "" si no había nada que comprobar. */
    public static String runIfPending(Context context, BooleanSupplier stop) {
        Context app = context.getApplicationContext();
        JsonObject waiting = SelfVerification.pending(SelfLineage.read(SelfVerification.lineageFile(app), 5),
                SelfLineage.read(SelfVerification.verdictsFile(app), 50), SelfVerification.appUpdatedAt(app));
        MotorConversacional motor = MotorConversacional.current();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String issue = waiting == null || !waiting.has("issue") ? "" : waiting.get("issue").getAsString();
        boolean sameChange = issue.equals(prefs.getString("issue", ""));
        long now = System.currentTimeMillis();
        // Un arreglo de un error del registro no se mide con el examen: se mira si ese error vuelve a aparecer.
        if (waiting != null && salve.core.selfpr.EvolutionCycle.VERIFY_ERRORS.equals(
                waiting.has("verificar") ? waiting.get("verificar").getAsString() : "")) {
            JsonObject verdict = salve.core.selfpr.EvolutionSignals.errorVerdict(waiting,
                    salve.core.trace.ExecutionTrace.journal(app).recent(), SelfVerification.appUpdatedAt(app),
                    System.currentTimeMillis());
            if (verdict == null) return "Mi último cambio arregla un error de mi registro: espero a que esa tarea se use "
                    + salve.core.selfpr.EvolutionSignals.RUNS_TO_JUDGE + " veces con la versión nueva para ver si vuelve a fallar.";
            try { SelfLineage.append(SelfVerification.verdictsFile(app), verdict); }
            catch (java.io.IOException unsaved) { return "No pude guardar el veredicto de mi último cambio."; }
            return "Comprobé mi último cambio en mi registro: " + SelfVerification.sentence(verdict);
        }
        String reason = SelfExamPolicy.reasonNotToRun(waiting != null, motor != null,
                motor != null && motor.llm != null && !motor.llm.isLoading(),
                sameChange ? prefs.getLong("attempt_at", 0L) : 0L, sameChange ? prefs.getInt("attempts", 0) : 0, now);
        if (reason != null) return reason;
        prefs.edit().putString("issue", issue).putLong("attempt_at", now)
                .putInt("attempts", (sameChange ? prefs.getInt("attempts", 0) : 0) + 1).apply();
        String report;
        try { report = motor.examen.runQuietly(stop); }
        catch (RuntimeException failed) {
            return "Empecé a examinarme y falló (" + failed.getClass().getSimpleName() + "); no guardé ninguna nota.";
        }
        if (report == null) return "Empecé a examinarme y tuve que parar (volviste, o el modelo no estaba listo); no guardé ninguna nota.";
        // El veredicto ya quedó guardado al terminar el examen; aquí solo se cuenta.
        String verdict = SelfVerification.state(app);
        return "Me examiné sola con mi modelo de verdad para comprobar mi último cambio. " + verdict;
    }

    /**
     * Evolución autónoma, dentro de la misma puerta de sueño: primero revierte un cambio suyo que empeoró el
     * examen; si no, propone uno nuevo solo con las cinco fases cumplidas. Bryan lo apaga en la pestaña Evolución.
     *
     * @return lo que pasó, para el registro de ejecución.
     */
    public static String evolveIfReady(Context context, BooleanSupplier stop) {
        Context app = context.getApplicationContext();
        if (!salve.core.selfpr.EvolutionCycle.autonomous(app)) return "Evolución autónoma apagada por Bryan.";
        MotorConversacional motor = MotorConversacional.current();
        java.util.List<JsonObject> lineage = SelfLineage.read(SelfVerification.lineageFile(app), 50);
        JsonObject revert = salve.core.selfpr.EvolutionCycle.revertTarget(lineage,
                SelfLineage.read(SelfVerification.verdictsFile(app), 50));
        salve.core.selfpr.EvolutionCycle.Plan plan = salve.core.selfpr.EvolutionCycle.load(app);
        if (revert == null && !plan.ready) {
            salve.core.selfpr.EvolutionCycle.Phase at = plan.phase(plan.current);
            return "Fase " + at.number + " (" + at.name + "): " + at.reasoning;
        }
        if (motor == null) return "Podría actuar, pero mi motor de conversación no está despierto en este proceso.";
        if (stop.getAsBoolean()) return "";
        if (revert != null) return motor.selfPr.revert(revert);
        if (motor.llm == null || motor.llm.isLoading()) return "Mis cinco fases están cumplidas, pero mi modelo no está cargado.";
        return motor.selfPr.runAutonomous(plan, stop);
    }
}
