package salve.presentation.ui;

import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import salve.core.agent.ToolBridgeConfig;

/** A separate private settings surface: secrets never travel through conversation/cloud logs. */
public final class ToolBridgeActivity extends AppCompatActivity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(32, 40, 32, 32);
        TextView title = new TextView(this); title.setText("Herramientas remotas de Salve"); title.setTextSize(22); layout.addView(title);
        TextView info = new TextView(this);
        info.setText("Conecta tu servicio de herramientas. Recibirá consultas públicas y las páginas que pidas leer. "
                + "El código sólo se enviará cuando solicites ‘resuelve con código’. Los recuerdos personales no se envían como consultas. "
                + "No necesitas este servicio para la memoria ni la lectura web local."); layout.addView(info);
        EditText endpoint = new EditText(this); endpoint.setHint("https://tu-servicio.example"); endpoint.setSingleLine(true);
        endpoint.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)}); layout.addView(endpoint);
        EditText token = new EditText(this); token.setHint("Token privado del servicio"); token.setSingleLine(true);
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256)});
        token.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS); layout.addView(token);
        ToolBridgeConfig existing = ToolBridgeConfig.load(this); if (existing != null) endpoint.setText(existing.endpoint);
        TextView status = new TextView(this); layout.addView(status);
        Button save = new Button(this); save.setText("Guardar conexión"); layout.addView(save);
        save.setOnClickListener(v -> {
            try {
                String secret = token.getText().toString();
                ToolBridgeConfig current = ToolBridgeConfig.load(this);
                if (secret.isEmpty() && current != null && current.endpoint.equals(endpoint.getText().toString().trim())) secret = current.token;
                ToolBridgeConfig.save(this, endpoint.getText().toString(), secret); token.setText("");
                status.setText("Configuración guardada. Cada herramienta comprobará el servicio al ejecutarse. Los planes anteriores conservan su destino original.");
            } catch (RuntimeException invalid) { status.setText(invalid.getMessage()); }
        });
        Button remove = new Button(this); remove.setText("Desconectar servicio"); layout.addView(remove);
        remove.setOnClickListener(v -> { ToolBridgeConfig.clear(this); token.setText(""); status.setText("Desconectado. Los planes que necesiten el puente quedarán bloqueados."); });
        ScrollView scroll = new ScrollView(this); scroll.addView(layout); setContentView(scroll);
    }
}
