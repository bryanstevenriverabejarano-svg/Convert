package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReflexionDao {
    @Insert
    void insertReflexion(ReflexionEntity reflexion);

    @Query("SELECT * FROM reflexiones")
    List<ReflexionEntity> getAllReflexiones();
}