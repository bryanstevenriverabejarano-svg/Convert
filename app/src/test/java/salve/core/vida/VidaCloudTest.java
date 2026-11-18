package salve.core.vida;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class VidaCloudTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneId.of("Europe/Madrid"));

    /** Nube falsa: guarda la última copia y puede simular estar caída. */
    static final class FakeCloud implements VidaService.Cloud {
        String remote; boolean down; final List<String> pushes = new ArrayList<>();
        @Override public String fetch() throws IOException { if (down) throw new IOException("sin red"); return remote; }
        @Override public void push(String content) { pushes.add(content); remote = content; }
    }

    private File file() throws IOException { return new File(Files.createTempDirectory("vida").toFile(), "vida/vida.tsv"); }

    @Test public void uploadsEveryChangeAfterChecking() throws Exception {
        FakeCloud cloud = new FakeCloud();
        VidaService service = new VidaService(file(), clock, cloud);
        service.respond("curl predicador, 15 kilos, 8 reps");
        assertEquals(1, cloud.pushes.size());
        assertEquals(1, VidaLog.parse(cloud.remote).sets.size());
    }

    @Test public void newPhoneRestoresBeforeWriting() throws Exception {
        FakeCloud cloud = new FakeCloud();
        new VidaService(file(), clock, cloud).respond("sentadilla, 60 kg, 8 reps");
        // Móvil nuevo: archivo local vacío; la primera anotación no pisa la copia remota.
        VidaService fresh = new VidaService(file(), clock, cloud);
        fresh.respond("press banca, 50 kg, 10 reps");
        VidaLog remote = VidaLog.parse(cloud.remote);
        assertEquals(2, remote.sets.size());
        assertTrue(fresh.handles("historial de sentadilla"));
    }

    @Test public void offlineNeverOverwritesCloud() throws Exception {
        FakeCloud cloud = new FakeCloud();
        new VidaService(file(), clock, cloud).respond("sentadilla, 60 kg, 8 reps");
        int before = cloud.pushes.size();
        cloud.down = true;
        File local = file();
        VidaService offline = new VidaService(local, clock, cloud);
        offline.respond("press banca, 50 kg, 10 reps");
        assertEquals("sin comprobar no se sube", before, cloud.pushes.size());
        cloud.down = false;
        offline.respond("he comido arroz");
        assertEquals(3, VidaLog.parse(cloud.remote).sets.size() + VidaLog.parse(cloud.remote).meals.size());
    }

    @Test public void recoverCommandAndDeletionTombstone() throws Exception {
        FakeCloud cloud = new FakeCloud();
        VidaService service = new VidaService(file(), clock, cloud);
        service.respond("sentadilla, 60 kg, 8 reps");
        assertTrue(service.handles("recupera mis datos de vida"));
        assertTrue(service.respond("Recupera mis datos de Vida").contains("1 series"));
        assertTrue(service.respond("borra mis datos de vida").contains("nube"));
        assertTrue(VidaLog.parse(cloud.remote).isEmpty());
    }
}
