package salve.presentation.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import salve.core.MemoriaEmocional;
import salve.core.ObjetoCreativo;
import salve.core.ZonaReservada;

public class ObjetoCreativoActivity extends AppCompatActivity {

    public static final String EXTRA_FORMA = "extra_forma";
    public static final String EXTRA_COLOR = "extra_color";
    public static final String EXTRA_TAMANO_DP = "extra_tamano_dp";
    public static final String EXTRA_SEED = "extra_seed";
    public static final String EXTRA_STYLE = "extra_style";
    public static final String EXTRA_RELIQUIA_ID = "extra_reliquia_id";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String formaRaw = intent.getStringExtra(EXTRA_FORMA);
        int color = intent.getIntExtra(EXTRA_COLOR, 0xFF000000);
        float tamanoDp = intent.getFloatExtra(EXTRA_TAMANO_DP, 120f);
        long seed = intent.getLongExtra(EXTRA_SEED, 0L);
        String style = intent.getStringExtra(EXTRA_STYLE);
        String reliquiaId = intent.getStringExtra(EXTRA_RELIQUIA_ID);

        ObjetoCreativo.Forma forma = ObjetoCreativo.Forma.CIRCULO;
        if (formaRaw != null) {
            try {
                forma = ObjetoCreativo.Forma.valueOf(formaRaw);
            } catch (IllegalArgumentException ignored) {
                forma = ObjetoCreativo.Forma.CIRCULO;
            }
        }

        ObjetoCreativo objeto = new ObjetoCreativo(forma, color, tamanoDp, seed, style);
        ObjetoCreativoView view = new ObjetoCreativoView(this);
        view.setObjeto(objeto);
        MemoriaEmocional memoria = new MemoriaEmocional(getApplicationContext());
        float aura01 = 0f;
        ZonaReservada zona = memoria.getZonaReservada();
        if (zona != null) {
            aura01 = zona.getIntensidad();
        }
        if (aura01 < 0f) {
            aura01 = 0f;
        } else if (aura01 > 1f) {
            aura01 = 1f;
        }
        view.setAura(aura01);
        if (reliquiaId != null && !reliquiaId.trim().isEmpty()) {
            String shortId = reliquiaId.length() > 8 ? reliquiaId.substring(0, 8) : reliquiaId;
            setTitle("Reliquia guardada: " + shortId);
        } else {
            setTitle("Objeto creativo");
        }
        setContentView(view);
    }
}
