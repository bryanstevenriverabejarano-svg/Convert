package salve.core.salvente;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Flujo de génesis con aprobación humana: proponer → (editar nombre o capacidades) → aprobar o
 * cancelar. Hasta aprobar no existe nada fuera de esta sesión: ni registro, ni clave, ni memoria.
 * Cancelar descarta la propuesta sin dejar rastro, incluso si el modelo aún estaba pensando: su
 * respuesta tardía se ignora. Aprobar vuelve a validar contra los nombres actuales antes de guardar.
 *
 * Hilos: propose/regenerate/approve consultan el almacén y el modelo (llámalos fuera del hilo de
 * la interfaz); rename/toggle/cancel/draft solo usan memoria y son seguros en el hilo de la interfaz.
 * El candado nunca se mantiene mientras el modelo piensa.
 */
public final class SalventeGenesisSession {
    /** Lo único que la sesión necesita del almacén. */
    public interface Store {
        Collection<String> names();
        /** Guarda de forma atómica (ficha + identidad); si algo falla no queda nada a medias. */
        SalventeProfile create(SalventeProfile approved);
    }

    private final SalventeGenesisEngine engine;
    private final Store store;
    private SalventeGenesisEngine.Request request;
    private SalventeGenesisEngine.Draft draft;
    private List<String> taken = new ArrayList<>();
    private int attempt;
    /** Cambia con cada propuesta y cada cancelación: una respuesta de una época anterior se descarta. */
    private long epoch;

    public SalventeGenesisSession(SalventeGenesisEngine engine, Store store) { this.engine = engine; this.store = store; }

    /** @return la propuesta, o null si se canceló (o se pidió otra) mientras se generaba. */
    public SalventeGenesisEngine.Draft propose(String purpose, String preferredName) {
        SalventeGenesisEngine.Request next = new SalventeGenesisEngine.Request(purpose, preferredName);
        long mine;
        synchronized (this) { request = next; attempt = 0; draft = null; mine = ++epoch; }
        return generate(next, 0, mine);
    }

    /** Otra variante para el mismo propósito; null si se canceló entretanto. */
    public SalventeGenesisEngine.Draft regenerate() {
        SalventeGenesisEngine.Request current; int next; long mine;
        synchronized (this) {
            if (request == null) throw new IllegalStateException("Primero describe el Salvente.");
            current = request; next = ++attempt; mine = ++epoch;
        }
        return generate(current, next, mine);
    }

    private SalventeGenesisEngine.Draft generate(SalventeGenesisEngine.Request r, int n, long mine) {
        List<String> names = new ArrayList<>(store.names());
        SalventeGenesisEngine.Draft result = engine.propose(r, names, n);   // sin candado: puede tardar
        synchronized (this) {
            if (mine != epoch) return null;
            taken = names; draft = result;
            return result;
        }
    }

    public synchronized SalventeGenesisEngine.Draft draft() { return draft; }

    /** Cambios de Bryan sobre la propuesta; se validan al momento y la propuesta solo cambia si son válidos. */
    public synchronized SalventeProfileValidator.Result rename(String name) {
        return replace(current().withName(SalventeGenesisEngine.clean(name, 30)));
    }

    public synchronized SalventeProfileValidator.Result toggle(SalventeCapability capability) {
        if (capability.isRequired()) return SalventeProfileValidator.validate(current(), taken);
        Set<SalventeCapability> caps = EnumSet.noneOf(SalventeCapability.class);
        caps.addAll(current().capabilities);
        if (!caps.remove(capability)) caps.add(capability);
        return replace(current().withCapabilities(caps));
    }

    /** Aprobación explícita: valida de nuevo con los nombres actuales y guarda. Después la sesión queda vacía. */
    public SalventeProfile approve() {
        SalventeProfile profile; long mine;
        synchronized (this) { profile = current(); mine = epoch; }
        SalventeProfileValidator.requireValid(profile, store.names());
        synchronized (this) {
            if (mine != epoch || draft == null || draft.profile != profile) throw new IllegalStateException("La propuesta cambió; revísala otra vez.");
            SalventeProfile saved = store.create(profile);
            clear();
            return saved;
        }
    }

    /** Cancelar: se olvida la propuesta. No se ha escrito nada, así que no hay nada que borrar. */
    public synchronized void cancel() { clear(); }

    private SalventeProfileValidator.Result replace(SalventeProfile candidate) {
        SalventeProfileValidator.Result result = SalventeProfileValidator.validate(candidate, taken);
        if (result.ok()) draft = new SalventeGenesisEngine.Draft(candidate, draft.notes);
        return result;
    }

    private SalventeProfile current() {
        if (draft == null) throw new IllegalStateException("No hay ninguna propuesta abierta.");
        return draft.profile;
    }

    private void clear() { draft = null; request = null; attempt = 0; taken = new ArrayList<>(); epoch++; }
}
