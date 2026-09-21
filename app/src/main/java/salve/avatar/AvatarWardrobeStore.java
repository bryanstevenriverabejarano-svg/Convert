package salve.avatar;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Shared wardrobe; file IO runs on one worker and immutable selections publish on main. */
public final class AvatarWardrobeStore {
    private static AvatarWardrobeStore instance;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Runnable> listeners = new ArrayList<>();
    private volatile List<AvatarDesignSpec> designs = Collections.singletonList(AvatarDesignSpec.original());
    private volatile List<AvatarDesignCatalog.Template> templates = Collections.singletonList(AvatarDesignCatalog.template("original_dress"));
    private volatile AvatarDesignSpec selected = AvatarDesignSpec.original();
    private volatile String lastError = "";
    private volatile boolean ready;
    private AvatarWardrobeRepository repository;

    private AvatarWardrobeStore(Context context) {
        Context app = context.getApplicationContext();
        io.execute(() -> {
            List<AvatarDesignCatalog.Template> available = new ArrayList<>();
            for (AvatarDesignCatalog.Template template : AvatarDesignCatalog.templates()) {
                if (template.assetPath == null || validArtwork(app, template.assetPath)) available.add(template);
            }
            templates = Collections.unmodifiableList(available);
            try {
                repository = new AvatarWardrobeRepository(new File(app.getFilesDir(), "avatar/wardrobe.json"));
                ready = true; publish("");
            } catch (IOException | RuntimeException error) {
                publish("No pude abrir el armario guardado. El archivo se ha conservado sin reemplazarlo.");
            }
        });
    }
    public static synchronized AvatarWardrobeStore get(Context context) {
        if (instance == null) instance = new AvatarWardrobeStore(context);
        return instance;
    }
    public AvatarDesignSpec selected() { return selected; }
    public List<AvatarDesignSpec> list() { return designs; }
    public List<AvatarDesignCatalog.Template> availableTemplates() { return templates; }
    public boolean isReady() { return ready; }
    public String lastError() { return lastError; }
    public void addListener(Runnable listener) { requireMain(); if (!listeners.contains(listener)) listeners.add(listener); }
    public void removeListener(Runnable listener) { requireMain(); listeners.remove(listener); }
    public void execute(AvatarDesignRequest request, Consumer<String> completed) { execute(request, () -> true, completed); }
    public void execute(AvatarDesignRequest request, BooleanSupplier wanted, Consumer<String> completed) {
        io.execute(() -> executeOnIo(request, wanted, completed));
    }
    private void executeOnIo(AvatarDesignRequest request, BooleanSupplier wanted, Consumer<String> completed) {
            if (!wanted.getAsBoolean()) return;
            String result;
            try {
                if (repository == null) throw new IOException("Armario no disponible");
                switch (request.kind) {
                    case LIST:
                        StringBuilder names = new StringBuilder("Diseños guardados: ");
                        for (AvatarDesignSpec design : repository.list()) {
                            if (names.length() > 20) names.append("; ");
                            names.append(design.name);
                            if (design.id.equals(repository.selected().id)) names.append(" (puesto)");
                        }
                        result = names.append(". Puedes pedirme que me ponga o elimine un diseño por su nombre.").toString();
                        break;
                    case CREATE:
                        ensureAvailable(request.template);
                        AvatarDesignSpec created = repository.create(request);
                        result = "Guardé el diseño ‘" + created.name + "’ sobre la plantilla "
                                + AvatarDesignCatalog.template(created.template).name + "."
                                + (request.wear ? " Lo seleccioné para mi vestuario." : " Está disponible en mi armario.");
                        break;
                    case WEAR:
                        AvatarDesignSpec target = resolve(request); ensureAvailable(target.template);
                        repository.select(target.id); result = "Seleccioné el diseño ‘" + target.name + "’."; break;
                    case DELETE:
                        AvatarDesignSpec removed = resolve(request); repository.delete(removed.id);
                        result = "Eliminé el diseño ‘" + removed.name + "’ del armario."; break;
                    default: throw new IllegalArgumentException("Acción de armario no disponible.");
                }
                publish("");
            } catch (IllegalArgumentException invalid) { result = invalid.getMessage(); }
            catch (IOException | RuntimeException failure) { result = "No pude guardar el cambio en el armario. El diseño anterior se conserva."; }
            String answer = result;
            main.post(() -> { if (wanted.getAsBoolean() && completed != null) completed.accept(answer); });
    }
    /** A bundled template can be worn directly; repeated requests reuse its unchanged design. */
    public void wearTemplate(String templateId, Consumer<String> completed) {
        wearTemplate(templateId, () -> true, completed);
    }
    public void wearTemplate(String templateId, BooleanSupplier wanted, Consumer<String> completed) {
        io.execute(() -> {
            if (!wanted.getAsBoolean()) return;
            if (repository == null) {
                main.post(() -> { if (wanted.getAsBoolean()) completed.accept("El armario no está disponible."); }); return;
            }
            try {
                ensureAvailable(templateId);
                if (templateId.equals("original_dress")) {
                    executeOnIo(AvatarDesignRequest.select(AvatarDesignSpec.ORIGINAL_ID), wanted, completed); return;
                }
                for (AvatarDesignSpec design : repository.list()) {
                    if (design.template.equals(templateId) && design.palette == AvatarDesignSpec.Palette.ORIGINAL
                            && design.pattern == AvatarDesignSpec.Pattern.NONE) {
                        executeOnIo(AvatarDesignRequest.select(design.id), wanted, completed); return;
                    }
                }
                String base = AvatarDesignCatalog.template(templateId).name, name = base;
                for (int suffix = 2; containsName(name); suffix++) name = base + " " + suffix;
                executeOnIo(AvatarDesignRequest.create(name, templateId, AvatarDesignSpec.Palette.ORIGINAL, AvatarDesignSpec.Pattern.NONE, true), wanted, completed);
            } catch (IllegalArgumentException invalid) {
                main.post(() -> { if (wanted.getAsBoolean()) completed.accept(invalid.getMessage()); });
            }
        });
    }
    private boolean containsName(String name) {
        for (AvatarDesignSpec design : repository.list())
            if (AvatarDesignSpec.normalizedName(design.name).equals(AvatarDesignSpec.normalizedName(name))) return true;
        return false;
    }
    private AvatarDesignSpec resolve(AvatarDesignRequest request) {
        if (request.id != null) return repository.find(request.id);
        for (AvatarDesignSpec design : repository.list())
            if (AvatarDesignSpec.normalizedName(design.name).equals(AvatarDesignSpec.normalizedName(request.name))) return design;
        throw new IllegalArgumentException("No hay un diseño guardado con ese nombre.");
    }
    private void ensureAvailable(String id) {
        for (AvatarDesignCatalog.Template template : templates) if (template.id.equals(id)) return;
        throw new IllegalArgumentException("Esa plantilla ilustrada no está disponible en esta instalación.");
    }
    private void publish(String error) {
        List<AvatarDesignSpec> saved = repository == null ? designs : repository.list();
        AvatarDesignSpec choice = repository == null ? AvatarDesignSpec.original() : repository.selected();
        String issue = error;
        try { ensureAvailable(choice.template); }
        catch (IllegalArgumentException missing) { choice = AvatarDesignSpec.original(); issue = "La plantilla seleccionada no está disponible; muestro el vestido original."; }
        AvatarDesignSpec current = choice; String message = issue;
        main.post(() -> {
            designs = saved; selected = current; lastError = message;
            for (Runnable listener : new ArrayList<>(listeners)) listener.run();
        });
    }
    private static boolean validArtwork(Context context, String path) {
        try (InputStream stream = context.getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
            if (options.outWidth != 1024 || options.outHeight != 1536 || !"image/png".equals(options.outMimeType)) return false;
        } catch (IOException | RuntimeException unavailable) { return false; }
        // A valid header alone does not prove the PNG pixels can actually be decoded.
        try (InputStream stream = context.getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            Bitmap decoded = BitmapFactory.decodeStream(stream, null, options);
            if (decoded == null) return false;
            boolean valid = decoded.getWidth() == 1024 && decoded.getHeight() == 1536;
            decoded.recycle(); return valid;
        } catch (IOException | RuntimeException | OutOfMemoryError unavailable) { return false; }
    }
    private static void requireMain() {
        if (Looper.myLooper() != Looper.getMainLooper()) throw new IllegalStateException("Observe the wardrobe on main");
    }
}
