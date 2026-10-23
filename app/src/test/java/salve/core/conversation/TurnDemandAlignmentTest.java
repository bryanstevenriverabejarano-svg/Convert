package salve.core.conversation;

import org.junit.Test;
import static org.junit.Assert.*;

public class TurnDemandAlignmentTest {
    @Test public void whyQuestionCannotBePassedByGenericGreeting() {
        String question = "¿Por qué eres única en tu especie?";
        assertNotNull(TurnChecks.problem(question,
                "Soy Salve. Es un placer conversar contigo.", true));
        assertNull(TurnChecks.problem(question,
                "Me distingo porque conservo una memoria diferente de cada experiencia y la uso para responder.",
                true));
    }
    @Test public void contrastsNeedContrastNotOnlyAnIntro() {
        assertNotNull(TurnChecks.problem("Compara Android y Windows",
                "Los dos sistemas son interesantes.", false));
        assertNull(TurnChecks.problem("Compara Android y Windows",
                "Android y Windows cumplen objetivos distintos, mientras uno está pensado principalmente para móviles y el otro para PC.",
                false));
    }
    @Test public void noFalseAlarmForNormalConversationAndClarifications() {
        assertNull(TurnChecks.problem("Hola", "Hola, ¿cómo estás?", false));
        assertNull(TurnChecks.problem("¿Puedes explicarme por qué pasa?",
                "Pasa porque el aire se expande al calentarse.", false));
    }
}