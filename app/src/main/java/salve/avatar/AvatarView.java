package salve.avatar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;

/** Shared 3D host for the conversation, room and floating companion. All content is bundled. */
public final class AvatarView extends FrameLayout {
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private static final String PAGE = ORIGIN + "/avatar3d/index.html";
    private final AvatarStore store;
    private final AvatarWardrobeStore wardrobe;
    private final AvatarMotionController motion = AvatarMotionController.get();
    private final GestureDetector gestures;
    private final TextView status;
    private WebView web;
    private IllustratedAvatarView fallback;
    private boolean attached, enabled = true, immersive, overlay, ready, failed, polling;
    private long loadStarted;
    private Runnable frameListener;
    private final Runnable changed = this::refresh;
    private final Runnable frame = new Runnable() {
        @Override public void run() {
            if (!active()) return;
            long now = SystemClock.uptimeMillis();
            store.advance(now);
            motion.advance(now);
            if (frameListener != null) frameListener.run();
            refresh();
            postDelayed(this, 33);
        }
    };

    public AvatarView(Context context) { this(context, null); }
    public AvatarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        store = AvatarStore.get(context);
        wardrobe = AvatarWardrobeStore.get(context);
        status = new TextView(context);
        status.setText("Cargando a Salve en 3D…");
        status.setTextColor(Color.rgb(165, 225, 235));
        status.setGravity(android.view.Gravity.CENTER);
        status.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(status, new LayoutParams(-1, -1));
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent event) { return true; }
            @Override public boolean onDoubleTap(MotionEvent event) { performClick(); return true; }
            @Override public void onLongPress(MotionEvent event) { performLongClick(); }
        });
        setFocusable(true);
        setClickable(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createRenderer() {
        if (web != null || failed || !attached) return;
        try {
            WebView next = new WebView(getContext());
            web = next;
            next.setBackgroundColor(Color.TRANSPARENT);
            next.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            WebSettings settings = next.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setAllowContentAccess(false);
            settings.setAllowFileAccessFromFileURLs(false);
            settings.setAllowUniversalAccessFromFileURLs(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
            settings.setDomStorageEnabled(false);
            settings.setMediaPlaybackRequiresUserGesture(true);
            next.setWebViewClient(new WebViewClient() {
                @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
                @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    // Exact allowlist: no network fallback, file access, chat text or JavaScript interface.
                    Uri uri = request.getUrl();
                    String path = uri.getPath();
                    if ("GET".equals(request.getMethod()) && "https".equals(uri.getScheme())
                            && "appassets.androidplatform.net".equals(uri.getHost()) && uri.getPort() == -1
                            && uri.getQuery() == null && Avatar3dState.allowedAsset(path)) {
                        try {
                            String mime = path.endsWith(".html") ? "text/html" : path.endsWith(".js")
                                    ? "text/javascript" : "model/gltf-binary";
                            return new WebResourceResponse(mime, path.endsWith(".glb") ? null : "UTF-8",
                                    200, "OK", Collections.singletonMap("Cache-Control", "no-store"),
                                    getContext().getAssets().open(path.substring(1)));
                        } catch (IOException missing) { /* The page reports failure and keeps a visible fallback. */ }
                    }
                    return new WebResourceResponse("text/plain", "UTF-8", 403, "Forbidden",
                            Collections.emptyMap(), new ByteArrayInputStream(new byte[0]));
                }
                @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    if (web == view) failRenderer();
                    return true;
                }
            });
            next.setVisibility(INVISIBLE);
            addView(next, 0, new LayoutParams(-1, -1));
            loadStarted = SystemClock.uptimeMillis();
            next.loadUrl(PAGE);
        } catch (RuntimeException unavailable) { failRenderer(); }
    }

    private boolean active() { return attached && enabled && isShown() && getWindowVisibility() == VISIBLE; }
    private boolean use3d() {
        AvatarDesignSpec design = wardrobe.selected();
        // The supplied model only has the original Núcleo suit. Keep existing explicit wardrobe choices.
        return "core".equals(design.template) && design.palette == AvatarDesignSpec.Palette.ORIGINAL
                && design.pattern == AvatarDesignSpec.Pattern.NONE && store.state().getPose() != AvatarState.Pose.SLEEPING;
    }
    private void refresh() {
        if (store == null || status == null) return; // Framework visibility callbacks can precede field initialization.
        boolean want = use3d() && !failed;
        if (want && active()) createRenderer();
        if (web != null && want && active() && !polling) {
            polling = true;
            WebView current = web;
            String script = "window.Salve3D ? window.Salve3D.update("
                    + Avatar3dState.encode(store.state(), motion.snapshot(), overlay) + ") : 'loading'";
            current.evaluateJavascript(script, result -> {
                if (web != current) return;
                polling = false;
                if ("\"error\"".equals(result)) failRenderer();
                else if ("\"ready\"".equals(result) && !ready) { ready = true; refresh(); }
                else if (!ready && active() && SystemClock.uptimeMillis() - loadStarted > 60000) failRenderer();
            });
        }
        boolean show = want && ready;
        if (web != null) {
            web.setVisibility(show ? VISIBLE : INVISIBLE);
            if (!want || !active()) {
                web.evaluateJavascript("window.Salve3D && window.Salve3D.pause()", null);
                web.onPause();
            } else web.onResume();
        }
        boolean legacy = !want;
        if (legacy && fallback == null) {
            fallback = new IllustratedAvatarView(getContext());
            fallback.setImmersiveMode(immersive);
            fallback.setOverlayMode(overlay);
            fallback.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(fallback, 0, new LayoutParams(-1, -1));
        }
        if (fallback != null) {
            fallback.setVisibility(legacy ? VISIBLE : GONE);
            // Only change this when the state changes; restarting its timer on every frame would starve it.
            if (legacy != fallback.isActivated()) {
                fallback.setActivated(legacy);
                fallback.setAnimationEnabled(legacy && active());
            }
        }
        status.setVisibility(!show && !legacy ? VISIBLE : GONE);
        String description = "Salve, " + AvatarBodyCue.label(store.state().getPose())
                + (show ? ", avatar 3D. " : legacy ? ", avatar ilustrado. " : ", cargando 3D. ")
                + (motion.snapshot().speaking ? "Hablando. " : motion.snapshot().listening ? "Escuchando. " : "")
                + (immersive ? "Doble toque para conversar; mantén pulsado para opciones." : "");
        if (!description.contentEquals(getContentDescription() == null ? "" : getContentDescription())) setContentDescription(description);
    }
    private void failRenderer() {
        failed = true;
        releaseRenderer();
        refresh();
        announceForAccessibility("No se pudo cargar el 3D. Salve continúa en modo ilustrado.");
    }
    private void releaseRenderer() {
        WebView previous = web;
        web = null; ready = false; polling = false;
        if (previous != null) { removeView(previous); previous.destroy(); }
    }
    private void restart() {
        if (frame == null) return;
        removeCallbacks(frame);
        if (fallback != null) fallback.setAnimationEnabled(active() && fallback.getVisibility() == VISIBLE);
        if (active()) post(frame);
        else if (web != null) { web.evaluateJavascript("window.Salve3D && window.Salve3D.pause()", null); web.onPause(); }
    }
    public void setImmersiveMode(boolean value) { immersive = value; if (fallback != null) fallback.setImmersiveMode(value); refresh(); }
    public void setOverlayMode(boolean value) { overlay = value; if (fallback != null) fallback.setOverlayMode(value); refresh(); }
    public void setFrameListener(Runnable listener) { frameListener = listener; }
    public void setAnimationEnabled(boolean value) { enabled = value; restart(); }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow(); attached = true;
        store.addListener(changed); wardrobe.addListener(changed); motion.addListener(changed); restart();
    }
    @Override protected void onDetachedFromWindow() {
        attached = false; removeCallbacks(frame);
        store.removeListener(changed); wardrobe.removeListener(changed); motion.removeListener(changed);
        releaseRenderer(); store.save(); super.onDetachedFromWindow();
    }
    @Override protected void onVisibilityChanged(View changedView, int visibility) { super.onVisibilityChanged(changedView, visibility); restart(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); restart(); }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) { return true; }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (overlay) return super.onTouchEvent(event);
        if (immersive) { gestures.onTouchEvent(event); return true; }
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (!hasOnClickListeners()) store.change(s -> s.walkTo(event.getX() / Math.max(1f, getWidth())));
            performClick();
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
