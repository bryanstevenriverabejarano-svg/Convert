package salve.core.web;

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

/** Clave de la API de búsqueda del dueño, cifrada con AndroidKeyStore. Solo la escribe la UI de ajustes. */
public final class WebSearchKeyStore {
    private static final String PREFS = "salve_web_search_keys", ALIAS = "salve.web.search.v1";
    private WebSearchKeyStore() { }

    public static boolean hasBraveKey(Context context) { return braveKey(context) != null; }

    /** Devuelve null si no hay clave o no puede descifrarse (p. ej. tras restaurar en otro móvil). */
    public static String braveKey(Context context) {
        SharedPreferences prefs = prefs(context);
        if (!prefs.contains("brave")) return null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(prefs.getString("brave_iv", ""), Base64.NO_WRAP)));
            String value = new String(cipher.doFinal(Base64.decode(prefs.getString("brave", ""), Base64.NO_WRAP)), StandardCharsets.UTF_8);
            return BraveSearchApi.validKey(value) ? value : null;
        } catch (Exception unreadable) { return null; }
    }

    public static void saveBraveKey(Context context, String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!BraveSearchApi.validKey(value)) throw new IllegalArgumentException("Pega la clave tal cual aparece en el panel de Brave Search API");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
            String data = Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
            if (!prefs(context).edit().putString("brave", data)
                    .putString("brave_iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)).commit())
                throw new IllegalStateException();
        } catch (IllegalArgumentException invalid) { throw invalid; }
        catch (Exception failure) { throw new IllegalStateException("No se pudo guardar la clave cifrada"); }
    }

    public static void removeBraveKey(Context context) { prefs(context).edit().remove("brave").remove("brave_iv").commit(); }

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
