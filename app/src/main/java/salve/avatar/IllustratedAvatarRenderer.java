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
import android.os.Handler;
import android.os.Looper;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;
import com.salve.app.R;

/** Textured 2D puppet. The user's original pixels remain the character, including its face. */
public final class IllustratedAvatarRenderer {
    private static final class Artwork {
        final Bitmap bitmap; final AvatarRig rig;
        Artwork(Bitmap bitmap, AvatarRig rig) { this.bitmap=bitmap; this.rig=rig; }
    }
    private static final class Assets {
        final Bitmap original;
        final Artwork baseArtwork;
        final AvatarRig rig, cyberRig, coreRig;
        final Context context;
        final ExecutorService wardrobeWorker=Executors.newSingleThreadExecutor();
        final AtomicLong revision=new AtomicLong();
        final Handler main=new Handler(Looper.getMainLooper());
        final List<Runnable> listeners=new ArrayList<>();
        volatile Artwork dressed;
        private String requestedId=AvatarDesignSpec.CORE_ID;
        Assets(Context context) throws java.io.IOException {
            this.context=context;
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            original = BitmapFactory.decodeResource(context.getResources(), R.drawable.salve_original, options);
            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("avatar/rig.json"), StandardCharsets.UTF_8)) {
                rig = new AvatarRig(reader);
            }
            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("avatar/cyber-rig.json"), StandardCharsets.UTF_8)) { cyberRig = new AvatarRig(reader); }
            try (InputStreamReader reader = new InputStreamReader(context.getAssets().open("avatar/core/rig.json"), StandardCharsets.UTF_8)) { coreRig = new AvatarRig(reader); }
            if (original == null || original.getWidth() != rig.width || original.getHeight() != rig.height)
                throw new IllegalArgumentException("The portrait and its rig do not match");
            Bitmap base=BitmapFactory.decodeResource(context.getResources(),R.drawable.salve_imagen,options);
            if(base==null || base.getWidth()!=coreRig.width || base.getHeight()!=coreRig.height)
                throw new java.io.IOException("Missing or invalid core artwork");
            baseArtwork=new Artwork(base,coreRig);
            dressed=baseArtwork;
        }
        synchronized void select(AvatarDesignSpec design) {
            if(requestedId.equals(design.id))return;
            requestedId=design.id;long task=revision.incrementAndGet();
            if(AvatarDesignSpec.ORIGINAL_ID.equals(design.id)){dressed=new Artwork(original,rig);notifyViews();return;}
            wardrobeWorker.execute(() -> {
                if(revision.get()!=task)return;
                try {
                    Artwork ready=compose(design);
                    publish(task,ready);
                } catch(java.io.IOException|RuntimeException error) {
                    publish(task,baseArtwork);
                    Log.w("SalveAvatar","No pude componer el vestuario; muestro el núcleo como imagen base",error);
                }
            });
        }
        private synchronized void publish(long task,Artwork bitmap) {
            if(revision.get()==task){dressed=bitmap;notifyViews();}
        }
        private void notifyViews(){main.post(() -> {for(Runnable listener:new ArrayList<>(listeners))listener.run();});}
        private Artwork compose(AvatarDesignSpec design) throws java.io.IOException {
            AvatarDesignCatalog.Template template=AvatarDesignCatalog.template(design.template);
            Bitmap source=original;
            if(template.assetPath!=null)try(InputStream stream=context.getAssets().open(template.assetPath)) {
                source=BitmapFactory.decodeStream(stream);
            }
            if(source==null||source.getWidth()!=rig.width||source.getHeight()!=rig.height)
                throw new java.io.IOException("Artwork does not match the frontal rig");
            int[] pixels=new int[rig.width*rig.height];source.getPixels(pixels,0,rig.width,0,0,rig.width,rig.height);
            boolean core="core".equals(design.template);
            boolean cyber="kuro".equals(design.template)||"shiro".equals(design.template);
            if(source!=original && !cyber && !core) {
                // New frontal clothing has its own silhouette. Preserve the approved face/head above
                // the collar; blending only at the hair/neck boundary avoids hard horizontal seams.
                int[] head=new int[rig.width*390];original.getPixels(head,0,rig.width,0,0,rig.width,390);
                for(int y=0;y<390;y++)for(int x=0;x<rig.width;x++) {
                    int at=y*rig.width+x;pixels[at]=AvatarClothingStyle.blendHead(head[at],pixels[at],(390-y)/20f);
                }

            }
            String pattern=design.pattern.name();
            for(int y=420;y<=960;y++)for(int x=250;x<=690;x++) {
                int at=y*rig.width+x;pixels[at]=AvatarClothingStyle.pixel(design.template,x,y,pixels[at],design.color,pattern);
            }
            if(source!=original)source.recycle();
            return new Artwork(Bitmap.createBitmap(pixels,rig.width,rig.height,Bitmap.Config.ARGB_8888),core?coreRig:cyber?cyberRig:rig);
        }
    }
    // Main, room and overlay share immutable decoded textures, not three copies of the illustration.
    private static Assets sharedAssets;
    private final Assets assets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path mouth = new Path();
    private final Matrix face = new Matrix();
    private final float[] vertices, faceFrom = {450,310, 510,310, 450,350}, faceTo = new float[6];
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
        textureBounds = new RectF(0, 0, r.width, r.height);
    }
    public void selectWardrobe(AvatarDesignSpec design) { assets.select(design); }
    public void addListener(Runnable listener){if(!assets.listeners.contains(listener))assets.listeners.add(listener);}
    public void removeListener(Runnable listener){assets.listeners.remove(listener);}

    /** Draw at the source illustration's 1024x1536 coordinates. Host sets size and screen location. */
    public void draw(Canvas canvas, AvatarMotion.Snapshot pose, float stride, boolean asleep) {
        Artwork artwork=assets.dressed;
        AvatarRig r = artwork.rig;
        Bitmap texture=artwork.bitmap;
        float armL = asleep ? 0 : pose.leftArm, armR = asleep ? 0 : pose.rightArm;
        float step = asleep ? 0 : stride;
        float kneeL = asleep ? 0 : pose.leftKnee, kneeR = asleep ? 0 : pose.rightKnee;
        float blink = asleep ? 1 : pose.blink;
        float tilt = asleep ? 0 : pose.headTilt;
        float yaw = asleep ? 0 : pose.headYaw;
        float pitch = asleep ? 0 : pose.headPitch;
        float gazeX = asleep ? 0 : pose.gazeX, gazeY = asleep ? 0 : pose.gazeY;
        if (!asleep && pose.expression == AvatarMotion.Expression.CURIOUS) {
            tilt += 1.5f; gazeX += .1f;
        } else if (!asleep && (pose.expression == AvatarMotion.Expression.CONCERNED || pose.expression == AvatarMotion.Expression.SAD)) {
            tilt -= 1.2f; gazeY += .2f; blink = Math.max(blink, .10f);
        } else if (!asleep && pose.expression == AvatarMotion.Expression.ANGRY) {
            pitch += 2; blink = Math.max(blink, .18f);
        }
        canvas.save();
        canvas.rotate(asleep ? 0 : AvatarRig.limit(pose.bodyTilt, -3, 3), r.bodyPivot[0], r.bodyPivot[1]);
        // Every triangle uses the same vertices. Limbs stay connected through a continuous skin field.
        boolean neutral = tilt == 0 && yaw == 0 && pitch == 0 && blink == 0 && gazeX == 0 && gazeY == 0
                && pose.breath == .5f && armL == 0 && armR == 0 && step == 0 && kneeL == 0 && kneeR == 0;
        if (neutral) {
            canvas.drawBitmap(texture, null, textureBounds, paint);
        } else {
            r.frame(tilt,yaw,pitch,blink,gazeX,gazeY,pose.breath,armL,armR,step,kneeL,kneeR).fillVertices(vertices);
            canvas.drawBitmapMesh(texture,r.columns,r.rows,vertices,0,null,0,paint);
        }
        boolean speakingMouth = !asleep && (pose.speaking || pose.gesture == AvatarMotion.Gesture.LAUGH) && pose.mouthOpen > .04f;
        boolean smile = !asleep && !speakingMouth && pose.expression == AvatarMotion.Expression.WARM;
        boolean emotion = !asleep && (pose.expression == AvatarMotion.Expression.SAD
                || pose.expression == AvatarMotion.Expression.ANGRY || pose.expression == AvatarMotion.Expression.SHY
                || pose.expression == AvatarMotion.Expression.SURPRISED);
        if (speakingMouth || smile || emotion) {
            float mx=r.mouth[0], my=r.mouth[1];
            faceFrom[0]=mx-27; faceFrom[1]=my-20; faceFrom[2]=mx+33; faceFrom[3]=my-20; faceFrom[4]=mx-27; faceFrom[5]=my+20;
            mouthSample.set((int)mx-12,(int)my-15,(int)mx+13,(int)my-7);
            mouthCover.set(mx-18,my-8,mx+19,my+10);
            for (int i = 0; i < faceFrom.length; i += 2)
                r.deform(faceFrom[i], faceFrom[i+1], tilt, yaw, pitch, blink, gazeX, gazeY, pose.breath, faceTo, i);
            face.setPolyToPoly(faceFrom, 0, faceTo, 0, 3);
            canvas.save(); canvas.concat(face);
            if (speakingMouth) drawMouth(canvas, AvatarRig.limit(pose.mouthOpen, 0, 1), artwork);
            else if (smile) drawSmile(canvas, artwork);
            else if (pose.expression == AvatarMotion.Expression.SURPRISED) drawMouth(canvas, .7f, artwork);
            if (emotion) drawEmotion(canvas, artwork, pose.expression);
            canvas.restore();
        }
        canvas.restore();
    }
    private void drawEmotion(Canvas canvas, Artwork artwork, AvatarMotion.Expression expression) {
        AvatarRig r = artwork.rig;
        for (float[] eye : new float[][] {r.leftEye, r.rightEye}) {
            if (expression == AvatarMotion.Expression.SAD) {
                paint.setColor(0xB58CCEFF);
                canvas.drawOval(eye[0]-3, eye[1]+12, eye[0]+3, eye[1]+29, paint);
            } else if (expression == AvatarMotion.Expression.SHY) {
                paint.setColor(0x48ED98AB);
                canvas.drawOval(eye[0]-14, eye[1]+18, eye[0]+14, eye[1]+27, paint);
            } else if (expression == AvatarMotion.Expression.ANGRY) {
                float side = eye == r.leftEye ? 1 : -1;
                paint.setColor(0xB78C818D); paint.setStrokeWidth(2);
                canvas.drawLine(eye[0]-13*side,eye[1]-17,eye[0]+12*side,eye[1]-11,paint);
            }
        }
        paint.setColor(Color.WHITE);
    }
    private void drawMouth(Canvas canvas, float open, Artwork artwork) {
        // Only replace the tiny closed-mouth line during speech; eyes, nose and face stay textured.
        canvas.drawBitmap(artwork.bitmap, mouthSample, mouthCover, paint);
        float x = artwork.rig.mouth[0], y = artwork.rig.mouth[1], half = 7 + open * 7, height = 2 + open * 13;
        mouth.reset(); mouth.moveTo(x - half, y - 1);
        mouth.quadTo(x, y - 2.5f, x + half, y - 1);
        mouth.quadTo(x + half * .7f, y + height, x, y + height);
        mouth.quadTo(x - half * .7f, y + height, x - half, y - 1); mouth.close();
        paint.setColor(0xFF754945); canvas.drawPath(mouth, paint);
        paint.setColor(Color.WHITE);
    }
    private void drawSmile(Canvas canvas, Artwork artwork) {
        canvas.drawBitmap(artwork.bitmap, mouthSample, mouthCover, paint);
        float x = artwork.rig.mouth[0], y = artwork.rig.mouth[1];
        mouth.reset(); mouth.moveTo(x - 13, y - 1); mouth.quadTo(x, y + 6, x + 13, y - 1);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.7f); paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xFFBA8A7E); canvas.drawPath(mouth, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
    }
}
