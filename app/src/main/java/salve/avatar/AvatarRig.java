package salve.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;

/** Geometry for the approved illustration. No image generation, Android or executable model output. */
public final class AvatarRig {
    public static final class Part {
        public final float[] pivot, polygon, fill;
        private Part(JsonObject value) {
            pivot = points(value, "pivot", 2);
            polygon = points(value, "polygon", -1);
            fill = value.has("fill") ? points(value, "fill", -1) : new float[0];
        }
    }
    public final int width, height, columns, rows;
    public final String sourceSha256;
    public final float[] headPivot, bodyPivot, leftEye, rightEye, mouth;
    public final Part leftArm, rightArm, leftLeg, rightLeg;

    public AvatarRig(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unknown avatar rig");
        width = root.get("width").getAsInt(); height = root.get("height").getAsInt();
        columns = root.get("meshColumns").getAsInt(); rows = root.get("meshRows").getAsInt();
        if (width != 1024 || height != 1536 || columns < 8 || columns > 80 || rows < 8 || rows > 120)
            throw new IllegalArgumentException("Invalid avatar geometry");
        sourceSha256 = root.get("sourceSha256").getAsString();
        headPivot = points(root, "headPivot", 2); bodyPivot = points(root, "bodyPivot", 2);
        leftEye = points(root, "leftEye", 2); rightEye = points(root, "rightEye", 2);
        mouth = points(root, "mouth", 2);
        leftArm = new Part(root.getAsJsonObject("leftArm")); rightArm = new Part(root.getAsJsonObject("rightArm"));
        leftLeg = new Part(root.getAsJsonObject("leftLeg")); rightLeg = new Part(root.getAsJsonObject("rightLeg"));
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
    public static float armAngle(float degrees) { return limit(degrees, -20, 20); }
    public static float legAngle(float stride) { return limit(stride, -1, 1) * 3.4f; }
    public static float limit(float value, float min, float max) {
        return Float.isFinite(value) ? Math.max(min, Math.min(max, value)) : Math.max(min, Math.min(max, 0));
    }
    private static float smooth(float start, float end, float value) {
        float t = limit((value - start) / (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
