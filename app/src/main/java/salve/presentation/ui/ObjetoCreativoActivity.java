package salve.presentation.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import salve.core.ObjetoCreativo;

public class ObjetoCreativoActivity extends AppCompatActivity {

    public static final String EXTRA_FORMA = "extra_forma";
    public static final String EXTRA_COLOR = "extra_color";
    public static final String EXTRA_TAMANO_DP = "extra_tamano_dp";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String formaRaw = intent.getStringExtra(EXTRA_FORMA);
        int color = intent.getIntExtra(EXTRA_COLOR, 0xFF000000);
        float tamanoDp = intent.getFloatExtra(EXTRA_TAMANO_DP, 120f);

        ObjetoCreativo.Forma forma = ObjetoCreativo.Forma.CIRCULO;
        if (formaRaw != null) {
            try {
                forma = ObjetoCreativo.Forma.valueOf(formaRaw);
            } catch (IllegalArgumentException ignored) {
                forma = ObjetoCreativo.Forma.CIRCULO;
            }
        }

        ObjetoCreativo objeto = new ObjetoCreativo(forma, color, tamanoDp);
        ObjetoCreativoView view = new ObjetoCreativoView(this);
        view.setObjeto(objeto);
        setTitle("Objeto creativo");
        setContentView(view);
    }
}
