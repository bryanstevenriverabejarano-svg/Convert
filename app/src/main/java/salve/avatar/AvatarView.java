package salve.avatar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Small articulated 2D character rendered locally. Clothes and furniture are layered geometry. */
public final class AvatarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
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
        super.onAttachedToWindow(); attached = true; store.addListener(changed); restartFrames();
    }
    @Override protected void onDetachedFromWindow() {
        attached = false; removeCallbacks(frame); store.removeListener(changed); store.save();
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
                + (s.getOutfit() == AvatarState.Outfit.PAJAMAS ? ", con pijama" : ", con vestido")
                + ". Usa los botones para cambiar su estado.");
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (overlay) return super.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // A host screen may use the character as the entry point to the room.
            if (hasOnClickListeners()) { performClick(); return true; }
            float scale = Math.min(getWidth() / 320f, getHeight() / 280f);
            float left = (getWidth() - 320f * scale) / 2f;
            float target = ((event.getX() - left) / Math.max(.001f, scale) - 70f) / 180f;
            store.change(s -> s.walkTo(target));
            performClick(); return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        AvatarState s = store.state();
        float scale = Math.min(getWidth() / 320f, getHeight() / 280f);
        canvas.save();
        canvas.translate((getWidth() - 320f * scale) / 2f, (getHeight() - 280f * scale) / 2f);
        canvas.scale(scale, scale);
        if (!overlay) drawRoom(canvas);
        float floor = 248f;
        float bedX = overlay ? 160f : 70f + AvatarState.BED_X * 180f;
        if (s.hasBed()) drawBed(canvas, bedX, floor, false, s.getAccent());
        if (s.getPose() == AvatarState.Pose.SLEEPING) {
            canvas.save();
            canvas.translate(bedX - 74f, floor - 38f);
            canvas.rotate(-90f);
            canvas.scale(.63f, .63f);
            drawCharacter(canvas, s, 0f, true);
            canvas.restore();
            drawBed(canvas, bedX, floor, true, s.getAccent());
            text(canvas, "z", bedX + 48, floor - 72 - (float) Math.sin(phase) * 3, 15, 0xFFA8DADD);
            text(canvas, "z", bedX + 61, floor - 90, 11, 0xFFA8DADD);
        } else {
            float x = overlay ? 160f : 70f + s.getX() * 180f;
            float stride = s.getPose() == AvatarState.Pose.WALKING ? (float) Math.sin(phase * 10f) : 0f;
            canvas.save();
            canvas.translate(x, floor - 173f - Math.abs(stride) * 2f);
            if (s.getPose() == AvatarState.Pose.WALKING && !s.isFacingRight()) canvas.scale(-1, 1);
            drawCharacter(canvas, s, stride, false);
            canvas.restore();
        }
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
    private void drawCharacter(Canvas c, AvatarState s, float stride, boolean asleep) {
        final int skin = 0xFFFFDDC9, hair = 0xFFF7F5EF, dark = 0xFF162B32, accent = s.getAccent();
        float breath = asleep ? (float) Math.sin(phase * 1.8f) * .6f : (float) Math.sin(phase * 2f) * .5f;
        oval(c, -37, 15, 38, 128, 0xFFDADFE4);
        // Legs rotate independently around their hips, rather than sliding a single image.
        limb(c, -12, 120, stride * 23, 49, s.getOutfit() == AvatarState.Outfit.PAJAMAS ? accent : skin, dark, true);
        limb(c, 12, 120, -stride * 23, 49, s.getOutfit() == AvatarState.Outfit.PAJAMAS ? accent : skin, dark, true);
        limb(c, -23, 82, -stride * 26 - 8, 39, s.getOutfit() == AvatarState.Outfit.PAJAMAS ? accent : hair, skin, false);
        limb(c, 23, 82, stride * 26 + 8, 39, s.getOutfit() == AvatarState.Outfit.PAJAMAS ? accent : hair, skin, false);
        rounded(c, -8, 62, 8, 85, 5, skin);
        if (s.getOutfit() == AvatarState.Outfit.DAY) {
            polygon(c, dark, -23,78, 23,78, 35,132, -35,132);
            line(c, -32, 128, 32, 128, 4, accent);
            line(c, 0, 80, 0, 125, 3, accent);
            rounded(c, -27, 105, 27, 111, 3, dark);
            circle(c, 0, 108, 8, accent); circle(c, 0, 108, 5, dark);
            polygon(c, hair, -19,77, -2,87, -6,74);
            polygon(c, hair, 19,77, 2,87, 6,74);
        } else {
            rounded(c, -25, 78, 25, 124, 9, accent);
            line(c, 0, 79, 0, 121, 2, lighten(accent));
            circle(c, 3, 91, 1.5f, Color.WHITE); circle(c, 3, 103, 1.5f, Color.WHITE);
        }
        if (s.getPattern() == AvatarState.Pattern.STRIPES) {
            for (int y = 88; y < 123; y += 10) line(c, -18, y, 18, y, 2, 0x88FFFFFF);
        } else if (s.getPattern() == AvatarState.Pattern.STARS) {
            for (int i = 0; i < 4; i++) {
                float sx = i % 2 == 0 ? -13 : 13, sy = 88 + i * 10;
                line(c, sx-3,sy,sx+3,sy,1.5f,Color.WHITE); line(c,sx,sy-3,sx,sy+3,1.5f,Color.WHITE);
            }
        }
        c.save(); c.translate(0, breath);
        oval(c, -31, 9, 31, 76, skin);
        // White fringe and long side locks retain Salve's existing visual identity.
        polygon(c, hair, -35,40, -32,17, -16,4, 12,3, 30,17, 35,40, 20,26, 11,43, 2,22, -9,41, -20,23);
        polygon(c, hair, -31,27, -22,36, -27,90, -43,121, -35,72);
        polygon(c, hair, 31,27, 22,36, 27,90, 43,121, 35,72);
        if (asleep) {
            line(c, -20, 52, -7, 54, 2, dark); line(c, 7, 54, 20, 52, 2, dark);
        } else {
            oval(c, -22, 43, -6, 62, Color.WHITE); oval(c, 6, 43, 22, 62, Color.WHITE);
            oval(c, -19, 44, -9, 61, 0xFF27BEE6); oval(c, 9, 44, 19, 61, 0xFF27BEE6);
            oval(c, -16, 47, -12, 59, 0xFF164A67); oval(c, 12, 47, 16, 59, 0xFF164A67);
            circle(c,-13,47,2.2f,Color.WHITE); circle(c,15,47,2.2f,Color.WHITE);
            line(c,-22,43,-6,43,1.5f,dark); line(c,6,43,22,43,1.5f,dark);
        }
        oval(c,-26,60,-15,65,0x66F795A1); oval(c,15,60,26,65,0x66F795A1);
        line(c,-3,67,3,67,1.5f,0xFFB77575);
        c.restore();
    }
    private void limb(Canvas c, float x, float y, float degrees, float length, int color, int end, boolean leg) {
        c.save(); c.translate(x,y); c.rotate(degrees);
        rounded(c,-6,0,6,length,5,color);
        rounded(c,-7,length-6,leg ? 12 : 7,length+3,4,end);
        if (leg) line(c,-5,length-7,7,length-7,2,store.state().getAccent());
        c.restore();
    }
    private void color(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void rounded(Canvas c,float l,float t,float r,float b,float radius,int color) { color(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void oval(Canvas c,float l,float t,float r,float b,int color) { color(color); c.drawOval(l,t,r,b,paint); }
    private void circle(Canvas c,float x,float y,float radius,int color) { color(color); c.drawCircle(x,y,radius,paint); }
    private void line(Canvas c,float x,float y,float xx,float yy,float width,int color) {
        color(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x,y,xx,yy,paint);
    }
    private void polygon(Canvas c,int color,float... points) {
        color(color); path.reset(); path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);
        path.close(); c.drawPath(path,paint);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color) { color(color); paint.setTextSize(size); c.drawText(value,x,y,paint); }
    private int lighten(int color) { return Color.rgb((Color.red(color)+255)/2,(Color.green(color)+255)/2,(Color.blue(color)+255)/2); }
}
