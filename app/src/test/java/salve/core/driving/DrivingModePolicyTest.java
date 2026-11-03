package salve.core.driving;

import org.junit.Test;
import static org.junit.Assert.*;

/** Neither USB charging nor ordinary Bluetooth are proof of Android Auto projection. */
public class DrivingModePolicyTest {
    @Test public void onlyProjectedAndroidAutoEnablesDrivingMode() {
        assertFalse(DrivingModePolicy.active(true, -1));
        assertFalse(DrivingModePolicy.active(true, 0));
        assertFalse(DrivingModePolicy.active(true, 1));
        assertTrue(DrivingModePolicy.active(true, 2));
        assertFalse(DrivingModePolicy.active(false, 2));
    }

    @Test public void statusNeverClaimsConnectionWhileDisabledOrUnknown() {
        assertTrue(DrivingModePolicy.status(false, 2).contains("desactivado"));
        assertTrue(DrivingModePolicy.status(true, -1).contains("Comprobando"));
        assertTrue(DrivingModePolicy.status(true, 0).contains("Sin conexión"));
        assertTrue(DrivingModePolicy.status(true, 2).contains("conectado"));
    }
}
