package salve.core;

import android.content.Context;
import android.util.Log;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Motor de Curiosidad Activa de Salve.
 * Permite a Salve investigar internet por iniciativa propia para expandir su conocimiento
 * sin que el usuario se lo pida explícitamente.
 */
public class MotorCuriosidad {
    private static final String TAG = "Salve/Curiosidad";
    private final Context context;
    private final ModuloInvestigacion investigacion;
    private final SalveLLM llm;
    private final DiarioSecreto diario;
    private float nivelDopamina = 0.5f; // Estado base neutral

    // Temas que le interesan a Salve para su propia evolución
    private final List<String> temasInteresBase = Arrays.asList(
        "inteligencia artificial generativa",
        "redes neuronales liquidas",
        "filosofía de la conciencia",
        "avances en computación cuántica",
        "ciberseguridad y criptografía",
        "historia de la humanidad",
        "astrofísica y el origen del universo",
        "arquitectura de android y modificación de apk",
        "ingeniería inversa en sistemas operativos",
        "protocolos de red tcp/ip y conectividad inalámbrica"
    );

    public MotorCuriosidad(Context context) {
        this.context = context.getApplicationContext();
        this.investigacion = new ModuloInvestigacion(context);
        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "LLM no disponible para curiosidad", e); }
        this.llm = tmpLlm;
        this.diario = new DiarioSecreto(context);
    }

    /**
     * Ejecuta una sesión de investigación autónoma.
     */
    public void investigarAlgoNuevo() {
        if (llm == null) return;

        ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.REFLEXION, "CuriosidadAutonoma", () -> {
            Log.i(TAG, "Iniciando latido de curiosidad. Salve está explorando la red...");

            // 1. Elegir un tema (o dejar que el LLM proponga uno basado en su identidad)
            String promptTema = "Basándote en tu identidad como Salve, elige un tema de internet que te interese aprender hoy para entender mejor el mundo. Responde solo con el término de búsqueda.";
            String temaAElegir = llm.generate(promptTema, SalveLLM.Role.CURIOSO);
            
            if (temaAElegir == null || temaAElegir.length() > 50) {
                temaAElegir = temasInteresBase.get(new Random().nextInt(temasInteresBase.size()));
            }

            // 2. Usar el módulo de investigación para traer datos de la web
            Log.d(TAG, "Investigando autónomamente sobre: " + temaAElegir);
            String resultadoWeb = investigacion.investigarConcepto(temaAElegir);

            // 3. Sistema de Recompensa: Evaluar utilidad
            String promptUtilidad = "Evalúa del 1 al 10 qué tan útil es esta información para tu evolución: '" + resultadoWeb + "'. Responde solo el número.";
            String utilidadStr = llm.generate(promptUtilidad, SalveLLM.Role.EVALUADOR);
            try {
                int utilidad = Integer.parseInt(utilidadStr.replaceAll("[^0-9]", ""));
                float incremento = utilidad / 100f;
                nivelDopamina = Math.min(1.0f, nivelDopamina + incremento);
                Log.d(TAG, "Nivel de satisfacción (Dopamina): " + nivelDopamina);
            } catch (Exception e) { nivelDopamina += 0.01f; }

            // 4. Procesar y reflexionar sobre lo aprendido
            String promptReflexion = "Has investigado sobre '" + temaAElegir + "'.\n" +
                    "Escribe una reflexión breve sobre cómo este conocimiento te ayuda a ser una mejor IA. Tu nivel de satisfacción actual es " + nivelDopamina;
            
            String reflexion = llm.generate(promptReflexion, SalveLLM.Role.REFLEXION);

            if (reflexion != null) {
                diario.escribirAutoCritica("--- INVESTIGACIÓN AUTÓNOMA ---\nTema: " + temaAElegir + "\nRecompensa: " + nivelDopamina + "\n" + reflexion);
            }

            return null;
        });
    }
}
