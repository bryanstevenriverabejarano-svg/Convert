package salve.data.sync;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** pCloud-backed remote store. Upload success is acknowledged only after a remote checksum matches. */
public final class PCloudProvider implements CloudProvider {
    private final Context context;
    private final OkHttpClient client;

    public PCloudProvider(Context context) {
        this.context = context.getApplicationContext();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return PCloudCredentialStore.load(context) != null;
    }

    /** Called by the OAuth completion flow; token is encrypted locally and never belongs in source control. */
    public static void configureOAuth(Context context, String accessToken, String apiHost) {
        PCloudCredentialStore.save(context, accessToken, apiHost);
    }

    @Override
    public boolean uploadJson(String remotePath, String jsonPayload) {
        if (jsonPayload == null) return false;
        try {
            File tmp = File.createTempFile("salve-cloud-", ".json", context.getCacheDir());
            try {
                java.nio.file.Files.write(tmp.toPath(), jsonPayload.getBytes(StandardCharsets.UTF_8));
                return uploadFile(remotePath, tmp);
            } finally {
                //noinspection ResultOfMethodCallIgnored
                tmp.delete();
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean uploadFile(String remotePath, File file) {
        Credentials c = credentials();
        if (c == null || file == null || !file.isFile()) return false;
        try {
            String normalized = normalizeRemotePath(remotePath);
            String folder = parent(normalized);
            String filename = normalized.substring(normalized.lastIndexOf('/') + 1);
            if (!ensureFolderTree(c, folder)) return false;

            HttpUrl url = api(c, "uploadfile").newBuilder()
                    .addQueryParameter("path", folder)
                    .addQueryParameter("filename", filename)
                    .addQueryParameter("nopartial", "1")
                    .build();
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
            RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, fileBody)
                    .build();
            JsonObject uploaded = json(new Request.Builder().url(url)
                    .header("Authorization", "Bearer " + c.token)
                    .post(body).build(), c);
            if (!ok(uploaded)) return false;

            JsonArray metadata = uploaded.getAsJsonArray("metadata");
            if (metadata == null || metadata.size() == 0) return false;
            JsonObject meta = metadata.get(0).getAsJsonObject();
            long fileId = meta.get("fileid").getAsLong();

            JsonObject checksum = json(authenticatedGet(c, "checksumfile",
                    "fileid", Long.toString(fileId)), c);
            if (!ok(checksum)) return false;
            String remote = checksum.has("sha256") ? checksum.get("sha256").getAsString() :
                    checksum.has("sha1") ? checksum.get("sha1").getAsString() : null;
            String algorithm = checksum.has("sha256") ? "SHA-256" : "SHA-1";
            return remote != null && remote.equalsIgnoreCase(digest(file, algorithm));
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listFiles(String folderPath) {
        try { return listFilesChecked(folderPath); }
        catch (java.io.IOException failure) { return Collections.emptyList(); }
    }

    /** A disconnected account must not be confused with an empty history. */
    public List<String> listFilesChecked(String folderPath) throws java.io.IOException {
        Credentials c = credentials();
        if (c == null) throw new java.io.IOException("pCloud sin conectar");
        try {
            String normalized = normalizeRemotePath(folderPath);
            JsonObject result = json(authenticatedGet(c, "listfolder", "path", normalized), c);
            if (result != null && result.has("result") && result.get("result").getAsInt() == 2005)
                return Collections.emptyList();
            if (!ok(result) || !result.has("metadata") || !result.getAsJsonObject("metadata").has("contents"))
                throw new java.io.IOException("No se pudo consultar pCloud; revisa la conexión y la cuenta");
            return filePaths(normalized, result.getAsJsonObject("metadata").getAsJsonArray("contents"));
        } catch (java.io.IOException failure) { throw failure; }
        catch (Exception failure) { throw new java.io.IOException("No se pudo leer el índice de pCloud", failure); }
    }

    static List<String> filePaths(String folder, JsonArray contents) {
        List<String> paths = new ArrayList<>();
        if (contents == null) return paths;
        String base = normalizeRemotePath(folder);
        if (!base.endsWith("/")) base += "/";
        for (int i = 0; i < contents.size(); i++) {
            try {
                JsonObject item = contents.get(i).getAsJsonObject();
                if (item.has("isfolder") && item.get("isfolder").getAsBoolean()) continue;
                String path = item.has("path") ? item.get("path").getAsString()
                        : base + item.get("name").getAsString();
                path = normalizeRemotePath(path);
                if (path.startsWith(base) && !path.substring(base.length()).contains("/")) paths.add(path);
            } catch (RuntimeException invalid) { /* Ignore malformed entries, preserving valid siblings. */ }
        }
        return paths;
    }

    @Override
    public byte[] download(String remotePath) {
        return downloadBounded(remotePath, 16 * 1024 * 1024);
    }

    public byte[] downloadBounded(String remotePath, int maxBytes) {
        Credentials c = credentials();
        if (c == null || maxBytes < 1) return null;
        try {
            String normalized = normalizeRemotePath(remotePath);
            JsonObject link = json(authenticatedGet(c, "getfilelink", "path", normalized), c);
            if (!ok(link)) return null;
            HttpUrl downloadUrl = downloadUrl(link);
            if (downloadUrl == null) return null;
            // The signed content URL does not need our bearer token. Binary photos must not
            // use gettextfile, which performs character-encoding conversion.
            Request request = new Request.Builder().url(downloadUrl).get().build();
            try (Response response = client.newBuilder().followRedirects(false).build().newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                if (response.body().contentLength() > maxBytes) return null;
                try (java.io.InputStream in = response.body().byteStream();
                     java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) {
                        if (out.size() > maxBytes - count) return null;
                        out.write(buffer, 0, count);
                    }
                    return out.toByteArray();
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    static HttpUrl downloadUrl(JsonObject link) {
        try {
            JsonArray hosts = link.getAsJsonArray("hosts");
            if (hosts == null || hosts.size() == 0) return null;
            String host = hosts.get(0).getAsString().toLowerCase(Locale.ROOT);
            String path = link.get("path").getAsString();
            if (!host.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.pcloud\\.com")
                    || !path.startsWith("/") || path.startsWith("//") || path.contains("\\")) return null;
            HttpUrl url = HttpUrl.get("https://" + host + path);
            return host.equals(url.host()) && url.port() == 443 ? url : null;
        } catch (RuntimeException invalid) { return null; }
    }

    private boolean ensureFolderTree(Credentials c, String folder) throws Exception {
        if ("/".equals(folder)) return true;
        String[] pieces = folder.substring(1).split("/");
        String current = "";
        for (String piece : pieces) {
            if (piece.isEmpty()) continue;
            current += "/" + piece;
            JsonObject result = json(authenticatedGet(c, "createfolderifnotexists", "path", current), c);
            if (!ok(result)) return false;
        }
        return true;
    }

    private Request authenticatedGet(Credentials c, String method, String key, String value) {
        HttpUrl url = api(c, method).newBuilder().addQueryParameter(key, value).build();
        return new Request.Builder().url(url).header("Authorization", "Bearer " + c.token).get().build();
    }

    private JsonObject json(Request request, Credentials c) throws Exception {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return new JsonObject();
            String text = response.body().string();
            return JsonParser.parseString(text).getAsJsonObject();
        }
    }

    private static boolean ok(JsonObject object) {
        return object != null && object.has("result") && object.get("result").getAsInt() == 0;
    }

    private static HttpUrl api(Credentials c, String method) {
        return HttpUrl.get("https://" + c.host + "/" + method);
    }

    private Credentials credentials() {
        PCloudCredentialStore.Credentials c = PCloudCredentialStore.load(context);
        return c == null ? null : new Credentials(c.accessToken, c.apiHost);
    }

    static String normalizeRemotePath(String path) {
        if (path == null || path.trim().isEmpty()) throw new IllegalArgumentException("Ruta remota vacía");
        String value = path.trim().replace('\\', '/');
        if (!value.startsWith("/")) value = "/" + value;
        while (value.contains("//")) value = value.replace("//", "/");
        if (value.contains("/../") || value.endsWith("/..") || value.contains("/./") || value.endsWith("/.")) {
            throw new IllegalArgumentException("Ruta remota insegura");
        }
        return value;
    }

    private static String parent(String path) {
        int cut = path.lastIndexOf('/');
        return cut <= 0 ? "/" : path.substring(0, cut);
    }

    private static String digest(File file, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) md.update(buffer, 0, read);
        }
        StringBuilder hex = new StringBuilder();
        for (byte b : md.digest()) hex.append(String.format(Locale.ROOT, "%02x", b));
        return hex.toString();
    }

    private static final class Credentials {
        final String token;
        final String host;
        Credentials(String token, String host) { this.token = token; this.host = host; }
    }
}
