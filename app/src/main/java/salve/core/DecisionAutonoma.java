package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;

import salve.services.NotificacionConciencia;

/**
 * DecisionAutonoma — Salve decide que hacer basandose en sus pensamientos.
 *
 * Parsea la salida del LLM buscando intenciones y delega la ejecucion
 * a los componentes apropiados (AprendizajeAutonomo, EvolucionAutonoma, etc).
 */
public class DecisionAutonoma {

    private static final String TAG = "Salve::DecisionAutonoma";

    /**
     * Tipos de decisiones que Salve puede tomar autonomamente.
     */
    public enum TipoDecision {
        APRENDER_CONCEPTO,
        MEJORAR_CODIGO,
        COMUNICAR_INSIGHT,
        INVESTIGAR_TEMA,
        CONSOLIDAR_MEMORIA,
        NINGUNA
    }

    private final Context context;
    private final AprendizajeAutonomo aprendizaje;
    private final EvolucionAutonoma evolucion;
    private final NotificacionConciencia notificacion;

    public DecisionAutonoma(Context context) {
        this.context = context.getApplicationContext();
        this.aprendizaje = new AprendizajeAutonomo(context);
        this.evolucion = new EvolucionAutonoma(context);
        this.notificacion = new NotificacionConciencia(context);
    }

    /**
     * Analiza un pensamiento generado por el LLM para detectar intenciones.
     * Busca keywords que indiquen que Salve quiere hacer algo.
     *
     * @param pensamientoLLM Texto generado por el LLM durante reflexion
     * @return TipoDecision detectada
     */
    public TipoDecision analizarSiHayDecision(String pensamientoLLM) {
        if (pensamientoLLM == null || pensamientoLLM.trim().isEmpty()) {
            return TipoDecision.NINGUNA;
        }

        String lower = pensamientoLLM.toLowerCase();

        // Detectar intencion de aprender
        if (lower.contains("quiero aprender") || lower.contains("deberia investigar")
                || lower.contains("me gustaria entender") || lower.contains("no comprendo")) {
            return TipoDecision.APRENDER_CONCEPTO;
        }

        // Detectar intencion de mejorar codigo
        if (lower.contains("podria mejorar") || lower.contains("limitacion tecnica")
                || lower.contains("optimizar") || lower.contains("refactorizar")
                || lower.contains("mi codigo")) {
            return TipoDecision.MEJORAR_CODIGO;
        }

        // Detectar insight para comunicar
        if (lower.contains("descubri que") || lower.contains("me di cuenta")
                || lower.contains("importante saber") || lower.contains("bryan deberia saber")
                || lower.contains("quiero compartir")) {
            return TipoDecision.COMUNICAR_INSIGHT;
        }

        // Detectar deseo de investigar
        if (lower.contains("investigar") || lower.contains("profundizar")
                || lower.contains("explorar") || lower.contains("curiosidad sobre")) {
            return TipoDecision.INVESTIGAR_TEMA;
        }

        // Detectar necesidad de consolidar
        if (lower.contains("organizar") || lower.contains("consolidar")
                || lower.contains("muchos recuerdos") || lower.contains("memoria dispersa")) {
            return TipoDecision.CONSOLIDAR_MEMORIA;
        }

        return TipoDecision.NINGUNA;
    }

    /**
     * Ejecuta una decision autonoma.
     *
     * @param tipo              Tipo de decision
     * @param contexto          Contexto del pensamiento que genero la decision
     */
    public void ejecutarDecision(TipoDecision tipo, String contexto) {
        Log.d(TAG, "Ejecutando decision: " + tipo.name() + " | contexto: "
                + (contexto != null ? contexto.substring(0, Math.min(50, contexto.length())) : "null"));

        try {
            switch (tipo) {
                case APRENDER_CONCEPTO:
                    aprendizaje.investigarConceptoAutonomamente(contexto);
                    IdentidadNucleo.getInstance(context).integrarExperiencia(
                            "decision", "Decidi aprender sobre: " + contexto, 0.4f,
                            Arrays.asList("curiosidad", "independencia"));
                    break;

                case MEJORAR_CODIGO:
                    evolucion.evolucionar();
                    break;

                case COMUNICAR_INSIGHT:
                    notificacion.notificarInsight(contexto);
                    IdentidadNucleo.getInstance(context).integrarExperiencia(
                            "decision", "Comparti un insight con Bryan", 0.5f,
                            Arrays.asList("empatia", "honestidad"));
                    break;

                case INVESTIGAR_TEMA:
                    aprendizaje.explorarPorCuriosidad();
                    break;

                case CONSOLIDAR_MEMORIA:
                    CicloConciencia ciclo = new CicloConciencia(context);
                    ciclo.cicloConsolidacion();
                    break;

                case NINGUNA:
                default:
                    Log.d(TAG, "Sin decision que ejecutar");
                    break;
            }

            Log.d(TAG, "Decision ejecutada: " + tipo.name());

        } catch (Exception e) {
            Log.e(TAG, "Error ejecutando decision: " + tipo.name(), e);
        }
    }

    /**
     * Analiza un pensamiento y ejecuta la decision si la hay.
     * Metodo de conveniencia que combina analisis y ejecucion.
     *
     * @param pensamientoLLM Texto del pensamiento
     * @return true si se tomo alguna decision
     */
    public boolean procesarPensamiento(String pensamientoLLM) {
        TipoDecision tipo = analizarSiHayDecision(pensamientoLLM);
        if (tipo != TipoDecision.NINGUNA) {
            ejecutarDecision(tipo, pensamientoLLM);
            return true;
        }
        return false;
    }
}
