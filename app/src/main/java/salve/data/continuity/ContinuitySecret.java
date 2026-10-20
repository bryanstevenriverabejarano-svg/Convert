package salve.data.continuity;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Solo si Bryan activa la copia automática: la frase se guarda en este teléfono cifrada con una clave
 * no exportable de Android Keystore, para poder sellar la copia de cada noche sin preguntarle. Quien
 * controle este teléfono desbloqueado ya tiene la memoria original, así que no añade exposición;
 * el Guardián sigue sin poder leer nada. Desactivar la copia automática borra la frase.
 */
final class ContinuitySecret {
    private static final String ALIAS = "salve.continuity.v1";
    private final File file;

    ContinuitySecret(Context context) { file = new File(context.getNoBackupFilesDir(), "continuity/frase.enc"); }

    boolean exists() { return file.isFile(); }

    void store(char[] passphrase) throws Exception {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(CharBuffer.wrap(passphrase));
        byte[] plain = Arrays.copyOfRange(encoded.array(), encoded.position(), encoded.limit());
        Arrays.fill(encoded.array(), (byte) 0);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] body = cipher.doFinal(plain);
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Sin carpeta");
            byte[] out = ByteBuffer.allocate(12 + body.length).put(cipher.getIV()).put(body).array();
            File tmp = new File(file.getPath() + ".tmp");
            Files.write(tmp.toPath(), out);
            if (!tmp.renameTo(file)) throw new IllegalStateException("No se pudo guardar la frase");
        } finally { Arrays.fill(plain, (byte) 0); }
    }

    char[] load() throws Exception {
        byte[] data = Files.readAllBytes(file.toPath());
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, data, 0, 12));
        byte[] plain = cipher.doFinal(data, 12, data.length - 12);
        try {
            CharBuffer chars = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(plain));
            char[] out = Arrays.copyOfRange(chars.array(), chars.position(), chars.limit());
            Arrays.fill(chars.array(), '\0');
            return out;
        } finally { Arrays.fill(plain, (byte) 0); }
    }

    void forget() { if (file.exists() && !file.delete()) file.deleteOnExit(); }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        java.security.Key existing = store.getKey(ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }
}
