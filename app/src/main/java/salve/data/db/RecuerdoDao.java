package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

/**
 * DAO para la entidad RecuerdoEntity.
 * Proporciona métodos para insertar y consultar recuerdos.
 */
@Dao
public interface RecuerdoDao {

    /**
     * Inserta un nuevo recuerdo en la base de datos.
     *
     * @param recuerdo la entidad RecuerdoEntity a insertar.
     */
    @Insert
    void insertRecuerdo(RecuerdoEntity recuerdo);

    /**
     * Recupera todos los recuerdos cuya frase contenga la palabraClave indicada.
     *
     * @param palabraClave fragmento de texto a buscar dentro de la columna 'frase'.
     * @return lista de entidades RecuerdoEntity que cumplan el criterio.
     */
    @Query("SELECT * FROM recuerdos WHERE frase LIKE '%' || :palabraClave || '%'")
    List<RecuerdoEntity> filtrarRecuerdos(String palabraClave);

    @Query("SELECT * FROM recuerdos WHERE frase LIKE '%' || :palabraClave || '%' ORDER BY timestamp DESC, id DESC LIMIT :limite")
    List<RecuerdoEntity> buscarRecientes(String palabraClave, int limite);

    @Query("SELECT * FROM recuerdos ORDER BY timestamp ASC, id ASC LIMIT 1")
    RecuerdoEntity primerRecuerdo();

    @Query("SELECT * FROM recuerdos ORDER BY timestamp DESC, id DESC LIMIT 1")
    RecuerdoEntity ultimoRecuerdo();

    /** Exact JSON tag boundaries avoid matching profile:name_extra for profile:name. */
    @Query("SELECT * FROM recuerdos WHERE instr(etiquetas, '\"' || :etiqueta || '\"') > 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    RecuerdoEntity ultimoPorEtiqueta(String etiqueta);

    @Query("DELETE FROM recuerdos WHERE instr(etiquetas, '\"' || :etiqueta || '\"') > 0")
    int eliminarPorEtiqueta(String etiqueta);

    @Query("SELECT COUNT(*) FROM recuerdos WHERE timestamp = :time AND frase = :text")
    int countExact(long time, String text);

    @Query("SELECT COUNT(*) FROM recuerdos WHERE frase = :text AND timestamp BETWEEN :time - 5000 AND :time + 5000 AND (etiquetas IS NULL OR (instr(etiquetas, '\"memoria_manual\"') = 0 AND instr(etiquetas, '\"memoria_auto\"') = 0))")
    int canonicalNear(long time, String text);

    @Query("DELETE FROM recuerdos WHERE frase = :text AND timestamp BETWEEN :time - 5000 AND :time + 5000 AND (instr(etiquetas, '\"memoria_manual\"') > 0 OR instr(etiquetas, '\"memoria_auto\"') > 0) AND (instr(etiquetas, '\"pcloud\"') > 0 OR instr(etiquetas, '\"diario_local\"') > 0)")
    int deleteLegacyCopies(long time, String text);

    @Query("SELECT * FROM recuerdos WHERE etiquetas LIKE '%profile:%' ORDER BY timestamp ASC")
    List<RecuerdoEntity> perfiles();

    @Query("SELECT * FROM recuerdos WHERE (etiquetas IS NULL OR (instr(etiquetas, '\"manifiesto\"') = 0 AND instr(etiquetas, '\"identidad_creativa\"') = 0 AND instr(etiquetas, '\"investigacion_publica\"') = 0 AND instr(etiquetas, '\"fuentes_externas\"') = 0)) ORDER BY timestamp ASC, id ASC LIMIT 1")
    RecuerdoEntity primerRecuerdoCompartido();

    @Query("SELECT * FROM recuerdos WHERE (etiquetas IS NULL OR (instr(etiquetas, '\"manifiesto\"') = 0 AND instr(etiquetas, '\"identidad_creativa\"') = 0 AND instr(etiquetas, '\"investigacion_publica\"') = 0 AND instr(etiquetas, '\"fuentes_externas\"') = 0)) ORDER BY timestamp DESC, id DESC LIMIT 1")
    RecuerdoEntity ultimoRecuerdoCompartido();

    /** Expressions are built from quoted tokens; Room binds them as data. */
    @Query("SELECT recuerdos.* FROM recuerdos JOIN recuerdos_fts ON recuerdos.id = recuerdos_fts.rowid WHERE recuerdos_fts MATCH :expression AND (:personal = 0 OR (etiquetas IS NULL OR (instr(etiquetas, '\"manifiesto\"') = 0 AND instr(etiquetas, '\"identidad_creativa\"') = 0 AND instr(etiquetas, '\"investigacion_publica\"') = 0 AND instr(etiquetas, '\"fuentes_externas\"') = 0))) ORDER BY timestamp DESC, id DESC LIMIT :limite")
    List<RecuerdoEntity> buscarIndice(String expression, boolean personal, int limite);

    @Transaction
    default void reemplazarPorEtiqueta(String etiqueta, RecuerdoEntity recuerdo) {
        eliminarPorEtiqueta(etiqueta);
        insertRecuerdo(recuerdo);
    }
}
