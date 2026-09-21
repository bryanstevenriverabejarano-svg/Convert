package salve.avatar;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Main-thread store shared by the room and overlay; writes on actions/arrival, never each frame. */
public final class AvatarStore {
    private static AvatarStore instance;
    private final SharedPreferences preferences;
    private final List<Runnable> listeners = new ArrayList<>();
    private final AvatarState state;
    private long lastFrame;

    private AvatarStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("salve_avatar", Context.MODE_PRIVATE);
        state = AvatarState.decode(preferences.getString("state", null));
    }
    public static synchronized AvatarStore get(Context context) {
        if (instance == null) instance = new AvatarStore(context);
        return instance;
    }
    public AvatarState state() { return state; }
    public void change(Consumer<AvatarState> action) { action.accept(state); save(); notifyViews(); }
    public void addListener(Runnable listener) { if (!listeners.contains(listener)) listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    public void save() { preferences.edit().putString("state", state.encode()).apply(); }
    public void advance(long nowMillis) {
        // Two simultaneous views share elapsed wall time; they cannot double movement speed.
        float delta = lastFrame == 0 ? 0f : (nowMillis - lastFrame) / 1000f;
        lastFrame = nowMillis;
        AvatarState.Pose before = state.getPose();
        state.advance(delta);
        if (before != state.getPose()) { save(); notifyViews(); }
    }
    private void notifyViews() { for (Runnable listener : new ArrayList<>(listeners)) listener.run(); }
}
