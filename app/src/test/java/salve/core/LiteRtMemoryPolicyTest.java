package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

/** Caso real: Galaxy con 10,82 GiB, 3,09 GiB libres y Gemma 4 E2B ya cargado para texto (15/10/2026). */
public class LiteRtMemoryPolicyTest {
    private static final long GIB = 1024L * ModelMemoryPolicy.MIB;
    private final ModelMemoryPolicy.Snapshot ram = new ModelMemoryPolicy.Snapshot(
            (long) (3.09 * GIB), (long) (10.82 * GIB), 768 * ModelMemoryPolicy.MIB, false);

    @Test public void residentWeightsAreNotChargedTwiceWhenAddingVision() {
        String id = LocalModelPolicy.FALLBACK;
        assertTrue("Antes exigía 3,66 GiB contando otra vez los pesos ya cargados",
                ModelMemoryPolicy.requiredLiteRt(id, false, ram.threshold, true, 4096) > ram.available);
        assertNotEquals(0, ModelMemoryPolicy.selectLiteRtContext(id, ram, true, true));
    }

    @Test public void longContextOnlyWhenItFits() {
        String id = LocalModelPolicy.FALLBACK;
        ModelMemoryPolicy.Snapshot plenty = new ModelMemoryPolicy.Snapshot(6L * GIB, 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(ModelMemoryPolicy.LITERT_LONG, ModelMemoryPolicy.selectLiteRtContext(id, plenty, false, true));
        ModelMemoryPolicy.Snapshot tight = new ModelMemoryPolicy.Snapshot((long) (3.8 * GIB), 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(ModelMemoryPolicy.LITERT_STANDARD, ModelMemoryPolicy.selectLiteRtContext(id, tight, false, false));
        ModelMemoryPolicy.Snapshot low = new ModelMemoryPolicy.Snapshot(2L * GIB, 12L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals(0, ModelMemoryPolicy.selectLiteRtContext(id, low, false, false));
    }

    /** Caso real del 20/10/2026: Galaxy de 10,82 GiB con 2,71 GiB «disponibles» (apps en caché). */
    @Test public void roomyPhoneLoadsGemmaWithoutCleaningRamByHand() {
        String id = LocalModelPolicy.FALLBACK;
        ModelMemoryPolicy.Snapshot cached = new ModelMemoryPolicy.Snapshot(
                (long) (2.71 * GIB), (long) (10.82 * GIB), 768 * ModelMemoryPolicy.MIB, false);
        assertEquals("la medida estricta sigue diciendo que no", 0, ModelMemoryPolicy.selectLiteRtContext(id, cached, false, false));
        assertEquals(ModelMemoryPolicy.LITERT_STANDARD, ModelMemoryPolicy.admitLiteRt(id, cached, false, false));
        assertTrue("descargar no gasta RAM", ModelMemoryPolicy.canPrepare(id, cached, false));
        assertFalse("la visión de antemano sigue siendo estricta",
                ModelMemoryPolicy.selectLiteRtContext(id, cached, false, true) != 0);
    }

    @Test public void smallOrStressedPhonesStillRefuse() {
        String id = LocalModelPolicy.FALLBACK;
        ModelMemoryPolicy.Snapshot small = new ModelMemoryPolicy.Snapshot(
                (long) (2.71 * GIB), 4L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertEquals("4 GiB en total: no hay caché que liberar", 0, ModelMemoryPolicy.admitLiteRt(id, small, false, false));
        ModelMemoryPolicy.Snapshot low = new ModelMemoryPolicy.Snapshot(
                (long) (2.71 * GIB), (long) (10.82 * GIB), 768 * ModelMemoryPolicy.MIB, true);
        assertEquals("Android ya avisa de memoria baja", 0, ModelMemoryPolicy.admitLiteRt(id, low, false, false));
        ModelMemoryPolicy.Snapshot almostNothing = new ModelMemoryPolicy.Snapshot(
                900 * ModelMemoryPolicy.MIB, (long) (10.82 * GIB), 768 * ModelMemoryPolicy.MIB, false);
        assertEquals("ni el motor cabe", 0, ModelMemoryPolicy.admitLiteRt(id, almostNothing, false, false));
        ModelMemoryPolicy.Snapshot tiny = new ModelMemoryPolicy.Snapshot(2L * GIB, 3L * GIB, 768 * ModelMemoryPolicy.MIB, false);
        assertFalse("un móvil de 3 GiB no descarga Gemma", ModelMemoryPolicy.canPrepare(id, tiny, false));
        assertFalse("los Dolphin siguen como antes", ModelMemoryPolicy.canPrepare(LocalModelPolicy.PRIMARY,
                new ModelMemoryPolicy.Snapshot(3L * GIB, 16L * GIB, 768 * ModelMemoryPolicy.MIB, false), false));
    }
}
