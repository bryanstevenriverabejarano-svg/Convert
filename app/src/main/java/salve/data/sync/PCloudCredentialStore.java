package salve.data.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Stores the pCloud OAuth bearer token encrypted with an Android Keystore key. */
public final class PCloudCredentialStore {
    private static final String PREFS = "salve_pcloud_oauth";
    private static final String ALIAS = "salve.pcloud.oauth.v1";
    private static final String TOKEN = "token";
    private static final String IV = "iv";
    private static final String HOST = "host";

    private PCloudCredentialStore() {}

    public static void save(Context context, String accessToken, String apiHost) {
        if (context == null || accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Credencial pCloud ausente");
        }
        String host = normalizeHost(apiHost);
        try {
            SecretKey key = key();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(accessToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(TOKEN, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString(HOST, host)
                    .apply();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la credencial pCloud", e);
        }
    }

    public static Credentials load(Context context) {
        if (context == null) return null;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String encrypted = prefs.getString(TOKEN, null);
        String iv = prefs.getString(IV, null);
        String host = prefs.getString(HOST, null);
        if (encrypted == null || iv == null || host == null) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(),
                    new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            String token = new String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)),
                    java.nio.charset.StandardCharsets.UTF_8);
            return new Credentials(token, normalizeHost(host));
        } catch (Exception e) {
            return null;
        }
    }

    public static void clear(Context context) {
        if (context != null) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    static String normalizeHost(String host) {
        if (host == null || host.trim().isEmpty()) return "api.pcloud.com";
        String value = host.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceFirst("^https?://", "").replaceAll("/+$", "");
        if (!"api.pcloud.com".equals(value) && !"eapi.pcloud.com".equals(value)) {
            throw new IllegalArgumentException("Host pCloud no permitido");
        }
        return value;
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;

        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    public static final class Credentials {
        public final String accessToken;
        public final String apiHost;
        Credentials(String accessToken, String apiHost) {
            this.accessToken = accessToken;
            this.apiHost = apiHost;
        }
    }
}
