package salve.core.memory;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class MemorySearchQueryTest {
    @Test public void boundsTopicsAndRemovesConversationalNoise() {
        MemorySearchQuery q = MemorySearchQuery.parse("¿Qué te dije de mi móvil Samsung proyecto nube extra?");
        assertEquals(4, q.terms().size()); assertEquals("movil", q.terms().get(0));
        assertTrue(q.expandedExpression().contains("\"telefono\""));
        assertFalse(q.exactExpression().contains("dije"));
    }
    @Test public void ftsOperatorsAreQuotedAsLiteralWords() {
        MemorySearchQuery q = MemorySearchQuery.parse("Orion NOT NEAR* (AND) \" :*");
        assertEquals("\"orion\" \"not\" \"near\" \"and\"", q.exactExpression());
        assertFalse(q.exactExpression().contains("*"));
    }
    @Test public void blankGreetingsAndHugeTokensDoNotMakeFullDatabaseQueries() {
        assertTrue(MemorySearchQuery.parse("hola gracias").isEmpty());
        assertTrue(MemorySearchQuery.parse(null).isEmpty());
        assertTrue(MemorySearchQuery.parse(String.join("", Collections.nCopies(5000, "x"))).isEmpty());
    }
    @Test public void normalizedDuplicatesAndShortNamesArePreservedOnce() {
        assertEquals(java.util.Arrays.asList("ana", "orion"), MemorySearchQuery.parse("Ana orión ORION").terms());
        assertEquals(MemorySearchQuery.parse("Orión").terms(), MemorySearchQuery.parse("Orio\u0301n").terms());
    }
    @Test public void excerptsPreserveUnicodeWithoutSplittingSurrogatePairs() {
        String text = String.join("", Collections.nCopies(400, "🙂")) + " telescopio "
                + String.join("", Collections.nCopies(400, "🙂"));
        String excerpt = MemoryEvidenceExcerpt.extract(text, MemorySearchQuery.parse("telescopio"), 620);
        assertTrue(excerpt.contains("telescopio")); assertTrue(excerpt.length() <= 620);
        for (int i = 0; i < excerpt.length(); i++) {
            if (Character.isHighSurrogate(excerpt.charAt(i))) assertTrue(Character.isLowSurrogate(excerpt.charAt(++i)));
            else assertFalse(Character.isLowSurrogate(excerpt.charAt(i)));
        }
    }
    @Test public void provenanceSeparatesStorageFromAuthorshipAndRejectsTagPrefixes() {
        salve.data.db.RecuerdoEntity r = new salve.data.db.RecuerdoEntity();
        r.etiquetas = "[\"pcloud\",\"fuentes_externas\",\"hecho_usuario\"]";
        assertEquals("investigacion_publica", MemoryProvenance.kind(r));
        assertEquals("pcloud_restaurado", MemoryProvenance.storage(r));
        assertFalse(MemoryProvenance.isPersonalCandidate(r));
        r.etiquetas = "[\"fuentes_externas_extra\"]"; assertTrue(MemoryProvenance.isPersonalCandidate(r));
        r.etiquetas = "broken"; assertEquals("registro_sin_autoria_confirmada", MemoryProvenance.kind(r));
    }
}
