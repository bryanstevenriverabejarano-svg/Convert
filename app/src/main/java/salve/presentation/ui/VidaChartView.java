package salve.presentation.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.Locale;

/** Gráfica de línea sencilla para la evolución de un ejercicio (sin librerías). */
final class VidaChartView extends View {
    private double[] values = new double[0];
    private String[] labels = new String[0];
    private String unit = "kg";
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), dot = new Paint(Paint.ANTI_ALIAS_FLAG),
            grid = new Paint(Paint.ANTI_ALIAS_FLAG), text = new Paint(Paint.ANTI_ALIAS_FLAG), fill = new Paint(Paint.ANTI_ALIAS_FLAG);

    VidaChartView(Context c) {
        super(c);
        float d = c.getResources().getDisplayMetrics().density;
        line.setColor(VidaUi.BLUE); line.setStrokeWidth(3 * d); line.setStyle(Paint.Style.STROKE); line.setStrokeJoin(Paint.Join.ROUND);
        dot.setColor(VidaUi.BLUE);
        grid.setColor(Color.rgb(60, 60, 64)); grid.setStrokeWidth(1 * d);
        text.setColor(VidaUi.MUTED); text.setTextSize(12 * d);
        fill.setColor(Color.argb(50, 30, 136, 229)); fill.setStyle(Paint.Style.FILL);
    }

    void setData(double[] values, String[] labels, String unit) {
        this.values = values; this.labels = labels; this.unit = unit;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float d = getResources().getDisplayMetrics().density;
        float left = 44 * d, right = getWidth() - 12 * d, top = 12 * d, bottom = getHeight() - 24 * d;
        if (values.length == 0) { canvas.drawText("Sin datos todavía", left, (top + bottom) / 2, text); return; }
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        double pad = Math.max(1, (max - min) * 0.15);
        min = Math.max(0, min - pad); max = max + pad;
        for (int i = 0; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, grid);
            double v = min + (max - min) * i / 3;
            canvas.drawText(String.format(Locale.ROOT, "%.0f", v), 4 * d, y + 4 * d, text);
        }
        Path path = new Path(), area = new Path();
        float step = values.length == 1 ? 0 : (right - left) / (values.length - 1);
        for (int i = 0; i < values.length; i++) {
            float x = values.length == 1 ? (left + right) / 2 : left + step * i;
            float y = (float) (bottom - (bottom - top) * (values[i] - min) / (max - min));
            if (i == 0) { path.moveTo(x, y); area.moveTo(x, bottom); area.lineTo(x, y); }
            else { path.lineTo(x, y); area.lineTo(x, y); }
            if (i == values.length - 1) { area.lineTo(x, bottom); area.close(); }
            canvas.drawCircle(x, y, (i == values.length - 1 ? 5 : 3) * d, dot);
        }
        canvas.drawPath(area, fill);
        canvas.drawPath(path, line);
        if (labels.length > 0) {
            canvas.drawText(labels[0], left, getHeight() - 6 * d, text);
            String last = labels[labels.length - 1] + "  " + String.format(Locale.ROOT, "%.1f", values[values.length - 1]).replace(".0", "") + " " + unit;
            float w = text.measureText(last);
            canvas.drawText(last, Math.max(left, right - w), getHeight() - 6 * d, text);
        }
    }
}
