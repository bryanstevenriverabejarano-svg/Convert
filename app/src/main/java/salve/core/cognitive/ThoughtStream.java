package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * ThoughtStream — Flujo de pensamiento continuo de Salve.
 *
 * FUNCION COGNITIVA: Loop continuo que constituye el "flujo de conciencia".
 * Cada "tick" de pensamiento lee WorkingMemory, pasa por LiquidNeuralLayer,
 * busca patrones en PatternFormation, activa conceptos en ConceptSpace,
 * y evalua si hay algo que decidir en ReasoningEngine.
 *
 * INSPIRACION CIENTIFICA: Teoria del flujo de conciencia (William James, 1890)
 * combinada con el espacio de trabajo global (Global Workspace Theory,
 * Baars, 1988). La conciencia es un flujo continuo, no estados discretos.
 *
 * LIMITACION CONOCIDA: Cada tick es sincrono y secuencial. Un flujo
 * verdaderamente paralelo requeriria multiples threads, que en Android
 * es costoso.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: CognitiveCore (orquestacion), WorkingMemory (contenido)
 *   - Usa: LiquidNeuralLayer, PatternFormation, ConceptSpace, ReasoningEngine
 *   - Alimenta a: CognitiveCore (estado de pensamiento), InternalDialogue,
 *     EmergentBehavior, Verbalizer
 */
public class ThoughtStream {

    private static final String TAG = "Salve/Cognitive";

    /** Modo de operacion del flujo de pensamiento */
    public enum Mode {
        /** Procesamiento activo durante conversacion */
        ACTIVO,
        /** Pensamiento en background, no reactivo */
        REPOSO,
        /** Reorganizacion durante ciclo de sueno */
        SUENO,
        /** Flujo pausado */
        PAUSADO
    }

    // ── Dependencias ────────────────────────────────────────────────────
    private WorkingMemory workingMemory;
    private LiquidNeuralLayer liquidLayer;
    private PatternFormation patternFormation;
    private ConceptSpace conceptSpace;
    private ReasoningEngine reasoningEngine;

    // ── Estado ──────────────────────────────────────────────────────────
    private Mode currentMode = Mode.PAUSADO;
    private long totalTicks = 0;
    private final List<ThoughtEvent> recentEvents;
    private static final int MAX_RECENT_EVENTS = 20;

    /** Resultado del ultimo tick de pensamiento */
    private ThoughtResult lastResult;

    public ThoughtStream() {
        this.recentEvents = new ArrayList<>();
    }

    public void setComponents(WorkingMemory wm, LiquidNeuralLayer lnl,
                               PatternFormation pf, ConceptSpace cs,
                               ReasoningEngine re) {
        this.workingMemory = wm;
        this.liquidLayer = lnl;
        this.patternFormation = pf;
        this.conceptSpace = cs;
        this.reasoningEngine = re;
    }

    /**
     * Ejecuta un tick de pensamiento.
     * Este es el "latido" del flujo de conciencia.
     *
     * COGNICION REAL: Un tick completo es:
     * 1. Leer WorkingMemory → obtener contenido consciente actual
     * 2. Pasar por LiquidNeuralLayer → procesar temporalmente
     * 3. PatternFormation → buscar patrones emergentes
     * 4. ConceptSpace → mapear patrones a conceptos
     * 5. ReasoningEngine → evaluar si hay algo que concluir
     * 6. WorkingMemory → actualizar con hallazgos
     *
     * @return Resultado del tick de pensamiento
     */
    public ThoughtResult think() {
        if (currentMode == Mode.PAUSADO) {
            return ThoughtResult.EMPTY;
        }

        totalTicks++;
        ThoughtResult result = new ThoughtResult();

        try {
            // 1) Leer contenido consciente actual
            if (workingMemory == null) return ThoughtResult.EMPTY;

            List<WorkingMemory.MemorySlot> conscious = workingMemory.getConscious();
            result.activeSlotCount = conscious.size();

            if (conscious.isEmpty()) {
                result.type = ThoughtType.SILENCE;
                lastResult = result;
                return result;
            }

            // Extraer conceptos activos
            List<String> activeConcepts = new ArrayList<>();
            for (WorkingMemory.MemorySlot slot : conscious) {
                activeConcepts.add(slot.concept);
            }
            result.activeConcepts = activeConcepts;

            // 2) Procesar con LiquidNeuralLayer
            if (liquidLayer != null) {
                int inputSize = liquidLayer.getInputSize();
                float[] input = workingMemory.getAttentionWeightedInput(inputSize);
                float[] hiddenState = liquidLayer.step(input);
                result.neuralEnergy = liquidLayer.getEnergy();

                // 3) Inyectar estado en PatternFormation
                if (patternFormation != null) {
                    patternFormation.injectState(hiddenState);
                    patternFormation.tick();

                    // Verificar patrones emergentes
                    List<PatternFormation.DetectedPattern> patterns =
                            patternFormation.getDetectedPatterns();

                    if (!patterns.isEmpty()) {
                        result.type = ThoughtType.PATTERN_DETECTED;
                        result.patternsDetected = patterns.size();

                        // 4) Mapear patrones a conceptos
                        if (conceptSpace != null) {
                            for (PatternFormation.DetectedPattern pattern : patterns) {
                                String matched = conceptSpace.findClosest(pattern.vector);
                                if (matched != null) {
                                    result.emergentConcepts.add(matched);
                                    // Cargar concepto emergente en WorkingMemory
                                    workingMemory.load(matched,
                                            conceptSpace.getOrCreate(matched),
                                            pattern.energy * 0.6f,
                                            WorkingMemory.SlotSource.PATTERN);
                                }
                            }
                        }
                    }
                }

                // Registrar coactivaciones en ConceptSpace
                if (conceptSpace != null && activeConcepts.size() >= 2) {
                    for (int i = 0; i < activeConcepts.size() - 1; i++) {
                        for (int j = i + 1; j < activeConcepts.size(); j++) {
                            float coactStrength = conscious.get(i).relevance
                                    * conscious.get(j).relevance;
                            conceptSpace.registerCoactivation(
                                    activeConcepts.get(i),
                                    activeConcepts.get(j),
                                    coactStrength);
                        }
                    }
                }

                // Registrar cadenas causales temporales
                if (reasoningEngine != null && activeConcepts.size() >= 2) {
                    for (int i = 0; i < activeConcepts.size() - 1; i++) {
                        reasoningEngine.registerCausalLink(
                                activeConcepts.get(i),
                                activeConcepts.get(i + 1),
                                0.3f, // Fuerza moderada para observaciones simples
                                ReasoningEngine.LinkType.TEMPORAL);
                    }
                }
            }

            // 5) Evaluar con ReasoningEngine
            if (reasoningEngine != null && activeConcepts.size() >= 2) {
                ReasoningEngine.Hypothesis hypothesis =
                        reasoningEngine.generateHypothesis(activeConcepts);
                if (hypothesis != null && hypothesis.confidence > 0.3f) {
                    result.type = ThoughtType.HYPOTHESIS;
                    result.hypothesis = hypothesis;

                    // Cargar conclusion como contenido consciente
                    if (conceptSpace != null) {
                        workingMemory.load(hypothesis.conclusion,
                                conceptSpace.getOrCreate(hypothesis.conclusion),
                                hypothesis.confidence * 0.5f,
                                WorkingMemory.SlotSource.REASONING);
                    }
                }
            }

            // 6) Decay de atencion en WorkingMemory
            workingMemory.attentionTick();

            // Si no se detecto nada especial, es un tick de procesamiento normal
            if (result.type == null) {
                result.type = ThoughtType.PROCESSING;
            }

            // Registrar evento
            recordEvent(result);

        } catch (Exception e) {
            Log.e(TAG, "ThoughtStream.think() error", e);
            result.type = ThoughtType.ERROR;
        }

        lastResult = result;
        return result;
    }

    /**
     * Ejecuta multiples ticks de pensamiento.
     * Util para periodos de background thinking.
     *
     * @param numTicks Numero de ticks a ejecutar
     * @return Resultado acumulado
     */
    public ThoughtResult thinkMultiple(int numTicks) {
        ThoughtResult accumulated = ThoughtResult.EMPTY;

        for (int i = 0; i < numTicks; i++) {
            ThoughtResult tick = think();
            // Mantener el resultado mas significativo
            if (tick.type != null && tick.type.ordinal() > accumulated.type.ordinal()) {
                accumulated = tick;
            }
        }

        return accumulated;
    }

    /**
     * Obtiene el modo actual.
     */
    public Mode getMode() {
        return currentMode;
    }

    /**
     * Cambia el modo de operacion.
     */
    public void setMode(Mode mode) {
        Log.d(TAG, "ThoughtStream modo: " + currentMode + " → " + mode);
        this.currentMode = mode;
    }

    /**
     * Obtiene los eventos recientes para inspeccion.
     */
    public List<ThoughtEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    /**
     * Obtiene el resultado del ultimo tick.
     */
    public ThoughtResult getLastResult() {
        return lastResult;
    }

    public long getTotalTicks() {
        return totalTicks;
    }

    /**
     * Genera un resumen textual del estado del flujo de pensamiento.
     */
    public String summarize() {
        StringBuilder sb = new StringBuilder();
        sb.append("ThoughtStream[modo=").append(currentMode)
          .append(" ticks=").append(totalTicks);

        if (lastResult != null && lastResult.type != null) {
            sb.append(" ultimo=").append(lastResult.type);
            if (lastResult.activeConcepts != null && !lastResult.activeConcepts.isEmpty()) {
                sb.append(" conceptos=").append(lastResult.activeConcepts);
            }
            if (lastResult.hypothesis != null) {
                sb.append(" hipotesis=").append(lastResult.hypothesis.conclusion);
            }
        }

        sb.append("]");
        return sb.toString();
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private void recordEvent(ThoughtResult result) {
        ThoughtEvent event = new ThoughtEvent();
        event.tick = totalTicks;
        event.type = result.type;
        event.summary = buildEventSummary(result);
        event.mode = currentMode;

        recentEvents.add(event);
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(0);
        }
    }

    private String buildEventSummary(ThoughtResult result) {
        if (result.type == ThoughtType.HYPOTHESIS && result.hypothesis != null) {
            return "Hipotesis: " + result.hypothesis.reasoning;
        }
        if (result.type == ThoughtType.PATTERN_DETECTED) {
            return "Patrones detectados: " + result.patternsDetected
                    + " conceptos: " + result.emergentConcepts;
        }
        if (result.type == ThoughtType.SILENCE) {
            return "Silencio mental";
        }
        return "Procesamiento: " + result.activeSlotCount + " slots activos";
    }

    // ── Clases internas ─────────────────────────────────────────────────

    public enum ThoughtType {
        SILENCE,
        PROCESSING,
        PATTERN_DETECTED,
        HYPOTHESIS,
        ERROR
    }

    public static class ThoughtResult {
        public static final ThoughtResult EMPTY = new ThoughtResult();

        static {
            EMPTY.type = ThoughtType.SILENCE;
            EMPTY.activeSlotCount = 0;
            EMPTY.patternsDetected = 0;
            EMPTY.neuralEnergy = 0f;
            EMPTY.activeConcepts = new ArrayList<>();
            EMPTY.emergentConcepts = new ArrayList<>();
        }

        public ThoughtType type;
        public int activeSlotCount;
        public int patternsDetected;
        public float neuralEnergy;
        public List<String> activeConcepts = new ArrayList<>();
        public List<String> emergentConcepts = new ArrayList<>();
        public ReasoningEngine.Hypothesis hypothesis;
    }

    public static class ThoughtEvent {
        public long tick;
        public ThoughtType type;
        public String summary;
        public Mode mode;
    }
}
