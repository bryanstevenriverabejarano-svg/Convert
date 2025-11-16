package salve.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "misiones")
public class MisionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;      // Clave primaria autogenerada
    public String texto; // Texto de la misión (almacenado en minúsculas, si gustas)

    // Opcionalmente, podrías tener más campos, ej. fecha de creación, prioridad, etc.
}