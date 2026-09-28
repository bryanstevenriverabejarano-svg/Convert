package salve.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * MemoriaDatabase.java
 *
 * Base de datos Room que gestiona entidades de:
 *  - Recuerdos de largo plazo.
 *  - Reflexiones generadas en el ciclo de sueño.
 *  - Misiones estáticas y dinámicas.
 *  - Plugins dinámicos cargados y persistidos.
 *  - Eventos de sincronización pendientes (offline-first).
 */
@Database(
        entities = {
                RecuerdoEntity.class,           // Recuerdos persistidos
                ReflexionEntity.class,          // Reflexiones generadas
                MisionEntity.class,             // Misiones estáticas/dinámicas
                PluginEntity.class,             // Plugins dinámicos descubiertos
                SyncEventEntity.class,          // Eventos pendientes de sincronizar
                KnowledgeNodeEntity.class,      // Nodos del grafo de conocimiento vivo
                KnowledgeRelationEntity.class,  // Relaciones del grafo
                MemorySyncStateEntity.class,
                ResearchTaskEntity.class,
                AgentControlEntity.class
        },
        version = 6,
        exportSchema = true
)
public abstract class MemoriaDatabase extends RoomDatabase {

    /** DAO para operaciones sobre RecuerdoEntity. */
    public abstract RecuerdoDao recuerdoDao();

    /** DAO para operaciones sobre ReflexionEntity. */
    public abstract ReflexionDao reflexionDao();

    /** DAO para operaciones sobre MisionEntity. */
    public abstract MisionDao misionDao();

    /** DAO para operaciones sobre PluginEntity. */
    public abstract PluginDao pluginDao();

    /** DAO para eventos de sincronización pendientes (offline-first). */
    public abstract SyncEventDao syncEventDao();

    /** DAO para nodos del grafo de conocimiento. */
    public abstract KnowledgeNodeDao knowledgeNodeDao();

    /** DAO para relaciones del grafo de conocimiento. */
    public abstract KnowledgeRelationDao knowledgeRelationDao();

    public abstract MemorySyncStateDao memorySyncStateDao();

    public abstract ResearchTaskDao researchTaskDao();

    // ============================================================
    // SINGLETON
    // ============================================================
    private static volatile MemoriaDatabase INSTANCE;

    /** Shared production/test configuration; missing migration paths preserve the database. */
    public static RoomDatabase.Builder<MemoriaDatabase> builder(Context context, String name) {
        return Room.databaseBuilder(context.getApplicationContext(), MemoriaDatabase.class, name)
                .addMigrations(MemoryMigrations.FROM_4_TO_5, MemoryMigrations.FROM_5_TO_6);
    }

    public static MemoriaDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MemoriaDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = builder(context, "memoria.db").build();
                }
            }
        }
        return INSTANCE;
    }
}
