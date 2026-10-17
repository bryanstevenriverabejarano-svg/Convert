package salve.core.compute;

import com.google.gson.*;
import java.io.*;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import okhttp3.*;
import salve.core.cognition.CognitivePrompt;

/** TLS verification, no redirects/retries/proxies, bounded JSON and promptly cancellable calls. */
public final class ComputeHttpTransport implements ComputeRouter.Transport {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(2, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS)
            .dns(host -> {
                java.util.List<java.net.InetAddress> addresses = Dns.SYSTEM.lookup(host);
                for (java.net.InetAddress ip : addresses)
                    if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isMulticastAddress())
                        throw new java.net.UnknownHostException("Destino de nodo no admitido");
                return addresses;
            }).build();
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
        if (stopped.getAsBoolean()) throw new CancellationException();
        Request.Builder request = new Request.Builder().url(node.endpoint + path)
                .header("Authorization", "Bearer " + node.token).header("Accept", "application/json");
        if (payload != null) request.post(RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8")));
        Call call = CLIENT.newCall(request.build()); call.timeout().timeout(seconds, TimeUnit.SECONDS);
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
                if (System.nanoTime() >= deadline) throw new IOException("Tiempo de nodo agotado");
                try { return future.get(100, TimeUnit.MILLISECONDS); }
                catch (TimeoutException pending) { /* check cancellation, revocation and deadline */ }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new CancellationException(); }
                catch (ExecutionException failed) { throw new IOException("La petición al nodo falló"); }
            }
        } finally { call.cancel(); }
    }
}
