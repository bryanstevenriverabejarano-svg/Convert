package salve.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Le decimos a Room que esta clase representa la tabla "recuerdos"
@Entity(tableName = "recuerdos")
public class RecuerdoEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;  // Este campo será la clave primaria en la tabla

    public String frase;      // Ejemplo: "Hoy salí a pasear"
    public String emocion;    // Ejemplo: "feliz", "triste"
    public int intensidad;    // Un entero para representar la fuerza de la emoción
    public String etiquetas;  // Lo guardamos como cadena JSON (por ejemplo ["verano","playa"])
    public String binario;    // Aquí guardaremos el texto codificado en "binario"
    public long timestamp;    // Momento en que se creó o guardó el recuerdo
}