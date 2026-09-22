package salve.core.autonomy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bounded, atomic local registry; the caller provides a private, no-backup path. */
public final class FileToolStateStore implements AutonomousToolLab.StateStore {
    static final int MAX_BYTES = 1024 * 1024;
    private final File file;

    public FileToolStateStore(File file) {
        if (file == null) throw new IllegalArgumentException("Archivo ausente.");
        this.file = file.getAbsoluteFile();
    }

    @Override public synchronized String read() throws IOException {
        if (!file.exists()) return null;
        if (!file.isFile() || file.length() > MAX_BYTES)
            throw new IOException("Registro de herramientas ilegible o demasiado grande.");
        try (InputStream in = Files.newInputStream(file.toPath());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int count;
            while ((count = in.read(chunk)) != -1) {
                if (out.size() + count > MAX_BYTES)
                    throw new IOException("Registro de herramientas demasiado grande.");
                out.write(chunk, 0, count);
            }
            return StandardCharsets.UTF_8.newDecoder()
                    .decode(ByteBuffer.wrap(out.toByteArray())).toString();
        }
    }

    @Override public synchronized void write(String json) throws IOException {
        if (json == null) throw new IOException("Estado ausente.");
        if (json.length() > MAX_BYTES) throw new IOException("Registro demasiado grande.");
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(json));
        if (encoded.remaining() > MAX_BYTES) throw new IOException("Registro demasiado grande.");
        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Almacenamiento de herramientas no disponible.");
        Path temporary = Files.createTempFile(parent.toPath(), file.getName() + ".", ".pending");
        try {
            try (FileOutputStream out = new FileOutputStream(temporary.toFile())) {
                out.write(bytes);
                out.getFD().sync();
            }
            // A failed atomic move preserves the previous state. No truncating fallback.
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
