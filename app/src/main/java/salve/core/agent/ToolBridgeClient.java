package salve.core.agent;

import com.google.gson.*;
import java.io.IOException;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import okhttp3.*;
import salve.core.NetworkResourcePolicy;

/** The configured endpoint alone receives its bearer. Redirects and proxies are disabled. */
public final class ToolBridgeClient {
    private final ToolBridgeConfig config;
    private final OkHttpClient client;
    public ToolBridgeClient(ToolBridgeConfig config) {
        this.config = config;
        client = new OkHttpClient.Builder().proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false)
                .connectTimeout(5, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).callTimeout(30, TimeUnit.SECONDS)
                .dns(host -> {
                    java.util.List<java.net.InetAddress> addresses = Dns.SYSTEM.lookup(host);
                    for (java.net.InetAddress address : addresses) if (!NetworkResourcePolicy.isPublicAddress(address.getAddress()))
                        throw new java.net.UnknownHostException("Destino no público");
                    return addresses;
                }).build();
    }
    public AgentRun.Observation execute(AgentRun.Pending call, BooleanSupplier stopped) throws IOException {
        if (stopped.getAsBoolean()) throw new java.util.concurrent.CancellationException();
        JsonObject payload = new JsonObject(); payload.addProperty("tool", call.tool); payload.addProperty("input", call.input);
        Request request = new Request.Builder().url(config.endpoint + "/v1/execute")
                .header("Authorization", "Bearer " + config.token).header("Idempotency-Key", call.id)
                .post(RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8"))).build();
        try (Response response = client.newCall(request).execute()) {
            if (stopped.getAsBoolean()) throw new java.util.concurrent.CancellationException();
            if (!response.isSuccessful() || response.body() == null) throw new IOException("El puente no completó la petición.");
            if (response.body().contentLength() > 24_000) throw new IOException("Respuesta demasiado grande");
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            java.io.InputStream input = response.body().byteStream();
            byte[] chunk = new byte[2048]; int count;
            while ((count = input.read(chunk)) != -1) {
                if (buffer.size() + count > 24_000) throw new IOException("Respuesta demasiado grande");
                buffer.write(chunk, 0, count);
            }
            byte[] bytes = buffer.toByteArray();
            JsonObject json = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            AgentRun.Observation receipt = new AgentRun.Observation(call.id, call.tool, json.get("status").getAsString(), json.get("text").getAsString());
            for (String key : new String[]{"sources", "links"}) if (json.has(key)) for (JsonElement value : json.getAsJsonArray(key)) {
                String url = value.getAsString();
                if (url.length() > 500 || !NetworkResourcePolicy.validateKnowledgeUrl(url).allowed) throw new IOException("Fuente no válida");
                (key.equals("sources") ? receipt.sources : receipt.links).add(url);
            }
            return receipt;
        }
    }
}
