package salve.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ModelMemoryPolicyTest {
    private ModelMemoryPolicy.Snapshot ram(long available, boolean low) {
        return new ModelMemoryPolicy.Snapshot(available, 12L * 1024 * ModelMemoryPolicy.MIB, 0, low);
    }
    @Test public void eightBRequiresAvailableRamNotJustAnExpensivePhone() {
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(8L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.LIGHT, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
    }
    @Test public void uncensoredGemmaLoadsOnTheS24UltraEvenWithFewGigabytesAvailable() {
        long gib = 1024 * ModelMemoryPolicy.MIB;
        String id = LocalModelPolicy.UNCENSORED;
        // S24 Ultra de Bryan: 10,82 GiB totales, ~2,7–3,1 GiB «disponibles» por las apps en caché.
        ModelMemoryPolicy.Snapshot s24 = new ModelMemoryPolicy.Snapshot(2900L * ModelMemoryPolicy.MIB, 11080L * ModelMemoryPolicy.MIB, 0, false);
        assertFalse("la regla estricta nunca lo dejaría", ModelMemoryPolicy.canLoad(id, s24, false));
        assertEquals("regla de móvil grande: contexto normal", GgufContextPolicy.STANDARD, ModelMemoryPolicy.selectContext(id, s24, false));
        assertTrue("puede empezar a descargarse", ModelMemoryPolicy.canPrepare(id, s24, false));
        ModelMemoryPolicy.Snapshot tight = new ModelMemoryPolicy.Snapshot(
                ModelMemoryPolicy.required(id, true, 0, GgufContextPolicy.COMPACT), 11080L * ModelMemoryPolicy.MIB, 0, true);
        assertEquals(GgufContextPolicy.COMPACT, ModelMemoryPolicy.selectContext(id, tight, false));
        ModelMemoryPolicy.Snapshot small = new ModelMemoryPolicy.Snapshot(3 * gib, 8 * gib, 0, false);
        assertEquals(0, ModelMemoryPolicy.selectContext(id, small, false));
        assertFalse(ModelMemoryPolicy.canPrepare(id, small, false));
        assertEquals(0, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, s24, false));
    }

    @Test public void retiredQwenFilesAreRecognised() {
        assertTrue(LocalModelPolicy.isRetiredQwen("Qwen3.5-9B-Uncensored-HauhauCS-Aggressive-Q4_K_M-0a41c68.gguf"));
        assertTrue(LocalModelPolicy.isRetiredQwen("Qwen3.5-4B-Uncensored-HauhauCS-Aggressive-Q4_K_M-c09cdbc.gguf.part"));
        assertFalse(LocalModelPolicy.isRetiredQwen(LocalModelPolicy.UNCENSORED_FILE));
        assertTrue(LocalModelPolicy.isRetiredNormalE4b("gemma-4-E4B-it-2eee7ac.litertlm"));
        assertFalse("el E2B normal es el respaldo de emergencia", LocalModelPolicy.isRetiredNormalE4b("gemma-4-E2B-it-6e5c4f1.litertlm"));
        assertEquals(LocalModelPolicy.UNCENSORED_E2B, LocalModelPolicy.idForPath("/x/" + LocalModelPolicy.UNCENSORED_E2B_FILE));
        // Cada modelo sin censura tiene sus ojos; la visión no es un modelo que se pueda activar.
        assertEquals(LocalModelPolicy.UNCENSORED_VISION, LocalModelPolicy.visionFor(LocalModelPolicy.UNCENSORED));
        assertEquals(LocalModelPolicy.UNCENSORED_E2B_VISION, LocalModelPolicy.visionFor(LocalModelPolicy.UNCENSORED_E2B));
        assertNull(LocalModelPolicy.visionFor(LocalModelPolicy.FALLBACK));
        assertTrue(LocalModelPolicy.isVisionCompanion(LocalModelPolicy.UNCENSORED_VISION));
        assertFalse(LocalModelPolicy.isUncensored(LocalModelPolicy.UNCENSORED_VISION));
        assertNull("el mmproj no se confunde con un modelo", LocalModelPolicy.idForPath("/x/" + LocalModelPolicy.UNCENSORED_VISION_FILE));
        assertTrue(ModelMemoryPolicy.required(LocalModelPolicy.UNCENSORED_E2B, false, 0) < ModelMemoryPolicy.required(LocalModelPolicy.UNCENSORED, false, 0));
        assertEquals(LocalModelPolicy.UNCENSORED, LocalModelPolicy.idForPath("/x/" + LocalModelPolicy.UNCENSORED_FILE));
    }

    @Test public void boundariesIncludeOverheadAndSystemReserve() {
        long need = ModelMemoryPolicy.required(LocalModelPolicy.PRIMARY, false, 0);
        assertTrue(need > ModelMemoryPolicy.LARGE_WEIGHTS);
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need - 1, false), false));
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need, false), false));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(need, true), false));
    }
    @Test public void loadedWeightsAndAllocatedContextAreNotChargedTwice() {
        long twoGiB = 2L * 1024 * ModelMemoryPolicy.MIB;
        assertTrue(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(twoGiB, false), true));
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, ram(twoGiB, false), false));
        assertFalse(ModelMemoryPolicy.underPressure(ram(1024 * ModelMemoryPolicy.MIB, false)));
        assertTrue(ModelMemoryPolicy.underPressure(ram(512 * ModelMemoryPolicy.MIB, false)));
    }
    @Test public void unknownAndLowMemoryNeverAdmit8B() {
        assertFalse(ModelMemoryPolicy.canLoad(LocalModelPolicy.PRIMARY, new ModelMemoryPolicy.Snapshot(0, 0, 0, false), false));
        assertTrue(ModelMemoryPolicy.underPressure(ram(9L * 1024 * ModelMemoryPolicy.MIB, true)));
        assertTrue(ModelMemoryPolicy.underPressure(new ModelMemoryPolicy.Snapshot(100, 50, 0, false)));
    }
    @Test public void androidThresholdOverridesTheMinimumReserve() {
        long threshold = 2L * 1024 * ModelMemoryPolicy.MIB;
        assertTrue(ModelMemoryPolicy.underPressure(new ModelMemoryPolicy.Snapshot(threshold - 1, threshold * 6, threshold, false)));
        assertEquals(ModelMemoryPolicy.LARGE_WEIGHTS + 1024 * ModelMemoryPolicy.MIB + threshold,
                ModelMemoryPolicy.required(LocalModelPolicy.PRIMARY, false, threshold));
    }
    @Test public void compactContextIsChosenOnlyInTheNarrowSupportedMemoryBand() {
        for (String id : new String[]{LocalModelPolicy.PRIMARY, LocalModelPolicy.LIGHT}) {
            long compact = ModelMemoryPolicy.required(id, false, 0, 2048);
            long standard = ModelMemoryPolicy.required(id, false, 0, 4096);
            assertTrue(compact < standard);
            assertEquals(0, ModelMemoryPolicy.selectContext(id, ram(compact - 1, false), false));
            assertEquals(2048, ModelMemoryPolicy.selectContext(id, ram(compact, false), false));
            assertEquals(2048, ModelMemoryPolicy.selectContext(id, ram(standard - 1, false), false));
            assertEquals(4096, ModelMemoryPolicy.selectContext(id, ram(standard, false), false));
            assertEquals(0, ModelMemoryPolicy.selectContext(id, ram(standard, true), false));
        }
    }
    @Test public void shrinkingContextDoesNotPretendEightBWeightsFitFourGiB() {
        assertEquals(0, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertEquals(4096, ModelMemoryPolicy.selectContext(LocalModelPolicy.LIGHT, ram(4L * 1024 * ModelMemoryPolicy.MIB, false), false));
        assertEquals(2048, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, ram(1600 * ModelMemoryPolicy.MIB, false), true));
    }
    @Test public void preparationDoesNotChargeAResidentEngineTwiceButActivationStillChecksRam() {
        ModelMemoryPolicy.Snapshot current = ram(3L * 1024 * ModelMemoryPolicy.MIB, false);
        assertTrue(ModelMemoryPolicy.canPrepare(LocalModelPolicy.PRIMARY, current, true));
        assertFalse(ModelMemoryPolicy.canPrepare(LocalModelPolicy.PRIMARY, current, false));
        assertEquals(0, ModelMemoryPolicy.selectContext(LocalModelPolicy.PRIMARY, current, false));
        assertEquals(ModelMemoryPolicy.required(LocalModelPolicy.FALLBACK, false, 0, 4096),
                ModelMemoryPolicy.required(LocalModelPolicy.FALLBACK, false, 0, 2048));
    }
}
