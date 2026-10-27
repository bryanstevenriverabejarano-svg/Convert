package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ReasoningEngine — Motor de razonamiento por cadenas causales.
 *
 * FUNCION COGNITIVA: Encadena conceptos causalmente (A → B → C),
 * soporta razonamiento por analogia ("A es a B como C es a ?"),
 * mantiene un grafo de relaciones causales aprendidas, y puede generar
 * hipotesis ("Si ocurre X, probablemente pase Y").
 *
 * INSPIRACION CIENTIFICA: Teoria de modelos mentales (Johnson-Laird, 1983)
 * combinada con razonamiento causal Bayesiano simplificado (Pearl, 2000).
 * Las cadenas causales son el mecanismo mas basico de prediccion humana.
 *
 * LIMITACION CONOCIDA: El razonamiento causal es correlacional, no causal
 * real. La fuerza de las relaciones se basa en coocurrencia observada,
 * no en intervencion experimental.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: WorkingMemory (premisas), ConceptSpace (similitud)
 *   - Alimenta a: DecisionEngine (planes enriquecidos), ThoughtStream (conclusiones),
 *     InternalDialogue (hipotesis a evaluar), Verbalizer (razonamientos a expresar)
 */
public class ReasoningEngine {

    private static final String TAG = "Salve/Cognitive";

    /** Grafo de relaciones causales: concepto → lista de (concepto, fuerza, tipo) */
    private final Map<String, List<CausalLink>> causalGraph;

    /** Referencia al espacio de conceptos para analogias */
    private ConceptSpace conceptSpace;

    /** Maximo de cadenas causales por concepto */
    private static final int MAX_LINKS_PER_CONCEPT = 20;

    /** Fuerza minima para considerar una relacion causal */
    private static final float MIN_CAUSAL_STRENGTH = 0.1f;

    public ReasoningEngine() {
        this.causalGraph = new HashMap<>();
    }

    public void setConceptSpace(ConceptSpace conceptSpace) {
        this.conceptSpace = conceptSpace;
    }

    // ── Aprendizaje causal ──────────────────────────────────────────────

    /**
     * Registra una relacion causal observada: cuando A ocurre, B suele seguir.
     *
     * COGNICION REAL: Esto se llama cada vez que Salve observa una secuencia
     * de conceptos en WorkingMemory. Si "tristeza" siempre precede a
     * "consuelo", se forma la cadena causal tristeza → consuelo.
     *
     * @param cause    Concepto causa
     * @param effect   Concepto efecto
     * @param strength Fuerza observada de la relacion (0.0 - 1.0)
     * @param type     Tipo de relacion (TEMPORAL, SEMANTIC, LOGICAL)
     */
    public synchronized void registerCausalLink(String cause, String effect,
                                                 float strength, LinkType type) {
        if (cause == null || effect == null) return;

        String key = normalize(cause);
        String effectKey = normalize(effect);

        List<CausalLink> links = causalGraph.computeIfAbsent(key, k -> new ArrayList<>());

        // Buscar link existente
        for (CausalLink link : links) {
            if (effectKey.equals(link.effect)) {
                // Actualizar con media movil exponencial
                link.strength = link.strength * 0.7f + strength * 0.3f;
                link.observationCount++;
                return;
            }
        }

        // Crear nuevo link
        if (links.size() < MAX_LINKS_PER_CONCEPT) {
            CausalLink newLink = new CausalLink();
            newLink.effect = effectKey;
            newLink.strength = strength;
            newLink.type = type;
            newLink.observationCount = 1;
            links.add(newLink);
        } else {
            // Reemplazar el mas debil si el nuevo es mas fuerte
            CausalLink weakest = null;
            for (CausalLink l : links) {
                if (weakest == null || l.strength < weakest.strength) {
                    weakest = l;
                }
            }
            if (weakest != null && weakest.strength < strength) {
                weakest.effect = effectKey;
                weakest.strength = strength;
                weakest.type = type;
                weakest.observationCount = 1;
            }
        }
    }

    // ── Razonamiento causal ─────────────────────────────────────────────

    /**
     * Predice que conceptos seguiran a un concepto dado.
     * Sigue las cadenas causales mas fuertes.
     *
     * @param cause    Concepto de partida
     * @param maxDepth Profundidad maxima de la cadena
     * @return Lista de predicciones con su probabilidad estimada
     */
    public synchronized List<Prediction> predict(String cause, int maxDepth) {
        if (cause == null) return Collections.emptyList();

        List<Prediction> raw = new ArrayList<>();
        predictRecursive(normalize(cause), maxDepth, 1.0f, raw, new ArrayList<>());

        // Un mismo efecto alcanzado por varios caminos cuenta una vez, con su camino más fuerte
        // (antes salía repetido y engordaba las hipótesis).
        Map<String, Prediction> best = new HashMap<>();
        for (Prediction p : raw) {
            Prediction known = best.get(p.concept);
            if (known == null || p.probability > known.probability) best.put(p.concept, p);
        }
        best.remove(normalize(cause));
        List<Prediction> predictions = new ArrayList<>(best.values());
        Collections.sort(predictions, (a, b) -> Float.compare(b.probability, a.probability));
        return predictions;
    }

    private void predictRecursive(String concept, int depth, float probability,
                                   List<Prediction> results, List<String> visited) {
        // chainLength = número de enlaces desde el origen (visited ya incluye este concepto abajo)
        if (depth <= 0 || probability < 0.05f) return;
        if (visited.contains(concept)) return; // Evitar ciclos

        visited.add(concept);

        List<CausalLink> links = causalGraph.get(concept);
        if (links == null) return;

        for (CausalLink link : links) {
            if (link.strength < MIN_CAUSAL_STRENGTH) continue;

            float effectProbability = probability * link.strength;

            Prediction pred = new Prediction();
            pred.concept = link.effect;
            pred.probability = effectProbability;
            pred.chainLength = visited.size();
            pred.linkType = link.type;
            results.add(pred);

            // Continuar la cadena
            predictRecursive(link.effect, depth - 1, effectProbability, results, visited);
        }

        visited.remove(visited.size() - 1);
    }

    /**
     * Razonamiento por analogia: "A es a B como C es a ?"
     * Usa el espacio de conceptos para encontrar la relacion.
     *
     * COGNICION REAL: La analogia es una de las formas mas poderosas de
     * razonamiento humano. Encuentra patrones estructurales entre
     * relaciones de conceptos.
     *
     * @param a Primer termino de la analogia
     * @param b Segundo termino (relacionado con a)
     * @param c Tercer termino (se busca su analogo)
     * @return Concepto D tal que A:B :: C:D, o null si no se encuentra
     */
    public synchronized String analogize(String a, String b, String c) {
        if (conceptSpace == null) {
            Log.w(TAG, "ReasoningEngine: analogia sin ConceptSpace");
            return null;
        }

        float[] vecA = conceptSpace.getOrCreate(a);
        float[] vecB = conceptSpace.getOrCreate(b);
        float[] vecC = conceptSpace.getOrCreate(c);

        // Vector de relacion: B - A
        int size = vecA.length;
        float[] relation = new float[size];
        for (int i = 0; i < size; i++) {
            relation[i] = vecB[i] - vecA[i];
        }

        // Aplicar relacion a C: D = C + (B - A)
        float[] targetVec = new float[size];
        for (int i = 0; i < size; i++) {
            targetVec[i] = vecC[i] + relation[i];
        }

        // Buscar el concepto mas cercano al vector objetivo
        return conceptSpace.findClosest(targetVec);
    }

    /**
     * Genera una hipotesis basada en el estado actual de WorkingMemory.
     * Examina los conceptos activos y predice que podria pasar.
     *
     * @param activeConcepts Conceptos actualmente activos en WorkingMemory
     * @return Hipotesis generada, o null si no hay base para hipotetizar
     */
    public synchronized Hypothesis generateHypothesis(List<String> activeConcepts) {
        if (activeConcepts == null || activeConcepts.isEmpty()) return null;

        // Buscar predicciones para cada concepto activo
        List<Prediction> allPredictions = new ArrayList<>();
        for (String concept : activeConcepts) {
            allPredictions.addAll(predict(concept, 2));
        }

        if (allPredictions.isEmpty()) return null;

        // Agrupar por efecto con OR ruidoso (Pearl, 1988): causas independientes que apuntan al mismo
        // efecto lo hacen más probable, pero nunca por encima de 1 (antes se sumaban sin más).
        Map<String, Float> notHappening = new HashMap<>();
        for (Prediction p : allPredictions) {
            float miss = notHappening.containsKey(p.concept) ? notHappening.get(p.concept) : 1f;
            notHappening.put(p.concept, miss * (1f - Math.min(1f, p.probability)));
        }
        Map<String, Float> aggregated = new HashMap<>();
        for (Map.Entry<String, Float> e : notHappening.entrySet()) aggregated.put(e.getKey(), 1f - e.getValue());
        for (String premise : activeConcepts) aggregated.remove(normalize(premise));

        // Encontrar la prediccion mas fuerte
        String bestConcept = null;
        float bestProb = 0f;
        for (Map.Entry<String, Float> entry : aggregated.entrySet()) {
            if (entry.getValue() > bestProb) {
                bestProb = entry.getValue();
                bestConcept = entry.getKey();
            }
        }

        if (bestConcept == null) return null;

        Hypothesis hypothesis = new Hypothesis();
        hypothesis.premises = new ArrayList<>(activeConcepts);
        hypothesis.conclusion = bestConcept;
        hypothesis.confidence = Math.min(1.0f, bestProb);
        hypothesis.reasoning = buildReasoningChain(activeConcepts, bestConcept);

        return hypothesis;
    }

    /**
     * Razonamiento hacia atrás (abducción): qué causas conocidas llevan a {@code effect}, directas o
     * en cadena hasta {@code maxDepth} pasos. Responde a «¿por qué X?».
     */
    public synchronized List<Prediction> explain(String effect, int maxDepth) {
        if (effect == null) return Collections.emptyList();
        String target = normalize(effect);
        Map<String, Prediction> best = new HashMap<>();
        java.util.ArrayDeque<String> frontier = new java.util.ArrayDeque<>();
        Map<String, Float> reach = new HashMap<>();
        reach.put(target, 1f);
        frontier.add(target);
        for (int depth = 1; depth <= maxDepth && !frontier.isEmpty(); depth++) {
            java.util.ArrayDeque<String> next = new java.util.ArrayDeque<>();
            while (!frontier.isEmpty()) {
                String current = frontier.poll();
                float p = reach.get(current);
                for (Map.Entry<String, List<CausalLink>> entry : causalGraph.entrySet()) {
                    for (CausalLink link : entry.getValue()) {
                        if (!current.equals(link.effect) || link.strength < MIN_CAUSAL_STRENGTH) continue;
                        String cause = entry.getKey();
                        float q = p * link.strength;
                        if (cause.equals(target) || q < 0.05f) continue;
                        Prediction known = best.get(cause);
                        if (known != null && known.probability >= q) continue;
                        Prediction pred = new Prediction();
                        pred.concept = cause;
                        pred.probability = q;
                        pred.chainLength = depth;
                        pred.linkType = link.type;
                        best.put(cause, pred);
                        reach.put(cause, q);
                        next.add(cause);
                    }
                }
            }
            frontier = next;
        }
        List<Prediction> out = new ArrayList<>(best.values());
        Collections.sort(out, (a, b) -> Float.compare(b.probability, a.probability));
        return out;
    }

    /** El enlace directo cause → effect, o null. */
    public synchronized CausalLink link(String cause, String effect) {
        List<CausalLink> links = causalGraph.get(normalize(cause));
        if (links == null) return null;
        for (CausalLink l : links) if (normalize(effect).equals(l.effect)) return l;
        return null;
    }

    /** Todos los conceptos conocidos (causas y efectos). */
    public synchronized java.util.Set<String> concepts() {
        java.util.Set<String> all = new java.util.LinkedHashSet<>(causalGraph.keySet());
        for (List<CausalLink> links : causalGraph.values()) for (CausalLink l : links) all.add(l.effect);
        return all;
    }

    /**
     * Evalua si un concepto tiene relaciones causales conocidas.
     */
    public synchronized boolean hasCausalKnowledge(String concept) {
        if (concept == null) return false;
        List<CausalLink> links = causalGraph.get(normalize(concept));
        return links != null && !links.isEmpty();
    }

    /**
     * Obtiene todas las cadenas causales para serializacion.
     */
    public synchronized Map<String, List<CausalLink>> getAllCausalLinks() {
        Map<String, List<CausalLink>> copy = new HashMap<>();
        for (Map.Entry<String, List<CausalLink>> entry : causalGraph.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Restaura cadenas causales desde datos serializados.
     */
    public synchronized void setAllCausalLinks(Map<String, List<CausalLink>> saved) {
        if (saved == null) return;
        causalGraph.clear();
        causalGraph.putAll(saved);
        Log.d(TAG, "ReasoningEngine restaurado: " + causalGraph.size() + " conceptos causales");
    }

    /**
     * Obtiene estadisticas del grafo causal.
     */
    public synchronized int getTotalLinks() {
        int total = 0;
        for (List<CausalLink> links : causalGraph.values()) {
            total += links.size();
        }
        return total;
    }

    // ── Internos ──────────────────────────────────────────────────────────

    private String normalize(String s) {
        return s.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String buildReasoningChain(List<String> premises, String conclusion) {
        StringBuilder sb = new StringBuilder();
        sb.append("Dado que observo: ");
        for (int i = 0; i < premises.size(); i++) {
            sb.append(premises.get(i));
            if (i < premises.size() - 1) sb.append(", ");
        }
        sb.append(" → predigo: ").append(conclusion);
        return sb.toString();
    }

    // ── Clases internas ─────────────────────────────────────────────────

    public static class CausalLink {
        public String effect;
        public float strength;
        public LinkType type;
        public int observationCount;

        @Override
        public String toString() {
            return "→" + effect + "(" + String.format("%.2f", strength)
                    + " x" + observationCount + ")";
        }
    }

    public enum LinkType {
        /** Relacion temporal: A ocurre antes que B */
        TEMPORAL,
        /** Relacion semantica: A esta relacionado con B */
        SEMANTIC,
        /** Relacion logica: A implica B */
        LOGICAL
    }

    public static class Prediction {
        public String concept;
        public float probability;
        public int chainLength;
        public LinkType linkType;

        @Override
        public String toString() {
            return concept + "(p=" + String.format("%.2f", probability)
                    + " chain=" + chainLength + ")";
        }
    }

    public static class Hypothesis {
        public List<String> premises;
        public String conclusion;
        public float confidence;
        public String reasoning;

        @Override
        public String toString() {
            return "Hypothesis: " + reasoning
                    + " (confidence=" + String.format("%.2f", confidence) + ")";
        }
    }
}
