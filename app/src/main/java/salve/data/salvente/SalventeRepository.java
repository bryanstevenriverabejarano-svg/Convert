package salve.data.salvente;

import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import salve.core.salvente.SalventeCapability;
import salve.core.salvente.SalventeGenesisSession;
import salve.core.salvente.SalventeProfile;
import salve.core.salvente.SalventeProfileValidator;
import salve.core.salvente.SalventeTrait;

/**
 * Almacén de Salventes: fichas en salventes.db y recuerdos cifrados con la clave de cada uno.
 * Crear es atómico (clave + fila, o nada); borrar quita filas y destruye la clave.
 */
public final class SalventeRepository implements SalventeGenesisSession.Store {
    /** Recuerdos que conserva cada Salvente; los más antiguos se descartan. */
    public static final int MAX_MEMORIES = 500;
    public static final int MAX_MEMORY_CHARS = 2000;

    public static final class Memory {
        public final long at; public final String text;
        Memory(long at, String text) { this.at = at; this.text = text; }
    }

    private final SalventeDatabase db;
    private final SalventeIdentityManager identity;
    private final LongSupplier clock;

    public SalventeRepository(SalventeDatabase db, SalventeKeyVault keys, LongSupplier clock) {
        this.db = db; this.identity = new SalventeIdentityManager(db.dao(), keys); this.clock = clock;
    }

    public static SalventeRepository get(Context context) {
        return new SalventeRepository(SalventeDatabase.get(context), new AndroidSalventeKeyVault(), System::currentTimeMillis);
    }

    @Override public Collection<String> names() { return identity.takenNames(); }

    @Override public SalventeProfile create(SalventeProfile approved) {
        try { identity.admit(approved); }
        catch (IllegalArgumentException | IllegalStateException invalid) { throw invalid; }
        catch (Exception keystore) { throw new IllegalStateException("No se pudo crear su clave propia en el teléfono"); }
        try {
            db.runInTransaction(() -> db.dao().insert(toEntity(approved, clock.getAsLong())));
            return approved;
        } catch (RuntimeException failure) {
            try { identity.retire(approved.id); } catch (Exception ignored) { }
            throw new IllegalStateException("No se pudo guardar el Salvente; no ha quedado nada a medias");
        }
    }

    public List<SalventeProfile> all() {
        List<SalventeProfile> out = new ArrayList<>();
        for (SalventeProfileEntity e : db.dao().all()) out.add(fromEntity(e));
        return out;
    }

    public SalventeProfile get(String id) { SalventeProfileEntity e = db.dao().get(id); return e == null ? null : fromEntity(e); }

    public boolean setState(String id, SalventeProfile.State state) { return db.dao().setState(id, state.name(), clock.getAsLong()) == 1; }

    /** Borra la ficha y sus recuerdos (cascada) y destruye su clave. */
    public boolean delete(String id) {
        boolean removed = db.dao().delete(id) == 1;
        try { identity.retire(id); } catch (Exception ignored) { /* sin clave ya no hay nada legible */ }
        return removed;
    }

    /** Guarda un recuerdo en el espacio del Salvente, cifrado con su clave. */
    public void remember(String id, String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (db.dao().countId(id) == 0) throw new IllegalArgumentException("Ese Salvente no existe");
        String value = text.trim().length() > MAX_MEMORY_CHARS ? text.trim().substring(0, MAX_MEMORY_CHARS) : text.trim();
        SalventeKeyVault.Sealed sealed;
        try { sealed = identity.keys().seal(id, value.getBytes(StandardCharsets.UTF_8)); }
        catch (Exception keystore) { throw new IllegalStateException("No se pudo cifrar el recuerdo"); }
        SalventeMemoryEntity m = new SalventeMemoryEntity();
        m.salventeId = id; m.createdAt = clock.getAsLong(); m.iv = sealed.iv; m.ciphertext = sealed.ciphertext;
        db.runInTransaction(() -> { db.dao().insertMemory(m); db.dao().trimMemories(id, MAX_MEMORIES); });
    }

    /** Recuerdos más recientes primero. Los que no se pueden descifrar (clave perdida) se omiten. */
    public List<Memory> memories(String id, int limit) {
        List<Memory> out = new ArrayList<>();
        for (SalventeMemoryEntity m : db.dao().memories(id, Math.max(1, Math.min(limit, MAX_MEMORIES)))) {
            try { out.add(new Memory(m.createdAt, new String(identity.keys().open(id, m.iv, m.ciphertext), StandardCharsets.UTF_8))); }
            catch (Exception unreadable) { /* clave destruida o dato manipulado: no se inventa nada */ }
        }
        return out;
    }

    public int memoryCount(String id) { return db.dao().memoryCount(id); }

    /** Olvida todo lo que recuerda un Salvente; conserva su identidad y su clave. */
    public int forgetMemories(String id) { return db.dao().deleteMemories(id); }

    /** Para la conversación: sus recuerdos más recientes, ya descifrados. */
    public java.util.List<salve.core.salvente.SalventeMind.Memory> recentForMind(String id, int max) {
        java.util.List<salve.core.salvente.SalventeMind.Memory> out = new ArrayList<>();
        for (Memory m : memories(id, max)) out.add(new salve.core.salvente.SalventeMind.Memory(m.at, m.text));
        return out;
    }

    /** Todo, descifrado en memoria, para sellarlo en una copia. Nunca se escribe así en disco. */
    public List<salve.core.salvente.SalventeArchive.Entry> exportAll() {
        List<salve.core.salvente.SalventeArchive.Entry> out = new ArrayList<>();
        for (SalventeProfile p : all()) {
            List<salve.core.salvente.SalventeMind.Memory> mem = recentForMind(p.id, MAX_MEMORIES);
            java.util.Collections.reverse(mem);   // cronológico
            out.add(new salve.core.salvente.SalventeArchive.Entry(p, mem));
        }
        return out;
    }

    /**
     * Restaura un Salvente de una copia con una clave nueva de este teléfono. Si ya existe (mismo id)
     * o su nombre está ocupado, no lo toca y devuelve false. Atómico: clave + ficha + recuerdos, o nada.
     */
    public boolean restore(salve.core.salvente.SalventeArchive.Entry entry) {
        SalventeProfile p = entry.profile;
        if (db.dao().countId(p.id) > 0) return false;
        for (String other : names()) if (SalventeProfileValidator.nameKey(other).equals(SalventeProfileValidator.nameKey(p.name))) return false;
        try { identity.admit(p); }
        catch (IllegalArgumentException | IllegalStateException invalid) { return false; }
        catch (Exception keystore) { throw new IllegalStateException("No se pudo crear su clave en este teléfono"); }
        try {
            List<SalventeMemoryEntity> rows = new ArrayList<>();
            List<salve.core.salvente.SalventeMind.Memory> mem = entry.memories;
            for (salve.core.salvente.SalventeMind.Memory m : mem.subList(Math.max(0, mem.size() - MAX_MEMORIES), mem.size())) {
                if (m.text.trim().isEmpty()) continue;
                String value = m.text.length() > MAX_MEMORY_CHARS ? m.text.substring(0, MAX_MEMORY_CHARS) : m.text;
                SalventeKeyVault.Sealed sealed = identity.keys().seal(p.id, value.getBytes(StandardCharsets.UTF_8));
                SalventeMemoryEntity row = new SalventeMemoryEntity();
                row.salventeId = p.id; row.createdAt = m.at; row.iv = sealed.iv; row.ciphertext = sealed.ciphertext;
                rows.add(row);
            }
            db.runInTransaction(() -> { db.dao().insert(toEntity(p, clock.getAsLong())); db.dao().insertMemories(rows); });
            return true;
        } catch (Exception failure) {
            try { identity.retire(p.id); } catch (Exception ignored) { }
            throw new IllegalStateException("No se pudo restaurar a " + p.name + "; no ha quedado nada a medias");
        }
    }

    static SalventeProfileEntity toEntity(SalventeProfile p, long now) {
        SalventeProfileEntity e = new SalventeProfileEntity();
        e.id = p.id; e.name = p.name; e.nameKey = SalventeProfileValidator.nameKey(p.name); e.species = p.species;
        e.purpose = p.purpose; e.greeting = p.greeting; e.state = p.state.name(); e.origin = p.origin.name();
        Map<String, Double> traits = new LinkedHashMap<>();
        for (SalventeTrait t : SalventeTrait.values()) traits.put(t.key, p.trait(t));
        e.traits = traits;
        List<String> caps = new ArrayList<>();
        for (SalventeCapability c : p.capabilities) caps.add(c.name());
        e.capabilities = caps;
        e.createdAt = p.createdAt; e.updatedAt = now;
        return e;
    }

    static SalventeProfile fromEntity(SalventeProfileEntity e) {
        EnumMap<SalventeTrait, Double> traits = new EnumMap<>(SalventeTrait.class);
        for (Map.Entry<String, Double> t : e.traits.entrySet()) {
            SalventeTrait trait = SalventeTrait.fromKey(t.getKey());
            if (trait != null && t.getValue() != null) traits.put(trait, t.getValue());
        }
        EnumSet<SalventeCapability> caps = EnumSet.noneOf(SalventeCapability.class);
        for (String c : e.capabilities) { SalventeCapability cap = SalventeCapability.fromKey(c); if (cap != null) caps.add(cap); }
        SalventeProfile.State state;
        try { state = SalventeProfile.State.valueOf(e.state); } catch (RuntimeException unknown) { state = SalventeProfile.State.PAUSADO; }
        SalventeProfile.Origin origin;
        try { origin = SalventeProfile.Origin.valueOf(e.origin); } catch (RuntimeException unknown) { origin = SalventeProfile.Origin.DETERMINISTA; }
        return new SalventeProfile(e.id, e.name, e.species, e.purpose, e.greeting, traits, caps, state, origin, e.createdAt);
    }
}
