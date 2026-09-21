package salve.presentation.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import salve.avatar.AvatarState;
import salve.avatar.AvatarStore;
import salve.avatar.AvatarView;
import salve.services.BurbujaFlotanteService;

/** Local, interactive wardrobe and room. No image generator, network or model required. */
public final class AvatarRoomActivity extends Activity {
    private AvatarStore store;
    private AvatarView avatar;
    private TextView status;
    private boolean returningFromOverlaySettings;
    private final Runnable changed = this::updateStatus;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("La habitación de Salve");
        store = AvatarStore.get(this);
        returningFromOverlaySettings = savedInstanceState != null && savedInstanceState.getBoolean("overlay_settings");
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(0xFF101827);
        scroll.addView(root);
        TextView title = label("Salve · Mi espacio", 25, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(label("Camina, cambia de ropa y descansa. Tu habitación se conserva al cerrar la app.", 15, 0xFFB3C5D1));
        avatar = new AvatarView(this);
        root.addView(avatar, new LinearLayout.LayoutParams(-1, dp(310)));
        status = label("", 14, 0xFF80E1DA);
        root.addView(status);
        row(root, "Caminar", () -> store.change(s -> s.walkTo(s.getX() > .5f ? .08f : .92f)),
                "Detenerse", () -> store.change(AvatarState::wake));
        row(root, "Crear cama", () -> store.change(AvatarState::createBed), "Acostarse", () -> {
            if (!store.state().hasBed()) toast("Crea primero una cama.");
            else store.change(AvatarState::sleep);
        });
        row(root, "Despertar", () -> store.change(AvatarState::wake), "Guardar cama", () -> store.change(AvatarState::removeBed));
        row(root, "Vestido", () -> wear(AvatarState.Outfit.DAY), "Pijama", () -> wear(AvatarState.Outfit.PAJAMAS));
        row(root, "Color de ropa", this::chooseColor, "Diseño", this::choosePattern);
        root.addView(label("Sobre otras apps", 19, Color.WHITE));
        root.addView(label("Activa el personaje flotante para acompañarte por la pantalla. Arrástralo para moverlo; el botón × lo cierra.", 14, 0xFFB3C5D1));
        button(root, "Mostrar sobre otras apps", this::showOverlay);
        button(root, "Ocultar personaje flotante", () -> stopService(new Intent(this, BurbujaFlotanteService.class)));
        button(root, "Volver", this::finish);
        setContentView(scroll);
        updateStatus();
    }
    private void wear(AvatarState.Outfit outfit) {
        store.change(s -> s.wear(outfit, s.getAccent(), s.getPattern()));
    }
    private void chooseColor() {
        int[] colors = {0xFF15CCC8, 0xFF9A86E8, 0xFFE19CAD, 0xFF609EE8, 0xFFE1B36C};
        new AlertDialog.Builder(this).setTitle("Color de ropa y manta")
                .setItems(new String[]{"Turquesa", "Lavanda", "Rosa", "Azul", "Ámbar"}, (d, i) ->
                        store.change(s -> s.wear(s.getOutfit(), colors[i], s.getPattern())))
                .setNegativeButton("Cancelar", null).show();
    }
    private void choosePattern() {
        AvatarState.Pattern[] patterns = AvatarState.Pattern.values();
        new AlertDialog.Builder(this).setTitle("Diseño de la prenda")
                .setItems(new String[]{"Liso", "Estrellas", "Rayas"}, (d, i) ->
                        store.change(s -> s.wear(s.getOutfit(), s.getAccent(), patterns[i])))
                .setNegativeButton("Cancelar", null).show();
    }
    private void showOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            returningFromOverlaySettings = true;
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName())));
            } catch (RuntimeException e) { returningFromOverlaySettings = false; toast("No pude abrir el permiso de superposición."); }
            return;
        }
        startOverlay();
    }
    private void startOverlay() {
        try {
            Intent intent = new Intent(this, BurbujaFlotanteService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
            toast("Personaje flotante activado.");
        } catch (RuntimeException e) { toast("Android no permitió iniciar el personaje flotante. Revisa el permiso."); }
    }
    @Override protected void onResume() {
        super.onResume();
        store.addListener(changed);
        avatar.setAnimationEnabled(true);
        updateStatus();
        if (returningFromOverlaySettings) {
            returningFromOverlaySettings = false;
            if (Settings.canDrawOverlays(this)) startOverlay();
            else toast("Puedes usar la habitación sin activar la superposición.");
        }
    }
    @Override protected void onPause() {
        avatar.setAnimationEnabled(false); store.removeListener(changed); store.save(); super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("overlay_settings", returningFromOverlaySettings); super.onSaveInstanceState(state);
    }
    private void updateStatus() {
        if (status == null) return;
        AvatarState s = store.state();
        status.setText((s.getPose() == AvatarState.Pose.SLEEPING ? "Descansando en la cama"
                : s.isGoingToSleep() ? "Caminando hacia la cama" : s.getPose() == AvatarState.Pose.WALKING ? "Caminando" : "Despierta")
                + " · " + (s.getOutfit() == AvatarState.Outfit.PAJAMAS ? "Pijama" : "Vestido")
                + " · " + (s.hasBed() ? "Cama creada" : "Sin cama"));
    }
    private TextView label(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(6), 0, dp(10)); return view;
    }
    private void row(LinearLayout parent, String a, Runnable aa, String b, Runnable bb) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER); parent.addView(row);
        Button first = makeButton(a, aa), second = makeButton(b, bb);
        row.addView(first, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(second, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void button(LinearLayout parent, String text, Runnable action) { parent.addView(makeButton(text, action)); }
    private Button makeButton(String text, Runnable action) {
        Button button = new Button(this); button.setText(text); button.setAllCaps(false); button.setMinHeight(dp(48));
        button.setOnClickListener(v -> action.run()); return button;
    }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }
}
