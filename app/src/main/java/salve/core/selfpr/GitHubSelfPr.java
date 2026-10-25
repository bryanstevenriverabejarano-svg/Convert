package salve.core.selfpr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo único que Salve hace en GitHub desde el teléfono: leer su propio código público y abrir una issue
 * con su propuesta. El token solo tiene permiso de issues. El flujo de GitHub Actions prueba el parche,
 * abre el PR y lo fusiona solo si la compilación completa de Android pasa (Bryan puede apagarlo con
 * SALVE_AUTOMERGE=false).
 */
public final class GitHubSelfPr {
    public static final String OWNER = "bryanstevenriverabejarano-svg", REPO = "Convert", BRANCH = "main";
    private static final String API = "https://api.github.com/repos/" + OWNER + "/" + REPO;
    private static final int MAX_BYTES = 400_000;

    /** Para las pruebas: sustituye la red. */
    public interface Transport {
        Response send(String method, String url, String token, String accept, String body) throws IOException;
    }

    public static final class Response {
        public final int code; public final String body;
        public Response(int code, String body) { this.code = code; this.body = body; }
    }

    public static final class Submitted {
        public final int number; public final String url;
        Submitted(int number, String url) { this.number = number; this.url = url; }
    }

    /** Estado de una propuesta, según el comentario que deja el flujo de GitHub. */
    public static final class Status {
        public final int number; public final String title, state, outcome, pr, detail;
        Status(int number, String title, String state, String outcome, String pr, String detail) {
            this.number = number; this.title = title; this.state = state; this.outcome = outcome; this.pr = pr; this.detail = detail;
        }
        public String describe() {
            String what = "merged".equals(outcome) ? "fusionada en main" + (pr.isEmpty() ? "" : " (" + pr + ")")
                    : "passed".equals(outcome) ? "pasó las pruebas" + (pr.isEmpty() ? "" : " → PR " + pr)
                    : "failed".equals(outcome) ? "no pasó las pruebas"
                    : "rejected".equals(outcome) ? "rechazada antes de probar"
                    : "open".equals(state) ? "en cola o probándose" : "cerrada";
            return "#" + number + " " + title.replace(SelfPatch.TITLE_PREFIX, "") + ": " + what
                    + (detail.isEmpty() ? "" : " (" + detail + ")");
        }
    }

    /** Tokens de GitHub: de grano fino (github_pat_) o clásicos (ghp_). */
    public static boolean validToken(String value) {
        return value != null && value.matches("(?:github_pat_|ghp_)[A-Za-z0-9_]{20,250}");
    }

    private final Transport transport;
    private final String token;

    public GitHubSelfPr(String token) { this(token, GitHubSelfPr::http); }
    public GitHubSelfPr(String token, Transport transport) { this.token = token; this.transport = transport; }

    /** SHA del último commit de main: la revisión exacta sobre la que escribe su parche. */
    public String headRevision() throws IOException {
        JsonObject commit = json(call("GET", API + "/commits/" + BRANCH, "application/vnd.github+json", null)).getAsJsonObject();
        String sha = commit.has("sha") ? commit.get("sha").getAsString() : "";
        if (!sha.matches("[0-9a-f]{40}")) throw new IOException("GitHub no devolvió una revisión válida");
        return sha;
    }

    /** El archivo tal cual está en esa revisión (bytes exactos: su SHA-256 lo comprueba el flujo). */
    public String source(String revision, String path) throws IOException {
        if (!revision.matches("[0-9a-f]{40}") || !path.matches("app/src/main/java/salve/core/[A-Za-z0-9_/]+\\.java"))
            throw new IOException("Ruta o revisión no permitidas");
        return call("GET", API + "/contents/" + path + "?ref=" + revision, "application/vnd.github.raw", null);
    }

    public Submitted openIssue(String title, String body) throws IOException {
        JsonObject request = new JsonObject();
        request.addProperty("title", title);
        request.addProperty("body", body);
        JsonObject created = json(call("POST", API + "/issues", "application/vnd.github+json", request.toString())).getAsJsonObject();
        return new Submitted(created.get("number").getAsInt(), created.get("html_url").getAsString());
    }

    private static final Pattern OUTCOME = Pattern.compile("salve-self-pr-result:\\s*(merged|passed|failed|rejected)");
    private static final Pattern PR = Pattern.compile("salve-self-pr-pr:\\s*(https://github\\.com/\\S+/pull/\\d+)");
    private static final Pattern DETAIL = Pattern.compile("salve-self-pr-detail:\\s*([^\\n]*)");

    /** Sus últimas propuestas y lo que dijo GitHub Actions de cada una. */
    public List<Status> recent(int max) throws IOException {
        JsonArray issues = json(call("GET", API + "/issues?state=all&per_page=30&creator="
                + URLEncoder.encode(OWNER, "UTF-8"), "application/vnd.github+json", null)).getAsJsonArray();
        List<Status> out = new ArrayList<>();
        for (JsonElement e : issues) {
            JsonObject issue = e.getAsJsonObject();
            String title = str(issue, "title");
            if (!title.startsWith(SelfPatch.TITLE_PREFIX) || issue.has("pull_request")) continue;
            int number = issue.get("number").getAsInt();
            String outcome = "", pr = "", detail = "";
            if (issue.has("comments") && issue.get("comments").getAsInt() > 0) {
                JsonArray comments = json(call("GET", API + "/issues/" + number + "/comments?per_page=10",
                        "application/vnd.github+json", null)).getAsJsonArray();
                for (JsonElement c : comments) {
                    String body = str(c.getAsJsonObject(), "body");
                    Matcher o = OUTCOME.matcher(body);
                    if (!o.find()) continue;
                    outcome = o.group(1);
                    Matcher p = PR.matcher(body); pr = p.find() ? p.group(1) : "";
                    Matcher d = DETAIL.matcher(body); detail = d.find() ? SelfPatch.clip(d.group(1), 160) : "";
                }
            }
            out.add(new Status(number, title, str(issue, "state"), outcome, pr, detail));
            if (out.size() >= max) break;
        }
        return out;
    }

    private String call(String method, String url, String accept, String body) throws IOException {
        Response r = transport.send(method, url, token, accept, body);
        if (r.code == 401) throw new IOException("GitHub no acepta el token (¿caducado o mal copiado?)");
        if (r.code == 403 || r.code == 404 && "POST".equals(method))
            throw new IOException("El token no tiene permiso para issues en " + OWNER + "/" + REPO + " (HTTP " + r.code + ")");
        if (r.code < 200 || r.code >= 300) throw new IOException("GitHub respondió HTTP " + r.code);
        return r.body;
    }

    private static JsonElement json(String text) throws IOException {
        try { return JsonParser.parseString(text); }
        catch (RuntimeException broken) { throw new IOException("Respuesta de GitHub ilegible"); }
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static Response http(String method, String url, String token, String accept, String body) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        try {
            c.setRequestMethod(method);
            c.setConnectTimeout(15_000); c.setReadTimeout(20_000);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("Accept", accept);
            c.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            c.setRequestProperty("User-Agent", "Salve-self-pr");
            if (token != null && !token.isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token);
            if (body != null) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
            }
            int code = c.getResponseCode();
            InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            if (in != null) try (InputStream stream = in) {
                byte[] buf = new byte[8192];
                for (int n; (n = stream.read(buf)) > 0; ) {
                    data.write(buf, 0, n);
                    if (data.size() > MAX_BYTES) throw new IOException("Respuesta de GitHub demasiado grande");
                }
            }
            return new Response(code, new String(data.toByteArray(), StandardCharsets.UTF_8));
        } finally { c.disconnect(); }
    }
}
