package salve.data.salvente;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.util.List;
import java.util.Map;

/** Ficha persistida de un Salvente. El id nunca cambia; nameKey (sin tildes ni mayúsculas) es único. */
@Entity(tableName = "salvente_profiles", indices = {@Index(value = "name_key", unique = true)})
public class SalventeProfileEntity {
    @PrimaryKey @NonNull public String id = "";
    @NonNull public String name = "";
    @ColumnInfo(name = "name_key") @NonNull public String nameKey = "";
    @NonNull public String species = "";
    @NonNull public String purpose = "";
    @NonNull public String greeting = "";
    /** Los 14 rasgos como JSON {"curiosidad":0.7,…}. */
    @NonNull public Map<String, Double> traits = new java.util.LinkedHashMap<>();
    /** Capacidades como JSON ["CONVERSACION",…]. */
    @NonNull public List<String> capabilities = new java.util.ArrayList<>();
    @NonNull public String state = "";
    @NonNull public String origin = "";
    @ColumnInfo(name = "created_at") public long createdAt;
    @ColumnInfo(name = "updated_at") public long updatedAt;
}
