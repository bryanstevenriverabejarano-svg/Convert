package salve.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pixel anchors belong to the exact PNG bytes, never to a pose name alone. */
final class CorePoseLandmarks {
    static final class Entry {
        final String sourceSha256;
        final float[] leftEye, rightEye, mouth, groundAnchor;
        final float scaleToStanding, mouthRotationDegrees, faceScale;
        private Entry(String sha, float[] left, float[] right, float[] mouth, float[] ground,
                      float scale, float rotation, float faceScale) {
            sourceSha256=sha; leftEye=left; rightEye=right; this.mouth=mouth; groundAnchor=ground;
            scaleToStanding=scale; mouthRotationDegrees=rotation; this.faceScale=faceScale;
        }
    }
    static Entry read(Reader reader, String id) {
        JsonObject root=JsonParser.parseReader(reader).getAsJsonObject();
        if(root.get("version").getAsInt()!=1 || !"pixels_top_left".equals(root.get("coordinateSystem").getAsString()))
            throw new IllegalArgumentException("Unknown pose landmarks");
        JsonObject value=root.getAsJsonObject("views").getAsJsonObject(id);
        if(value==null)throw new IllegalArgumentException("Missing landmarks for "+id);
        CoreViewCatalog.Entry view=CoreViewCatalog.find(id);
        if(value.get("width").getAsInt()!=view.width || value.get("height").getAsInt()!=view.height)
            throw new IllegalArgumentException("Landmarks dimensions do not match catalog");
        String sha=value.get("sourceSha256").getAsString();
        if(!sha.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Invalid image hash");
        float[] left=point(value,"leftEye",view), right=point(value,"rightEye",view);
        float distance=(float)Math.hypot(right[0]-left[0],right[1]-left[1]);
        if(distance<2)throw new IllegalArgumentException("Invalid eye distance");
        float factor=finite(value,"scaleToStanding",.1f,5f);
        float rotation=value.has("mouthRotationDegrees")?finite(value,"mouthRotationDegrees",-180,180):0;
        float faceScale=value.has("faceScale")?finite(value,"faceScale",.1f,5f):Math.max(.1f,Math.min(5f,distance/68f));
        return new Entry(sha,left,right,point(value,"mouth",view),point(value,"groundAnchor",view),factor,rotation,faceScale);
    }
    private static float finite(JsonObject value,String key,float low,float high) {
        float result=value.get(key).getAsFloat();
        if(!Float.isFinite(result)||result<low||result>high)throw new IllegalArgumentException("Invalid "+key);
        return result;
    }
    private static float[] point(JsonObject value,String key,CoreViewCatalog.Entry view) {
        JsonArray values=value.getAsJsonArray(key);
        if(values==null||values.size()!=2)throw new IllegalArgumentException("Invalid "+key);
        float x=values.get(0).getAsFloat(),y=values.get(1).getAsFloat();
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<0||x>=view.width||y<0||y>=view.height)
            throw new IllegalArgumentException("Anchor outside image");
        return new float[]{x,y};
    }
    /** Only the unchanged original assets may use these historical anchors. */
    static Entry original(String id) {
        if("seated".equals(id))return new Entry("68d59233dce94ed5dbcbd7b9881a1b4cd87f2331e0592b320609f29b214e63b5",new float[]{467,246},new float[]{564,210},new float[]{540,289},new float[]{512,1536},.55f,-12,1);
        if("kneeling".equals(id))return new Entry("1ad4fa15517f17c0ff88fc228dbe535ff06869cbbd02010ad63ebf10278f70be",new float[]{470,220},new float[]{561,240},new float[]{506,296},new float[]{512,1536},.55f,7,1);
        if("crouched".equals(id))return new Entry("5966ded2cc3ca82f619b128ea94d24074ae81432d1456f441e7ee7eefdc29ee5",new float[]{303,294},new float[]{377,261},new float[]{345,349},new float[]{512,1536},.60f,0,1);
        throw new IllegalArgumentException("This view has no original stage anchors");
    }
    static void verifyHash(InputStream source,String expected) throws IOException {
        try {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[8192];int length;
            while((length=source.read(buffer))!=-1)digest.update(buffer,0,length);
            StringBuilder actual=new StringBuilder();
            for(byte value:digest.digest())actual.append(String.format(java.util.Locale.ROOT,"%02x",value&255));
            if(!actual.toString().equals(expected))throw new IOException("PNG and landmarks hash mismatch");
        } catch(NoSuchAlgorithmException impossible) { throw new IOException("SHA-256 unavailable",impossible); }
    }
    private CorePoseLandmarks() {}
}
