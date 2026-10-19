package salve.data.salvente;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Rasgos y capacidades como JSON estructurado dentro de la fila. */
public final class SalventeConverters {
    private static final Gson GSON = new Gson();
    private static final Type TRAITS = new TypeToken<LinkedHashMap<String, Double>>() { }.getType();
    private static final Type LIST = new TypeToken<ArrayList<String>>() { }.getType();

    @TypeConverter public static String traitsToJson(Map<String, Double> traits) { return GSON.toJson(traits == null ? new LinkedHashMap<>() : traits); }

    @TypeConverter public static Map<String, Double> traitsFromJson(String json) {
        Map<String, Double> value = json == null ? null : GSON.fromJson(json, TRAITS);
        return value == null ? new LinkedHashMap<>() : value;
    }

    @TypeConverter public static String listToJson(List<String> list) { return GSON.toJson(list == null ? new ArrayList<>() : list); }

    @TypeConverter public static List<String> listFromJson(String json) {
        List<String> value = json == null ? null : GSON.fromJson(json, LIST);
        return value == null ? new ArrayList<>() : value;
    }
}
