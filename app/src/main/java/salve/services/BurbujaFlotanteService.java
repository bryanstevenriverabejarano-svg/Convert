package salve.services;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import salve.avatar.AvatarState;
import salve.avatar.AvatarStore;
import salve.avatar.AvatarView;
import salve.presentation.ui.AvatarRoomActivity;

/** Explicitly started companion overlay. No ambient recording or automatic app interaction. */
public final class BurbujaFlotanteService extends Service {
    private static final String STOP = "salve.avatar.STOP";
    private static final String CHANNEL = "salve_companion";
    private static final int NOTIFICATION = 3102;
    private WindowManager windows;
    private WindowManager.LayoutParams params;
    private LinearLayout window;
    private AvatarView avatar;
    private AvatarStore store;
    private boolean receiverRegistered, added;
    private int minX, minY, maxX, maxY;
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { updateVisibility(); }
    };
    private final Runnable changed = this::positionFromState;

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (window != null) return START_NOT_STICKY;
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Activa Mostrar sobre otras apps desde la habitación de Salve.", Toast.LENGTH_LONG).show();
            stopSelf(); return START_NOT_STICKY;
        }
        try { showNotification(); createOverlay(); }
        catch (RuntimeException error) {
            Log.w("SalveAvatar", "No se pudo iniciar la superposición", error);
            Toast.makeText(this, "No pude mostrar el personaje flotante.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void showNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL, "Personaje flotante", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 1, new Intent(this, AvatarRoomActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent close = PendingIntent.getService(this, 2, new Intent(this, BurbujaFlotanteService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_myplaces).setContentTitle("Salve está en tu pantalla")
                .setContentText("Toca para abrir la habitación. Puedes cerrar el personaje en cualquier momento.")
                .setContentIntent(open).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(null, "Cerrar", close).build()).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION, notification);
    }
    private void createOverlay() {
        store = AvatarStore.get(this);
        windows = (WindowManager) getSystemService(WINDOW_SERVICE);
        window = new LinearLayout(this);
        window.setOrientation(LinearLayout.VERTICAL);
        // Only this compact rectangle consumes touches; the rest of the display remains usable.
        avatar = new AvatarView(this);
        avatar.setOverlayMode(true);
        window.addView(avatar, new LinearLayout.LayoutParams(-1, dp(178)));
        controls("← →", "Caminar por la pantalla", () -> store.change(s -> s.walkTo(s.getX() > .5f ? 0f : 1f)),
                "II", "Detenerse o despertar", () -> store.change(AvatarState::wake),
                "×", "Cerrar personaje", this::stopSelf);
        controls("Cama", "Crear cama y acostarse", () -> store.change(s -> { s.createBed(); s.sleep(); }),
                "Ropa", "Cambiar vestido o pijama", () -> store.change(s -> s.wear(
                        s.getOutfit() == AvatarState.Outfit.DAY ? AvatarState.Outfit.PAJAMAS : AvatarState.Outfit.DAY,
                        s.getAccent(), s.getPattern())),
                "…", "Abrir habitación y vestuario", this::openRoom);
        params = new WindowManager.LayoutParams(dp(190), dp(274), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        bounds(); setPositionFromState(); installDrag();
        avatar.setFrameListener(this::positionFromState);
        windows.addView(window, params);
        added = true;
        store.addListener(changed);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON); filter.addAction(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenReceiver, filter);
        receiverRegistered = true;
        updateVisibility();
    }
    private void controls(String a, String ad, Runnable aa, String b, String bd, Runnable bb, String c, String cd, Runnable cc) {
        LinearLayout row = new LinearLayout(this);
        window.addView(row, new LinearLayout.LayoutParams(-1, dp(48)));
        control(row, a, ad, aa); control(row, b, bd, bb); control(row, c, cd, cc);
    }
    private void control(LinearLayout row, String text, String description, Runnable action) {
        Button button = new Button(this);
        button.setText(text); button.setTextSize(12); button.setAllCaps(false); button.setPadding(0,0,0,0);
        button.setContentDescription(description); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setOnClickListener(v -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
    }
    private void installDrag() {
        avatar.setOnClickListener(v -> openRoom());
        avatar.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved;
            final int slop = ViewConfiguration.get(BurbujaFlotanteService.this).getScaledTouchSlop();
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX(); downY = event.getRawY(); startX = params.x; startY = params.y; moved = false;
                        store.change(AvatarState::wake); return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                        if (Math.abs(dx) + Math.abs(dy) > slop) moved = true;
                        if (moved) {
                            params.x = clamp(startX + Math.round(dx), minX, maxX);
                            params.y = clamp(startY + Math.round(dy), minY, maxY);
                            store.state().moveByUser(fraction(params.x, minX, maxX), fraction(params.y, minY, maxY));
                            updateWindow();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        store.save(); if (!moved) view.performClick(); return true;
                    case MotionEvent.ACTION_CANCEL:
                        store.save(); return true;
                    default: return false;
                }
            }
        });
    }
    private void bounds() {
        int width, height, left = 0, right = 0, top = 0, bottom = 0;
        if (Build.VERSION.SDK_INT >= 30) {
            Rect rect = windows.getCurrentWindowMetrics().getBounds();
            width = rect.width(); height = rect.height();
            android.graphics.Insets insets = windows.getCurrentWindowMetrics().getWindowInsets()
                    .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            left = insets.left; top = insets.top; right = insets.right; bottom = insets.bottom;
        } else {
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            windows.getDefaultDisplay().getRealMetrics(metrics);
            width = metrics.widthPixels; height = metrics.heightPixels; top = dp(26); bottom = dp(32);
        }
        params.width = Math.min(dp(190), Math.max(dp(100), width - left - right));
        params.height = Math.min(dp(274), Math.max(dp(160), height - top - bottom));
        minX = left; minY = top;
        maxX = Math.max(minX, width - right - params.width);
        maxY = Math.max(minY, height - bottom - params.height);
    }
    private void setPositionFromState() {
        params.x = minX + Math.round(store.state().getX() * (maxX - minX));
        params.y = minY + Math.round(store.state().getOverlayY() * (maxY - minY));
    }
    private void positionFromState() {
        if (!added) return;
        int oldX = params.x, oldY = params.y;
        setPositionFromState();
        if (oldX != params.x || oldY != params.y) updateWindow();
    }
    private void updateWindow() {
        if (!added) return;
        try { windows.updateViewLayout(window, params); }
        catch (RuntimeException error) { Log.w("SalveAvatar", "Superposición retirada por Android", error); stopSelf(); }
    }
    private void updateVisibility() {
        if (window == null) return;
        PowerManager power = getSystemService(PowerManager.class);
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean visible = power.isInteractive() && !keyguard.isKeyguardLocked();
        window.setVisibility(visible ? View.VISIBLE : View.GONE);
        avatar.setAnimationEnabled(visible);
    }
    private void openRoom() {
        try { startActivity(new Intent(this, AvatarRoomActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException error) { Log.w("SalveAvatar", "No pude abrir la habitación", error); }
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (params != null) { bounds(); setPositionFromState(); updateWindow(); }
    }
    @Override public void onDestroy() {
        if (receiverRegistered) { unregisterReceiver(screenReceiver); receiverRegistered = false; }
        if (store != null) { store.removeListener(changed); store.save(); }
        if (avatar != null) { avatar.setFrameListener(null); avatar.setAnimationEnabled(false); }
        if (added) {
            try { windows.removeView(window); } catch (IllegalArgumentException ignored) { }
            added = false;
        }
        window = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float fraction(int value, int min, int max) { return max <= min ? 0f : (value - min) / (float) (max - min); }
}
