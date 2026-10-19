package salve.data.salvente;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Un recuerdo de un Salvente, cifrado con la clave propia de ese Salvente (AES-GCM en Android
 * Keystore, con su id como dato autenticado). Al borrar el Salvente se borran las filas y la clave.
 */
@Entity(tableName = "salvente_memories",
        foreignKeys = @ForeignKey(entity = SalventeProfileEntity.class, parentColumns = "id", childColumns = "salvente_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("salvente_id")})
public class SalventeMemoryEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    @ColumnInfo(name = "salvente_id") @NonNull public String salventeId = "";
    @ColumnInfo(name = "created_at") public long createdAt;
    @NonNull public byte[] iv = new byte[0];
    @NonNull public byte[] ciphertext = new byte[0];
}
