package salve.core.privatefamily;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypted atomic state in noBackupFilesDir. No keys, locations or tokens in diagnostics. */
public final class FamilyVault {
    private static final Object LOCK = new Object();
    private static final String ALIAS = "salve.private.family.v1";
    private final AtomicFile file;
    public static final class Data {
        public String config = "", ca = "";
        public long revision;
        /** Última ubicación recibida de cada nodo (JSON de PrivateLocation) y su versión. */
        public Map<String, String> locations = new HashMap<>();
        public Map<String, String> versions = new HashMap<>();
        public Map<String, Long> receivedAt = new HashMap<>();
    }
    public FamilyVault(Context context) { file = new AtomicFile(new File(context.getNoBackupFilesDir(), "private-family.enc")); }
    public Data read() {
        synchronized (LOCK) {
            if (!file.getBaseFile().exists()) return new Data();
            try {
                byte[] bytes = file.readFully();
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOf(bytes, 12)));
                Data data = new Gson().fromJson(new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8), Data.class);
                if (data.locations == null) data.locations = new HashMap<>();
                if (data.versions == null) data.versions = new HashMap<>();
                if (data.receivedAt == null) data.receivedAt = new HashMap<>();
                return data;
            } catch (Exception unreadable) { throw new IllegalStateException("No se pudo abrir la configuración privada"); }
        }
    }
    public void edit(java.util.function.Consumer<Data> change) {
        synchronized (LOCK) {
            Data data = read(); change.accept(data);
            FileOutputStream output = null;
            try {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
                byte[] encrypted = cipher.doFinal(new Gson().toJson(data).getBytes(StandardCharsets.UTF_8));
                output = file.startWrite(); output.write(cipher.getIV()); output.write(encrypted); file.finishWrite(output);
            } catch (Exception failure) { if (output != null) file.failWrite(output); throw new IllegalStateException("No se pudo guardar la configuración privada"); }
        }
    }
    public void forget() { synchronized (LOCK) { file.delete(); } }
    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null); if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
