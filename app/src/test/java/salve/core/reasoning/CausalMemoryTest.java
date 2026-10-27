package salve.core.reasoning;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import salve.core.cognitive.ReasoningEngine;

public class CausalMemoryTest {
    @Test public void extractsOnlyStatedCauses() {
        List<CausalMemory.Relation> r = CausalMemory.extract("No duermo bien porque tomo café tarde.");
        assertEquals(1, r.size());
        assertEquals("tomo cafe tarde", r.get(0).cause);
        assertEquals("no duermo bien", r.get(0).effect);

        r = CausalMemory.extract("Si llueve, se corta la luz del taller");
        assertEquals("llueve", r.get(0).cause);
        assertEquals("corta la luz del taller", r.get(0).effect);

        r = CausalMemory.extract("El estrés provoca dolor de cabeza. Y cuando hace frío, me duele la rodilla.");
        assertEquals(2, r.size());
        assertEquals("estres", r.get(0).cause);
        assertEquals("dolor de cabeza", r.get(0).effect);
        assertEquals(ReasoningEngine.LinkType.TEMPORAL, r.get(1).type);
    }

    @Test public void questionsAndEmptyClausesTeachNothing() {
        assertTrue(CausalMemory.extract("¿Por qué no duermo bien?").isEmpty());
        assertTrue(CausalMemory.extract("Sí, claro").isEmpty());
        assertTrue(CausalMemory.extract("Lo hago porque sí").isEmpty());
        assertTrue(CausalMemory.extract("hola Salve").isEmpty());
        assertTrue(CausalMemory.extract(null).isEmpty());
    }

    @Test public void answersWhyWithStatedAndDeducedCauses() {
        CausalMemory memory = new CausalMemory(new ReasoningEngine(), null);
        memory.learn("El estrés me hace tomar café tarde.");
        memory.learn("No duermo bien porque tomo café tarde.");
        String why = memory.promptBlock("¿Por qué no duermo bien?");
        assertTrue(why, why.contains("cafe tarde → no duermo bien"));
        assertTrue(why, why.contains("estres") && why.contains("deducido"));
        assertEquals("", memory.promptBlock("¿Qué tal estás?"));
    }

    @Test public void answersWhatIfByChainingForward() {
        CausalMemory memory = new CausalMemory(new ReasoningEngine(), null);
        memory.learn("Si llueve, se corta la luz del taller. Si se corta la luz del taller, se para la impresora.");
        String block = memory.promptBlock("¿Qué pasa si llueve mañana?");
        assertTrue(block, block.contains("llueve → corta la luz del taller"));
        assertTrue(block, block.contains("para la impresora") && block.contains("deducido"));
    }

    @Test public void survivesRestart() throws Exception {
        File file = File.createTempFile("causal", ".tsv");
        file.delete();
        new CausalMemory(new ReasoningEngine(), file).learn("El estrés provoca dolor de cabeza.");
        assertTrue(Files.readAllLines(file.toPath()).get(0).startsWith("estres\tdolor de cabeza"));
        CausalMemory reloaded = new CausalMemory(new ReasoningEngine(), file);
        assertTrue(reloaded.promptBlock("¿por qué me duele la cabeza? tengo dolor de cabeza").contains("estres"));
        file.delete();
    }
}
