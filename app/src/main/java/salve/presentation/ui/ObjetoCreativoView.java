package salve.presentation.ui;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;

import salve.core.ObjetoCreativo;
import salve.core.ParametricGlyph;

public class ObjetoCreativoView extends View {

    private final Paint paint;
    private ObjetoCreativo objeto;
    private float aura01 = 0f;
    private float breath01 = 0f;
    private float baseStrokeDp = 2.5f;
    private float maxExtraStrokeDp = 6.0f;
    private ValueAnimator breathAnimator;

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

    public void setAura(float aura01) {
        this.aura01 = clamp01(aura01);
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startBreathing();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopBreathing();
        super.onDetachedFromWindow();
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
                paint.setStyle(Paint.Style.FILL);
                canvas.drawCircle(centerX, centerY, half, paint);
                break;
            case CUADRADO:
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(centerX - half, centerY - half, centerX + half, centerY + half, paint);
                break;
            case TRIANGULO:
                paint.setStyle(Paint.Style.FILL);
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
                float strokePx = dpToPx(baseStrokeDp + maxExtraStrokeDp * aura01 * (0.5f + 0.5f * breath01));
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(strokePx);
                Path glyph = ParametricGlyph.build(objeto.getSeed(), style, sizePx);
                float scale = 0.97f + 0.06f * breath01;
                canvas.save();
                canvas.translate(centerX, centerY);
                canvas.scale(scale, scale);
                canvas.drawPath(glyph, paint);
                canvas.restore();
                break;
            default:
                break;
        }
    }

    private void startBreathing() {
        if (breathAnimator != null) {
            return;
        }
        breathAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
        breathAnimator.setDuration(2800);
        breathAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        breathAnimator.setRepeatCount(ValueAnimator.INFINITE);
        breathAnimator.addUpdateListener(animation -> {
            breath01 = (float) animation.getAnimatedValue();
            invalidate();
        });
        breathAnimator.start();
    }

    private void stopBreathing() {
        if (breathAnimator != null) {
            breathAnimator.cancel();
            breathAnimator = null;
        }
    }

    private float dpToPx(float dp) {
        float density = getResources().getDisplayMetrics().density;
        return dp * density;
    }

    private float clamp01(float value) {
        if (value < 0f) {
            return 0f;
        }
        if (value > 1f) {
            return 1f;
        }
        return value;
    }
}
