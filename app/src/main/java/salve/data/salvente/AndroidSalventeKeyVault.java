package salve.data.salvente;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Claves AES-256-GCM no exportables en Android Keystore, una por Salvente. */
public final class AndroidSalventeKeyVault implements SalventeKeyVault {
    private static final String PREFIX = "salve.salvente.";

    @Override public void create(String id) throws Exception {
        if (exists(id)) throw new IllegalStateException("Ese Salvente ya tiene clave");
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(PREFIX + id, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        generator.generateKey();
    }

    @Override public boolean exists(String id) throws Exception { return store().containsAlias(PREFIX + id); }

    @Override public Sealed seal(String id, byte[] plain) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key(id));
        cipher.updateAAD(id.getBytes(StandardCharsets.UTF_8));
        byte[] data = cipher.doFinal(plain);
        return new Sealed(cipher.getIV(), data);
    }

    @Override public byte[] open(String id, byte[] iv, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(id), new GCMParameterSpec(128, iv));
        cipher.updateAAD(id.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(ciphertext);
    }

    @Override public void destroy(String id) throws Exception { store().deleteEntry(PREFIX + id); }

    private static SecretKey key(String id) throws Exception {
        Key key = store().getKey(PREFIX + id, null);
        if (!(key instanceof SecretKey)) throw new IllegalStateException("Este Salvente no tiene clave en el teléfono");
        return (SecretKey) key;
    }

    private static KeyStore store() throws Exception { KeyStore s = KeyStore.getInstance("AndroidKeyStore"); s.load(null); return s; }
}
