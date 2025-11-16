package salve.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SyncEventDao {

    // Inserta un evento en cola
    @Insert
    long insert(SyncEventEntity e);

    // Pendientes de envío (orden temporal ascendente), ignorando los que fallaron demasiadas veces
    @Query("SELECT * FROM sync_events WHERE tries < 20 ORDER BY createdAt ASC LIMIT :limit")
    List<SyncEventEntity> getPending(int limit);

    // ✅ NUEVO: últimos N eventos (enviados o no) para construir el grafo
    @Query("SELECT * FROM sync_events ORDER BY createdAt DESC LIMIT :n")
    List<SyncEventEntity> getLast(int n);

    // Borra un evento (por ejemplo, tras enviarlo con éxito)
    @Delete
    void delete(SyncEventEntity e);

    // Incrementa contador de reintentos cuando falla el envío
    @Query("UPDATE sync_events SET tries = tries + 1 WHERE id = :id")
    void incTries(long id);

    // Purga los que superaron el máximo de reintentos
    @Query("DELETE FROM sync_events WHERE tries >= :maxTries")
    void purgeFailed(int maxTries);
}
