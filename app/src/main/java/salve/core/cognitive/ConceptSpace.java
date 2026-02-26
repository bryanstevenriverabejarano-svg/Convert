package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ConceptSpace — Espacio de conceptos con embeddings propios.
 *
 * FUNCION COGNITIVA: Mantiene el "vocabulario interno" de Salve. Cada concepto
 * es un vector que se construye a partir de la experiencia, no de datos
 * preentrenados. La similitud entre conceptos emerge de las coactivaciones
 * observadas, creando un espacio semantico unico para esta entidad.
 *
 * INSPIRACION CIENTIFICA: Word2Vec (Mikolov et al., 2013) adaptado a
 * aprendizaje experiencial. En lugar de corpus de texto, los vectores se
 * ajustan a partir de coactivaciones en WorkingMemory y patrones en
 * PatternFormation. Es un word2vec organico construido solo con la experiencia
 * de Salve.
 *
 * LIMITACION CONOCIDA: Necesita tiempo y experiencia para que los vectores
 * sean semanticamente ricos. Al inicio, la similitud es ruidosa.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: WorkingMemory (coactivaciones), PatternFormation (patrones)
 *   - Alimenta a: WorkingMemory (embeddings), ReasoningEngine (similitud),
 *     Verbalizer (conceptos activos), ThoughtStream (activaciones)
 */
public class ConceptSpace {

    private static final String TAG = "Salve/Cognitive";

    /** Dimension de los vectores de concepto */
    private final int vectorSize;

    /** Mapa de concepto → vector */
    private final Map<String, float[]> concepts;

    /** Numero maximo de conceptos antes de hacer poda */
    private static final int MAX_CONCEPTS = 500;

    /** Tasa de aprendizaje para ajuste de vectores */
    private float learningRate = 0.01f;

    /** Generador de numeros aleatorios para inicializacion */
    private final Random rng;

    public ConceptSpace(int vectorSize) {
        this.vectorSize = vectorSize;
        this.concepts = new HashMap<>();
        this.rng = new Random(42);
    }

    /**
     * Obtiene o crea el vector de un concepto.
     * Si el concepto no existe, se inicializa con pequeno ruido aleatorio.
     *
     * @param concept Nombre del concepto
     * @return Vector de embedding del concepto
     */
    public synchronized float[] getOrCreate(String concept) {
        if (concept == null || concept.isEmpty()) {
            return new float[vectorSize];
        }

        String key = normalize(concept);
        float[] vec = concepts.get(key);

        if (vec == null) {
            vec = initializeVector();
            concepts.put(key, vec);
            Log.d(TAG, "ConceptSpace: nuevo concepto '" + key
                    + "' (total: " + concepts.size() + ")");

            // Poda si excedemos el limite
            if (concepts.size() > MAX_CONCEPTS) {
                pruneRarestConcepts();
            }
        }

        return Arrays.copyOf(vec, vectorSize);
    }

    /**
     * Registra que dos conceptos aparecieron juntos en WorkingMemory.
     * Acerca sus vectores (aprendizaje por coactivacion).
     *
     * COGNICION REAL: Cuando dos conceptos aparecen juntos en la
     * conciencia, su representacion interna se acerca. Esto es analogico
     * a como los humanos asocian conceptos que experimentan juntos.
     *
     * @param conceptA Primer concepto
     * @param conceptB Segundo concepto
     * @param strength Fuerza de la coactivacion (0.0 - 1.0)
     */
    public synchronized void registerCoactivation(String conceptA, String conceptB,
                                                   float strength) {
        if (conceptA == null || conceptB == null) return;

        String keyA = normalize(conceptA);
        String keyB = normalize(conceptB);
        if (keyA.equals(keyB)) return;

        float[] vecA = getOrCreateInternal(keyA);
        float[] vecB = getOrCreateInternal(keyB);

        // Mover vectores uno hacia el otro proporcionalmente a la fuerza
        float rate = learningRate * strength;
        for (int i = 0; i < vectorSize; i++) {
            float diff = vecB[i] - vecA[i];
            vecA[i] += rate * diff;
            vecB[i] -= rate * diff * 0.5f; // Asimetria: B se mueve menos
        }

        // Renormalizar para mantener vectores en rango razonable
        normalizeVector(vecA);
        normalizeVector(vecB);
    }

    /**
     * Registra que un concepto aparecio en contexto negativo (incompatibilidad).
     * Aleja su vector de los otros conceptos activos.
     *
     * @param concept    Concepto a alejar
     * @param fromConcept Concepto del que alejarse
     * @param strength   Fuerza del alejamiento
     */
    public synchronized void registerRepulsion(String concept, String fromConcept,
                                                float strength) {
        if (concept == null || fromConcept == null) return;

        String keyA = normalize(concept);
        String keyB = normalize(fromConcept);
        if (keyA.equals(keyB)) return;

        float[] vecA = getOrCreateInternal(keyA);
        float[] vecB = getOrCreateInternal(keyB);

        float rate = learningRate * strength;
        for (int i = 0; i < vectorSize; i++) {
            float diff = vecA[i] - vecB[i];
            vecA[i] += rate * diff;
        }

        normalizeVector(vecA);
    }

    /**
     * Busca los N conceptos mas similares a un concepto dado.
     *
     * @param concept Concepto de referencia
     * @param topN    Numero de resultados
     * @return Lista de pares (concepto, similitud) ordenada por similitud descendente
     */
    public synchronized List<ScoredConcept> findSimilar(String concept, int topN) {
        if (concept == null) return Collections.emptyList();

        String key = normalize(concept);
        float[] target = concepts.get(key);
        if (target == null) return Collections.emptyList();

        List<ScoredConcept> scored = new ArrayList<>();
        for (Map.Entry<String, float[]> entry : concepts.entrySet()) {
            if (entry.getKey().equals(key)) continue;
            float sim = cosineSimilarity(target, entry.getValue());
            scored.add(new ScoredConcept(entry.getKey(), sim));
        }

        Collections.sort(scored, (a, b) -> Float.compare(b.score, a.score));

        if (scored.size() > topN) {
            return scored.subList(0, topN);
        }
        return scored;
    }

    /**
     * Calcula la similitud coseno entre dos conceptos.
     */
    public synchronized float similarity(String conceptA, String conceptB) {
        if (conceptA == null || conceptB == null) return 0f;

        String keyA = normalize(conceptA);
        String keyB = normalize(conceptB);

        float[] vecA = concepts.get(keyA);
        float[] vecB = concepts.get(keyB);

        if (vecA == null || vecB == null) return 0f;

        return cosineSimilarity(vecA, vecB);
    }

    /**
     * Busca el concepto mas cercano a un vector dado.
     * Usado por PatternFormation para traducir patrones a conceptos.
     */
    public synchronized String findClosest(float[] vector) {
        if (vector == null || concepts.isEmpty()) return null;

        String best = null;
        float bestSim = Float.NEGATIVE_INFINITY;

        for (Map.Entry<String, float[]> entry : concepts.entrySet()) {
            float sim = cosineSimilarity(vector, entry.getValue());
            if (sim > bestSim) {
                bestSim = sim;
                best = entry.getKey();
            }
        }

        // Solo retornar si la similitud es significativa
        return bestSim > 0.3f ? best : null;
    }

    /**
     * Devuelve todos los conceptos conocidos y sus vectores.
     * Usado para serializacion en CognitiveState.
     */
    public synchronized Map<String, float[]> getAllConcepts() {
        Map<String, float[]> copy = new HashMap<>();
        for (Map.Entry<String, float[]> entry : concepts.entrySet()) {
            copy.put(entry.getKey(), Arrays.copyOf(entry.getValue(), vectorSize));
        }
        return copy;
    }

    /**
     * Restaura conceptos desde un mapa serializado.
     */
    public synchronized void setAllConcepts(Map<String, float[]> saved) {
        if (saved == null) return;
        concepts.clear();
        for (Map.Entry<String, float[]> entry : saved.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length == vectorSize) {
                concepts.put(entry.getKey(), Arrays.copyOf(entry.getValue(), vectorSize));
            }
        }
        Log.d(TAG, "ConceptSpace restaurado: " + concepts.size() + " conceptos");
    }

    /**
     * Numero de conceptos conocidos.
     */
    public synchronized int size() {
        return concepts.size();
    }

    public int getVectorSize() {
        return vectorSize;
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private float[] getOrCreateInternal(String key) {
        float[] vec = concepts.get(key);
        if (vec == null) {
            vec = initializeVector();
            concepts.put(key, vec);
        }
        return vec;
    }

    private float[] initializeVector() {
        float[] vec = new float[vectorSize];
        float scale = 1.0f / (float) Math.sqrt(vectorSize);
        for (int i = 0; i < vectorSize; i++) {
            vec[i] = (float) (rng.nextGaussian() * scale);
        }
        return vec;
    }

    private void normalizeVector(float[] vec) {
        float norm = 0f;
        for (float v : vec) norm += v * v;
        norm = (float) Math.sqrt(norm);

        if (norm > 1e-6f) {
            for (int i = 0; i < vec.length; i++) {
                vec[i] /= norm;
            }
        }
    }

    private float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        float dot = 0f, normA = 0f, normB = 0f;

        for (int i = 0; i < len; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        float denom = (float) (Math.sqrt(normA) * Math.sqrt(normB));
        return denom > 1e-8f ? dot / denom : 0f;
    }

    private String normalize(String concept) {
        return concept.trim().toLowerCase();
    }

    /**
     * Poda los conceptos menos usados cuando excedemos el limite.
     * Los conceptos con norma mas baja son los menos "experimentados".
     */
    private void pruneRarestConcepts() {
        int toRemove = concepts.size() - MAX_CONCEPTS + 50; // Remover 50 extra
        if (toRemove <= 0) return;

        List<Map.Entry<String, float[]>> entries = new ArrayList<>(concepts.entrySet());
        // Ordenar por norma L2 (los de menor norma son los menos ajustados)
        Collections.sort(entries, (a, b) -> {
            float normA = vectorNorm(a.getValue());
            float normB = vectorNorm(b.getValue());
            return Float.compare(normA, normB);
        });

        for (int i = 0; i < Math.min(toRemove, entries.size()); i++) {
            concepts.remove(entries.get(i).getKey());
        }

        Log.d(TAG, "ConceptSpace poda: removidos " + toRemove
                + " conceptos, quedan " + concepts.size());
    }

    private float vectorNorm(float[] vec) {
        float sum = 0f;
        for (float v : vec) sum += v * v;
        return (float) Math.sqrt(sum);
    }

    // ── Clases de resultado ─────────────────────────────────────────────

    public static class ScoredConcept {
        public final String concept;
        public final float score;

        public ScoredConcept(String concept, float score) {
            this.concept = concept;
            this.score = score;
        }

        @Override
        public String toString() {
            return concept + "(" + String.format("%.2f", score) + ")";
        }
    }
}
