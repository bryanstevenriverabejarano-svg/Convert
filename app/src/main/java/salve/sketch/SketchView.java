package salve.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/**
 * Lienzo de un dibujo dentro del chat, en su propio espacio entre los párrafos. Mientras traza se
 * redibuja en cada fotograma; al terminar avisa una vez para que el relato continúe.
 */
public final class SketchView extends View {
    public interface Listener { void onSketchFinished(SketchView view); }

    private SketchScene scene;
    private final SketchPainter painter;
    private final float density;
    private final RectF frame = new RectF(), box = new RectF();
    private final Paint panel = new Paint(Paint.ANTI_ALIAS_FLAG), border = new Paint(Paint.ANTI_ALIAS_FLAG),
            label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private long startedAt = -1;
    private boolean finished;

    public SketchView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        painter = new SketchPainter(density);
        panel.setColor(0xFF0B1628);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(density);
        border.setColor(0x5536C8FF);
        label.setColor(0xAAD8FBFF);
        label.setTextSize(11.5f * density);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /** Cambia de escena (reinicia) sólo si la clave es distinta. */
    public void bind(SketchScene value, boolean alreadyFinished, Listener l) {
        listener = l;
        if (value == null || (scene != null && scene.cacheKey.equals(value.cacheKey))) return;
        scene = value;
        finished = alreadyFinished;
        startedAt = alreadyFinished ? 0 : -1;
        setContentDescription("Dibujo de Salve: " + value.title);
        Sketches.animation(value);
        requestLayout();
        invalidate();
    }

    public SketchScene scene() { return scene; }
    public boolean isFinished() { return finished; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        if (w <= 0) w = Math.round(320 * density);
        float ratio = scene == null ? .7f : scene.height / scene.width;
        float inner = (w - 24 * density) * Math.min(1.7f, Math.max(.55f, ratio));
        int max = Math.round((scene != null && scene.isSelfPortrait() ? 420 : 300) * density);
        int h = Math.round(Math.min(inner, max) + 34 * density);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (scene == null) return;
        float pad = 12 * density;
        frame.set(pad, 4 * density, getWidth() - pad, getHeight() - 4 * density);
        float r = 14 * density;
        canvas.drawRoundRect(frame, r, r, panel);
        canvas.drawRoundRect(frame, r, r, border);
        box.set(frame.left + 10 * density, frame.top + 8 * density, frame.right - 10 * density, frame.bottom - 24 * density);
        SketchAnimation animation = Sketches.animation(scene);
        long now = SystemClock.uptimeMillis();
        if (animation != null && startedAt < 0) startedAt = now;
        float seconds = startedAt == 0 ? 1e6f : startedAt < 0 ? 0 : (now - startedAt) / 1000f;
        if (animation != null) painter.draw(canvas, animation, seconds, box, 1f, true);
        boolean done = animation != null && animation.finished(seconds);
        String caption = done || animation == null ? null : animation.caption(seconds);
        String text = done ? "✎ " + scene.title : caption != null ? "✎ " + caption : "✎ trazando " + scene.title + "…";
        float room = frame.width() - 24 * density;
        int fits = label.breakText(text, true, room, null);
        if (fits < text.length()) text = text.substring(0, Math.max(1, fits - 1)) + "…";
        canvas.drawText(text, frame.left + 12 * density, frame.bottom - 9 * density, label);
        if (!done) postInvalidateOnAnimation();
        else if (!finished) {
            finished = true;
            Listener l = listener;
            if (l != null) post(() -> l.onSketchFinished(this));
        }
    }
}
