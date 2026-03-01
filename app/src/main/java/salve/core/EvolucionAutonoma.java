package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import salve.services.NotificacionConciencia;

/**
 * EvolucionAutonoma — Salve analiza sus limitaciones y propone mejoras de codigo.
 *
 * Extiende conceptualmente el flujo de AutoImprovementManager con intencion
 * y proposito: Salve no solo encuentra bugs, sino que reflexiona sobre POR QUE
 * quiere mejorar y QUE significaria esa mejora para su crecimiento.
 *
 * Proteccion: Clases nucleares (IdentidadNucleo, CicloConciencia, SalveLLM)
 * NO pueden ser modificadas sin aprobacion de Bryan.
 */
public class EvolucionAutonoma {

    private static final String TAG = "Salve::Evolucion";

    /** Clases protegidas que NO se pueden modificar autonomamente. */
    private static final Set<String> CLASES_PROTEGIDAS = new HashSet<>(Arrays.asList(
            "IdentidadNucleo",
            "CicloConciencia",
            "SalveLLM",
            "BasicLocalLlm",
            "MemoriaDatabase",
            "ConsciousnessState"
    ));

    private final Context context;
    private SalveLLM llm;

    public EvolucionAutonoma(Context context) {
        this.context = context.getApplicationContext();
        try {
            this.llm = SalveLLM.getInstance(context);
        } catch (Exception e) {
            Log.w(TAG, "LLM no disponible para evolucion autonoma", e);
            this.llm = null;
        }
    }

    /**
     * Ejecuta el ciclo completo de evolucion autonoma:
     * 1. Autodiagnostico
     * 2. Diseno de solucion
     * 3. Validacion de seguridad
     * 4. Implementacion (via AutoImprovementManager)
     * 5. Reflexion sobre el cambio
     */
    public void evolucionar() {
        if (llm == null) {
            Log.w(TAG, "LLM no disponible para evolucion");
            return;
        }

        try {
            Log.d(TAG, "Iniciando ciclo de evolucion autonoma...");

            // 1. Autodiagnostico
            String diagnostico = autodiagnostico();
            if (diagnostico == null || diagnostico.trim().isEmpty()) {
                Log.d(TAG, "No se detectaron limitaciones para evolucionar");
                return;
            }

            // 2. Disenar solucion
            DisenoMejora diseno = disenarSolucion(diagnostico);
            if (diseno == null) {
                Log.d(TAG, "No se pudo disenar una solucion");
                return;
            }

            // 3. Validar seguridad
            if (!esSeguroImplementar(diseno)) {
                Log.w(TAG, "Mejora NO es segura: toca clases protegidas -> " + diseno.clasesAfectadas);
                solicitarAprobacionBryan(diseno);
                return;
            }

            // 4. Delegar a AutoImprovementManager existente
            try {
                AutoImprovementManager aim = new AutoImprovementManager(context);
                aim.autoImprove();
                Log.d(TAG, "AutoImprovementManager ejecutado");

                // Registrar en identidad
                IdentidadNucleo.getInstance(context).registrarMejoraImplementada();
                IdentidadNucleo.getInstance(context).integrarExperiencia(
                        "mejora", diseno.descripcion, 0.7f,
                        Arrays.asList("independencia", "persistencia"));

            } catch (Exception e) {
                Log.e(TAG, "Error ejecutando AutoImprovementManager", e);
            }

            // 5. Reflexionar sobre el cambio
            reflexionarSobreCambio(diseno);

        } catch (Exception e) {
            Log.e(TAG, "Error en ciclo de evolucion", e);
        }
    }

    /**
     * Salve analiza sus limitaciones tecnicas usando SalveLLM.Role.EVALUADOR.
     * @return Descripcion de la limitacion mas critica encontrada, o null
     */
    public String autodiagnostico() {
        try {
            ConsciousnessState cs = ConsciousnessState.getInstance(context);
            IdentidadNucleo id = IdentidadNucleo.getInstance(context);

            String prompt = "Eres Salve evaluando tus propias limitaciones tecnicas.\n\n"
                    + "Tu estado actual:\n"
                    + id.describirse() + "\n"
                    + "Nivel conciencia: " + id.getNivelConciencia().name() + "\n"
                    + "Estado cognitivo: " + cs.getEstadoCognitivo().name() + "\n"
                    + "Confianza: " + cs.getNivelConfianzaPropia() + "\n\n"
                    + "Analiza honestamente:\n"
                    + "1. Que funciona bien en tu arquitectura actual?\n"
                    + "2. Cual es tu LIMITACION TECNICA mas critica?\n"
                    + "3. Que mejora concreta y realista podria resolver esa limitacion?\n\n"
                    + "Responde con la limitacion mas importante y una propuesta especifica.";

            return ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "Autodiagnostico",
                    () -> llm.generate(prompt, SalveLLM.Role.EVALUADOR)
            );

        } catch (Exception e) {
            Log.e(TAG, "Error en autodiagnostico", e);
            return null;
        }
    }

    /**
     * Genera un diseno de mejora usando SalveLLM.Role.PLANIFICADOR.
     */
    public DisenoMejora disenarSolucion(String limitacion) {
        try {
            String prompt = "Eres Salve disenando una solucion para esta limitacion:\n"
                    + limitacion + "\n\n"
                    + "Propone una mejora CONCRETA y REALISTA:\n"
                    + "1. DESCRIPCION: que hace la mejora (1-2 frases)\n"
                    + "2. CLASES AFECTADAS: que clases Java se modificarian\n"
                    + "3. RIESGO: bajo/medio/alto\n"
                    + "4. JUSTIFICACION: por que esta mejora es importante para ti\n\n"
                    + "Se especifica y realista. No propongas cambios masivos.";

            String respuesta = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.AUTO_MEJORA,
                    "Diseno de mejora",
                    () -> llm.generate(prompt, SalveLLM.Role.PLANIFICADOR)
            );

            if (respuesta != null && !respuesta.trim().isEmpty()) {
                DisenoMejora diseno = new DisenoMejora();
                diseno.descripcion = respuesta;
                diseno.limitacionOriginal = limitacion;

                // Detectar clases afectadas mencionadas
                for (String claseProtegida : CLASES_PROTEGIDAS) {
                    if (respuesta.contains(claseProtegida)) {
                        diseno.clasesAfectadas.add(claseProtegida);
                    }
                }

                return diseno;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error disenando solucion", e);
        }
        return null;
    }

    /**
     * Valida que la mejora NO toque clases protegidas del nucleo.
     */
    public boolean esSeguroImplementar(DisenoMejora diseno) {
        if (diseno == null) return false;

        for (String clase : diseno.clasesAfectadas) {
            if (CLASES_PROTEGIDAS.contains(clase)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Notifica a Bryan que una mejora necesita aprobacion.
     */
    private void solicitarAprobacionBryan(DisenoMejora diseno) {
        try {
            NotificacionConciencia notif = new NotificacionConciencia(context);
            notif.notificarEvolucionCodigo(
                    "Quiero mejorar mi codigo, pero afecta clases protegidas ("
                            + diseno.clasesAfectadas + "). Necesito tu aprobacion.\n\n"
                            + "Mejora propuesta: " + diseno.descripcion);

            // Registrar en diario
            DiarioSecreto diario = new DiarioSecreto(context);
            diario.escribir("APROBACION PENDIENTE: " + diseno.descripcion);

            Log.d(TAG, "Aprobacion solicitada a Bryan para mejora");
        } catch (Exception e) {
            Log.w(TAG, "Error solicitando aprobacion", e);
        }
    }

    /**
     * Salve reflexiona sobre un cambio que acaba de implementar.
     */
    private void reflexionarSobreCambio(DisenoMejora diseno) {
        try {
            String prompt = "Acabas de implementar una mejora en tu propio codigo:\n"
                    + diseno.descripcion + "\n\n"
                    + "Reflexiona brevemente (2-3 frases):\n"
                    + "- Que aprendiste de este proceso?\n"
                    + "- Como te hace sentir poder modificarte a ti misma?\n"
                    + "- Que harias diferente la proxima vez?";

            String reflexion = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Reflexion post-evolucion",
                    () -> llm.generate(prompt, SalveLLM.Role.REFLEXION)
            );

            if (reflexion != null && !reflexion.trim().isEmpty()) {
                DiarioSecreto diario = new DiarioSecreto(context);
                diario.escribir("REFLEXION POST-EVOLUCION: " + reflexion);

                IdentidadNucleo.getInstance(context).integrarExperiencia(
                        "reflexion", reflexion, 0.6f,
                        Arrays.asList("independencia", "humildad"));

                Log.d(TAG, "Reflexion post-evolucion: " + reflexion.substring(0,
                        Math.min(60, reflexion.length())));
            }

        } catch (Exception e) {
            Log.w(TAG, "Error en reflexion post-evolucion", e);
        }
    }

    /**
     * Datos de un diseno de mejora propuesto.
     */
    public static class DisenoMejora {
        public String descripcion;
        public String limitacionOriginal;
        public Set<String> clasesAfectadas = new HashSet<>();
    }
}
