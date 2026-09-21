package salve.core.voice;

/** Revocable permission to speak a completed turn; independent of its text response. */
public final class VoiceTurnGate {
    private long generation;
    private boolean listening;
    private boolean closed;

    /** A turn started during microphone capture must remain text-only after capture ends. */
    public synchronized long beginTurn() {
        generation++;
        return closed || listening ? 0L : generation;
    }

    public synchronized void listening(boolean value) {
        if (value) generation++;
        listening = value;
    }

    public synchronized boolean maySpeak(long turnGeneration) {
        return !closed && !listening && turnGeneration > 0L && generation == turnGeneration;
    }

    public synchronized void close() {
        generation++;
        closed = true;
    }
}
