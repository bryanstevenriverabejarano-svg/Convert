package salve.data.sync;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PCloudProviderTest {
    @Test
    public void acceptsOfficialPCloudApiHosts() {
        assertEquals("api.pcloud.com", PCloudCredentialStore.normalizeHost("https://api.pcloud.com/"));
        assertEquals("eapi.pcloud.com", PCloudCredentialStore.normalizeHost("eapi.pcloud.com"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsArbitraryOAuthHost() {
        PCloudCredentialStore.normalizeHost("evil.example");
    }

    @Test
    public void normalizesRemotePaths() {
        assertEquals("/Salve/events/1.json",
                PCloudProvider.normalizeRemotePath("Salve//events/1.json"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsTraversalInRemotePath() {
        PCloudProvider.normalizeRemotePath("/Salve/../secret");
    }
}
