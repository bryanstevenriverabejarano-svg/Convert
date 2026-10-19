package salve.data.salvente;

import java.util.List;
import salve.core.salvente.SalventeProfile;
import salve.core.salvente.SalventeProfileValidator;

/**
 * Segregación de identidades: cada Salvente es una entidad distinta de Salve y de los demás.
 * Valida que el id sea nuevo e inmutable y que el nombre no suplante a Salve ni repita otro, y
 * gestiona el ciclo de vida de su clave propia (se crea al aprobar, se destruye al borrar).
 */
public final class SalventeIdentityManager {
    private final SalventeDao dao;
    private final SalventeKeyVault keys;

    public SalventeIdentityManager(SalventeDao dao, SalventeKeyVault keys) { this.dao = dao; this.keys = keys; }

    public List<String> takenNames() { return dao.names(); }

    /** Comprueba la identidad y crea su clave. Si algo no encaja, no queda ninguna clave creada. */
    public void admit(SalventeProfile profile) throws Exception {
        SalventeProfileValidator.requireValid(profile, takenNames());
        if (dao.countId(profile.id) > 0 || keys.exists(profile.id)) throw new IllegalStateException("Ese identificador ya existe");
        keys.create(profile.id);
    }

    /** Destrucción criptográfica: sin la clave, ningún recuerdo cifrado de ese Salvente se puede leer. */
    public void retire(String salventeId) throws Exception { keys.destroy(salventeId); }

    public SalventeKeyVault keys() { return keys; }
}
