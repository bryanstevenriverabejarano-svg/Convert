package salve.core.evaluation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.mind.SkillLab;

/**
 * Nightly, low-power rehearsal of already-authorized local problem families.
 * Distinguishes execution, external verification and durable storage. Never
 * claims to modify the LLM's weights or creates unreviewed code.
 */
public final class NightPracticePolicy {
    private static final Pattern RESULT = Pattern.compile(
            "^Jugué ([1-3]) retos inventados por mí; resolví ([0-3])\\.");
    public static final int MAX_ROUNDS = 2; // one extra practice when verified failures dominate
    private NightPracticePolicy() { }

    public static final class Plan {
        public final int rounds;
        public final String reason;
        private Plan(int rounds, String reason) {
            this.rounds = rounds;
            this.reason = reason;
        }
    }

    public static final class Outcome {
        public final int attempted, solved;
        public final boolean saved;
        public final String reason;
        private Outcome(int attempted, int solved, boolean saved, String reason) {
            this.attempted = attempted;
            this.solved = solved;
            this.saved = saved;
            this.reason = reason;
        }
        public boolean persistedPractice() { return saved && attempted > 0; }
    }

    public static Plan choose(List<SkillLab.Outcome> history, long now) {
        int passed = 0, failed = 0;
        if (history != null) {
            int first = Math.max(0, history.size() - 12);
            for (int i = first; i < history.size(); i++) {
                SkillLab.Outcome outcome = history.get(i);
                if (outcome == null || outcome.verifier == null
                        || outcome.verifier.trim().isEmpty()
                        || outcome.at < 0 || outcome.at > now) continue;
                if (outcome.ok) passed++; else failed++;
            }
        }
        // Extra exploration only when there are at least three failures and
        // failure rate exceeds success rate. Not a claim of generic learning.
        return failed >= 3 && failed > passed
                ? new Plan(MAX_ROUNDS, "repetir_habilidad_con_fallos_verificados")
                : new Plan(1, "practica_local_limitada");
    }

    /** Parse only the trusted local PlayCycle summary, never LLM prose. */
    public static Outcome observed(String output, int requestedRounds) {
        if (output == null || requestedRounds < 1 || requestedRounds > MAX_ROUNDS)
            return new Outcome(0, 0, false, "sin_resultado_verificable");
        Matcher m = RESULT.matcher(output);
        if (!m.find()) return new Outcome(0, 0, false, "sin_resultado_verificable");
        try {
            int attempts = Integer.parseInt(m.group(1));
            int solved = Integer.parseInt(m.group(2));
            if (attempts > requestedRounds || solved > attempts)
                return new Outcome(0, 0, false, "resultado_inconsistente");
            boolean durable = !output.contains("no pude guardar mi progreso del juego");
            return new Outcome(attempts, solved, durable,
                    durable ? "verificado_y_guardado" : "verificado_no_persistido");
        } catch (NumberFormatException bad) {
            return new Outcome(0, 0, false, "resultado_invalido");
        }
    }
}
