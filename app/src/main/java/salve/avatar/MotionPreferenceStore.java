package salve.avatar;

import android.content.Context;
import android.content.SharedPreferences;

/** One small preference record, written on explicit feedback rather than every animation frame. */
public final class MotionPreferenceStore {
    private static MotionPreferenceStore instance;
    private final SharedPreferences preferences;
    private volatile MotionPreferenceProfile profile;

    private MotionPreferenceStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("salve_motion_preferences", Context.MODE_PRIVATE);
        String saved;
        try { saved = preferences.getString("profile", null); }
        catch (ClassCastException corruptType) { saved = null; }
        profile = MotionPreferenceProfile.decode(saved);
    }

    public static synchronized MotionPreferenceStore get(Context context) {
        if (context == null) throw new IllegalArgumentException("Context is required");
        if (instance == null) instance = new MotionPreferenceStore(context);
        return instance;
    }

    public MotionPreferenceProfile profile() { return profile; }

    public synchronized MotionPreferenceProfile record(MotionPreferenceProfile.Feedback feedback) {
        profile = profile.withFeedback(feedback);
        if (feedback == MotionPreferenceProfile.Feedback.RESET) preferences.edit().remove("profile").apply();
        else preferences.edit().putString("profile", profile.encode()).apply();
        return profile;
    }
}
