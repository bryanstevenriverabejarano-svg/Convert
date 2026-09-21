package salve.avatar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Shared room/overlay host for the approved illustrated character and its local motion rig. */
public final class AvatarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final IllustratedAvatarRenderer portrait;
    private final AvatarMotionController motion = AvatarMotionController.get();
    private final AvatarStore store;
    private boolean overlay, animationEnabled = true, attached;
    private Runnable frameListener;
    private float phase;
    private final Runnable changed = () -> { updateDescription(); invalidate(); };
    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!attached || !animationEnabled || !isShown() || getWindowVisibility() != VISIBLE) return;
            long now = SystemClock.uptimeMillis();
            store.advance(now);
            motion.advance(now);
            phase = (now % 60000L) / 1000f;
            if (frameListener != null) frameListener.run();
            invalidate();
            postDelayed(this, 33);
        }
    };

    public AvatarView(Context context) { this(context, null); }
    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        store = AvatarStore.get(context);
        portrait = new IllustratedAvatarRenderer(context);
        setFocusable(true);
        setClickable(true);
        updateDescription();
    }
    public void setOverlayMode(boolean value) { overlay = value; invalidate(); }
    public void setFrameListener(Runnable listener) { frameListener = listener; }
    public void setAnimationEnabled(boolean value) { animationEnabled = value; restartFrames(); }
    private void restartFrames() {
        if (frame == null) return;
        removeCallbacks(frame);
        if (attached && animationEnabled && isShown() && getWindowVisibility() == VISIBLE) post(frame);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); attached = true; store.addListener(changed); motion.addListener(changed); restartFrames();
    }
    @Override protected void onDetachedFromWindow() {
        attached = false; removeCallbacks(frame); store.removeListener(changed); motion.removeListener(changed); store.save();
        super.onDetachedFromWindow();
    }
    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility); restartFrames();
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility); restartFrames();
    }
    private void updateDescription() {
        AvatarState s = store.state();
        setContentDescription("Salve, " + (s.getPose() == AvatarState.Pose.SLEEPING ? "dormida en su cama"
                : s.getPose() == AvatarState.Pose.WALKING ? "caminando" : "despierta")
                + ", con su ilustración original. "
                + (motion.snapshot().speaking ? "Hablando." : motion.snapshot().listening ? "Escuchando." : ""));
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (overlay) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // A host screen may use the character as the entry point to the room.
            if (hasOnClickListeners()) { performClick(); return true; }
            float scale = Math.min(getWidth() / 320f, getHeight() / 280f);
            float left = (getWidth() - 320f * scale) / 2f;
            float target = ((event.getX() - left) / Math.max(.001f, scale) - 78f) / 164f;
            store.change(s -> s.walkTo(target));
            performClick(); return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        AvatarState s = store.state();
        AvatarMotion.Snapshot expression = motion.snapshot();
        float worldWidth = overlay ? 220f : 320f;
        float scale = Math.min(getWidth() / worldWidth, getHeight() / 280f);
        canvas.save();
        canvas.translate((getWidth() - worldWidth * scale) / 2f, (getHeight() - 280f * scale) / 2f);
        canvas.scale(scale, scale);
        if (!overlay) drawRoom(canvas);
        float floor = 252f;
        float bedX = overlay ? 110f : 78f + AvatarState.BED_X * 164f;
        if (s.hasBed()) drawBed(canvas, bedX, floor, false, s.getAccent());
        if (s.getPose() == AvatarState.Pose.SLEEPING) {
            canvas.save();
            canvas.translate(bedX + 68f, floor - 31f);
            canvas.rotate(-90f);
            drawPortrait(canvas, expression, 146f, 0, true);
            canvas.restore();
            drawBed(canvas, bedX, floor, true, s.getAccent());
            text(canvas, "z", bedX + 48, floor - 72 - (float) Math.sin(phase) * 3, 15, 0xFFA8DADD);
            text(canvas, "z", bedX + 61, floor - 90, 11, 0xFFA8DADD);
        } else {
            float x = overlay ? 110f : 78f + s.getX() * 164f;
            float stride = s.getPose() == AvatarState.Pose.WALKING ? (float) Math.sin(phase * 10f) : 0f;
            canvas.save();
            canvas.translate(x, floor - Math.abs(stride) * 1.1f);
            if (s.getPose() == AvatarState.Pose.WALKING && !s.isFacingRight()) canvas.scale(-1, 1);
            drawPortrait(canvas, expression, overlay ? 244f : 226f, stride, false);
            canvas.restore();
        }
        canvas.restore();
    }
    private void drawPortrait(Canvas canvas, AvatarMotion.Snapshot expression, float height, float stride, boolean sleeping) {
        canvas.save();
        canvas.translate(-height / 3f, -height);
        canvas.scale(height / 1536f, height / 1536f);
        portrait.draw(canvas, expression, stride, sleeping);
        canvas.restore();
    }
    private void drawRoom(Canvas c) {
        rounded(c, 0, 0, 320, 280, 22, 0xFF151E30);
        rounded(c, 24, 25, 118, 132, 18, 0xFF25374B);
        rounded(c, 30, 31, 112, 126, 14, 0xFF0E1629);
        circle(c, 89, 54, 13, 0xFFEEE3B2);
        circle(c, 95, 49, 12, 0xFF0E1629);
        circle(c, 49, 58, 1.5f, Color.WHITE); circle(c, 74, 92, 1.5f, Color.WHITE);
        line(c, 71, 30, 71, 126, 3, 0xFF314C5F);
        line(c, 30, 81, 112, 81, 3, 0xFF314C5F);
        rounded(c, 0, 226, 320, 280, 20, 0xFF202C3F);
        oval(c, 35, 233, 290, 268, 0xFF304859);
        rounded(c, 255, 100, 291, 145, 9, 0xFFBFA885);
        line(c, 273, 115, 273, 73, 3, 0xFF80B8A0);
        oval(c, 250, 75, 274, 92, 0xFF67A394);
        oval(c, 272, 63, 294, 81, 0xFF88BBA1);
    }
    private void drawBed(Canvas c, float x, float y, boolean blanket, int accent) {
        if (blanket) {
            rounded(c, x - 21, y - 47, x + 75, y - 12, 9, accent);
            rounded(c, x - 21, y - 47, x + 75, y - 36, 6, lighten(accent));
            line(c, x + 6, y - 32, x + 61, y - 32, 2, 0x558FFFFF);
            return;
        }
        rounded(c, x - 85, y - 53, x - 76, y + 4, 4, 0xFF57726B);
        rounded(c, x - 78, y - 27, x + 82, y - 5, 6, 0xFF7A9386);
        rounded(c, x - 72, y - 37, x + 77, y - 18, 8, 0xFFE5E2D9);
        rounded(c, x - 68, y - 47, x - 26, y - 30, 7, Color.WHITE);
        rounded(c, x + 71, y - 31, x + 80, y + 5, 4, 0xFF57726B);
        line(c, x - 68, y - 5, x - 68, y + 8, 5, 0xFF57726B);
        if (store.state().getPose() != AvatarState.Pose.SLEEPING)
            rounded(c, x - 14, y - 38, x + 70, y - 16, 5, accent);
    }
    private void color(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void rounded(Canvas c,float l,float t,float r,float b,float radius,int color) { color(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void oval(Canvas c,float l,float t,float r,float b,int color) { color(color); c.drawOval(l,t,r,b,paint); }
    private void circle(Canvas c,float x,float y,float radius,int color) { color(color); c.drawCircle(x,y,radius,paint); }
    private void line(Canvas c,float x,float y,float xx,float yy,float width,int color) {
        color(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x,y,xx,yy,paint);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color) { color(color); paint.setTextSize(size); c.drawText(value,x,y,paint); }
    private int lighten(int color) { return Color.rgb((Color.red(color)+255)/2,(Color.green(color)+255)/2,(Color.blue(color)+255)/2); }
}
