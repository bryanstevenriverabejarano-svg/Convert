package salve.core.goals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Local bounded storage. The Android caller supplies a file in its no-backup directory. */
public final class GoalFileStore implements GoalAutonomy.Store {
    private static final int MAX_BYTES = 128_000;
    private final File file;

    public GoalFileStore(File file) { this.file = file; }

    @Override public String read() throws IOException {
        if (!file.exists()) return null;
        if (!file.isFile() || file.length() > MAX_BYTES) throw new IOException("Objetivos ilegibles.");
        try (InputStream in = Files.newInputStream(file.toPath());
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > MAX_BYTES) throw new IOException("Objetivos demasiado grandes.");
                out.write(buffer, 0, count);
            }
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(out.toByteArray())).toString();
        }
    }

    @Override public void write(String json) throws IOException {
        if (json == null) throw new IOException("Estado ausente.");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_BYTES) throw new IOException("Objetivos demasiado grandes.");
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Almacenamiento no disponible.");
        File temporary = new File(parent, file.getName() + ".pending");
        try {
            try (FileOutputStream out = new FileOutputStream(temporary)) {
                out.write(bytes);
                out.getFD().sync();
            }
            // No non-atomic fallback: a failed write must preserve the previous journal.
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }
}
