package salve.core.voice;

/** Revocable ownership of live voice callbacks, separate from ordinary chat and avatar events. */
public final class LiveVoiceChannel {
    public interface Listener {
        void onReply(long token, String text, boolean audioQueued);
        void onSpeechStarted(long token);
        void onSpeechFinished(long token, boolean completed);
        default void onDiagnostic(long token, VoiceDiagnosticLog.Event event, long code) { }
    }

    public static final class Ticket {
        private final long token;
        private Listener listener;
        private Ticket(long token, Listener listener) { this.token = token; this.listener = listener; }
    }

    private Ticket active;

    public synchronized Ticket open(long token, Listener listener) {
        if (token <= 0 || listener == null) throw new IllegalArgumentException("Turno de voz inválido.");
        cancel();
        active = new Ticket(token, listener);
        return active;
    }

    public synchronized void cancel() {
        if (active != null) active.listener = null;
        active = null;
    }

    public synchronized boolean isActive() { return active != null; }
    public synchronized boolean owns(Ticket ticket) { return ticket != null && ticket == active; }

    /** Android calls these dispatch methods on its main thread. */
    public void reply(Ticket ticket, String text, boolean audioQueued) {
        Listener listener = listenerFor(ticket);
        if (listener != null) listener.onReply(ticket.token, text, audioQueued);
    }
    public void started(Ticket ticket) {
        Listener listener = listenerFor(ticket);
        if (listener != null) listener.onSpeechStarted(ticket.token);
    }
    public void finished(Ticket ticket, boolean completed) {
        Listener listener = listenerFor(ticket);
        if (listener != null) listener.onSpeechFinished(ticket.token, completed);
    }
    /** Technical events obey the same ticket revocation as TTS callbacks. */
    public void diagnostic(Ticket ticket, VoiceDiagnosticLog.Event event, long code) {
        Listener listener = listenerFor(ticket);
        if (listener != null) listener.onDiagnostic(ticket.token, event, code);
    }
    private synchronized Listener listenerFor(Ticket ticket) {
        // Never hold this lock while a UI listener calls back into the conversation motor.
        return owns(ticket) ? ticket.listener : null;
    }
}
