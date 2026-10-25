package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Lo que Salve hace en GitHub: leer su código y abrir una issue. Nada más. */
public class GitHubSelfPrTest {
    private static final String SHA = "0123456789abcdef0123456789abcdef01234567";

    @Test public void readsTheExactSourceAndOpensOnlyAnIssue() throws Exception {
        List<String> calls = new ArrayList<>();
        GitHubSelfPr github = new GitHubSelfPr("github_pat_test", (method, url, token, accept, body) -> {
            calls.add(method + " " + url);
            assertEquals("github_pat_test", token);
            if (url.endsWith("/commits/main")) return new GitHubSelfPr.Response(200, "{\"sha\":\"" + SHA + "\"}");
            if (url.contains("/contents/")) {
                assertEquals("application/vnd.github.raw", accept);
                return new GitHubSelfPr.Response(200, "class X {}\n");
            }
            if (method.equals("POST")) {
                JsonObject issue = JsonParser.parseString(body).getAsJsonObject();
                assertTrue(issue.get("title").getAsString().startsWith("[Salve] "));
                return new GitHubSelfPr.Response(201, "{\"number\":7,\"html_url\":\"https://github.com/o/r/issues/7\"}");
            }
            return new GitHubSelfPr.Response(500, "");
        });
        assertEquals(SHA, github.headRevision());
        assertEquals("class X {}\n", github.source(SHA, "app/src/main/java/salve/core/conversation/TurnChecks.java"));
        GitHubSelfPr.Submitted sent = github.openIssue("[Salve] Propuesta", "cuerpo");
        assertEquals(7, sent.number);
        for (String call : calls) {
            assertTrue(call, call.contains("api.github.com/repos/bryanstevenriverabejarano-svg/Convert/"));
            assertFalse("nunca toca PRs, ramas ni fusiones", call.matches(".*/(pulls|merges|git/refs|merge).*"));
        }
        assertEquals("solo una escritura: la issue", 1, calls.stream().filter(c -> c.startsWith("POST")).count());
    }

    @Test public void refusesPathsOutsideItsCore() {
        GitHubSelfPr github = new GitHubSelfPr("t", (m, u, t, a, b) -> new GitHubSelfPr.Response(200, "x"));
        for (String path : new String[]{"app/build.gradle", "app/src/main/java/salve/core/../../../../.github/workflows/x.java",
                "app/src/main/AndroidManifest.xml", "scripts/auto_improvement_runner.py"}) {
            try { github.source(SHA, path); fail(path); } catch (IOException expected) { }
        }
    }

    @Test public void explainsTokenProblemsPlainly() {
        GitHubSelfPr expired = new GitHubSelfPr("t", (m, u, t, a, b) -> new GitHubSelfPr.Response(401, "{}"));
        try { expired.headRevision(); fail(); } catch (IOException e) { assertTrue(e.getMessage().contains("token")); }
        GitHubSelfPr noIssues = new GitHubSelfPr("t", (m, u, t, a, b) -> new GitHubSelfPr.Response(403, "{}"));
        try { noIssues.openIssue("[Salve] x", "y"); fail(); } catch (IOException e) { assertTrue(e.getMessage().contains("issues")); }
    }

    @Test public void statusComesFromTheWorkflowComment() throws Exception {
        GitHubSelfPr github = new GitHubSelfPr("t", (method, url, token, accept, body) -> {
            if (url.contains("/issues?")) return new GitHubSelfPr.Response(200, "["
                    + "{\"number\":9,\"title\":\"[Salve] Propuesta para TurnChecks: Devuelve la pregunta\",\"state\":\"closed\",\"comments\":1},"
                    + "{\"number\":8,\"title\":\"Otra cosa\",\"state\":\"open\",\"comments\":0},"
                    + "{\"number\":6,\"title\":\"[Salve] Propuesta para MemoryRecall: x\",\"state\":\"open\",\"comments\":0},"
                    + "{\"number\":5,\"title\":\"[Salve] Propuesta para ReplyReviewer: y\",\"state\":\"closed\",\"comments\":1}]");
            if (url.contains("/issues/5/comments")) return new GitHubSelfPr.Response(200,
                    "[{\"body\":\"<!-- salve-self-pr-result: merged -->\\n<!-- salve-self-pr-pr: https://github.com/o/r/pull/238 -->\"}]");
            if (url.contains("/issues/9/comments")) return new GitHubSelfPr.Response(200,
                    "[{\"body\":\"Probado.\\n<!-- salve-self-pr-result: passed -->\\n<!-- salve-self-pr-pr: https://github.com/o/r/pull/240 -->\"}]");
            return new GitHubSelfPr.Response(404, "");
        });
        List<GitHubSelfPr.Status> recent = github.recent(5);
        assertEquals(3, recent.size());
        assertEquals("merged", recent.get(2).outcome);
        assertTrue(recent.get(2).describe().contains("fusionada en main"));
        assertEquals("passed", recent.get(0).outcome);
        assertEquals("https://github.com/o/r/pull/240", recent.get(0).pr);
        assertTrue(recent.get(0).describe().contains("pasó las pruebas"));
        assertTrue(recent.get(1).describe().contains("en cola"));
    }

    @Test public void onlyGitHubTokensAreAccepted() {
        assertTrue(GitHubSelfPr.validToken("github_pat_11ABCDEFG0123456789_abcdefghijklmnopqrstuvwxyz"));
        assertTrue(GitHubSelfPr.validToken("ghp_abcdefghijklmnopqrstuvwxyz0123456789"));
        assertFalse(GitHubSelfPr.validToken("hola"));
        assertFalse(GitHubSelfPr.validToken("github_pat_ con espacios y más cosas largas"));
        assertFalse(GitHubSelfPr.validToken(null));
    }

    @Test public void bryanCanAskInHisOwnWords() {
        for (String yes : new String[]{"Abre un PR", "salve, haz un pull request", "Propón un parche", "hazte un PR por favor",
                "prográmate y abre un PR", "crea tu propio PR"})
            assertTrue(yes, SelfPrCommands.isCommand(yes));
        for (String no : new String[]{"abre la puerta", "haz un dibujo", "qué es un PR", "propuestas de mejora"})
            assertFalse(no, SelfPrCommands.isCommand(no));
        assertTrue(SelfPrCommands.isStatusCommand("¿Cómo van tus PR?"));
        assertTrue(SelfPrCommands.isStatusCommand("estado de tus propuestas en GitHub"));
        assertFalse(SelfPrCommands.isStatusCommand("¿cómo van tus cuentos?"));
    }
}
