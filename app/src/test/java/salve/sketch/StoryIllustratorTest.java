package salve.sketch;

import static org.junit.Assert.*;
import org.junit.Test;

public class StoryIllustratorTest {
    private static final String STORY = "Había una vez un castillo en lo alto de una montaña.\n\n"
            + "Cada noche la luna iluminaba sus torres.\n\n"
            + "Un dragón dormía junto al castillo, soñando con el mar.\n\n"
            + "Y colorín colorado, este cuento se ha acabado.";

    @Test public void illustratesParagraphsWithoutRepeatingMotifs() throws Exception {
        String out = StoryIllustrator.annotate(STORY, SketchTestAssets.library(), false);
        assertTrue(out.contains("[[salve_draw:castillo+montana]]"));
        assertTrue(out.contains("[[salve_draw:luna]]"));
        assertTrue(out.contains("[[salve_draw:dragon+mar]]"));
        assertEquals(3, SketchProtocol.extract(out, SketchTestAssets.library()).scenes.size());
        // El texto de voz queda idéntico al relato original.
        assertEquals(SketchProtocol.extract(STORY, SketchTestAssets.library()).clean,
                SketchProtocol.extract(out, SketchTestAssets.library()).clean);
        assertTrue(out.indexOf("[[salve_draw:luna]]") > out.indexOf("torres."));
    }

    @Test public void whileWritingTheLastParagraphWaits() throws Exception {
        String partial = "Había una vez un castillo.\n\nUn dragón dor";
        String out = StoryIllustrator.annotate(partial, SketchTestAssets.library(), true);
        assertTrue(out.contains("[[salve_draw:castillo]]"));
        assertFalse(out.contains("dragon]]"));
    }

    @Test public void alreadyIllustratedOrEmptyTextIsUntouched() throws Exception {
        String tagged = "Mira [[salve_draw:arbol]] un árbol.";
        assertSame(tagged, StoryIllustrator.annotate(tagged, SketchTestAssets.library(), false));
        assertEquals("", StoryIllustrator.annotate("", SketchTestAssets.library(), false));
    }

    @Test public void longSingleParagraphIsSplitBySentences() throws Exception {
        String text = "Una niña caminaba por el bosque buscando una flor mágica que brillaba de noche. "
                + "Llevaba consigo una vela pequeña para no perderse entre los árboles altos. "
                + "De pronto vio una mariposa enorme que la guió hasta un puente de piedra. "
                + "Al otro lado, la flor la esperaba bajo la luz de las estrellas.";
        String out = StoryIllustrator.annotate(text, SketchTestAssets.library(), false);
        assertTrue(out.split("\\[\\[salve_draw:").length - 1 >= 2);
    }
}
