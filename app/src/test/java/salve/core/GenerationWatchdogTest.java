package salve.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class GenerationWatchdogTest {
    @Test public void aLongStoryThatKeepsWritingIsNotCut() {
        // 120 s escribiendo, último fragmento hace 1 s: el antiguo límite fijo de 90 s lo habría cortado.
        assertEquals(GenerationWatchdog.Verdict.RUNNING, GenerationWatchdog.check(120_000, 0, 119_000, false, true));
    }
    @Test public void stalledOrSilentEnginesAreStopped() {
        assertEquals(GenerationWatchdog.Verdict.STALLED, GenerationWatchdog.check(50_000, 0, 25_000, false, true));
        assertEquals(GenerationWatchdog.Verdict.NO_FIRST_TOKEN, GenerationWatchdog.check(61_000, 0, 0, false, false));
        assertEquals(GenerationWatchdog.Verdict.TOO_LONG, GenerationWatchdog.check(301_000, 0, 300_500, false, true));
        assertEquals(GenerationWatchdog.Verdict.TOO_LONG, GenerationWatchdog.check(61_000, 0, 60_900, true, false));
    }
    @Test public void onlyFreeTextKeepsWhatWasWritten() {
        assertTrue(GenerationWatchdog.keepPartial(false, 800));
        assertFalse(GenerationWatchdog.keepPartial(true, 800));
        assertFalse(GenerationWatchdog.keepPartial(false, 50));
    }
}
