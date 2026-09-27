package salve.data.sync;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/** One-shot loopback receiver for pCloud's mobile implicit grant. No client secret is used. */
public final class PCloudOAuth {
    // Public OAuth client identifier. The client secret must never be shipped in the APK.
    // Copied from pCloud: third character is lowercase l; the z is lowercase too.
    public static final String CLIENT_ID = "YSl7EDHzBkH";
    public static final String REDIRECT_URI = "http://localhost:8765/callback";
    private static final int PORT = 8765;
    private volatile boolean cancelled;
    private ServerSocket server;

    public interface Result { void complete(String error); }

    public void cancel() {
        cancelled = true;
        try { if (server != null) server.close(); } catch (Exception ignored) {}
    }

    public void start(Context context, String clientId, Result result) throws Exception {
        if (clientId == null || !clientId.matches("[A-Za-z0-9_-]{5,128}"))
            throw new IllegalArgumentException("Client ID de pCloud no válido");
        server = new ServerSocket(PORT, 1, InetAddress.getByName("127.0.0.1"));
        byte[] random = new byte[24];
        new SecureRandom().nextBytes(random);
        StringBuilder state = new StringBuilder();
        for (byte b : random) state.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        String expected = state.toString();
        Thread receiver = new Thread(() -> {
            String error = "La conexión con pCloud caducó o fue cancelada";
            try (ServerSocket listener = server) {
                listener.setSoTimeout(180000);
                while (!cancelled) {
                    try (Socket socket = listener.accept()) {
                        socket.setSoTimeout(8000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        String request = reader.readLine();
                        if (request == null) continue;
                        String[] parts = request.split(" ");
                        String route = parts.length > 1 ? parts[1] : "";
                        int length = 0;
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) {
                            if (line.toLowerCase(java.util.Locale.ROOT).startsWith("content-length:"))
                                length = Integer.parseInt(line.substring(15).trim());
                        }
                        boolean callback = "GET".equals(parts[0]) && "/callback".equals(route);
                        boolean finish = "POST".equals(parts[0]) && "/finish".equals(route) && length > 0 && length < 8192;
                        String html = callback ? "<!doctype html><meta name='viewport' content='width=device-width'><p>Conectando Salve con pCloud…</p><script>fetch('/finish',{method:'POST',body:location.hash.slice(1)}).then(r=>r.text()).then(t=>document.body.textContent=t).catch(()=>document.body.textContent='No se pudo conectar. Vuelve a Salve.');history.replaceState(null,'','/callback');</script>"
                                : "Puedes volver a Salve.";
                        if (finish) {
                            char[] chars = new char[length];
                            int count = 0;
                            while (count < length) {
                                int n = reader.read(chars, count, length - count);
                                if (n < 0) break;
                                count += n;
                            }
                            Map<String, String> values = parse(new String(chars, 0, count));
                            if (expected.equals(values.get("state")) && "bearer".equalsIgnoreCase(values.get("token_type"))
                                    && values.get("access_token") != null && !values.get("access_token").isEmpty()
                                    && values.get("hostname") != null) {
                                CloudSyncManager.configurePCloud(context, values.get("access_token"), values.get("hostname"));
                                CloudSyncManager.setEnabled(context, true);
                                error = null;
                                html = "Salve ya está conectada con pCloud. Puedes volver a la app.";
                            } else {
                                html = "No se pudo verificar la autorización. Vuelve a Salve e inténtalo de nuevo.";
                            }
                        }
                        byte[] body = html.getBytes(StandardCharsets.UTF_8);
                        OutputStream out = socket.getOutputStream();
                        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; connect-src 'self'\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                        out.write(body);
                        out.flush();
                        if (finish && error == null) break;
                    }
                }
            } catch (Exception e) {
                if (!cancelled) error = "No se completó la conexión con pCloud";
            } finally {
                if (!cancelled) result.complete(error);
            }
        }, "salve-pcloud-oauth");
        receiver.start();
        Uri url = Uri.parse("https://my.pcloud.com/oauth2/authorize").buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("response_type", "token")
                .appendQueryParameter("redirect_uri", REDIRECT_URI)
                .appendQueryParameter("state", expected).build();
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, url));
        } catch (Exception e) {
            cancel();
            throw e;
        }
    }

    static Map<String, String> parse(String form) throws Exception {
        Map<String, String> result = new HashMap<>();
        for (String pair : form.split("&")) {
            int equal = pair.indexOf('=');
            if (equal > 0) result.put(URLDecoder.decode(pair.substring(0, equal), "UTF-8"),
                    URLDecoder.decode(pair.substring(equal + 1), "UTF-8"));
        }
        return result;
    }
}
