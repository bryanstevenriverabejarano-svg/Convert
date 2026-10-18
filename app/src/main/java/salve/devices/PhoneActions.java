package salve.devices;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import salve.core.devicecontrol.AppActionCommand;

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
        Intent spotify = playFromSearch(query).setPackage(SPOTIFY);
        Intent spotifySearch = new Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(query))).setPackage(SPOTIFY);
        return start(context, spotify) || start(context, spotifySearch) || start(context, playFromSearch(query));
    }

    private static Intent playFromSearch(String query) {
        return new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                .putExtra(SearchManager.QUERY, query);
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
