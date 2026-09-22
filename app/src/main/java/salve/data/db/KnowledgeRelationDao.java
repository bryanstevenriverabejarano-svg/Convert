package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface KnowledgeRelationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(KnowledgeRelationEntity entity);

    @Query("SELECT * FROM knowledge_relations WHERE origenId = :nodeId ORDER BY creadoEn DESC LIMIT :limit")
    List<KnowledgeRelationEntity> relacionesDesde(long nodeId, int limit);

    @Query("SELECT * FROM knowledge_relations ORDER BY creadoEn DESC LIMIT :limit")
    List<KnowledgeRelationEntity> recientes(int limit);

    /** One-hop traversal includes incoming links as well as outgoing links. */
    @Query("SELECT * FROM knowledge_relations WHERE origenId = :nodeId OR destinoId = :nodeId ORDER BY peso DESC, creadoEn DESC, id ASC LIMIT :limit")
    List<KnowledgeRelationEntity> relacionesDeNodo(long nodeId, int limit);
}
