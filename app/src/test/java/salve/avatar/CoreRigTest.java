package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.StringReader;

public final class CoreRigTest {
    private AvatarRig rig() throws Exception {
        Path p=Paths.get("src/main/assets/avatar/core/rig.json");
        if(!Files.exists(p))p=Paths.get("app/src/main/assets/avatar/core/rig.json");
        return new AvatarRig(new StringReader(new String(Files.readAllBytes(p),StandardCharsets.UTF_8)));
    }
    @Test public void neutralCorePreservesEveryPixelCoordinate() throws Exception {
        AvatarRig r=rig();float[] points=new float[r.vertexCount()*2];r.frame(0,0,0,0,0,0,.5f,0,0,0,0,0).fillVertices(points);
        for(int y=0,n=0;y<=r.rows;y++)for(int x=0;x<=r.columns;x++,n+=2){assertEquals(x*r.width/(float)r.columns,points[n],.001f);assertEquals(y*r.height/(float)r.rows,points[n+1],.001f);}
    }
    @Test public void allSuppliedViewsAreUniqueAndUnknownViewsRejected(){
        java.util.Set<String> ids=new java.util.HashSet<>();
        for(CoreViewCatalog.Entry e:CoreViewCatalog.VIEWS){assertTrue(ids.add(e.id));assertSame(e,CoreViewCatalog.find(e.id));assertTrue(e.width>0&&e.height>0);}
        assertEquals(20,ids.size());assertEquals(1374,CoreViewCatalog.find("kneeling_variant").width);
        try{CoreViewCatalog.find("../../secret");fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void newCoreEnvelopeDoesNotFoldMeshTriangles() throws Exception {
        AvatarRig r=rig();float[] v=new float[r.vertexCount()*2];int[] triangles=r.triangleIndices();
        for(float head:new float[]{-7,0,7})for(float arms:new float[]{-25,0,25})for(float step:new float[]{-1,0,1})for(float blink:new float[]{0,1}){
            r.frame(head,0,0,blink,0,0,.5f,arms,-arms,step,0,0).fillVertices(v);
            for(float coordinate:v)assertTrue(Float.isFinite(coordinate));
            for(int i=0;i<triangles.length;i+=3){int a=triangles[i]*2,b=triangles[i+1]*2,c=triangles[i+2]*2;
                float area=(v[b]-v[a])*(v[c+1]-v[a+1])-(v[b+1]-v[a+1])*(v[c]-v[a]);
                assertTrue("Core mesh folded at "+i,area>0);
            }
        }
    }
}
