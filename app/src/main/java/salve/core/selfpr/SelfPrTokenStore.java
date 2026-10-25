package salve.core.selfpr;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Token de GitHub de Bryan para que Salve abra issues con sus propuestas. Se cifra con AndroidKeyStore,
 * nunca se muestra ni se envía al modelo. Debe ser un token de grano fino con acceso SOLO a Convert y
 * SOLO al permiso «Issues: lectura y escritura». Las pruebas y la fusión las hace GitHub Actions.
 */
public final class SelfPrTokenStore {
    private static final String PREFS = "salve_self_pr", ALIAS = "salve.self.pr.v1";
    private SelfPrTokenStore() { }

    /** Tokens de GitHub: de grano fino (github_pat_) o clásicos (ghp_). */
    public static boolean validToken(String value) { return GitHubSelfPr.validToken(value); }

    public static boolean has(Context context) { return token(context) != null; }

    public static String token(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains("token")) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)));
            String value = new String(cipher.doFinal(Base64.decode(prefs.getString("token", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8);
            return validToken(value) ? value : null;
        } catch (Exception unreadable) { return null; }
    }

    public static void save(Context context, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!validToken(value)) throw new IllegalArgumentException("Pega el token tal cual (empieza por github_pat_)");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            String data = Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!prefs(context).edit().putString("token", data)
                    .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)).commit())
                throw new IllegalStateException();
        } catch (IllegalArgumentException invalid) { throw invalid; }
        catch (Exception failure) { throw new IllegalStateException("No se pudo guardar el token cifrado"); }
    }

    public static void remove(Context context) { prefs(context).edit().remove("token").remove("iv").commit(); }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
