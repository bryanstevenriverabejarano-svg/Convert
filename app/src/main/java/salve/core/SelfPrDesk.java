package salve.core;

import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import salve.core.evaluation.SleepImprovementRuntime;
import salve.core.selfpr.GitHubSelfPr;
import salve.core.selfpr.SelfPatch;
import salve.core.selfpr.SelfPrTokenStore;

/**
 * «Abre un PR» (o, mientras duerme, sus cinco fases de evolución): Salve elige un fallo repetido de su último examen, lee el archivo responsable tal cual
 * está en GitHub, escribe un cambio con su propio modelo y lo envía como issue. GitHub Actions lo prueba
 * y, si pasa la compilación completa de Android, lo fusiona sola (Bryan puede apagarlo con SALVE_AUTOMERGE=false).
 */
final class SelfPrDesk {
    private static final String TAG = "Salve/SelfPr";
    private static final int ATTEMPTS = 2;
    private final MotorConversacional motor;

    SelfPrDesk(MotorConversacional motor) { this.motor = motor; }

    static boolean isCommand(String input) { return salve.core.selfpr.SelfPrCommands.isCommand(input); }

    static boolean isStatusCommand(String input) { return salve.core.selfpr.SelfPrCommands.isStatusCommand(input); }

    /** Hace todo el ciclo (minutos: descarga y una o dos inferencias) y devuelve lo que pasó, sin adornos. */
    String run() { return run(null); }

    /** {@code entrada}: la frase de Bryan; si trae un objetivo («abre un PR para que…») el cambio es dirigido. */
    String run(String entrada) { return run(entrada, null, () -> false); }

    /**
     * Mientras duerme, sin que nadie se lo pida: solo con las cinco fases cumplidas ({@link
     * salve.core.selfpr.EvolutionCycle#decide}). Nunca se llega aquí desde una frase del chat.
     */
    String runAutonomous(salve.core.selfpr.EvolutionCycle.Plan plan, java.util.function.BooleanSupplier stop) {
        if (plan == null || !plan.ready || plan.candidate == null) return "Mis cinco fases no están cumplidas; no cambio nada.";
        return run(null, plan.candidate, stop);
    }

    private String run(String entrada, JsonObject autonomousCandidate, java.util.function.BooleanSupplier stop) {
        boolean autonomous = autonomousCandidate != null;
        if (!autonomous && !motor.creatorSide()) return "Solo Bryan puede pedirme que proponga cambios en mi código.";
        String token = SelfPrTokenStore.token(motor.context);
        if (token == null) return "Para abrir PRs necesito un token de GitHub con permiso solo de issues. "
                + "Ponlo en Ajustes → IA y cámara → PRs de Salve (GitHub).";
        if (motor.llm == null || motor.llm.isLoading())
            return "Necesito mi modelo cargado para escribir el cambio. Espera a que diga «activo» y pídemelo otra vez.";
        JsonObject exam = lastExam();
        // Desde el chat solo con la orden explícita de Bryan («abre un PR»). Sola, únicamente desde el sueño y
        // con las cinco fases cumplidas (EvolutionCycle), que ya revalidó el candidato contra el examen real.
        JsonObject sleepCandidate = SleepImprovementRuntime.eligibleCandidate(motor.context, exam);
        JsonObject candidate = autonomous ? autonomousCandidate : sleepCandidate != null ? sleepCandidate
                : salve.core.synergy.SynergyEvolution.candidate(exam);
        // Cambio dirigido: Bryan dice qué quiere y ella busca en su propio código dónde va.
        String goal = autonomous ? null : salve.core.selfpr.SelfChangeScope.goal(entrada);
        if (goal != null) {
            String chosen = salve.core.selfpr.SelfChangeScope.choose(
                    salve.core.mind.SelfCodeLoader.get(motor.context), goal);
            if (chosen == null) return "He buscado en mi código y no encuentro un módulo que pueda tocar para «" + goal
                    + "». Puedo cambiar casi todo mi núcleo, pero no mi examen, la puerta de mis PR, la identidad ni los datos privados. "
                    + "Dime el nombre del archivo o descríbelo de otra forma.";
            candidate = new JsonObject();
            candidate.addProperty("issue_id", "");
            candidate.addProperty("source_module_candidate", chosen);
            candidate.addProperty("observed_failures", 0);
        }
        if (candidate == null) return exam == null
                ? "Primero necesito examinarme para saber qué arreglar: dime «hazte el examen» y luego «abre un PR»."
                : "En mi último examen no hay un fallo repetido (al menos dos casos) en un módulo que sepa tocar. "
                + "No voy a cambiar código sin una razón medida.";
        String issueId = candidate.get("issue_id").getAsString();
        String path = candidate.get("source_module_candidate").getAsString();
        // Fuera del examen (registro, revisión de su código, investigación, sueño) el candidato trae su propio
        // problema, idea y pruebas (EvolutionSignals); del examen salen del catálogo de siempre.
        String origin = text(candidate, "fuente"), verify = text(candidate, "verificar");
        boolean observedElsewhere = goal == null && !origin.isEmpty() && !"Examen".equals(origin);
        String[] what = goal != null || observedElsewhere ? null : salve.core.evaluation.ImprovementProposals.problem(issueId);
        String problem = goal != null ? goal : observedElsewhere ? text(candidate, "problem") : what == null ? issueId : what[0];
        String idea = goal != null ? "el cambio que lo consiga, sin quitar nada de lo que ya funciona"
                : observedElsewhere ? text(candidate, "idea") : what == null ? "arreglarlo de verdad" : what[1];
        List<String> examples = new java.util.ArrayList<>();
        if (observedElsewhere && candidate.has("examples") && candidate.get("examples").isJsonArray())
            for (com.google.gson.JsonElement e : candidate.getAsJsonArray("examples")) examples.add(e.getAsString());
        else if (goal == null) examples = salve.core.evaluation.ImprovementProposals.examples(exam, issueId, 3);
        if (!salve.core.selfpr.SelfChangeScope.allowed(path) && goal != null)
            return "Ese archivo no lo puedo tocar yo: es parte de lo que me mide o me autoriza.";

        GitHubSelfPr github = new GitHubSelfPr(token);
        String revision, source;
        try {
            motor.estado("Leyendo mi código en GitHub: " + path + "…");
            revision = github.headRevision();
            source = github.source(revision, path);
        } catch (java.io.IOException failed) {
            return "No pude leer mi código en GitHub: " + failed.getMessage() + ".";
        }

        int budget = Math.max(1200, Math.min(2600, motor.llm.getConversationPromptBudgetChars() - 1200));
        String excerpt = SelfPatch.excerpt(source, SelfPatch.keywords(issueId,
                goal != null ? java.util.Collections.singletonList(goal) : examples), budget);
        String error = null, model = "";
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            if (motor.closed) return "Lo dejé a medias porque se cerró la sesión.";
            if (stop.getAsBoolean()) return "Lo dejé a medias: tuve que parar (volviste o dejé de cargar). No envié nada.";
            motor.estado("Escribiendo mi parche para «" + problem + "» (intento " + attempt + " de " + ATTEMPTS + ")…");
            // Con un nodo propio (un modelo mayor en el PC) le enseño mucho más de mi código y el parche lo
            // escribe él; si no hay nodo o falla, lo escribo yo en el teléfono con la ventana pequeña de siempre.
            ModelResult result = null;
            if (motor.llm.isDistributedEnabled()) {
                String wide = SelfPatch.excerpt(source, SelfPatch.keywords(issueId,
                        goal != null ? java.util.Collections.singletonList(goal) : examples), SelfPatch.NODE_WINDOW_CHARS);
                result = motor.llm.generateOnNodeOnly(
                        SelfPatch.prompt(problem, idea, examples, path, wide, error, goal != null, origin), SalveLLM.Role.PLANIFICADOR);
                if (!result.isSuccess()) result = null;
            }
            if (result == null) result = motor.llm.generateResult(
                    SelfPatch.prompt(problem, idea, examples, path, excerpt, error, goal != null, origin), SalveLLM.Role.PLANIFICADOR);
            if (result == null || !result.isSuccess()) {
                error = "mi modelo no respondió" + (result == null || result.getError() == null ? "" : ": " + result.getError());
                continue;
            }
            model = result.getProvider() == null ? "" : result.getProvider();
            try {
                SelfPatch.Reply reply = SelfPatch.parse(result.getText());
                String changed = SelfPatch.apply(source, reply.edits);
                String syntax = SelfPatch.syntaxProblem(source, changed);
                if (syntax != null) throw new SelfPatch.PatchError(syntax);
                List<String> risks = SelfPatch.risks(source, changed);
                if (!risks.isEmpty()) throw new SelfPatch.PatchError("añade llamadas no permitidas: " + String.join(", ", risks));
                String patch = SelfPatch.unifiedDiff(path, source, changed);
                JsonObject proposal = SelfPatch.proposal(path, revision, source, patch,
                        (goal != null ? "Cambio pedido por Bryan: " + problem + ". "
                                : observedElsewhere ? problem + " (" + origin + "). "
                                : problem + " (" + candidate.get("observed_failures").getAsInt() + " fallos en el examen). ")
                                + (reply.explanation.isEmpty() ? "" : "Salve: " + reply.explanation), true, true);
                motor.estado("Enviando mi propuesta a GitHub…");
                GitHubSelfPr.Submitted sent = github.openIssue(SelfPatch.issueTitle(problem, proposal),
                        SelfPatch.issueBody(problem, reply.explanation, examples, model.isEmpty() ? modelName() : model, proposal));
                try {
                    JsonObject lineageEntry = salve.core.selfpr.SelfLineage.entry(
                            System.currentTimeMillis(), autonomous ? salve.core.selfpr.EvolutionCycle.ORIGIN_AUTONOMOUS
                                    : goal != null ? "bryan" : "examen", problem, path, revision, sent.url,
                            salve.core.selfpr.EvolutionCycle.VERIFY_ERRORS.equals(verify)
                                    ? candidate.get("observed_failures").getAsInt()
                                    : salve.core.selfpr.SelfLineage.failedCases(exam));
                    if (!origin.isEmpty()) lineageEntry.addProperty("fuente", origin);
                    if (!verify.isEmpty()) lineageEntry.addProperty("verificar", verify);
                    if (candidate.has("firma")) lineageEntry.addProperty("firma", text(candidate, "firma"));
                    lineageEntry.addProperty("casos_examen", salve.core.selfpr.SelfLineage.totalCases(exam));
                    // Lo que cambió, tal cual: así puede deshacerlo sola si el examen empeora.
                    lineageEntry.add("cambios", salve.core.selfpr.EvolutionCycle.record(reply.edits));
                    salve.core.selfpr.SelfLineage.append(lineageFile(), lineageEntry);
                } catch (java.io.IOException unsaved) { Log.w(TAG, "No pude anotar el linaje"); }
                salve.core.trace.ExecutionTrace.note("PROPUESTA DE CÓDIGO ENVIADA", path + "\nRevisión: " + revision
                        + "\nIssue: " + sent.url + "\n\n" + patch);
                if (autonomous) return "Cumplí mis cinco fases y propuse sola un cambio para «" + problem + "» en "
                        + path.substring(path.lastIndexOf('/') + 1) + ": " + sent.url + ". GitHub lo prueba y lo fusiona si pasa; "
                        + "después me examino con la versión nueva." + (reply.explanation.isEmpty() ? "" : " Lo que cambio: " + reply.explanation);
                return "He escrito mi propuesta para «" + problem + "» en " + path.substring(path.lastIndexOf('/') + 1)
                        + " y la he enviado: " + sent.url + "\nGitHub Actions la va a probar. Si pasa las pruebas y la compilación "
                        + "completa de Android, la fusiono yo sola y se genera un APK nuevo. Pregúntame «¿cómo van tus PR?» para saber el resultado."
                        + (reply.explanation.isEmpty() ? "" : "\nLo que cambio: " + reply.explanation);
            } catch (SelfPatch.PatchError rejected) {
                error = rejected.getMessage();
                Log.i(TAG, "Intento " + attempt + " descartado: " + error);
            } catch (java.io.IOException failed) {
                return "Escribí el parche pero no pude enviarlo a GitHub: " + failed.getMessage() + ".";
            }
        }
        salve.core.trace.ExecutionTrace.note("PROPUESTA DE CÓDIGO DESCARTADA", path + "\n" + error);
        return "Lo intenté " + ATTEMPTS + " veces con «" + problem + "» y no me salió un parche válido (" + error
                + "). No he enviado nada a GitHub. Puedes verlo en el Registro de ejecución.";
    }

    /**
     * Deshace un cambio suyo que empeoró el examen: aplica sus ediciones al revés sobre el código de hoy y lo
     * envía por la misma puerta (GitHub prueba y fusiona). Si el código cambió desde entonces y no encaja, no
     * envía nada y lo deja dicho para Bryan.
     */
    String revert(JsonObject change) {
        String token = SelfPrTokenStore.token(motor.context);
        if (token == null || change == null) return "";
        String path = change.has("archivo") ? change.get("archivo").getAsString() : "";
        String goal = change.has("objetivo") ? change.get("objetivo").getAsString() : "";
        if (!salve.core.selfpr.SelfChangeScope.allowed(path)) return "";
        java.util.List<SelfPatch.Edit> undo = salve.core.selfpr.EvolutionCycle.inverse(change);
        if (undo.isEmpty()) return "Mi último cambio empeoró el examen, pero no guardé sus ediciones: hay que revertirlo a mano ("
                + (change.has("issue") ? change.get("issue").getAsString() : path) + ").";
        GitHubSelfPr github = new GitHubSelfPr(token);
        try {
            String revision = github.headRevision();
            String source = github.source(revision, path);
            String reverted = SelfPatch.apply(source, undo);
            String syntax = SelfPatch.syntaxProblem(source, reverted);
            if (syntax != null) throw new SelfPatch.PatchError(syntax);
            String patch = SelfPatch.unifiedDiff(path, source, reverted);
            JsonObject verdict = salve.core.selfpr.EvolutionCycle.verdictFor(
                    salve.core.selfpr.SelfLineage.read(salve.core.selfpr.SelfVerification.verdictsFile(motor.context), 50),
                    change.has("issue") ? change.get("issue").getAsString() : "");
            String problem = "Revertir: " + (goal.isEmpty() ? path.substring(path.lastIndexOf('/') + 1) : goal);
            String why = "Mi cambio anterior empeoró el examen"
                    + (verdict == null ? "" : " (de " + verdict.get("antes").getAsInt() + " a " + verdict.get("ahora").getAsInt() + " casos fallidos)")
                    + ". Lo deshago tal cual.";
            JsonObject proposal = SelfPatch.proposal(path, revision, source, patch, why, true, true);
            GitHubSelfPr.Submitted sent = github.openIssue(SelfPatch.issueTitle(problem, proposal),
                    SelfPatch.issueBody(problem, why, new java.util.ArrayList<>(), modelName(), proposal));
            JsonObject entry = salve.core.selfpr.SelfLineage.entry(System.currentTimeMillis(),
                    salve.core.selfpr.EvolutionCycle.ORIGIN_REVERT, problem, path, revision, sent.url,
                    verdict == null ? -1 : verdict.get("ahora").getAsInt());
            JsonObject exam = lastExam();
            entry.addProperty("casos_examen", salve.core.selfpr.SelfLineage.totalCases(exam));
            entry.add("cambios", salve.core.selfpr.EvolutionCycle.record(undo));
            salve.core.selfpr.SelfLineage.append(lineageFile(), entry);
            salve.core.trace.ExecutionTrace.note("REVERSIÓN ENVIADA", path + "\nIssue: " + sent.url + "\n\n" + patch);
            return "Mi último cambio empeoró el examen, así que lo he revertido yo sola: " + sent.url
                    + ". GitHub lo prueba y lo fusiona si pasa.";
        } catch (SelfPatch.PatchError | java.io.IOException failed) {
            salve.core.trace.ExecutionTrace.note("REVERSIÓN NO ENVIADA", path + "\n" + failed.getMessage());
            return "Mi último cambio empeoró el examen y no pude revertirlo sola (" + failed.getMessage()
                    + "). Hay que revertirlo a mano: " + (change.has("issue") ? change.get("issue").getAsString() : path) + ".";
        }
    }

    String status() {
        String token = SelfPrTokenStore.token(motor.context);
        if (token == null) return "Aún no tengo token de GitHub, así que no he abierto ninguna propuesta.";
        try {
            List<GitHubSelfPr.Status> recent = new GitHubSelfPr(token).recent(5);
            if (recent.isEmpty()) return "Todavía no he enviado ninguna propuesta a GitHub. Dime «abre un PR» cuando quieras.";
            StringBuilder out = new StringBuilder("Mis últimas propuestas:");
            for (GitHubSelfPr.Status s : recent) out.append("\n• ").append(s.describe());
            String lineage = salve.core.selfpr.SelfLineage.describe(salve.core.selfpr.SelfLineage.read(lineageFile(), 5),
                    salve.core.selfpr.SelfLineage.failedCases(lastExam()));
            if (!lineage.isEmpty()) out.append("\n\n").append(lineage);
            String verification = salve.core.selfpr.SelfVerification.state(motor.context);
            if (!verification.isEmpty()) out.append("\n\n").append(verification);
            return out.toString();
        } catch (java.io.IOException failed) {
            return "No pude consultar GitHub: " + failed.getMessage() + ".";
        }
    }

    private static String text(JsonObject o, String key) {
        return o != null && o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : "";
    }

    private File lineageFile() { return new File(new File(motor.context.getFilesDir(), "selfpr"), "linaje.jsonl"); }

    private String modelName() {
        try { return motor.getModelStatusDescription(); } catch (RuntimeException unknown) { return ""; }
    }

    private JsonObject lastExam() {
        File file = new File(new File(motor.context.getFilesDir(), "exam"), "ultimo.json");
        if (!file.isFile()) return null;
        try { return JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (Exception damaged) { return null; }
    }
}
