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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.text.InputFilter;
import java.util.List;
import salve.avatar.AvatarState;
import salve.avatar.AvatarStore;
import salve.avatar.AvatarView;
import salve.avatar.AvatarMotion;
import salve.avatar.AvatarMotionController;
import salve.avatar.AvatarDesignCatalog;
import salve.avatar.AvatarDesignSpec;
import salve.avatar.AvatarDesignRequest;
import salve.avatar.AvatarWardrobeStore;
import salve.avatar.MotionPreferenceProfile;
import salve.avatar.MotionPreferenceStore;
import salve.services.BurbujaFlotanteService;

/** Original illustrated character, gesture previews and persistent room controls. */
public final class AvatarRoomActivity extends Activity {
    private AvatarStore store;
    private AvatarView avatar;
    private TextView status;
    private TextView motionStatus;
    private LinearLayout wardrobeRows;
    private AvatarWardrobeStore wardrobe;
    private boolean returningFromOverlaySettings;
    private final Runnable changed = this::updateStatus;
    private final Runnable wardrobeChanged = this::updateWardrobe;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("La habitación de Salve");
        store = AvatarStore.get(this);
        wardrobe = AvatarWardrobeStore.get(this);
        AvatarMotionController.get().initializePreferences(this);
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
        root.addView(label("Salve reacciona mientras conversáis. Aquí puedes ver sus gestos, caminar y descansar.", 15, 0xFFB3C5D1));
        avatar = new AvatarView(this);
        root.addView(avatar, new LinearLayout.LayoutParams(-1, dp(310)));
        status = label("", 14, 0xFF80E1DA);
        root.addView(status);
        row(root, "Saludar", () -> preview(AvatarMotion.Gesture.WAVE, AvatarMotion.Expression.WARM),
                "Asentir", () -> preview(AvatarMotion.Gesture.NOD, AvatarMotion.Expression.WARM));
        row(root, "Explicar", () -> preview(AvatarMotion.Gesture.EXPLAIN, AvatarMotion.Expression.NEUTRAL),
                "Negar", () -> preview(AvatarMotion.Gesture.SHAKE, AvatarMotion.Expression.NEUTRAL));
        row(root, "Caminar", () -> store.change(s -> s.walkTo(s.getX() > .5f ? .08f : .92f)),
                "Detenerse", () -> store.change(AvatarState::wake));
        row(root, "Crear cama", () -> store.change(AvatarState::createBed), "Acostarse", () -> {
            if (!store.state().hasBed()) toast("Crea primero una cama.");
            else store.change(AvatarState::sleep);
        });
        row(root, "Despertar", () -> store.change(AvatarState::wake), "Guardar cama", () -> store.change(AvatarState::removeBed));
        button(root, "Taller de posturas", () -> startActivity(new Intent(this, AvatarPoseActivity.class)));
        root.addView(label("Cómo me muevo", 19, Color.WHITE));
        motionStatus = label("", 14, 0xFFB3C5D1); root.addView(motionStatus);
        row(root, "Más despacio", () -> movementFeedback(MotionPreferenceProfile.Feedback.TOO_FAST),
                "Más suave", () -> movementFeedback(MotionPreferenceProfile.Feedback.TOO_STRONG));
        row(root, "Así está bien", () -> movementFeedback(MotionPreferenceProfile.Feedback.COMFORTABLE),
                "Restablecer", () -> movementFeedback(MotionPreferenceProfile.Feedback.RESET));
        root.addView(label("Vestuario", 19, Color.WHITE));
        root.addView(label("Crea diseños con las prendas ilustradas disponibles, colores y patrones. Salve conserva su rostro y guarda cada diseño en el armario.", 14, 0xFFB3C5D1));
        button(root, "Diseñar una prenda", this::createDesign);
        row(root, "Vestido original", () -> wardrobe.wearTemplate("original_dress", this::toast),
                "Pijama", () -> wardrobe.wearTemplate("pajamas", this::toast));
        button(root, "Conjunto exploradora", () -> wardrobe.wearTemplate("explorer", this::toast));
        wardrobeRows = new LinearLayout(this); wardrobeRows.setOrientation(LinearLayout.VERTICAL); root.addView(wardrobeRows);
        button(root, "Color de la manta", this::chooseColor);
        root.addView(label("Sobre otras apps", 19, Color.WHITE));
        root.addView(label("Activa el personaje flotante para acompañarte por la pantalla. Arrástralo para moverlo; el botón × lo cierra.", 14, 0xFFB3C5D1));
        button(root, "Mostrar sobre otras apps", this::showOverlay);
        button(root, "Ocultar personaje flotante", () -> stopService(new Intent(this, BurbujaFlotanteService.class)));
        button(root, "Volver", this::finish);
        setContentView(scroll);
        updateStatus();
        updateWardrobe();
    }
    private void preview(AvatarMotion.Gesture gesture, AvatarMotion.Expression expression) {
        if (store.state().getPose() == AvatarState.Pose.SLEEPING) store.change(AvatarState::wake);
        AvatarMotionController.get().previewGesture(gesture, expression);
    }
    private void movementFeedback(MotionPreferenceProfile.Feedback feedback) {
        AvatarMotionController.get().recordFeedback(this, feedback);
    }
    private void createDesign() {
        if (!wardrobe.isReady()) { toast("El armario todavía no está disponible."); return; }
        List<AvatarDesignCatalog.Template> templates = wardrobe.availableTemplates();
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(8), dp(22), 0);
        EditText name = new EditText(this); name.setHint("Nombre del diseño"); name.setSingleLine(true);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(48)}); form.addView(name);
        String[] templateNames = new String[templates.size()];
        for (int i = 0; i < templates.size(); i++) templateNames[i] = templates.get(i).name;
        Spinner template = selector(form, "Prenda", templateNames);
        Spinner palette = selector(form, "Color de la ropa", new String[]{"Colores originales", "Turquesa", "Lavanda", "Rosa", "Azul", "Ámbar"});
        Spinner pattern = selector(form, "Patrón de la ropa", new String[]{"Sin patrón", "Estrellas", "Rayas"});
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Diseñar y guardar")
                .setView(form).setPositiveButton("Guardar y vestir", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                AvatarDesignRequest request = AvatarDesignRequest.create(name.getText().toString(),
                        templates.get(template.getSelectedItemPosition()).id,
                        AvatarDesignSpec.Palette.values()[palette.getSelectedItemPosition()],
                        AvatarDesignSpec.Pattern.values()[pattern.getSelectedItemPosition()], true);
                wardrobe.execute(request, this::toast); dialog.dismiss();
            } catch (IllegalArgumentException invalid) { name.setError(invalid.getMessage()); }
        }));
        dialog.show();
    }
    private Spinner selector(LinearLayout form, String title, String[] values) {
        TextView caption = new TextView(this); caption.setText(title); caption.setPadding(0, dp(12), 0, dp(3)); form.addView(caption);
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter); form.addView(spinner);
        return spinner;
    }
    private void updateWardrobe() {
        if (wardrobeRows == null) return;
        wardrobeRows.removeAllViews();
        if (!wardrobe.lastError().isEmpty()) wardrobeRows.addView(label(wardrobe.lastError(), 14, 0xFFE7B59D));
        for (AvatarDesignSpec design : wardrobe.list()) {
            String title = design.name + (wardrobe.selected().id.equals(design.id) ? " · Puesto" : "");
            wardrobeRows.addView(label(title, 15, 0xFFD9E7EB));
            if (design.id.equals(AvatarDesignSpec.ORIGINAL_ID)) continue;
            row(wardrobeRows, "Vestir", () -> wardrobe.execute(AvatarDesignRequest.select(design.id), this::toast),
                    "Eliminar", () -> wardrobe.execute(AvatarDesignRequest.delete(design.id), this::toast));
        }
        updateStatus();
    }
    private void chooseColor() {
        int[] colors = {0xFF15CCC8, 0xFF9A86E8, 0xFFE19CAD, 0xFF609EE8, 0xFFE1B36C};
        new AlertDialog.Builder(this).setTitle("Color de la manta")
                .setItems(new String[]{"Turquesa", "Lavanda", "Rosa", "Azul", "Ámbar"}, (d, i) ->
                        store.change(s -> s.wear(s.getOutfit(), colors[i], s.getPattern())))
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
        wardrobe.addListener(wardrobeChanged);
        AvatarMotionController.get().addListener(changed);
        avatar.setAnimationEnabled(true);
        updateStatus();
        updateWardrobe();
        if (returningFromOverlaySettings) {
            returningFromOverlaySettings = false;
            if (Settings.canDrawOverlays(this)) startOverlay();
            else toast("Puedes usar la habitación sin activar la superposición.");
        }
    }
    @Override protected void onPause() {
        avatar.setAnimationEnabled(false); store.removeListener(changed); wardrobe.removeListener(wardrobeChanged);
        AvatarMotionController.get().removeListener(changed); store.save(); super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("overlay_settings", returningFromOverlaySettings); super.onSaveInstanceState(state);
    }
    private void updateStatus() {
        if (status == null) return;
        AvatarState s = store.state();
        status.setText((s.getPose() == AvatarState.Pose.SLEEPING ? "Descansando en la cama"
                : s.isGoingToSleep() ? "Caminando hacia la cama" : s.getPose() == AvatarState.Pose.WALKING ? "Caminando" : "Despierta")
                + " · " + wardrobe.selected().name
                + " · " + (s.hasBed() ? "Cama creada" : "Sin cama"));
        if (motionStatus != null) motionStatus.setText(MotionPreferenceStore.get(this).profile().description());
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
