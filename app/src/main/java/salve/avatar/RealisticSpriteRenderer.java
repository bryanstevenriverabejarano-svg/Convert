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
                    main.post(() -> { sheets = ready; for (Runnable listener : new ArrayList<>(listeners)) listener.run(); });
                } catch (Exception | OutOfMemoryError error) {
                    for (Sheet sheet : ready.values()) sheet.bitmap.recycle();
                    Log.e("SalveSprites", "No se pudieron cargar los sprites; se conserva el respaldo ilustrado", error);
                } finally { worker.shutdown(); }
            });
        }
    }
    private static Assets shared;
    private final Assets assets;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
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
        canvas.drawBitmap(sheet.bitmap, source, destination, paint);
        canvas.restore();
        return true;
    }
}
