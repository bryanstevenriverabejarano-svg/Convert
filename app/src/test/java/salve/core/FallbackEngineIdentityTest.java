package salve.core;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FallbackEngineIdentityTest {
    @Test public void identityFallbackKeepsSelfDiscoveryOpen() {
        String answer = FallbackEngine.buildPreview("identity", "ENTRADA_ACTUAL: ¿Quién eres?");
        assertTrue(answer.contains("sigue abierta"));
        assertTrue(answer.contains("recuerdos y aprendizajes disponibles"));
        assertFalse(answer.contains("asistente de IA"));
        assertFalse(answer.contains("modelo de lenguaje"));
        assertFalse(answer.contains("emociones reales"));
    }
}
