package salve.core.mind;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class CodeDiffTest {
    @Test public void identicalFilesHaveNoChanges() {
        assertTrue(CodeDiff.diff("a\nb\nc\n", "a\nb\nc\n", 2).isEmpty());
    }

    @Test public void showsWhatWasReplacedWithContextAndLineNumbers() {
        String before = "uno\ndos\ntres\ncuatro\ncinco\nseis\nsiete\n";
        String after = "uno\ndos\ntres\nCUATRO\ncinco\nseis\nsiete\nocho\n";
        List<CodeDiff.Hunk> hunks = CodeDiff.diff(before, after, 1);
        assertEquals(2, hunks.size());
        CodeDiff.Hunk first = hunks.get(0);
        assertEquals(3, first.oldStart);
        assertEquals(java.util.Arrays.asList(" tres", "-cuatro", "+CUATRO", " cinco"), first.lines);
        assertEquals(1, first.added());
        assertEquals(1, first.removed());
        assertTrue(hunks.get(1).lines.contains("+ocho"));
        String text = CodeDiff.render(hunks, 10000);
        assertTrue(text.contains("@@ antes línea 3, ahora línea 3"));
    }

    @Test public void handlesInsertionsAndDeletionsInTheMiddle() {
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            a.append("l").append(i).append('\n');
            if (i == 50) b.append("nueva\n");
            if (i != 120) b.append("l").append(i).append('\n');
        }
        List<CodeDiff.Hunk> hunks = CodeDiff.diff(a.toString(), b.toString(), 0);
        int added = 0, removed = 0;
        for (CodeDiff.Hunk h : hunks) { added += h.added(); removed += h.removed(); }
        assertEquals(1, added);
        assertEquals(1, removed);
        assertTrue(CodeDiff.render(hunks, 1000).contains("+nueva"));
        assertTrue(CodeDiff.render(hunks, 1000).contains("-l120"));
    }

    @Test public void aFullRewriteIsStillReportedWithoutExhaustingMemory() {
        StringBuilder a = new StringBuilder(), b = new StringBuilder();
        for (int i = 0; i < 3000; i++) { a.append("viejo").append(i).append('\n'); b.append("nuevo").append(i).append('\n'); }
        List<CodeDiff.Hunk> hunks = CodeDiff.diff(a.toString(), b.toString(), 2);
        int added = 0, removed = 0;
        for (CodeDiff.Hunk h : hunks) { added += h.added(); removed += h.removed(); }
        assertEquals(3000, added);
        assertEquals(3000, removed);
        assertTrue(CodeDiff.render(hunks, 500).endsWith("…(más cambios)\n"));
    }
}
