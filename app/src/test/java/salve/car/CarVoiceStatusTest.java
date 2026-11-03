package salve.car;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Test;

/** La pantalla del coche sigue el estado de la voz y solo se redibuja cuando cambia. */
public class CarVoiceStatusTest {
    @After public void reset() { CarVoiceStatus.set(null); }

    @Test public void withoutConversationOffersToTalk() {
        CarVoiceStatus.set(null);
        assertFalse(CarVoiceStatus.active());
        assertEquals("Lista para hablar contigo", CarVoiceStatus.line());
        assertTrue(CarVoiceStatus.hint().contains("Hablar con Salve"));
    }

    @Test public void showsTheVoiceStateAndHowToEnd() {
        CarVoiceStatus.set("  Te escucho…  ");
        assertTrue(CarVoiceStatus.active());
        assertEquals("Te escucho…", CarVoiceStatus.line());
        assertTrue(CarVoiceStatus.hint().contains("adiós, Salve"));
        CarVoiceStatus.set("   ");
        assertFalse("texto vacío = sin conversación", CarVoiceStatus.active());
    }

    @Test public void notifiesOnlyRealChangesAndStopsAfterRemoval() {
        AtomicInteger calls = new AtomicInteger();
        CarVoiceStatus.Listener l = calls::incrementAndGet;
        CarVoiceStatus.addListener(l);
        CarVoiceStatus.addListener(l); // sin duplicados
        CarVoiceStatus.set("Pensando…");
        CarVoiceStatus.set("Pensando…"); // igual: no redibuja
        CarVoiceStatus.set("Hablando…");
        assertEquals(2, calls.get());
        CarVoiceStatus.removeListener(l);
        CarVoiceStatus.set(null);
        assertEquals(2, calls.get());
    }

    @Test public void aBrokenScreenDoesNotBlockTheOthers() {
        AtomicInteger calls = new AtomicInteger();
        CarVoiceStatus.Listener broken = () -> { throw new IllegalStateException("pantalla cerrada"); };
        CarVoiceStatus.Listener ok = calls::incrementAndGet;
        CarVoiceStatus.addListener(broken);
        CarVoiceStatus.addListener(ok);
        try {
            CarVoiceStatus.set("Te escucho…");
            assertEquals(1, calls.get());
        } finally {
            CarVoiceStatus.removeListener(broken);
            CarVoiceStatus.removeListener(ok);
        }
    }
}
