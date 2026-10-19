package salve.core.salvente;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sobre sellado para el Guardián: AES-256-GCM con una clave derivada de una frase que solo conoce
 * Bryan (PBKDF2-HMAC-SHA256). El Guardián guarda bytes que no puede leer; sin la frase, nadie
 * (tampoco Salve en otro móvil) puede abrirlos. La frase no se guarda en ningún sitio.
 *
 * Formato: "SLVB1" | iteraciones (4 bytes) | sal (16) | iv (12) | cifrado+etiqueta. La cabecera
 * entera va como dato autenticado: cambiar un solo byte hace fallar la apertura.
 */
public final class SealedBox {
    public static final byte[] MAGIC = "SLVB1".getBytes(StandardCharsets.US_ASCII);
    public static final int ITERATIONS = 210_000, MIN_ITERATIONS = 100_000, MAX_ITERATIONS = 2_000_000, MIN_PASSPHRASE = 12;
    private static final int SALT = 16, IV = 12, HEADER = 5 + 4 + SALT;

    private SealedBox() { }

    public static void checkPassphrase(char[] passphrase) {
        if (passphrase == null || passphrase.length < MIN_PASSPHRASE)
            throw new IllegalArgumentException("La frase debe tener al menos " + MIN_PASSPHRASE + " caracteres.");
    }

    public static byte[] seal(byte[] plain, char[] passphrase, SecureRandom random) throws Exception {
        return seal(plain, passphrase, random, ITERATIONS);
    }

    static byte[] seal(byte[] plain, char[] passphrase, SecureRandom random, int iterations) throws Exception {
        checkPassphrase(passphrase);
        byte[] salt = new byte[SALT], iv = new byte[IV];
        random.nextBytes(salt); random.nextBytes(iv);
        byte[] header = ByteBuffer.allocate(HEADER).put(MAGIC).putInt(iterations).put(salt).array();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, derive(passphrase, salt, iterations), new GCMParameterSpec(128, iv));
        cipher.updateAAD(header);
        byte[] body = cipher.doFinal(plain);
        return ByteBuffer.allocate(HEADER + IV + body.length).put(header).put(iv).put(body).array();
    }

    /** Abre el sobre; con una frase equivocada o datos alterados lanza IllegalArgumentException. */
    public static byte[] open(byte[] sealed, char[] passphrase) {
        if (sealed == null || sealed.length < HEADER + IV + 16 || !Arrays.equals(Arrays.copyOf(sealed, 5), MAGIC))
            throw new IllegalArgumentException("No es una copia sellada de Salve.");
        ByteBuffer in = ByteBuffer.wrap(sealed);
        in.position(5);
        int iterations = in.getInt();
        if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) throw new IllegalArgumentException("Copia con parámetros no admitidos.");
        byte[] salt = new byte[SALT], iv = new byte[IV];
        in.get(salt); in.get(iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, derive(passphrase, salt, iterations), new GCMParameterSpec(128, iv));
            cipher.updateAAD(sealed, 0, HEADER);
            return cipher.doFinal(sealed, HEADER + IV, sealed.length - HEADER - IV);
        } catch (Exception wrong) {
            throw new IllegalArgumentException("La frase no es correcta o la copia está dañada.");
        }
    }

    private static SecretKey derive(char[] passphrase, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, iterations, 256);
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            try { return new SecretKeySpec(key, "AES"); } finally { Arrays.fill(key, (byte) 0); }
        } finally { spec.clearPassword(); }
    }
}
