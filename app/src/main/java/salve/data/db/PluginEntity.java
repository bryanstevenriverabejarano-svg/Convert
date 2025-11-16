package salve.data.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "plugins")
public class PluginEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "version")
    public String version;

    @ColumnInfo(name = "file_path")
    public String filePath;

    @ColumnInfo(name = "score")
    public double score;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public PluginEntity(String name, String version, String filePath, double score, long timestamp) {
        this.name = name;
        this.version = version;
        this.filePath = filePath;
        this.score = score;
        this.timestamp = timestamp;
    }
}