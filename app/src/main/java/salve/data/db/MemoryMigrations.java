package salve.data.db;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/** Only migrations backed by known schemas. Unknown versions are never erased. */
public final class MemoryMigrations {
    private MemoryMigrations() { }
    public static final Migration FROM_4_TO_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS memory_sync_state (`key` TEXT NOT NULL, updatedAt INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY(`key`))");
        }
    };
    public static final Migration FROM_5_TO_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS agent_research_tasks (id TEXT NOT NULL, question TEXT NOT NULL, status TEXT NOT NULL, owner TEXT NOT NULL, receipt TEXT, error TEXT, attempts INTEGER NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, leaseUntil INTEGER NOT NULL, nextAttemptAt INTEGER NOT NULL, PRIMARY KEY(id))");
            db.execSQL("CREATE TABLE IF NOT EXISTS agent_control (id INTEGER NOT NULL, paused INTEGER NOT NULL, PRIMARY KEY(id))");
        }
    };
}
