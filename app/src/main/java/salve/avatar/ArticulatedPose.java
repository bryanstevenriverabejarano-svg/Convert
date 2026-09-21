package salve.avatar;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.Strictness;
import java.io.StringReader;
import java.io.IOException;

/** A bounded frontal pose, not generated code or a claim to arbitrary 3D motion. */
public final class ArticulatedPose {
    public enum Joint {
        TORSO("Torso", -18, 18), HEAD("Cabeza", -22, 22),
        LEFT_SHOULDER("Hombro izquierdo", -20, 140), LEFT_ELBOW("Codo izquierdo", -110, 15),
        RIGHT_SHOULDER("Hombro derecho", -140, 20), RIGHT_ELBOW("Codo derecho", -15, 110),
        LEFT_HIP("Cadera izquierda", -32, 32), LEFT_KNEE("Rodilla izquierda", -65, 0),
        RIGHT_HIP("Cadera derecha", -32, 32), RIGHT_KNEE("Rodilla derecha", 0, 65);
        public final String label;
        public final int min, max;
        Joint(String label, int min, int max) { this.label=label; this.min=min; this.max=max; }
    }
    public enum Preset { REST, ARMS_UP, GREETING, ELBOWS_BENT, CROUCH }
    private final float[] angles;
    public ArticulatedPose() { angles=new float[Joint.values().length]; }
    private ArticulatedPose(float[] values) { angles=values; }
    public float angle(Joint joint) { return angles[joint.ordinal()]; }
    public ArticulatedPose with(Joint joint,float value) {
        if(joint==null || !Float.isFinite(value) || value<joint.min || value>joint.max)
            throw new IllegalArgumentException("Articulación fuera de sus límites.");
        float[] copy=angles.clone();copy[joint.ordinal()]=value;return new ArticulatedPose(copy);
    }
    public static ArticulatedPose preset(Preset id) {
        ArticulatedPose p=new ArticulatedPose();
        switch(id) {
            case ARMS_UP: return p.with(Joint.LEFT_SHOULDER,140).with(Joint.RIGHT_SHOULDER,-140);
            case GREETING: return p.with(Joint.RIGHT_SHOULDER,-120).with(Joint.RIGHT_ELBOW,-15).with(Joint.HEAD,-8);
            case ELBOWS_BENT: return p.with(Joint.LEFT_SHOULDER,30).with(Joint.LEFT_ELBOW,-95)
                    .with(Joint.RIGHT_SHOULDER,-30).with(Joint.RIGHT_ELBOW,95);
            case CROUCH: return p.with(Joint.LEFT_HIP,15).with(Joint.LEFT_KNEE,-30)
                    .with(Joint.RIGHT_HIP,-15).with(Joint.RIGHT_KNEE,30).with(Joint.LEFT_SHOULDER,25).with(Joint.RIGHT_SHOULDER,-25);
            default: return p;
        }
    }
    public static ArticulatedPose interpolate(ArticulatedPose a,ArticulatedPose b,float progress) {
        if(!Float.isFinite(progress))throw new IllegalArgumentException("Tiempo no finito.");
        float t=Math.max(0,Math.min(1,progress));t=t*t*(3-2*t);
        float[] values=new float[Joint.values().length];
        for(int i=0;i<values.length;i++)values[i]=a.angles[i]+(b.angles[i]-a.angles[i])*t;
        return new ArticulatedPose(values);
    }
    public String toJson() {
        StringBuilder out=new StringBuilder("{\"version\":1");
        for(Joint j:Joint.values())out.append(",\"").append(j.name()).append("\":").append(angle(j));
        return out.append('}').toString();
    }
    public static ArticulatedPose fromJson(String json) {
        if(json==null || json.length()>2048)throw new IllegalArgumentException("Postura demasiado grande.");
        ArticulatedPose p=new ArticulatedPose();boolean version=false;boolean[] seen=new boolean[Joint.values().length];
        try(JsonReader r=new JsonReader(new StringReader(json))) {
            r.setStrictness(Strictness.STRICT);r.beginObject();
            while(r.hasNext()) {
                String key=r.nextName();
                if(r.peek()!=JsonToken.NUMBER)throw new IllegalArgumentException("Valor de postura no numérico.");
                if(key.equals("version")) {
                    if(version || !r.nextString().equals("1"))throw new IllegalArgumentException("Versión de postura incorrecta.");
                    version=true;
                } else {
                    Joint joint=Joint.valueOf(key);
                    if(seen[joint.ordinal()])throw new IllegalArgumentException("Articulación duplicada.");
                    seen[joint.ordinal()]=true;p=p.with(joint,(float)r.nextDouble());
                }
            }
            r.endObject();
            if(!version || r.peek()!=JsonToken.END_DOCUMENT)throw new IllegalArgumentException("Postura incompleta.");
            for(boolean present:seen)if(!present)throw new IllegalArgumentException("Falta una articulación.");
            return p;
        } catch(IOException|IllegalStateException e) { throw new IllegalArgumentException("Postura no válida.",e); }
    }
}
