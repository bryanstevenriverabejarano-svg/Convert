package salve.core.voice;

import android.content.Context;
import android.content.SharedPreferences;

public final class VoiceProfileStore {
    private final SharedPreferences preferences;
    public VoiceProfileStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("salve_voice", Context.MODE_PRIVATE);
    }
    public VoiceProfile load() {
        VoiceProfile fallback = VoiceProfile.defaults();
        try {
            return new VoiceProfile(preferences.getString("voice", ""), preferences.getFloat("rate", fallback.rate),
                    preferences.getFloat("pitch", fallback.pitch), preferences.getBoolean("network", false));
        } catch (RuntimeException invalid) { return fallback; }
    }
    public boolean save(VoiceProfile value) {
        return preferences.edit().putString("voice", value.voiceName).putFloat("rate", value.rate)
                .putFloat("pitch", value.pitch).putBoolean("network", value.allowNetwork).commit();
    }
}
