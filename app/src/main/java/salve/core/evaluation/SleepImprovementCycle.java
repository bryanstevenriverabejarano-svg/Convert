package salve.core.evaluation;

import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import salve.core.mind.SkillLab;
import salve.core.synergy.SynergyEvolution;

/**
 * Read-only nightly audit. Uses the SAME examination evidence and strategy journal as
 * the foreground, without inventing a new grading model or granting GitHub permissions.
 * Only bounded categorical metadata may reach persistent sleep reports.
 */
public final class SleepImprovementCycle {
    public static final long MAX_EXAM_AGE_MS = 14L * 24 * 60 * 60_000L;
    private SleepImprovementCycle() { }

    public static JsonObject assess(JsonObject exam, List<SkillLab.Outcome> history,
                                    List<SkillLab.Decision> decisions, boolean practiceRan, long now) {
        JsonObject out = new JsonObject();
        out.addProperty("schema", 1);
        out.addProperty("evaluated_at", Math.max(0L, now));
        out.addProperty("practice_ran", practiceRan);
        int observed = 0, passed = 0, failed = 0, adopted = 0;
        if (history != null) {
            for (int i = Math.max(0, history.size() - 20); i < history.size(); i++) {
                SkillLab.Outcome o = history.get(i);
                if (o == null || o.verifier == null || o.verifier.trim().isEmpty()
                        || o.at > now || o.at < 0) continue;
                observed++;
                if (o.ok) passed++; else failed++;
            }
        }
        if (decisions != null) {
            for (SkillLab.Decision d : decisions)
                if (d != null && "adoptado".equals(d.verdict) && d.at >= 0 && d.at <= now) adopted++;
        }
        out.addProperty("skill_observations", observed);
        out.addProperty("skill_successes", passed);
        out.addProperty("skill_failures", failed);
        out.addProperty("skill_adoptions", adopted);

        long examAt = timestamp(exam);
        out.addProperty("exam_at", examAt);
        String status = examAt <= 0 ? "MISSING" : examAt > now + 60_000L
                || now - examAt > MAX_EXAM_AGE_MS ? "STALE" : "CURRENT";
        out.addProperty("exam_status", status);
        JsonObject candidate = "CURRENT".equals(status) ? SynergyEvolution.candidate(exam) : null;
        if (candidate != null) {
            out.addProperty("stage", "READY_FOR_EXPLICIT_PR");
            out.addProperty("issue_id", candidate.get("issue_id").getAsString());
            out.addProperty("observed_failures", candidate.get("observed_failures").getAsInt());
            out.addProperty("source_module_candidate", candidate.get("source_module_candidate").getAsString());
        } else out.addProperty("stage", "OBSERVING");
        return out;
    }

    /** A stored hint is never authority: re-derive the candidate from the same current exam. */
    public static JsonObject candidateFor(JsonObject report, JsonObject exam) {
        if (report == null || exam == null) return null;
        try {
            if (!report.has("schema") || report.get("schema").getAsInt() != 1
                    || !"CURRENT".equals(report.get("exam_status").getAsString())
                    || !"READY_FOR_EXPLICIT_PR".equals(report.get("stage").getAsString())
                    || timestamp(exam) <= 0 || timestamp(exam) != report.get("exam_at").getAsLong())
                return null;
            JsonObject actual = SynergyEvolution.candidate(exam);
            return actual != null
                    && actual.get("issue_id").getAsString().equals(report.get("issue_id").getAsString())
                    && actual.get("source_module_candidate").getAsString().equals(report.get("source_module_candidate").getAsString())
                    && actual.get("observed_failures").getAsInt() == report.get("observed_failures").getAsInt()
                    ? actual : null;
        } catch (RuntimeException invalid) { return null; }
    }

    private static long timestamp(JsonObject exam) {
        if (exam == null || !exam.has("at")) return 0;
        try { return Math.max(0L, exam.get("at").getAsLong()); }
        catch (RuntimeException bad) { return 0; }
    }

    public static boolean asksStatus(String input) {
        String s = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "");
        return s.matches("(?:que|cual) (?:aprendiste|has aprendido) (?:mientras dormias|durmiendo|en el sueno)")
                || s.matches("(?:muestrame |dame |ensename )?(?:tu )?(?:informe|resumen) (?:de|del) (?:sueno|mejoras nocturnas)")
                || s.matches("(?:que hiciste|en que pensaste) (?:mientras dormias|durmiendo)")
                || s.matches("(?:tus )?mejoras nocturnas");
    }

    public static String describe(JsonObject report) {
        if (report == null) return "Todavía no tengo un informe de mejora nocturna guardado.";
        try {
            if (report.get("schema").getAsInt() != 1) return "No pude interpretar el informe de sueño.";
            StringBuilder text = new StringBuilder("Mi último análisis nocturno: ");
            text.append(report.get("practice_ran").getAsBoolean() ? "practiqué un reto local." : "no pude practicar un reto nuevo.");
            int count = report.get("skill_observations").getAsInt();
            text.append(" Mi laboratorio conserva ").append(count)
                .append(" observaciones verificadas recientes (")
                .append(report.get("skill_successes").getAsInt()).append(" favorables y ")
                .append(report.get("skill_failures").getAsInt()).append(" fallidas). ");
            String status = report.get("exam_status").getAsString();
            if ("CURRENT".equals(status) && "READY_FOR_EXPLICIT_PR".equals(report.get("stage").getAsString())) {
                text.append("En mi último examen válido encontré ")
                    .append(report.get("observed_failures").getAsInt())
                    .append(" fallos repetidos del tipo ").append(report.get("issue_id").getAsString())
                    .append(". Preparé la prioridad, pero NO he creado ni fusionado código. ")
                    .append("Dime «abre un PR» si quieres que intente un parche con esa evidencia.");
            } else if ("STALE".equals(status))
                text.append("Mi examen conversacional está desactualizado: no sería fiable generar un parche nuevo desde él.");
            else if ("MISSING".equals(status))
                text.append("Todavía no tengo un examen conversacional real para escoger un parche.");
            else text.append("El examen no señala un fallo repetido que permita proponer un parche.");
            text.append(" Esta revisión reutiliza resultados existentes; no es un examen conversacional nuevo.");
            return text.toString();
        } catch (RuntimeException invalid) { return "El informe nocturno está incompleto y no lo daré por válido."; }
    }
}
