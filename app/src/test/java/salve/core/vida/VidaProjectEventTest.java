package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VidaProjectEventTest {
    private final LocalDate today = LocalDate.of(2026, 11, 20);

    @Test public void eventsSurviveSerializationAndCloudMerge() {
        VidaLog base = VidaCoach.addProject(new VidaLog(), "Vídeo", Arrays.asList("Escena"), today);
        String id = base.projects.get(0).id;
        VidaLog local = VidaCoach.recordProjectEvent(base, id, today, "crear", "Escena grabada");
        VidaLog remote = VidaCoach.recordProjectEvent(base, id, today.minusDays(1), "corregir", "Audio ajustado");
        VidaLog merged = VidaService.merge(local, remote);
        assertEquals(2, merged.projectEvents.size());
        assertEquals(2, VidaLog.parse(merged.serialize()).projectEvents.size());
        assertEquals(2, VidaService.merge(merged, merged).projectEvents.size());
    }

    @Test public void historySeparatesProjectsAndOrdersDates() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Vídeo", Arrays.asList("Escena"), today);
        log = VidaCoach.addProject(log, "Curso", Arrays.asList("Tema"), today);
        String id = log.projects.get(0).id;
        log = VidaCoach.recordProjectEvent(log, id, today, "completar", "Escena uno");
        log = VidaCoach.recordProjectEvent(log, id, today.minusDays(2), "aplazar", "Revisar mañana");
        log = VidaCoach.recordProjectEvent(log, log.projects.get(1).id, today, "crear", "Notas");
        assertEquals(2, VidaCoach.projectHistory(log, id).size());
        assertEquals(today.minusDays(2).toString(), VidaCoach.projectHistory(log, id).get(0).date);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownProjectIsRejected() {
        VidaCoach.recordProjectEvent(new VidaLog(), "missing", today, "crear", "Prueba");
    }
}
