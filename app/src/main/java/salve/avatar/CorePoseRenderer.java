package salve.avatar;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Original 2D pose artwork, not a synthetic 3D mesh or a morphed transition. */
final class CorePoseRenderer {
    private final Context context;
    private final Runnable changed;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private ExecutorService worker;
    private volatile long generation;
    private String requested, loaded;
    private Bitmap bitmap;
    private boolean attached;

    CorePoseRenderer(Context context, Runnable changed) {
        this.context = context.getApplicationContext(); this.changed = changed;
    }
    void attach() { attached = true; worker = Executors.newSingleThreadExecutor(); }
    void detach() {
        attached = false; generation++;
        if (worker != null) worker.shutdownNow();
        worker = null; requested = loaded = null;
        if (bitmap != null) bitmap.recycle(); bitmap = null;
    }
    void select(String id) {
        if (!attached || java.util.Objects.equals(requested, id)) return;
        requested = id; final long task = ++generation;
        loaded = null;
        if (bitmap != null) bitmap.recycle(); bitmap = null;
        if (id == null) return;
        // One queue slot matters: superseded requests never decode or publish a stale pose.
        worker.execute(() -> {
            if (task != generation) return;
            Bitmap decoded = null;
            try (InputStream in = context.getAssets().open("avatar/core/" + id + ".png")) {
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
                decoded = BitmapFactory.decodeStream(in, null, options);
                if (decoded == null || decoded.getWidth() != 1024 || decoded.getHeight() != 1536)
                    throw new IllegalArgumentException("Unexpected core pose dimensions");
                Bitmap ready = decoded;
                main.post(() -> {
                    if (!attached || task != generation) { ready.recycle(); return; }
                    bitmap = ready; loaded = id; changed.run();
                });
            } catch (java.io.IOException | RuntimeException | OutOfMemoryError error) {
                if (decoded != null) decoded.recycle();
                main.post(() -> { if (attached && task == generation) changed.run(); });
            }
        });
    }
    /** Source images fill different canvases; scale by face size, not the pose's bounding box. */
    boolean draw(Canvas canvas, String id, AvatarMotion.Snapshot motion, float standingHeight) {
        if (bitmap == null || !id.equals(loaded)) return false;
        float factor = "seated".equals(id) ? .55f : "kneeling".equals(id) ? .55f : .60f;
        float scale = standingHeight / 1536f * factor;
        canvas.save(); canvas.translate(-512f * scale, -1536f * scale); canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        boolean movingMouth = (motion.speaking || motion.gesture == AvatarMotion.Gesture.LAUGH) && motion.mouthOpen > .04f;
        if (movingMouth || motion.expression == AvatarMotion.Expression.WARM || motion.expression == AvatarMotion.Expression.SURPRISED) {
            // Landmarks were checked against each exact source. No whole-face replacement.
            float x = "seated".equals(id) ? 540 : "kneeling".equals(id) ? 506 : 345;
            float y = "seated".equals(id) ? 289 : "kneeling".equals(id) ? 296 : 349;
            canvas.save(); canvas.rotate("seated".equals(id) ? -12 : "kneeling".equals(id) ? 7 : 0, x, y);
            Rect sample = new Rect((int)x-8, (int)y-12, (int)x+8, (int)y-7);
            canvas.drawBitmap(bitmap, sample, new RectF(x-14,y-4,x+14,y+6), paint);
            paint.setColor(0xFF754945);
            if (movingMouth || motion.expression == AvatarMotion.Expression.SURPRISED)
                canvas.drawOval(x-9,y-2,x+9,y+2+10*(movingMouth?motion.mouthOpen:.7f),paint);
            else {
                Path smile = new Path(); smile.moveTo(x-12,y-1); smile.quadTo(x,y+7,x+12,y-1);
                paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f); canvas.drawPath(smile,paint);
                paint.setStyle(Paint.Style.FILL);
            }
            paint.setColor(Color.WHITE); canvas.restore();
        }
        float[][] eyes = "seated".equals(id) ? new float[][]{{467,246},{564,210}}
                : "kneeling".equals(id) ? new float[][]{{470,220},{561,240}} : new float[][]{{303,294},{377,261}};
        for(int i=0;i<eyes.length;i++) {
            float x=eyes[i][0],y=eyes[i][1];
            if(motion.expression==AvatarMotion.Expression.SAD){
                paint.setColor(0xB58CCEFF);canvas.drawOval(x-3,y+12,x+3,y+34,paint);
            } else if(motion.expression==AvatarMotion.Expression.SHY){
                paint.setColor(0x48ED98AB);canvas.drawOval(x-17,y+20,x+17,y+30,paint);
            } else if(motion.expression==AvatarMotion.Expression.ANGRY){
                float side=i==0?1:-1;paint.setColor(0xB78C818D);paint.setStrokeWidth(2);
                canvas.drawLine(x-17*side,y-20,x+15*side,y-13,paint);
            }
        }
        paint.setColor(Color.WHITE);
        canvas.restore(); return true;
    }
}
