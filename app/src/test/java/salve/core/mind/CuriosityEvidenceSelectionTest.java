package salve.core.mind;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

/** Regression from real autonomous curiosity notifications citing unrelated sources. */
public class CuriosityEvidenceSelectionTest {
    @Test public void doesNotArchiveGenericIntroFromLooselyMatchingPage() {
        String q = "¿Qué relación tienen geometría y funcionalidad en la arquitectura?";
        String junk = "La geometría se utiliza para describir formas y posiciones en proyectos visuales. "
                + "Los informes corporativos analizan la funcionalidad financiera en empresas de sectores diversos.";
        assertNull("Separate keywords in separate sentences do not answer the question",
                CuriosityCycle.summarize(Collections.singletonList(junk),
                        Collections.singletonList("https://example.org/corporativo"), q));
        String good = "La geometría de los espacios afecta a su funcionalidad: la posición de paredes, recorridos "
                + "y puertas determina cómo se utiliza cada habitación de un edificio.";
        CuriosityCycle.Finding finding = CuriosityCycle.summarize(
                Arrays.asList(junk, good),
                Arrays.asList("https://bad.example", "https://architecture.example/space"), q);
        assertNotNull(finding);
        assertEquals("https://architecture.example/space", finding.url);
        assertTrue(finding.summary.contains("geometría"));
        assertTrue(finding.summary.contains("funcionalidad"));
        assertFalse(finding.summary.contains("corporativos"));
    }
    @Test public void rejectPageWithUnrelatedIntroductionBeforeActuallyRelevantEvidence() {
        String q = "¿Por qué los pulpos tienen tres corazones?";
        String intro = "Los pulpos son animales marinos con un cuerpo flexible y ocho brazos muy conocidos.";
        String finding = "Los pulpos tienen tres corazones porque dos bombean sangre hacia las branquias "
                + "y el tercero envía sangre al resto del cuerpo.";
        CuriosityCycle.Finding result = CuriosityCycle.summarize(
                Collections.singletonList(intro + " " + finding),
                Collections.singletonList("https://science.example/pulpos"), q);
        assertNotNull(result);
        assertTrue(result.summary.startsWith("Los pulpos tienen tres corazones porque"));
    }
}
