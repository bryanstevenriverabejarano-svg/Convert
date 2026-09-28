package salve.avatar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.GestureDetector;
import android.view.View;

/** Shared room/overlay host for the approved illustrated character and its local motion rig. */
public final class AvatarView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final IllustratedAvatarRenderer portrait;
    private final IllustratedBedRenderer bed;
    private final AvatarMotionController motion = AvatarMotionController.get();
    private final AvatarStore store;
    private final AvatarWardrobeStore wardrobe;
    private static final AvatarLocomotion locomotion = new AvatarLocomotion();
    private final GestureDetector stageGestures;
    private boolean immersive;
    private boolean overlay, animationEnabled = true, attached;
    private Runnable frameListener;
    private float phase;
    private final Runnable changed = () -> { updateDescription(); invalidate(); };
    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!attached || !animationEnabled || !isShown() || getWindowVisibility() != VISIBLE) return;
            long now = SystemClock.uptimeMillis();
            store.advance(now);
            locomotion.advance(now,store.state().getX(),store.state().getPose()==AvatarState.Pose.WALKING);
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
        wardrobe = AvatarWardrobeStore.get(context);
        portrait = new IllustratedAvatarRenderer(context);
        bed = new IllustratedBedRenderer(context);
        stageGestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onDoubleTap(MotionEvent event) { performClick(); return true; }
            @Override public void onLongPress(MotionEvent event) { performLongClick(); }
        });
        setFocusable(true);
        setClickable(true);
        updateDescription();
    }
    public void setImmersiveMode(boolean value) { immersive = value; updateDescription(); invalidate(); }
    public void setOverlayMode(boolean value) { overlay = value; invalidate(); }
    public void setFrameListener(Runnable listener) { frameListener = listener; }
    public void setAnimationEnabled(boolean value) { animationEnabled = value; restartFrames(); }
    private void restartFrames() {
        if (frame == null) return;
        removeCallbacks(frame);
        if (attached && animationEnabled && isShown() && getWindowVisibility() == VISIBLE) post(frame);
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); attached = true; store.addListener(changed); motion.addListener(changed);
        wardrobe.addListener(changed);portrait.addListener(changed);restartFrames();
    }
    @Override protected void onDetachedFromWindow() {
        attached = false; removeCallbacks(frame); store.removeListener(changed); motion.removeListener(changed);
        wardrobe.removeListener(changed);portrait.removeListener(changed);store.save();
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
                + (immersive ? ". Doble toque para conversar; mantén pulsado para opciones. " : ", avatar ilustrado. ")
                + (motion.snapshot().speaking ? "Hablando." : motion.snapshot().listening ? "Escuchando." : ""));
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (overlay) return super.onTouchEvent(event);
        if (immersive) {
            stageGestures.onTouchEvent(event);
            return true;
        }
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
        portrait.selectWardrobe(wardrobe.selected());
        AvatarMotion.Snapshot expression = motion.snapshot();
        if (immersive && !overlay) { drawStage(canvas, s, expression); return; }
        float worldWidth = overlay ? 220f : 320f;
        float scale = Math.min(getWidth() / worldWidth, getHeight() / 280f);
        canvas.save();
        canvas.translate((getWidth() - worldWidth * scale) / 2f, (getHeight() - 280f * scale) / 2f);
        canvas.scale(scale, scale);
        if (!overlay) drawRoom(canvas);
        float floor = 252f;
        float bedX = overlay ? 110f : 206f;
        if (s.hasBed()) bed.draw(canvas, bedX, floor, false);
        if (s.getPose() == AvatarState.Pose.SLEEPING) {
            canvas.save();
            canvas.translate(bedX + 90f, floor - 41f);
            canvas.rotate(-90f);
            drawPortrait(canvas, expression, 171f, 0, true);
            canvas.restore();
            bed.draw(canvas, bedX, floor, true);
            text(canvas, "z", bedX + 48, floor - 72 - (float) Math.sin(phase) * 3, 15, 0xFFA8DADD);
            text(canvas, "z", bedX + 61, floor - 90, 11, 0xFFA8DADD);
        } else {
            float x = overlay ? 110f : 78f + s.getX() * 164f;
            float stride = locomotion.stride();
            canvas.save();
            canvas.translate(x, floor - locomotion.lift() * .7f);
            // A frontal drawing keeps its actual handedness; direction is scene movement, not a flip.
            drawPortrait(canvas, expression, overlay ? 244f : 226f, stride, false);
            canvas.restore();
        }
        canvas.restore();
    }
    private void drawStage(Canvas canvas, AvatarState state, AvatarMotion.Snapshot expression) {
        AvatarStageLayout layout = AvatarStageLayout.fit(getWidth(), getHeight(), state.getX());
        if (layout.height <= 0) return;
        boolean sleeping = state.getPose() == AvatarState.Pose.SLEEPING;
        if (state.hasBed()) {
            canvas.save();
            canvas.translate(layout.bedCenterX, layout.bedFloor(getHeight(), sleeping));
            canvas.scale(layout.bedScale, layout.bedScale);
            bed.draw(canvas, 0, 0, false);
            if (sleeping) {
                canvas.save(); canvas.translate(90, -41); canvas.rotate(-90);
                drawPortrait(canvas, expression, 171, 0, true);
                canvas.restore();
                bed.draw(canvas, 0, 0, true);
                text(canvas, "z", 48, -72 - (float) Math.sin(phase) * 3, 15, 0xFFA8DADD);
                text(canvas, "z", 61, -90, 11, 0xFFA8DADD);
            }
            canvas.restore();
        }
        if (!sleeping) {
            // A quiet contact shadow replaces the permanent room furniture.
            float radius = layout.height * .14f;
            oval(canvas, layout.centerX - radius, layout.floor - 6,
                    layout.centerX + radius, layout.floor + 2, 0x28000000);
            canvas.save();
            canvas.translate(layout.centerX, layout.floor - locomotion.lift() * layout.height / 330f);
            drawPortrait(canvas, expression, layout.height, locomotion.stride(), false);
            canvas.restore();
        }
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
    private void color(int color) { paint.setColor(color); paint.setStyle(Paint.Style.FILL); }
    private void rounded(Canvas c,float l,float t,float r,float b,float radius,int color) { color(color); c.drawRoundRect(l,t,r,b,radius,radius,paint); }
    private void oval(Canvas c,float l,float t,float r,float b,int color) { color(color); c.drawOval(l,t,r,b,paint); }
    private void circle(Canvas c,float x,float y,float radius,int color) { color(color); c.drawCircle(x,y,radius,paint); }
    private void line(Canvas c,float x,float y,float xx,float yy,float width,int color) {
        color(color); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x,y,xx,yy,paint);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color) { color(color); paint.setTextSize(size); c.drawText(value,x,y,paint); }
}
