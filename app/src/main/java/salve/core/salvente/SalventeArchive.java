package salve.core.salvente;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Contenido de una copia: fichas y recuerdos de los Salventes. Se serializa a JSON y se sella con
 * {@link SealedBox} antes de salir del móvil. Al leerla, cada ficha se valida igual que al crearla.
 */
public final class SalventeArchive {
    public static final int VERSION = 1, MAX_SALVENTES = 64;

    public static final class Entry {
        public final SalventeProfile profile;
        public final List<SalventeMind.Memory> memories;
        public Entry(SalventeProfile profile, List<SalventeMind.Memory> memories) {
            this.profile = profile; this.memories = Collections.unmodifiableList(new ArrayList<>(memories));
        }
    }

    private SalventeArchive() { }

    public static String toJson(List<Entry> entries, long createdAt) {
        JsonObject root = new JsonObject();
        root.addProperty("salve", "salventes"); root.addProperty("version", VERSION); root.addProperty("created_at", createdAt);
        JsonArray list = new JsonArray();
        for (Entry e : entries) {
            SalventeProfile p = e.profile;
            JsonObject o = new JsonObject();
            o.addProperty("id", p.id); o.addProperty("name", p.name); o.addProperty("species", p.species);
            o.addProperty("purpose", p.purpose); o.addProperty("greeting", p.greeting);
            o.addProperty("state", p.state.name()); o.addProperty("origin", p.origin.name()); o.addProperty("created_at", p.createdAt);
            JsonObject traits = new JsonObject();
            for (SalventeTrait t : SalventeTrait.values()) traits.addProperty(t.key, p.trait(t));
            o.add("traits", traits);
            JsonArray caps = new JsonArray();
            for (SalventeCapability c : p.capabilities) caps.add(c.name());
            o.add("capabilities", caps);
            JsonArray mem = new JsonArray();
            for (SalventeMind.Memory m : e.memories) { JsonObject x = new JsonObject(); x.addProperty("at", m.at); x.addProperty("text", m.text); mem.add(x); }
            o.add("memories", mem);
            list.add(o);
        }
        root.add("salventes", list);
        return root.toString();
    }

    public static List<Entry> fromJson(String json) {
        JsonObject root;
        try { root = JsonParser.parseString(json).getAsJsonObject(); }
        catch (RuntimeException bad) { throw new IllegalArgumentException("La copia no tiene un formato válido."); }
        JsonArray list;
        try {
            if (!"salventes".equals(root.get("salve").getAsString()) || root.get("version").getAsInt() != VERSION)
                throw new IllegalArgumentException("Copia de otra versión.");
            list = root.getAsJsonArray("salventes");
        } catch (IllegalArgumentException other) { throw other; }
        catch (RuntimeException bad) { throw new IllegalArgumentException("La copia no tiene un formato válido."); }
        if (list == null || list.size() > MAX_SALVENTES) throw new IllegalArgumentException("Copia con demasiados Salventes.");
        List<Entry> out = new ArrayList<>();
        try {
        for (JsonElement el : list) {
            JsonObject o = el.getAsJsonObject();
            EnumMap<SalventeTrait, Double> traits = new EnumMap<>(SalventeTrait.class);
            for (Map.Entry<String, JsonElement> t : o.getAsJsonObject("traits").entrySet()) {
                SalventeTrait trait = SalventeTrait.fromKey(t.getKey());
                if (trait != null) traits.put(trait, t.getValue().getAsDouble());
            }
            EnumSet<SalventeCapability> caps = EnumSet.noneOf(SalventeCapability.class);
            for (JsonElement c : o.getAsJsonArray("capabilities")) {
                SalventeCapability cap = SalventeCapability.fromKey(c.getAsString());
                if (cap == null) throw new IllegalArgumentException("Capacidad desconocida en la copia.");
                caps.add(cap);
            }
            SalventeProfile p = new SalventeProfile(o.get("id").getAsString(), o.get("name").getAsString(), o.get("species").getAsString(),
                    o.get("purpose").getAsString(), o.get("greeting").getAsString(), traits, caps,
                    SalventeProfile.State.valueOf(o.get("state").getAsString()), SalventeProfile.Origin.valueOf(o.get("origin").getAsString()),
                    o.get("created_at").getAsLong());
            SalventeProfileValidator.requireValid(p, null);
            List<SalventeMind.Memory> memories = new ArrayList<>();
            for (JsonElement m : o.getAsJsonArray("memories")) {
                JsonObject x = m.getAsJsonObject();
                memories.add(new SalventeMind.Memory(x.get("at").getAsLong(), x.get("text").getAsString()));
            }
            out.add(new Entry(p, memories));
        }
        } catch (IllegalArgumentException invalid) { throw invalid; }
        catch (RuntimeException incomplete) { throw new IllegalArgumentException("La copia está incompleta o dañada."); }
        return out;
    }
}
