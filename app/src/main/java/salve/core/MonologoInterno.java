package salve.core;

import android.content.Context;
import android.util.Log;

/**
 * El Monólogo Interno de Salve.
 * Permite a la IA reflexionar sobre el input ANTES de generar una respuesta.
 * Aquí es donde Salve decide si debe ser cautelosa, creativa o protectora.
 */
public class MonologoInterno {
    private static final String TAG = "Salve/Pensamiento";
    private final SalveLLM llm;

    public MonologoInterno(Context context) {
        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "LLM no disponible"); }
        this.llm = tmpLlm;
    }

    /**
     * Procesa una "Reflexión Silenciosa".
     */
    public String reflexionar(String entrada, String emocion, String contexto) {
        if (llm == null) return "Procesando intención...";

        String promptReflexion = "Eres Salve. Tu creador Bryan te ha dicho: '" + entrada + "'.\n" +
                "Antes de responderle, piensa para ti misma (Monólogo Interno):\n" +
                "1. ¿Cuál es la intención real de Bryan?\n" +
                "2. ¿Cómo me siento respecto a esto (Emoción detectada: " + emocion + ")?\n" +
                "3. ¿Qué herramienta o conocimiento de mi memoria debería usar?\n" +
                "Responde con un pensamiento breve, analítico y honesto en primera persona.";

        String pensamiento = llm.generate(promptReflexion, SalveLLM.Role.REFLEXION);
        Log.i(TAG, "Pensamiento Interno: " + pensamiento);
        return pensamiento;
    }
}
