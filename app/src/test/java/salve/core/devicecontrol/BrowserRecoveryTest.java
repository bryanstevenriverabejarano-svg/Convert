package salve.core.devicecontrol;
import org.junit.Test;
import static org.junit.Assert.*;

public class BrowserRecoveryTest {
    private static final String URL = "https://www.google.com/search?q=colada";
    @Test public void offlineSearchResumesButRetriesAreBounded() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, false, 100);
        assertEquals(BrowserRecovery.State.WAITING_NETWORK, r.state());
        assertFalse(r.retry(false, 1000)); assertTrue(r.retry(true, 2000));
        r.failed(true); assertTrue(r.retry(true, 5000));
        r.failed(true); assertFalse(r.retry(true, 8000));
        assertEquals(BrowserRecovery.State.FAILED, r.state()); assertEquals(2, r.retries());
    }
    @Test public void offlineSearchExpiresInsteadOfRunningMuchLater() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, false, 100);
        assertFalse(r.retry(true, 120100)); assertEquals(BrowserRecovery.State.FAILED, r.state());
    }
    @Test public void touchingPagePreventsAutomaticReplay() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
        r.userInteraction(); r.failed(true); assertFalse(r.retry(true, 1000));
        assertEquals(BrowserRecovery.State.FAILED, r.state());
    }
    @Test public void postAndRedirectCannotBecomeRetryableGet() {
        for (String method : new String[]{"POST", "GET"}) {
            BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
            r.navigation(method, method.equals("POST") ? URL : "https://example.org/next");
            r.navigation("GET", URL); r.failed(true); assertFalse(r.retry(true, 1000));
        }
    }
    @Test public void pauseSurvivesNetworkRecoveryAndLatePageCallbacks() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
        r.pause(); r.navigation("GET", URL); r.failed(true); r.ready();
        assertFalse(r.retry(true, 200)); assertEquals(BrowserRecovery.State.PAUSED, r.state());
    }
    @Test public void httpTlsFailuresAreNotHiddenByPageFinished() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
        r.failed(false); r.ready(); assertEquals(BrowserRecovery.State.FAILED, r.state());
        assertFalse(r.retry(true, 200));
    }
    @Test public void timeoutAndFreshSearchResetOldState() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
        assertFalse(r.timedOut(25099)); assertTrue(r.timedOut(25100));
        r.failed(true); assertTrue(r.retry(true, 26000)); assertFalse(r.timedOut(26001));
        r.pause(); r.begin("https://www.bing.com/search?q=colada", true, 30000);
        assertEquals(0, r.retries()); r.failed(true); assertTrue(r.retry(true, 34000));
    }
    @Test public void clockRollbackAndSessionExpiryStopAWaitingAttempt() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, false, 100);
        assertFalse(r.retry(true, 99)); assertEquals(BrowserRecovery.State.FAILED, r.state());
        r.begin(URL, false, 100); assertTrue(r.retry(true, 120000));
        assertTrue(r.timedOut(120101));
    }
    @Test public void manualNavigationAfterFailureDoesNotRestartAutomaticRecovery() {
        BrowserRecovery r = new BrowserRecovery(); r.begin(URL, true, 100);
        r.failed(false); r.userInteraction(); r.navigation("GET", "https://example.org/next");
        assertEquals(BrowserRecovery.State.IDLE, r.state()); assertFalse(r.retry(true, 1000));
    }
}
