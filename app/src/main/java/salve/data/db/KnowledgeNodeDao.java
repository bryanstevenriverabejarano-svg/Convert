package salve.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface KnowledgeNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(KnowledgeNodeEntity entity);

    @Update
    void update(KnowledgeNodeEntity entity);

    @Query("SELECT * FROM knowledge_nodes WHERE etiqueta = :label LIMIT 1")
    KnowledgeNodeEntity findByEtiqueta(String label);

    @Query("SELECT * FROM knowledge_nodes ORDER BY relevanciaCreativa DESC, creadoEn DESC LIMIT :limit")
    List<KnowledgeNodeEntity> fetchMasRelevantes(int limit);

    @Query("SELECT * FROM knowledge_nodes WHERE tipo = :tipo ORDER BY creadoEn DESC LIMIT :limit")
    List<KnowledgeNodeEntity> fetchPorTipo(String tipo, int limit);

    @Query("SELECT * FROM knowledge_nodes WHERE id = :id LIMIT 1")
    KnowledgeNodeEntity findById(long id);

    @Query("SELECT * FROM knowledge_nodes WHERE etiqueta LIKE '%' || :texto || '%' OR resumen LIKE '%' || :texto || '%' OR etiquetasSerializadas LIKE '%' || :texto || '%' ORDER BY relevanciaCreativa DESC, creadoEn DESC, id ASC LIMIT :limit")
    List<KnowledgeNodeEntity> buscarPorTexto(String texto, int limit);

    @Query("UPDATE knowledge_nodes SET relevanciaCreativa = relevanciaCreativa + :delta WHERE id = :nodeId")
    void incrementarRelevancia(long nodeId, int delta);
}
