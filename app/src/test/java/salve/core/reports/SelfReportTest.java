package salve.core.reports;

import static org.junit.Assert.*;

import org.junit.Test;

public class SelfReportTest {
    public static final String BRYAN = "Salve, quiero que examines tu propio código fuente real. Primero identifica qué "
            + "herramientas tienes para acceder a tu repositorio. Después, revisa cómo funcionan tu memoria, tu LLM y "
            + "tu sistema de autonomía. Busca posibles mejoras sin duplicar funciones existentes. Indícame los archivos "
            + "y métodos que has inspeccionado, qué problemas has comprobado y qué cambios propones. Finalmente, redacta "
            + "un informe PDF y guárdalo en tu biblioteca de informes. Si no puedes acceder a algún archivo, dímelo "
            + "claramente y no inventes resultados.";

    @Test public void bryansRequestAsksForAReport() {
        assertTrue(SelfReport.requested(BRYAN));
        assertTrue(SelfReport.requested("hazme un informe de lo que has aprendido"));
        assertTrue(SelfReport.requested("guárdalo en pdf"));
    }

    @Test public void mentioningAReportIsNotAskingForOne() {
        assertFalse(SelfReport.requested("¿qué decía el informe del tiempo?"));
        assertFalse(SelfReport.requested("hola Salve"));
        assertFalse(SelfReport.requested(null));
    }

    @Test public void composedReportSaysItIsUnverified() {
        String body = SelfReport.compose("Mi memoria", "Propuesta: unificar índices.");
        assertTrue(body.startsWith("INFORME DE SALVE\nMi memoria"));
        assertTrue(body.contains("sin implementar ni verificar"));
        assertTrue(body.endsWith("Propuesta: unificar índices."));
        assertTrue(SelfReport.compose("  ", "x").contains("Informe de Salve"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyReportIsRejected() { SelfReport.compose("t", "   "); }
}
