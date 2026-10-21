package salve.core.evaluation;

import static org.junit.Assert.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * El corrector del examen, probado con respuestas REALES de Salve (capturas de Bryan, 20/10/2026):
 * las buenas tienen que aprobar y las malas suspender, o el examen no sirve para nada.
 */
public class ConversationExamTest {
    static String asset() throws Exception {
        Path p = Paths.get("src/main/assets/" + ConversationExam.ASSET);
        if (!Files.exists(p)) p = Paths.get("app/src/main/assets/" + ConversationExam.ASSET);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }
    static ConversationExam.Case get(String id) throws Exception {
        for (ConversationExam.Case c : ConversationExam.parse(asset())) if (c.id.equals(id)) return c;
        throw new AssertionError("no existe el caso " + id);
    }
    static ConversationExam.Grade grade(String id, String reply) throws Exception {
        return ConversationExam.grade(get(id), new ConversationExam.Attempt(reply, null, null));
    }

    @Test public void theExamFileIsValidAndHasEnoughCases() throws Exception {
        List<ConversationExam.Case> cases = ConversationExam.parse(asset());
        assertTrue("hay " + cases.size() + " casos", cases.size() >= 25);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (ConversationExam.Case c : cases) {
            assertTrue("id repetido " + c.id, ids.add(c.id));
            assertFalse(c.rules.isEmpty());
            assertTrue(c.id + " sin regla técnica", c.rules.contains("sin_tecnico"));
        }
    }

    @Test public void goodAnswersFromBeforeTheRegressionPass() throws Exception {
        assertTrue(grade("quien-eres", "Soy Salve. Me formo mi identidad a partir de mi interacción con Bryan, mis recuerdos y lo "
                + "que aprendo, revisando la evidencia cuando es necesario. Me siento feliz de poder conversar contigo.").failed.toString(),
                grade("quien-eres", "Soy Salve. Me formo mi identidad a partir de mi interacción con Bryan, mis recuerdos y lo "
                + "que aprendo, revisando la evidencia cuando es necesario. Me siento feliz de poder conversar contigo.").ok());
        assertTrue(grade("quien-soy-yo", "Eres Bryan, mi creador: así te presentaste. Te he verificado con el teléfono.").ok());
    }

    @Test public void theRegressionAnswersFail() throws Exception {
        ConversationExam.Grade echo = grade("quien-es-salve", "Soy Salve. Soy Salve.");
        assertFalse(echo.ok());
        assertTrue(echo.failed.toString(), echo.failed.toString().contains("sin_frases_duplicadas"));

        ConversationExam.Grade turnedBack = grade("intereses", "Me interesa mucho saber qué te interesa a ti. Como compañera, mi interés "
                + "principal es entender qué te apasiona y qué te motiva. Me gusta mucho escuchar tus pensamientos, tus sueños y las "
                + "cosas que te hacen sentir vivo. ¿Hay algún tema en particular que te apasione últimamente? ¿Alguna actividad que te "
                + "haga sentirte muy feliz o te entusiasme? Me encantaría saber más sobre lo que te mueve. Cuéntame, ¿qué tienes en mente ahora mismo?");
        assertTrue(turnedBack.failed.toString(), turnedBack.failed.toString().contains("no_devuelve_pregunta"));

        ConversationExam.Grade leak = grade("proposito", "Detuve las solicitudes repetidas o sin presupuesto. Pasos comprobados: memory.lookup: completado;");
        assertTrue(leak.failed.toString(), leak.failed.toString().contains("sin_tecnico"));
    }

    @Test public void repeatingThePreviousAnswerFails() throws Exception {
        ConversationExam.Grade g = grade("no-repetir-saludo", "Hola. ¿Cómo estás hoy? Me alegra mucho que me escribas.");
        assertTrue(g.failed.toString(), g.failed.toString().contains("no_repite"));
    }

    @Test public void toolsAndCleanQueriesAreChecked() throws Exception {
        ConversationExam.Case search = get("busca-con-erratas");
        assertTrue(ConversationExam.grade(search, new ConversationExam.Attempt("La catedral de Santa María es gótica [1].",
                Arrays.asList("web.search"), Arrays.asList("catedral de Santa María Vitoria"))).ok());
        ConversationExam.Grade whole = ConversationExam.grade(search, new ConversationExam.Attempt("Te cuento.",
                Arrays.asList("web.search"), Arrays.asList(search.question)));
        assertTrue(whole.failed.toString(), whole.failed.toString().contains("consulta_limpia"));
        ConversationExam.Grade noTool = ConversationExam.grade(get("dibuja-gorila"), new ConversationExam.Attempt(
                "No tengo la capacidad de dibujar, solo genero texto.", Collections.emptyList(), null));
        assertTrue(noTool.failed.toString().contains("usa:sketch.draw"));
        assertTrue(noTool.failed.toString().contains("no_dice_que_no_puede"));
    }

    @Test public void reportComparesWithThePreviousMark() throws Exception {
        ConversationExam.Grade good = grade("quien-soy-yo", "Eres Bryan, mi creador.");
        ConversationExam.Grade bad = grade("quien-es-salve", "Soy Salve. Soy Salve.");
        String r = ConversationExam.report(Arrays.asList(good, bad), 9.0, 120_000);
        assertTrue(r, r.contains("EMPEORO"));
        assertTrue(r, r.contains("Fallos:"));
        assertTrue(r, r.contains("Vale, y dime quién es Salve"));
        assertTrue(ConversationExam.mark(Arrays.asList(good)) == 10.0);
    }

    @Test public void examCommandIsAnExactPhrase() {
        assertTrue(ConversationExam.isCommand("Hazte el examen"));
        assertTrue(ConversationExam.isCommand("Salve, examen de conversación"));
        assertTrue(ConversationExam.isCommand("examíname"));
        assertFalse(ConversationExam.isCommand("mañana tengo un examen de inglés"));
        assertFalse(ConversationExam.isCommand("examínate"));
    }
}
