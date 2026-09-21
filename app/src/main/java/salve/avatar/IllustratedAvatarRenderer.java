package salve.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import com.salve.app.R;

/** Textured 2D puppet. The user's original pixels remain the character, including its face. */
public final class IllustratedAvatarRenderer {
    private static final class Assets {
        final Bitmap original, hair;
        final AvatarRig rig;
        Assets(Context context) throws java.io.IOException {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            original = BitmapFactory.decodeResource(context.getResources(), R.drawable.salve_imagen, options);
            hair = BitmapFactory.decodeResource(context.getResources(), R.drawable.salve_hair_fill, options);
            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("avatar/rig.json"), StandardCharsets.UTF_8)) {
                rig = new AvatarRig(reader);
            }
            if (original == null || original.getWidth() != rig.width || original.getHeight() != rig.height)
                throw new IllegalArgumentException("The portrait and its rig do not match");
        }
    }
    // Main, room and overlay share immutable decoded textures, not three copies of the illustration.
    private static Assets sharedAssets;
    private final Assets assets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path mouth = new Path();
    private final Matrix face = new Matrix();
    private final float[] vertices, faceFrom = {450,310, 510,310, 450,350}, faceTo = new float[6];
    private final Path leftArm, rightArm, leftLeg, rightLeg, leftFill, rightFill;
    private final Rect mouthSample = new Rect(465, 315, 490, 323);
    private final RectF mouthCover = new RectF(459, 322, 496, 340);
    private final RectF textureBounds;

    public IllustratedAvatarRenderer(Context context) {
        try {
            if (sharedAssets == null) sharedAssets = new Assets(context.getApplicationContext());
            assets = sharedAssets;
        } catch (java.io.IOException | RuntimeException error) {
            Log.e("SalveAvatar", "No se pudo cargar la ilustración articulada", error);
            throw new IllegalStateException("Missing bundled avatar artwork", error);
        }
        AvatarRig r = assets.rig;
        vertices = new float[(r.columns + 1) * (r.rows + 1) * 2];
        leftArm = path(r.leftArm.polygon); rightArm = path(r.rightArm.polygon);
        leftLeg = path(r.leftLeg.polygon); rightLeg = path(r.rightLeg.polygon);
        leftFill = path(r.leftArm.fill); rightFill = path(r.rightArm.fill);
        textureBounds = new RectF(0, 0, r.width, r.height);
    }

    /** Draw at the source illustration's 1024x1536 coordinates. Host sets size and screen location. */
    public void draw(Canvas canvas, AvatarMotion.Snapshot pose, float stride, boolean asleep) {
        AvatarRig r = assets.rig;
        float armL = asleep ? 0 : AvatarRig.armAngle(pose.leftArm);
        float armR = asleep ? 0 : AvatarRig.armAngle(pose.rightArm);
        float legL = asleep ? 0 : AvatarRig.legAngle(stride);
        float legR = -legL;
        boolean moveL = Math.abs(armL) > .05f, moveR = Math.abs(armR) > .05f;
        boolean walking = Math.abs(legL) > .01f;
        float blink = asleep ? 1 : pose.blink;
        float tilt = asleep ? 0 : pose.headTilt;
        float yaw = asleep ? 0 : pose.headYaw;
        float pitch = asleep ? 0 : pose.headPitch;
        float gazeX = asleep ? 0 : pose.gazeX, gazeY = asleep ? 0 : pose.gazeY;
        if (!asleep && pose.expression == AvatarMotion.Expression.CURIOUS) {
            tilt += 1.5f; gazeX += .1f;
        } else if (!asleep && pose.expression == AvatarMotion.Expression.CONCERNED) {
            tilt -= 1.2f; gazeY += .2f; blink = Math.max(blink, .10f);
        }
        canvas.save();
        canvas.rotate(asleep ? 0 : AvatarRig.limit(pose.bodyTilt, -3, 3), r.bodyPivot[0], r.bodyPivot[1]);
        // Reveal only the small hidden hair regions behind a moving sleeve. Never replace the hair.
        if (moveL) fillHidden(canvas, leftFill);
        if (moveR) fillHidden(canvas, rightFill);
        if (walking) {
            part(canvas, leftLeg, r.leftLeg, legL);
            part(canvas, rightLeg, r.rightLeg, legR);
        }
        canvas.save();
        if (moveL) canvas.clipOutPath(leftArm);
        if (moveR) canvas.clipOutPath(rightArm);
        if (walking) { canvas.clipOutPath(leftLeg); canvas.clipOutPath(rightLeg); }
        // A neutral pose uses the source directly, with no raster reconstruction or shape redraw.
        if (tilt == 0 && yaw == 0 && pitch == 0 && blink == 0 && gazeX == 0 && gazeY == 0 && pose.breath == .5f) {
            canvas.drawBitmap(assets.original, null, textureBounds, paint);
        } else {
            int at = 0;
            for (int row = 0; row <= r.rows; row++) {
                for (int col = 0; col <= r.columns; col++) {
                    r.deform(col * r.width / (float)r.columns, row * r.height / (float)r.rows,
                            tilt, yaw, pitch, blink, gazeX, gazeY, pose.breath, vertices, at);
                    at += 2;
                }
            }
            canvas.drawBitmapMesh(assets.original, r.columns, r.rows, vertices, 0, null, 0, paint);
        }
        canvas.restore();
        if (moveL) part(canvas, leftArm, r.leftArm, armL);
        if (moveR) part(canvas, rightArm, r.rightArm, armR);
        boolean speakingMouth = !asleep && pose.speaking && pose.mouthOpen > .04f;
        boolean smile = !asleep && !speakingMouth && pose.expression == AvatarMotion.Expression.WARM;
        if (speakingMouth || smile) {
            for (int i = 0; i < faceFrom.length; i += 2)
                r.deform(faceFrom[i], faceFrom[i+1], tilt, yaw, pitch, blink, gazeX, gazeY, pose.breath, faceTo, i);
            face.setPolyToPoly(faceFrom, 0, faceTo, 0, 3);
            canvas.save(); canvas.concat(face);
            if (speakingMouth) drawMouth(canvas, AvatarRig.limit(pose.mouthOpen, 0, 1));
            else drawSmile(canvas);
            canvas.restore();
        }
        canvas.restore();
    }
    private void fillHidden(Canvas canvas, Path region) {
        if (assets.hair == null) return;
        canvas.save(); canvas.clipPath(region);
        canvas.drawBitmap(assets.hair, null, textureBounds, paint);
        canvas.restore();
    }
    private void part(Canvas canvas, Path shape, AvatarRig.Part part, float degrees) {
        canvas.save(); canvas.rotate(degrees, part.pivot[0], part.pivot[1]);
        canvas.clipPath(shape); canvas.drawBitmap(assets.original, null, textureBounds, paint); canvas.restore();
    }
    private void drawMouth(Canvas canvas, float open) {
        // Only replace the tiny closed-mouth line during speech; eyes, nose and face stay textured.
        canvas.drawBitmap(assets.original, mouthSample, mouthCover, paint);
        float x = assets.rig.mouth[0], y = assets.rig.mouth[1], half = 7 + open * 7, height = 2 + open * 13;
        mouth.reset(); mouth.moveTo(x - half, y - 1);
        mouth.quadTo(x, y - 2.5f, x + half, y - 1);
        mouth.quadTo(x + half * .7f, y + height, x, y + height);
        mouth.quadTo(x - half * .7f, y + height, x - half, y - 1); mouth.close();
        paint.setColor(0xFF754945); canvas.drawPath(mouth, paint);
        paint.setColor(Color.WHITE);
    }
    private void drawSmile(Canvas canvas) {
        canvas.drawBitmap(assets.original, mouthSample, mouthCover, paint);
        float x = assets.rig.mouth[0], y = assets.rig.mouth[1];
        mouth.reset(); mouth.moveTo(x - 13, y - 1); mouth.quadTo(x, y + 6, x + 13, y - 1);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFFBA8A7E); canvas.drawPath(mouth, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
    }
    private static Path path(float[] points) {
        Path result = new Path();
        if (points.length > 1) {
            result.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) result.lineTo(points[i], points[i+1]);
            result.close();
        }
        return result;
    }
}
