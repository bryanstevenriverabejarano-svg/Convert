package salve.presentation.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

/** Anillo de progreso y mini barras para el panel de Vida (dibujados a mano, sin librerías). */
final class VidaWidgets {
    private VidaWidgets() {}

    /** Anillo con el porcentaje en el centro. */
    static final class Ring extends View {
        private float fraction; private String label = "";
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG), arc = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;
        Ring(Context c, int color) {
            super(c);
            this.color = color;
            float d = c.getResources().getDisplayMetrics().density;
            track.setStyle(Paint.Style.STROKE); track.setStrokeWidth(8 * d); track.setColor(Color.rgb(32, 44, 70));
            arc.setStyle(Paint.Style.STROKE); arc.setStrokeWidth(8 * d); arc.setStrokeCap(Paint.Cap.ROUND);
            text.setColor(Color.WHITE); text.setTypeface(Typeface.DEFAULT_BOLD); text.setTextAlign(Paint.Align.CENTER);
        }
        void set(float fraction, String label) { this.fraction = Math.max(0, Math.min(1, fraction)); this.label = label; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            float s = Math.min(getWidth(), getHeight()), pad = track.getStrokeWidth();
            RectF r = new RectF((getWidth() - s) / 2 + pad, (getHeight() - s) / 2 + pad, (getWidth() + s) / 2 - pad, (getHeight() + s) / 2 - pad);
            canvas.drawArc(r, 0, 360, false, track);
            arc.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, Color.rgb(64, 196, 255), color, Shader.TileMode.CLAMP));
            if (fraction > 0) canvas.drawArc(r, -90, 360 * fraction, false, arc);
            text.setTextSize(s * 0.24f);
            canvas.drawText(label, getWidth() / 2f, getHeight() / 2f + text.getTextSize() * 0.35f, text);
        }
    }

    /** Barras pequeñas (p. ej. días ganados o entrenos por semana). */
    static final class Bars extends View {
        private float[] values = new float[0];
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
        Bars(Context c) { super(c); bar.setColor(VidaUi.BLUE); }
        void set(float[] values) { this.values = values; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            if (values.length == 0) return;
            float max = 0.0001f; for (float v : values) max = Math.max(max, v);
            float gap = getWidth() / (values.length * 4f), w = (getWidth() - gap * (values.length - 1)) / values.length;
            float r = w / 4;
            for (int i = 0; i < values.length; i++) {
                float h = Math.max(getHeight() * 0.12f, getHeight() * values[i] / max);
                float x = i * (w + gap);
                bar.setAlpha(i == values.length - 1 ? 255 : 150);
                canvas.drawRoundRect(new RectF(x, getHeight() - h, x + w, getHeight()), r, r, bar);
            }
        }
    }

    /** Barra de progreso fina y redondeada. */
    static final class Line extends View {
        private float fraction; private final int color;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Line(Context c, int color) { super(c); this.color = color; }
        void set(float f) { fraction = Math.max(0, Math.min(1, f)); invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            float h = getHeight(), r = h / 2;
            p.setColor(Color.rgb(32, 44, 70));
            canvas.drawRoundRect(new RectF(0, 0, getWidth(), h), r, r, p);
            if (fraction <= 0) return;
            p.setColor(color);
            canvas.drawRoundRect(new RectF(0, 0, Math.max(h, getWidth() * fraction), h), r, r, p);
        }
    }
}
