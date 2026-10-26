package salve.avatar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Malla del cuerpo astral generada por scripts/build_astral_body.py a partir de base.png.
 * Calcula, sin Android, dónde va cada vértice: el pelo cuelga y ondea, la cabeza se inclina,
 * y brazos, torso y piernas permanecen fijos sobre la ilustración aprobada.
 */
public final class AstralBodyRig {
    public final int width, height, cols, rows;
    public final int eyeLeft, eyeTop, eyeRight, eyeBottom;
    public final float neckX, neckY, heartX, heartY, feetX, feetY;
    /** Altura donde empieza a colgar el pelo y largo de la caída (px de base.png). */
    public final float hairTop, hairSpan;
    /** Distancia del corazón al punto más lejano de la ilustración: hasta ahí llega la luz que recorre las líneas. */
    public final float reach;
    /** Recuadro de la boca (px de base.png) y nº de bocas dibujadas; 0 si esta versión no tiene. */
    public final int mouthLeft, mouthTop, mouthRight, mouthBottom, mouthFrames;
    private final float[] hair, head, rest;

    private AstralBodyRig(JsonObject spec) {
        JsonArray size = spec.getAsJsonArray("size");
        width = size.get(0).getAsInt(); height = size.get(1).getAsInt();
        JsonArray eye = spec.getAsJsonArray("eyeBox");
        eyeLeft = eye.get(0).getAsInt(); eyeTop = eye.get(1).getAsInt();
        eyeRight = eye.get(2).getAsInt(); eyeBottom = eye.get(3).getAsInt();
        JsonArray neck = spec.getAsJsonArray("neck"), heart = spec.getAsJsonArray("heart"), feet = spec.getAsJsonArray("feet");
        neckX = neck.get(0).getAsFloat(); neckY = neck.get(1).getAsFloat();
        heartX = heart.get(0).getAsFloat(); heartY = heart.get(1).getAsFloat();
        feetX = feet.get(0).getAsFloat(); feetY = feet.get(1).getAsFloat();
        // La v1 no los traía: sus valores eran fijos en el código.
        hairTop = spec.has("hairTop") ? spec.get("hairTop").getAsFloat() : 300f;
        hairSpan = spec.has("hairSpan") ? spec.get("hairSpan").getAsFloat() : 700f;
        if (!(hairSpan > 0)) throw new IllegalArgumentException("Caída del pelo inválida");
        if (spec.has("mouthBox") && spec.has("mouthFrames")) {
            JsonArray mouth = spec.getAsJsonArray("mouthBox");
            mouthLeft = mouth.get(0).getAsInt(); mouthTop = mouth.get(1).getAsInt();
            mouthRight = mouth.get(2).getAsInt(); mouthBottom = mouth.get(3).getAsInt();
            mouthFrames = spec.getAsJsonArray("mouthFrames").size();
            if (mouthRight <= mouthLeft || mouthBottom <= mouthTop || mouthFrames < 1)
                throw new IllegalArgumentException("Boca inválida");
        } else { mouthLeft = mouthTop = mouthRight = mouthBottom = mouthFrames = 0; }
        JsonObject mesh = spec.getAsJsonObject("mesh");
        cols = mesh.get("cols").getAsInt(); rows = mesh.get("rows").getAsInt();
        int count = (cols + 1) * (rows + 1);
        hair = floats(mesh.getAsJsonArray("hair"), count);
        head = floats(mesh.getAsJsonArray("head"), count);
        rest = new float[count * 2];
        for (int r = 0; r <= rows; r++) for (int c = 0; c <= cols; c++) {
            int i = r * (cols + 1) + c;
            rest[2 * i] = width * (float) c / cols;
            rest[2 * i + 1] = height * (float) r / rows;
        }
        if (width <= 0 || height <= 0 || cols < 2 || rows < 2 || eyeRight <= eyeLeft || eyeBottom <= eyeTop)
            throw new IllegalArgumentException("Rig astral inválido");
        float far = 0;
        for (float[] corner : new float[][]{{0, 0}, {width, 0}, {0, height}, {width, height}})
            far = Math.max(far, (float) Math.hypot(corner[0] - heartX, corner[1] - heartY));
        reach = far;
    }

    public static AstralBodyRig parse(String json) {
        return new AstralBodyRig(JsonParser.parseString(json).getAsJsonObject());
    }

    public int vertexCount() { return (cols + 1) * (rows + 1); }

    /** Vértices deformados en píxeles de base.png (formato de Canvas.drawBitmapMesh). */
    public float[] deform(AstralBody.Snapshot body, float[] out) {
        if (out == null || out.length != rest.length) out = new float[rest.length];
        float tilt = (float) Math.toRadians(body == null ? 0 : body.headTilt);
        float cos = (float) Math.cos(tilt), sin = (float) Math.sin(tilt);
        float swing = body == null ? 0 : body.hairSwing, wave = body == null ? 0 : body.hairWave;
        float hx = body == null ? 0 : body.headShiftX, hy = body == null ? 0 : body.headShiftY;
        for (int i = 0, n = vertexCount(); i < n; i++) {
            float x = rest[2 * i], y = rest[2 * i + 1];
            float h = head[i];
            // Cabeza: giro rígido alrededor del cuello, mezclado por peso hacia los hombros.
            float rx = neckX + (x - neckX) * cos - (y - neckY) * sin + hx;
            float ry = neckY + (x - neckX) * sin + (y - neckY) * cos + hy;
            float dx = (rx - x) * h, dy = (ry - y) * h;
            float w = hair[i];
            if (w > 0) {
                float hang = Math.max(0, (y - hairTop) / hairSpan);
                hang = hang * (float) Math.sqrt(hang);
                float side = x < neckX ? 0f : 1.7f;
                dx += w * (swing * hang + 2.6f * hang * (float) Math.sin(wave + y * .011f + side));
                // Al balancearse, la punta sube un poco: el mechón mantiene su longitud.
                dy -= w * Math.abs(swing) * hang * .08f;
            }
            out[2 * i] = x + dx;
            out[2 * i + 1] = y + dy;
        }
        return out;
    }

    /** Peso del pelo en el vértice (para pruebas y diagnóstico). */
    public float hairWeight(int col, int row) { return hair[row * (cols + 1) + col]; }
    public float headWeight(int col, int row) { return head[row * (cols + 1) + col]; }

    private static float[] floats(JsonArray values, int count) {
        if (values == null || values.size() != count) throw new IllegalArgumentException("Pesos de malla incompletos");
        float[] out = new float[count];
        for (int i = 0; i < count; i++) {
            float v = values.get(i).getAsFloat();
            if (!Float.isFinite(v) || v < 0 || v > 1) throw new IllegalArgumentException("Peso fuera de rango");
            out[i] = v;
        }
        return out;
    }
}
