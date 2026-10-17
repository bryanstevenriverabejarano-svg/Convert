package salve.core.compute;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** One encrypted registry, excluded from backup; only settings UI writes it. */
public final class ComputeNodeStore {
    private static final String PREFS = "salve_compute_nodes", ALIAS = "salve.compute.nodes.v1";
    private final SharedPreferences prefs;
    public ComputeNodeStore(Context context) { prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public long revision() { return prefs.getLong("revision", 0); }
    public boolean enabled() { return prefs.getBoolean("enabled", false); }
    public void setEnabled(boolean enabled) {
        if (enabled && load().isEmpty()) throw new IllegalStateException("Añade un nodo primero");
        if (!prefs.edit().putBoolean("enabled", enabled).putLong("revision", revision() + 1).commit()) throw new IllegalStateException("No se pudo guardar el modo");
    }
    public synchronized List<ComputeNode> load() {
        if (!prefs.contains("registry")) return Collections.emptyList();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
            String json = new String(cipher.doFinal(Base64.decode(prefs.getString("registry", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8);
            List<ComputeNode> nodes = new ArrayList<>();
            for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) {
                JsonObject n = e.getAsJsonObject();
                nodes.add(new ComputeNode(n.get("id").getAsString(), n.get("name").getAsString(), n.get("endpoint").getAsString(), n.get("token").getAsString()));
                if (nodes.size() > ComputeRouter.MAX_NODES) throw new IllegalStateException();
            }
            return Collections.unmodifiableList(nodes);
        } catch (Exception unreadable) { return Collections.emptyList(); }
    }
    public synchronized void add(String name, String endpoint, String token) {
        List<ComputeNode> nodes = new ArrayList<>(load());
        ComputeNode node = new ComputeNode(UUID.randomUUID().toString(), name.trim(), endpoint, token.trim());
        if (nodes.size() >= ComputeRouter.MAX_NODES) throw new IllegalArgumentException("Máximo cuatro nodos");
        for (ComputeNode existing : nodes) if (existing.endpoint.equalsIgnoreCase(node.endpoint)) throw new IllegalArgumentException("Este servidor ya está registrado");
        nodes.add(node); save(nodes);
    }
    public synchronized void remove(String id) {
        List<ComputeNode> nodes = new ArrayList<>(load()); nodes.removeIf(n -> n.id.equals(id)); save(nodes);
    }
    private void save(List<ComputeNode> nodes) {
        try {
            JsonArray array = new JsonArray();
            for (ComputeNode n : nodes) {
                JsonObject o = new JsonObject(); o.addProperty("id", n.id); o.addProperty("name", n.name);
                o.addProperty("endpoint", n.endpoint); o.addProperty("token", n.token); array.add(o);
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            SharedPreferences.Editor edit = prefs.edit().putLong("revision", revision() + 1).putString("registry", Base64.encodeToString(cipher.doFinal(array.toString().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP))
                    .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP));
            // Keep owned routing even with no nodes; removing the last one must never enable Gemini.
            if (!edit.commit()) throw new IllegalStateException();
        } catch (Exception failure) { throw new IllegalStateException("No se pudo guardar el registro cifrado"); }
    }
    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key key = store.getKey(ALIAS, null); if (key instanceof SecretKey) return (SecretKey) key;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
