package salve.core.voice;

import static org.junit.Assert.*;
import org.junit.Test;

public class SpeechEndpointerTest {
    private static SpeechEndpointer.Event run(SpeechEndpointer e, double rms, int ms) {
        SpeechEndpointer.Event last = SpeechEndpointer.Event.NONE;
        for (int t = 0; t < ms; t += 20) {
            SpeechEndpointer.Event ev = e.feed(rms, 20);
            if (ev != SpeechEndpointer.Event.NONE) last = ev;
        }
        return last;
    }

    @Test public void detectsAPhraseOverCarNoise() {
        SpeechEndpointer e = new SpeechEndpointer();
        assertEquals(SpeechEndpointer.Event.NONE, run(e, 300, 300));     // ruido del coche
        assertEquals(SpeechEndpointer.Event.NONE, run(e, 320, 1000));    // sigue el ruido: no es voz
        assertEquals(SpeechEndpointer.Event.SPEECH_STARTED, run(e, 3000, 400));
        assertEquals(SpeechEndpointer.Event.NONE, run(e, 300, 300));     // pausa corta entre palabras
        assertEquals(SpeechEndpointer.Event.NONE, run(e, 2500, 600));
        assertEquals(SpeechEndpointer.Event.SPEECH_ENDED, run(e, 300, 1000));
        assertEquals("terminado: no hay más eventos", SpeechEndpointer.Event.NONE, e.feed(3000, 20));
    }

    @Test public void givesUpWithoutSpeechAndCutsMonologues() {
        SpeechEndpointer quiet = new SpeechEndpointer();
        assertEquals(SpeechEndpointer.Event.NO_SPEECH, run(quiet, 200, 9000));
        SpeechEndpointer longOne = new SpeechEndpointer();
        run(longOne, 200, 300);
        assertEquals(SpeechEndpointer.Event.TOO_LONG, run(longOne, 3000, 16000));
    }

    @Test public void rms() {
        assertEquals(1000.0, SpeechEndpointer.rms(new short[]{1000, -1000, 1000, -1000}, 4), 0.001);
        assertEquals(0.0, SpeechEndpointer.rms(new short[0], 0), 0.0);
    }
}
