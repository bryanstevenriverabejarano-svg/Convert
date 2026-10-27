package salve.avatar;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Las versiones del cuerpo de Salve, en orden de evolución. La primera es la actual; las anteriores se
 * conservan intactas en el Vestuario como registro de cómo ha ido cambiando.
 * Cada versión es una carpeta de assets avatar/&lt;id&gt;/ con base.png, manifest.json y su capa astral.
 */
public final class AvatarBodyVersions {
    public static final class Version {
        public final String id, name, approved, note;
        Version(String id, String name, String approved, String note) {
            this.id = id; this.name = name; this.approved = approved; this.note = note;
        }
    }

    public static final String CURRENT = "realistic";
    public static final String V1 = "realistic_v1";
    /** Variantes del Vestuario que comparten las animaciones de la actual y solo cambian su base. */
    public static final String OPEN = "realistic_abierto", OCT26 = "realistic_2610";
    private static final List<Version> ALL = Collections.unmodifiableList(Arrays.asList(
            new Version(CURRENT, "Salve actual", "27 oct 2026",
                    "Traje blanco de una pieza con el pecho cubierto, colas y líneas cian que recorren su cuerpo desde el núcleo del cuello; anda, gira, baila, duerme y se despierta."),
            new Version(OPEN, "Salve · escote abierto", "27 oct 2026",
                    "La misma Salve con el escote abierto; mismas animaciones."),
            new Version(OCT26, "Salve del 26 de octubre", "26 oct 2026",
                    "La base de las 32 ilustraciones, con las bocas H1–H3 dibujadas; mismas animaciones."),
            new Version(V1, "Salve v1", "12 oct 2026",
                    "Su primer cuerpo realista: traje blanco y circuitos azules, con pasos, baile y posturas propios.")));

    private static final String PREFS = "salve_avatar_body", KEY = "version";
    private static volatile String selected;
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();

    private AvatarBodyVersions() { }

    public static List<Version> all() { return ALL; }

    public static Version find(String id) {
        for (Version v : ALL) if (v.id.equals(id)) return v;
        return null;
    }

    /** Carpeta del cuerpo que se dibuja ahora (la actual si no se eligió otra o si la guardada ya no existe). */
    public static String selected(Context context) {
        String value = selected;
        if (value == null) {
            String saved = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, CURRENT);
            value = find(saved) == null ? CURRENT : saved;
            selected = value;
        }
        return value;
    }

    public static void select(Context context, String id) {
        if (find(id) == null) throw new IllegalArgumentException("Esa versión de Salve no existe.");
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY, id).apply();
        selected = id;
        for (Runnable listener : LISTENERS) listener.run();
    }

    public static void addListener(Runnable listener) { if (!LISTENERS.contains(listener)) LISTENERS.add(listener); }
    public static void removeListener(Runnable listener) { LISTENERS.remove(listener); }
}
