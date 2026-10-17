package salve.presentation.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

/**
 * Tres puntos de luz que laten en ola, en los colores de Salve (cian → azul eléctrico, como los
 * circuitos de su traje). Sólo se anima mientras está visible.
 */
public final class ThinkingDotsView extends View {
    private static final int[] CORE = {0xFF7FF3FF, 0xFF4FC3FF, 0xFF3A7BFF};
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long start = SystemClock.uptimeMillis();

    public ThinkingDotsView(Context context) { this(context, null); }

    public ThinkingDotsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        float d = getResources().getDisplayMetrics().density;
        setMeasuredDimension(resolveSize(Math.round(44 * d), widthSpec), resolveSize(Math.round(22 * d), heightSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float r = Math.min(h * 0.22f, w / 12f);
        float gap = (w - 6 * r) / 4f;
        float t = (SystemClock.uptimeMillis() - start) / 1000f;
        for (int i = 0; i < 3; i++) {
            // Ola: cada punto sube y se ilumina un poco después del anterior.
            float phase = (float) Math.sin((t * 2.4f - i * 0.55f) * Math.PI);
            float lift = Math.max(0f, phase);
            float cx = gap + r + i * (2 * r + gap), cy = h / 2f - lift * h * 0.18f;
            float rr = r * (0.85f + 0.3f * lift);
            int alpha = Math.round(140 + 115 * lift);
            glow.setShader(new RadialGradient(cx, cy, rr * 2.6f, (CORE[i] & 0x00FFFFFF) | (Math.round(alpha * 0.45f) << 24),
                    CORE[i] & 0x00FFFFFF, Shader.TileMode.CLAMP));
            canvas.drawCircle(cx, cy, rr * 2.6f, glow);
            dot.setColor((CORE[i] & 0x00FFFFFF) | (alpha << 24));
            canvas.drawCircle(cx, cy, rr, dot);
        }
        if (isShown()) postInvalidateOnAnimation();
    }

    @Override protected void onVisibilityChanged(View changed, int visibility) {
        super.onVisibilityChanged(changed, visibility);
        if (visibility == VISIBLE) postInvalidateOnAnimation();
    }
}
