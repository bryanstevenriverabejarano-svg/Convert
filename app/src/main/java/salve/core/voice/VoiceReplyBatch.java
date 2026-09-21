package salve.core.voice;

import java.util.Objects;
import java.util.function.Consumer;

/** Collects a request's replies until its owner and retained continuations finish. */
public final class VoiceReplyBatch<T> {
    private final Consumer<T> finalReply;
    private int pending = 1;
    private T latest;

    public VoiceReplyBatch(Consumer<T> finalReply) {
        this.finalReply = Objects.requireNonNull(finalReply, "finalReply");
    }

    /** Retain before scheduling a continuation; every successful retain needs one complete. */
    public synchronized boolean retain() {
        if (pending == 0) return false;
        pending++;
        return true;
    }

    /** Replace the reply to deliver when all work finishes. */
    public synchronized boolean offer(T value) {
        if (pending == 0) return false;
        latest = value;
        return true;
    }

    /** Finish one unit of work. The final callback runs once, outside this object's lock. */
    public void complete() {
        T reply;
        synchronized (this) {
            if (pending == 0 || --pending != 0) return;
            reply = latest;
            latest = null;
        }
        finalReply.accept(reply);
    }
}
