package salve.data.continuity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import salve.core.compute.ComputeHttpTransport;
import salve.core.compute.ComputeNode;
import salve.core.continuity.MemorySnapshot;
import salve.core.salvente.SealedBox;
import salve.data.db.MemoriaDatabase;

/**
 * Continuidad de Salve: su memoria entera (recuerdos y mente) sellada en el Guardián ciego.
 *
 * Copiar: instantánea coherente de memoria.db con VACUUM INTO + archivos de la mente → zip
 * validado → sellado con la frase de Bryan → se comprueba que abre → subida con SHA-256 verificado.
 *
 * Restaurar nunca sustituye nada en caliente: descarga, abre y valida, deja todo preparado en una
 * carpeta y se aplica en el siguiente arranque, antes de abrir la base de datos. La memoria que
 * hubiera en este teléfono se conserva aparte (continuity/previous) por si hay que volver atrás.
 */
public final class SalveContinuity {
    private static final String TAG = "Salve/Continuity";
    public static final String BLOB = "salve-memoria";
    /** Versiones de memoria.db que esta app abre (con MemoryMigrations 4→8). */
    public static final int MIN_SCHEMA = 4, SCHEMA = 8;
    static final String PREFS = "salve_continuity", READY = "READY";

    private final Context app;

    public SalveContinuity(Context context) { app = context.getApplicationContext(); }

    // -------------------------------------------------------------- recoger

    public static boolean supported() { return Build.VERSION.SDK_INT >= 30; }   // VACUUM INTO (SQLite 3.27+)

    /** Todo lo que forma su memoria, en el formato de {@link MemorySnapshot}. */
    public Map<String, byte[]> collect() throws Exception {
        if (!supported()) throw new IllegalStateException("La copia de la memoria necesita Android 11 o posterior.");
        return collectUnchecked();
    }

    /** Sin la comprobación de versión: el SQLite de las pruebas ya admite VACUUM INTO. */
    Map<String, byte[]> collectUnchecked() throws Exception {
        Map<String, byte[]> out = new LinkedHashMap<>();
        out.put(MemorySnapshot.DATABASE, snapshotDatabase());
        addJson(out, "nobackup/mind", new File(app.getNoBackupFilesDir(), "mind"));
        addJson(out, "nobackup/autonomy", new File(app.getNoBackupFilesDir(), "autonomy"));
        addJson(out, "files/sketch", new File(app.getFilesDir(), "sketch"));
        return out;
    }

    private byte[] snapshotDatabase() throws IOException {
        File dir = new File(app.getNoBackupFilesDir(), "continuity");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("Sin carpeta de trabajo");
        File snap = new File(dir, "instantanea.db");
        if (snap.exists() && !snap.delete()) throw new IOException("No se pudo preparar la instantánea");
        try {
            SupportSQLiteDatabase db = MemoriaDatabase.getInstance(app).getOpenHelper().getWritableDatabase();
            // Copia transaccional y coherente, incluidos los cambios aún en el WAL; no bloquea a Salve.
            db.execSQL("VACUUM INTO '" + snap.getAbsolutePath().replace("'", "''") + "'");
            return Files.readAllBytes(snap.toPath());
        } finally {
            if (snap.exists() && !snap.delete()) Log.w(TAG, "No se borró la instantánea temporal");
        }
    }

    private static void addJson(Map<String, byte[]> out, String prefix, File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        Arrays.sort(files);
        for (File f : files) {
            String name = prefix + "/" + f.getName();
            if (!f.isFile() || !MemorySnapshot.allowed(name) || f.length() > 16L * 1024 * 1024) continue;
            out.put(name, Files.readAllBytes(f.toPath()));
        }
    }

    // ---------------------------------------------------------------- copiar

    public static final class Result {
        public final long bytes; public final int files; public final long at;
        Result(long bytes, int files, long at) { this.bytes = bytes; this.files = files; this.at = at; }
    }

    /**
     * Copia pedida por Bryan: espera (hasta 90 s) a que termine el ciclo interno que esté en curso,
     * para no fotografiar la memoria a mitad de un sueño.
     */
    public Result backup(ComputeNode guardian, char[] passphrase, java.util.function.BooleanSupplier stopped) throws Exception {
        salve.core.mind.BackgroundMindGate.Lease lease = null;
        long deadline = System.currentTimeMillis() + 90_000;
        try {
            while ((lease = salve.core.mind.BackgroundMindGate.get().acquire(salve.core.mind.BackgroundMindGate.Task.CONTINUITY,
                    false, false, false)) == null) {
                if (stopped.getAsBoolean() || System.currentTimeMillis() > deadline)
                    throw new IllegalStateException("Estoy en mitad de un ciclo interno (soñando o reflexionando); inténtalo en un minuto.");
                Thread.sleep(1000);
            }
            Result result = backupHolding(guardian, passphrase, stopped);
            lease.complete();
            return result;
        } finally {
            Arrays.fill(passphrase, '\0');
            if (lease != null) lease.close();
        }
    }

    /** Copia nocturna: solo si la puerta está libre y no hay conversación; si no, null (se reintenta). */
    Result backupInBackground(ComputeNode guardian, char[] passphrase, java.util.function.BooleanSupplier stopped,
                              boolean conversing, boolean mindPaused, boolean modelLoading) throws Exception {
        try (salve.core.mind.BackgroundMindGate.Lease lease = salve.core.mind.BackgroundMindGate.get().acquire(
                salve.core.mind.BackgroundMindGate.Task.CONTINUITY, conversing, mindPaused, modelLoading)) {
            if (lease == null) { Arrays.fill(passphrase, '\0'); return null; }
            Result result = backupHolding(guardian, passphrase, stopped);
            lease.complete();
            return result;
        }
    }

    private Result backupHolding(ComputeNode guardian, char[] passphrase, java.util.function.BooleanSupplier stopped) throws Exception {
        try {
            SealedBox.checkPassphrase(passphrase);
            Map<String, byte[]> entries = collect();
            long now = System.currentTimeMillis();
            byte[] zip = MemorySnapshot.pack(entries, now, SCHEMA);
            byte[] sealed = SealedBox.seal(zip, passphrase.clone(), new SecureRandom());
            // Antes de subir: la copia debe abrirse y validar igual que en una restauración.
            MemorySnapshot.unpack(SealedBox.open(sealed, passphrase.clone()), MIN_SCHEMA, SCHEMA);
            new ComputeHttpTransport(app).vaultPut(guardian, BLOB, sealed, stopped);
            prefs().edit().putLong("last_at", now).putLong("last_bytes", sealed.length).putString("node", guardian.id)
                    .putString("node_name", guardian.name).remove("last_error").apply();
            return new Result(sealed.length, entries.size(), now);
        } catch (Exception failure) {
            prefs().edit().putString("last_error", failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()).apply();
            throw failure;
        } finally { Arrays.fill(passphrase, '\0'); }
    }

    // ------------------------------------------------------------- restaurar

    /** Descarga, abre y valida; deja la restauración preparada para el próximo arranque. */
    public MemorySnapshot.Contents stageRestore(ComputeNode guardian, char[] passphrase, java.util.function.BooleanSupplier stopped) throws Exception {
        try {
            byte[] sealed = new ComputeHttpTransport(app).vaultGet(guardian, BLOB, stopped);
            if (sealed == null) throw new IllegalStateException("El Guardián aún no tiene ninguna copia de la memoria de Salve.");
            MemorySnapshot.Contents contents = MemorySnapshot.unpack(SealedBox.open(sealed, passphrase.clone()), MIN_SCHEMA, SCHEMA);
            stage(app, contents);
            return contents;
        } finally { Arrays.fill(passphrase, '\0'); }
    }

    static void stage(Context app, MemorySnapshot.Contents contents) throws IOException {
        File pending = new File(app.getNoBackupFilesDir(), "continuity/pending");
        deleteTree(pending);
        if (!pending.mkdirs()) throw new IOException("No se pudo preparar la restauración");
        StringBuilder names = new StringBuilder();
        for (Map.Entry<String, byte[]> e : contents.entries.entrySet()) {
            File target = new File(pending, e.getKey());
            File parent = target.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("No se pudo preparar la restauración");
            Files.write(target.toPath(), e.getValue());
            names.append(e.getKey()).append('\n');
        }
        File marker = new File(pending, READY + ".tmp");
        Files.write(marker.toPath(), names.toString().getBytes(StandardCharsets.UTF_8));
        if (!marker.renameTo(new File(pending, READY))) throw new IOException("No se pudo marcar la restauración");
    }

    public static boolean pending(Context context) {
        return new File(context.getNoBackupFilesDir(), "continuity/pending/" + READY).isFile();
    }

    /**
     * Se llama al arrancar, ANTES de abrir memoria.db. Mueve la memoria actual a continuity/previous
     * y coloca la restaurada. Si algo falla a mitad, deshace lo movido.
     */
    public static void applyPending(Context context) {
        File base = context.getNoBackupFilesDir();
        File pending = new File(base, "continuity/pending"), marker = new File(pending, READY);
        if (!marker.isFile()) return;
        File previous = new File(base, "continuity/previous");
        java.util.List<File[]> moved = new java.util.ArrayList<>();   // {desde, hasta} para deshacer
        try {
            String[] names = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).split("\n");
            deleteTree(previous);
            if (!previous.mkdirs()) throw new IOException("Sin carpeta para la memoria anterior");
            File db = context.getDatabasePath("memoria.db");
            for (String suffix : new String[]{"", "-wal", "-shm", "-journal"})
                move(new File(db.getPath() + suffix), new File(previous, "db/memoria.db" + suffix), moved);
            // Los vectores de recuerdos corresponden a la memoria anterior: se recalculan.
            File[] vectors = new File(base, "mind").listFiles((d, n) -> n.startsWith("memory_vectors_"));
            if (vectors != null) for (File v : vectors) move(v, new File(previous, "nobackup/mind/" + v.getName()), moved);
            for (String name : names) {
                if (!MemorySnapshot.allowed(name)) continue;
                File target = target(context, name);
                if (target.exists()) move(target, new File(previous, name), moved);
                move(new File(pending, name), target, moved);
            }
            if (!marker.delete()) throw new IOException("No se pudo cerrar la restauración");
            deleteTree(pending);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong("restored_at", System.currentTimeMillis()).apply();
            Log.i(TAG, "Memoria restaurada; la anterior queda en continuity/previous");
        } catch (Exception failure) {
            Log.e(TAG, "La restauración falló; se deshace", failure);
            for (int i = moved.size() - 1; i >= 0; i--) {
                File[] m = moved.get(i);
                if (m[1].exists() && !m[1].renameTo(m[0])) Log.e(TAG, "No se pudo deshacer " + m[0].getName());
            }
            if (!marker.delete()) Log.w(TAG, "Marcador de restauración no borrado");
        }
    }

    static File target(Context context, String name) {
        if (MemorySnapshot.DATABASE.equals(name)) return context.getDatabasePath("memoria.db");
        if (name.startsWith("nobackup/")) return new File(context.getNoBackupFilesDir(), name.substring("nobackup/".length()));
        return new File(context.getFilesDir(), name.substring("files/".length()));
    }

    private static void move(File from, File to, java.util.List<File[]> moved) throws IOException {
        if (!from.exists()) return;
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("Sin carpeta destino");
        if (!from.renameTo(to)) throw new IOException("No se pudo mover " + from.getName());
        moved.add(new File[]{from, to});
    }

    static void deleteTree(File f) {
        if (!f.exists()) return;
        File[] children = f.listFiles();
        if (children != null) for (File c : children) deleteTree(c);
        if (!f.delete()) Log.w(TAG, "No se pudo borrar " + f.getName());
    }

    // ------------------------------------------------------ copia automática

    /** Activa la copia nocturna con este Guardián, guardando la frase cifrada con Keystore. */
    public void enableAuto(ComputeNode guardian, char[] passphrase) throws Exception {
        try {
            SealedBox.checkPassphrase(passphrase);
            new ContinuitySecret(app).store(passphrase);
            prefs().edit().putBoolean("auto", true).putString("node", guardian.id).putString("node_name", guardian.name).apply();
            ContinuityWorker.schedule(app);
        } finally { Arrays.fill(passphrase, '\0'); }
    }

    public void disableAuto() {
        new ContinuitySecret(app).forget();
        prefs().edit().putBoolean("auto", false).apply();
        ContinuityWorker.cancel(app);
    }

    // ---------------------------------------------------------------- estado

    public SharedPreferences prefs() { return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    /** Para Salve y la pantalla: cuándo fue la última copia y dónde. */
    public String describe() {
        SharedPreferences p = prefs();
        long last = p.getLong("last_at", 0);
        StringBuilder out = new StringBuilder(last == 0 ? "nunca se ha copiado mi memoria en un Guardián"
                : "última copia de mi memoria: " + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT, new java.util.Locale("es", "ES")).format(new java.util.Date(last))
                + " en " + p.getString("node_name", "el Guardián") + " (" + Math.max(1, p.getLong("last_bytes", 0) / 1024) + " KB sellados)");
        out.append(p.getBoolean("auto", false) ? "; copia automática cada noche activada" : "; copia automática desactivada");
        String error = p.getString("last_error", null);
        if (error != null) out.append("; el último intento falló: ").append(error);
        return out.toString();
    }
}
