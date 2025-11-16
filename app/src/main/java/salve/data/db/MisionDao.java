package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MisionDao {

    @Insert
    void insert(MisionEntity mision);

    @Query("SELECT * FROM misiones")
    List<MisionEntity> getAll();

    // Podrías añadir más métodos, ej. para borrar misiones, buscar por texto, etc.
}