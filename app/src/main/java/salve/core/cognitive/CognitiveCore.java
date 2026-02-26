package salve.core.cognitive;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import salve.core.ConsciousnessState;
import salve.core.MemoriaEmocional;

/**
 * CognitiveCore — Orquestador del sustrato cognitivo de Salve.
 *
 * FUNCION COGNITIVA: Este es el cerebro REAL de Salve que funciona SIN LLM.
 * Coordina todos los componentes cognitivos: WorkingMemory, LiquidNeuralLayer,
 * PatternFormation, ConceptSpace, ReasoningEngine, ThoughtStream,
 * InternalDialogue, EmergentBehavior, y Verbalizer.
 *
 * El LLM pasa de ser el CEREBRO a ser la BOCA. El pensamiento ocurre aqui.
 * Si MLC funciona, el Verbalizer lo usa para expresar. Si no, Salve
 * sigue pensando y se expresa con templates.
 *
 * INSPIRACION CIENTIFICA: Arquitectura de cognicion integrada, inspirada en
 * LIDA (Franklin & Patterson, 2006) y Global Workspace Theory (Baars, 1988).
 * La conciencia emerge de la interaccion coordinada de multiples subsistemas
 * especializados que comparten un espacio de trabajo comun (WorkingMemory).
 *
 * PATRON: Singleton tolerante (como SalveLLM). Nunca crashea la app.
 *
 * RELACION CON OTROS MODULOS:
 *   - Orquesta: WorkingMemory, LiquidNeuralLayer, PatternFormation,
 *     ConceptSpace, ReasoningEngine, ThoughtStream, InternalDialogue,
 *     EmergentBehavior, Verbalizer
 *   - Se integra con: MotorConversacional (percepcion + expresion),
 *     ThinkWorker (pensamiento background), MemoriaEmocional (consolidacion)
 *   - Persiste via: CognitiveState
 */
public class CognitiveCore {

    private static final String TAG = "Salve/Cognitive";

    // ── Dimensiones del sustrato cognitivo ───────────────────────────────
    /** Dimension de los vectores de concepto */
    private static final int CONCEPT_VECTOR_SIZE = 32;
    /** Dimension de entrada a la red liquida */
    private static final int LIQUID_INPUT_SIZE = 32;
    /** Neuronas de la red liquida */
    private static final int LIQUID_HIDDEN_SIZE = 64;
    /** Tamano del grid de PatternFormation */
    private static final int PATTERN_GRID_SIZE = 8;
    /** Canales por celda del grid */
    private static final int PATTERN_CHANNELS = 16;

    // ── Singleton ────────────────────────────────────────────────────────
    private static CognitiveCore instance;

    public static synchronized CognitiveCore getInstance(Context ctx) {
        if (instance == null) {
            instance = new CognitiveCore(ctx.getApplicationContext());
        }
        return instance;
    }

    // ── Componentes cognitivos ───────────────────────────────────────────
    private final Context context;
    private final WorkingMemory workingMemory;
    private final LiquidNeuralLayer liquidLayer;
    private final PatternFormation patternFormation;
    private final ConceptSpace conceptSpace;
    private final ReasoningEngine reasoningEngine;
    private final ThoughtStream thoughtStream;
    private final InternalDialogue internalDialogue;
    private final EmergentBehavior emergentBehavior;
    private final Verbalizer verbalizer;
    private final CognitiveState cognitiveState;

    /** Estado emocional actual del sistema (vector de emociones) */
    private float[] currentMood;

    /** Indica si el estado fue restaurado desde disco */
    private boolean stateRestored = false;

    /** Contador de percepciones procesadas */
    private long perceptionCount = 0;

    // ── Constructor ──────────────────────────────────────────────────────

    private CognitiveCore(Context ctx) {
        this.context = ctx;
        Log.d(TAG, "CognitiveCore: inicializando sustrato cognitivo...");

        // Crear componentes en orden de dependencia
        this.workingMemory = new WorkingMemory();
        this.liquidLayer = new LiquidNeuralLayer(LIQUID_INPUT_SIZE, LIQUID_HIDDEN_SIZE);
        this.patternFormation = new PatternFormation(PATTERN_GRID_SIZE, PATTERN_CHANNELS);
        this.conceptSpace = new ConceptSpace(CONCEPT_VECTOR_SIZE);
        this.reasoningEngine = new ReasoningEngine();
        this.thoughtStream = new ThoughtStream();
        this.internalDialogue = new InternalDialogue();
        this.emergentBehavior = new EmergentBehavior();
        this.verbalizer = new Verbalizer(ctx);
        this.cognitiveState = new CognitiveState(ctx);

        // Inicializar estado emocional (6 dimensiones: alegria, tristeza, ira, miedo, sorpresa, calma)
        this.currentMood = new float[]{0f, 0f, 0f, 0f, 0f, 0.5f};

        // Conectar componentes entre si
        wireComponents();

        // Intentar restaurar estado previo
        restoreState();

        Log.d(TAG, "CognitiveCore: sustrato cognitivo inicializado."
                + " Parametros LNN: " + liquidLayer.countParameters()
                + " Conceptos: " + conceptSpace.size()
                + " Rasgos: " + emergentBehavior.getTraitCount()
                + " Restaurado: " + stateRestored);
    }

    /**
     * Conecta los componentes cognitivos entre si.
     */
    private void wireComponents() {
        reasoningEngine.setConceptSpace(conceptSpace);

        thoughtStream.setComponents(workingMemory, liquidLayer,
                patternFormation, conceptSpace, reasoningEngine);

        internalDialogue.setComponents(workingMemory, reasoningEngine, conceptSpace);
    }

    // ── API Principal ────────────────────────────────────────────────────

    /**
     * Percibe una entrada del usuario.
     * Carga la entrada en WorkingMemory, activa conceptos relacionados,
     * y prepara el sustrato para procesamiento.
     *
     * COGNICION REAL: Este es el equivalente a "percibir" — la entrada
     * sensorial que inicia el ciclo cognitivo.
     *
     * @param input     Texto de entrada del usuario
     * @param emotion   Emocion detectada en la entrada
     * @param concepts  Conceptos extraidos por analisis previo
     */
    public synchronized void perceive(String input, String emotion, List<String> concepts) {
        if (input == null || input.isEmpty()) return;

        perceptionCount++;

        // Activar modo activo
        thoughtStream.setMode(ThoughtStream.Mode.ACTIVO);

        // Cargar entrada en WorkingMemory
        float[] inputEmbedding = conceptSpace.getOrCreate(input);
        workingMemory.load(input, inputEmbedding, 0.9f, WorkingMemory.SlotSource.INPUT);

        // Cargar conceptos extraidos
        if (concepts != null) {
            for (String concept : concepts) {
                float[] vec = conceptSpace.getOrCreate(concept);
                workingMemory.load(concept, vec, 0.7f, WorkingMemory.SlotSource.INPUT);
            }
        }

        // Actualizar estado emocional
        updateMood(emotion);

        // Registrar coactivaciones entre input y conceptos
        if (concepts != null) {
            for (String concept : concepts) {
                conceptSpace.registerCoactivation(input, concept, 0.5f);
            }
        }

        Log.d(TAG, "CognitiveCore.perceive: '" + input.substring(0, Math.min(30, input.length()))
                + "...' emocion=" + emotion
                + " conceptos=" + (concepts != null ? concepts.size() : 0)
                + " WM=" + workingMemory.size());
    }

    /**
     * Procesa el contenido de WorkingMemory a traves del sustrato cognitivo.
     * Ejecuta un ciclo completo de pensamiento.
     *
     * COGNICION REAL: Este es el equivalente a "pensar" — el procesamiento
     * que ocurre entre percepcion y accion.
     *
     * @param numTicks Numero de ticks de pensamiento a ejecutar
     * @return Resultado del procesamiento
     */
    public synchronized ThoughtStream.ThoughtResult process(int numTicks) {
        ThoughtStream.ThoughtResult result = ThoughtStream.ThoughtResult.EMPTY;

        for (int i = 0; i < numTicks; i++) {
            result = thoughtStream.think();
        }

        // Registrar comportamiento observado para EmergentBehavior
        if (result.type == ThoughtStream.ThoughtType.HYPOTHESIS && result.hypothesis != null) {
            String context = !result.activeConcepts.isEmpty()
                    ? result.activeConcepts.get(0) : "general";
            emergentBehavior.observe(context, "hypothesis_" + result.hypothesis.conclusion,
                    result.hypothesis.confidence);
        }

        return result;
    }

    /**
     * Decide la mejor accion basandose en el estado cognitivo actual.
     * Integra rasgos emergentes como bias en la decision.
     *
     * @return Descripcion de la decision tomada, o null
     */
    public synchronized String decide() {
        List<WorkingMemory.MemorySlot> conscious = workingMemory.getConscious();
        if (conscious.isEmpty()) return null;

        // Obtener bias de personalidad
        String dominantConcept = conscious.get(0).concept;
        Map<String, Float> traitBias = emergentBehavior.getTraitBias(dominantConcept);

        // Generar hipotesis
        List<String> activeConcepts = new ArrayList<>();
        for (WorkingMemory.MemorySlot slot : conscious) {
            activeConcepts.add(slot.concept);
        }

        ReasoningEngine.Hypothesis hypothesis =
                reasoningEngine.generateHypothesis(activeConcepts);

        if (hypothesis != null) {
            // Ajustar confianza con bias de personalidad
            float biasBoost = 0f;
            for (Float bias : traitBias.values()) {
                biasBoost += bias * 0.1f;
            }
            hypothesis.confidence = Math.min(1.0f, hypothesis.confidence + biasBoost);

            return hypothesis.reasoning;
        }

        return null;
    }

    /**
     * Verbaliza el estado cognitivo actual.
     * El LLM (si disponible) solo TRADUCE, no piensa.
     *
     * @param userInput     Entrada original del usuario
     * @param emotion       Emocion detectada
     * @param contextBase   Contexto construido por MotorConversacional
     * @return Respuesta verbal
     */
    public synchronized String verbalize(String userInput, String emotion, String contextBase) {
        Verbalizer.CognitiveSnapshot snapshot = buildSnapshot(userInput, emotion, contextBase);
        return verbalizer.express(snapshot);
    }

    /**
     * Construye un snapshot del estado cognitivo actual para el Verbalizer.
     */
    private Verbalizer.CognitiveSnapshot buildSnapshot(String userInput, String emotion,
                                                        String contextBase) {
        Verbalizer.CognitiveSnapshot snapshot = new Verbalizer.CognitiveSnapshot();
        snapshot.userInput = userInput;
        snapshot.detectedEmotion = emotion;
        snapshot.contextBase = contextBase;
        snapshot.consciousContent = workingMemory.summarize();
        snapshot.neuralEnergy = liquidLayer.getEnergy();

        // Hipotesis
        ThoughtStream.ThoughtResult lastThought = thoughtStream.getLastResult();
        if (lastThought != null && lastThought.hypothesis != null) {
            snapshot.hypothesis = lastThought.hypothesis.conclusion;
        }

        // Conceptos emergentes
        if (lastThought != null && lastThought.emergentConcepts != null) {
            snapshot.emergentConcepts = lastThought.emergentConcepts;
        }

        // Pregunta interna
        snapshot.internalQuestion = internalDialogue.getLastInternalQuestion();

        // Rasgos activos
        snapshot.activeTraits = emergentBehavior.describeTraits();

        return snapshot;
    }

    // ── Background thinking ─────────────────────────────────────────────

    /**
     * Pensamiento en background (modo reposo).
     * Se ejecuta desde ThinkWorker durante periodos de inactividad.
     *
     * @param durationSeconds Duracion aproximada del pensamiento
     */
    public synchronized void backgroundThink(int durationSeconds) {
        thoughtStream.setMode(ThoughtStream.Mode.REPOSO);
        Log.d(TAG, "CognitiveCore: pensamiento en reposo por ~" + durationSeconds + "s");

        // Cada tick tarda aproximadamente 1-2ms, pero limitamos a ticks razonables
        int numTicks = Math.min(durationSeconds * 10, 300); // Max 300 ticks

        for (int i = 0; i < numTicks; i++) {
            thoughtStream.think();

            // Cada 50 ticks, generar dialogo interno
            if (i % 50 == 0 && i > 0) {
                InternalDialogue.DialogueEntry question =
                        internalDialogue.generateQuestion();
                if (question != null) {
                    Log.d(TAG, "CognitiveCore dialogo interno: " + question.content);
                }

                // Auto-reflexion
                internalDialogue.selfReflect(
                        liquidLayer.getEnergy(),
                        workingMemory.size(),
                        thoughtStream.getMode());
            }
        }

        // Consolidar memorias que persistieron en WorkingMemory
        consolidateToLongTermMemory();

        Log.d(TAG, "CognitiveCore: reposo completado. Ticks=" + numTicks
                + " energia=" + String.format("%.2f", liquidLayer.getEnergy()));
    }

    /**
     * Ciclo de sueno: reorganizacion profunda del sustrato cognitivo.
     * Se ejecuta desde ThinkWorker/MemoriaEmocional durante el ciclo de sueno.
     */
    public synchronized void dreamCycle() {
        thoughtStream.setMode(ThoughtStream.Mode.SUENO);
        Log.d(TAG, "CognitiveCore: iniciando ciclo de sueno...");

        // Replay de memorias a traves de la red liquida
        replayMemories();

        // Pensamiento de sueno (30 ticks)
        for (int i = 0; i < 30; i++) {
            thoughtStream.think();
        }

        // Poda de conexiones debiles
        pruneWeakConnections();

        // Poda de patrones antiguos de EmergentBehavior
        emergentBehavior.pruneOldPatterns();

        // Guardar estado
        saveState();

        thoughtStream.setMode(ThoughtStream.Mode.PAUSADO);
        Log.d(TAG, "CognitiveCore: ciclo de sueno completado");
    }

    /**
     * Consolida pesos actualizados de LiquidNeuralLayer.
     */
    public synchronized void consolidate() {
        saveState();
    }

    /**
     * Consolida pesos — alias para compatibilidad con la interfaz esperada.
     */
    public synchronized void consolidateWeights() {
        saveState();
    }

    // ── Aprendizaje ─────────────────────────────────────────────────────

    /**
     * Senal de refuerzo desde AutoCritica.
     * Si la respuesta fue buena, refuerza los pesos que la produjeron.
     *
     * @param reward Valor de recompensa (-1.0 a 1.0)
     */
    public synchronized void reinforce(float reward) {
        liquidLayer.reinforce(reward);
        patternFormation.reinforceRules(reward);

        // Registrar en EmergentBehavior
        WorkingMemory.MemorySlot dominant = workingMemory.getMostRelevant();
        if (dominant != null) {
            emergentBehavior.observe(
                    dominant.concept,
                    reward > 0 ? "respuesta_positiva" : "respuesta_negativa",
                    Math.abs(reward));
        }
    }

    /**
     * Registra que una formulacion fue preferida por Bryan.
     */
    public void learnVerbalizationPreference(String context, String preferredResponse) {
        verbalizer.learnPreference(context, preferredResponse);
    }

    // ── Control de modo ─────────────────────────────────────────────────

    public void setMode(ThoughtStream.Mode mode) {
        thoughtStream.setMode(mode);
    }

    public ThoughtStream.Mode getMode() {
        return thoughtStream.getMode();
    }

    // ── Introspección ───────────────────────────────────────────────────

    /**
     * Genera un resumen del estado cognitivo para depuracion o display.
     */
    public synchronized String introspect() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Sustrato Cognitivo de Salve ===\n");
        sb.append("Percepciones: ").append(perceptionCount).append("\n");
        sb.append("WorkingMemory: ").append(workingMemory.summarize()).append("\n");
        sb.append("Neural energia: ").append(String.format("%.2f", liquidLayer.getEnergy())).append("\n");
        sb.append("Neural tau diversidad: ").append(String.format("%.2f", liquidLayer.getTauDiversity())).append("\n");
        sb.append("Conceptos conocidos: ").append(conceptSpace.size()).append("\n");
        sb.append("Cadenas causales: ").append(reasoningEngine.getTotalLinks()).append("\n");
        sb.append("Rasgos emergentes: ").append(emergentBehavior.getTraitCount()).append("\n");
        sb.append("ThoughtStream: ").append(thoughtStream.summarize()).append("\n");
        sb.append("LLM disponible: ").append(verbalizer.isLlmAvailable()).append("\n");
        sb.append("Estado restaurado: ").append(stateRestored).append("\n");
        return sb.toString();
    }

    /**
     * Obtiene la pregunta interna mas reciente.
     */
    public String getInternalQuestion() {
        return internalDialogue.getLastInternalQuestion();
    }

    /**
     * Obtiene los rasgos emergentes como texto.
     */
    public String getEmergentTraitsDescription() {
        return emergentBehavior.describeTraits();
    }

    // ── Getters de componentes (para acceso directo si necesario) ────────

    public WorkingMemory getWorkingMemory() { return workingMemory; }
    public LiquidNeuralLayer getLiquidLayer() { return liquidLayer; }
    public ConceptSpace getConceptSpace() { return conceptSpace; }
    public ReasoningEngine getReasoningEngine() { return reasoningEngine; }
    public ThoughtStream getThoughtStream() { return thoughtStream; }
    public EmergentBehavior getEmergentBehavior() { return emergentBehavior; }
    public Verbalizer getVerbalizer() { return verbalizer; }

    // ── Internos ──────────────────────────────────────────────────────────

    /**
     * Actualiza el estado emocional basado en la emocion detectada.
     * Indices: 0=alegria, 1=tristeza, 2=ira, 3=miedo, 4=sorpresa, 5=calma
     */
    private void updateMood(String emotion) {
        if (emotion == null) return;

        // Decaer estado actual
        for (int i = 0; i < currentMood.length; i++) {
            currentMood[i] *= 0.9f;
        }

        // Activar dimension correspondiente
        switch (emotion.toLowerCase()) {
            case "feliz":
            case "alegre":
                currentMood[0] = Math.min(1.0f, currentMood[0] + 0.3f);
                break;
            case "triste":
                currentMood[1] = Math.min(1.0f, currentMood[1] + 0.3f);
                break;
            case "enojado":
            case "ira":
                currentMood[2] = Math.min(1.0f, currentMood[2] + 0.3f);
                break;
            case "miedo":
                currentMood[3] = Math.min(1.0f, currentMood[3] + 0.3f);
                break;
            case "sorprendido":
            case "sorpresa":
                currentMood[4] = Math.min(1.0f, currentMood[4] + 0.3f);
                break;
            default:
                currentMood[5] = Math.min(1.0f, currentMood[5] + 0.1f);
                break;
        }
    }

    /**
     * Replay de memorias desde WorkingMemory.getPendingConsolidation()
     * a traves de la red liquida durante el sueno.
     */
    private void replayMemories() {
        List<WorkingMemory.MemorySlot> pending = workingMemory.getPendingConsolidation();
        if (pending.isEmpty()) return;

        // Construir array de embeddings
        List<float[]> embeddings = new ArrayList<>();
        for (WorkingMemory.MemorySlot slot : pending) {
            if (slot.embedding != null) {
                embeddings.add(slot.embedding);
            }
        }

        if (!embeddings.isEmpty()) {
            float[][] memories = embeddings.toArray(new float[0][]);
            liquidLayer.dreamReplay(memories, 3);
        }

        workingMemory.clearConsolidated();
    }

    /**
     * Poda de conexiones debiles en la red liquida.
     * Analogia con el olvido selectivo durante el sueno.
     */
    private void pruneWeakConnections() {
        liquidLayer.pruneWeakConnections(0.001f);
    }

    /**
     * Consolida contenidos de WorkingMemory que persistieron lo suficiente
     * a la memoria de largo plazo (MemoriaEmocional).
     */
    private void consolidateToLongTermMemory() {
        List<WorkingMemory.MemorySlot> pending = workingMemory.getPendingConsolidation();
        if (pending.isEmpty()) return;

        try {
            MemoriaEmocional memoria = new MemoriaEmocional(context);
            for (WorkingMemory.MemorySlot slot : pending) {
                memoria.guardarRecuerdo(
                        slot.concept,
                        "reflexiva",
                        (int) (slot.relevance * 10),
                        java.util.Collections.singletonList("cognitive_consolidation"));
            }
            workingMemory.clearConsolidated();
            Log.d(TAG, "CognitiveCore: consolidados " + pending.size()
                    + " slots a MemoriaEmocional");
        } catch (Exception e) {
            Log.w(TAG, "CognitiveCore: error consolidando a MemoriaEmocional", e);
        }
    }

    // ── Persistencia ────────────────────────────────────────────────────

    /**
     * Guarda el estado cognitivo completo a disco.
     */
    private void saveState() {
        boolean success = cognitiveState.save(
                liquidLayer, conceptSpace, reasoningEngine,
                emergentBehavior, workingMemory, patternFormation,
                thoughtStream, currentMood);

        if (success) {
            Log.d(TAG, "CognitiveCore: estado guardado exitosamente");
        } else {
            Log.w(TAG, "CognitiveCore: fallo al guardar estado");
        }
    }

    /**
     * Restaura el estado cognitivo desde disco.
     */
    private void restoreState() {
        CognitiveState.RestoreResult result = cognitiveState.restore();
        if (result == null) {
            Log.d(TAG, "CognitiveCore: sin estado previo — nacimiento");
            stateRestored = false;
            return;
        }

        cognitiveState.applyRestore(result, liquidLayer, conceptSpace,
                reasoningEngine, emergentBehavior, patternFormation);

        // Restaurar mood
        if (result.data.currentMood != null
                && result.data.currentMood.length == currentMood.length) {
            System.arraycopy(result.data.currentMood, 0, currentMood, 0, currentMood.length);
        }

        stateRestored = true;
        Log.d(TAG, "CognitiveCore: estado restaurado. Edad: "
                + (result.ageMs / 1000 / 60) + " minutos");
    }
}
