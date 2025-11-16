package salve.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reflexiones")
public class ReflexionEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String tipo;
    public String contenido;
    public double profundidad;
    public String emocion;
    public String origen;
    public double certeza;
    public String estado;
    public long timestamp;
}