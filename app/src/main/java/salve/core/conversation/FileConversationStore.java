package salve.core.conversation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/** Bounded private checkpoint. One I/O queue also orders writes across activity recreation. */
public final class FileConversationStore {
    private static final int MAGIC = 0x53414C31; // SAL1, versioned file format
    private static final int MAX_MESSAGES = 16, MAX_CHARS = 8000, MAX_BYTES = 48_000;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "salve-conversation-checkpoint");
        thread.setDaemon(true);
        return thread;
    });
    private final File file;
    private volatile String warning = "";
    private volatile boolean unreadable;

    public FileConversationStore(File file) { this.file = file; }

    public List<ChatMessage> load() {
        try { return IO.submit(this::read).get(); }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            unreadable = true;
            warning = "No pude recuperar el hilo anterior; el archivo se conserva.";
        } catch (ExecutionException failed) {
            unreadable = true;
            warning = "No pude recuperar el hilo anterior; el archivo se conserva.";
        }
        return Collections.emptyList();
    }

    private List<ChatMessage> read() throws IOException {
        if (!file.exists()) return Collections.emptyList();
        if (file.length() > MAX_BYTES) throw new IOException("Checkpoint fuera de límites");
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            if (input.readInt() != MAGIC) throw new IOException("Versión desconocida");
            int count = input.readInt();
            if (count < 0 || count > MAX_MESSAGES) throw new IOException("Conteo inválido");
            List<ChatMessage> messages = new ArrayList<>();
            int chars = 0;
            for (int i = 0; i < count; i++) {
                int role = input.readUnsignedByte();
                long timestamp = input.readLong();
                String content = input.readUTF();
                chars += content.length();
                if (role > 1 || timestamp < 0 || content.trim().isEmpty() || chars > MAX_CHARS)
                    throw new IOException("Mensaje inválido");
                messages.add(new ChatMessage(ChatMessage.Role.values()[role], content, timestamp));
            }
            if (input.read() != -1) throw new IOException("Datos adicionales");
            return messages;
        }
    }

    public void save(List<ChatMessage> messages) {
        final List<ChatMessage> copy = new ArrayList<>(messages);
        IO.execute(() -> {
            // Never overwrite a checkpoint that failed to load. Explicit clear can reset it.
            if (unreadable && !copy.isEmpty()) return;
            try {
                write(copy);
                unreadable = false;
                warning = "";
            } catch (IOException | RuntimeException failed) {
                warning = "No pude guardar el hilo; los mensajes nuevos podrían perderse al cerrar Salve.";
            }
        });
    }

    private void write(List<ChatMessage> messages) throws IOException {
        if (messages.size() > MAX_MESSAGES) throw new IOException("Demasiados mensajes");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC); output.writeInt(messages.size());
            int chars = 0;
            for (ChatMessage message : messages) {
                chars += message.getContent().length();
                if (chars > MAX_CHARS || message.getContent().isEmpty() || message.getTimestamp() < 0)
                    throw new IOException("Mensaje fuera de límites");
                output.writeByte(message.getRole().ordinal());
                output.writeLong(message.getTimestamp());
                output.writeUTF(message.getContent());
            }
        }
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Directorio no disponible");
        File temporary = File.createTempFile("conversation-", ".tmp", parent);
        try {
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                bytes.writeTo(output); output.getFD().sync();
            }
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary.toPath()); }
    }

    public String warning() { return warning; }

    /** A test/maintenance barrier; normal chat writes never wait on disk. */
    public static void awaitWrites() throws InterruptedException, ExecutionException {
        IO.submit(() -> { }).get();
    }
}
