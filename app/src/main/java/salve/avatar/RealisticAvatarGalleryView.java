package salve.avatar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Gallery for the eight current views; no legacy frontal rig is applied to the new pixels. */
public final class RealisticAvatarGalleryView extends View {
    private final RealisticSpriteRenderer renderer;
    private final Runnable changed = this::invalidate;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int index;
    public RealisticAvatarGalleryView(Context context) { super(context); renderer = new RealisticSpriteRenderer(context); }
    public void select(int value) { index = Math.max(0, Math.min(7, value)); invalidate(); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); renderer.addListener(changed); }
    @Override protected void onDetachedFromWindow() { renderer.removeListener(changed); super.onDetachedFromWindow(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!renderer.drawView(canvas, index, getWidth(), getHeight())) {
            paint.setColor(0xFFD9F3FF); paint.setTextSize(32);
            canvas.drawText("Cargando a Salve…", 16, 48, paint);
        }
    }
}
