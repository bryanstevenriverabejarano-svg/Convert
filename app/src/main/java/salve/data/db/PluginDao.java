package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PluginDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlugin(PluginEntity plugin);

    @Query("SELECT * FROM plugins")
    List<PluginEntity> getAllPlugins();

    @Query("DELETE FROM plugins WHERE name = :name")
    void deletePlugin(String name);

    @Query("DELETE FROM plugins")
    void deleteAllPlugins();
}