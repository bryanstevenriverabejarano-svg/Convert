package salve.core.compute;

import com.google.gson.*;
import java.io.*;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import okhttp3.*;
import salve.core.cognition.CognitivePrompt;

/** HTTPS con TLS mutuo hacia los nodos propios (CA propia), sin proxy ni redirecciones; JSON acotado y cancelable. */
public final class ComputeHttpTransport implements ComputeRouter.Transport {
    /** Nodos sin perfil (registrados antes del TLS mutuo): HTTPS con las CA del sistema, como siempre. */
    private static final OkHttpClient LEGACY = new OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).callTimeout(50, TimeUnit.SECONDS)
            .dns(host -> {
                java.util.List<java.net.InetAddress> addresses = Dns.SYSTEM.lookup(host);
                for (java.net.InetAddress ip : addresses)
                    if (ip.isAnyLocalAddress() || ip.isLoopbackAddress() || ip.isLinkLocalAddress() || ip.isMulticastAddress())
                        throw new java.net.UnknownHostException("Destino de nodo no admitido");
                return addresses;
            }).build();
    /** Cliente mTLS y direcciones por nodo; recuerda qué dirección (local o remota) funcionó. */
    private static final java.util.Map<String, Route> ROUTES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long PREFERENCE_MS = 10 * 60 * 1000L;
    private final salve.core.privatefamily.FamilyVault vault;
    public ComputeHttpTransport(android.content.Context context) { vault = new salve.core.privatefamily.FamilyVault(context); }

    private static final class Route {
        final OkHttpClient client; final java.util.List<String> bases;
        volatile int preferred; volatile long preferredAt;
        Route(OkHttpClient client, java.util.List<String> bases) { this.client = client; this.bases = bases; }
        java.util.List<String> ordered() {
            if (preferred == 0 || System.currentTimeMillis() - preferredAt > PREFERENCE_MS) return bases;
            java.util.List<String> out = new java.util.ArrayList<>(bases);
            out.add(0, out.remove(preferred));
            return out;
        }
        void worked(String base) { preferred = bases.indexOf(base); preferredAt = System.currentTimeMillis(); }
    }

    private Route route(ComputeNode node) throws IOException {
        salve.core.privatefamily.FamilyVault.Credential credential;
        try { credential = vault.read().credentials.get(node.id); }
        catch (RuntimeException unreadable) { credential = null; }
        if (credential == null) return new Route(LEGACY, java.util.Collections.singletonList(node.endpoint));
        String key = node.id + "|" + node.endpoint + "|" + credential.remote + "|" + credential.cert.hashCode() + "|" + credential.ca.hashCode();
        Route known = ROUTES.get(key);
        if (known != null) return known;
        java.util.List<String> bases = new java.util.ArrayList<>();
        bases.add(node.endpoint);
        if (credential.remote != null && !credential.remote.isEmpty()) bases.add(credential.remote);
        try {
            Route created = new Route(salve.core.privatefamily.MutualTls.client(credential), java.util.Collections.unmodifiableList(bases));
            ROUTES.keySet().removeIf(other -> other.startsWith(node.id + "|"));
            ROUTES.put(key, created);
            return created;
        } catch (Exception invalid) { throw new IOException("Certificado del perfil no válido"); }
    }

    /** Olvida el cliente de un nodo (al retirarlo o cambiar su perfil). */
    public static void forget(String nodeId) { ROUTES.keySet().removeIf(other -> other.startsWith(nodeId + "|")); }

    /** Reparte un trabajo a un satélite a través del nodo; devuelve la respuesta (con el id del trabajo). */
    public JsonObject satelliteEnqueue(ComputeNode node, com.google.gson.JsonObject task, BooleanSupplier stopped) throws IOException {
        return request(node, salve.core.satellite.SatelliteProtocol.ENQUEUE, task, 10, stopped, null);
    }

    /** Pregunta al nodo si ya hay resultado para un trabajo repartido a un satélite. */
    public JsonObject satelliteResult(ComputeNode node, String taskId, BooleanSupplier stopped) throws IOException {
        return request(node, salve.core.satellite.SatelliteProtocol.RESULT_OF + taskId, null, 5, stopped, null);
    }
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
    /** Copias selladas del Guardián: el nodo guarda bytes que no puede leer (cabecera SLVB1). */
    public static final int VAULT_MAX = 96 * 1024 * 1024;
    private static final MediaType OCTET = MediaType.get("application/octet-stream");

    /** Sube una copia sellada y comprueba que el Guardián guardó exactamente esos bytes. */
    public void vaultPut(ComputeNode node, String name, byte[] sealed, BooleanSupplier stopped) throws IOException {
        if (sealed == null || sealed.length > VAULT_MAX) throw new IOException("Copia demasiado grande");
        byte[] raw = exchange(node, "POST", "/v1/vault/" + vaultName(name), RequestBody.create(sealed, OCTET), "json", 4096,
                transferSeconds(sealed.length), stopped);
        try {
            JsonObject json = JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject();
            String expected = sha256(sealed);
            if (!json.get("saved").getAsBoolean() || !expected.equals(json.get("sha256").getAsString()))
                throw new IOException("El Guardián no confirmó la copia íntegra");
        } catch (RuntimeException malformed) { throw new IOException("Respuesta del Guardián inválida"); }
    }

    /** Descarga una copia sellada; null si el Guardián no tiene ninguna con ese nombre. */
    public byte[] vaultGet(ComputeNode node, String name, BooleanSupplier stopped) throws IOException {
        try { return exchange(node, "GET", "/v1/vault/" + vaultName(name), null, "octet-stream", VAULT_MAX, transferSeconds(VAULT_MAX / 4), stopped); }
        catch (IOException failure) {
            if ("Nodo no disponible (404)".equals(failure.getMessage())) {
                if (!isGuardian(node, stopped)) throw new IOException("Ese nodo no es Guardián: actívalo en Salve Nodo con --reconfigurar");
                return null;
            }
            throw failure;
        }
    }

    /** Dispositivos de casa permitidos en el nodo (Home Assistant local); IOException «(404)» si no tiene hogar. */
    public JsonObject home(ComputeNode node, BooleanSupplier stopped) throws IOException {
        return json(exchange(node, "GET", "/v1/home", null, "json", 262144, 15, stopped));
    }

    /** Una acción sobre una entidad permitida. Solo se llama tras la confirmación explícita de Bryan. */
    public JsonObject homeAction(ComputeNode node, String entity, String service, Double value, BooleanSupplier stopped) throws IOException {
        JsonObject body = new JsonObject(); body.addProperty("entity", entity); body.addProperty("service", service);
        if (value != null) body.addProperty("value", value);
        return json(exchange(node, "POST", "/v1/home/action",
                RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")), "json", 65536, 15, stopped));
    }

    private static JsonObject json(byte[] raw) throws IOException {
        try { return JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (RuntimeException malformed) { throw new IOException("Respuesta del nodo inválida"); }
    }

    /** ¿Tiene este nodo activado el Guardián? */
    public boolean isGuardian(ComputeNode node, BooleanSupplier stopped) throws IOException {
        try { exchange(node, "GET", "/v1/vault", null, "json", 65536, 10, stopped); return true; }
        catch (IOException failure) { if ("Nodo no disponible (404)".equals(failure.getMessage())) return false; throw failure; }
    }

    /** Plazo según tamaño: 60 s más lo que tardaría a ~256 KB/s (subida doméstica lenta), con tope de 15 min. */
    static int transferSeconds(long bytes) { return (int) Math.min(900, 60 + bytes / (256 * 1024)); }

    private static String vaultName(String name) {
        if (name == null || !name.matches("[a-z0-9][a-z0-9-]{2,63}")) throw new IllegalArgumentException("Nombre de copia no válido");
        return name;
    }

    static String sha256(byte[] data) {
        try {
            StringBuilder out = new StringBuilder();
            for (byte b : java.security.MessageDigest.getInstance("SHA-256").digest(data)) out.append(String.format("%02x", b));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    @Override public ComputeRouter.Health health(ComputeNode node, BooleanSupplier stopped) throws IOException {
        JsonObject json = request(node, "/v1/health", null, 2, stopped);
        try {
            if (json.get("protocol").getAsInt() != 1 || !json.get("ready").getAsBoolean()) throw new IOException("Nodo no preparado");
            ComputeRouter.Health health = new ComputeRouter.Health(json.get("model").getAsString(), json.get("free_slots").getAsInt(),
                    json.get("max_prompt_chars").getAsInt(), json.get("max_output_tokens").getAsInt());
            telemetry(health, json);
            return health;
        } catch (RuntimeException malformed) { throw new IOException("Capacidades inválidas"); }
    }
    /** Campos opcionales; si vienen mal formados se ignoran (la telemetría nunca invalida un nodo). */
    static void telemetry(ComputeRouter.Health health, JsonObject json) {
        try {
            if (json.has("roles") && json.get("roles").isJsonArray()) {
                java.util.List<String> roles = new java.util.ArrayList<>();
                for (JsonElement r : json.getAsJsonArray("roles"))
                    if (r.isJsonPrimitive() && r.getAsString().matches("[a-z]{1,20}") && roles.size() < 8) roles.add(r.getAsString());
                health.roles = java.util.Collections.unmodifiableList(roles);
            }
            if (json.has("gpu") && json.get("gpu").isJsonObject()) {
                JsonObject gpu = json.getAsJsonObject("gpu");
                String name = gpu.has("name") ? gpu.get("name").getAsString() : "";
                if (!name.matches("[^\\p{Cntrl}]{1,80}")) return;
                double used = gpu.has("vram_used_gb") ? gpu.get("vram_used_gb").getAsDouble() : Double.NaN;
                double total = gpu.has("vram_total_gb") ? gpu.get("vram_total_gb").getAsDouble() : Double.NaN;
                int load = gpu.has("load_pct") ? gpu.get("load_pct").getAsInt() : -1;
                if (!(total > 0 && total < 1024 && used >= 0 && used <= total)) { used = Double.NaN; total = Double.NaN; }
                health.gpu = name; health.vramUsedGb = used; health.vramTotalGb = total;
                health.gpuLoad = load >= 0 && load <= 100 ? load : -1;
            }
        } catch (RuntimeException malformed) { /* sin telemetría */ }
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
    /**
     * Primero la dirección local; si no hay conexión, la remota (IP pública o DDNS). Una tarea
     * (POST) solo se reintenta en la otra dirección si no llegó a conectarse: nunca se duplica.
     */
    private JsonObject request(ComputeNode node, String path, JsonObject payload, int seconds, BooleanSupplier stopped,
                               String knownLocation) throws IOException {
        if (stopped.getAsBoolean()) throw new CancellationException();
        Route route = route(node);
        java.util.List<String> bases = route.ordered();
        for (int i = 0; ; i++) {
            try {
                JsonObject result = once(route.client, bases.get(i), node, path, payload, seconds, stopped, knownLocation);
                route.worked(bases.get(i));
                return result;
            } catch (IOException failure) {
                if (i + 1 >= bases.size() || !retryable(failure, payload == null)) throw failure;
            }
        }
    }

    /** Como request, pero con método, cuerpo y tipo de respuesta libres (para el Guardián). */
    private byte[] exchange(ComputeNode node, String method, String path, RequestBody body, String subtype, int maximum,
                            int seconds, BooleanSupplier stopped) throws IOException {
        if (stopped.getAsBoolean()) throw new CancellationException();
        Route route = route(node);
        java.util.List<String> bases = route.ordered();
        for (int i = 0; ; i++) {
            try {
                Request.Builder request = new Request.Builder().url(bases.get(i) + path)
                        .header("Authorization", "Bearer " + node.token)
                        .header("Accept", "json".equals(subtype) ? "application/json" : "application/octet-stream")
                        .method(method, body);
                byte[] result = call(route.client, request.build(), subtype, maximum, seconds, stopped);
                route.worked(bases.get(i));
                return result;
            } catch (IOException failure) {
                if (i + 1 >= bases.size() || !retryable(failure, "GET".equals(method))) throw failure;
            }
        }
    }

    static boolean retryable(IOException failure, boolean safe) {
        String message = failure.getMessage() == null ? "" : failure.getMessage();
        if (message.startsWith("No hay conexión con el nodo") || message.startsWith("No se encuentra el servidor")) return true;
        return safe && message.startsWith("Tiempo de nodo agotado");
    }

    private JsonObject once(OkHttpClient client, String base, ComputeNode node, String path, JsonObject payload, int seconds,
                            BooleanSupplier stopped, String knownLocation) throws IOException {
        Request.Builder request = new Request.Builder().url(base + path)
                .header("Authorization", "Bearer " + node.token).header("Accept", "application/json");
        if (knownLocation != null && knownLocation.matches("[a-f0-9]{16}|none")) request.header("X-Salve-Location-Version", knownLocation);
        if (payload != null) request.post(RequestBody.create(payload.toString(), MediaType.get("application/json; charset=utf-8")));
        byte[] raw = call(client, request.build(), "json", payload == null ? 4096 : 262144, seconds, stopped);
        try { return JsonParser.parseString(new String(raw, StandardCharsets.UTF_8)).getAsJsonObject(); }
        catch (RuntimeException malformed) { throw new IOException("La petición al nodo falló"); }
    }

    /** Ejecuta una llamada cancelable con plazo, comprobando tipo y tamaño de la respuesta. */
    private byte[] call(OkHttpClient client, Request built, String subtype, int maximum, int seconds,
                        BooleanSupplier stopped) throws IOException {
        Call call = client.newCall(built); call.timeout().timeout(seconds, TimeUnit.SECONDS);
        CompletableFuture<byte[]> future = new CompletableFuture<>();
        call.enqueue(new Callback() {
            @Override public void onFailure(Call ignored, IOException error) { future.completeExceptionally(error); }
            @Override public void onResponse(Call ignored, Response response) {
                try (Response close = response) {
                    if (!response.isSuccessful() || response.body() == null) throw new IOException("Nodo no disponible (" + response.code() + ")");
                    MediaType type = response.body().contentType();
                    if (type == null || !"application".equals(type.type()) || !subtype.equals(type.subtype())) throw new IOException("Tipo de respuesta inválido");
                    if (response.body().contentLength() > maximum) throw new IOException("Respuesta demasiado grande");
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream(); InputStream stream = response.body().byteStream();
                    byte[] buffer = new byte[8192]; int count;
                    while ((count = stream.read(buffer)) != -1) {
                        if (bytes.size() + count > maximum) throw new IOException("Respuesta demasiado grande");
                        bytes.write(buffer, 0, count);
                    }
                    future.complete(bytes.toByteArray());
                } catch (Exception error) { future.completeExceptionally(error); }
            }
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        try {
            while (true) {
                if (stopped.getAsBoolean()) throw new CancellationException();
                if (System.nanoTime() >= deadline) throw new IOException("Tiempo de nodo agotado");
                try { byte[] result = future.get(100, TimeUnit.MILLISECONDS);
                    if (stopped.getAsBoolean()) throw new CancellationException();
                    return result; }
                catch (TimeoutException pending) { /* check cancellation, revocation and deadline */ }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new CancellationException(); }
                catch (ExecutionException failed) { throw diagnose(failed.getCause()); }
            }
        } finally { call.cancel(); }
    }
    /** Mensajes fijos y útiles para el dueño; nunca incluyen token, cuerpo ni contenido de la tarea. */
    static IOException diagnose(Throwable cause) {
        if (cause instanceof javax.net.ssl.SSLException) return new IOException("Certificado HTTPS no válido");
        if (cause instanceof java.net.UnknownHostException) return new IOException("No se encuentra el servidor");
        if (cause instanceof java.net.ConnectException || cause instanceof java.net.NoRouteToHostException)
            return new IOException("No hay conexión con el nodo");
        if (cause instanceof java.net.SocketTimeoutException && cause.getMessage() != null
                && cause.getMessage().toLowerCase(java.util.Locale.ROOT).contains("connect"))
            return new IOException("No hay conexión con el nodo");
        if (cause instanceof java.net.SocketTimeoutException || cause instanceof java.io.InterruptedIOException)
            return new IOException("Tiempo de nodo agotado");
        if (cause instanceof IOException && cause.getMessage() != null
                && cause.getMessage().matches("Nodo no disponible \\([0-9]{3}\\)")) return (IOException) cause;
        return new IOException("La petición al nodo falló");
    }
}
