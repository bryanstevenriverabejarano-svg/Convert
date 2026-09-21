package salve.core;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class BuscadorDescargadorModelosTest {
    @Test public void onlyAnExplicitCommandCanSupplyApproval() {
        assertTrue(BuscadorDescargadorModelos.esSolicitudExplicita("Salve, descarga un modelo!"));
        assertFalse(BuscadorDescargadorModelos.esSolicitudExplicita("No quiero que diga descarga un modelo"));
        assertFalse(BuscadorDescargadorModelos.esSolicitudExplicita("¿Qué hace descarga un modelo?"));
    }
    @Test public void anExplicitRequestInvokesThePinnedWorkerWithoutClaimingCompletion() {
        AtomicInteger calls = new AtomicInteger();
        BuscadorDescargadorModelos.Result result = new BuscadorDescargadorModelos(calls::incrementAndGet)
                .solicitarDescarga("Descarga un modelo", true);
        assertEquals(BuscadorDescargadorModelos.Status.REQUESTED, result.status);
        assertEquals(1, calls.get()); assertTrue(result.message.contains("aún no están confirmadas"));
    }
    @Test public void unapprovedAutonomousRequestsDoNotSilentlyEnqueue() {
        AtomicInteger calls = new AtomicInteger();
        BuscadorDescargadorModelos.Result result = new BuscadorDescargadorModelos(calls::incrementAndGet)
                .solicitarDescarga("Necesito un modelo", false);
        assertEquals(BuscadorDescargadorModelos.Status.BLOCKED, result.status); assertEquals(0, calls.get());
    }
    @Test public void failureToSubmitNeverReturnsSuccess() {
        BuscadorDescargadorModelos.Result result = new BuscadorDescargadorModelos(() -> { throw new IllegalStateException(); })
                .solicitarDescarga("Descarga un modelo", true);
        assertEquals(BuscadorDescargadorModelos.Status.ERROR, result.status);
    }
}
