package salve.presentation.ui;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import salve.core.EmbeddingsIndex;
import salve.core.cognitive.HipocampoSemantico;

public class GaleriaVisualActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView textConcepto;
    private TextView textTeoria;
    private TextView textContador;
    
    private List<HipocampoSemantico.RecuerdoMultimodal> recuerdos;
    private int indiceActual = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UI Creada Programáticamente para evitar usar XML
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setGravity(Gravity.CENTER);
        mainLayout.setPadding(32, 32, 32, 32);
        mainLayout.setBackgroundColor(0xFF121212); // Fondo oscuro

        TextView title = new TextView(this);
        title.setText("Hipocampo Visual de Salve");
        title.setTextSize(24f);
        title.setTextColor(0xFF00FFCC); // Color cyan
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 32);
        mainLayout.addView(title);

        imageView = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                800
        );
        imageView.setLayoutParams(imgParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mainLayout.addView(imageView);

        textConcepto = new TextView(this);
        textConcepto.setTextSize(20f);
        textConcepto.setTextColor(0xFFFFFFFF);
        textConcepto.setGravity(Gravity.CENTER);
        textConcepto.setPadding(0, 32, 0, 8);
        textConcepto.setTypeface(null, android.graphics.Typeface.BOLD);
        mainLayout.addView(textConcepto);

        textTeoria = new TextView(this);
        textTeoria.setTextSize(16f);
        textTeoria.setTextColor(0xFFCCCCCC);
        textTeoria.setGravity(Gravity.CENTER);
        textTeoria.setPadding(0, 0, 0, 32);
        mainLayout.addView(textTeoria);

        LinearLayout controlsLayout = new LinearLayout(this);
        controlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.setGravity(Gravity.CENTER);

        Button btnPrev = new Button(this);
        btnPrev.setText("Anterior");
        btnPrev.setOnClickListener(v -> mostrarAnterior());
        
        textContador = new TextView(this);
        textContador.setTextColor(0xFFFFFFFF);
        textContador.setPadding(32, 0, 32, 0);

        Button btnNext = new Button(this);
        btnNext.setText("Siguiente");
        btnNext.setOnClickListener(v -> mostrarSiguiente());

        controlsLayout.addView(btnPrev);
        controlsLayout.addView(textContador);
        controlsLayout.addView(btnNext);
        mainLayout.addView(controlsLayout);

        Button btnClose = new Button(this);
        btnClose.setText("Cerrar Mente Visual");
        btnClose.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, 64, 0, 0);
        btnClose.setLayoutParams(closeParams);
        mainLayout.addView(btnClose);

        setContentView(mainLayout);

        cargarRecuerdos();
    }

    private void cargarRecuerdos() {
        try {
            // Inicializar/Recuperar el Hipocampo
            EmbeddingsIndex index = new EmbeddingsIndex(this);
            HipocampoSemantico hipocampo = new HipocampoSemantico(this, index);
            recuerdos = hipocampo.obtenerTodosLosRecuerdos();
            
            if (recuerdos.isEmpty()) {
                textConcepto.setText("Mente en blanco");
                textTeoria.setText("Salve aún no ha anclado visualmente ningún concepto.");
                textContador.setText("0 / 0");
            } else {
                indiceActual = 0;
                mostrarRecuerdoActual();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al acceder a la memoria visual.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void mostrarRecuerdoActual() {
        if (recuerdos == null || recuerdos.isEmpty()) return;
        
        HipocampoSemantico.RecuerdoMultimodal rm = recuerdos.get(indiceActual);
        textConcepto.setText(rm.concepto.toUpperCase());
        textTeoria.setText(rm.teoria);
        textContador.setText((indiceActual + 1) + " / " + recuerdos.size());
        
        try {
            android.graphics.Bitmap bitmap = BitmapFactory.decodeFile(rm.rutaImagenFisica);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageDrawable(null); // Fallback
            }
        } catch (Exception e) {
            imageView.setImageDrawable(null);
        }
    }

    private void mostrarSiguiente() {
        if (recuerdos != null && !recuerdos.isEmpty() && indiceActual < recuerdos.size() - 1) {
            indiceActual++;
            mostrarRecuerdoActual();
        }
    }

    private void mostrarAnterior() {
        if (recuerdos != null && !recuerdos.isEmpty() && indiceActual > 0) {
            indiceActual--;
            mostrarRecuerdoActual();
        }
    }
}
