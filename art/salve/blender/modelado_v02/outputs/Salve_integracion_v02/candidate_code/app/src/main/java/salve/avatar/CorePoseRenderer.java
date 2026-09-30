package salve.avatar;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Catalogued 2D pose artwork with image-specific facial and contact anchors. */
final class CorePoseRenderer {
    private final Context context;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private ExecutorService worker;
    private volatile long generation;
    private String requested, loaded;
    private Bitmap bitmap;
    private CorePoseLandmarks.Entry landmarks;
    private boolean attached;

    CorePoseRenderer(Context context, Runnable changed) {
        this.context = context.getApplicationContext(); this.changed = changed;
    }
    void attach() { attached = true; worker = Executors.newSingleThreadExecutor(); }
    void detach() {
        attached = false; generation++;
        if (worker != null) worker.shutdownNow();
        worker = null; requested = loaded = null; landmarks = null;
        if (bitmap != null) bitmap.recycle(); bitmap = null;
    }
    void select(String id) {
        if (!attached || java.util.Objects.equals(requested, id)) return;
        requested = id; final long task = ++generation;
        loaded = null; landmarks = null;
        if (bitmap != null) bitmap.recycle(); bitmap = null;
        if (id == null) return;
        // One queue slot matters: superseded requests never decode or publish a stale pose.
        worker.execute(() -> {
            if (task != generation) return;
            Bitmap decoded = null;
            try {
                CoreViewCatalog.Entry entry=CoreViewCatalog.find(id);
                CorePoseLandmarks.Entry anchors;
                try(java.io.Reader reader=new java.io.InputStreamReader(context.getAssets().open("avatar/core/landmarks.json"),java.nio.charset.StandardCharsets.UTF_8)) {
                    anchors=CorePoseLandmarks.read(reader,id);
                } catch(java.io.FileNotFoundException originalAssets) { anchors=CorePoseLandmarks.original(id); }
                if(anchors.sourceSha256!=null)try(InputStream source=context.getAssets().open(entry.path)) {
                    CorePoseLandmarks.verifyHash(source,anchors.sourceSha256);
                }
                final CorePoseLandmarks.Entry readyAnchors=anchors;
                try (InputStream in = context.getAssets().open(entry.path)) {
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
                decoded = BitmapFactory.decodeStream(in, null, options);
                if (decoded == null || decoded.getWidth() != entry.width || decoded.getHeight() != entry.height)
                    throw new IllegalArgumentException("Unexpected core pose dimensions");
                Bitmap ready = decoded;
                main.post(() -> {
                    if (!attached || task != generation) { ready.recycle(); return; }
                    bitmap = ready; landmarks = readyAnchors; loaded = id; changed.run();
                });
                }
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError error) {
                if (decoded != null) decoded.recycle();
                main.post(() -> { if (attached && task == generation) changed.run(); });
            }
        });
    }
    /** Source images fill different canvases; scale by face size, not the pose's bounding box. */
    boolean draw(Canvas canvas, String id, AvatarMotion.Snapshot motion, float standingHeight) {
        CorePoseLandmarks.Entry anchors=landmarks;
        if (bitmap == null || anchors == null || !id.equals(loaded)) return false;
        float scale = standingHeight / 1536f * anchors.scaleToStanding;
        canvas.save(); canvas.translate(-anchors.groundAnchor[0] * scale, -anchors.groundAnchor[1] * scale); canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        boolean movingMouth = (motion.speaking || motion.gesture == AvatarMotion.Gesture.LAUGH) && motion.mouthOpen > .04f;
        if (movingMouth || motion.expression == AvatarMotion.Expression.WARM || motion.expression == AvatarMotion.Expression.SURPRISED) {
            float x = anchors.mouth[0], y = anchors.mouth[1], s=anchors.faceScale;
            canvas.save(); canvas.rotate(anchors.mouthRotationDegrees, x, y);
            Rect sample = new Rect((int)(x-8*s), (int)(y-12*s), (int)(x+8*s), (int)(y-7*s));
            canvas.drawBitmap(bitmap, sample, new RectF(x-14*s,y-4*s,x+14*s,y+6*s), paint);
            paint.setColor(0xFF754945);
            if (movingMouth || motion.expression == AvatarMotion.Expression.SURPRISED)
                canvas.drawOval(x-9*s,y-2*s,x+9*s,y+(2+10*(movingMouth?motion.mouthOpen:.7f))*s,paint);
            else {
                Path smile = new Path(); smile.moveTo(x-12*s,y-s); smile.quadTo(x,y+7*s,x+12*s,y-s);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f*s); canvas.drawPath(smile,paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(Color.WHITE); canvas.restore();
        }
        float[][] eyes = new float[][]{anchors.leftEye,anchors.rightEye};
        for(int i=0;i<eyes.length;i++) {
            float x=eyes[i][0],y=eyes[i][1],s=anchors.faceScale;
            if(motion.expression==AvatarMotion.Expression.SAD){
                paint.setColor(0xB58CCEFF);canvas.drawOval(x-3*s,y+12*s,x+3*s,y+34*s,paint);
            } else if(motion.expression==AvatarMotion.Expression.SHY){
                paint.setColor(0x48ED98AB);canvas.drawOval(x-17*s,y+20*s,x+17*s,y+30*s,paint);
            } else if(motion.expression==AvatarMotion.Expression.ANGRY){
                float side=i==0?1:-1;paint.setColor(0xB78C818D);paint.setStrokeWidth(2*s);
                canvas.drawLine(x-17*side*s,y-20*s,x+15*side*s,y-13*s,paint);
            }
        }
        paint.setColor(Color.WHITE);
        canvas.restore(); return true;
    }
}

