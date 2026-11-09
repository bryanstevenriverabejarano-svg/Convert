package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.util.List;
import org.junit.Test;
import salve.core.mind.SelfCode;

/** Salve puede cambiar su estructura cuando Bryan lo pide, pero no lo que la mide ni lo que la autoriza. */
public class SelfChangeScopeTest {
    @Test public void aDirectedChangeNeedsAnExplicitGoal() {
        assertEquals("respondas mas rapido a la bateria",
                SelfChangeScope.goal("Salve, abre un PR para que respondas más rápido a la batería"));
        assertEquals("leer mejor los sensores", SelfChangeScope.goal("Cambia tu código para leer mejor los sensores"));
        assertEquals("tu memoria de trabajo", SelfChangeScope.goal("modifica tu estructura de tu memoria de trabajo"));
        assertNull("la orden de siempre sigue siendo la del examen", SelfChangeScope.goal("abre un PR"));
        assertNull(SelfChangeScope.goal("abre un PR para mejorarte"));
        assertNull("una negación no es una orden", SelfChangeScope.goal("no abras un PR para que cambies la voz"));
        assertNull(SelfChangeScope.goal("¿cómo van tus PR?"));
        assertNull(SelfChangeScope.goal(null));
        assertTrue(SelfPrCommands.isCommand("abre un PR para que respondas más rápido a la batería"));
        assertTrue(SelfPrCommands.isCommand("abre un PR"));
        assertFalse(SelfPrCommands.isCommand("no abras un PR para que cambies la voz"));
    }

    @Test public void sheCannotRewriteHerOwnJudgeOrHerGate() {
        String root = SelfChangeScope.ROOT;
        assertTrue(SelfChangeScope.allowed(root + "sensors/DeviceSenses.java"));
        assertTrue(SelfChangeScope.allowed(root + "conversation/TurnChecks.java"));
        assertTrue(SelfChangeScope.allowed(root + "MotorConversacional.java"));
        for (String inside : new String[]{"evaluation/ConversationExam.java", "selfpr/SelfPatch.java", "selfpr/SelfChangeScope.java",
                "identity/IdentityManager.java", "privatefamily/X.java", "finance/Y.java", "SelfPrDesk.java", "ObjectiveGovernance.java"})
            assertFalse(inside, SelfChangeScope.allowed(root + inside));
        assertFalse("fuera del núcleo", SelfChangeScope.allowed("app/src/main/java/salve/services/SalveAccessibilityService.java"));
        assertFalse(SelfChangeScope.allowed(root + "../services/X.java"));
        assertFalse(SelfChangeScope.allowed(root + "LocalModelPolicy.kt"));
        assertFalse(SelfChangeScope.allowed(".github/workflows/salve-self-pr.yml"));
        assertFalse(SelfChangeScope.allowed(null));
    }

    @Test public void theTargetComesFromHerOwnCodeIndexAndSkipsProtectedFiles() {
        SelfCode index = SelfCode.parse("{\"files\":["
                + "{\"p\":\"salve/core/evaluation/ConversationExam.java\",\"l\":300,\"d\":\"Examen de conversación con sensores\",\"m\":[]},"
                + "{\"p\":\"salve/core/sensors/DeviceSenses.java\",\"l\":250,\"d\":\"Los sentidos del teléfono: sensores, batería, luz\","
                + "\"m\":[{\"n\":\"offered\",\"d\":\"cuándo se ofrecen los sensores\"}]},"
                + "{\"p\":\"salve/avatar/AstralBody.java\",\"l\":240,\"d\":\"Cuerpo astral\",\"m\":[]}]}");
        assertEquals(SelfChangeScope.ROOT + "sensors/DeviceSenses.java", SelfChangeScope.choose(index, "leer mejor los sensores"));
        assertNull("su cuerpo queda fuera del núcleo que puede tocar", SelfChangeScope.choose(index, "cuerpo astral"));
        assertNull(SelfChangeScope.choose(SelfCode.parse("{}"), "leer mejor los sensores"));
        assertNull(SelfChangeScope.choose(index, null));
    }

    @Test public void lineageIsAppendOnlyAndSaysWhenTheExamGotWorse() throws Exception {
        File file = new File(File.createTempFile("linaje", ".tmp").getParentFile(), "salve-linaje-" + System.nanoTime() + "/linaje.jsonl");
        assertTrue(SelfLineage.read(file, 5).isEmpty());
        SelfLineage.append(file, SelfLineage.entry(1_000L, "examen", "no repite", SelfChangeScope.ROOT + "conversation/ReplyReviewer.java",
                "0123456789abcdef0123456789abcdef01234567", "https://github.com/o/r/issues/1", 3));
        SelfLineage.append(file, SelfLineage.entry(2_000L, "bryan", "leer mejor los sensores", SelfChangeScope.ROOT + "sensors/DeviceSenses.java",
                "0123456789abcdef0123456789abcdef01234567", "https://github.com/o/r/issues/2", 2));
        java.nio.file.Files.write(file.toPath(), "esto no es json\n".getBytes(), java.nio.file.StandardOpenOption.APPEND);
        List<JsonObject> all = SelfLineage.read(file, 5);
        assertEquals(2, all.size());
        assertEquals(1, SelfLineage.read(file, 1).size());
        String worse = SelfLineage.describe(all, 4);
        assertTrue(worse, worse.contains("DeviceSenses.java: «leer mejor los sensores» (me lo pidió Bryan)"));
        assertTrue(worse, worse.contains("ReplyReviewer.java: «no repite» (por mi examen)"));
        assertTrue(worse, worse.contains("tenía 2 casos fallidos; el más reciente tiene 4. Ha empeorado"));
        assertTrue(SelfLineage.describe(all, 1).contains("Ha mejorado."));
        assertFalse("sin examen no se compara", SelfLineage.describe(all, -1).contains("casos fallidos"));
        assertEquals("", SelfLineage.describe(SelfLineage.read(new File("no-existe.jsonl"), 5), 0));

        JsonObject exam = JsonParser.parseString("{\"detail\":{\"cases\":[{\"failed\":[\"no_repite\"]},{\"failed\":[]},{\"question\":\"x\"}]}}").getAsJsonObject();
        assertEquals(1, SelfLineage.failedCases(exam));
        assertEquals(-1, SelfLineage.failedCases(null));
        assertEquals(-1, SelfLineage.failedCases(new JsonObject()));
        file.delete(); file.getParentFile().delete();
    }

    @Test public void theModelIsToldWhoAskedForTheChange() {
        String asked = SelfPatch.prompt("leer mejor los sensores", "el cambio mínimo", null, "X.java", "class X {}\n", null, true);
        assertTrue(asked.contains("Cambio que te pide Bryan: leer mejor los sensores"));
        assertTrue(SelfPatch.prompt("no repite", "idea", null, "X.java", "class X {}\n", null).contains("Problema que mostró tu examen"));
    }
}
