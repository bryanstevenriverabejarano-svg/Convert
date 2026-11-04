package salve.core.voice;

import static org.junit.Assert.*;
import static salve.core.voice.ModelSwitchCommand.Target.*;
import org.junit.Test;

/** Cambiar de modelo por voz, tal como lo escribe el dictado en el coche. */
public class ModelSwitchCommandTest {
    @Test public void understandsE2bHoweverTheDictationWritesIt() {
        assertEquals(E2B, ModelSwitchCommand.parse("Usa el modelo Gemma 4 E2B"));
        assertEquals(E2B, ModelSwitchCommand.parse("usa el modelo gemma 42b"));
        assertEquals(E2B, ModelSwitchCommand.parse("Salve, cámbiate a gemma cuatro e dos b"));
        assertEquals(E2B, ModelSwitchCommand.parse("pasa a gema 4 2 b por favor"));
        assertEquals(E2B, ModelSwitchCommand.parse("vuelve a Gemma 4 E2B"));
        assertEquals(E2B, ModelSwitchCommand.parse("quiero que uses el modelo rápido"));
        assertEquals(E2B, ModelSwitchCommand.parse("usa el modelo más ligero"));
    }

    @Test public void understandsE4b() {
        assertEquals(E4B, ModelSwitchCommand.parse("usa el modelo Gemma 4 E4B"));
        assertEquals(E4B, ModelSwitchCommand.parse("cámbiate a gemma 44b"));
        assertEquals(E4B, ModelSwitchCommand.parse("activa gemma cuatro e cuatro b"));
        assertEquals(E4B, ModelSwitchCommand.parse("usa el modelo grande"));
        assertEquals(E4B, ModelSwitchCommand.parse("vuelve al modelo más listo"));
    }

    @Test public void questionsAndOtherSentencesAreNotSwitches() {
        assertNull(ModelSwitchCommand.parse("qué tal funciona gemma 4 e2b"));
        assertNull(ModelSwitchCommand.parse("¿qué modelo usas?"));
        assertNull(ModelSwitchCommand.parse("pon te arrepentirás en youtube"));
        assertNull(ModelSwitchCommand.parse("cambia de e2b a e4b o no sé"));
        assertNull(ModelSwitchCommand.parse(null));
        assertNull(ModelSwitchCommand.parse("usa el modelo gemma 4 e2b para escribir un cuento muy largo sobre dragones que vuelan por la noche en el bosque"));
    }

    @Test public void onlyTheTwoUncensoredModelsRemain() {
        assertEquals(E4B, ModelSwitchCommand.parse("usa el modelo sin censura"));
        assertEquals(E4B, ModelSwitchCommand.parse("descarga gemma 4 e4b sin censura"));
        assertEquals(E2B, ModelSwitchCommand.parse("usa gemma 4 e2b sin censura"));
        assertEquals(E2B, ModelSwitchCommand.parse("usa el modelo rápido sin censura"));
        assertEquals("decir Qwen lleva al sustituto", E4B, ModelSwitchCommand.parse("usa el modelo cuen"));
        assertNull("una pregunta no cambia nada", ModelSwitchCommand.parse("qué tal es el modelo sin censura"));
        assertNull("«cuenta» no es Qwen", ModelSwitchCommand.parse("usa la cuenta de gmail"));
    }

    @Test public void labels() {
        assertEquals("Gemma 4 E2B sin censura", ModelSwitchCommand.label(E2B));
        assertEquals("Gemma 4 E4B sin censura", ModelSwitchCommand.label(E4B));
    }
}
