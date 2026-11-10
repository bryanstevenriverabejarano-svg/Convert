package salve.services;

import static org.junit.Assert.*;
import org.junit.Test;

/** El informe de un cierre por señal dice lo medido y marca como probable lo que deduce. */
public class CrashReportTest {
    @Test public void aKillWhileHoldingAThirdOfTheRamPointsAtMemory() {
        String text = CrashReport.signalDetail(9, 100, 4166, 11264, true);
        assertTrue(text, text.startsWith("Señal 9 (SIGKILL: la mató el sistema, no falló su código); Salve estaba en pantalla."));
        assertTrue(text, text.contains("Ocupaba 4166 MB de 11264 MB del teléfono (37 %)."));
        assertTrue(text, text.contains("Probable: Android la cerró para recuperar memoria"));
        assertTrue(text, text.contains("cuidado del dispositivo de Samsung"));
        assertTrue(CrashReport.signalDetail(9, 100, 4166, 11264, false).contains("no distingue ese caso"));
    }

    @Test public void aSmallProcessKilledInBackgroundIsNotBlamedOnMemory() {
        String text = CrashReport.signalDetail(9, 400, 300, 11264, true);
        assertTrue(text, text.contains("estaba en segundo plano"));
        assertTrue(text, text.contains("no parece falta de memoria"));
        assertTrue(CrashReport.signalDetail(9, 125, 300, 11264, true).contains("servicio en primer plano"));
    }

    @Test public void otherSignalsAreNamedWithoutGuessingACause() {
        assertTrue(CrashReport.signalDetail(6, 100, 4000, 11264, true).contains("SIGABRT: una librería nativa abortó"));
        assertTrue(CrashReport.signalDetail(11, 100, 4000, 11264, true).contains("SIGSEGV"));
        assertFalse(CrashReport.signalDetail(11, 100, 4000, 11264, true).contains("Probable"));
        assertTrue(CrashReport.signalDetail(31, 100, 10, 0, true).contains("señal poco habitual"));
        assertFalse("sin total no se inventa un porcentaje", CrashReport.signalDetail(31, 100, 10, 0, true).contains("%"));
    }
}
