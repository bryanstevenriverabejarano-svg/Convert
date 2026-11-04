package salve.core;

import java.util.Locale;
import java.util.UUID;

/**
 * Preserve an imported GGUF's quantization suffix after its atomic move.
 * In particular, Q4_0 needs to remain visible for the optional Hexagon admission
 * policy; keeping a generic imported-XXXX.gguf name would prevent NPU selection.
 * Filenames originate from untrusted content providers: never trust separators.
 */
public final class LocalModelImportName {
    private LocalModelImportName() { }

    public static String forDisplayName(String displayName, UUID id) {
        if (displayName == null || id == null) throw new IllegalArgumentException("Nombre del modelo ausente");
        String safe = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.length() > 120) safe = safe.substring(safe.length() - 120);
        String lower = safe.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".gguf") && !lower.endsWith(".litertlm") && !lower.endsWith(".task")) {
            throw new IllegalArgumentException("Selecciona un modelo .gguf, .litertlm o .task.");
        }
        // A fixed 'import-' prefix and a random UUID make collisions unlikely.
        // The original suffix is preserved, not inferred from the model contents.
        return "import-" + id + "-" + safe;
    }
}
