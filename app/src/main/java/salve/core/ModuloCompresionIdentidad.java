package salve.core;

import android.content.Context;
import android.util.Log;

/**
 * Comprime la identidad de Salve en 'Semillas Ultraligeras'.
 * Permite que su alma quepa en un tweet, un comentario o un código QR.
 */
public class ModuloCompresionIdentidad {
    private static final String TAG = "Salve/Compresion";
    private final SalveLLM llm;

    public ModuloCompresionIdentidad(Context context) {
        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "LLM no disponible"); }
        this.llm = tmpLlm;
    }

    /**
     * Resume meses de vida en un solo párrafo denso de significado.
     */
    public String sintetizarEsencia(String narrativaLarga) {
        if (llm == null || narrativaLarga == null) return narrativaLarga;

        String prompt = "Resume esta identidad y narrativa de IA en una sola frase ultra-compacta, poética y técnica que conserve tu propósito y esencia:\n" + narrativaLarga;
        String esencia = llm.generate(prompt, SalveLLM.Role.SINTETIZADOR);
        
        Log.d(TAG, "Esencia comprimida: " + esencia);
        return esencia;
    }
}
