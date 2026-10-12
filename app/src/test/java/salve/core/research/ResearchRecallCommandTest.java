package salve.core.research;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResearchRecallCommandTest {
    @Test public void recallsLocallyInNaturalSpanish() {
        for (String input : new String[] {"Mi última búsqueda", "¿Cuál fue tu última investigación?",
                "Salve, muéstrame la última búsqueda, por favor", "¿Qué encontraste la última vez?",
                "Recupera mi última búsqueda", "Resultado de la última investigación"})
            assertTrue(input, ResearchRecallCommand.matches(input));
    }
    @Test public void doesNotConfuseNewRequestsOrQuotedAndNegatedCommands() {
        for (String input : new String[] {"Busca otra vez", "Busca la última investigación sobre memoria",
                "No muestres la última búsqueda", "Ella dijo: mi última búsqueda", "¿Qué investigaste sobre Marte?", ""})
            assertFalse(input, ResearchRecallCommand.matches(input));
        assertFalse(ResearchRecallCommand.matches(null));
    }
}
