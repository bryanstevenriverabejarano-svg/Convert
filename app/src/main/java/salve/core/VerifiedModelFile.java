package salve.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** Resumable, bounded file transfer. A model becomes visible only after size and SHA-256 match. */
public final class VerifiedModelFile {
    private static final long SPACE_MARGIN = 256L * 1024 * 1024;
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");
    private final OkHttpClient client;
    private volatile Call activeCall;
    private volatile boolean cancelRequested;

    public interface Progress {
        void update(long bytes, long total, boolean verifying) throws IOException;
    }

    public VerifiedModelFile(OkHttpClient client) {
        // Check every redirect before sending a request to its destination.
        this.client = client.newBuilder().followRedirects(false).followSslRedirects(false).build();
    }

    /** Interrupt an outstanding socket read as well as transfers/hashing between reads. One-shot instance. */
    public void cancel() {
        cancelRequested = true;
        Call call = activeCall;
        if (call != null) call.cancel();
    }

    public File download(String url, File target, long size, String sha256,
                         Progress progress, BooleanSupplier cancelled) throws IOException {
        if (size <= 0 || sha256 == null || !sha256.matches("[a-fA-F0-9]{64}")) {
            throw new IOException("El catálogo debe indicar tamaño y SHA-256 del modelo");
        }
        validateUrl(url);
        target = target.getAbsoluteFile();
        File parent = target.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("No se pudo crear la carpeta del modelo");
        // Also protects against independent workers/processes writing the same .part file.
        BooleanSupplier stopped = () -> cancelRequested || cancelled.getAsBoolean();
        try (java.io.RandomAccessFile lockFile = new java.io.RandomAccessFile(target + ".lock", "rw");
             java.nio.channels.FileLock ignored = acquireLock(lockFile.getChannel(), stopped)) {
            return downloadLocked(url, target, size, sha256, progress, stopped);
        } finally {
            activeCall = null;
        }
    }

    private static java.nio.channels.FileLock acquireLock(java.nio.channels.FileChannel channel,
                                                           BooleanSupplier cancelled) throws IOException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (true) {
            checkCancelled(cancelled);
            try {
                java.nio.channels.FileLock lock = channel.tryLock();
                if (lock != null) return lock;
            } catch (java.nio.channels.OverlappingFileLockException busy) {
                // A quickly resumed worker may start while its cancelled predecessor is closing the file.
            }
            if (System.nanoTime() >= deadline) throw new IOException("Ya hay una descarga de este modelo en curso");
            try { Thread.sleep(50); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Descarga pausada; el progreso se conserva");
            }
        }
    }

    private File downloadLocked(String url, File target, long size, String sha256,
                                Progress progress, BooleanSupplier cancelled) throws IOException {
        checkCancelled(cancelled);
        if (target.isFile() && target.length() == size) {
            progress.update(size, size, true);
            if (sha256.equalsIgnoreCase(digest(target, cancelled))) return target;
        }
        File partial = new File(target + ".part");
        if (partial.exists() && partial.length() > size) Files.delete(partial.toPath());
        long offset = partial.isFile() ? partial.length() : 0L;
        if (offset == size) {
            progress.update(size, size, true);
            if (sha256.equalsIgnoreCase(digest(partial, cancelled))) {
                return publish(partial, target, cancelled);
            }
            Files.delete(partial.toPath());
            offset = 0L;
        }
        ensureSpace(target.getParentFile(), size - offset);
        // One retry from zero if the server rejects a previously saved range.
        for (int attempt = 0; attempt < 2; attempt++) {
            checkCancelled(cancelled);
            try (Response response = open(url, offset, cancelled)) {
                if (response.code() == 416 && offset > 0 && attempt == 0) {
                    Files.deleteIfExists(partial.toPath());
                    offset = 0L;
                    ensureSpace(target.getParentFile(), size);
                    continue;
                }
                if (response.code() != 200 && response.code() != 206) {
                    throw new IOException("HTTP " + response.code() + " al descargar el modelo");
                }
                ResponseBody body = response.body();
                if (body == null) throw new IOException("Respuesta de descarga vacía");
                if (response.code() == 206) {
                    Matcher range = CONTENT_RANGE.matcher(value(response.header("Content-Range")));
                    if (!range.matches() || number(range.group(1)) != offset
                            || number(range.group(2)) != size - 1 || number(range.group(3)) != size) {
                        throw new IOException("El servidor devolvió un rango incompatible");
                    }
                } else {
                    // A server may ignore Range. Never append a complete 200 response.
                    ensureSpace(target.getParentFile(), size - offset);
                    offset = 0L;
                }
                if (body.contentLength() >= 0 && body.contentLength() != size - offset) {
                    throw new IOException("El tamaño publicado no coincide con la descarga");
                }
                String type = value(body.contentType() == null ? null : body.contentType().toString());
                if (type.contains("text/html") || type.contains("application/json")) {
                    throw new IOException("El enlace devolvió una página, no un modelo");
                }
                long written = offset;
                long lastProgress = 0L;
                try (InputStream input = body.byteStream();
                     FileOutputStream output = new FileOutputStream(partial, offset > 0)) {
                    byte[] buffer = new byte[256 * 1024];
                    while (true) {
                        checkCancelled(cancelled);
                        int count = input.read(buffer);
                        if (count < 0) break;
                        if (count > size - written) {
                            throw new IOException("El modelo supera el tamaño autorizado");
                        }
                        output.write(buffer, 0, count);
                        written += count;
                        long now = System.nanoTime();
                        if (now - lastProgress >= 500_000_000L || written == size) {
                            progress.update(written, size, false);
                            lastProgress = now;
                        }
                    }
                    output.getFD().sync();
                }
                if (written != size) throw new IOException("Descarga interrumpida; puedes reanudarla");
                progress.update(size, size, true);
                if (!sha256.equalsIgnoreCase(digest(partial, cancelled))) {
                    Files.deleteIfExists(partial.toPath());
                    throw new IOException("SHA-256 incorrecto; el archivo se ha descartado");
                }
                return publish(partial, target, cancelled);
            }
        }
        throw new IOException("No se pudo reanudar la descarga");
    }

    private Response open(String url, long offset, BooleanSupplier cancelled) throws IOException {
        String next = url;
        for (int redirect = 0; redirect <= 5; redirect++) {
            checkCancelled(cancelled);
            validateUrl(next);
            Request.Builder request = new Request.Builder().url(next).header("Accept-Encoding", "identity");
            if (offset > 0) request.header("Range", "bytes=" + offset + "-");
            Call call = client.newCall(request.build());
            activeCall = call;
            // Covers cancellation racing with assignment, before execute starts.
            if (cancelRequested || cancelled.getAsBoolean()) call.cancel();
            checkCancelled(cancelled);
            Response response = call.execute();
            int code = response.code();
            if (code != 301 && code != 302 && code != 303 && code != 307 && code != 308) return response;
            String location = response.header("Location");
            okhttp3.HttpUrl resolved = location == null ? null : response.request().url().resolve(location);
            response.close();
            if (resolved == null) throw new IOException("Redirección de descarga inválida");
            next = resolved.toString();
        }
        throw new IOException("Demasiadas redirecciones al descargar el modelo");
    }

    private static File publish(File partial, File target, BooleanSupplier cancelled) throws IOException {
        checkCancelled(cancelled);
        Files.move(partial.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static String digest(File file, BooleanSupplier cancelled) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[256 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled(cancelled);
                    digest.update(buffer, 0, count);
                }
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte item : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static void ensureSpace(File directory, long remaining) throws IOException {
        if (remaining > directory.getUsableSpace() - SPACE_MARGIN) {
            throw new IOException("Espacio insuficiente: libera espacio y reanuda la descarga");
        }
    }

    private static void validateUrl(String url) throws IOException {
        NetworkResourcePolicy.Validation result = NetworkResourcePolicy.validateModelUrl(url);
        if (!result.allowed) throw new IOException(result.reason);
    }

    private static void checkCancelled(BooleanSupplier cancelled) throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) {
            throw new InterruptedIOException("Descarga pausada; el progreso se conserva");
        }
    }

    private static String value(String value) { return value == null ? "" : value; }
    private static long number(String value) throws IOException {
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { throw new IOException("Rango de descarga inválido", e); }
    }
}
