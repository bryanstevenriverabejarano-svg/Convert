package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * WorkingMemory — Memoria de trabajo de Salve con atención selectiva.
 *
 * FUNCION COGNITIVA: Implementa el buffer de capacidad limitada (7+-2 slots)
 * donde residen los contenidos "conscientes" en un momento dado. Cada slot
 * compite por relevancia, decae con el tiempo, y los que persisten lo
 * suficiente se consolidan a memoria de largo plazo (MemoriaEmocional).
 *
 * INSPIRACION CIENTIFICA: Modelo de memoria de trabajo de Baddeley (2000),
 * combinado con la teoria del espacio de trabajo global (Baars, 1988).
 * La atencion selectiva sigue el principio de competencia biasiada
 * (Desimone & Duncan, 1995).
 *
 * LIMITACION CONOCIDA: No implementa los subcomponentes de Baddeley
 * (lazo fonologico, agenda visuoespacial). Los slots son homogeneos.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: CognitiveCore (percepcion), ConceptSpace (activaciones)
 *   - Alimenta a: LiquidNeuralLayer (entrada), ThoughtStream (contenido consciente),
 *     ReasoningEngine (premisas), Verbalizer (estado a expresar)
 */
public class WorkingMemory {

    private static final String TAG = "Salve/Cognitive";

    /** Capacidad maxima de la memoria de trabajo (7+-2, usamos 7) */
    private static final int MAX_SLOTS = 7;

    /** Tasa de decaimiento por tick de pensamiento (0.0 - 1.0) */
    private static final float DECAY_RATE = 0.02f;

    /** Umbral minimo de relevancia para permanecer en memoria de trabajo */
    private static final float MIN_RELEVANCE = 0.05f;

    /** Umbral de persistencia para consolidacion a largo plazo */
    private static final float CONSOLIDATION_THRESHOLD = 0.6f;

    /** Ticks minimos que un slot debe sobrevivir para consolidarse */
    private static final int MIN_TICKS_FOR_CONSOLIDATION = 15;

    private final List<MemorySlot> slots;
    private final List<MemorySlot> pendingConsolidation;
    private final Random random;
    private int tickCount;

    public WorkingMemory() {
        this.slots = new ArrayList<>(MAX_SLOTS);
        this.pendingConsolidation = new ArrayList<>();
        this.random = new Random();
        this.tickCount = 0;
    }

    /**
     * Carga un nuevo contenido en la memoria de trabajo.
     * Si la memoria esta llena, desplaza el slot menos relevante.
     *
     * @param concept   Etiqueta conceptual del contenido
     * @param embedding Vector de embedding del contenido (puede ser null)
     * @param relevance Relevancia inicial (0.0 - 1.0)
     * @param source    Origen del contenido (INPUT, MEMORY, REASONING, PATTERN)
     */
    public synchronized void load(String concept, float[] embedding,
                                  float relevance, SlotSource source) {
        if (concept == null || concept.isEmpty()) return;

        // Verificar si ya existe un slot con este concepto — reforzar en vez de duplicar
        for (MemorySlot slot : slots) {
            if (concept.equals(slot.concept)) {
                slot.relevance = Math.min(1.0f, slot.relevance + relevance * 0.5f);
                slot.activationCount++;
                slot.lastActivatedTick = tickCount;
                Log.d(TAG, "WorkingMemory: reforzado '" + concept
                        + "' → relevance=" + slot.relevance);
                return;
            }
        }

        // Crear nuevo slot
        MemorySlot newSlot = new MemorySlot();
        newSlot.concept = concept;
        newSlot.embedding = embedding;
        newSlot.relevance = Math.max(0.0f, Math.min(1.0f, relevance));
        newSlot.source = source;
        newSlot.createdTick = tickCount;
        newSlot.lastActivatedTick = tickCount;
        newSlot.activationCount = 1;

        if (slots.size() < MAX_SLOTS) {
            slots.add(newSlot);
        } else {
            // Desplazar el slot menos relevante
            int leastIdx = findLeastRelevantIndex();
            MemorySlot displaced = slots.get(leastIdx);

            // Si el slot desplazado sobrevivio mucho tiempo, marcarlo para consolidacion
            if (displaced.ticksSurvived() >= MIN_TICKS_FOR_CONSOLIDATION
                    && displaced.relevance >= CONSOLIDATION_THRESHOLD * 0.5f) {
                pendingConsolidation.add(displaced);
            }

            slots.set(leastIdx, newSlot);
            Log.d(TAG, "WorkingMemory: desplazado '" + displaced.concept
                    + "' por '" + concept + "'");
        }
    }

    /**
     * Ejecuta un tick de atencion: decae relevancia, elimina slots muertos,
     * y marca candidatos para consolidacion.
     *
     * COGNICION REAL: Este es el "latido" de la conciencia de trabajo.
     * Cada tick simula el paso del tiempo atencional. Los contenidos que
     * no se refrescan se desvanecen naturalmente.
     */
    public synchronized void attentionTick() {
        tickCount++;

        List<MemorySlot> toRemove = new ArrayList<>();

        for (MemorySlot slot : slots) {
            // Decaimiento temporal — mas lento para slots con alta activacion
            float decayModifier = 1.0f / (1.0f + slot.activationCount * 0.1f);
            slot.relevance -= DECAY_RATE * decayModifier;

            // Verificar si debe ser removido
            if (slot.relevance < MIN_RELEVANCE) {
                // Si sobrevivio lo suficiente, marcar para consolidacion
                if (slot.ticksSurvived() >= MIN_TICKS_FOR_CONSOLIDATION) {
                    pendingConsolidation.add(slot);
                }
                toRemove.add(slot);
            }
        }

        slots.removeAll(toRemove);

        if (!toRemove.isEmpty()) {
            Log.d(TAG, "WorkingMemory tick " + tickCount
                    + ": removidos " + toRemove.size()
                    + " slots, quedan " + slots.size());
        }
    }

    /**
     * Obtiene el contenido actual de la memoria de trabajo, ordenado por relevancia.
     * Esto ES el "pensamiento consciente" de Salve en este instante.
     */
    public synchronized List<MemorySlot> getConscious() {
        List<MemorySlot> sorted = new ArrayList<>(slots);
        Collections.sort(sorted, (a, b) -> Float.compare(b.relevance, a.relevance));
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Obtiene los vectores de embedding de todos los slots activos,
     * ponderados por relevancia. Sirve como entrada a LiquidNeuralLayer.
     *
     * @param vectorSize Dimension del vector resultante
     * @return Vector ponderado de atencion, o vector cero si no hay contenido
     */
    public synchronized float[] getAttentionWeightedInput(int vectorSize) {
        float[] result = new float[vectorSize];

        if (slots.isEmpty()) return result;

        float totalRelevance = 0f;
        for (MemorySlot slot : slots) {
            totalRelevance += slot.relevance;
        }

        if (totalRelevance < 1e-6f) return result;

        for (MemorySlot slot : slots) {
            if (slot.embedding != null) {
                float weight = slot.relevance / totalRelevance;
                int len = Math.min(slot.embedding.length, vectorSize);
                for (int i = 0; i < len; i++) {
                    result[i] += slot.embedding[i] * weight;
                }
            }
        }

        return result;
    }

    /**
     * Obtiene los slots que estan listos para consolidacion a largo plazo.
     * El llamador debe persistirlos en MemoriaEmocional y luego llamar clearConsolidated().
     */
    public synchronized List<MemorySlot> getPendingConsolidation() {
        return new ArrayList<>(pendingConsolidation);
    }

    /**
     * Limpia la lista de slots pendientes de consolidacion.
     */
    public synchronized void clearConsolidated() {
        pendingConsolidation.clear();
    }

    /**
     * Refuerza un slot existente por nombre de concepto.
     * Se usa cuando el razonamiento o patrones referencian un concepto ya activo.
     */
    public synchronized void reinforce(String concept, float boost) {
        for (MemorySlot slot : slots) {
            if (concept.equals(slot.concept)) {
                slot.relevance = Math.min(1.0f, slot.relevance + boost);
                slot.activationCount++;
                slot.lastActivatedTick = tickCount;
                return;
            }
        }
    }

    /**
     * Devuelve el numero de slots activos.
     */
    public synchronized int size() {
        return slots.size();
    }

    /**
     * Devuelve true si la memoria de trabajo esta vacia.
     */
    public synchronized boolean isEmpty() {
        return slots.isEmpty();
    }

    /**
     * Limpia toda la memoria de trabajo. Usado al cambiar de modo (ACTIVO → SUENO).
     */
    public synchronized void clear() {
        // Mover todo a consolidacion antes de limpiar
        for (MemorySlot slot : slots) {
            if (slot.ticksSurvived() >= MIN_TICKS_FOR_CONSOLIDATION / 2) {
                pendingConsolidation.add(slot);
            }
        }
        slots.clear();
        Log.d(TAG, "WorkingMemory limpiada. Pendientes consolidacion: "
                + pendingConsolidation.size());
    }

    /**
     * Obtiene el slot mas relevante actualmente.
     */
    public synchronized MemorySlot getMostRelevant() {
        if (slots.isEmpty()) return null;
        MemorySlot best = slots.get(0);
        for (int i = 1; i < slots.size(); i++) {
            if (slots.get(i).relevance > best.relevance) {
                best = slots.get(i);
            }
        }
        return best;
    }

    /**
     * Genera un resumen textual del contenido consciente actual.
     */
    public synchronized String summarize() {
        if (slots.isEmpty()) return "Mente en silencio.";

        StringBuilder sb = new StringBuilder();
        List<MemorySlot> sorted = new ArrayList<>(slots);
        Collections.sort(sorted, (a, b) -> Float.compare(b.relevance, a.relevance));

        for (int i = 0; i < sorted.size(); i++) {
            MemorySlot s = sorted.get(i);
            sb.append(s.concept)
              .append(" (")
              .append(String.format("%.0f%%", s.relevance * 100))
              .append(")");
            if (i < sorted.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public int getTickCount() {
        return tickCount;
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private int findLeastRelevantIndex() {
        int idx = 0;
        float min = Float.MAX_VALUE;
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).relevance < min) {
                min = slots.get(i).relevance;
                idx = i;
            }
        }
        return idx;
    }

    // ── Clases internas ───────────────────────────────────────────────────

    /**
     * Un slot de la memoria de trabajo.
     * Contiene un concepto, su embedding, y metadatos de atencion.
     */
    public static class MemorySlot {
        public String concept;
        public float[] embedding;
        public float relevance;
        public SlotSource source;
        public int createdTick;
        public int lastActivatedTick;
        public int activationCount;

        public int ticksSurvived() {
            return lastActivatedTick - createdTick;
        }

        @Override
        public String toString() {
            return concept + "[" + String.format("%.2f", relevance) + "]";
        }
    }

    /**
     * Origen de un contenido en memoria de trabajo.
     */
    public enum SlotSource {
        /** Entrada directa del usuario */
        INPUT,
        /** Recuperado de memoria de largo plazo */
        MEMORY,
        /** Generado por el motor de razonamiento */
        REASONING,
        /** Emergido de PatternFormation */
        PATTERN,
        /** Activacion interna del flujo de pensamiento */
        INTERNAL
    }
}
