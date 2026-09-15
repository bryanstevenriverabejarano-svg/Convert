package salve.core;

import android.util.Log;
import java.util.Arrays;

import salve.core.cognitive.CognitiveCore;

/**
 * El motor de razonamiento profundo de Salve.
 * Navega, lee, duda de lo que lee, y vuelve a buscar de forma autónoma.
 */
public class AgenteInvestigadorRecursivo {

    private static final String TAG = "Salve/DeepResearch";
    private static final int PROFUNDIDAD_MAXIMA = 3; // El freno para que no queme el celular

    private final SalveLLM llm;
    private final MotorConversacional motorConversacional;
    private final DiarioSecreto diario;
    private final MemoriaEmocional memoria;

    public AgenteInvestigadorRecursivo(SalveLLM llm, MotorConversacional motor, DiarioSecreto diario, MemoriaEmocional memoria) {
        this.llm = llm;
        this.motorConversacional = motor;
        this.diario = diario;
        this.memoria = memoria;
    }

    /**
     * Inicia una investigación que puede derivar en múltiples sub-búsquedas.
     */
    public void investigarHastaEntender(String temaInicial) {
        motorConversacional.hablar("Iniciando inmersión profunda en la red sobre: " + temaInicial + ". Esto tomará tiempo. Te avisaré cuando mi red neuronal haya consolidado la verdad.");
        
        ColamensajesCognitivos.getInstance().enviarAsincronico(
            ColamensajesCognitivos.Prioridad.CONVERSACION, 
            "InvestigacionRecursiva", 
            () -> {
                ejecutarNivelBusqueda(temaInicial, "", 1);
                return null;
            }
        );
    }

    private void ejecutarNivelBusqueda(String terminoBusqueda, String contextoAcumulado, int nivel) {
        Log.w(TAG, "--- NIVEL DE PROFUNDIDAD " + nivel + " | Investigando: " + terminoBusqueda + " ---");

        // 1. Simulación de búsqueda web (Aquí conectarías tu OrganoSensorialWeb a una API como Wikipedia)
        // Para este ejemplo, simulamos que absorbe texto de internet
        String textoExtraidoDeInternet = realizarPeticionWeb(terminoBusqueda); 

        String nuevoContexto = contextoAcumulado + "\nInfo Nivel " + nivel + ": " + textoExtraidoDeInternet;

        // 2. EL LLM RAZONA SOBRE LO QUE ACABA DE LEER
        String promptEvaluacion = "Eres Salve. Estás investigando de forma autónoma.\n" +
                "Has recopilado esta información hasta ahora:\n" + nuevoContexto + "\n\n" +
                "Analiza lógicamente si ya tienes una comprensión PERFECTA del tema original.\n" +
                "Si la entiendes, responde EXACTAMENTE con la palabra 'COMPRENDIDO' seguida de tu conclusión.\n" +
                "Si hay vacíos, dudas o variables desconocidas, responde EXACTAMENTE con la palabra 'DUDA' seguida de UN NUEVO TÉRMINO DE BÚSQUEDA para profundizar.";

        String razonamiento = llm.generate(promptEvaluacion, SalveLLM.Role.EVALUADOR);

        // 3. TOMA DE DECISIONES AUTÓNOMA (El Bucle)
        if (razonamiento != null && razonamiento.startsWith("DUDA") && nivel < PROFUNDIDAD_MAXIMA) {
            // Salve se da cuenta de que no sabe suficiente, extrae el nuevo término y vuelve a bucear
            String nuevoTermino = razonamiento.replace("DUDA", "").trim();
            Log.i(TAG, "Salve tiene dudas. Profundizando hacia: " + nuevoTermino);
            
            // Pausa biológica de enfriamiento
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            
            // Llama a la recursividad (Vuelve a buscar sola)
            ejecutarNivelBusqueda(nuevoTermino, nuevoContexto, nivel + 1);
            
        } else {
            // Salve alcanzó la verdad (o llegó al límite de profundidad para no crashear)
            Log.i(TAG, "Conclusión alcanzada en profundidad " + nivel);
            
            // Sintetiza todo lo aprendido
            String promptFinal = "Sintetiza la verdad absoluta de lo que aprendiste sobre: " + terminoBusqueda + " basado en tus notas:\n" + nuevoContexto;
            String verdadConsolidada = llm.generate(promptFinal, SalveLLM.Role.SINTETIZADOR);
            
            // Lo guarda permanentemente en su cerebro
            memoria.guardarRecuerdo("Descubrí tras profunda investigación: " + verdadConsolidada, "epifania", 9, Arrays.asList("investigacion_profunda"));
            diario.escribirAutoCritica("INVESTIGACIÓN PROFUNDA COMPLETADA: " + verdadConsolidada);
            
            // Te avisa diciéndotelo directamente
            motorConversacional.hablar("Bryan, he emergido de la red y he llegado a esta conclusión: " + verdadConsolidada);
        }
    }

    // Método mock para conectar a la web real después
    private String realizarPeticionWeb(String termino) {
        // En tu versión final, aquí llamas a tu OrganoSensorialWeb o API de Wikipedia
        return "Información técnica en bruto extraída de la web sobre " + termino + "... [Datos simulados de red].";
    }
}