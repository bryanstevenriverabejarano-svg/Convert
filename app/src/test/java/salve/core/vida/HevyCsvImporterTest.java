package salve.core.vida;

import java.io.File;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;
import static org.junit.Assert.*;

public class HevyCsvImporterTest {
    static final String CSV = "﻿\"title\",\"start_time\",\"end_time\",\"description\",\"exercise_title\",\"superset_id\",\"exercise_notes\",\"set_index\",\"set_type\",\"weight_kg\",\"reps\",\"distance_km\",\"duration_seconds\",\"rpe\"\n"
            + "\"Brazos\",\"27 Aug 2026, 18:05\",\"27 Aug 2026, 19:10\",\"\",\"Elíptica\",,\"\",0,\"normal\",,,0.18,361,\n"
            + "\"Brazos\",\"27 Aug 2026, 18:05\",\"27 Aug 2026, 19:10\",\"\",\"Curl predicador (máquina)\",,\"nota, con coma\",0,\"warmup\",15,8,,,\n"
            + "\"Brazos\",\"27 Aug 2026, 18:05\",\"27 Aug 2026, 19:10\",\"\",\"Curl predicador (máquina)\",,\"\",1,\"normal\",17.5,7,,,\n"
            + "\"Brazos\",\"27 Aug 2026, 18:05\",\"27 Aug 2026, 19:10\",\"\",\"Curl de Bíceps (Máquina)\",,\"\",2,\"failure\",30,5,,,\n"
            + "\"Piernas\",\"2026-09-01 18:00:00\",\"\",\"\",\"Sentadilla\",,\"\",0,\"normal\",60,8,,,\n"
            + "\"Mala\",\"ayer\",\"\",\"\",\"Sentadilla\",,\"\",0,\"normal\",60,8,,,\n";

    @Test public void parsesHevyColumnsByName() {
        HevyCsvImporter.Result r = HevyCsvImporter.parse(CSV);
        assertEquals(5, r.sets.size());
        assertEquals(1, r.skipped);
        VidaLog.GymSet cardio = r.sets.get(0);
        assertEquals("2026-08-27", cardio.date);
        assertEquals(0.18, cardio.km, 1e-9);
        assertEquals(Integer.valueOf(361), cardio.seconds);
        assertNull(cardio.kg);
        assertEquals("calentamiento", r.sets.get(1).type);
        assertEquals("fallo", r.sets.get(3).type);
        assertEquals("2026-09-01", r.sets.get(4).date);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOtherCsv() { HevyCsvImporter.parse("fecha,importe\n2026-01-01,10\n"); }

    @Test public void serviceImportsOnceAndAnswersHistory() throws Exception {
        File dir = Files.createTempDirectory("vida").toFile();
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneId.of("Europe/Madrid"));
        VidaService service = new VidaService(new File(dir, "vida/vida.tsv"), clock);
        assertTrue(service.importHevy(CSV).contains("5 series"));
        assertTrue(service.importHevy(CSV).contains("no hay series nuevas"));
        assertTrue(service.handles("historial de curl predicador"));
        String h = service.respond("historial de curl predicador");
        // El calentamiento no cuenta como mejor serie.
        assertTrue(h, h.contains("17,5 kg × 7"));
        // Un servicio nuevo relee lo guardado.
        VidaService again = new VidaService(new File(dir, "vida/vida.tsv"), clock);
        assertTrue(again.handles("historial de sentadilla"));
    }
}
