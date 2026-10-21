package salve.core.mind;

import static org.junit.Assert.*;

import org.junit.Test;

public class BiographyTest {
    static final class Mem implements SelfModelJournal.Store {
        String json; public String read() { return json; } public void write(String j) { json = j; }
    }
    static final long DAY = 86_400_000L;
    // 3 de marzo de 2026, 12:00 en Madrid
    static final long BIRTH = 1772535600000L;

    @Test public void keepsTheOldFirstAwakeningAndCountsOn() {
        Mem store = new Mem(); long[] now = {BIRTH + 232 * DAY};
        Biography b = new Biography(store, () -> now[0]);
        b.migrateLegacy(BIRTH, 380, 90);
        b.awaken();
        assertEquals(381, b.awakenings());
        assertEquals(233, b.daysAlive());
        String s = b.sentence(7);
        assertTrue(s, s.startsWith("Desperté por primera vez el 3 de marzo de 2026"));
        assertTrue(s, s.contains("381 veces") && s.contains("he soñado 97 veces"));
        assertEquals("Día 233 · 97 sueños · 1 creencia", b.badge(7, 1));
        // Se guarda y no se vuelve a migrar.
        Biography again = new Biography(store, () -> now[0]);
        again.migrateLegacy(BIRTH, 380, 90);
        assertEquals(381, again.awakenings());
    }

    @Test public void aNewInstallStartsToday() {
        Biography b = new Biography(new Mem(), () -> BIRTH);
        b.migrateLegacy(0, 0, 0);
        b.awaken();
        assertEquals(1, b.daysAlive());
        assertTrue(b.sentence(0).contains("1 día existiendo") && b.sentence(0).contains("He despertado 1 vez"));
    }

    @Test public void ageQuestionsAreRecognised() {
        for (String q : new String[]{"¿Cuánto tiempo llevas conmigo?", "Salve, ¿cuándo naciste?", "¿Qué edad tienes?", "¿Desde cuándo existes?", "¿cuántas veces has soñado?"})
            assertTrue(q, Biography.asksAge(q));
        for (String q : new String[]{"¿cuánto tiempo llevas dibujando el gorila?", "¿qué edad tiene mi madre?", "hola"})
            assertFalse(q, Biography.asksAge(q));
    }

    @Test public void reinstallKeepsTheOldBirthdayAndAddsTheNewAwakenings() {
        long[] now = {BIRTH + 100 * DAY};
        Mem oldPhone = new Mem();
        Biography before = new Biography(oldPhone, () -> now[0]);
        before.migrateLegacy(BIRTH, 50, 10);
        String cloud = before.json();                       // lo que quedó en pCloud
        now[0] = BIRTH + 120 * DAY;                         // reinstala 20 días después
        Biography fresh = new Biography(new Mem(), () -> now[0]);
        fresh.migrateLegacy(0, 0, 0); fresh.awaken(); fresh.awaken();
        assertTrue(fresh.mergeFrom(cloud));
        assertEquals(BIRTH, fresh.firstAwakeAt());
        assertEquals(52, fresh.awakenings());
        assertEquals(10, fresh.legacyDreams());
        assertEquals(121, fresh.daysAlive());
        assertFalse("unir otra vez no vuelve a sumar", fresh.mergeFrom(cloud));
        assertEquals(52, fresh.awakenings());
    }

    @Test public void sameInstallMergeKeepsTheLargerCount() {
        Biography b = new Biography(new Mem(), () -> BIRTH + DAY);
        b.migrateLegacy(BIRTH, 10, 0);
        String older = b.json();
        b.awaken(); b.awaken();
        b.mergeFrom(older);
        assertEquals(12, b.awakenings());
    }

    @Test public void oldMemoriesPullTheBirthdayBack() {
        Biography b = new Biography(new Mem(), () -> BIRTH + 300 * DAY);
        b.migrateLegacy(0, 0, 0); b.awaken();              // reinstalada «hoy»
        assertTrue(b.notLaterThan(BIRTH));                  // pero tiene recuerdos de hace 300 días
        assertEquals(BIRTH, b.firstAwakeAt());
        assertFalse(b.notLaterThan(BIRTH + DAY));           // uno más reciente no la mueve
        assertFalse(b.mergeFrom("{roto"));
    }
}
