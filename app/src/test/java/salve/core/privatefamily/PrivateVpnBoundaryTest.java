package salve.core.privatefamily;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.io.IOException;
import java.util.concurrent.CancellationException;
import salve.core.compute.ComputeHttpTransport;
import salve.core.compute.ComputeNode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class PrivateVpnBoundaryTest {
    @Test public void disconnectedTunnelCannotCreateARequestLease() {
        PrivateVpn vpn = PrivateVpn.get(RuntimeEnvironment.getApplication());
        assertFalse(vpn.available());
        assertThrows(IOException.class, () -> vpn.lease("https://10.77.0.2:8766"));
        assertThrows(IOException.class, () -> vpn.lease("https://public.example"));
        assertFalse(vpn.isUp());
    }
    @Test public void familyAndLocationRequestsFailBeforeConnectingWithoutVpn() {
        ComputeHttpTransport transport = new ComputeHttpTransport(RuntimeEnvironment.getApplication());
        ComputeNode node = new ComputeNode("test", "Propio", "https://10.77.0.2:8766", "r".repeat(43));
        assertThrows(IOException.class, () -> transport.familyStatus(node, () -> false));
        assertThrows(IOException.class, () -> transport.shareLocation(node, "w".repeat(43), null, () -> false));
        assertThrows(CancellationException.class, () -> transport.familyStatus(node, () -> true));
    }
}
