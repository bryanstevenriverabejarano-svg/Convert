package salve.data.sync;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
            if (metadata == null || metadata.isEmpty()) return false;
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

    @Override
    public byte[] download(String remotePath) {
        Credentials c = credentials();
        if (c == null) return null;
        try {
            String normalized = normalizeRemotePath(remotePath);
            Request request = authenticatedGet(c, "gettextfile", "path", normalized);
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                return response.body().bytes();
            }
        } catch (Exception e) {
            return null;
        }
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
