package salve.avatar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public final class CorePoseLandmarksTest {
    private static final String ABC_SHA="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    private String document(String coordinates,String sha) {
        return "{\"version\":1,\"coordinateSystem\":\"pixels_top_left\",\"views\":{\"seated\":{"
                + "\"sourceSha256\":\""+sha+"\",\"width\":1024,\"height\":1536,"
                + "\"leftEye\":"+coordinates+",\"rightEye\":[500,200],\"mouth\":[450,250],"
                + "\"groundAnchor\":[512,1500],\"scaleToStanding\":0.5,\"mouthRotationDegrees\":12}}}";
    }
    @Test public void sourceBoundAnchorsControlScaleAndContact() {
        CorePoseLandmarks.Entry entry=CorePoseLandmarks.read(new StringReader(document("[400,200]",ABC_SHA)),"seated");
        assertEquals(ABC_SHA,entry.sourceSha256);assertEquals(.5f,entry.scaleToStanding,0);
        assertArrayEquals(new float[]{512,1500},entry.groundAnchor,0);
        assertEquals(12,entry.mouthRotationDegrees,0);assertEquals(100f/68f,entry.faceScale,.00001f);
    }
    @Test public void exactPngBytesMustMatchTheirLandmarks() throws Exception {
        CorePoseLandmarks.verifyHash(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)),ABC_SHA);
        try {CorePoseLandmarks.verifyHash(new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8)),ABC_SHA);fail();}
        catch(IOException expected) {assertTrue(expected.getMessage().contains("mismatch"));}
    }
    @Test public void outOfCanvasAnchorsAreRejected() {
        try {CorePoseLandmarks.read(new StringReader(document("[1024,200]",ABC_SHA)),"seated");fail();}
        catch(IllegalArgumentException expected) {assertTrue(expected.getMessage().contains("outside"));}
    }
    @Test public void coincidentEyesCannotInventAFaceScale() {
        try {CorePoseLandmarks.read(new StringReader(document("[500,200]",ABC_SHA)),"seated");fail();}
        catch(IllegalArgumentException expected) {assertTrue(expected.getMessage().contains("distance"));}
    }
    @Test public void historicalAnchorsAreBoundToHistoricalArtwork() {
        assertEquals("68d59233dce94ed5dbcbd7b9881a1b4cd87f2331e0592b320609f29b214e63b5",CorePoseLandmarks.original("seated").sourceSha256);
        try {CorePoseLandmarks.original("kneeling_variant");fail();}
        catch(IllegalArgumentException expected) {assertTrue(expected.getMessage().contains("no original"));}
    }
}
