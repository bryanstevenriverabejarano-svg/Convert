package salve.core.mind;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.text.Normalizer;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * Cuánto está cada persona en su vida: cuántas veces ha hablado con ella o de ella, y cuándo fue la
 * última. Con quien habla a diario (Bryan) pesa más que alguien mencionado una vez hace meses, y sus
 * recuerdos con esa persona salen antes cuando dos se parecen igual a la pregunta.
 */
public final class PersonFamiliarity {
    static final class Entry {
        String name;
        long mentions;
        long lastAt;
    }

    private static final Gson GSON = new Gson();
    /** A partir de unas 60 conversaciones ya es de las personas más cercanas. */
    private static final double SATURATION = Math.log1p(60);
    private static final double FADE_DAYS = 45;
    private final SelfModelJournal.Store store;
    private final LongSupplier clock;
    private Map<String, Entry> people;

    public PersonFamiliarity(SelfModelJournal.Store store, LongSupplier clock) {
        this.store = store; this.clock = clock;
        try {
            String raw = store.read();
            people = raw == null ? null : GSON.fromJson(raw, new TypeToken<LinkedHashMap<String, Entry>>() { }.getType());
        } catch (Exception damaged) { people = null; }
        if (people == null) people = new LinkedHashMap<>();
    }

    /** Una conversación con ella o una mención suya. */
    public synchronized void saw(String name) {
        String key = key(name);
        if (key.length() < 3) return;
        Entry e = people.get(key);
        if (e == null) { e = new Entry(); e.name = firstName(name); people.put(key, e); }
        e.mentions++;
        e.lastAt = clock.getAsLong();
        save();
    }

    /**
     * Personas conocidas que aparecen en el mensaje (por su primer nombre, palabra entera).
     * @return cuántas se contaron
     */
    public synchronized int sawIn(String text, Collection<String> knownNames) {
        if (text == null || knownNames == null) return 0;
        String t = " " + normalize(text).replaceAll("[^\\p{L}0-9]+", " ") + " ";
        int n = 0;
        for (String name : knownNames) {
            String k = key(name);
            if (k.length() >= 3 && t.contains(" " + k + " ")) { saw(name); n++; }
        }
        return n;
    }

    /** 0..1: más conversaciones y más recientes, más peso. */
    public synchronized double weight(String name) {
        Entry e = people.get(key(name));
        return e == null ? 0 : weight(e, clock.getAsLong());
    }

    /** Todas, nombre normalizado → peso, para ordenar recuerdos. */
    public synchronized Map<String, Double> weights() {
        long now = clock.getAsLong();
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Entry> e : people.entrySet()) out.put(e.getKey(), weight(e.getValue(), now));
        return out;
    }

    /** Los nombres más habituales ahora mismo (peso > 0.2), de más a menos. */
    public synchronized java.util.List<String> closest(int max) {
        long now = clock.getAsLong();
        java.util.List<Entry> all = new java.util.ArrayList<>(people.values());
        all.removeIf(e -> e == null || weight(e, now) <= 0.2);
        all.sort((a, b) -> Double.compare(weight(b, now), weight(a, now)));
        java.util.List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(max, all.size()); i++) out.add(all.get(i).name);
        return out;
    }

    public synchronized long mentions(String name) {
        Entry e = people.get(key(name));
        return e == null ? 0 : e.mentions;
    }

    /** Une la copia de pCloud (tras reinstalar no empieza de cero): se queda con lo mayor. */
    public synchronized boolean mergeFrom(String remoteJson) {
        Map<String, Entry> r;
        try { r = remoteJson == null ? null : GSON.fromJson(remoteJson, new TypeToken<LinkedHashMap<String, Entry>>() { }.getType()); }
        catch (RuntimeException damaged) { return false; }
        if (r == null) return false;
        boolean changed = false;
        for (Map.Entry<String, Entry> e : r.entrySet()) {
            if (e.getValue() == null) continue;
            Entry mine = people.get(e.getKey());
            if (mine == null) { people.put(e.getKey(), e.getValue()); changed = true; continue; }
            if (e.getValue().mentions > mine.mentions) { mine.mentions = e.getValue().mentions; changed = true; }
            if (e.getValue().lastAt > mine.lastAt) { mine.lastAt = e.getValue().lastAt; changed = true; }
        }
        if (changed) save();
        return changed;
    }

    public synchronized String json() { return GSON.toJson(people); }

    private static double weight(Entry e, long now) {
        double often = Math.min(1, Math.log1p(e.mentions) / SATURATION);
        double days = e.lastAt <= 0 ? 365 : Math.max(0, (now - e.lastAt) / 86_400_000.0);
        return often * (0.5 + 0.5 * Math.exp(-days / FADE_DAYS));
    }

    static String key(String name) { return normalize(firstName(name)); }

    private static String firstName(String name) {
        String t = name == null ? "" : name.trim();
        int space = t.indexOf(' ');
        return space < 0 ? t : t.substring(0, space);
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }

    private void save() {
        try { store.write(GSON.toJson(people)); } catch (Exception ignored) { /* best effort */ }
    }
}
