package salve.car;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import salve.core.voice.AssistantLaunch;
import salve.presentation.ui.MainActivity;

/**
 * «Hablar» y «Terminar» desde el coche (pantalla, reproductor o botones del volante). Abre la misma
 * conversación por voz que «Toca para hablar» de la notificación y la termina igual que su botón.
 */
public final class CarVoiceControl {
    private static final String TAG = "Salve/Coche";

    private CarVoiceControl() { }

    /** @return false si Android no dejó abrir la conversación (entonces hay que tocar la notificación). */
    public static boolean talk(Context context) {
        if (CarVoiceStatus.active()) return true;
        Intent voice = new Intent(context, MainActivity.class)
                .setAction(AssistantLaunch.ACTION_VOICE_COMMAND)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.getApplicationContext().startActivity(voice);
            return true;
        } catch (RuntimeException refused) {
            Log.w(TAG, "Android no dejó abrir la voz desde el coche: " + refused.getClass().getSimpleName());
            return false;
        }
    }

    public static void end() {
        salve.services.VoiceForegroundService.end();
    }
}
