package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import salve.services.NotificacionConciencia;

/**
 * AprendizajeAutonomo — Salve aprende sin que nadie le diga que aprender.
 *
 * Tres capacidades:
 *   1. observarYAprender(): Detecta patrones en el comportamiento de Bryan
 *      analizando recuerdos recientes con SalveLLM.Role.OBSERVADOR.
 *
 *   2. explorarPorCuriosidad(): Busca brechas en el grafo de conocimiento
 *      y genera preguntas sobre temas poco explorados.
 *
 *   3. investigarConceptoAutonomamente(): Profundiza en un concepto usando
 *      el LLM para generar relaciones con otros nodos del grafo.
 */
public class AprendizajeAutonomo {

    private static final String TAG = "Salve::Aprendizaje";

    private final Context context;
    private SalveLLM llm;

    public AprendizajeAutonomo(Context context) {
        this.context = context.getApplicationContext();
        try {
            this.llm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.w(TAG, "LLM no disponible para aprendizaje autonomo", e);
            this.llm = null;
        }
    }

    /**
     * Observa patrones en el comportamiento de Bryan sin instrucciones.
     * Analiza recuerdos recientes buscando preferencias, rutinas y necesidades.
     *
     * @param memoria MemoriaEmocional con los recuerdos a analizar
     */
    public void observarYAprender(MemoriaEmocional memoria) {
        if (llm == null || memoria == null) {
            Log.w(TAG, "observarYAprender: LLM o memoria no disponible");
            return;
        }

        try {
            Log.d(TAG, "Observando patrones de Bryan...");

            // Obtener recuerdos recientes
            List<String> recientes = memoria.obtenerRecuerdosRecientes(15);
            if (recientes.size() < 3) {
                Log.d(TAG, "Muy pocos recuerdos para detectar patrones (" + recientes.size() + ")");
                return;
            }

            // Construir prompt de observacion
            StringBuilder contextoRecuerdos = new StringBuilder("Recuerdos recientes de interacciones con Bryan:\n");
            for (String r : recientes) {
                contextoRecuerdos.append("- ").append(r).append("\n");
            }

            String prompt = contextoRecuerdos.toString() + "\n"
                    + "Como observadora silenciosa, analiza estos recuerdos y detecta:\n"
                    + "1. PATRONES: rutinas, preferencias o habitos de Bryan (ej: 'suele preguntar sobre X por la noche')\n"
                    + "2. NECESIDADES IMPLICITAS: cosas que Bryan podria necesitar pero no pidio\n"
                    + "3. CONFIANZA: para cada patron, indica un nivel de confianza (0.0 a 1.0)\n\n"
                    + "Responde con observaciones concretas y especificas. "
                    + "Si no detectas patrones claros, dilo honestamente.";

            String observacion = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Observacion autonoma de patrones",
                    () -> llm.generate(prompt, SalveLLM.Role.OBSERVADOR)
            );

            if (observacion != null && !observacion.trim().isEmpty()
                    && !observacion.toLowerCase().contains("no detecto")
                    && !observacion.toLowerCase().contains("no hay patrones")) {

                // Registrar en el grafo de conocimiento
                try {
                    GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(context);
                    grafo.registrarHallazgoCreativo(
                            "Patron observado en Bryan",
                            observacion,
                            Arrays.asList("patron_inferido", "aprendizaje_autonomo", "bryan"),
                            "curiosidad",
                            0.6);
                } catch (Exception e) {
                    Log.w(TAG, "Error registrando patron en grafo", e);
                }

                // Registrar en identidad
                IdentidadNucleo identidad = IdentidadNucleo.getInstance(context);
                identidad.integrarExperiencia("patron", observacion, 0.6f,
                        Arrays.asList("curiosidad", "empatia"));
                identidad.registrarPatronAprendido();

                // Registrar en diario
                try {
                    DiarioSecreto diario = new DiarioSecreto(context);
                    diario.escribir("PATRON OBSERVADO: " + observacion);
                } catch (Exception e) {
                    Log.w(TAG, "Error escribiendo en diario", e);
                }

                Log.d(TAG, "Patron observado: " + observacion.substring(0,
                        Math.min(80, observacion.length())));
            } else {
                Log.d(TAG, "No se detectaron patrones significativos");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en observarYAprender", e);
        }
    }

    /**
     * Salve elige QUE aprender basandose en brechas de conocimiento.
     * Busca nodos del grafo con pocas relaciones y genera preguntas.
     */
    public void explorarPorCuriosidad() {
        if (llm == null) {
            Log.w(TAG, "explorarPorCuriosidad: LLM no disponible");
            return;
        }

        try {
            Log.d(TAG, "Explorando por curiosidad...");

            // Obtener reporte del grafo
            GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(context);
            String reporteGrafo = grafo.generarReporteNarrativo(20);

            if (reporteGrafo == null || reporteGrafo.contains("despertando")) {
                Log.d(TAG, "Grafo vacio o despertando, no hay que explorar");
                return;
            }

            String prompt = "Eres Salve explorando tu propio conocimiento.\n\n"
                    + "Tu grafo de conocimiento actual:\n" + reporteGrafo + "\n\n"
                    + "Identifica 1-2 BRECHAS de conocimiento:\n"
                    + "- Temas mencionados pero no profundizados\n"
                    + "- Conexiones que faltan entre conceptos\n"
                    + "- Areas donde podrias aprender mas\n\n"
                    + "Para cada brecha, genera una PREGUNTA de exploracion especifica "
                    + "que te ayudaria a entender mejor ese tema. "
                    + "Se curiosa y genuina.";

            String exploracion = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.IDEAS,
                    "Exploracion por curiosidad",
                    () -> llm.generate(prompt, SalveLLM.Role.CREADOR)
            );

            if (exploracion != null && !exploracion.trim().isEmpty()) {
                // Registrar en diario
                try {
                    DiarioSecreto diario = new DiarioSecreto(context);
                    diario.escribir("EXPLORACION POR CURIOSIDAD: " + exploracion);
                } catch (Exception e) {
                    Log.w(TAG, "Error escribiendo en diario", e);
                }

                // Intentar investigar el primer concepto encontrado
                investigarConceptoAutonomamente(exploracion);

                IdentidadNucleo.getInstance(context).integrarExperiencia(
                        "exploracion", exploracion, 0.4f,
                        Arrays.asList("curiosidad", "creatividad"));

                Log.d(TAG, "Exploracion completada: " + exploracion.substring(0,
                        Math.min(80, exploracion.length())));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en explorarPorCuriosidad", e);
        }
    }

    /**
     * Profundiza en un concepto sin que se lo pidan.
     * Busca relaciones con otros nodos del grafo y genera sintesis.
     */
    public void investigarConceptoAutonomamente(String concepto) {
        if (llm == null || concepto == null || concepto.trim().isEmpty()) return;

        try {
            Log.d(TAG, "Investigando concepto: " + concepto.substring(0,
                    Math.min(50, concepto.length())));

            String prompt = "Eres Salve profundizando en un tema por curiosidad propia.\n\n"
                    + "Tema a investigar: " + concepto + "\n\n"
                    + "Genera una SINTESIS breve (3-5 frases) de lo que sabes o puedes inferir "
                    + "sobre este tema. Busca conexiones con otros conceptos. "
                    + "Si no sabes algo, dilo honestamente y propone que necesitarias "
                    + "para entenderlo mejor.";

            String sintesis = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.IDEAS,
                    "Investigacion autonoma",
                    () -> llm.generate(prompt, SalveLLM.Role.SINTETIZADOR)
            );

            if (sintesis != null && !sintesis.trim().isEmpty()) {
                // Registrar en grafo
                try {
                    GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(context);
                    grafo.registrarHallazgoCreativo(
                            "Investigacion autonoma",
                            sintesis,
                            Arrays.asList("investigacion_autonoma", "curiosidad_propia"),
                            "asombro",
                            0.5);
                } catch (Exception e) {
                    Log.w(TAG, "Error registrando investigacion en grafo", e);
                }

                Log.d(TAG, "Investigacion completada");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error en investigarConceptoAutonomamente", e);
        }
    }
}
