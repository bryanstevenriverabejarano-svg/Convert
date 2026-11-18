package salve.presentation.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * Iconos de línea dibujados a mano (sin recursos ni librerías), con un leve resplandor azul.
 * Todos se dibujan en una caja de 24×24 y se escalan al tamaño de la vista.
 */
final class VidaIcon extends View {
    enum Type { HOME, CALENDAR, LAYERS, CHECK_CIRCLE, DUMBBELL, FORK_KNIFE, MOON, BARS, HEART_PULSE, TARGET, SPARKLE,
        PLAY, PENCIL, BED, CHECKBOX, CHECKBOX_ON, CLAPPER, CLOCK, BRIEFCASE, LAPTOP, BOOK, COMPASS, DROP, PILL, STAR, PLUS, CHEVRON, BELL, GEAR }

    private final Type type;
    private final int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    VidaIcon(Context c, Type type, int color) {
        super(c);
        this.type = type; this.color = color;
        p.setStyle(Paint.Style.STROKE); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        p.setColor(color);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        p.setShadowLayer(3f * c.getResources().getDisplayMetrics().density, 0, 0, Color.argb(160, 41, 160, 255));
    }

    @Override protected void onDraw(Canvas canvas) {
        float s = Math.min(getWidth(), getHeight()) / 24f;
        canvas.save();
        canvas.translate((getWidth() - 24 * s) / 2, (getHeight() - 24 * s) / 2);
        canvas.scale(s, s);
        p.setStrokeWidth(1.9f);
        p.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        switch (type) {
            case HOME:
                path.moveTo(3, 11); path.lineTo(12, 3.5f); path.lineTo(21, 11);
                path.moveTo(5.5f, 9.5f); path.lineTo(5.5f, 20); path.lineTo(18.5f, 20); path.lineTo(18.5f, 9.5f);
                path.moveTo(10, 20); path.lineTo(10, 14); path.lineTo(14, 14); path.lineTo(14, 20);
                break;
            case CALENDAR:
                canvas.drawRoundRect(new RectF(3.5f, 5, 20.5f, 20.5f), 2.5f, 2.5f, p);
                path.moveTo(3.5f, 9.5f); path.lineTo(20.5f, 9.5f);
                path.moveTo(8, 3); path.lineTo(8, 7); path.moveTo(16, 3); path.lineTo(16, 7);
                dots(canvas, new float[]{8, 13, 12, 13, 16, 13, 8, 17, 12, 17});
                break;
            case LAYERS:
                path.moveTo(12, 3.5f); path.lineTo(21, 8); path.lineTo(12, 12.5f); path.lineTo(3, 8); path.close();
                path.moveTo(3, 12); path.lineTo(12, 16.5f); path.lineTo(21, 12);
                path.moveTo(3, 16); path.lineTo(12, 20.5f); path.lineTo(21, 16);
                break;
            case CHECK_CIRCLE:
                canvas.drawCircle(12, 12, 8.5f, p);
                path.moveTo(8, 12.3f); path.lineTo(11, 15.2f); path.lineTo(16.5f, 9.3f);
                break;
            case DUMBBELL:
                path.moveTo(8, 12); path.lineTo(16, 12);
                round(canvas, 5, 7, 8, 17); round(canvas, 16, 7, 19, 17);
                path.moveTo(2.5f, 9.5f); path.lineTo(2.5f, 14.5f); path.moveTo(21.5f, 9.5f); path.lineTo(21.5f, 14.5f);
                break;
            case FORK_KNIFE:
                path.moveTo(6, 3); path.lineTo(6, 9); path.moveTo(9, 3); path.lineTo(9, 9); path.moveTo(12, 3); path.lineTo(12, 9);
                path.moveTo(6, 9); path.quadTo(9, 12, 12, 9); path.moveTo(9, 10.5f); path.lineTo(9, 21);
                path.moveTo(18, 21); path.lineTo(18, 3); path.quadTo(14.5f, 6, 15, 12); path.lineTo(18, 12);
                break;
            case MOON:
                path.moveTo(15.5f, 3.5f);
                path.cubicTo(8.5f, 3.5f, 5, 12, 9.5f, 17.5f);
                path.cubicTo(13, 21.5f, 18.5f, 20.5f, 20.5f, 16.5f);
                path.cubicTo(14, 17.5f, 10.5f, 10, 15.5f, 3.5f);
                break;
            case BARS:
                round(canvas, 4, 13, 7.5f, 20); round(canvas, 10.25f, 8, 13.75f, 20); round(canvas, 16.5f, 4, 20, 20);
                break;
            case HEART_PULSE:
                path.moveTo(12, 20);
                path.cubicTo(4, 14.5f, 2.5f, 10.5f, 3.5f, 7.5f);
                path.cubicTo(5, 3.5f, 10, 3.5f, 12, 7);
                path.cubicTo(14, 3.5f, 19, 3.5f, 20.5f, 7.5f);
                path.cubicTo(21.5f, 10.5f, 20, 14.5f, 12, 20);
                path.moveTo(4.5f, 12); path.lineTo(8.5f, 12); path.lineTo(10, 9.5f); path.lineTo(12.5f, 14.5f); path.lineTo(14, 12); path.lineTo(19.5f, 12);
                break;
            case TARGET:
                canvas.drawCircle(11, 13, 8, p); canvas.drawCircle(11, 13, 4.5f, p);
                p.setStyle(Paint.Style.FILL); canvas.drawCircle(11, 13, 1.4f, p); p.setStyle(Paint.Style.STROKE);
                path.moveTo(11, 13); path.lineTo(19, 5); path.moveTo(16.5f, 4.5f); path.lineTo(19.5f, 4.5f); path.lineTo(19.5f, 7.5f);
                break;
            case SPARKLE:
                path.moveTo(12, 2.5f); path.quadTo(13, 11, 21.5f, 12); path.quadTo(13, 13, 12, 21.5f);
                path.quadTo(11, 13, 2.5f, 12); path.quadTo(11, 11, 12, 2.5f);
                break;
            case PLAY:
                path.moveTo(7, 4.5f); path.lineTo(19, 12); path.lineTo(7, 19.5f); path.close();
                break;
            case PENCIL:
                path.moveTo(4, 20); path.lineTo(5, 15.5f); path.lineTo(16, 4.5f); path.lineTo(19.5f, 8); path.lineTo(8.5f, 19); path.close();
                path.moveTo(13.5f, 7); path.lineTo(17, 10.5f);
                break;
            case BED:
                path.moveTo(3, 6); path.lineTo(3, 19); path.moveTo(3, 15); path.lineTo(21, 15); path.lineTo(21, 19);
                path.moveTo(3, 12); path.lineTo(21, 12); path.lineTo(21, 15);
                round(canvas, 5, 8.5f, 9.5f, 12);
                break;
            case CHECKBOX:
            case CHECKBOX_ON:
                canvas.drawRoundRect(new RectF(4, 4, 20, 20), 3.5f, 3.5f, p);
                if (type == Type.CHECKBOX_ON) {
                    p.setStyle(Paint.Style.FILL); p.setAlpha(70);
                    canvas.drawRoundRect(new RectF(4, 4, 20, 20), 3.5f, 3.5f, p);
                    p.setAlpha(255); p.setStyle(Paint.Style.STROKE);
                    path.moveTo(8, 12.3f); path.lineTo(11, 15.2f); path.lineTo(16.5f, 9.3f);
                }
                break;
            case CLAPPER:
                canvas.drawRoundRect(new RectF(3.5f, 9.5f, 20.5f, 20.5f), 2, 2, p);
                path.moveTo(3.5f, 9.5f); path.lineTo(19.5f, 4.5f);
                path.moveTo(8, 8); path.lineTo(6.5f, 4.5f); path.moveTo(13, 6.5f); path.lineTo(11.5f, 3.5f);
                path.moveTo(10, 12.5f); path.lineTo(15, 15); path.lineTo(10, 17.5f); path.close();
                break;
            case CLOCK:
                canvas.drawCircle(12, 12, 8.5f, p);
                path.moveTo(12, 7); path.lineTo(12, 12); path.lineTo(15.5f, 14);
                break;
            case BRIEFCASE:
                canvas.drawRoundRect(new RectF(3, 7.5f, 21, 19.5f), 2.5f, 2.5f, p);
                path.moveTo(9, 7.5f); path.lineTo(9, 5); path.lineTo(15, 5); path.lineTo(15, 7.5f);
                path.moveTo(3, 12.5f); path.lineTo(21, 12.5f);
                break;
            case LAPTOP:
                canvas.drawRoundRect(new RectF(5, 5, 19, 15), 1.5f, 1.5f, p);
                path.moveTo(2.5f, 18.5f); path.lineTo(21.5f, 18.5f);
                break;
            case BOOK:
                path.moveTo(12, 6.5f); path.quadTo(8, 4, 3.5f, 5); path.lineTo(3.5f, 18.5f); path.quadTo(8, 17.5f, 12, 20);
                path.quadTo(16, 17.5f, 20.5f, 18.5f); path.lineTo(20.5f, 5); path.quadTo(16, 4, 12, 6.5f); path.lineTo(12, 20);
                break;
            case COMPASS:
                canvas.drawCircle(12, 12, 8.5f, p);
                path.moveTo(15.5f, 8.5f); path.lineTo(13.2f, 13.2f); path.lineTo(8.5f, 15.5f); path.lineTo(10.8f, 10.8f); path.close();
                break;
            case DROP:
                path.moveTo(12, 3.5f); path.cubicTo(8, 9, 5.5f, 12, 5.5f, 14.5f);
                path.cubicTo(5.5f, 18.5f, 8.5f, 20.5f, 12, 20.5f); path.cubicTo(15.5f, 20.5f, 18.5f, 18.5f, 18.5f, 14.5f);
                path.cubicTo(18.5f, 12, 16, 9, 12, 3.5f);
                break;
            case PILL:
                canvas.save(); canvas.rotate(-45, 12, 12);
                canvas.drawRoundRect(new RectF(4, 8.5f, 20, 15.5f), 3.5f, 3.5f, p);
                canvas.drawLine(12, 8.5f, 12, 15.5f, p);
                canvas.restore();
                break;
            case STAR:
                for (int i = 0; i < 5; i++) {
                    double a1 = Math.toRadians(-90 + i * 72), a2 = Math.toRadians(-90 + i * 72 + 36);
                    float x1 = (float) (12 + 8.5 * Math.cos(a1)), y1 = (float) (12.5 + 8.5 * Math.sin(a1));
                    float x2 = (float) (12 + 3.8 * Math.cos(a2)), y2 = (float) (12.5 + 3.8 * Math.sin(a2));
                    if (i == 0) path.moveTo(x1, y1); else path.lineTo(x1, y1);
                    path.lineTo(x2, y2);
                }
                path.close();
                break;
            case PLUS:
                path.moveTo(12, 5); path.lineTo(12, 19); path.moveTo(5, 12); path.lineTo(19, 12);
                break;
            case CHEVRON:
                path.moveTo(9, 5); path.lineTo(16, 12); path.lineTo(9, 19);
                break;
            case BELL:
                path.moveTo(6, 16.5f); path.lineTo(6, 11); path.cubicTo(6, 7.5f, 8.5f, 5, 12, 5); path.cubicTo(15.5f, 5, 18, 7.5f, 18, 11);
                path.lineTo(18, 16.5f); path.lineTo(19.5f, 18); path.lineTo(4.5f, 18); path.close();
                path.moveTo(10, 20.5f); path.lineTo(14, 20.5f);
                break;
            case GEAR:
                canvas.drawCircle(12, 12, 3, p);
                for (int k = 0; k < 8; k++) {
                    double ang = Math.PI * k / 4;
                    path.moveTo((float) (12 + 6 * Math.cos(ang)), (float) (12 + 6 * Math.sin(ang)));
                    path.lineTo((float) (12 + 8.5 * Math.cos(ang)), (float) (12 + 8.5 * Math.sin(ang)));
                }
                canvas.drawCircle(12, 12, 6, p);
                break;
        }
        canvas.drawPath(path, p);
        canvas.restore();
    }

    private void round(Canvas c, float l, float t, float r, float b) { c.drawRoundRect(new RectF(l, t, r, b), 1.5f, 1.5f, p); }

    private void dots(Canvas c, float[] xy) {
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < xy.length; i += 2) c.drawCircle(xy[i], xy[i + 1], 1.1f, p);
        p.setStyle(Paint.Style.STROKE);
    }
}
