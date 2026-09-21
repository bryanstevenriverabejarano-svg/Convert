package salve.avatar;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** Opt-in UI owner. Background workers cannot animate a session they never opened. */
public final class AvatarMotionController {
    public static final class Session {
        private final long id;
        private volatile boolean open = true;
        private Session(long id) { this.id = id; }
    }
    private static final AvatarMotionController INSTANCE = new AvatarMotionController();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicLong ids = new AtomicLong();
    private final AvatarMotion motion = new AvatarMotion();
    private final List<Runnable> listeners = new ArrayList<>();
    private volatile Session current;
    private volatile AvatarMotion.Snapshot snapshot = motion.snapshot();
    private long lastFrame;
    private AvatarMotionController() { }
    public static AvatarMotionController get() { return INSTANCE; }

    /** The owner is required but not retained, so this singleton never holds an Activity. */
    public synchronized Session openSession(Object owner) {
        if (owner == null) throw new IllegalArgumentException("A visual owner is required");
        if (current != null) current.open = false;
        Session session = new Session(ids.incrementAndGet());
        current = session;
        dispatch(session, m -> m.activate(session.id));
        return session;
    }
    public long beginTurn(Session session, String input) {
        long turn = ids.incrementAndGet();
        dispatch(session, m -> m.beginTurn(session.id, turn, input)); return turn;
    }
    public void listening(Session s, boolean value) { dispatch(s, m -> m.listening(s.id, value)); }
    public void response(Session s, long turn, AvatarMotionProtocol.Result value) { dispatch(s, m -> m.response(s.id, turn, value)); }
    public void clarification(Session s, long turn) { dispatch(s, m -> m.clarification(s.id, turn)); }
    public void error(Session s, long turn) { dispatch(s, m -> m.error(s.id, turn)); }
    public void endTurn(Session s, long turn) { dispatch(s, m -> m.endTurn(s.id, turn)); }
    public void speechPending(Session s, long turn, String id) { dispatch(s, m -> m.speechPending(s.id, turn, id)); }
    public void speechStart(Session s, String id) { dispatch(s, m -> m.speechStart(s.id, id)); }
    public void speechRange(Session s, String id, int start, int end) { dispatch(s, m -> m.speechRange(s.id, id, start, end)); }
    public void speechEnd(Session s, String id) { dispatch(s, m -> m.speechEnd(s.id, id)); }
    public void pause(Session s) { dispatch(s, m -> m.pause(s.id)); }
    public void previewGesture(AvatarMotion.Gesture gesture, AvatarMotion.Expression expression) {
        onMain(() -> { advanceClock(SystemClock.uptimeMillis()); motion.previewGesture(gesture, expression); publish(); });
    }
    public synchronized void closeSession(Session s) {
        if (s == null || current != s) return;
        s.open = false; current = null;
        onMain(() -> { motion.close(s.id); publish(); });
    }
    public AvatarMotion.Snapshot snapshot() { return snapshot; }
    /** Renderers call on main with uptimeMillis; simultaneous views cannot advance time twice. */
    public AvatarMotion.Snapshot advance(long nowMillis) {
        requireMain();
        advanceClock(nowMillis);
        snapshot = motion.snapshot(); return snapshot;
    }
    private void advanceClock(long nowMillis) {
        if (nowMillis > lastFrame) {
            motion.advance(lastFrame == 0 ? 0f : (nowMillis - lastFrame) / 1000f);
            lastFrame = nowMillis;
        }
    }
    public void addListener(Runnable listener) {
        requireMain(); if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }
    public void removeListener(Runnable listener) { requireMain(); listeners.remove(listener); }
    private void dispatch(Session s, Consumer<AvatarMotion> event) {
        if (s == null || !s.open || current != s) return;
        onMain(() -> {
            if (s.open && current == s) {
                // Expire old cues before an event, so a newly delivered cue starts at its own time.
                advanceClock(SystemClock.uptimeMillis()); event.accept(motion); publish();
            }
        });
    }
    private void publish() {
        snapshot = motion.snapshot();
        for (Runnable listener : new ArrayList<>(listeners)) listener.run();
    }
    private void onMain(Runnable action) {
        // Always enqueue: an inline microphone event must not overtake an earlier worker turn.
        main.post(action);
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Render on main thread");
    }
}
