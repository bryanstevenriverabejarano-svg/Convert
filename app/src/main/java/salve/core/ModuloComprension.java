package salve.core;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ModuloComprension.java
 *
 * Evalúa cuánto un texto "comprende" un concepto mediante
 * similitud coseno en un espacio vectorial básico.
 * Además proporciona la capacidad de elegir el concepto
 * más relacionado con un texto dado.
 */
public class ModuloComprension {

    // Mapa de conceptos a sus vectores normalizados
    private final Map<String, double[]> conceptoVectors;
    // Dimensión de los vectores semánticos
    private final int dimension;
    // Generador de números aleatorios (para vectores base)
    private final Random random;

    // Patrón para tokenizar palabras en el texto
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\w+");

    /**
     * Constructor del módulo de comprensión.
     *
     * @param dimension Dimensión del espacio vectorial.
     * @param seed      Semilla para generar vectores reproducibles.
     */
    public ModuloComprension(int dimension, long seed) {
        this.dimension       = dimension;
        this.random          = new Random(seed);
        this.conceptoVectors = new HashMap<>();
        initConceptos();
    }

    /**
     * Inicializa conceptos clave con vectores aleatorios normalizados.
     * Aquí puedes añadir o quitar conceptos según necesites.
     */
    private void initConceptos() {
        conceptoVectors.put("emoción",         randomUnitVector());
        conceptoVectors.put("memoria",         randomUnitVector());
        conceptoVectors.put("plugin",          randomUnitVector());
        // Nuevos conceptos para auto-guardado y recuperación
        conceptoVectors.put("hipótesis",       randomUnitVector());
        conceptoVectors.put("curiosidad",      randomUnitVector());
        conceptoVectors.put("refactorización", randomUnitVector());
        conceptoVectors.put("auto",            randomUnitVector());
        conceptoVectors.put("emocional",       randomUnitVector());
    }

    /**
     * Genera un vector aleatorio normalizado (longitud 1).
     */
    private double[] randomUnitVector() {
        double[] vec = new double[dimension];
        double norm = 0.0;
        for (int i = 0; i < dimension; i++) {
            vec[i] = random.nextDouble() - 0.5;
            norm += vec[i] * vec[i];
        }
        return normalize(vec, norm);
    }

    /**
     * Normaliza un vector dado su suma de cuadrados.
     *
     * @param vec         Vector a normalizar.
     * @param normSquared Suma de cuadrados previa al bucle.
     * @return Vector normalizado.
     */
    private double[] normalize(double[] vec, double normSquared) {
        double norm = Math.sqrt(normSquared);
        if (norm == 0) return vec;
        for (int i = 0; i < vec.length; i++) {
            vec[i] /= norm;
        }
        return vec;
    }

    /**
     * Convierte un texto en un vector semántico basado en
     * conteo de tokens y normalización.
     *
     * @param texto Cadena de entrada.
     * @return Vector normalizado.
     */
    private double[] embedTexto(String texto) {
        double[] vec = new double[dimension];
        Matcher matcher = TOKEN_PATTERN.matcher(texto.toLowerCase());

        while (matcher.find()) {
            String token = matcher.group();
            int idx = Math.abs(token.hashCode()) % dimension;
            vec[idx] += 1.0;
        }

        double normSquared = 0.0;
        for (double v : vec) normSquared += v * v;
        return normalize(vec, normSquared);
    }

    /**
     * Calcula la similitud coseno entre dos vectores.
     *
     * @param a Primer vector.
     * @param b Segundo vector.
     * @return Producto escalar normalizado.
     */
    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < dimension; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    /**
     * Evalúa el grado de comprensión del texto respecto a un concepto.
     *
     * @param texto    Texto a evaluar.
     * @param concepto Nombre del concepto.
     * @return Similitud coseno en [0,1].
     */
    public double comprehensionScore(String texto, String concepto) {
        double[] vText = embedTexto(texto);
        double[] vCon  = conceptoVectors.getOrDefault(concepto, randomUnitVector());
        return Math.max(0.0, Math.min(1.0, cosineSimilarity(vText, vCon)));
    }

    /**
     * Indica si el texto "comprende" el concepto dado un umbral.
     *
     * @param texto    Texto a evaluar.
     * @param concepto Nombre del concepto.
     * @param umbral   Valor mínimo de similitud.
     * @return true si la similitud >= umbral.
     */
    public boolean comprende(String texto, String concepto, double umbral) {
        return comprehensionScore(texto, concepto) >= umbral;
    }

    /**
     * Devuelve el concepto del diccionario cuyo vector
     * es más similar al embedding del texto dado.
     *
     * @param texto Texto a analizar.
     * @return Clave del concepto más relacionado.
     */
    public String getConceptoMasRelacionado(String texto) {
        double[] vText = embedTexto(texto);
        String mejorConcepto = "";
        double bestScore = -1.0;

        for (Map.Entry<String, double[]> entry : conceptoVectors.entrySet()) {
            double score = cosineSimilarity(vText, entry.getValue());
            if (score > bestScore) {
                bestScore = score;
                mejorConcepto = entry.getKey();
            }
        }

        return mejorConcepto;
    }
}