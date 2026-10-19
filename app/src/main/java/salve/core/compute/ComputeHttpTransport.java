package salve.core.compute;

import com.google.gson.*;
import java.io.*;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import okhttp3.*;
import salve.core.cognition.CognitivePrompt;

/** Over Salve's own WireGuard tunnel only: own CA, no redirects/retries/proxies, bounded JSON, cancellable. */
public final class ComputeHttpTransport implements ComputeRouter.Transport {
    private final salve.core.privatefamily.PrivateVpn vpn;
    /** Toda conexión a un nodo va por el túnel WireGuard propio de Salve; sin túnel, no sale nada. */
    public ComputeHttpTransport(android.content.Context context) { vpn = salve.core.privatefamily.PrivateVpn.get(context); }
    public JsonObject familyStatus(ComputeNode node, BooleanSupplier stopped) throws IOException {
        return familyStatus(node, null, stopped);
    }
    /**
     * Estado y ubicación del nodo. Con {@code knownVersion} el nodo responde «sin cambios» y no
     * envía coordenadas si la ubicación no ha cambiado desde la última vez.
     */
    public JsonObject familyStatus(ComputeNode node, String knownVersion, BooleanSupplier stopped) throws IOException {
        return request(node, "/v1/family/status", null, 5, stopped, knownVersion);
    }
    public void shareLocation(ComputeNode node, String writerToken, JsonObject location, BooleanSupplier stopped) throws IOException {
        ComputeNode owner = new ComputeNode(node.id, node.name, node.endpoint, writerToken);
        JsonObject payload = new JsonObject(); payload.add("location", location == null ? JsonNull.INSTANCE : location);
        request(owner, "/v1/location", payload, 5, stopped, null);
    }
    @Override public ComputeRouter.Health health(ComputeNode node, BooleanSupplier stopped) throws IOException {
        JsonObject json = request(node, "/v1/health", null, 2, stopped);
        try {
            if (json.get("protocol").getAsInt() != 1 || !json.get("ready").getAsBoolean()) throw new IOException("Nodo no preparado");
            return new ComputeRouter.Health(json.get("model").getAsString(), json.get("free_slots").getAsInt(),
                    json.get("max_prompt_chars").getAsInt(), json.get("max_output_tokens").getAsInt());
        } catch (RuntimeException malformed) { throw new IOException("Capacidades inválidas"); }
    }
    @Override public String generate(ComputeNode node, ComputeRouter.Health health, String id, String role,
            String prompt, BooleanSupplier stopped) throws IOException {
        JsonObject data = new JsonObject(); data.addProperty("protocol", 1); data.addProperty("id", id);
        data.addProperty("role", role);
        data.addProperty("prompt", CognitivePrompt.withEngine(prompt, "Nodo " + node.name + " / " + health.model, false, null));
        data.addProperty("max_tokens", Math.min(health.maxOutputTokens, role.equals("NARRADOR") ? 2048 : 1024));
        JsonObject json = request(node, "/v1/generate", data, 50, stopped);
        try {
            if (json.get("protocol").getAsInt() != 1 || !id.equals(json.get("id").getAsString())
                    || !"SUCCESS".equals(json.get("status").getAsString())) throw new IOException("El nodo no completó la tarea");
            return json.get("text").getAsString();
        } catch (RuntimeException malformed) { throw new IOException("Respuesta inválida"); }
    }
    private JsonObject request(ComputeNode node, String path, JsonObject payload, int seconds, BooleanSupplier stopped) throws IOException {
        return request(node, path, payload, seconds, stopped, null);
    }
    private JsonObject request(ComputeNode node, String path, JsonObject payload, int seconds, BooleanSupplier stopped,
                               String knownLocation) throws IOException {
        if (stopped.getAsBoolean()) throw new CancellationException();
        Request.Builder request = new Request.Builder().url(node.endpoint + path)
                .header("Authorization", "Bearer " + node.token).header("Accept", "application/json");
        if (knownLocation != null && knownLocation.matches("[a-f0-9]{16}|none")) request.header("X-Salve-Location-Version", knownLocation);
        if (payload != null) request.post(RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8")));
        salve.core.privatefamily.PrivateVpn.Lease lease = vpn.lease(node.endpoint);
        Call call = lease.client.newCall(request.build()); call.timeout().timeout(seconds, TimeUnit.SECONDS);
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override public void onFailure(Call ignored, IOException error) { future.completeExceptionally(error); }
            @Override public void onResponse(Call ignored, Response response) {
                try (Response close = response) {
                    if (!response.isSuccessful() || response.body() == null) throw new IOException("Nodo no disponible (" + response.code() + ")");
                    MediaType type = response.body().contentType();
                    if (type == null || !"application".equals(type.type()) || !"json".equals(type.subtype())) throw new IOException("Tipo de respuesta inválido");
                    int maximum = payload == null ? 4096 : 262144;
                    if (response.body().contentLength() > maximum) throw new IOException("Respuesta demasiado grande");
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); InputStream stream = response.body().byteStream();
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = stream.read(buffer)) != -1) {
                        if (bytes.size() + count > maximum) throw new IOException("Respuesta demasiado grande");
                        bytes.write(buffer, 0, count);
                    }
                    future.complete(JsonParser.parseString(new String(bytes.toByteArray(), StandardCharsets.UTF_8)).getAsJsonObject());
                } catch (Exception error) { future.completeExceptionally(error); }
            }
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        try {
            while (true) {
                if (stopped.getAsBoolean()) throw new CancellationException();
                if (!lease.valid()) throw new IOException("VPN desconectada");
                if (System.nanoTime() >= deadline) throw new IOException("Tiempo de nodo agotado");
                try { JsonObject result = future.get(100, TimeUnit.MILLISECONDS);
                    if (!lease.valid() || stopped.getAsBoolean()) throw new CancellationException();
                    return result; }
                catch (TimeoutException pending) { /* check cancellation, revocation and deadline */ }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new CancellationException(); }
                catch (ExecutionException failed) { throw diagnose(failed.getCause()); }
            }
        } finally { call.cancel(); lease.close(); }
    }
    /** Mensajes fijos y útiles para el dueño; nunca incluyen token, cuerpo ni contenido de la tarea. */
    static IOException diagnose(Throwable cause) {
        if (cause instanceof javax.net.ssl.SSLException) return new IOException("Certificado HTTPS no válido");
        if (cause instanceof java.net.UnknownHostException) return new IOException("No se encuentra el servidor");
        if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.io.InterruptedIOException)
            return new IOException("Tiempo de nodo agotado");
        if (cause instanceof IOException && cause.getMessage() != null
                && cause.getMessage().matches("Nodo no disponible \\([0-9]{3}\\)")) return (IOException) cause;
        return new IOException("La petición al nodo falló");
    }
}
