package salve.avatar;

import static org.junit.Assert.*;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import org.junit.Test;

public final class AvatarRigTest {
    private Path mainFile(String suffix) {
        Path path = Paths.get("src/main/" + suffix);
        return Files.exists(path) ? path : Paths.get("app/src/main/" + suffix);
    }
    private String source() throws Exception {
        return new String(Files.readAllBytes(mainFile("assets/avatar/rig.json")), StandardCharsets.UTF_8);
    }
    private AvatarRig rig() throws Exception { return new AvatarRig(new StringReader(source())); }
    private float[] point(AvatarRig rig, float x, float y, float tilt, float blink, float gaze) {
        float[] result = new float[2];
        rig.deform(x, y, tilt, 0, 0, blink, gaze, 0, .5f, result, 0);
        return result;
    }
    @Test public void neutralMeshPreservesEveryOriginalVertex() throws Exception {
        AvatarRig r = rig();
        for (int row = 0; row <= r.rows; row++) for (int col = 0; col <= r.columns; col++) {
            float x = col * r.width / (float)r.columns, y = row * r.height / (float)r.rows;
            assertArrayEquals(new float[]{x,y}, point(r,x,y,0,0,0), .0001f);
        }
    }
    @Test public void approvedOriginalTextureHasNotBeenReplaced() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(mainFile("res/drawable/salve_imagen.png")));
        StringBuilder sha = new StringBuilder();
        for (byte b : digest) sha.append(String.format("%02x", b & 255));
        assertEquals("32346faf7219f417eb0983262ae7ac20a2e80063781a0ea32b843d3eb03fb0ec", sha.toString());
        assertEquals(sha.toString(), rig().sourceSha256);
    }
    @Test public void armsAndWalkStayWithinTheFlatArtEnvelope() {
        assertEquals(20, AvatarRig.armAngle(500), 0);
        assertEquals(-20, AvatarRig.armAngle(-500), 0);
        assertEquals(0, AvatarRig.armAngle(Float.NaN), 0);
        assertEquals(0, AvatarRig.legAngle(Float.POSITIVE_INFINITY), 0);
        assertEquals(3.4f, AvatarRig.legAngle(2), .001f);
    }
    @Test public void closingEyesCompressesTextureWithoutCrossingTheLashLine() throws Exception {
        AvatarRig r = rig();
        for (float[] eye : new float[][]{r.leftEye,r.rightEye}) {
            float[] upper = point(r, eye[0], eye[1]-17, 0, 1, 0);
            float[] lower = point(r, eye[0], eye[1]+17, 0, 1, 0);
            assertTrue(upper[1] < eye[1]); assertTrue(lower[1] > eye[1]);
            assertTrue(lower[1]-upper[1] < 4);
        }
    }
    @Test public void gazeMovesIrisButDoesNotMoveTheChin() throws Exception {
        AvatarRig r = rig();
        assertTrue(point(r,r.leftEye[0],r.leftEye[1],0,0,1)[0] > r.leftEye[0]);
        assertArrayEquals(new float[]{477,360},point(r,477,360,0,0,1),.001f);
    }
    @Test public void headTiltBlendsOutBeforeTheShouldersBecomeTheTorso() throws Exception {
        AvatarRig r = rig();
        assertArrayEquals(new float[]{480,550},point(r,480,550,7,0,0),.001f);
        assertTrue(Math.abs(point(r,480,200,7,0,0)[0]-480) > 10);
    }
    @Test public void extremeAndInvalidInputsCannotProduceInvalidMeshCoordinates() throws Exception {
        AvatarRig r = rig(); float[] output = new float[2];
        for (float tilt : new float[]{-100,100,Float.NaN}) {
            for (int y = 0; y <= r.height; y += 16) for (int x = 0; x <= r.width; x += 16) {
                r.deform(x,y,tilt,99,99,99,99,99,Float.NaN,output,0);
                assertTrue(Float.isFinite(output[0])); assertTrue(Float.isFinite(output[1]));
                assertTrue(output[0] >= -200 && output[0] <= r.width+200);
                assertTrue(output[1] >= -200 && output[1] <= r.height+200);
            }
        }
    }
    @Test public void polygonPointsStayInsideTheTexture() throws Exception {
        AvatarRig r = rig();
        for (AvatarRig.Part part : new AvatarRig.Part[]{r.leftArm,r.rightArm,r.leftLeg,r.rightLeg}) {
            for (int i = 0; i < part.polygon.length; i += 2) {
                assertTrue(part.polygon[i] <= r.width); assertTrue(part.polygon[i+1] <= r.height);
            }
        }
    }
    @Test(expected=IllegalArgumentException.class) public void incompatibleAssetGeometryIsRejected() throws Exception {
        new AvatarRig(new StringReader(source().replace("\"width\": 1024", "\"width\": 2048")));
    }
}
