package salve.presentation.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import salve.core.ObjetoCreativo;
import salve.core.ParametricGlyph;

public class ObjetoCreativoView extends View {

    private final Paint paint;
    private ObjetoCreativo objeto;

    public ObjetoCreativoView(Context context) {
        this(context, null);
    }

    public ObjetoCreativoView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ObjetoCreativoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
    }

    public void setObjeto(ObjetoCreativo objeto) {
        this.objeto = objeto;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (objeto == null) {
            return;
        }

        paint.setColor(objeto.getColorArgb());

        float density = getResources().getDisplayMetrics().density;
        float sizePx = objeto.getTamanoDp() * density;
        float half = sizePx / 2f;

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        switch (objeto.getForma()) {
            case CIRCULO:
                canvas.drawCircle(centerX, centerY, half, paint);
                break;
            case CUADRADO:
                canvas.drawRect(centerX - half, centerY - half, centerX + half, centerY + half, paint);
                break;
            case TRIANGULO:
                Path path = new Path();
                path.moveTo(centerX, centerY - half);
                path.lineTo(centerX - half, centerY + half);
                path.lineTo(centerX + half, centerY + half);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case GLIFO:
                ParametricGlyph.Style style = ParametricGlyph.Style.ORB;
                String styleRaw = objeto.getStyle();
                if (styleRaw != null) {
                    try {
                        style = ParametricGlyph.Style.valueOf(styleRaw.toUpperCase());
                    } catch (IllegalArgumentException ignored) {
                        style = ParametricGlyph.Style.ORB;
                    }
                }
                Path glyph = ParametricGlyph.build(objeto.getSeed(), style, sizePx);
                canvas.save();
                canvas.translate(centerX, centerY);
                canvas.drawPath(glyph, paint);
                canvas.restore();
                break;
            default:
                break;
        }
    }
}
