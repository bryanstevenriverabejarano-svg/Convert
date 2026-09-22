package salve.data.sync;

import org.junit.Test;
import static org.junit.Assert.*;

public class CloudUploadPolicyTest {
    private static final String FIXTURE = "synthetic-test-token-never-used-on-network";

    @Test public void missingAndExampleTokensCannotSendPrivateData() {
        for (String token : new String[]{null, "", "short", "pon_aqui_tu_clave_larga",
                "pon_aqui_tu_clave_larga_01234567890123456789", FIXTURE + "\n"}) {
            assertFalse(CloudUploadPolicy.isConfigured("https://example.com/sync", token));
        }
    }

    @Test public void malformedAndInsecureEndpointsCannotSendCredentials() {
        for (String endpoint : new String[]{null, "", "http://example.com/sync", "https:///sync",
                "https://user:password@example.com/sync", "https://example.com/sync#fragment"}) {
            assertFalse(CloudUploadPolicy.isConfigured(endpoint, FIXTURE));
        }
    }

    @Test public void validShapeIsOnlyAConfigurationPrecheck() {
        assertTrue(CloudUploadPolicy.isConfigured("https://example.com/sync", FIXTURE));
    }
}
