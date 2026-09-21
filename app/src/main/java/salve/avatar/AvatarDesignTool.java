package salve.avatar;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Real local design tool. It composes reviewed templates; it has no image-generation provider. */
public final class AvatarDesignTool {
    private static final Pattern TOOL = Pattern.compile("\"tool\"\\s*:\\s*\"AVATAR_[A-Z_]+\"");
    private final AvatarWardrobeStore store;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean closed = new AtomicBoolean();
    public AvatarDesignTool(Context context) { store = AvatarWardrobeStore.get(context); }
    public static String instruction() {
        return "\nVESTUARIO LOCAL: puedes crear diseños combinando plantillas ilustradas existentes; no generas PNG libres. "
                + "Plantillas: original_dress, pajamas, explorer. Colores: ORIGINAL,TURQUOISE,LAVENDER,ROSE,BLUE,AMBER. "
                + "Patrones: NONE,STARS,STRIPES. Usa un único JSON sin sufijos ni texto exterior. "
                + "Crear: {\"tool\":\"AVATAR_CREATE\",\"name\":\"Noche lavanda\",\"template\":\"pajamas\",\"color\":\"LAVENDER\",\"pattern\":\"STARS\",\"wear\":true}. "
                + "Listar: {\"tool\":\"AVATAR_LIST\"}. Vestir: {\"tool\":\"AVATAR_WEAR\",\"name\":\"Noche lavanda\"}. "
                + "Eliminar sólo si lo pide el usuario: {\"tool\":\"AVATAR_DELETE\",\"name\":\"Noche lavanda\"}. "
                + "No anuncies éxito: la herramienta confirmará el resultado.\n";
    }
    public boolean tryExecute(String raw, Consumer<String> completed) {
        if (raw == null) return false;
        String text = raw.trim();
        if (text.startsWith("```json\n") && text.endsWith("```")) text = text.substring(8, text.length() - 3).trim();
        else if (text.startsWith("```\n") && text.endsWith("```")) text = text.substring(4, text.length() - 3).trim();
        if (!text.startsWith("{") || !TOOL.matcher(text).find()) return false;
        if (closed.get()) return true;
        try {
            AvatarDesignRequest request = AvatarDesignRequest.parse(text);
            store.execute(request, () -> !closed.get(), completed);
        } catch (IllegalArgumentException invalid) {
            main.post(() -> { if (!closed.get()) completed.accept("No cambié el armario: " + invalid.getMessage()); });
        }
        return true;
    }
    public void wearTemplate(String templateId, Consumer<String> completed) {
        if (!closed.get()) store.wearTemplate(templateId, () -> !closed.get(), completed);
    }
    public void close() { closed.set(true); }
}
