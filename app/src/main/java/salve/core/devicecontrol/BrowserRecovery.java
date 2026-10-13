package salve.core.devicecontrol;

/** Recovery of the initial search GET only; no replay of forms, clicks, or device actions. */
public final class BrowserRecovery {
    public enum State { IDLE, LOADING, WAITING_NETWORK, READY, PAUSED, FAILED }
    public static final int MAX_RETRIES = 2;
    public static final long ATTEMPT_TIMEOUT_MS = 25000;
    public static final long SESSION_TIMEOUT_MS = 120000;
    private State state = State.IDLE;
    private String url = "";
    private boolean retryable;
    private int retries;
    private long started, attemptStarted;
    public synchronized void begin(String url, boolean network, long now) {
        this.url = url; retryable = true; retries = 0; started = now; attemptStarted = now;
        state = network ? State.LOADING : State.WAITING_NETWORK;
    }
    public synchronized void userInteraction() {
        retryable = false;
        // A later manual navigation can display progress, without reviving automatic recovery.
        if (state == State.FAILED || state == State.PAUSED || state == State.WAITING_NETWORK) state = State.IDLE;
    }
    public synchronized void navigation(String method, String destination) {
        if (!"GET".equals(method) || !url.equals(destination)) retryable = false;
    }
    public synchronized void ready() { if (state == State.LOADING) state = State.READY; }
    public synchronized void failed(boolean transientFailure) {
        if (state != State.LOADING) return;
        state = transientFailure && retryable && retries < MAX_RETRIES ? State.WAITING_NETWORK : State.FAILED;
    }
    public synchronized void pause() { state = State.PAUSED; retryable = false; }
    public synchronized boolean retry(boolean network, long now) {
        if (state != State.WAITING_NETWORK) return false;
        if (!retryable || retries >= MAX_RETRIES || now < started || now - started >= SESSION_TIMEOUT_MS) {
            state = State.FAILED; return false;
        }
        if (!network) return false;
        retries++; attemptStarted = now; state = State.LOADING; return true;
    }
    public synchronized boolean timedOut(long now) {
        return state == State.LOADING && (now < attemptStarted || now - attemptStarted >= ATTEMPT_TIMEOUT_MS || now - started >= SESSION_TIMEOUT_MS);
    }
    public synchronized State state() { return state; }
    public synchronized String url() { return url; }
    public synchronized int retries() { return retries; }
}
