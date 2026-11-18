package salve.core.vida;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serializa leer → interpretar → guardar del módulo Vida (mismo contrato que
 * {@code PersonalBudgetService}): una respuesta de éxito solo se emite tras guardar.
 */
public final class VidaService {
    private final Object lock = new Object();
    private final VidaStore store;
    private final VidaConversation conversation;
    /** Copia ligera del último registro leído, para decidir rutas sin tocar el disco. */
    private volatile VidaLog cached;

    public VidaService(File file) { this(file, Clock.systemDefaultZone()); }
    public VidaService(File file, Clock clock) { store = new VidaStore(file); conversation = new VidaConversation(clock); }

    public boolean handles(String input) {
        return VidaConversation.handles(input, query -> VidaConversation.knows(snapshot(), query));
    }

    public String respond(String input) {
        synchronized (lock) {
            VidaLog log;
            try { log = store.load(); }
            catch (IOException unreadable) {
                if (VidaConversation.normalize(input).matches("(?:borra|olvida|elimina) (?:todos )?mis datos de vida")) {
                    try { store.clear(); cached = new VidaLog(); return "He borrado tus datos de Vida de este móvil."; }
                    catch (IOException failed) { return "No pude borrar los datos de Vida. No se ha eliminado nada."; }
                }
                return "No puedo leer tu registro de Vida y lo he conservado sin reemplazarlo. Di «borra mis datos de vida» si quieres empezar de cero.";
            }
            try {
                VidaConversation.Response r = conversation.handle(input, log);
                if (r.clearAll) { store.clear(); cached = new VidaLog(); }
                else if (r.next != null) { store.save(r.next); cached = r.next; }
                else cached = log;
                return r.reply;
            } catch (IOException saveFailed) {
                return "No pude guardar la anotación. Tu registro anterior se conserva; vuelve a intentarlo.";
            } catch (IllegalArgumentException invalid) {
                return "No he guardado eso: faltan datos. Di «módulo vida» para ver ejemplos.";
            }
        }
    }

    /** Fusiona un CSV de Hevy sin duplicar series ya importadas. */
    public String importHevy(String csv) {
        HevyCsvImporter.Result parsed;
        try { parsed = HevyCsvImporter.parse(csv); }
        catch (IllegalArgumentException invalid) { return invalid.getMessage(); }
        synchronized (lock) {
            VidaLog log;
            try { log = store.load(); }
            catch (IOException unreadable) { return "No puedo leer tu registro de Vida; no he importado nada para no reemplazarlo."; }
            Set<String> keys = new HashSet<>();
            for (VidaLog.GymSet s : log.sets) keys.add(s.key);
            List<VidaLog.GymSet> next = new ArrayList<>(log.sets);
            Set<String> sessions = new HashSet<>();
            int added = 0;
            for (VidaLog.GymSet s : parsed.sets) {
                if (!keys.add(s.key)) continue;
                next.add(s); added++; sessions.add(s.date + "|" + s.workout);
            }
            if (added == 0) return "Hevy: no hay series nuevas (" + parsed.sets.size() + " ya estaban importadas).";
            VidaLog updated = log.withSets(next);
            try { store.save(updated); cached = updated; }
            catch (IOException failed) { return "No pude guardar la importación de Hevy. Tu registro anterior se conserva."; }
            return "Importado de Hevy: " + added + " series de " + sessions.size() + " entrenamientos"
                    + (parsed.skipped > 0 ? " (" + parsed.skipped + " filas sin fecha o ejercicio, ignoradas)" : "")
                    + ". Pregúntame «resumen semanal» o «historial de» un ejercicio.";
        }
    }

    private VidaLog snapshot() {
        VidaLog c = cached;
        if (c != null) return c;
        synchronized (lock) {
            try { cached = store.load(); } catch (IOException unreadable) { cached = new VidaLog(); }
            return cached;
        }
    }
}
