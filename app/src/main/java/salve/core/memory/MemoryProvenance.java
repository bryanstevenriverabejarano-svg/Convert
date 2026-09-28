package salve.core.memory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.HashSet;
import java.util.Set;
import salve.data.db.RecuerdoEntity;

/** Storage location does not determine who made a statement or whether it is true. */
public final class MemoryProvenance {
    private MemoryProvenance() { }
    public static boolean hasTag(RecuerdoEntity record, String tag) { return tags(record).contains(tag); }
    public static String kind(RecuerdoEntity record) {
        Set<String> tags = tags(record);
        if (tags.contains("sintesis_agente")) return "sintesis_agente";
        if (tags.contains("investigacion_publica") || tags.contains("fuentes_externas")) return "investigacion_publica";
        if (tags.contains("manifiesto") || tags.contains("identidad_creativa")) return "configuracion_sistema";
        if (tags.contains("hecho_usuario") || tags.contains("user_message") || tags.contains("voice_message"))
            return "declaracion_usuario";
        return "registro_sin_autoria_confirmada";
    }
    public static boolean isPersonalCandidate(RecuerdoEntity record) {
        String kind = kind(record);
        return !kind.equals("investigacion_publica") && !kind.equals("configuracion_sistema") && !kind.equals("sintesis_agente");
    }
    public static String storage(RecuerdoEntity record) {
        return hasTag(record, "pcloud") ? "pcloud_restaurado" : "registro_persistido";
    }
    private static Set<String> tags(RecuerdoEntity record) {
        Set<String> result = new HashSet<>();
        if (record == null || record.etiquetas == null || record.etiquetas.length() > 8192) return result;
        try {
            JsonElement json = JsonParser.parseString(record.etiquetas);
            if (json.isJsonArray()) for (JsonElement tag : json.getAsJsonArray())
                if (tag.isJsonPrimitive() && tag.getAsJsonPrimitive().isString()) result.add(tag.getAsString());
        } catch (RuntimeException malformed) { /* Legacy untyped records stay unclassified. */ }
        return result;
    }
}
