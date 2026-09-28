package salve.core.agent;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import salve.core.NetworkResourcePolicy;

/** Explicit user configuration. Credentials are never placed in a plan, prompt or log. */
public final class ToolBridgeConfig {
    private static final String PREFS = "salve_tool_bridge", ALIAS = "salve.tools.bridge.v1";
    public final String endpoint, token;
    private ToolBridgeConfig(String endpoint, String token) { this.endpoint = endpoint; this.token = token; }
    public static String endpoint(String input) {
        String value = input == null ? "" : input.trim().replaceAll("/+$", "");
        if (value.length() > 500 || !NetworkResourcePolicy.validateKnowledgeUrl(value).allowed) throw new IllegalArgumentException("Usa una URL HTTPS pública.");
        URI uri = URI.create(value);
        if (uri.getQuery() != null || uri.getFragment() != null) throw new IllegalArgumentException("La URL no puede incluir credenciales ni parámetros.");
        return value;
    }
    public static void save(Context context, String endpoint, String token) {
        String target = endpoint(endpoint);
        if (token == null || !token.matches("[A-Za-z0-9_-]{32,256}")) throw new IllegalArgumentException("El token debe tener entre 32 y 256 caracteres seguros.");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            String encrypted = Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("endpoint", target)
                    .putString("token", encrypted).putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)).commit())
                throw new IllegalStateException();
        } catch (Exception failed) { throw new IllegalStateException("No se pudo proteger la configuración del puente."); }
    }
    public static ToolBridgeConfig load(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains("token")) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
            return new ToolBridgeConfig(endpoint(prefs.getString("endpoint", "")), new String(cipher.doFinal(
                    Base64.decode(prefs.getString("token", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8));
        } catch (Exception unreadable) { return null; }
    }
    public static void clear(Context context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit(); }
    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
