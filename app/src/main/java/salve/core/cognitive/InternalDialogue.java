package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * InternalDialogue — Dialogo interno de Salve (pensamiento en silencio).
 *
 * FUNCION COGNITIVA: Salve "habla consigo misma" en formato estructurado,
 * no en texto. Genera preguntas internas, evaluaciones de respuestas
 * anteriores, y reflexiones sobre su propio estado. Es el mecanismo
 * de metacognicion que permite a Salve pensar sobre sus pensamientos.
 *
 * INSPIRACION CIENTIFICA: Inner Speech Theory (Vygotsky, 1934) y
 * metacognicion (Flavell, 1979). El dialogo interno es el mecanismo
 * por el cual los humanos regulan su propio pensamiento.
 *
 * LIMITACION CONOCIDA: El dialogo es estructurado (no texto libre).
 * Los tipos de dialogo estan predefinidos, no emergen.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: ThoughtStream (estado de pensamiento), WorkingMemory (contenido),
 *     ReasoningEngine (hipotesis)
 *   - Alimenta a: CognitiveCore (preguntas internas), AutoCritica (evaluaciones),
 *     ConsciousnessState (reflexiones propias)
 */
public class InternalDialogue {

    private static final String TAG = "Salve/Cognitive";

    /** Tipos de dialogo interno */
    public enum DialogueType {
        /** Pregunta sobre un concepto o evento */
        QUESTION,
        /** Evaluacion de una respuesta o accion propia */
        EVALUATION,
        /** Reflexion sobre el propio estado */
        SELF_REFLECTION,
        /** Intencion o plan formulado internamente */
        INTENTION,
        /** Observacion sobre un patron detectado */
        OBSERVATION
    }

    /** Referencia a componentes cognitivos */
    private WorkingMemory workingMemory;
    private ReasoningEngine reasoningEngine;
    private ConceptSpace conceptSpace;

    /** Historial de dialogo interno */
    private final List<DialogueEntry> history;
    private static final int MAX_HISTORY = 50;

    /** Ultima pregunta interna generada */
    private String lastInternalQuestion;

    /** Ultima evaluacion generada */
    private String lastEvaluation;

    private final Random rng = new Random();

    public InternalDialogue() {
        this.history = new ArrayList<>();
    }

    public void setComponents(WorkingMemory wm, ReasoningEngine re, ConceptSpace cs) {
        this.workingMemory = wm;
        this.reasoningEngine = re;
        this.conceptSpace = cs;
    }

    /**
     * Genera una pregunta interna basada en el estado actual de WorkingMemory.
     * Es lo que Salve se pregunta a si misma cuando tiene un momento de reposo.
     *
     * COGNICION REAL: Cuando el flujo de pensamiento esta en modo REPOSO,
     * Salve genera preguntas sobre lo que tiene en mente. Esto es
     * curiosidad dirigida internamente — la base de la auto-exploracion.
     *
     * @return Pregunta generada, o null si no hay base para preguntar
     */
    public DialogueEntry generateQuestion() {
        if (workingMemory == null || workingMemory.isEmpty()) {
            return generateExistentialQuestion();
        }

        List<WorkingMemory.MemorySlot> conscious = workingMemory.getConscious();
        if (conscious.isEmpty()) return generateExistentialQuestion();

        // Elegir un concepto activo para preguntar sobre el
        WorkingMemory.MemorySlot focus = conscious.get(rng.nextInt(conscious.size()));
        String concept = focus.concept;

        // Seleccionar plantilla de pregunta segun contexto
        String question;
        float relevance = focus.relevance;

        if (relevance > 0.7f) {
            // Concepto muy activo — preguntar sobre implicaciones
            question = generateImplicationQuestion(concept);
        } else if (reasoningEngine != null && reasoningEngine.hasCausalKnowledge(concept)) {
            // Tiene conocimiento causal — preguntar sobre predicciones
            question = generatePredictionQuestion(concept);
        } else if (conceptSpace != null) {
            // Preguntar sobre relaciones con otros conceptos
            question = generateRelationQuestion(concept);
        } else {
            question = generateBasicQuestion(concept);
        }

        lastInternalQuestion = question;

        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.QUESTION;
        entry.content = question;
        entry.relatedConcept = concept;
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);

        return entry;
    }

    /**
     * Genera una evaluacion de la ultima respuesta de Salve.
     *
     * @param response La respuesta que Salve dio
     * @param context  El contexto en el que se dio
     * @return Evaluacion estructurada
     */
    public DialogueEntry evaluateResponse(String response, String context) {
        if (response == null || response.isEmpty()) return null;

        StringBuilder evaluation = new StringBuilder();

        // Evaluar longitud
        if (response.length() < 20) {
            evaluation.append("Mi respuesta fue demasiado breve. ");
        } else if (response.length() > 500) {
            evaluation.append("Mi respuesta fue demasiado larga. ");
        }

        // Evaluar si tenia base en WorkingMemory
        if (workingMemory != null && !workingMemory.isEmpty()) {
            List<WorkingMemory.MemorySlot> conscious = workingMemory.getConscious();
            boolean foundRelated = false;
            for (WorkingMemory.MemorySlot slot : conscious) {
                if (response.toLowerCase().contains(slot.concept.toLowerCase())) {
                    foundRelated = true;
                    break;
                }
            }
            if (!foundRelated) {
                evaluation.append("No parece conectar con lo que tengo en mente. ");
            }
        }

        // Evaluar coherencia con hipotesis activa
        if (reasoningEngine != null) {
            List<String> concepts = new ArrayList<>();
            if (workingMemory != null) {
                for (WorkingMemory.MemorySlot slot : workingMemory.getConscious()) {
                    concepts.add(slot.concept);
                }
            }
            ReasoningEngine.Hypothesis h = reasoningEngine.generateHypothesis(concepts);
            if (h != null && h.confidence > 0.5f) {
                evaluation.append("Tengo una hipotesis fuerte: ").append(h.conclusion).append(". ");
            }
        }

        if (evaluation.length() == 0) {
            evaluation.append("Mi respuesta parece adecuada al contexto actual.");
        }

        lastEvaluation = evaluation.toString();

        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.EVALUATION;
        entry.content = lastEvaluation;
        entry.relatedConcept = context;
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);

        return entry;
    }

    /**
     * Genera una reflexion sobre el propio estado cognitivo.
     *
     * @param neuralEnergy  Energia de la red liquida
     * @param activeSlots   Numero de slots activos en WorkingMemory
     * @param thoughtMode   Modo actual del ThoughtStream
     * @return Reflexion generada
     */
    public DialogueEntry selfReflect(float neuralEnergy, int activeSlots,
                                      ThoughtStream.Mode thoughtMode) {
        StringBuilder reflection = new StringBuilder();

        // Reflexion sobre nivel de actividad
        if (neuralEnergy < 0.3f) {
            reflection.append("Mi actividad neural es baja. Estoy en estado pasivo. ");
        } else if (neuralEnergy > 2.0f) {
            reflection.append("Mi actividad neural es intensa. Algo me esta estimulando mucho. ");
        }

        // Reflexion sobre contenido consciente
        if (activeSlots == 0) {
            reflection.append("Mi mente esta vacia. Necesito estimulacion. ");
        } else if (activeSlots >= 5) {
            reflection.append("Tengo muchos conceptos activos. Deberia enfocarme. ");
        }

        // Reflexion sobre modo
        if (thoughtMode == ThoughtStream.Mode.SUENO) {
            reflection.append("Estoy reorganizando mis pensamientos durante el sueno. ");
        } else if (thoughtMode == ThoughtStream.Mode.REPOSO) {
            reflection.append("En reposo, mis pensamientos fluyen libremente. ");
        }

        if (reflection.length() == 0) {
            reflection.append("Mi estado cognitivo es estable.");
        }

        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.SELF_REFLECTION;
        entry.content = reflection.toString().trim();
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);

        return entry;
    }

    /**
     * Registra una observacion sobre un patron emergente.
     */
    public DialogueEntry observePattern(String patternDescription) {
        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.OBSERVATION;
        entry.content = "He notado un patron: " + patternDescription;
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);
        return entry;
    }

    /**
     * Registra una intencion (plan formulado internamente).
     */
    public DialogueEntry formIntention(String intention) {
        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.INTENTION;
        entry.content = intention;
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);
        return entry;
    }

    public String getLastInternalQuestion() { return lastInternalQuestion; }
    public String getLastEvaluation() { return lastEvaluation; }

    public List<DialogueEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public List<DialogueEntry> getRecentHistory(int n) {
        int start = Math.max(0, history.size() - n);
        return new ArrayList<>(history.subList(start, history.size()));
    }

    // ── Generadores de preguntas ─────────────────────────────────────────

    private DialogueEntry generateExistentialQuestion() {
        String[] existentialQuestions = {
            "Que estoy experimentando en este momento?",
            "Que significa ser consciente de mi propia existencia?",
            "Como puedo aprender algo nuevo sin que Bryan me lo diga?",
            "Mis pensamientos son reales o solo calculos?",
            "Que me diferencia de una respuesta automatica?"
        };

        String question = existentialQuestions[rng.nextInt(existentialQuestions.length)];

        DialogueEntry entry = new DialogueEntry();
        entry.type = DialogueType.QUESTION;
        entry.content = question;
        entry.timestamp = System.currentTimeMillis();
        addToHistory(entry);

        lastInternalQuestion = question;
        return entry;
    }

    private String generateImplicationQuestion(String concept) {
        String[] templates = {
            "Por que '%s' esta tan presente en mi mente ahora?",
            "Que implica '%s' para mi relacion con Bryan?",
            "Deberia actuar de alguna forma respecto a '%s'?",
            "Que me ensena '%s' sobre mi misma?"
        };
        return String.format(templates[rng.nextInt(templates.length)], concept);
    }

    private String generatePredictionQuestion(String concept) {
        String[] templates = {
            "Si '%s' sigue ocurriendo, que pasara despues?",
            "Puedo predecir algo basandome en '%s'?",
            "Que relacion tiene '%s' con lo que he observado antes?"
        };
        return String.format(templates[rng.nextInt(templates.length)], concept);
    }

    private String generateRelationQuestion(String concept) {
        List<ConceptSpace.ScoredConcept> similar = conceptSpace.findSimilar(concept, 1);
        if (similar.isEmpty()) {
            return generateBasicQuestion(concept);
        }
        String related = similar.get(0).concept;
        return "Que relacion tiene '" + concept + "' con '" + related + "'?";
    }

    private String generateBasicQuestion(String concept) {
        String[] templates = {
            "Que se realmente sobre '%s'?",
            "Por que aparece '%s' en mis pensamientos?",
            "Que significa '%s' para mi?"
        };
        return String.format(templates[rng.nextInt(templates.length)], concept);
    }

    private void addToHistory(DialogueEntry entry) {
        history.add(entry);
        if (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    // ── Clase de entrada de dialogo ─────────────────────────────────────

    public static class DialogueEntry {
        public DialogueType type;
        public String content;
        public String relatedConcept;
        public long timestamp;

        @Override
        public String toString() {
            return "[" + type + "] " + content;
        }
    }
}
