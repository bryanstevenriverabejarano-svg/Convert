package salve.data.salvente;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.function.LongSupplier;
import salve.core.salvente.SalventeArchive;
import salve.core.salvente.SealedBox;

/**
 * Copia de los Salventes en el Guardián ciego. Todo se sella en el móvil con la frase de Bryan antes
 * de salir; el Guardián (un PC con SalveNodo) solo ve bytes opacos. La frase no se guarda: se usa y
 * se borra de memoria. Antes de subir, la copia se vuelve a abrir aquí para no guardar algo ilegible.
 */
public final class SalventeGuardianBackup {
    public static final String BLOB = "salventes";

    /** El Guardián remoto, reducido a lo imprescindible. */
    public interface Remote {
        void put(String name, byte[] sealed) throws Exception;
        /** null si no hay copia. */
        byte[] get(String name) throws Exception;
    }

    public static final class Summary {
        public final int salventes, memories, skipped; public final long bytes;
        Summary(int salventes, int memories, int skipped, long bytes) {
            this.salventes = salventes; this.memories = memories; this.skipped = skipped; this.bytes = bytes;
        }
    }

    private final SalventeRepository repository;
    private final Remote remote;
    private final LongSupplier clock;

    public SalventeGuardianBackup(SalventeRepository repository, Remote remote, LongSupplier clock) {
        this.repository = repository; this.remote = remote; this.clock = clock;
    }

    public Summary backup(char[] passphrase) throws Exception {
        try {
            SealedBox.checkPassphrase(passphrase);
            List<SalventeArchive.Entry> entries = repository.exportAll();
            if (entries.isEmpty()) throw new IllegalStateException("Aún no hay Salventes que copiar.");
            int memories = 0; for (SalventeArchive.Entry e : entries) memories += e.memories.size();
            byte[] plain = SalventeArchive.toJson(entries, clock.getAsLong()).getBytes(StandardCharsets.UTF_8);
            byte[] sealed;
            try {
                sealed = SealedBox.seal(plain, passphrase.clone(), new SecureRandom());
                if (!Arrays.equals(plain, SealedBox.open(sealed, passphrase.clone())))
                    throw new IllegalStateException("La copia no se pudo comprobar; no se ha subido.");
            } finally { Arrays.fill(plain, (byte) 0); }
            remote.put(BLOB, sealed);
            return new Summary(entries.size(), memories, 0, sealed.length);
        } finally { Arrays.fill(passphrase, '\0'); }
    }

    public Summary restore(char[] passphrase) throws Exception {
        try {
            byte[] sealed = remote.get(BLOB);
            if (sealed == null) throw new IllegalStateException("El Guardián todavía no tiene ninguna copia.");
            byte[] plain = SealedBox.open(sealed, passphrase.clone());
            List<SalventeArchive.Entry> entries;
            try { entries = SalventeArchive.fromJson(new String(plain, StandardCharsets.UTF_8)); }
            finally { Arrays.fill(plain, (byte) 0); }
            int restored = 0, skipped = 0, memories = 0;
            for (SalventeArchive.Entry e : entries) {
                if (repository.restore(e)) { restored++; memories += e.memories.size(); } else skipped++;
            }
            return new Summary(restored, memories, skipped, sealed.length);
        } finally { Arrays.fill(passphrase, '\0'); }
    }
}
