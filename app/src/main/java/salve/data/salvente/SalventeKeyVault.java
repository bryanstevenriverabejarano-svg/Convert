package salve.data.salvente;

/**
 * Una clave por Salvente. Es lo que da aislamiento criptográfico: los recuerdos de un Salvente
 * solo se descifran con su clave y su id (dato autenticado), y destruir la clave los deja
 * irrecuperables aunque quedara alguna copia de la base de datos.
 */
public interface SalventeKeyVault {
    final class Sealed {
        public final byte[] iv, ciphertext;
        public Sealed(byte[] iv, byte[] ciphertext) { this.iv = iv; this.ciphertext = ciphertext; }
    }
    void create(String salventeId) throws Exception;
    boolean exists(String salventeId) throws Exception;
    Sealed seal(String salventeId, byte[] plain) throws Exception;
    byte[] open(String salventeId, byte[] iv, byte[] ciphertext) throws Exception;
    void destroy(String salventeId) throws Exception;
}
