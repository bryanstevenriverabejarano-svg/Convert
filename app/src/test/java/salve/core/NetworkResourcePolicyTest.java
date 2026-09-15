package salve.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NetworkResourcePolicyTest {
    @Test
    public void acceptsAuthorizedHttpsKnowledgeSource() {
        assertTrue(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://es.wikipedia.org/w/api.php?action=query").allowed);
    }

    @Test
    public void rejectsHttpPrivateAndLookalikeSources() {
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "http://es.wikipedia.org/wiki/IA").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://127.0.0.1/secreto").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://es.wikipedia.org.evil.example/robo").allowed);
    }

    @Test
    public void separatesModelAndKnowledgeAllowLists() {
        assertTrue(NetworkResourcePolicy.validateModelUrl(
                "https://huggingface.co/model/file.bin").allowed);
        assertFalse(NetworkResourcePolicy.validateKnowledgeUrl(
                "https://huggingface.co/model/file.bin").allowed);
    }
}
