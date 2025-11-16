package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Representa un nodo dentro del grafo de conocimiento vivo de Salve. Cada nodo
 * condensa un concepto, hallazgo o documento creativo y sirve como punto de
 * anclaje para generar narrativas contextualizadas.
 */
@Entity(tableName = "knowledge_nodes", indices = {@Index(value = {"etiqueta"}, unique = true)})
public class KnowledgeNodeEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Etiqueta legible del nodo (tema, documento, hallazgo, etc.). */
    @NonNull
    public String etiqueta = "";

    /** Tipo semántico del nodo (por ejemplo: tema, hallazgo, documento). */
    @NonNull
    public String tipo = "generico";

    /** Emoción dominante asociada al nodo para preservar tono creativo. */
    @NonNull
    public String emocionDominante = "curiosidad";

    /** Breve resumen o narrativa del nodo. */
    @NonNull
    public String resumen = "";

    /** Etiquetas auxiliares serializadas (JSON). */
    @NonNull
    public String etiquetasSerializadas = "[]";

    /** Puntuación heurística de relevancia creativa. */
    public int relevanciaCreativa = 1;

    /** Marca de tiempo en milisegundos. */
    public long creadoEn = System.currentTimeMillis();
}
