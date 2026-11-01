package salve.core.reports;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import salve.core.mind.CodeReviewCycle;

public class ReportLibraryTest {
    private static File pdf(File dir, long stamp) throws Exception {
        File f = new File(dir, "informe-" + stamp + ".pdf");
        Files.write(f.toPath(), new byte[]{'%', 'P', 'D', 'F'});
        return f;
    }

    @Test public void eachKindOfReportGetsATitleAndCategory() {
        File f = new File("informe-1.pdf");
        ReportLibrary.Entry code = ReportLibrary.describe(f,
                CodeReviewCycle.report(java.util.Arrays.asList("PARCHE · salve/core/SalveLLM.java (+1 / -1)\nCAMBIO: x"), 3, 10, "2026-11-01"), 1);
        assertEquals(ReportLibrary.Category.CODE, code.category);
        assertTrue(code.title.startsWith("Revisión de mi propio código"));
        assertTrue(code.preview.startsWith("Resumen: 1 archivo revisado"));

        ReportLibrary.Entry chat = ReportLibrary.describe(f, SelfReport.compose("Ideas para mis dibujos", "Propongo trazar más lento."), 1);
        assertEquals(ReportLibrary.Category.CHAT, chat.category);
        assertEquals("Ideas para mis dibujos", chat.title);
        assertEquals("Propongo trazar más lento.", chat.preview);

        ReportLibrary.Entry research = ReportLibrary.describe(f, "INFORME AUTÓNOMO DE SALVE\nCategoría: Investigación autónoma\n"
                + "Estado: hipótesis\n\nANÁLISIS DE SALVE\nLos pulpos tienen tres corazones.", 1);
        assertEquals(ReportLibrary.Category.RESEARCH, research.category);
        assertEquals("Los pulpos tienen tres corazones.", research.preview);
        assertEquals(ReportLibrary.Category.SLEEP, ReportLibrary.describe(f,
                "INFORME AUTÓNOMO DE SALVE\nCategoría: Sueño y práctica autónoma\nx", 1).category);
        ReportLibrary.Entry task = ReportLibrary.describe(f, "INFORME DE SALVE: Dibujo\n\nFuente: Registro", 1);
        assertEquals(ReportLibrary.Category.JOURNAL, task.category);
        assertEquals("Tarea: Dibujo", task.title);
    }

    @Test public void listsNewestFirstFiltersAndKeepsOldReportsWithoutCard() throws Exception {
        File dir = Files.createTempDirectory("biblioteca").toFile();
        File old = pdf(dir, 1_700_000_000_000L); // de antes de las fichas
        File newer = pdf(dir, 1_800_000_000_000L);
        ReportLibrary.writeMeta(ReportLibrary.describe(newer, SelfReport.compose("Revisión de mi propio código", "x"), 1_800_000_000_000L));
        List<ReportLibrary.Entry> all = ReportLibrary.list(dir, null);
        assertEquals(2, all.size());
        assertEquals(newer, all.get(0).pdf);
        assertEquals("Revisión de mi propio código", all.get(0).title);
        assertEquals("Informe", all.get(1).title);
        assertEquals(1_700_000_000_000L, all.get(1).createdAt);
        assertEquals(1, ReportLibrary.list(dir, ReportLibrary.Category.CODE).size());
        assertTrue(ReportLibrary.delete(all.get(0)));
        assertFalse(newer.exists());
        assertFalse(ReportLibrary.metaFile(newer).exists());
        assertEquals(1, ReportLibrary.list(dir, null).size());
        assertTrue(ReportLibrary.list(new File(dir, "no-existe"), null).isEmpty());
        assertTrue(old.exists());
    }

    @Test public void understandsHowBryanAsksToOpenThem() {
        assertEquals(ReportLibrary.TAB_EXECUTION, ReportLibrary.openRequest("Registro de ejecución ábrelo"));
        assertEquals(ReportLibrary.TAB_EXECUTION, ReportLibrary.openRequest("Abre el registro de ejecución"));
        assertEquals(ReportLibrary.TAB_LIBRARY, ReportLibrary.openRequest("Abre tu biblioteca"));
        assertEquals(ReportLibrary.TAB_LIBRARY, ReportLibrary.openRequest("muéstrame tus informes"));
        assertEquals(ReportLibrary.TAB_LIBRARY, ReportLibrary.openRequest("Salve, biblioteca de informes, ábrela por favor"));
        assertEquals(ReportLibrary.TAB_LIBRARY, ReportLibrary.openRequest("tus pdf"));
        assertNull(ReportLibrary.openRequest("¿qué dice tu último informe sobre la memoria?"));
        assertNull(ReportLibrary.openRequest("hazme un informe pdf de tu código"));
        assertNull(ReportLibrary.openRequest(null));
    }
}
