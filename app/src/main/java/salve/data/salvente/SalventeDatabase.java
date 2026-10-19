package salve.data.salvente;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Base de datos de los Salventes, separada de memoria.db a propósito: un fallo o una migración
 * aquí nunca puede tocar los recuerdos de Salve, y Salve no lee estas filas en su memoria.
 * Está excluida de las copias de seguridad porque sus claves de Keystore no se restauran.
 */
@Database(entities = {SalventeProfileEntity.class, SalventeMemoryEntity.class}, version = 1, exportSchema = true)
@TypeConverters(SalventeConverters.class)
public abstract class SalventeDatabase extends RoomDatabase {
    public static final String NAME = "salventes.db";
    public abstract SalventeDao dao();

    private static volatile SalventeDatabase instance;

    public static RoomDatabase.Builder<SalventeDatabase> builder(Context context, String name) {
        return Room.databaseBuilder(context.getApplicationContext(), SalventeDatabase.class, name);
    }

    public static SalventeDatabase get(Context context) {
        if (instance == null) synchronized (SalventeDatabase.class) {
            if (instance == null) instance = builder(context, NAME).build();
        }
        return instance;
    }
}
