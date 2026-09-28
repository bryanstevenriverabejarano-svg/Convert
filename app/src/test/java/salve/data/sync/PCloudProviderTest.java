package salve.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PCloudProviderTest {
    @Test public void listsNamesOrPathsWithoutEscapingRequestedFolder() {
        com.google.gson.JsonArray entries = com.google.gson.JsonParser.parseString(
                "[{\"name\":\"one.json\",\"isfolder\":false},{\"path\":\"/Salve/events/two.json\"},"
                        + "{\"name\":\"folder\",\"isfolder\":true},{\"name\":\"../secret\"},"
                        + "{\"path\":\"/another/file.json\"}]").getAsJsonArray();
        assertEquals(java.util.Arrays.asList("/Salve/events/one.json", "/Salve/events/two.json"),
                PCloudProvider.filePaths("/Salve/events", entries));
    }
    @Test public void binaryDownloadsUseAnOfficialSignedContentUrl() {
        com.google.gson.JsonObject link = com.google.gson.JsonParser.parseString(
                "{\"hosts\":[\"c63.pcloud.com\"],\"path\":\"/signed/photo.jpg\"}").getAsJsonObject();
        assertEquals("https://c63.pcloud.com/signed/photo.jpg", PCloudProvider.downloadUrl(link).toString());
        link.getAsJsonArray("hosts").set(0, new com.google.gson.JsonPrimitive("pcloud.com.evil.example"));
        org.junit.Assert.assertNull(PCloudProvider.downloadUrl(link));
        link.getAsJsonArray("hosts").set(0, new com.google.gson.JsonPrimitive("c63.pcloud.com"));
        link.addProperty("path", "//evil.example/photo.jpg");
        org.junit.Assert.assertNull(PCloudProvider.downloadUrl(link));
    }
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

    @Test
    public void parsesEventTimestampFromRemoteName() {
        assertEquals(1720000000123L,
                CloudSyncManager.timestampFromEventPath("/Salve/events/1720000000123-42.json"));
        assertTrue(CloudSyncManager.timestampFromEventPath("/Salve/events/bad.json") == 0L);
    }
}
