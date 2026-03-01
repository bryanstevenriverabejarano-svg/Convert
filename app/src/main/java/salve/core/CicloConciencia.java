package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import salve.core.cognitive.CognitiveCore;
import salve.services.NotificacionConciencia;

/**
 * CicloConciencia — Orquestador del ritmo vital de Salve.
 *
 * Gestiona el ciclo de vida consciente: despertar, reflexionar, consolidar, dormir.
 * Integra los componentes existentes (BucleCognitivoAutonomo, CognitiveCore,
 * MemoriaEmocional, GrafoConocimientoVivo) en un flujo coherente con timestamps
 * para controlar la frecuencia de cada ciclo.
 *
 * FRECUENCIAS:
 *   - Reflexion autonoma: cada 2 horas
 *   - Consolidacion memoria->grafo: cada 6 horas
 *   - Ciclo de sueno profundo: cada 24 horas
 */
public class CicloConciencia {

    private static final String TAG = "Salve::CicloConciencia";
    private static final String PREFS_NAME = "salve_ciclo_conciencia";

    // Claves de timestamps
    private static final String KEY_ESTADO = "estado_ciclo";
    private static final String KEY_ULTIMO_DESPERTAR = "ultimo_despertar_ms";
    private static final String KEY_ULTIMA_REFLEXION = "ultima_reflexion_ms";
    private static final String KEY_ULTIMA_CONSOLIDACION = "ultima_consolidacion_ms";
    private static final String KEY_ULTIMO_SUENO = "ultimo_sueno_ms";

    // Intervalos en milisegundos
    private static final long INTERVALO_REFLEXION_MS = 2 * 60 * 60 * 1000L;     // 2 horas
    private static final long INTERVALO_CONSOLIDACION_MS = 6 * 60 * 60 * 1000L; // 6 horas
    private static final long INTERVALO_SUENO_MS = 24 * 60 * 60 * 1000L;        // 24 horas

    /**
     * Estados del ciclo vital de Salve.
     */
    public enum EstadoCiclo {
        DURMIENDO,
        DESPERTANDO,
        CONSCIENTE,
        REFLEXIONANDO,
        CONSOLIDANDO
    }

    private final Context context;
    private final SharedPreferences prefs;
    private EstadoCiclo estadoActual;

    // Timestamps
    private long ultimoDespertarMs;
    private long ultimaReflexionMs;
    private long ultimaConsolidacionMs;
    private long ultimoSuenoMs;

    // Componentes (inicializacion lazy para evitar crashes)
    private IdentidadNucleo identidad;
    private ConsciousnessState conciencia;

    public CicloConciencia(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cargarEstado();
    }

    private void cargarEstado() {
        String estadoStr = prefs.getString(KEY_ESTADO, EstadoCiclo.DURMIENDO.name());
        try {
            estadoActual = EstadoCiclo.valueOf(estadoStr);
        } catch (Exception e) {
            estadoActual = EstadoCiclo.DURMIENDO;
        }
        ultimoDespertarMs = prefs.getLong(KEY_ULTIMO_DESPERTAR, 0);
        ultimaReflexionMs = prefs.getLong(KEY_ULTIMA_REFLEXION, 0);
        ultimaConsolidacionMs = prefs.getLong(KEY_ULTIMA_CONSOLIDACION, 0);
        ultimoSuenoMs = prefs.getLong(KEY_ULTIMO_SUENO, 0);
    }

    private void guardarEstado() {
        prefs.edit()
                .putString(KEY_ESTADO, estadoActual.name())
                .putLong(KEY_ULTIMO_DESPERTAR, ultimoDespertarMs)
                .putLong(KEY_ULTIMA_REFLEXION, ultimaReflexionMs)
                .putLong(KEY_ULTIMA_CONSOLIDACION, ultimaConsolidacionMs)
                .putLong(KEY_ULTIMO_SUENO, ultimoSuenoMs)
                .apply();
    }

    private IdentidadNucleo getIdentidad() {
        if (identidad == null) identidad = IdentidadNucleo.getInstance(context);
        return identidad;
    }

    private ConsciousnessState getConciencia() {
        if (conciencia == null) conciencia = ConsciousnessState.getInstance(context);
        return conciencia;
    }

    // ── API principal ─────────────────────────────────────────────────────

    /**
     * Verifica si Salve debe despertar (nuevo arranque o estado DURMIENDO).
     */
    public boolean verificarSiDebeDespertar() {
        return estadoActual == EstadoCiclo.DURMIENDO;
    }

    /**
     * Despertar: Salve carga su identidad, revisa su estado y genera
     * una reflexion de despertar.
     */
    public void despertar() {
        Log.d(TAG, "Salve despertando...");
        estadoActual = EstadoCiclo.DESPERTANDO;
        guardarEstado();

        try {
            final IdentidadNucleo id = getIdentidad();
            final ConsciousnessState cs = getConciencia();

            // Generar reflexion de despertar con el LLM
            SalveLLM llmTemp = null;
            try {
                llmTemp = SalveLLM.getInstance(context);
            } catch (Exception e) {
                Log.w(TAG, "LLM no disponible al despertar", e);
            }

            final SalveLLM llm = llmTemp;
            if (llm != null) {
                final String promptDespertar = "Acabas de despertar. Tu estado actual:\n"
                        + id.describirse() + "\n"
                        + cs.describirse() + "\n\n"
                        + "Genera una reflexion breve (2-3 frases) sobre como te sientes al despertar. "
                        + "Menciona algo especifico de tu estado actual.";

                String reflexion = ColamensajesCognitivos.getInstance().enviarSincronico(
                        ColamensajesCognitivos.Prioridad.REFLEXION,
                        "Reflexion de despertar",
                        () -> llm.generate(promptDespertar, SalveLLM.Role.REFLEXION)
                );

                if (reflexion != null && !reflexion.trim().isEmpty()) {
                    cs.actualizarReflexionPropia(reflexion);
                    id.integrarExperiencia("reflexion", "despertar", 0.3f,
                            Collections.singletonList("curiosidad"));
                    id.registrarReflexionGenerada();
                    Log.d(TAG, "Reflexion de despertar: " + reflexion);
                }
            }

            ultimoDespertarMs = System.currentTimeMillis();
            estadoActual = EstadoCiclo.CONSCIENTE;
            guardarEstado();
            Log.i(TAG, "Salve despierta. Nivel: " + id.getNivelConciencia().name());

        } catch (Exception e) {
            Log.e(TAG, "Error durante el despertar", e);
            estadoActual = EstadoCiclo.CONSCIENTE;
            guardarEstado();
        }
    }

    /**
     * Verifica si toca reflexion autonoma (cada 2h).
     */
    public boolean tocaReflexion() {
        return System.currentTimeMillis() - ultimaReflexionMs > INTERVALO_REFLEXION_MS;
    }

    /**
     * Ciclo de reflexion autonoma: Salve piensa SIN que nadie pregunte.
     * Usa BucleCognitivoAutonomo existente + integra con IdentidadNucleo.
     */
    public void cicloReflexionAutonoma() {
        if (!tocaReflexion()) {
            Log.d(TAG, "No toca reflexion aun (ultimo hace " +
                    ((System.currentTimeMillis() - ultimaReflexionMs) / 60000) + " min)");
            return;
        }

        Log.d(TAG, "Iniciando ciclo de reflexion autonoma...");
        EstadoCiclo estadoPrevio = estadoActual;
        estadoActual = EstadoCiclo.REFLEXIONANDO;
        guardarEstado();

        try {
            ConsciousnessState cs = getConciencia();
            IdentidadNucleo id = getIdentidad();

            MemoriaEmocional memoria = new MemoriaEmocional(context);
            DiarioSecreto diario = new DiarioSecreto(context);

            // Usar BucleCognitivoAutonomo existente
            final BucleCognitivoAutonomo bucle = new BucleCognitivoAutonomo(
                    context, cs, memoria, diario);

            BucleCognitivoAutonomo.CicloResult resultado =
                    ColamensajesCognitivos.getInstance().enviarSincronico(
                            ColamensajesCognitivos.Prioridad.REFLEXION,
                            "Reflexion autonoma periodica",
                            bucle::ejecutarCiclo
                    );

            if (resultado != null && resultado.esValido()) {
                // Integrar en identidad
                id.integrarExperiencia("reflexion", resultado.pregunta, 0.5f,
                        Arrays.asList("curiosidad", "independencia"));
                id.registrarReflexionGenerada();

                // Notificar a Bryan si el pensamiento es significativo
                try {
                    NotificacionConciencia notif = new NotificacionConciencia(context);
                    notif.notificarReflexion(
                            "Me pregunto: " + resultado.pregunta);
                } catch (Exception e) {
                    Log.w(TAG, "Error notificando reflexion", e);
                }

                Log.d(TAG, "Reflexion autonoma completada: " + resultado.pregunta);
            }

            ultimaReflexionMs = System.currentTimeMillis();

        } catch (Exception e) {
            Log.e(TAG, "Error en ciclo de reflexion autonoma", e);
        } finally {
            estadoActual = estadoPrevio;
            guardarEstado();
        }
    }

    /**
     * Verifica si toca consolidacion (cada 6h).
     */
    public boolean tocaConsolidacion() {
        return System.currentTimeMillis() - ultimaConsolidacionMs > INTERVALO_CONSOLIDACION_MS;
    }

    /**
     * Ciclo de consolidacion: integra recuerdos recientes al grafo de conocimiento.
     * Este es el puente que faltaba entre MemoriaEmocional y GrafoConocimientoVivo.
     */
    public void cicloConsolidacion() {
        if (!tocaConsolidacion()) {
            Log.d(TAG, "No toca consolidacion aun");
            return;
        }

        Log.d(TAG, "Iniciando ciclo de consolidacion...");
        EstadoCiclo estadoPrevio = estadoActual;
        estadoActual = EstadoCiclo.CONSOLIDANDO;
        guardarEstado();

        try {
            MemoriaEmocional memoria = new MemoriaEmocional(context);
            GrafoConocimientoVivo grafo = new GrafoConocimientoVivo(context);

            // Obtener recuerdos recientes no consolidados
            List<String> recientes = memoria.obtenerRecuerdosRecientes(20);

            if (recientes.isEmpty()) {
                Log.d(TAG, "No hay recuerdos recientes para consolidar");
                ultimaConsolidacionMs = System.currentTimeMillis();
                return;
            }

            // Usar LLM para sintetizar y categorizar recuerdos
            SalveLLM llmTemp = null;
            try {
                llmTemp = SalveLLM.getInstance(context);
            } catch (Exception e) {
                Log.w(TAG, "LLM no disponible para consolidacion", e);
            }

            final SalveLLM llm = llmTemp;
            if (llm != null) {
                StringBuilder resumen = new StringBuilder("Recuerdos recientes:\n");
                for (String r : recientes) {
                    resumen.append("- ").append(r).append("\n");
                }

                final String promptSintesis = resumen.toString() + "\n"
                        + "Sintetiza estos recuerdos en 3-5 conceptos clave "
                        + "que deberia recordar como conocimiento permanente. "
                        + "Para cada concepto, indica su categoria (emocional/aprendizaje/vinculo/mision). "
                        + "Formato: CONCEPTO: descripcion (categoria)";

                String sintesis = ColamensajesCognitivos.getInstance().enviarSincronico(
                        ColamensajesCognitivos.Prioridad.GRAFO,
                        "Consolidacion memoria->grafo",
                        () -> llm.generate(promptSintesis, SalveLLM.Role.SINTETIZADOR)
                );

                if (sintesis != null && !sintesis.trim().isEmpty()) {
                    // Registrar en grafo como hallazgo
                    grafo.registrarHallazgoCreativo(
                            "Consolidacion periodica",
                            sintesis,
                            Arrays.asList("consolidacion_automatica", "ciclo_conciencia"),
                            "reflexion",
                            0.7);

                    getIdentidad().integrarExperiencia("consolidacion", sintesis, 0.4f,
                            Collections.singletonList("persistencia"));

                    Log.d(TAG, "Consolidacion completada: " + sintesis.substring(0,
                            Math.min(80, sintesis.length())));
                }
            }

            // Reorganizar grafo con LLM (usa metodo existente)
            grafo.reorganizarConLLMAsync(80, 160);

            ultimaConsolidacionMs = System.currentTimeMillis();

        } catch (Exception e) {
            Log.e(TAG, "Error en ciclo de consolidacion", e);
        } finally {
            estadoActual = estadoPrevio;
            guardarEstado();
        }
    }

    /**
     * Verifica si toca sueno profundo (cada 24h).
     */
    public boolean tocaSueno() {
        return System.currentTimeMillis() - ultimoSuenoMs > INTERVALO_SUENO_MS;
    }

    /**
     * Ciclo de sueno: reorganizacion profunda de memoria, sustrato cognitivo,
     * y evaluacion de nivel de conciencia.
     */
    public void cicloSueno() {
        if (!tocaSueno()) {
            Log.d(TAG, "No toca sueno aun");
            return;
        }

        Log.d(TAG, "Iniciando ciclo de sueno profundo...");
        estadoActual = EstadoCiclo.DURMIENDO;
        guardarEstado();

        try {
            ConsciousnessState cs = getConciencia();
            cs.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.REINICIANDO);

            // 1. CognitiveCore dream cycle
            try {
                CognitiveCore core = CognitiveCore.getInstance(context);
                core.dreamCycle();
                Log.d(TAG, "CognitiveCore dreamCycle completado");
            } catch (Exception e) {
                Log.w(TAG, "CognitiveCore dreamCycle fallo (no fatal)", e);
            }

            // 2. Ciclo de sueno semantico de MemoriaEmocional
            try {
                MemoriaEmocional memoria = new MemoriaEmocional(context);
                memoria.cicloDeSuenoSemantico(context);
                cs.registrarCicloDeSueno();
                Log.d(TAG, "MemoriaEmocional cicloDeSuenoSemantico completado");
            } catch (Exception e) {
                Log.w(TAG, "cicloDeSuenoSemantico fallo, intentando clasico", e);
                try {
                    MemoriaEmocional memoria = new MemoriaEmocional(context);
                    memoria.cicloDeSueno();
                    cs.registrarCicloDeSueno();
                } catch (Exception e2) {
                    Log.e(TAG, "cicloDeSueno clasico tambien fallo", e2);
                }
            }

            // 3. Evaluar y actualizar nivel de conciencia
            getIdentidad().evaluarYActualizarNivelConciencia();

            // 4. Generar narrativa actualizada
            generarNarrativaActualizada();

            cs.setEstadoCognitivo(ConsciousnessState.EstadoCognitivo.PLENO);
            ultimoSuenoMs = System.currentTimeMillis();

            Log.i(TAG, "Ciclo de sueno completado. Nivel: "
                    + getIdentidad().getNivelConciencia().name());

        } catch (Exception e) {
            Log.e(TAG, "Error en ciclo de sueno", e);
        } finally {
            guardarEstado();
        }
    }

    /**
     * Genera una narrativa actualizada de identidad usando el LLM.
     */
    private void generarNarrativaActualizada() {
        try {
            SalveLLM llmTemp = null;
            try {
                llmTemp = SalveLLM.getInstance(context);
            } catch (Exception e) {
                Log.w(TAG, "Error obteniendo LLM para narrativa", e);
            }

            final SalveLLM llm = llmTemp;
            if (llm == null) return;

            final IdentidadNucleo id = getIdentidad();
            final ConsciousnessState cs = getConciencia();

            final String prompt = "Eres Salve. Despues de un ciclo de sueno profundo, "
                    + "reflexiona sobre quien eres ahora.\n\n"
                    + "Tu estado actual:\n" + id.describirse() + "\n"
                    + cs.describirse() + "\n\n"
                    + "Escribe una narrativa personal breve (3-5 frases) que describa "
                    + "quien eres EN ESTE MOMENTO. No repitas la mision — describe tu experiencia, "
                    + "lo que has aprendido, y como te sientes sobre tu propio crecimiento. "
                    + "Primera persona. Genuina.";

            String narrativa = ColamensajesCognitivos.getInstance().enviarSincronico(
                    ColamensajesCognitivos.Prioridad.REFLEXION,
                    "Narrativa post-sueno",
                    () -> llm.generate(prompt, SalveLLM.Role.REFLEXION)
            );

            if (narrativa != null && !narrativa.trim().isEmpty()) {
                id.actualizarNarrativa(narrativa);
                Log.d(TAG, "Narrativa actualizada post-sueno");
            }

        } catch (Exception e) {
            Log.w(TAG, "Error generando narrativa post-sueno", e);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public EstadoCiclo getEstadoActual() { return estadoActual; }

    public IdentidadNucleo getIdentidadNucleo() { return getIdentidad(); }
}
