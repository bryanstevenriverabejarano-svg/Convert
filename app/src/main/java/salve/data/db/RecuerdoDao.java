package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

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
}