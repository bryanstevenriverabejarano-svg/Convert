package salve.services;
import android.app.Application;
import android.content.Intent;
import android.os.*;
import android.webkit.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowSettings;
import salve.core.devicecontrol.BrowserRecovery;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class MobileWorkspaceServiceTest {
    private ServiceController<MobileWorkspaceService> controller;
    private MobileWorkspaceService service;
    private boolean destroyed;
    @Before public void setup() {
        ShadowSettings.setCanDrawOverlays(true);
        controller = Robolectric.buildService(MobileWorkspaceService.class).create(); service = controller.get();
        service.onStartCommand(new Intent(), 0, 1);
    }
    @After public void cleanup() { if (!destroyed) controller.destroy(); }
    private Object field(String name) throws Exception {
        java.lang.reflect.Field field = MobileWorkspaceService.class.getDeclaredField(name); field.setAccessible(true); return field.get(service);
    }
    @Test public void browserHasNoLocalFileAccessAndClosingStopsRecovery() throws Exception {
        WebView web = (WebView)field("web"); assertNotNull(web);
        assertTrue(web.getSettings().getJavaScriptEnabled());
        assertFalse(web.getSettings().getAllowFileAccess()); assertFalse(web.getSettings().getAllowContentAccess());
        BrowserRecovery recovery = (BrowserRecovery)field("recovery"); recovery.begin("https://www.google.com/", false, 100);
        controller.destroy(); destroyed = true;
        assertEquals(BrowserRecovery.State.PAUSED, recovery.state()); assertNull(field("web"));
    }
    @Test public void webFormsNeverResubmitAutomatically() throws Exception {
        WebView web = (WebView)field("web"); WebViewClient client = shadowOf(web).getWebViewClient();
        boolean[] called = new boolean[2]; Handler handler = new Handler(Looper.getMainLooper());
        client.onFormResubmission(web, Message.obtain(handler, () -> called[0] = true), Message.obtain(handler, () -> called[1] = true));
        shadowOf(Looper.getMainLooper()).idle(); assertTrue(called[0]); assertFalse(called[1]);
        assertEquals(BrowserRecovery.State.PAUSED, ((BrowserRecovery)field("recovery")).state());
    }
    @Test public void rendererDeathRemovesWebViewWithoutRecoveryLoop() throws Exception {
        WebView web = (WebView)field("web"); assertTrue(shadowOf(web).getWebViewClient().onRenderProcessGone(web, null));
        assertNull(field("web")); assertEquals(BrowserRecovery.State.PAUSED, ((BrowserRecovery)field("recovery")).state());
    }
}
