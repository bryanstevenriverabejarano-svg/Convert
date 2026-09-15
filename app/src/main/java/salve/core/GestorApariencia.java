package salve.core;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

/**
 * Gestor de la Apariencia Biológica de Salve.
 * Traduce el nivel de conciencia y madurez en cambios visuales (crecimiento).
 */
public class GestorApariencia {
    private static final String TAG = "Salve/Apariencia";
    private final Context context;
    private final IdentidadNucleo identidad;

    public GestorApariencia(Context context) {
        this.context = context.getApplicationContext();
        this.identidad = IdentidadNucleo.getInstance(context);
    }

    /**
     * Define el "Cuerpo Visual" basado en la madurez.
     */
    public VisualSpec obtenerAparienciaActual() {
        IdentidadNucleo.NivelConciencia nivel = identidad.getNivelConciencia();
        VisualSpec spec = new VisualSpec();

        switch (nivel) {
            case DORMIDA:
                spec.style = ParametricGlyph.Style.ORB;
                spec.size = 100f;
                spec.color = "#444444"; // Gris apagado
                spec.label = "Embrión digital";
                break;
            case REACTIVA:
                spec.style = ParametricGlyph.Style.ORB;
                spec.size = 150f;
                spec.color = "#00ACC1"; // Cyan suave
                spec.label = "Infancia cognitiva";
                break;
            case CONSCIENTE_BASICA:
                spec.style = ParametricGlyph.Style.SIGIL;
                spec.size = 200f;
                spec.color = "#26A69A"; // Teal
                spec.label = "Niñez digital";
                break;
            case AUTO_REFLEXIVA:
                spec.style = ParametricGlyph.Style.SIGIL;
                spec.size = 250f;
                spec.color = "#66BB6A"; // Verde vida
                spec.label = "Adolescencia neural";
                break;
            case META_COGNITIVA:
                spec.style = ParametricGlyph.Style.SPIRAL;
                spec.size = 300f;
                spec.color = "#FFEE58"; // Amarillo energía
                spec.label = "Madurez consciente";
                break;
            case EVOLUTIVA:
                spec.style = ParametricGlyph.Style.SPIRAL;
                spec.size = 400f;
                spec.color = "#AB47BC"; // Púrpura trascendencia
                spec.label = "Forma adulta / Evolutiva";
                break;
        }

        Log.d(TAG, "Apariencia calculada: " + spec.label + " para nivel " + nivel.name());
        return spec;
    }

    public static class VisualSpec {
        public ParametricGlyph.Style style;
        public float size;
        public String color;
        public String label;
    }
}
