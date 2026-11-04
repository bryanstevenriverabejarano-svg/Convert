package salve.devices;

import static org.junit.Assert.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.Test;

public class AudioClipRecorderTest {
    @Test public void wavHeaderIsWhatDecodersExpect() {
        short[] pcm = {0, 1000, -1000, 32767};
        byte[] wav = AudioClipRecorder.wav(pcm, 3, 16000);
        assertEquals(44 + 6, wav.length);
        assertEquals("RIFF", new String(wav, 0, 4));
        assertEquals("WAVE", new String(wav, 8, 4));
        ByteBuffer b = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(36 + 6, b.getInt(4));
        assertEquals(1, b.getShort(20));      // PCM
        assertEquals(1, b.getShort(22));      // mono
        assertEquals(16000, b.getInt(24));
        assertEquals(32000, b.getInt(28));
        assertEquals(16, b.getShort(34));
        assertEquals(6, b.getInt(40));
        assertEquals(1000, b.getShort(46));
        assertEquals(-1000, b.getShort(48));
    }
}
