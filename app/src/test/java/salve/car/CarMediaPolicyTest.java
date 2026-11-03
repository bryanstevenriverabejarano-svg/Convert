package salve.car;

import static org.junit.Assert.*;
import org.junit.Test;

/** Solo Android Auto (y la propia Salve) ven el reproductor: su última frase puede ser privada. */
public class CarMediaPolicyTest {
    @Test public void androidAutoAndSalveCanConnect() {
        assertTrue(CarMediaPolicy.allowed("com.google.android.projection.gearhead", "com.salve.app"));
        assertTrue(CarMediaPolicy.allowed("com.salve.app", "com.salve.app"));
    }

    @Test public void otherAppsCannot() {
        assertFalse(CarMediaPolicy.allowed("com.spotify.music", "com.salve.app"));
        assertFalse(CarMediaPolicy.allowed("com.android.systemui", "com.salve.app"));
        assertFalse(CarMediaPolicy.allowed(null, "com.salve.app"));
    }
}
