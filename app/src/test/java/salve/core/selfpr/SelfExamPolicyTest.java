package salve.core.selfpr;

import static org.junit.Assert.*;
import org.junit.Test;

/** Se examina sola solo para comprobar un cambio suyo, y sin insistir cada ciclo de sueño. */
public class SelfExamPolicyTest {
    private static final long NOW = 100L * 60 * 60 * 1000;

    @Test public void withNothingToVerifySheDoesNotSpendTheNightOnExams() {
        assertEquals("", SelfExamPolicy.reasonNotToRun(false, true, true, 0, 0, NOW));
    }

    @Test public void sheRunsWhenAChangeIsPendingAndEverythingIsReady() {
        assertNull(SelfExamPolicy.reasonNotToRun(true, true, true, 0, 0, NOW));
        assertNull("pasadas las horas de espera, reintenta",
                SelfExamPolicy.reasonNotToRun(true, true, true, NOW - SelfExamPolicy.RETRY_AFTER_MS, 1, NOW));
        assertNull("un reloj que retrocede no la bloquea para siempre",
                SelfExamPolicy.reasonNotToRun(true, true, true, NOW + 5_000, 1, NOW));
    }

    @Test public void sheSaysWhyWhenSheCannot() {
        assertTrue(SelfExamPolicy.reasonNotToRun(true, false, true, 0, 0, NOW).contains("motor de conversación no está despierto"));
        assertTrue(SelfExamPolicy.reasonNotToRun(true, true, false, 0, 0, NOW).contains("modelo no está cargado"));
        assertTrue(SelfExamPolicy.reasonNotToRun(true, true, true, NOW - 60_000, 1, NOW).contains("Lo intenté hace poco"));
        assertTrue(SelfExamPolicy.reasonNotToRun(true, true, true, 0, SelfExamPolicy.MAX_ATTEMPTS, NOW).contains("espero a que me lo pidas tú"));
    }
}
