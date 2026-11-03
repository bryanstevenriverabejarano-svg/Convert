package salve.devices;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import java.util.List;

/**
 * La música que suena en otra app (Spotify primero). Con «Acceso a notificaciones» habla con su
 * reproductor directamente, como el Asistente: play/pausa/siguiente, «qué suena» y buscar-y-reproducir
 * de verdad. Sin ese permiso, play/pausa/siguiente van como el botón de unos auriculares a Spotify.
 * Nunca toca las sesiones de la propia Salve.
 */
public final class MusicController {
    private static final String TAG = "Salve/Musica";

    private MusicController() { }

    /** ¿Tiene Salve «Acceso a notificaciones» (lo que deja ver y controlar la música)? */
    public static boolean hasAccess(Context context) {
        String enabled = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return enabled != null && enabled.contains(new ComponentName(context, salve.services.SalveMusicListener.class).flattenToString());
    }

    /** Abre el ajuste para conceder el acceso (solo con el móvil en la mano, nunca conduciendo). */
    public static boolean openAccessSettings(Context context) {
        Intent detail = Build.VERSION.SDK_INT >= 30
                ? new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                            new ComponentName(context, salve.services.SalveMusicListener.class).flattenToString())
                : new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        try { context.startActivity(detail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true; }
        catch (RuntimeException unavailable) {
            try {
                context.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            } catch (RuntimeException none) { return false; }
        }
    }

    /** Reproductor de otra app: el de Spotify si existe; si no, el que esté sonando; si no, el primero. */
    static MediaController controller(Context context) {
        if (!hasAccess(context)) return null;
        try {
            MediaSessionManager manager = context.getSystemService(MediaSessionManager.class);
            if (manager == null) return null;
            List<MediaController> sessions = manager.getActiveSessions(
                    new ComponentName(context, salve.services.SalveMusicListener.class));
            MediaController spotify = null, playing = null, first = null;
            for (MediaController c : sessions) {
                if (context.getPackageName().equals(c.getPackageName())) continue; // nunca la propia Salve
                if (first == null) first = c;
                if (PhoneActions.SPOTIFY.equals(c.getPackageName()) && spotify == null) spotify = c;
                PlaybackState state = c.getPlaybackState();
                if (playing == null && state != null && state.getState() == PlaybackState.STATE_PLAYING) playing = c;
            }
            return spotify != null ? spotify : playing != null ? playing : first;
        } catch (SecurityException | IllegalStateException denied) {
            Log.w(TAG, "Sin acceso a las sesiones multimedia: " + denied.getClass().getSimpleName());
            return null;
        }
    }

    public static boolean play(Context context) {
        MediaController c = controller(context);
        if (c != null) { c.getTransportControls().play(); return true; }
        return mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY);
    }

    public static boolean pause(Context context) {
        MediaController c = controller(context);
        if (c != null) { c.getTransportControls().pause(); return true; }
        return mediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE);
    }

    public static boolean next(Context context) {
        MediaController c = controller(context);
        if (c != null) { c.getTransportControls().skipToNext(); return true; }
        return mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT);
    }

    public static boolean previous(Context context) {
        MediaController c = controller(context);
        if (c != null) { c.getTransportControls().skipToPrevious(); return true; }
        return mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }

    /** «Título», de «Artista» de lo que suena; null si no se puede saber. */
    public static String nowPlaying(Context context) {
        MediaController c = controller(context);
        if (c == null) return null;
        MediaMetadata m = c.getMetadata();
        if (m == null) return null;
        String title = text(m, MediaMetadata.METADATA_KEY_TITLE);
        if (title == null) title = text(m, MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null) return null;
        String artist = text(m, MediaMetadata.METADATA_KEY_ARTIST);
        if (artist == null) artist = text(m, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE);
        PlaybackState state = c.getPlaybackState();
        boolean paused = state == null || state.getState() != PlaybackState.STATE_PLAYING;
        return "«" + title + "»" + (artist == null ? "" : ", de " + artist) + (paused ? " (en pausa)" : "");
    }

    /**
     * Buscar y reproducir de verdad en el reproductor de Spotify (como el Asistente). Solo si ya hay
     * reproductor de Spotify y permiso; si no, false y se usa el camino de siempre (abrir Spotify).
     */
    public static boolean playFromSearch(Context context, String query, Bundle extras) {
        MediaController c = controller(context);
        if (c == null || !PhoneActions.SPOTIFY.equals(c.getPackageName())) return false;
        try { c.getTransportControls().playFromSearch(query, extras == null ? new Bundle() : extras); return true; }
        catch (RuntimeException refused) { return false; }
    }

    /** Sin permiso: el botón de reproducción, como unos auriculares, dirigido a Spotify (o a lo que suene). */
    private static boolean mediaKey(Context context, int keyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
        if (PhoneActions.isInstalled(context, PhoneActions.SPOTIFY)) {
            try {
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(PhoneActions.SPOTIFY)
                        .putExtra(Intent.EXTRA_KEY_EVENT, down));
                context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(PhoneActions.SPOTIFY)
                        .putExtra(Intent.EXTRA_KEY_EVENT, up));
                return true;
            } catch (RuntimeException refused) { /* sigue con el sistema */ }
        }
        AudioManager audio = context.getSystemService(AudioManager.class);
        if (audio == null) return false;
        audio.dispatchMediaKeyEvent(down);
        audio.dispatchMediaKeyEvent(up);
        return true;
    }

    private static String text(MediaMetadata m, String key) {
        CharSequence v = m.getText(key);
        String s = v == null ? null : v.toString().trim();
        return s == null || s.isEmpty() ? null : s;
    }
}
