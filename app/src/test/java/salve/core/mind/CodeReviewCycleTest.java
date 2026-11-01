package salve.core.mind;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class CodeReviewCycleTest {
    private static final String INDEX = "{\"version\":1,\"files\":["
            + "{\"p\":\"salve/ui/Pantalla.java\",\"l\":3,\"d\":\"\",\"m\":[]},"
            + "{\"p\":\"salve/core/memory/Recuerdos.java\",\"l\":3,\"d\":\"\",\"m\":[]},"
            + "{\"p\":\"salve/core/SalveLLM.java\",\"l\":3,\"d\":\"\",\"m\":[]}]}";

    private static File tempDir() throws Exception { return Files.createTempDirectory("salve-review").toFile(); }

    /** Un «modelo» de prueba que apunta qué le preguntaron. */
    private static final class Fake implements CodeReviewCycle.Model {
        final List<String> prompts = new ArrayList<>();
        public String generate(String prompt) { prompts.add(prompt); return "QUÉ HACE: nota " + prompts.size(); }
    }

    private static Map<String, String> sources() {
        Map<String, String> files = new HashMap<>();
        files.put("salve/ui/Pantalla.java", "class Pantalla {\n  void pinta() {}\n}\n");
        files.put("salve/core/memory/Recuerdos.java", "class Recuerdos {\n  void guarda() {}\n}\n");
        files.put("salve/core/SalveLLM.java", "class SalveLLM {\n  void genera() {}\n}\n");
        return files;
    }

    private static CodeReviewCycle.Result run(CodeReviewStore store, Map<String, String> files, CodeReviewCycle.Model model, long budget) {
        long[] clock = {1000};
        return CodeReviewCycle.run(SelfCode.parse(INDEX), files::get, store, prompt -> {
            clock[0] += 10_000; return model.generate(prompt);
        }, () -> false, () -> clock[0], budget);
    }

    @Test public void firstNightReadsPriorityZonesFirstAndRemembers() throws Exception {
        File dir = tempDir();
        CodeReviewStore store = new CodeReviewStore(dir);
        Fake model = new Fake();
        CodeReviewCycle.Result r = run(store, sources(), model, 15_000); // tiempo para ~2 trozos
        assertFalse(r.patchDetected);
        assertEquals(2, r.done.size());
        assertTrue(r.done.contains("salve/core/memory/Recuerdos.java"));
        assertTrue(r.done.contains("salve/core/SalveLLM.java"));
        assertEquals(1, r.pendingAfter);
        assertTrue(r.outOfTime);
        assertTrue(model.prompts.get(0).contains("No inventes"));
        // Lo aprendido sobrevive a un reinicio (otra instancia del almacén).
        CodeReviewStore reopened = new CodeReviewStore(dir);
        assertNotNull(reopened.note("salve/core/SalveLLM.java"));
        assertNull(reopened.note("salve/ui/Pantalla.java"));
        assertEquals(2, reopened.findings().size());
    }

    @Test public void afterReadingEverythingOnlyAPatchWakesItUpAndOnlyTheChangeIsSent() throws Exception {
        CodeReviewStore store = new CodeReviewStore(tempDir());
        Map<String, String> files = sources();
        run(store, files, new Fake(), 1_000_000);
        Fake quiet = new Fake();
        CodeReviewCycle.Result nothing = run(store, files, quiet, 1_000_000);
        assertTrue(quiet.prompts.isEmpty());
        assertEquals("Mi código ya está revisado; no hay parches nuevos.", nothing.describe());

        files.put("salve/core/SalveLLM.java", "class SalveLLM {\n  void generaMasRapido() {}\n}\n");
        files.put("salve/core/Nuevo.java", "class Nuevo {}\n");
        Fake patch = new Fake();
        // El índice del parche ya incluye el archivo nuevo.
        String index = INDEX.substring(0, INDEX.length() - 2) + ",{\"p\":\"salve/core/Nuevo.java\",\"l\":1,\"d\":\"\",\"m\":[]}]}";
        CodeReviewCycle.Result r = CodeReviewCycle.run(SelfCode.parse(index), files::get, store, patch,
                () -> false, () -> 0L, 1_000_000);
        assertTrue(r.patchDetected);
        assertEquals(java.util.Arrays.asList("salve/core/SalveLLM.java", "salve/core/Nuevo.java"), r.done);
        String sent = patch.prompts.get(0);
        assertTrue(sent.contains("-  void genera() {}"));
        assertTrue(sent.contains("+  void generaMasRapido() {}"));
        assertTrue(sent.contains("Lo que ya sabías de este archivo"));
        assertFalse(sent.contains("Recuerdos")); // lo que no cambió no se relee
        assertTrue(store.note("salve/core/SalveLLM.java").startsWith("Último parche (+1 / -1 líneas)"));
    }

    @Test public void aSilentModelNeverMarksAnythingAsReviewed() throws Exception {
        CodeReviewStore store = new CodeReviewStore(tempDir());
        CodeReviewCycle.Result r = run(store, sources(), prompt -> null, 1_000_000);
        assertTrue(r.modelFailed);
        assertTrue(r.done.isEmpty());
        assertEquals(0, store.reviewedCount());
        assertTrue(r.describe().contains("no respondió"));
    }

    @Test public void longFilesContinueWhereTheyStopped() throws Exception {
        CodeReviewStore store = new CodeReviewStore(tempDir());
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 400; i++) big.append("    int valor").append(i).append(" = ").append(i).append("; // comentario largo\n");
        Map<String, String> files = new HashMap<>();
        files.put("salve/core/SalveLLM.java", big.toString());
        String index = "{\"files\":[{\"p\":\"salve/core/SalveLLM.java\",\"l\":400,\"d\":\"\",\"m\":[]}]}";
        long[] clock = {0};
        Fake model = new Fake();
        CodeReviewCycle.Result first = CodeReviewCycle.run(SelfCode.parse(index), files::get, store,
                p -> { clock[0] += 10; return model.generate(p); }, () -> false, () -> clock[0], 15);
        assertTrue(first.done.isEmpty());
        assertEquals(2, first.chunks);
        String hash = CodeReviewStore.hash(big.toString());
        int resume = store.partialLine("salve/core/SalveLLM.java", hash);
        assertTrue(resume > 1);
        CodeReviewCycle.Result second = CodeReviewCycle.run(SelfCode.parse(index), files::get, store,
                model, () -> false, () -> 0L, 1_000_000);
        assertEquals(1, second.done.size());
        assertTrue(model.prompts.get(2).contains("líneas " + resume + "–"));
        assertNotNull(store.note("salve/core/SalveLLM.java"));
        assertTrue(store.note("salve/core/SalveLLM.java").length() <= CodeReviewStore.MAX_NOTE);
    }

    @Test public void nightsCarryUnreportedFindingsAndReportSaysWhatIsLeft() throws Exception {
        CodeReviewStore store = new CodeReviewStore(tempDir());
        store.startNight("2026-11-01");
        store.addFinding("PARCHE · a.java (+1 / -1)\nCAMBIO: x");
        store.addSpent(5000);
        store.startNight("2026-11-02");
        assertEquals(0, store.spentMs());
        assertEquals(1, store.findings().size()); // no se informó: pasa a la noche siguiente
        assertFalse(store.reportedNight());
        String report = CodeReviewCycle.report(store.findings(), 3, 10, "2026-11-02");
        assertTrue(report.contains("1 con cambios del último parche"));
        assertTrue(report.contains("Me quedan 3 por leer"));
        assertTrue(report.contains("sin implementar ni verificar"));
        store.markReported();
        store.startNight("2026-11-03");
        assertTrue(store.findings().isEmpty());
    }

    @Test public void selfCodeAnswersWithWhatSheAlreadyLearned() {
        String index = "{\"files\":[{\"p\":\"salve/core/SalveLLM.java\",\"l\":3,\"d\":\"Motor del modelo local.\",\"m\":[]}]}";
        String a = SelfCode.parse(index).answer("tu LLM", p -> p.endsWith("SalveLLM.java") ? "QUÉ HACE: carga Gemma" : null);
        assertTrue(a.contains("LO_QUE_YA_REVISASTE"));
        assertTrue(a.contains("carga Gemma"));
        assertFalse(SelfCode.parse(index).answer("tu LLM", p -> null).contains("LO_QUE_YA_REVISASTE"));
    }

    @Test public void aHugePatchResumesWithoutClaimingAnUnreadTail() throws Exception {
        CodeReviewStore store = new CodeReviewStore(tempDir());
        Map<String, String> files = sources();
        run(store, files, new Fake(), 1_000_000);
        StringBuilder replacement = new StringBuilder("class SalveLLM {\n");
        for (int i = 0; i < 250; i++)
            replacement.append("  int numero").append(i).append(" = ").append(i + 10).append(";\n");
        replacement.append("}\n");
        files.put("salve/core/SalveLLM.java", replacement.toString());
        String hash = CodeReviewStore.hash(replacement.toString());
        long[] clock = {0};
        Fake firstModel = new Fake();
        CodeReviewCycle.Result first = CodeReviewCycle.run(SelfCode.parse(INDEX), files::get, store,
                prompt -> { clock[0] += 10; return firstModel.generate(prompt); },
                () -> false, () -> clock[0], 5); // one page; the rest must resume later
        assertFalse(first.done.contains("salve/core/SalveLLM.java"));
        assertTrue(store.partialLine("salve/core/SalveLLM.java", hash) > 1);
        assertTrue(first.pendingAfter > 0);
        assertNotEquals(hash, store.reviewed("salve/core/SalveLLM.java"));

        Fake secondModel = new Fake();
        CodeReviewCycle.Result second = CodeReviewCycle.run(SelfCode.parse(INDEX), files::get, store,
                secondModel, () -> false, () -> 0L, 1_000_000);
        assertTrue(second.done.contains("salve/core/SalveLLM.java"));
        assertEquals(hash, store.reviewed("salve/core/SalveLLM.java"));
        assertTrue("El final del parche nunca llegó al modelo",
                secondModel.prompts.stream().anyMatch(p -> p.contains("+  int numero249 = 259;")));
        assertTrue(secondModel.prompts.stream().noneMatch(p -> p.contains("…(más cambios)")));
    }

}
