package salve.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkResourcePolicyTest {
    @Test
    public void acceptsAuthorizedHttpsKnowledgeSource() {
        assertTrue(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://es.wikipedia.org/w/api.php?action=query").allowed);
        assertTrue(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://example.org/documentacion").allowed);
    }

    @Test
    public void rejectsHttpPrivateAndLookalikeSources() {
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "http://es.wikipedia.org/wiki/IA").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://127.0.0.1/secreto").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://router.local/secreto").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://[::1]/secreto").allowed);
    }

    @Test
    public void separatesModelAndKnowledgeAllowLists() {
        assertTrue(NetworkResourcePolicy.validateModelUrl(
                "https://huggingface.co/model/file.bin").allowed);
        assertTrue(NetworkResourcePolicy.validateModelUrl(
                "https://cas-bridge.xethub.hf.co/model/file.bin").allowed);
        assertTrue(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://huggingface.co/model/file.bin").allowed);
        assertFalse(NetworkResourcePolicy.validateModelUrl(
                "https://example.org/model.bin").allowed);
    }

    @Test
    public void rejectsPrivateAndReservedResolvedAddresses() {
        assertFalse(NetworkResourcePolicy.isPublicAddress(new byte[] {10, 0, 0, 1}));
        assertFalse(NetworkResourcePolicy.isPublicAddress(new byte[] {(byte) 192, (byte) 168, 1, 1}));
        assertFalse(NetworkResourcePolicy.isPublicAddress(new byte[] {100, 64, 0, 1}));
        assertTrue(NetworkResourcePolicy.isPublicAddress(new byte[] {8, 8, 8, 8}));
    }
}
