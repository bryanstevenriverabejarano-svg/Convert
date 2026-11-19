package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.Assert.*;

public class VidaFocusEvidenceTest {
    private final LocalDate today = LocalDate.of(2026, 11, 19);

    @Test public void completedMatchingFocusIsDatedEvidence() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Vídeo Bray", Arrays.asList("Terminar escena"), today.minusDays(20));
        log = VidaCoach.setFocus(log, today.minusDays(9), "Terminar escena");
        log = VidaCoach.toggleFocus(log, today.minusDays(9));
        assertEquals(today.minusDays(9), VidaCoach.lastRecordedProjectFocus(log, log.projects.get(0), today));
        assertTrue(VidaCoach.projectInsights(log, today).get(0).contains("Última tarea principal registrada"));
    }

    @Test public void unfinishedOrUnrelatedFocusDoesNotCount() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Vídeo Bray", Arrays.asList("Terminar escena"), today.minusDays(20));
        log = VidaCoach.setFocus(log, today.minusDays(8), "Terminar escena");
        log = VidaCoach.setFocus(log, today.minusDays(7), "Hacer la compra");
        log = VidaCoach.toggleFocus(log, today.minusDays(7));
        assertNull(VidaCoach.lastRecordedProjectFocus(log, log.projects.get(0), today));
    }

    @Test public void futureCompletedFocusIsIgnored() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Vídeo Bray", Arrays.asList("Terminar escena"), today);
        log = VidaCoach.setFocus(log, today.plusDays(1), "Terminar escena");
        log = VidaCoach.toggleFocus(log, today.plusDays(1));
        assertNull(VidaCoach.lastRecordedProjectFocus(log, log.projects.get(0), today));
    }
}
