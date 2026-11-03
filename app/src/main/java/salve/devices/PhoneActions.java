package salve.devices;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import salve.core.devicecontrol.AppActionCommand;
import salve.core.devicecontrol.AppNameMatcher;

/** Acciones visibles en otras apps del propio móvil. No envían nada ni llaman sin confirmación. */
public final class PhoneActions {
    public static final String MAPS = "com.google.android.apps.maps";
    public static final String SPOTIFY = "com.spotify.music";
    private static final int MAX_CONTACTS = 6;

    private PhoneActions() { }

    /** Ruta en Google Maps; si no está, cualquier app de mapas; si no, la web de Maps. */
    public static boolean navigate(Context context, String destination) {
        String q = Uri.encode(destination);
        Intent maps = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + q)).setPackage(MAPS);
        Intent anyMap = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + q));
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + q));
        return start(context, maps) || start(context, anyMap) || start(context, web);
    }

    /** Busca y reproduce en Spotify; si no está, en la app de música que lo admita. */
    public static boolean playMusic(Context context, String query) {
        // Con su reproductor a mano, Spotify lo pone de verdad (abrirlo por intent solo enseña resultados).
        if (MusicController.playFromSearch(context, query, null)) return true;
        Intent spotify = playFromSearch(query).setPackage(SPOTIFY);
        Intent spotifySearch = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(query))).setPackage(SPOTIFY);
        boolean opened = start(context, spotify) || start(context, spotifySearch) || start(context, playFromSearch(query));
        if (opened) playWhenSpotifyIsReady(context, query, null);
        return opened;
    }

    /**
     * Spotify recién abierto todavía no tiene reproductor: en cuanto aparece, se le pide que lo ponga
     * (si Salve tiene «Acceso a notificaciones»). Si no, se queda en la búsqueda, como antes.
     */
    private static void playWhenSpotifyIsReady(Context context, String query, android.os.Bundle extras) {
        if (!MusicController.hasAccess(context)) return;
        Context app = context.getApplicationContext();
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        java.util.concurrent.atomic.AtomicBoolean done = new java.util.concurrent.atomic.AtomicBoolean();
        long[] delays = {1500, 3000, 5000};
        for (long delay : delays) {
            main.postDelayed(() -> {
                // Una sola vez, en cuanto el reproductor de Spotify exista.
                if (!done.get() && MusicController.playFromSearch(app, query, extras)) done.set(true);
            }, delay);
        }
    }

    /** ¿Está instalada esta app? (Spotify y Maps están declarados en las consultas del manifest). */
    public static boolean isInstalled(Context context, String packageName) {
        try { context.getPackageManager().getPackageInfo(packageName, 0); return true; }
        catch (PackageManager.NameNotFoundException absent) { return false; }
    }

    /**
     * Pone una lista por su nombre. Se pide como «lista» (no como canción) para que Spotify busque entre
     * las listas, y las tuyas suelen salir las primeras; si no lo acepta, abre la búsqueda con ese nombre.
     */
    public static boolean playPlaylist(Context context, String name) {
        android.os.Bundle focus = playlistExtras(name);
        if (MusicController.playFromSearch(context, name, focus)) return true;
        Intent playlist = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
                .putExtra(MediaStore.EXTRA_MEDIA_PLAYLIST, name)
                .putExtra(SearchManager.QUERY, name)
                .setPackage(SPOTIFY);
        boolean opened = start(context, playlist) || searchMusic(context, name) || start(context, playFromSearch(name));
        if (opened) playWhenSpotifyIsReady(context, name, focus);
        return opened;
    }

    private static android.os.Bundle playlistExtras(String name) {
        android.os.Bundle b = new android.os.Bundle();
        b.putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE);
        b.putString(MediaStore.EXTRA_MEDIA_PLAYLIST, name);
        return b;
    }

    /** «Tus me gusta» de Spotify; si no se puede poner sola, abre la lista para darle a reproducir. */
    public static boolean playLiked(Context context) {
        if (MusicController.playFromSearch(context, "Tus me gusta", playlistExtras("Tus me gusta"))) return true;
        Intent liked = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
                .putExtra(MediaStore.EXTRA_MEDIA_PLAYLIST, "Tus me gusta")
                .putExtra(SearchManager.QUERY, "Tus me gusta")
                .setPackage(SPOTIFY);
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:collection:tracks")).setPackage(SPOTIFY);
        boolean opened = start(context, liked) || start(context, open);
        if (opened) playWhenSpotifyIsReady(context, "Tus me gusta", playlistExtras("Tus me gusta"));
        return opened;
    }

    /** Abre la búsqueda de Spotify con esos resultados, sin poner nada. */
    public static boolean searchMusic(Context context, String query) {
        return start(context, new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(query))).setPackage(SPOTIFY));
    }

    private static Intent playFromSearch(String query) {
        return new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                .putExtra(SearchManager.QUERY, query);
    }

    /** App instalada que el usuario puede abrir desde el lanzador. */
    public static final class InstalledApp {
        public final String label, packageName;
        InstalledApp(String label, String packageName) { this.label = label; this.packageName = packageName; }
    }

    /** La app del lanzador cuyo nombre encaja claramente con lo dicho; null si no hay ninguna o hay empate. */
    public static InstalledApp findApp(Context context, String spokenName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            Map<String, String> byPackage = new LinkedHashMap<>();
            for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
                if (info.activityInfo == null || !info.activityInfo.exported || !info.activityInfo.enabled) continue;
                String pkg = info.activityInfo.packageName;
                if (context.getPackageName().equals(pkg) || byPackage.containsKey(pkg)) continue;
                byPackage.put(pkg, info.loadLabel(pm).toString());
            }
            List<String> packages = new ArrayList<>(byPackage.keySet());
            List<String> labels = new ArrayList<>(byPackage.values());
            int index = AppNameMatcher.bestMatch(spokenName, labels, packages);
            return index < 0 ? null : new InstalledApp(labels.get(index), packages.get(index));
        } catch (RuntimeException unavailable) { return null; }
    }

    /** Abre la app (como pulsar su icono). Abrir se deshace solo: no hace falta confirmación. */
    public static boolean openApp(Context context, InstalledApp app) {
        if (app == null) return false;
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        return launch != null && start(context, launch);
    }

    private static boolean start(Context context, Intent intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            return false;
        }
    }

    public static final class Contact {
        public final String name, number, label;
        public Contact(String name, String number, String label) {
            this.name = name; this.number = number; this.label = label == null ? "" : label;
        }
        public String describe() { return name + (label.isEmpty() ? "" : " (" + label + ")") + "\n" + number; }
    }

    /** Requiere READ_CONTACTS. Coincidencias por nombre, favoritos primero, sin números repetidos. */
    public static List<Contact> findContacts(Context context, String name) {
        Map<String, Contact> unique = new LinkedHashMap<>();
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL};
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ? ESCAPE '\\'";
        String order = ContactsContract.CommonDataKinds.Phone.STARRED + " DESC, "
                + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";
        try (Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, new String[]{AppActionCommand.likeArgument(name)}, order)) {
            while (cursor != null && cursor.moveToNext() && unique.size() < MAX_CONTACTS) {
                String display = cursor.getString(0), number = cursor.getString(1);
                if (display == null || number == null || number.trim().isEmpty()) continue;
                CharSequence label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(
                        context.getResources(), cursor.getInt(2), cursor.getString(3));
                String key = number.replaceAll("[^0-9+]", "");
                if (!unique.containsKey(key)) unique.put(key, new Contact(display, number.trim(), label == null ? "" : label.toString()));
            }
        } catch (RuntimeException unreadable) { return new ArrayList<>(); }
        return new ArrayList<>(unique.values());
    }
}
