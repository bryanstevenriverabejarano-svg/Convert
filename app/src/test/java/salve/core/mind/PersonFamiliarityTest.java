package salve.core.mind;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class PersonFamiliarityTest {
    private static final long DAY = 86_400_000L;

    static final class Mem implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    @Test public void whoYouTalkToEveryDayWeighsMoreThanSomeoneNamedOnce() {
        AtomicLong now = new AtomicLong(1_800_000_000_000L);
        PersonFamiliarity p = new PersonFamiliarity(new Mem(), now::get);
        for (int i = 0; i < 40; i++) p.saw("Bryan Steven Rivera Bejarano");
        p.sawIn("Hoy he visto a Vivina", Arrays.asList("Vivina Rivera", "Juan"));
        assertTrue(p.weight("Bryan") > p.weight("Vivina"));
        assertEquals(0, p.weight("Juan"), 0);
        assertEquals(40, p.mentions("bryan"));
        assertEquals(Arrays.asList("Bryan"), p.closest(3));
    }

    @Test public void someoneNotSeenForMonthsFades() {
        AtomicLong now = new AtomicLong(1_800_000_000_000L);
        PersonFamiliarity p = new PersonFamiliarity(new Mem(), now::get);
        for (int i = 0; i < 30; i++) p.saw("Juan");
        double fresh = p.weight("Juan");
        now.addAndGet(120 * DAY);
        assertTrue(p.weight("Juan") < fresh);
        assertTrue("sigue sin olvidarlo del todo", p.weight("Juan") > 0);
    }

    @Test public void namesCountOnlyAsWholeWords() {
        PersonFamiliarity p = new PersonFamiliarity(new Mem(), () -> 1L);
        assertEquals(0, p.sawIn("Ana Bryanna estuvo", Arrays.asList("Bryan")));
        assertEquals(1, p.sawIn("Ayer con Brýan", Arrays.asList("Bryan")));
    }

    @Test public void survivesRestartAndMergesTheCloudCopy() {
        Mem store = new Mem();
        PersonFamiliarity a = new PersonFamiliarity(store, () -> 5L);
        a.saw("Bryan");
        a.saw("Bryan");
        assertEquals(2, new PersonFamiliarity(store, () -> 6L).mentions("Bryan"));

        PersonFamiliarity reinstalled = new PersonFamiliarity(new Mem(), () -> 9L);
        reinstalled.saw("Bryan");
        assertTrue(reinstalled.mergeFrom(a.json()));
        assertEquals(2, reinstalled.mentions("Bryan"));
        assertFalse("idempotente", reinstalled.mergeFrom(a.json()));
        assertFalse(reinstalled.mergeFrom("{roto"));
    }
}
