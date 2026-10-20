package salve.core.web;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class SourceListTest {
    @Test public void listsTitleHostAndUrlOncePerPage() {
        List<String[]> sources = Arrays.asList(
                new String[]{"Vitoria-Gasteiz - Wikipedia", "https://es.wikipedia.org/wiki/Vitoria"},
                new String[]{"Repetida", "https://es.wikipedia.org/wiki/Vitoria"},
                new String[]{"", "https://www.elcorreo.com/alava/noticia"});
        String note = SourceList.note(sources);
        assertTrue(note.contains("Fuentes que he leído"));
        assertTrue(note.contains("• Vitoria-Gasteiz - Wikipedia (es.wikipedia.org)\n  https://es.wikipedia.org/wiki/Vitoria"));
        assertTrue(note.contains("• elcorreo.com\n  https://www.elcorreo.com/alava/noticia"));
        assertFalse(note.contains("Repetida"));
    }

    @Test public void nothingReadMeansNoNote() {
        assertEquals("", SourceList.note(new ArrayList<>()));
        assertEquals("", SourceList.note(Arrays.asList(new String[][]{{"x", "javascript:alert(1)"}})));
    }

    @Test public void atMostFiveSources() {
        List<String[]> many = new ArrayList<>();
        for (int i = 0; i < 9; i++) many.add(new String[]{"P" + i, "https://ejemplo.org/" + i});
        String note = SourceList.note(many);
        assertTrue(note.contains("/4"));
        assertFalse(note.contains("/5"));
    }
}
