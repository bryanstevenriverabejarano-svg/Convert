package salve.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shared, asynchronously decoded RGBA sprites. No WebView, network or 3D dependencies. */
final class RealisticSpriteRenderer {
    private static final class Sheet {
        final Bitmap bitmap;
        final Rect[] frames;
        final float[] referenceHeights;
        Sheet(Bitmap bitmap, Rect[] frames, float[] referenceHeights) {
            this.bitmap = bitmap; this.frames = frames; this.referenceHeights = referenceHeights;
        }
    }
    private static final class Assets {
        final List<Runnable> listeners = new ArrayList<>();
        volatile Map<String, Sheet> sheets;
        /** Capa viva del cuerpo astral (opcional: sin ella se dibuja el PNG tal cual). */
        volatile Astral astral;
        Assets(Context context) {
            Handler main = new Handler(Looper.getMainLooper());
            ExecutorService worker = Executors.newSingleThreadExecutor();
            worker.execute(() -> {
                Map<String, Sheet> ready = new HashMap<>();
                try (InputStream input = context.getAssets().open("avatar/realistic/manifest.json")) {
                    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
                    JSONObject manifest = new JSONObject(bytes.toString("UTF-8")).getJSONObject("sheets");
                    for (String name : new String[]{"base", "views", "walk", "dance", "poses"}) {
                        JSONObject spec = manifest.getJSONObject(name);
                        Bitmap bitmap;
                        try (InputStream file = context.getAssets().open("avatar/realistic/" + name + ".png")) {
                            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
                            bitmap = BitmapFactory.decodeStream(file, null, options);
                        }
                        if (bitmap == null) throw new java.io.IOException("Missing sprite " + name);
                        // Add immediately so any validation failure also releases this bitmap.
                        JSONArray regions = spec.getJSONArray("frames");
                        Rect[] frames = new Rect[regions.length()];
                        float[] referenceHeights = new float[regions.length()];
                        ready.put(name, new Sheet(bitmap, frames, referenceHeights));
                        if (bitmap.getWidth() != spec.getInt("width") || bitmap.getHeight() != spec.getInt("height")
                                || !bitmap.hasAlpha()) throw new java.io.IOException("Invalid sprite " + name);
                        for (int i = 0; i < frames.length; i++) {
                            JSONArray r = regions.getJSONArray(i);
                            frames[i] = new Rect(r.getInt(0), r.getInt(1), r.getInt(2), r.getInt(3));
                            referenceHeights[i] = (float)spec.getJSONArray("frameHeights").getDouble(i);
                            if (!Float.isFinite(referenceHeights[i]) || referenceHeights[i] <= 0)
                                throw new java.io.IOException("Invalid sprite scale " + name);
                            if (frames[i].isEmpty() || frames[i].left < 0 || frames[i].top < 0
                                    || frames[i].right > bitmap.getWidth() || frames[i].bottom > bitmap.getHeight())
                                throw new java.io.IOException("Invalid sprite region " + name);
                        }
                    }
                    Astral living = loadAstral(context, ready.get("base"));
                    main.post(() -> { sheets = ready; astral = living; for (Runnable listener : new ArrayList<>(listeners)) listener.run(); });
                } catch (Exception | OutOfMemoryError error) {
                    for (Sheet sheet : ready.values()) sheet.bitmap.recycle();
                    Log.e("SalveSprites", "No se pudieron cargar los sprites; se conserva el respaldo ilustrado", error);
                } finally { worker.shutdown(); }
            });
        }
    }
    private static final class Astral {
        final AstralBodyRig rig;
        final Bitmap eyes, glow;
        Astral(AstralBodyRig rig, Bitmap eyes, Bitmap glow) { this.rig = rig; this.eyes = eyes; this.glow = glow; }
    }

    private static Astral loadAstral(Context context, Sheet base) {
        if (base == null) return null;
        try (InputStream input = context.getAssets().open("avatar/realistic/astral.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            AstralBodyRig rig = AstralBodyRig.parse(bytes.toString("UTF-8"));
            if (rig.width != base.bitmap.getWidth() || rig.height != base.bitmap.getHeight()) return null;
            Bitmap eyes = decode(context, "avatar/realistic/astral_eyes.png");
            Bitmap glow = decode(context, "avatar/realistic/astral_glow.png");
            if (eyes == null || glow == null) return null;
            return new Astral(rig, eyes, glow);
        } catch (Exception | OutOfMemoryError error) {
            Log.w("SalveSprites", "Cuerpo astral sin capa viva; se dibuja la ilustración fija", error);
            return null;
        }
    }

    private static Bitmap decode(Context context, String path) throws java.io.IOException {
        try (InputStream file = context.getAssets().open(path)) {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inScaled = false;
            return BitmapFactory.decodeStream(file, null, options);
        }
    }

    private static Assets shared;
    private final Assets assets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Rect eyeSource = new Rect();
    private final RectF eyeDestination = new RectF();
    private final android.graphics.Path ring = new android.graphics.Path();
    private float[] vertices;
    private static final android.graphics.ColorFilter CIRCUIT =
            new android.graphics.PorterDuffColorFilter(0xFFA6F4FF, android.graphics.PorterDuff.Mode.SRC_IN);
    private static final android.graphics.ColorFilter WAVE =
            new android.graphics.PorterDuffColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.SRC_IN);
    RealisticSpriteRenderer(Context context) {
        synchronized (RealisticSpriteRenderer.class) {
            if (shared == null) shared = new Assets(context.getApplicationContext());
            assets = shared;
        }
    }
    static boolean supports(AvatarDesignSpec design) {
        return "core".equals(design.template) && design.palette == AvatarDesignSpec.Palette.ORIGINAL
                && design.pattern == AvatarDesignSpec.Pattern.NONE;
    }
    void addListener(Runnable listener) { if (!assets.listeners.contains(listener)) assets.listeners.add(listener); }
    void removeListener(Runnable listener) { assets.listeners.remove(listener); }
    boolean drawView(Canvas canvas, int index, float width, float height) {
        Map<String, Sheet> sheets = assets.sheets;
        if (sheets == null || width <= 0 || height <= 0) return false;
        Sheet sheet = sheets.get(index == 0 ? "base" : "views");
        Rect source = sheet.frames[index == 0 ? 0 : Math.max(0, Math.min(7, index))];
        float scale = Math.min(width / source.width(), height / source.height()) * .94f;
        float w = source.width() * scale, h = source.height() * scale;
        destination.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2);
        canvas.drawBitmap(sheet.bitmap, source, destination, paint);
        return true;
    }
    boolean draw(Canvas canvas, AvatarState state, AvatarMotion.Snapshot motion, float height,
                 float gaitPhase, float time, boolean sleeping) {
        return draw(canvas, state, motion, null, height, gaitPhase, time, sleeping);
    }

    boolean draw(Canvas canvas, AvatarState state, AvatarMotion.Snapshot motion, AstralBody.Snapshot body, float height,
                 float gaitPhase, float time, boolean sleeping) {
        Map<String, Sheet> sheets = assets.sheets;
        if (sheets == null) return false;
        RealisticSpriteAnimation.Frame selected = RealisticSpriteAnimation.select(state, motion, gaitPhase, time, sleeping);
        Sheet sheet = sheets.get(selected.sheet);
        Rect source = sheet.frames[selected.index];
        float drawnHeight = height * selected.heightScale;
        // All walk frames use one scale even where the measured source regions differ in width.
        if ("walk".equals(selected.sheet)) drawnHeight *= .94f;
        drawnHeight *= source.height() / sheet.referenceHeights[selected.index];
        float width = drawnHeight * source.width() / source.height();
        // The source atlas regions are measured, not assumed to be equal-sized cells.
        // Fit their widest silhouette into the stage's existing safe envelope.
        if (width > height * .76f) { drawnHeight *= height * .76f / width; width = height * .76f; }
        canvas.save();
        if (selected.mirror) canvas.scale(-1, 1);
        float breath = sleeping ? 0 : motion.breath * height * .0015f;
        destination.set(-width / 2, -drawnHeight - breath, width / 2, -breath);
        Astral living = assets.astral;
        if (body != null && living != null && "base".equals(selected.sheet) && !selected.mirror)
            drawLiving(canvas, sheet.bitmap, living, body, drawnHeight / source.height());
        else canvas.drawBitmap(sheet.bitmap, source, destination, paint);
        canvas.restore();
        return true;
    }

    /** La ilustración base como cuerpo vivo: malla de pelo y cabeza, párpados y circuitos de luz. */
    private void drawLiving(Canvas canvas, Bitmap base, Astral living, AstralBody.Snapshot body, float scale) {
        AstralBodyRig rig = living.rig;
        canvas.save();
        canvas.translate(destination.left, destination.top);
        canvas.scale(scale, scale);
        canvas.rotate(body.sway, rig.feetX, rig.feetY);
        vertices = rig.deform(body, vertices);
        canvas.drawBitmapMesh(base, rig.cols, rig.rows, vertices, 0, null, 0, paint);
        if (body.blink > .3f) {
            int frame = body.blink >= .8f ? 1 : 0;
            int h = rig.eyeBottom - rig.eyeTop;
            eyeSource.set(0, frame * h, rig.eyeRight - rig.eyeLeft, (frame + 1) * h);
            eyeDestination.set(rig.eyeLeft, rig.eyeTop, rig.eyeRight, rig.eyeBottom);
            canvas.save();
            canvas.translate(body.headShiftX, body.headShiftY);
            canvas.rotate(body.headTilt, rig.neckX, rig.neckY);
            canvas.drawBitmap(living.eyes, eyeSource, eyeDestination, paint);
            canvas.restore();
        }
        float glow = Math.max(0, Math.min(1, body.glow));
        if (glow > .02f) {
            glowPaint.setColorFilter(CIRCUIT);
            glowPaint.setAlpha(Math.round(glow * 210));
            canvas.drawBitmapMesh(living.glow, rig.cols, rig.rows, vertices, 0, null, 0, glowPaint);
            if (body.glowRing >= 0) {
                // Una onda de energía sale del corazón y recorre los circuitos.
                float radius = body.glowRing * rig.height * .62f, band = 120f;
                ring.reset();
                ring.setFillType(android.graphics.Path.FillType.EVEN_ODD);
                ring.addCircle(rig.heartX, rig.heartY, radius + band, android.graphics.Path.Direction.CW);
                ring.addCircle(rig.heartX, rig.heartY, Math.max(0, radius - band), android.graphics.Path.Direction.CW);
                canvas.save();
                canvas.clipPath(ring);
                glowPaint.setColorFilter(WAVE);
                glowPaint.setAlpha(Math.round(230 * (1 - body.glowRing)));
                canvas.drawBitmapMesh(living.glow, rig.cols, rig.rows, vertices, 0, null, 0, glowPaint);
                canvas.restore();
            }
        }
        canvas.restore();
    }
}
