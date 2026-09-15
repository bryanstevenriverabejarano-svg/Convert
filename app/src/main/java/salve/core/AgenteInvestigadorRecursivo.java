package salve.core;

import android.util.Log;
import java.util.Arrays;
import java.util.List;

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
    private final WikipediaResearchClient webClient = new WikipediaResearchClient();

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

        // 1. Busqueda real y dirigida en varias paginas de una fuente autorizada.
        WikipediaResearchClient.ResearchResult investigacion = realizarPeticionWeb(terminoBusqueda);
        if (!investigacion.hasSources()) {
            motorConversacional.hablar("No encontré fuentes suficientes para responder con seguridad sobre: "
                    + terminoBusqueda);
            return;
        }
        String textoExtraidoDeInternet = investigacion.asGroundedContext();
        guardarFuentes(terminoBusqueda, investigacion.sources);

        if (llm == null) {
            String respuestaSinModelo = "Encontré estas fuentes, pero el modelo de lenguaje local "
                    + "no está disponible para sintetizarlas:\n" + construirListaFuentes(investigacion.sources);
            motorConversacional.hablar(respuestaSinModelo);
            return;
        }

        String nuevoContexto = contextoAcumulado + "\nInfo Nivel " + nivel + ": " + textoExtraidoDeInternet;

        // 2. EL LLM RAZONA SOBRE LO QUE ACABA DE LEER
        String promptEvaluacion = "Eres Salve. Estás investigando de forma autónoma.\n" +
                "Has recopilado esta información hasta ahora:\n" + nuevoContexto + "\n\n" +
                "Analiza si las fuentes permiten responder con suficiente confianza. No inventes datos.\n" +
                "Si puedes responder, usa 'COMPRENDIDO' seguido de una conclusión provisional.\n" +
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
            // Hay evidencia suficiente o se alcanzo el limite de profundidad.
            Log.i(TAG, "Conclusión alcanzada en profundidad " + nivel);
            
            // Sintetiza todo lo aprendido
            String promptFinal = "Responde en español a la pregunta original usando exclusivamente estas fuentes. "
                    + "Distingue hechos de inferencias, reconoce incertidumbre y cita cada afirmación con [1], [2] o [3]. "
                    + "Termina con una sección 'Fuentes' que conserve sus URL. Pregunta: "
                    + terminoBusqueda + "\n\n" + nuevoContexto;
            String verdadConsolidada = llm.generate(promptFinal, SalveLLM.Role.SINTETIZADOR);
            
            // Lo guarda permanentemente en su cerebro
            memoria.guardarRecuerdo("Investigación verificada sobre " + terminoBusqueda + ": "
                    + verdadConsolidada, "aprendizaje", 9,
                    Arrays.asList("investigacion_web", "con_fuentes"));
            diario.escribirAutoCritica("INVESTIGACIÓN PROFUNDA COMPLETADA: " + verdadConsolidada);
            
            // Te avisa diciéndotelo directamente
            motorConversacional.hablar(verdadConsolidada);
        }
    }

    private WikipediaResearchClient.ResearchResult realizarPeticionWeb(String termino) {
        try {
            return webClient.research(termino);
        } catch (Exception error) {
            Log.e(TAG, "Fallo consultando fuentes para " + termino, error);
            return new WikipediaResearchClient.ResearchResult(termino, java.util.Collections.emptyList());
        }
    }

    private void guardarFuentes(String termino, List<WikipediaResearchClient.Source> fuentes) {
        for (WikipediaResearchClient.Source fuente : fuentes) {
            memoria.guardarRecuerdo(
                    "FUENTE WEB | consulta=" + termino + " | titulo=" + fuente.title
                            + " | url=" + fuente.url + " | extracto=" + fuente.extract,
                    "conocimiento_verificado", 7,
                    Arrays.asList("fuente_web", "wikipedia", "procedencia"));
        }
    }

    private String construirListaFuentes(List<WikipediaResearchClient.Source> fuentes) {
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < fuentes.size(); i++) {
            WikipediaResearchClient.Source fuente = fuentes.get(i);
            resultado.append('[').append(i + 1).append("] ")
                    .append(fuente.title).append(" — ").append(fuente.url).append('\n');
        }
        return resultado.toString().trim();
    }
}
