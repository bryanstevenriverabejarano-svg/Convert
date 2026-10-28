package salve.sketch;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import java.util.WeakHashMap;

/**
 * Dibuja una {@link SketchAnimation} en un Canvas con la estética del cuerpo astral: líneas de luz
 * cian con halo, y mientras traza, la cadena de epiciclos girando hasta la punta de la pluma.
 */
public final class SketchPainter {
    private static final int EPICYCLES_SHOWN = 48;
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arm = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tip = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final Path partial = new Path();
    private final float[] pen = new float[2];
    private final float[] chain = new float[2 * (EPICYCLES_SHOWN + 2)];
    private final WeakHashMap<SketchAnimation, Path[]> finished = new WeakHashMap<>();

    public SketchPainter(float density) {
        this.density = density;
        for (Paint p : new Paint[]{glow, core, circle, arm}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        glow.setColor(0xFF36C8FF);
        core.setColor(0xFFD8FBFF);
        circle.setColor(0xFF7FD8FF);
        arm.setColor(0xFFB8EEFF);
        tip.setColor(0xFFFFFFFF);
    }

    /**
     * @param seconds tiempo desde que empezó el dibujo
     * @param alpha   opacidad global 0..1 (para desvanecer)
     * @param epicycles si se muestran los círculos mientras traza
     */
    public void draw(Canvas canvas, SketchAnimation animation, float seconds, RectF box, float alpha, boolean epicycles) {
        if (animation == null || box.width() <= 0 || box.height() <= 0 || alpha <= 0) return;
        SketchScene scene = animation.scene;
        float scale = Math.min(box.width() / scene.width, box.height() / scene.height);
        // El trazo se ve igual de fino en lienzos pequeños y grandes.
        float unit = 1f / Math.max(1e-4f, scale);
        canvas.save();
        canvas.translate(box.centerX() - scene.width * scale / 2, box.centerY() - scene.height * scale / 2);
        canvas.scale(scale, scale);
        glow.setStrokeWidth(6.5f * density * unit);
        core.setStrokeWidth(1.7f * density * unit);
        circle.setStrokeWidth(.8f * density * unit);
        arm.setStrokeWidth(1f * density * unit);
        int a = Math.round(255 * Math.max(0, Math.min(1, alpha)));
        glow.setAlpha(a * 70 / 255);
        core.setAlpha(a);
        Path[] done = paths(animation);
        int current = animation.strokeAt(seconds);
        for (int i = 0; i < Math.min(current, done.length); i++) {
            weigh(animation.weight(i), unit);
            canvas.drawPath(done[i], glow);
            canvas.drawPath(done[i], core);
        }
        if (current < animation.strokes.size()) {
            weigh(animation.weight(current), unit);
            FourierStroke stroke = animation.strokes.get(current);
            float progress = animation.progress(current, seconds);
            float[] pts = stroke.points();
            int n = pts.length / 2;
            float exact = progress * (n - 1);
            int whole = (int) exact;
            partial.rewind();
            partial.moveTo(pts[0], pts[1]);
            for (int k = 1; k <= whole && k < n; k++) partial.lineTo(pts[2 * k], pts[2 * k + 1]);
            float turn = progress * stroke.visibleTurn;
            stroke.point(turn, stroke.terms(), pen);
            partial.lineTo(pen[0], pen[1]);
            canvas.drawPath(partial, glow);
            canvas.drawPath(partial, core);
            if (epicycles) {
                int count = stroke.chain(turn, EPICYCLES_SHOWN, chain);
                circle.setAlpha(a * 90 / 255);
                arm.setAlpha(a * 150 / 255);
                for (int i = 1; i < count - 1; i++) {
                    float r = stroke.radius(i);
                    if (r * scale < .8f * density) continue;
                    canvas.drawCircle(chain[2 * i], chain[2 * i + 1], r, circle);
                    canvas.drawLine(chain[2 * i], chain[2 * i + 1], chain[2 * i + 2], chain[2 * i + 3], arm);
                }
                tip.setAlpha(a);
                canvas.drawCircle(pen[0], pen[1], 3.2f * density * unit, tip);
                glow.setAlpha(a * 120 / 255);
                canvas.drawCircle(pen[0], pen[1], 4f * density * unit, glow);
            }
        }
        canvas.restore();
    }

    /** El contorno con más cuerpo y los detalles más finos. */
    private void weigh(float w, float unit) {
        glow.setStrokeWidth(6.5f * density * unit * w);
        core.setStrokeWidth(1.7f * density * unit * w);
    }

    private Path[] paths(SketchAnimation animation) {
        Path[] cached = finished.get(animation);
        if (cached != null) return cached;
        cached = new Path[animation.strokes.size()];
        for (int i = 0; i < cached.length; i++) {
            float[] p = animation.strokes.get(i).points();
            Path path = new Path();
            path.moveTo(p[0], p[1]);
            for (int k = 2; k < p.length; k += 2) path.lineTo(p[k], p[k + 1]);
            cached[i] = path;
        }
        finished.put(animation, cached);
        return cached;
    }
}
