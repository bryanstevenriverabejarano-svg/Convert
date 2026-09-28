package salve.data.db;

import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.FtsOptions;

/** Derived index only. Room maintains it when canonical memories change or disappear. */
@Entity(tableName = "recuerdos_fts")
@Fts4(contentEntity = RecuerdoEntity.class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
public class RecuerdoSearchEntity {
    public String frase;
}
