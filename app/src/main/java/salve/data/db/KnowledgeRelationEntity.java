package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Representa una arista dirigida dentro del grafo de conocimiento vivo.
 */
@Entity(
        tableName = "knowledge_relations",
        foreignKeys = {
                @ForeignKey(
                        entity = KnowledgeNodeEntity.class,
                        parentColumns = "id",
                        childColumns = "origenId",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = KnowledgeNodeEntity.class,
                        parentColumns = "id",
                        childColumns = "destinoId",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("origenId"),
                @Index("destinoId"),
                @Index(value = {"origenId", "destinoId", "tipoRelacion"}, unique = true)
        }
)
public class KnowledgeRelationEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long origenId;
    public long destinoId;

    /** Tipo de relación (inspira, contrasta, profundiza, etc.). */
    @NonNull
    public String tipoRelacion = "conecta";

    /** Peso heurístico (0-1) que indica fuerza de la relación. */
    public double peso = 0.5;

    /** Narrativa o contexto breve de la relación. */
    @NonNull
    public String narrativa = "";

    public long creadoEn = System.currentTimeMillis();
}
