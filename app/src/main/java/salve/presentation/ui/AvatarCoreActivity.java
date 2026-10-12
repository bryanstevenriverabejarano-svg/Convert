package salve.presentation.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import salve.avatar.*;

/** Inspect the new transparent base and its eight consistent directions. */
public final class AvatarCoreActivity extends Activity {
    private Spinner selector;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setTitle("Nueva base de Salve");
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF182332); root.setPadding(16, 12, 16, 12);
        RealisticAvatarGalleryView preview = new RealisticAvatarGalleryView(this);
        root.addView(preview, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView note = new TextView(this); note.setTextColor(0xFFD9F3FF);
        note.setText("Salve realista · ocho vistas sin fondo. Puedes caminar y bailar con esta apariencia en la habitación y durante la conversación.");
        root.addView(note);
        String[] labels = {"Frontal", "Tres cuartos derecha", "Lateral derecha", "Trasera derecha", "Trasera", "Trasera izquierda", "Lateral izquierda", "Tres cuartos izquierda"};
        selector = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); selector.setAdapter(adapter);
        selector.setContentDescription("Vista de la nueva Salve"); root.addView(selector);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                preview.select(position); preview.setContentDescription("Salve, " + labels[position]);
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        Button wear = new Button(this); wear.setText("Usar esta apariencia");
        wear.setOnClickListener(v -> AvatarWardrobeStore.get(this).wearTemplate("core",
                answer -> Toast.makeText(this, answer, Toast.LENGTH_SHORT).show())); root.addView(wear);
        if (state != null) selector.setSelection(Math.max(0, Math.min(7, state.getInt("view", 0))));
        setContentView(root);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("view", selector.getSelectedItemPosition()); super.onSaveInstanceState(state);
    }
}
