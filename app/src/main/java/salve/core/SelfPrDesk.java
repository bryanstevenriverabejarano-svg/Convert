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
 * «Abre un PR»: Salve elige un fallo repetido de su último examen, lee el archivo responsable tal cual
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
    String run(String entrada) {
        if (!motor.creatorSide()) return "Solo Bryan puede pedirme que proponga cambios en mi código.";
        String token = SelfPrTokenStore.token(motor.context);
        if (token == null) return "Para abrir PRs necesito un token de GitHub con permiso solo de issues. "
                + "Ponlo en Ajustes → IA y cámara → PRs de Salve (GitHub).";
        if (motor.llm == null || motor.llm.isLoading())
            return "Necesito mi modelo cargado para escribir el cambio. Espera a que diga «activo» y pídemelo otra vez.";
        JsonObject exam = lastExam();
        // During sleep Salve can select a priority. It has NO power to publish:
        // this path is reachable only by Bryan's explicit «abre un PR» command.
        // Revalidate the stored hint against the exact real exam before using it.
        JsonObject sleepCandidate = SleepImprovementRuntime.eligibleCandidate(motor.context, exam);
        JsonObject candidate = sleepCandidate != null ? sleepCandidate
                : salve.core.synergy.SynergyEvolution.candidate(exam);
        // Cambio dirigido: Bryan dice qué quiere y ella busca en su propio código dónde va.
        String goal = salve.core.selfpr.SelfChangeScope.goal(entrada);
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
        String[] what = goal != null ? null : salve.core.evaluation.ImprovementProposals.problem(issueId);
        String problem = goal != null ? goal : what == null ? issueId : what[0];
        String idea = goal != null ? "el cambio mínimo que lo consiga, sin quitar nada de lo que ya funciona"
                : what == null ? "arreglarlo con el cambio mínimo" : what[1];
        List<String> examples = goal != null ? new java.util.ArrayList<>()
                : salve.core.evaluation.ImprovementProposals.examples(exam, issueId, 3);
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
            motor.estado("Escribiendo mi parche para «" + problem + "» (intento " + attempt + " de " + ATTEMPTS + ")…");
            ModelResult result = motor.llm.generateResult(
                    SelfPatch.prompt(problem, idea, examples, path, excerpt, error, goal != null), SalveLLM.Role.PLANIFICADOR);
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
                                : problem + " (" + candidate.get("observed_failures").getAsInt() + " fallos en el examen). ")
                                + (reply.explanation.isEmpty() ? "" : "Salve: " + reply.explanation), true, true);
                motor.estado("Enviando mi propuesta a GitHub…");
                GitHubSelfPr.Submitted sent = github.openIssue(SelfPatch.issueTitle(problem, proposal),
                        SelfPatch.issueBody(problem, reply.explanation, examples, model.isEmpty() ? modelName() : model, proposal));
                try {
                    JsonObject lineageEntry = salve.core.selfpr.SelfLineage.entry(
                            System.currentTimeMillis(), goal != null ? "bryan" : "examen", problem, path, revision, sent.url,
                            salve.core.selfpr.SelfLineage.failedCases(exam));
                    lineageEntry.addProperty("casos_examen", salve.core.selfpr.SelfLineage.totalCases(exam));
                    salve.core.selfpr.SelfLineage.append(lineageFile(), lineageEntry);
                } catch (java.io.IOException unsaved) { Log.w(TAG, "No pude anotar el linaje"); }
                salve.core.trace.ExecutionTrace.note("PROPUESTA DE CÓDIGO ENVIADA", path + "\nRevisión: " + revision
                        + "\nIssue: " + sent.url + "\n\n" + patch);
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
