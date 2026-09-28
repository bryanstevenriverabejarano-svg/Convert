package salve.core.visual;

import com.google.gson.Gson;
import java.util.Arrays;
import salve.data.db.KnowledgeNodeEntity;
import salve.data.db.KnowledgeRelationEntity;
import salve.data.db.RecuerdoEntity;

/** Called in a Room transaction. Model output cannot create an identity edge. */
public final class VisualMemoryGraph {
    public static final String EDGE = "aparece_en_segun_usuario";
    private VisualMemoryGraph() { }
    public static void index(salve.data.db.RecuerdoDao memories, salve.data.db.KnowledgeNodeDao nodes,
                             salve.data.db.KnowledgeRelationDao relations, VisualMemoryRecord record) {
        String tag = "visual:" + record.id;
        RecuerdoEntity memory = new RecuerdoEntity();
        memory.frase = record.memoryText(); memory.emocion = "observacion_visual"; memory.intensidad = 6;
        memory.etiquetas = new Gson().toJson(Arrays.asList("foto", "imagen", tag));
        memory.timestamp = record.createdAt; memory.binario = "";
        memories.reemplazarPorEtiqueta(tag, memory);

        KnowledgeNodeEntity photo = node(nodes, "foto:" + record.id, "foto", record.memoryText(), memory.etiquetas);
        // Editing an annotation replaces only the user identity edge of this photo.
        relations.deleteVisualIdentity(photo.id, EDGE);
        if (!record.personName.isEmpty()) {
            KnowledgeNodeEntity person = node(nodes, VisualMemoryRecord.personKey(record.personName),
                    "persona_confirmada", "Persona etiquetada por el usuario: " + record.personName
                            + ". Las relaciones personales se documentan en cada foto; no hay reconocimiento biométrico.",
                    new Gson().toJson(Arrays.asList("persona", "declaracion_usuario", record.personName)));
            KnowledgeRelationEntity edge = new KnowledgeRelationEntity();
            edge.origenId = person.id; edge.destinoId = photo.id; edge.tipoRelacion = EDGE;
            edge.peso = 1.0;
            edge.narrativa = "Declaración del usuario: " + record.personName + ". Relación: " + record.relationship
                    + ". Posición o descripción: " + record.position + ". No se dedujo por parecido facial.";
            relations.insert(edge);
        }
    }

    private static KnowledgeNodeEntity node(salve.data.db.KnowledgeNodeDao nodes, String label, String type, String text, String tags) {
        KnowledgeNodeEntity node = nodes.findByEtiqueta(label);
        boolean fresh = node == null;
        if (fresh) { node = new KnowledgeNodeEntity(); node.etiqueta = label; }
        node.tipo = type; node.resumen = text; node.etiquetasSerializadas = tags; node.relevanciaCreativa = 6;
        if (fresh) node.id = nodes.insert(node);
        else nodes.update(node); // REPLACE would cascade-delete links from other photos.
        return node;
    }
}
