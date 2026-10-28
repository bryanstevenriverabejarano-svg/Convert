package salve.core.mind;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class SelfCodeTest {
    static final String INDEX = "{\"version\":1,\"files\":["
            + "{\"p\":\"salve/core/DrawingStudio.java\",\"l\":1200,\"d\":\"Su taller de dibujo: buscar y trazar referencias.\","
            + "\"m\":[{\"n\":\"dibujarRetratoPropio\",\"d\":\"Se dibuja a sí misma con sus propias imágenes.\"}]},"
            + "{\"p\":\"salve/core/memory/MemoryWritePolicy.java\",\"l\":300,\"d\":\"Qué recuerdos se guardan.\",\"m\":[]}],"
            + "\"avatar\":{\"realistic\":[\"base.png\",\"walk.png\"],\"realistic_v1\":[\"base.png\"]}}";

    @Test public void findsTheFileThatAnswers() {
        SelfCode code = SelfCode.parse(INDEX);
        assertEquals(2, code.fileCount());
        assertEquals(1500, code.totalLines());
        List<SelfCode.FileInfo> hits = code.search("¿cómo dibujas?", 3);
        assertEquals("salve/core/DrawingStudio.java", hits.get(0).path);
        assertEquals("salve/core/memory/MemoryWritePolicy.java", code.search("dónde guardas tus recuerdos", 3).get(0).path);
    }

    @Test public void answerListsHerBodiesAndRelevantFiles() {
        String a = SelfCode.parse(INDEX).answer("cómo te dibujas a ti misma");
        assertTrue(a.contains("realistic"));
        assertTrue(a.contains("DrawingStudio.java"));
        assertTrue(a.contains("dibujarRetratoPropio"));
    }

    @Test public void brokenOrMissingIndexNeverFails() {
        assertEquals(0, SelfCode.parse("no es json").fileCount());
        assertTrue(SelfCode.parse(null).answer("lo que sea").startsWith("No tengo"));
    }

    @Test public void offeredOnlyWhenAskedAboutHerself() {
        assertTrue(SelfCode.offered("mira tu propio código y dime cómo funcionas"));
        assertTrue(SelfCode.offered("¿Cómo estás hecha?"));
        assertFalse(SelfCode.offered("dibújame un gato"));
    }
}
