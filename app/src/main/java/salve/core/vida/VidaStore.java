package salve.core.vida;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Un archivo local con el registro de Vida. Sigue el patrón de {@code PersonalBudgetStore}:
 * escritura atómica, límite de tamaño y nunca sobrescribe un archivo ilegible.
 * Quien lo crea lo coloca en el directorio no-backup de Android (no sube a la nube).
 */
public final class VidaStore {
    static final int MAX_BYTES = 4_000_000; // años de series de Hevy caben de sobra
    private final File file;

    public VidaStore(File file) { this.file = file; }

    public VidaLog load() throws IOException {
        if (!file.exists()) return new VidaLog();
        if (!file.isFile() || file.length() > MAX_BYTES) throw new IOException("Registro de Vida ilegible.");
        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        try { return VidaLog.parse(text); }
        catch (RuntimeException invalid) { throw new IOException("No se pudo leer el registro de Vida.", invalid); }
    }

    public void save(VidaLog log) throws IOException {
        byte[] bytes = log.serialize().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Registro de Vida demasiado grande.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("No se pudo preparar el almacenamiento.");
        File temporary = new File(parent, file.getName() + ".pending");
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) { out.write(bytes); out.getFD().sync(); }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary.toPath()); }
    }

    public void clear() throws IOException {
        Files.deleteIfExists(file.toPath());
        Files.deleteIfExists(new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".pending").toPath());
    }
}
