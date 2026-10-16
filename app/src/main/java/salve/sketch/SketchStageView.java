package salve.sketch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * El espacio astral delante de Salve: aquí aparece, grande y luminoso, lo que está dibujando.
 * No intercepta toques (el doble toque sigue llegando a Salve). Tras terminar se queda unos
 * segundos y se desvanece.
 */
public final class SketchStageView extends View {
    private static final float HOLD = 9f, FADE = 1.6f;
    private final SketchPainter painter;
    private final float density;
    private final RectF box = new RectF();
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int lastPart = -1;
    private final Runnable changed = this::invalidate;

    public SketchStageView(Context context) { this(context, null); }
    public SketchStageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        painter = new SketchPainter(density * 1.25f);
        veil.setColor(0xFF07101E);
        text.setColor(0xFFD8FBFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(13 * density);
        text.setShadowLayer(6 * density, 0, 0, 0xFF36C8FF);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); Sketches.addListener(changed); invalidate(); }
    @Override protected void onDetachedFromWindow() { Sketches.removeListener(changed); super.onDetachedFromWindow(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Sketches.Showing showing = Sketches.showing();
        if (showing == null) return;
        SketchAnimation animation = Sketches.animation(showing.scene);
        if (animation == null) { postInvalidateOnAnimation(); return; }
        float seconds = (SystemClock.uptimeMillis() - showing.startedAt) / 1000f;
        float after = seconds - animation.duration();
        float alpha = after <= HOLD ? 1f : 1f - (after - HOLD) / FADE;
        if (alpha <= 0) {
            Sketches.clearShowing();
            salve.avatar.AstralBody.get().setDrawing(null);
            return;
        }
        // Delante de ella, a la altura de las manos: lo proyecta sin taparse la cara.
        float w = getWidth(), h = getHeight();
        float side = Math.min(w * .8f, h * .4f);
        box.set((w - side) / 2, h * .52f, (w + side) / 2, h * .52f + side);
        veil.setAlpha(Math.round(110 * alpha));
        float r = 22 * density;
        canvas.drawRoundRect(box.left - 8 * density, box.top - 8 * density, box.right + 8 * density,
                box.bottom + 8 * density, r, r, veil);
        painter.draw(canvas, animation, seconds, box, alpha, true);
        // Qué parte está trazando ahora, y su atención la sigue («dibujo: brazos»).
        String caption = animation.caption(seconds);
        if (caption != null) {
            int fits = text.breakText(caption, true, w * .92f, null);
            String shown = fits < caption.length() ? caption.substring(0, Math.max(1, fits - 1)) + "…" : caption;
            text.setAlpha(Math.round(220 * alpha));
            canvas.drawText(shown, w / 2f, box.bottom + 26 * density, text);
            int part = animation.partAt(seconds);
            if (part != lastPart) {
                lastPart = part;
                salve.core.mind.AttentionSchema.get().attend(showing.scene.title + ": " + animation.scene.parts.get(part).name,
                        salve.core.mind.AttentionSchema.Source.DRAWING, System.currentTimeMillis());
            }
        } else lastPart = -1;
        postInvalidateOnAnimation();
    }
}
