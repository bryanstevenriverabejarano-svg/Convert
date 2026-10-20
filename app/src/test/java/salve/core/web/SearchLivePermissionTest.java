package salve.core.web;

import android.app.*;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;
import salve.presentation.ui.DeviceControlActivity;
import salve.services.MobileWorkspaceService;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class SearchLivePermissionTest {
    private Application app;
    @Before public void setup() {
        app = RuntimeEnvironment.getApplication();
        app.getSharedPreferences("salve_web_search",Context.MODE_PRIVATE).edit().clear().commit();
        SearchLive.setEnabled(app,true); ShadowSettings.setCanDrawOverlays(false);
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo(); memory.availMem = 2L * 1024 * 1024 * 1024;
        shadowOf(app.getSystemService(ActivityManager.class)).setMemoryInfo(memory);
    }
    @After public void cleanup() { SearchLive.setEnabled(app,false); }
    @Test public void opensPermissionFlowOnceWithoutGrantingItOrSendingRawSearch() {
        SearchLive.prepare(app,true);
        Intent request = shadowOf(app).getNextStartedActivity(); assertNotNull(request);
        assertEquals(DeviceControlActivity.class.getName(),request.getComponent().getClassName());
        assertTrue(request.getBooleanExtra(MobileWorkspaceService.LIVE,false));
        assertFalse(request.hasExtra(MobileWorkspaceService.QUERY));
        assertFalse(SearchLive.canShow(app)); assertFalse(SearchLive.resume(app));
        SearchLive.prepare(app,true); assertNull(shadowOf(app).getNextStartedActivity());
    }
    @Test public void disabledPanelOrBackgroundResearchNeverOpensSettings() {
        SearchLive.prepare(app,false); assertNull(shadowOf(app).getNextStartedActivity());
        SearchLive.setEnabled(app,false); SearchLive.prepare(app,true);
        assertNull(shadowOf(app).getNextStartedActivity()); assertNull(shadowOf(app).getNextStartedService());
    }
    @Test public void returnsToTheLatestPlannedQueryOnlyAfterActualPermissionGrant() {
        SearchLive.prepare(app,true); shadowOf(app).getNextStartedActivity();
        SearchLive.begin(app,"colada definición"); SearchLive.begin(app,"colada significado");
        assertFalse(SearchLive.resume(app)); assertNull(shadowOf(app).getNextStartedService());
        ShadowSettings.setCanDrawOverlays(true);
        assertTrue(SearchLive.resume(app));
        Intent opened = shadowOf(app).getNextStartedService(); assertNotNull(opened);
        assertEquals("colada significado",opened.getStringExtra(MobileWorkspaceService.QUERY));
    }
    @Test public void alreadyGrantedPermissionOpensStatusWindowBeforeAnySearch() {
        ShadowSettings.setCanDrawOverlays(true);
        assertNull(SearchLive.prepare(app,true)); assertNull(shadowOf(app).getNextStartedActivity());
        Intent opened = shadowOf(app).getNextStartedService(); assertNotNull(opened);
        assertTrue(opened.getBooleanExtra(MobileWorkspaceService.LIVE,false));
        assertFalse(opened.hasExtra(MobileWorkspaceService.QUERY));
    }
}
