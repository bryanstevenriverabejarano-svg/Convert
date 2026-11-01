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

    static final String MULTI = "{\"version\":1,\"files\":["
            + "{\"p\":\"salve/core/memory/MemoryWritePolicy.java\",\"l\":300,\"d\":\"Qué recuerdos se guardan.\",\"m\":[]},"
            + "{\"p\":\"salve/core/memory/MemoryIndex.java\",\"l\":900,\"d\":\"Índice de memoria y recuerdos.\",\"m\":[]},"
            + "{\"p\":\"salve/core/memory/MemoryRepair.java\",\"l\":700,\"d\":\"Repara la memoria.\",\"m\":[]},"
            + "{\"p\":\"salve/core/SalveLLM.java\",\"l\":2000,\"d\":\"Motor del modelo local.\","
            + "\"m\":[{\"n\":\"generateResult\",\"d\":\"Genera texto con el modelo.\"}]},"
            + "{\"p\":\"salve/core/autonomy/AutonomousToolCommand.java\",\"l\":400,\"d\":\"Autonomía: herramientas propias.\",\"m\":[]}]}";

    @Test public void severalTopicsEachFindTheirFiles() {
        SelfCode code = SelfCode.parse(MULTI);
        String q = "revisa cómo funcionan tu memoria, tu LLM y tu sistema de autonomía";
        java.util.Set<String> paths = new java.util.HashSet<>();
        for (SelfCode.FileInfo f : code.searchTopics(q, 2, 6)) paths.add(f.path);
        assertTrue(paths.contains("salve/core/SalveLLM.java"));
        assertTrue(paths.contains("salve/core/autonomy/AutonomousToolCommand.java"));
        assertTrue(paths.stream().anyMatch(p -> p.contains("/memory/")));
    }

    @Test public void reportListsWhatWasInspectedAndItsRealLimit() {
        String r = SelfCode.parse(MULTI).report("tu memoria y tu LLM");
        assertTrue(r.startsWith("He mirado el índice de mi propio código (5 archivos"));
        assertTrue(r.contains("SalveLLM.java"));
        assertTrue(r.contains("generateResult"));
        assertTrue(r.contains("no el código línea a línea"));
        assertFalse(r.contains("Responde en primera persona"));
        assertTrue(SelfCode.parse(null).report("x").startsWith("No he podido mirar mi código"));
        assertTrue(SelfCode.parse(MULTI).answer("tu memoria").contains("usa self.read"));
    }

    @Test public void onlyAnExplicitCodeRequestReplacesTheModelAnswer() {
        assertTrue(SelfCode.explicitCodeRequest(salve.core.reports.SelfReportTest.BRYAN));
        assertFalse(SelfCode.explicitCodeRequest("¿te gusta tu cuerpo?"));
        assertFalse(SelfCode.explicitCodeRequest("¿cómo estás hecha?"));
    }

    /** Regresión 2026-11-01: «real» encontraba RealisticSpriteRenderer y «LLM» archivos secundarios. */
    @Test public void requestWordsDoNotPickRandomFilesAndEachTopicGetsItsFile() {
        String index = "{\"files\":["
                + "{\"p\":\"salve/avatar/RealisticSpriteRenderer.java\",\"l\":1,\"d\":\"Sprites.\",\"m\":[]},"
                + "{\"p\":\"salve/core/AutoModelUpgrade.java\",\"l\":1,\"d\":\"Cambia de modelo Gemma.\",\"m\":[]},"
                + "{\"p\":\"salve/core/SalveLLM.java\",\"l\":1,\"d\":\"Motor del modelo local.\",\"m\":[]},"
                + "{\"p\":\"salve/core/MemoriaEmocional.java\",\"l\":1,\"d\":\"Recuerdos con emoción.\",\"m\":[]},"
                + "{\"p\":\"salve/core/GoalAutonomyRuntime.java\",\"l\":1,\"d\":\"Metas y autonomía.\",\"m\":[]}]}";
        SelfCode code = SelfCode.parse(index);
        java.util.List<String> paths = new java.util.ArrayList<>();
        for (SelfCode.FileInfo f : code.searchTopics(salve.core.reports.SelfReportTest.BRYAN, 2, 6)) paths.add(f.path);
        assertFalse(paths.contains("salve/avatar/RealisticSpriteRenderer.java"));
        assertTrue(paths.containsAll(java.util.Arrays.asList(
                "salve/core/SalveLLM.java", "salve/core/MemoriaEmocional.java", "salve/core/GoalAutonomyRuntime.java")));
        assertEquals("salve/core/SalveLLM.java", code.search("tu LLM", 1).get(0).path);
    }
}
