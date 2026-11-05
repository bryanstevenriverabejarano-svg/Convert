package salve.core.voice;

import static org.junit.Assert.*;
import org.junit.Test;

public class NeuralVoiceTest {
    @Test public void floatSamplesBecomeLittleEndianPcm16AndClip() {
        byte[] pcm = NeuralVoice.toPcm16(new float[]{0f, 1f, -1f, 2f, 0.5f});
        assertEquals(10, pcm.length);
        assertEquals(0, pcm[0]); assertEquals(0, pcm[1]);
        assertEquals((byte) 0xff, pcm[2]); assertEquals((byte) 0x7f, pcm[3]);   // 32767
        assertEquals((byte) 0x01, pcm[4]); assertEquals((byte) 0x80, pcm[5]);   // -32767
        assertEquals((byte) 0xff, pcm[6]); assertEquals((byte) 0x7f, pcm[7]);   // recortado a 1
        assertEquals(16383, (pcm[8] & 0xff) | (pcm[9] << 8));
    }

    @Test public void theFemaleSpeakerOfSharvardIsUsed() {
        assertEquals(1, NeuralVoice.FEMALE_SPEAKER);
        assertTrue(NeuralVoice.MODEL_FILE.endsWith(".onnx"));
    }
}
