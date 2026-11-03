package salve.car;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.media.MediaBrowserService;
import java.util.ArrayList;
import java.util.List;

/**
 * Salve en la pantalla del coche como «reproductor». Android Auto 17.7+ no deja pasar las apps de
 * plantillas instaladas fuera de la Play Store («Fuentes desconocidas» solo cubre multimedia, avisos,
 * SMS…), pero sí las multimedia. Así el coche enseña su imagen, su estado («Te escucho…»,
 * «Hablando…») y la última frase que dijo; Play (en la pantalla o en el volante) abre la conversación
 * por voz y Pausa la termina. No reproduce audio propio: la voz sigue siendo la de siempre.
 * Solo Android Auto puede conectarse (la última frase puede ser privada).
 */
public final class SalveMediaService extends MediaBrowserService {
    private static final String ROOT = "salve_root", TALK = "salve_talk", END = "salve_end";
    /** Al conectar el coche, Android Auto puede pulsar Play solo («Iniciar música automáticamente»). */
    private static final long IGNORE_AUTOPLAY_MS = 8000;
    private static final long START_CHECK_MS = 5000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final CarVoiceStatus.Listener statusListener = () -> main.post(this::publish);
    private MediaSession session;
    private Bitmap art;
    private long createdAt;
    private String notice;

    @Override public void onCreate() {
        super.onCreate();
        createdAt = SystemClock.elapsedRealtime();
        try { art = BitmapFactory.decodeResource(getResources(), com.salve.app.R.drawable.salve_car); }
        catch (RuntimeException | OutOfMemoryError ignored) { art = null; }
        session = new MediaSession(this, "Salve-coche");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() {
                // Play automático al conectar el coche: no abrir el micrófono sin que Bryan lo pida.
                if (SystemClock.elapsedRealtime() - createdAt < IGNORE_AUTOPLAY_MS) return;
                talk();
            }
            @Override public void onPlayFromMediaId(String mediaId, Bundle extras) {
                if (END.equals(mediaId)) end(); else talk();
            }
            @Override public void onPause() { end(); }
            @Override public void onStop() { end(); }
        }, main);
        setSessionToken(session.getSessionToken());
        session.setActive(true);
        CarVoiceStatus.addListener(statusListener);
        publish();
    }

    @Override public void onDestroy() {
        CarVoiceStatus.removeListener(statusListener);
        main.removeCallbacksAndMessages(null);
        if (session != null) { session.setActive(false); session.release(); }
        super.onDestroy();
    }

    @Override public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        return CarMediaPolicy.allowed(clientPackageName, getPackageName()) ? new BrowserRoot(ROOT, null) : null;
    }

    @Override public void onLoadChildren(String parentId, Result<List<MediaBrowser.MediaItem>> result) {
        List<MediaBrowser.MediaItem> items = new ArrayList<>();
        if (ROOT.equals(parentId)) {
            boolean active = CarVoiceStatus.active();
            items.add(item(active ? END : TALK, active ? "Terminar conversación" : "Hablar con Salve",
                    CarVoiceStatus.line()));
        }
        result.sendResult(items);
    }

    private MediaBrowser.MediaItem item(String id, String title, String subtitle) {
        MediaDescription.Builder d = new MediaDescription.Builder().setMediaId(id).setTitle(title).setSubtitle(subtitle);
        if (art != null) d.setIconBitmap(art);
        return new MediaBrowser.MediaItem(d.build(), MediaBrowser.MediaItem.FLAG_PLAYABLE);
    }

    private void talk() {
        notice = CarVoiceControl.talk(this) ? "Abriendo la conversación…" : CarMediaPolicy.TAP_PHONE;
        publish();
        main.postDelayed(() -> {
            if (!CarVoiceStatus.active()) { notice = CarMediaPolicy.TAP_PHONE; publish(); }
        }, START_CHECK_MS);
    }

    private void end() {
        CarVoiceControl.end();
        notice = null;
        publish();
    }

    /** Estado, imagen y última frase para la pantalla del coche. */
    private void publish() {
        if (session == null) return;
        boolean active = CarVoiceStatus.active();
        if (active) notice = null;
        String title = notice != null ? notice : CarVoiceStatus.line();
        String reply = CarVoiceStatus.lastReply();
        MediaMetadata.Builder m = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, active ? END : TALK)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, "Salve")
                .putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, reply != null ? reply : CarVoiceStatus.hint());
        if (art != null) {
            m.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art);
            m.putBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON, art);
        }
        session.setMetadata(m.build());
        session.setPlaybackState(new PlaybackState.Builder()
                .setState(active ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, active ? 1f : 0f)
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP
                        | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID)
                .build());
        notifyChildrenChanged(ROOT);
    }
}
