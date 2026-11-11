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
}
