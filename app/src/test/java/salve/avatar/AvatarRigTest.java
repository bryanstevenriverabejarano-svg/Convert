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
        assertEquals(9, AvatarRig.armAngle(500), .001f);
        assertEquals(-9, AvatarRig.armAngle(-500), .001f);
        assertEquals(0, AvatarRig.armAngle(Float.NaN), 0);
        assertEquals(0, AvatarRig.legAngle(Float.POSITIVE_INFINITY), 0);
        assertEquals(2.8f, AvatarRig.legAngle(2), .001f);
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
    private AvatarRig.Frame frame(AvatarRig r,float armL,float armR,float step,float head,float blink) {
        return r.frame(head,0,0,blink,0,0,.5f,armL,armR,step,0,0);
    }
    @Test public void neutralFullSkeletonPreservesTextureCoordinates() throws Exception {
        AvatarRig r=rig();float[] v=new float[r.vertexCount()*2];frame(r,0,0,0,0,0).fillVertices(v);
        for(int row=0,n=0;row<=r.rows;row++)for(int col=0;col<=r.columns;col++,n+=2) {
            assertEquals(col*r.width/(float)r.columns,v[n],.001f);
            assertEquals(row*r.height/(float)r.rows,v[n+1],.001f);
        }
    }
    @Test public void elbowAndKneeShareTheirParentsJointPosition() throws Exception {
        AvatarRig r=rig();AvatarRig.Frame f=frame(r,25,-25,1,0,0);
        assertEquals(f.leftUpper.x(r.leftElbow[0],r.leftElbow[1]),f.leftForearm.x(r.leftElbow[0],r.leftElbow[1]),.001f);
        assertEquals(f.leftUpper.y(r.leftElbow[0],r.leftElbow[1]),f.leftForearm.y(r.leftElbow[0],r.leftElbow[1]),.001f);
        assertEquals(f.rightUpper.x(r.rightElbow[0],r.rightElbow[1]),f.rightForearm.x(r.rightElbow[0],r.rightElbow[1]),.001f);
        assertEquals(f.leftThigh.x(r.leftKnee[0],r.leftKnee[1]),f.leftCalf.x(r.leftKnee[0],r.leftKnee[1]),.001f);
        assertEquals(f.leftThigh.y(r.leftKnee[0],r.leftKnee[1]),f.leftCalf.y(r.leftKnee[0],r.leftKnee[1]),.001f);
    }
    @Test public void formerCutBoundariesAreNowContinuous() throws Exception {
        AvatarRig r=rig();AvatarRig.Frame f=frame(r,25,-25,1,0,0);float[] a=new float[2],b=new float[2];
        for(float[] p:new float[][]{{284,818},{670,833},{399,1033},{535,1033},{305,637},{635,639}}) {
            f.point(p[0]-.01f,p[1],a);f.point(p[0]+.01f,p[1],b);
            assertTrue(Math.hypot(a[0]-b[0],a[1]-b[1])<.08);
        }
    }
    @Test public void sharedMeshHasNoInternalOpenEdges() throws Exception {
        AvatarRig r=rig();int[] triangles=r.triangleIndices();java.util.Map<Long,Integer> edges=new java.util.HashMap<>();
        for(int i=0;i<triangles.length;i+=3)for(int e=0;e<3;e++) {
            int a=triangles[i+e],b=triangles[i+(e+1)%3];long key=((long)Math.min(a,b)<<32)|Math.max(a,b);
            edges.put(key,edges.getOrDefault(key,0)+1);
        }
        int boundary=0;
        for(int count:edges.values()){assertTrue(count==1||count==2);if(count==1)boundary++;}
        assertEquals(2*r.columns+2*r.rows,boundary);
    }
    @Test public void validatedGestureEnvelopeNeverInvertsATriangle() throws Exception {
        AvatarRig r=rig();int[] triangles=r.triangleIndices();float[] v=new float[r.vertexCount()*2];
        for(float a:new float[]{-25,0,25})for(float b:new float[]{-25,0,25})
            for(float step:new float[]{-1,0,1})for(float head:new float[]{-7,0,7})for(float blink:new float[]{0,1}) {
                r.frame(head,0,0,blink,0,0,.5f,a,b,step,1,1).fillVertices(v);
                for(int i=0;i<v.length;i++){assertTrue(Float.isFinite(v[i]));assertTrue(v[i]>=-200&&v[i]<=1736);}
                for(int i=0;i<triangles.length;i+=3) {
                    int p=triangles[i]*2,q=triangles[i+1]*2,t=triangles[i+2]*2;
                    float cross=(v[q]-v[p])*(v[t+1]-v[p+1])-(v[q+1]-v[p+1])*(v[t]-v[p]);
                    assertTrue("Folded triangle at "+i+" arms="+a+","+b+" step="+step+" area="+cross,cross>2);
                }
            }
    }
    @Test(expected=IllegalArgumentException.class) public void incompatibleAssetGeometryIsRejected() throws Exception {
        new AvatarRig(new StringReader(source().replace("\"width\": 1024", "\"width\": 2048")));
    }
}
