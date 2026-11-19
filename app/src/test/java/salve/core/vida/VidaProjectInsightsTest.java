package salve.core.vida;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class VidaProjectInsightsTest {
    private final LocalDate day = LocalDate.of(2026, 11, 19);

    @Test public void nearFinishedProjectGetsFinishModeAdvice() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Vídeo Bray",
                Arrays.asList("Uno", "Dos", "Tres", "Cuatro", "Publicar"), day.minusDays(20));
        String id = log.projects.get(0).id;
        for (int i = 0; i < 4; i++) log = VidaCoach.toggleStep(log, id, i);
        assertTrue(VidaCoach.projectInsights(log, day).get(0).contains("Modo Terminar"));
        assertTrue(VidaCoach.weekReport(log, day).contains("Modo Terminar"));
    }

    @Test public void olderIncompleteProjectIsReviewedWithoutClaimingInactivity() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Curso", Arrays.asList("Tema uno", "Tema dos"), day.minusDays(20));
        assertTrue(VidaCoach.projectInsights(log, day).get(0).contains("lleva tiempo abierto"));
    }

    @Test public void twoActiveProjectsTriggerPauseReminder() {
        VidaLog log = VidaCoach.addProject(new VidaLog(), "Uno", Arrays.asList("Paso"), day);
        log = VidaCoach.addProject(log, "Dos", Arrays.asList("Paso"), day);
        List<String> insights = VidaCoach.projectInsights(log, day);
        assertTrue(insights.get(insights.size() - 1).contains("pausa uno"));
    }
}
