package salve.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;

/** Geometry for the approved illustration. No image generation, Android or executable model output. */
public final class AvatarRig {
    public final int width, height, columns, rows;
    public final String sourceSha256;
    public final float[] headPivot, bodyPivot, leftEye, rightEye, mouth;
    public final float[] leftShoulder, rightShoulder, leftElbow, rightElbow, leftPalm, rightPalm;
    public final float[] leftHip, rightHip, leftKnee, rightKnee;
    private final float[] rest, weights;

    public AvatarRig(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("version").getAsInt() != 2) throw new IllegalArgumentException("Unknown avatar rig");
        width = root.get("width").getAsInt(); height = root.get("height").getAsInt();
        columns = root.get("meshColumns").getAsInt(); rows = root.get("meshRows").getAsInt();
        if (width != 1024 || height != 1536 || columns < 8 || columns > 80 || rows < 8 || rows > 120)
            throw new IllegalArgumentException("Invalid avatar geometry");
        sourceSha256 = root.get("sourceSha256").getAsString();
        headPivot = points(root, "headPivot", 2); bodyPivot = points(root, "bodyPivot", 2);
        leftEye = points(root, "leftEye", 2); rightEye = points(root, "rightEye", 2);
        mouth = points(root, "mouth", 2);
        JsonObject joints = root.getAsJsonObject("joints");
        leftShoulder = points(joints,"leftShoulder",2); rightShoulder = points(joints,"rightShoulder",2);
        leftElbow = points(joints,"leftElbow",2); rightElbow = points(joints,"rightElbow",2);
        leftPalm = points(joints,"leftPalm",2); rightPalm = points(joints,"rightPalm",2);
        leftHip = points(joints,"leftHip",2); rightHip = points(joints,"rightHip",2);
        leftKnee = points(joints,"leftKnee",2); rightKnee = points(joints,"rightKnee",2);
        int count = (columns+1)*(rows+1);
        rest = new float[count*2]; weights = new float[count*6];
        for (int row=0,n=0; row<=rows; row++) for (int col=0; col<=columns; col++,n++) {
            float x=col*width/(float)columns,y=row*height/(float)rows;
            rest[n*2]=x;rest[n*2+1]=y; skinWeights(x,y,weights,n*6);
        }
    }
    private static float[] points(JsonObject value, String name, int count) {
        JsonArray array = value.getAsJsonArray(name);
        if (array == null || (count > 0 && array.size() != count)
                || (count < 0 && (array.size() < 6 || array.size() > 160 || array.size() % 2 != 0)))
            throw new IllegalArgumentException("Invalid rig points: " + name);
        float[] result = new float[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsFloat();
            if (!Float.isFinite(result[i]) || result[i] < 0 || result[i] > 1536)
                throw new IllegalArgumentException("Invalid rig coordinate");
        }
        return result;
    }
    /** Small 2D deformation only: a yaw cue is a lateral head offset, never an invented 3D turn. */
    public void deform(float x, float y, float tilt, float yaw, float pitch, float blink,
                       float gazeX, float gazeY, float breath, float[] out, int index) {
        float px = x, py = y;
        float closed = limit(blink, 0, 1);
        for (int eye = 0; eye < 2; eye++) {
            float[] center = eye == 0 ? leftEye : rightEye;
            float dx = x - center[0], dy = y - center[1];
            if (Math.abs(dx) > 60 || Math.abs(dy) > 75) continue;
            float edge = smooth(52, 32, Math.abs(dx));
            // Compress the existing eye texture into its lash line; keep the supplied face artwork.
            py -= dy * closed * .96f * edge * (float)Math.exp(-Math.pow(dy / 43f, 4));
            float iris = (float)Math.exp(-Math.pow(dx / 22f, 4) - Math.pow(dy / 29f, 4));
            px += limit(gazeX, -1, 1) * 3f * iris * (1 - closed);
            py += limit(gazeY, -1, 1) * 2.2f * iris * (1 - closed);
        }
        float head = smooth(490, 325, y);
        float angle = (float)Math.toRadians(limit(tilt, -7, 7) * head);
        if (angle != 0) {
            float dx = px - headPivot[0], dy = py - headPivot[1];
            px = headPivot[0] + dx * (float)Math.cos(angle) - dy * (float)Math.sin(angle);
            py = headPivot[1] + dx * (float)Math.sin(angle) + dy * (float)Math.cos(angle);
        }
        px += limit(yaw, -14, 14) * .55f * head;
        py += limit(pitch, -10, 10) * .7f * head;
        // One continuous mesh avoids seams at the neck and behind the front locks.
        float chest = smooth(920, 420, y) * smooth(220, 440, y);
        float breathing = (limit(breath, 0, 1) - .5f) * 2f;
        py -= breathing * 2f * chest;
        float hair = smooth(350, 510, y) * smooth(870, 710, y) * limit(Math.abs(x - 480) / 350f, 0, 1);
        px += breathing * 1.6f * hair;
        out[index] = px; out[index + 1] = py;
    }
    /** Scale the gesture envelope, rather than clipping away its oscillation. */
    public static float armAngle(float degrees) { return limit(degrees, -25, 25) * .36f; }
    public static float legAngle(float stride) { return limit(stride, -1, 1) * 2.8f; }

    /** Parent transforms are composed at rest joints; forearms and calves cannot detach at a joint. */
    static final class Transform {
        final float a,b,c,d,tx,ty;
        Transform(float a,float b,float c,float d,float tx,float ty) { this.a=a;this.b=b;this.c=c;this.d=d;this.tx=tx;this.ty=ty; }
        static Transform rotate(float degrees,float[] pivot) {
            float rad=(float)Math.toRadians(degrees),c=(float)Math.cos(rad),s=(float)Math.sin(rad);
            return new Transform(c,s,-s,c,pivot[0]-c*pivot[0]+s*pivot[1],pivot[1]-s*pivot[0]-c*pivot[1]);
        }
        Transform child(Transform t) { return new Transform(a*t.a+c*t.b,b*t.a+d*t.b,a*t.c+c*t.d,b*t.c+d*t.d,a*t.tx+c*t.ty+tx,b*t.tx+d*t.ty+ty); }
        float x(float x,float y) { return a*x+c*y+tx; }
        float y(float x,float y) { return b*x+d*y+ty; }
    }
    public final class Frame {
        private final float tilt,yaw,pitch,blink,gazeX,gazeY,breath;
        final Transform leftUpper,leftForearm,rightUpper,rightForearm,leftThigh,leftCalf,rightThigh,rightCalf;
        private Frame(float tilt,float yaw,float pitch,float blink,float gazeX,float gazeY,float breath,
                      float armL,float armR,float stride,float kneeL,float kneeR) {
            this.tilt=tilt;this.yaw=yaw;this.pitch=pitch;this.blink=blink;this.gazeX=gazeX;this.gazeY=gazeY;this.breath=breath;
            float la=armAngle(armL),ra=armAngle(armR),step=limit(stride,-1,1);
            // Inward rotation has less room: the flat arm shares pixels with the dress and hair.
            if(la<0)la*=.4f;if(ra>0)ra*=.4f;
            leftUpper=Transform.rotate(la,leftShoulder);rightUpper=Transform.rotate(ra,rightShoulder);
            leftForearm=leftUpper.child(Transform.rotate(la*.22f,leftElbow));
            rightForearm=rightUpper.child(Transform.rotate(ra*.22f,rightElbow));
            leftThigh=Transform.rotate(legAngle(step),leftHip);rightThigh=Transform.rotate(-legAngle(step),rightHip);
            leftCalf=leftThigh.child(Transform.rotate(-Math.max(0,step)*2.2f-limit(kneeL,0,1)*2,leftKnee));
            rightCalf=rightThigh.child(Transform.rotate(Math.max(0,-step)*2.2f+limit(kneeR,0,1)*2,rightKnee));
        }
        public void fillVertices(float[] output) {
            if(output.length!=rest.length)throw new IllegalArgumentException("Wrong shared vertex buffer");
            for(int i=0,n=0;i<rest.length;i+=2,n+=6) {
                deform(rest[i],rest[i+1],tilt,yaw,pitch,blink,gazeX,gazeY,breath,output,i);
                skin(rest[i],rest[i+1],weights,n,output,i);
            }
        }
        /** Arbitrary probes use exactly the same field as shared mesh vertices. */
        public void point(float x,float y,float[] output) {
            float[] w=new float[6];skinWeights(x,y,w,0);
            deform(x,y,tilt,yaw,pitch,blink,gazeX,gazeY,breath,output,0);skin(x,y,w,0,output,0);
        }
        private void skin(float x,float y,float[] w,int at,float[] output,int index) {
            add(x,y,leftUpper,leftForearm,w[at],w[at+4],output,index);
            add(x,y,rightUpper,rightForearm,w[at+1],w[at+4],output,index);
            add(x,y,leftThigh,leftCalf,w[at+2],w[at+5],output,index);
            add(x,y,rightThigh,rightCalf,w[at+3],w[at+5],output,index);
        }
        private void add(float x,float y,Transform parent,Transform child,float weight,float blend,float[] out,int at) {
            if(weight<=0)return;
            out[at]+=weight*((parent.x(x,y)-x)*(1-blend)+(child.x(x,y)-x)*blend);
            out[at+1]+=weight*((parent.y(x,y)-y)*(1-blend)+(child.y(x,y)-y)*blend);
        }
    }
    public Frame frame(float tilt,float yaw,float pitch,float blink,float gazeX,float gazeY,float breath,
                       float armL,float armR,float stride,float kneeL,float kneeR) {
        return new Frame(tilt,yaw,pitch,blink,gazeX,gazeY,breath,armL,armR,stride,kneeL,kneeR);
    }
    public int vertexCount() { return rest.length/2; }
    /** Indices share corners: no duplicate limb vertices, cut masks, holes or disconnected islands. */
    public int[] triangleIndices() {
        int[] result=new int[columns*rows*6];
        for(int row=0,i=0;row<rows;row++)for(int col=0;col<columns;col++) {
            int a=row*(columns+1)+col,b=a+1,c=a+columns+1,d=c+1;
            result[i++]=a;result[i++]=b;result[i++]=c;result[i++]=b;result[i++]=d;result[i++]=c;
        }
        return result;
    }
    private void skinWeights(float x,float y,float[] output,int at) {
        output[at]=armWeight(x,y,true);output[at+1]=armWeight(x,y,false);
        float legs=smooth(995,1125,y),side=smooth(570,400,x);
        output[at+2]=legs*side;output[at+3]=legs*(1-side);
        output[at+4]=smooth(590,750,y);output[at+5]=smooth(1145,1290,y);
    }
    private float armWeight(float x,float y,boolean left) {
        float[] shoulder=left?leftShoulder:rightShoulder,elbow=left?leftElbow:rightElbow,palm=left?leftPalm:rightPalm;
        float distance=Math.min(segmentDistance(x,y,shoulder,elbow),segmentDistance(x,y,elbow,palm));
        float side=left?smooth(375,295,x):smooth(555,635,x);
        return smooth(245,75,distance)*side*smooth(400,535,y)*smooth(1080,930,y);
    }
    private static float segmentDistance(float x,float y,float[] a,float[] b) {
        float dx=b[0]-a[0],dy=b[1]-a[1],t=limit(((x-a[0])*dx+(y-a[1])*dy)/(dx*dx+dy*dy),0,1);
        return (float)Math.hypot(x-a[0]-dx*t,y-a[1]-dy*t);
    }
    public static float limit(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : Math.max(min, Math.min(max, 0));
    }
    private static float smooth(float start, float end, float value) {
        float t = limit((value - start) / (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
