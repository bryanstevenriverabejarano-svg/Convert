package salve.presentation.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import salve.core.compute.ConstellationModel;
import salve.core.compute.ConstellationModel.State;

/**
 * Constelación de nodos: el móvil como núcleo cian que emite pulsos y los nodos alrededor,
 * unidos por enlaces del color de su estado. Solo dibuja; los datos los pone la pantalla.
 */
public final class NodeConstellationView extends View {
    public interface Listener { void onNodeTapped(int index); }

    public static final class Item {
        public final String name, location;
        public final State state;
        public Item(String name, String location, State state) {
            this.name = name; this.location = location == null ? "" : location; this.state = state;
        }
    }

    private static final int CYAN = 0xFF22D3EE, CYAN_SOFT = 0xFF67E8F9, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8;
    private static final int GREEN = 0xFF34D399, AMBER = 0xFFFBBF24, RED = 0xFFF87171, SLATE = 0xFF475569;
    private static final long PULSE_MS = 2600;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint link = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final DashPathEffect dashed;
    private final android.graphics.Path linkPath = new android.graphics.Path();
    private final float density;
    private final float[][] stars = new float[70][3];
    private List<Item> items = Collections.emptyList();
    private float[][] points = new float[0][2];
    private float cx, cy, orbit;
    private Listener listener;
    private Paint background;

    public NodeConstellationView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        stroke.setStyle(Paint.Style.STROKE);
        link.setStyle(Paint.Style.STROKE);
        link.setStrokeWidth(1.6f * density);
        dashed = new DashPathEffect(new float[]{6 * density, 6 * density}, 0);
        text.setColor(INK);
        text.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sub.setColor(MUTED);
        sub.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        sub.setTextAlign(Paint.Align.CENTER);
        Random random = new Random(7); // estrellas fijas: el fondo no parpadea entre aperturas
        for (float[] star : stars) { star[0] = random.nextFloat(); star[1] = random.nextFloat(); star[2] = 0.3f + random.nextFloat(); }
        setClickable(true);
        setFocusable(true);
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setItems(List<Item> value) {
        items = value == null ? Collections.emptyList() : new ArrayList<>(value);
        layoutPoints();
        StringBuilder description = new StringBuilder("Constelación: este móvil en el centro");
        for (Item item : items) description.append("; ").append(item.name).append(", ").append(ConstellationModel.label(item.state));
        setContentDescription(description);
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        cx = w / 2f; cy = h / 2f;
        orbit = Math.min(w, h) * 0.34f;
        background = new Paint();
        background.setShader(new RadialGradient(cx, cy, Math.max(w, h) * 0.75f,
                new int[]{0xFF0A2235, 0xFF050E1A, 0xFF02060C}, new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        layoutPoints();
    }

    private void layoutPoints() { points = ConstellationModel.positions(items.size(), cx, cy, orbit); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean animate = ValueAnimator.areAnimatorsEnabled();
        float t = animate ? (SystemClock.uptimeMillis() % PULSE_MS) / (float) PULSE_MS : 0.35f;
        if (background != null) canvas.drawRect(0, 0, getWidth(), getHeight(), background);
        drawStars(canvas);

        // Órbita
        stroke.setColor(CYAN); stroke.setAlpha(38); stroke.setStrokeWidth(1f * density); stroke.setPathEffect(null);
        canvas.drawCircle(cx, cy, orbit, stroke);

        // Pulsos del núcleo: tres ondas que se expanden hasta la órbita
        float core = 20 * density;
        for (int i = 0; i < 3; i++) {
            float phase = (t + i / 3f) % 1f;
            stroke.setColor(CYAN); stroke.setStrokeWidth(2f * density);
            stroke.setAlpha((int) (150 * (1f - phase)));
            canvas.drawCircle(cx, cy, core + (orbit - core) * phase, stroke);
        }

        // Enlaces y paquetes de datos hacia los nodos activos
        for (int i = 0; i < points.length; i++) {
            State state = items.get(i).state;
            link.setColor(color(state));
            link.setAlpha(state == State.DISABLED ? 50 : state == State.CHECKING ? 90 : 150);
            link.setPathEffect(state == State.OFFLINE || state == State.DISABLED || state == State.CHECKING ? dashed : null);
            // Con trazo discontinuo se dibuja como Path: drawLine no aplica el patrón con aceleración en Android 8.
            linkPath.reset(); linkPath.moveTo(cx, cy); linkPath.lineTo(points[i][0], points[i][1]);
            canvas.drawPath(linkPath, link);
            if (state == State.ACTIVE) {
                float f = (t * 1.5f + i * 0.21f) % 1f;
                fill.setColor(CYAN_SOFT); fill.setAlpha(230);
                canvas.drawCircle(cx + (points[i][0] - cx) * f, cy + (points[i][1] - cy) * f, 2.6f * density, fill);
            }
        }

        // Núcleo: halo y centro
        for (int r = 3; r >= 1; r--) {
            fill.setColor(CYAN); fill.setAlpha(28 * (4 - r));
            canvas.drawCircle(cx, cy, core + r * 6 * density, fill);
        }
        fill.setColor(CYAN); fill.setAlpha(255);
        canvas.drawCircle(cx, cy, core, fill);
        fill.setColor(0xFF04131C);
        canvas.drawCircle(cx, cy, core * 0.42f, fill);
        canvas.drawText("Este móvil", cx, cy + core + 22 * density, text);

        // Nodos
        float node = 13 * density;
        for (int i = 0; i < points.length; i++) {
            Item item = items.get(i);
            float x = points[i][0], y = points[i][1];
            int c = color(item.state);
            fill.setColor(c); fill.setAlpha(item.state == State.DISABLED ? 70 : 60);
            canvas.drawCircle(x, y, node * (item.state == State.ACTIVE ? 1.9f + 0.25f * (float) Math.sin(t * 2 * Math.PI) : 1.7f), fill);
            fill.setAlpha(item.state == State.DISABLED ? 120 : 255);
            canvas.drawCircle(x, y, node, fill);
            stroke.setColor(CYAN); stroke.setAlpha(200); stroke.setStrokeWidth(1.5f * density);
            canvas.drawCircle(x, y, node + 3 * density, stroke);
            float below = y + node + 20 * density;
            canvas.drawText(ellipsize(item.name, text), x, below, text);
            String second = item.location.isEmpty() ? ConstellationModel.label(item.state)
                    : item.location + " · " + ConstellationModel.label(item.state);
            canvas.drawText(ellipsize(second, sub), x, below + 16 * density, sub);
        }

        if (items.isEmpty()) {
            canvas.drawText("Aún no hay nodos", cx, cy + orbit * 0.75f, text);
            canvas.drawText("Añade uno desde Nodos de cómputo → Añadir un nodo", cx, cy + orbit * 0.75f + 18 * density, sub);
        }
        if (animate && isAttachedToWindow()) postInvalidateOnAnimation();
    }

    private void drawStars(Canvas canvas) {
        fill.setColor(CYAN_SOFT);
        for (float[] star : stars) {
            fill.setAlpha((int) (40 * star[2]));
            canvas.drawCircle(star[0] * getWidth(), star[1] * getHeight(), star[2] * density, fill);
        }
    }

    private String ellipsize(String value, Paint paint) {
        float max = Math.max(60 * density, orbit * 0.9f);
        return TextUtils.ellipsize(value, new android.text.TextPaint(paint), max, TextUtils.TruncateAt.END).toString();
    }

    private static int color(State state) {
        switch (state) {
            case ACTIVE: return GREEN;
            case BUSY: return AMBER;
            case OFFLINE: return RED;
            case DISABLED: return SLATE;
            default: return CYAN_SOFT;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int index = ConstellationModel.hit(points, event.getX(), event.getY(), 36 * density);
            if (index >= 0 && listener != null) { performClick(); listener.onNodeTapped(index); return true; }
        }
        return event.getAction() == MotionEvent.ACTION_DOWN || super.onTouchEvent(event);
    }

    @Override public boolean performClick() { return super.performClick(); }
}
