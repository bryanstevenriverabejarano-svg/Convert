package salve.core.salvente;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Un turno de conversación con un Salvente: recuerda (solo de su espacio), piensa con el modelo,
 * corrige una deriva de identidad con un reintento y guarda el intercambio en su memoria.
 *
 * Sin modelo no hay respuesta: no se inventa una frase de plantilla que parezca pensamiento.
 */
public final class SalventeConversation {
    public interface Memories {
        List<SalventeMind.Memory> recent(int max);
        void remember(String text);
    }

    public static final class Reply {
        public final String text; public final int recalled; public final boolean corrected;
        Reply(String text, int recalled, boolean corrected) { this.text = text; this.recalled = recalled; this.corrected = corrected; }
    }

    /** Cuántos recuerdos se examinan para elegir los relevantes. */
    public static final int POOL = 200;

    private final SalventeProfile profile;
    private final Memories memories;
    private final SalventeGenesisEngine.Generator model;
    private final String speaker;
    private final List<SalventeMind.Turn> turns = new ArrayList<>();

    public SalventeConversation(SalventeProfile profile, Memories memories, SalventeGenesisEngine.Generator model, String speaker) {
        this.profile = profile; this.memories = memories; this.model = model;
        this.speaker = speaker == null || speaker.trim().isEmpty() ? "Bryan" : speaker.trim();
    }

    public synchronized List<SalventeMind.Turn> turns() { return Collections.unmodifiableList(new ArrayList<>(turns)); }

    public Reply say(String message) throws Exception {
        if (profile.state == SalventeProfile.State.PAUSADO) throw new IllegalStateException(profile.name + " está en pausa.");
        String said = message == null ? "" : message.trim();
        if (said.isEmpty()) throw new IllegalArgumentException("Escribe algo.");
        List<SalventeMind.Turn> history;
        synchronized (this) { history = new ArrayList<>(turns); }
        List<SalventeMind.Memory> recalled = SalventeMind.recall(memories.recent(POOL), said);

        String reply = SalventeMind.clean(model.generate(SalventeMind.prompt(profile, recalled, history, speaker, said, false)), profile, speaker);
        boolean corrected = false;
        if (SalventeMind.identityDrift(reply) || reply.isEmpty()) {
            corrected = true;   // una sola corrección: se recuerda explícitamente quién es
            reply = SalventeMind.clean(model.generate(SalventeMind.prompt(profile, recalled, history, speaker, said, true)), profile, speaker);
            if (SalventeMind.identityDrift(reply)) reply = reply.replaceAll("(?i)\\bsalve\\b", profile.name);
        }
        if (reply.isEmpty()) throw new IllegalStateException("El modelo no respondió nada útil.");
        synchronized (this) {
            turns.add(new SalventeMind.Turn(false, said));
            turns.add(new SalventeMind.Turn(true, reply));
            while (turns.size() > 2 * SalventeMind.MAX_TURNS) turns.remove(0);
        }
        memories.remember(SalventeMind.memoryOf(speaker, said, reply));
        return new Reply(reply, recalled.size(), corrected);
    }
}
