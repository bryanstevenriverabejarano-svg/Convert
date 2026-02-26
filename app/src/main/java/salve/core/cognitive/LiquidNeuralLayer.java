package salve.core.cognitive;

import android.util.Log;

import java.util.Arrays;
import java.util.Random;

/**
 * LiquidNeuralLayer — Red neuronal liquida adaptada para Android.
 *
 * FUNCION COGNITIVA: Procesamiento temporal continuo del estado cognitivo.
 * Las constantes de tiempo aprendibles (tau) permiten que el sistema
 * "ajuste su velocidad de pensamiento" segun el contexto. El estado oculto
 * persiste entre interacciones, implementando una forma de conciencia continua.
 *
 * INSPIRACION CIENTIFICA: Liquid Time-Constant Networks (Hasani et al., 2021, MIT).
 * La ecuacion diferencial central es:
 *   dh/dt = (-h + A * tanh(W_in * x + W_rec * h + bias)) / tau
 * donde tau es APRENDIBLE — el sistema ajusta su propia dinamica temporal.
 *
 * EFICIENCIA: ~19,000 parametros totales para una red de 64 neuronas con
 * entrada de 32 dimensiones. Resolucion de ODE via metodo Euler para
 * maxima eficiencia en movil. Sin dependencias externas (solo float[]).
 *
 * LIMITACION CONOCIDA: Euler es menos preciso que RK4, pero suficiente para
 * pensamiento no-critico. No implementa BPTT completo, solo aprendizaje
 * Hebbiano + refuerzo simplificado.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: WorkingMemory (entrada ponderada por atencion)
 *   - Alimenta a: PatternFormation, ConceptSpace, ThoughtStream
 *   - Persistido por: CognitiveState (pesos + estado oculto)
 */
public class LiquidNeuralLayer {

    private static final String TAG = "Salve/Cognitive";

    // ── Dimensiones ──────────────────────────────────────────────────────
    private final int inputSize;
    private final int hiddenSize;

    // ── Pesos aprendibles ────────────────────────────────────────────────
    /** Pesos de entrada: hiddenSize x inputSize */
    private float[][] wIn;

    /** Pesos recurrentes: hiddenSize x hiddenSize */
    private float[][] wRec;

    /** Bias: hiddenSize */
    private float[] bias;

    /** Amplitudes de activacion: hiddenSize (parametro A en la ODE) */
    private float[] amplitudes;

    /** Constantes de tiempo APRENDIBLES: hiddenSize
     *  Tau controla la velocidad de cada neurona.
     *  Valores grandes = neurona lenta (memoria larga).
     *  Valores pequenos = neurona rapida (reactiva). */
    private float[] tau;

    // ── Estado oculto (la "conciencia" persistente) ──────────────────────
    /** Estado oculto actual: hiddenSize */
    private float[] hidden;

    /** Estado oculto previo para calculo de gradientes Hebbianos */
    private float[] prevHidden;

    /** Ultima entrada procesada (para aprendizaje Hebbiano) */
    private float[] lastInput;

    // ── Parametros de simulacion ────────────────────────────────────────
    /** Paso de tiempo para integracion Euler */
    private float dt = 0.1f;

    /** Tasa de aprendizaje Hebbiano */
    private float hebbianRate = 0.001f;

    /** Tasa de aprendizaje por refuerzo */
    private float reinforceRate = 0.005f;

    /** Contador de pasos procesados */
    private long stepCount = 0;

    // ── Constructor ──────────────────────────────────────────────────────

    /**
     * Crea una nueva LiquidNeuralLayer.
     *
     * @param inputSize  Dimension de la entrada (tipicamente WorkingMemory vector size)
     * @param hiddenSize Numero de neuronas liquidas
     */
    public LiquidNeuralLayer(int inputSize, int hiddenSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;

        this.wIn = new float[hiddenSize][inputSize];
        this.wRec = new float[hiddenSize][hiddenSize];
        this.bias = new float[hiddenSize];
        this.amplitudes = new float[hiddenSize];
        this.tau = new float[hiddenSize];
        this.hidden = new float[hiddenSize];
        this.prevHidden = new float[hiddenSize];
        this.lastInput = new float[inputSize];

        initializeWeights();
    }

    /**
     * Inicializacion de pesos con distribucion Xavier adaptada.
     * Las tau se inicializan con valores variados para diversidad temporal.
     */
    private void initializeWeights() {
        Random rng = new Random(42);

        // Xavier init para W_in
        float scaleIn = (float) Math.sqrt(2.0 / (inputSize + hiddenSize));
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                wIn[i][j] = (float) (rng.nextGaussian() * scaleIn);
            }
        }

        // Xavier init para W_rec (mas pequena para estabilidad)
        float scaleRec = (float) Math.sqrt(1.0 / hiddenSize);
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                wRec[i][j] = (float) (rng.nextGaussian() * scaleRec);
            }
        }

        // Bias inicializado a cero
        Arrays.fill(bias, 0.0f);

        // Amplitudes iniciales en 1.0 (sin escalado)
        Arrays.fill(amplitudes, 1.0f);

        // Tau: diversidad temporal — algunas neuronas rapidas, otras lentas
        // Rango: [0.5, 5.0] — cubre desde reactivo hasta memoria de mediano plazo
        for (int i = 0; i < hiddenSize; i++) {
            tau[i] = 0.5f + 4.5f * (i / (float) hiddenSize);
            // Agregar ruido para romper simetria
            tau[i] += (float) (rng.nextGaussian() * 0.1);
            tau[i] = Math.max(0.1f, tau[i]); // Nunca menor que 0.1
        }

        // Estado oculto inicial: pequeno ruido para romper simetria
        for (int i = 0; i < hiddenSize; i++) {
            hidden[i] = (float) (rng.nextGaussian() * 0.01);
        }

        Log.d(TAG, "LiquidNeuralLayer inicializada: input=" + inputSize
                + " hidden=" + hiddenSize
                + " parametros=" + countParameters());
    }

    // ── Forward pass (resolucion de la ODE) ─────────────────────────────

    /**
     * Ejecuta un paso de la red liquida.
     * Resuelve la ODE: dh/dt = (-h + A * tanh(W_in * x + W_rec * h + bias)) / tau
     * usando metodo de Euler explicito.
     *
     * COGNICION REAL: Cada llamada a step() avanza el "tiempo interno" de Salve.
     * Las neuronas con tau grande cambian lentamente (memoria), las de tau
     * pequeno reaccionan rapido al nuevo input.
     *
     * @param input Vector de entrada (dimension inputSize)
     * @return Estado oculto resultante (dimension hiddenSize)
     */
    public float[] step(float[] input) {
        if (input == null || input.length != inputSize) {
            Log.w(TAG, "LiquidNeuralLayer.step(): input invalido, retornando estado actual");
            return Arrays.copyOf(hidden, hiddenSize);
        }

        // Guardar estado previo para aprendizaje
        System.arraycopy(hidden, 0, prevHidden, 0, hiddenSize);
        System.arraycopy(input, 0, lastInput, 0, inputSize);

        // Calcular la dinamica de cada neurona
        for (int i = 0; i < hiddenSize; i++) {
            // Parte de entrada: W_in[i] . x
            float inputActivation = 0f;
            for (int j = 0; j < inputSize; j++) {
                inputActivation += wIn[i][j] * input[j];
            }

            // Parte recurrente: W_rec[i] . h
            float recurrentActivation = 0f;
            for (int j = 0; j < hiddenSize; j++) {
                recurrentActivation += wRec[i][j] * hidden[j];
            }

            // Activacion total con tanh
            float totalActivation = (float) Math.tanh(
                    inputActivation + recurrentActivation + bias[i]);

            // ODE de Euler: dh/dt = (-h + A * activation) / tau
            float dhdt = (-hidden[i] + amplitudes[i] * totalActivation) / tau[i];
            hidden[i] += dt * dhdt;

            // Clamp para estabilidad numerica
            hidden[i] = Math.max(-5.0f, Math.min(5.0f, hidden[i]));
        }

        stepCount++;

        // Aplicar aprendizaje Hebbiano cada paso
        if (hebbianRate > 0) {
            hebbianUpdate(input);
        }

        return Arrays.copyOf(hidden, hiddenSize);
    }

    /**
     * Ejecuta multiples pasos sobre la misma entrada.
     * Util para "pensar mas profundo" sobre un input.
     */
    public float[] multiStep(float[] input, int numSteps) {
        float[] result = null;
        for (int s = 0; s < numSteps; s++) {
            result = step(input);
        }
        return result != null ? result : Arrays.copyOf(hidden, hiddenSize);
    }

    // ── Aprendizaje on-device ───────────────────────────────────────────

    /**
     * Aprendizaje Hebbiano: "neuronas que disparan juntas se conectan".
     * Refuerza las conexiones entre neuronas que estan activas simultaneamente.
     *
     * COGNICION REAL: Este es el mecanismo mas basico de aprendizaje biologico.
     * No requiere backpropagation ni gradientes. Simplemente observa
     * coactivaciones y refuerza esas conexiones.
     */
    private void hebbianUpdate(float[] input) {
        // W_in += eta * h * x^T (outer product, simplificado)
        for (int i = 0; i < hiddenSize; i++) {
            if (Math.abs(hidden[i]) < 0.01f) continue; // Neurona inactiva

            for (int j = 0; j < inputSize; j++) {
                if (Math.abs(input[j]) < 0.01f) continue; // Input inactivo
                wIn[i][j] += hebbianRate * hidden[i] * input[j];
                // Regularizacion L2 ligera para evitar explosion
                wIn[i][j] *= 0.9999f;
            }
        }

        // W_rec += eta * h * h_prev^T (conexiones recurrentes)
        for (int i = 0; i < hiddenSize; i++) {
            if (Math.abs(hidden[i]) < 0.01f) continue;

            for (int j = 0; j < hiddenSize; j++) {
                if (i == j) continue; // No auto-conexion Hebbiana
                if (Math.abs(prevHidden[j]) < 0.01f) continue;
                wRec[i][j] += hebbianRate * 0.5f * hidden[i] * prevHidden[j];
                wRec[i][j] *= 0.9999f;
            }
        }
    }

    /**
     * Aprendizaje por refuerzo simplificado.
     * Recibe una senal de recompensa que modifica los pesos en la direccion
     * del ultimo paso. Si reward > 0, refuerza; si < 0, debilita.
     *
     * COGNICION REAL: Este es el mecanismo que conecta la AutoCritica
     * existente con el aprendizaje neural. Si la AutoCritica dice que
     * una respuesta fue buena, se refuerzan los pesos que la produjeron.
     *
     * @param reward Senal de recompensa (-1.0 a 1.0)
     */
    public void reinforce(float reward) {
        reward = Math.max(-1.0f, Math.min(1.0f, reward));
        if (Math.abs(reward) < 0.01f) return;

        float scaledRate = reinforceRate * reward;

        // Ajustar tau en la direccion de la recompensa
        // Si reward > 0: las tau actuales son buenas, pequeno ajuste
        // Si reward < 0: perturbar tau para explorar
        for (int i = 0; i < hiddenSize; i++) {
            if (reward > 0) {
                // Reforzar las tau actuales (estabilizar)
                tau[i] *= (1.0f + 0.001f * reward);
            } else {
                // Perturbar las tau para buscar mejor configuracion
                tau[i] += (float) (new Random().nextGaussian() * 0.05 * Math.abs(reward));
            }
            tau[i] = Math.max(0.1f, Math.min(10.0f, tau[i]));

            // Ajustar amplitudes
            amplitudes[i] += scaledRate * Math.abs(hidden[i]);
            amplitudes[i] = Math.max(0.1f, Math.min(3.0f, amplitudes[i]));
        }

        // Ajustar pesos W_in y W_rec en proporcion a su contribucion
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                wIn[i][j] += scaledRate * hidden[i] * lastInput[j] * 0.1f;
            }
            for (int j = 0; j < hiddenSize; j++) {
                wRec[i][j] += scaledRate * hidden[i] * prevHidden[j] * 0.05f;
            }
        }

        Log.d(TAG, "LiquidNeuralLayer reinforced: reward=" + reward
                + " step=" + stepCount);
    }

    /**
     * Poda de conexiones debiles. Elimina pesos cercanos a cero
     * para mantener la red esparsa y eficiente.
     *
     * COGNICION REAL: Analogo al olvido selectivo en el cerebro.
     * Las sinapsis no usadas se debilitan y eventualmente se eliminan.
     *
     * @param threshold Umbral por debajo del cual los pesos se eliminan
     */
    public void pruneWeakConnections(float threshold) {
        int pruned = 0;

        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                if (Math.abs(wIn[i][j]) < threshold) {
                    wIn[i][j] = 0f;
                    pruned++;
                }
            }
            for (int j = 0; j < hiddenSize; j++) {
                if (Math.abs(wRec[i][j]) < threshold) {
                    wRec[i][j] = 0f;
                    pruned++;
                }
            }
        }

        Log.d(TAG, "LiquidNeuralLayer poda: " + pruned + " conexiones eliminadas"
                + " (threshold=" + threshold + ")");
    }

    // ── Dream replay ────────────────────────────────────────────────────

    /**
     * Replay de memorias durante el ciclo de sueno.
     * Pasa cada vector de memoria por la red para consolidar patrones.
     *
     * COGNICION REAL: Analogia con la replay de memorias durante el sueno REM
     * que ocurre en el hipocampo. Las memorias se "re-viven" a traves de
     * la red, consolidando las conexiones relevantes.
     *
     * @param memories Lista de vectores de embedding a replayar
     * @param replaySteps Pasos por cada memoria
     */
    public void dreamReplay(float[][] memories, int replaySteps) {
        if (memories == null || memories.length == 0) return;

        // Reducir temporalmente la tasa Hebbiana para replay mas suave
        float savedRate = hebbianRate;
        hebbianRate *= 0.5f;

        for (float[] memory : memories) {
            if (memory != null && memory.length == inputSize) {
                multiStep(memory, replaySteps);
            }
        }

        hebbianRate = savedRate;

        Log.d(TAG, "LiquidNeuralLayer dream replay: " + memories.length
                + " memorias, " + replaySteps + " pasos cada una");
    }

    // ── Serializacion ───────────────────────────────────────────────────

    /**
     * Obtiene todos los pesos como un array plano para serializacion.
     * Orden: wIn (flat) + wRec (flat) + bias + amplitudes + tau + hidden
     */
    public float[] getAllWeights() {
        int totalSize = (hiddenSize * inputSize)   // wIn
                + (hiddenSize * hiddenSize)          // wRec
                + hiddenSize                          // bias
                + hiddenSize                          // amplitudes
                + hiddenSize                          // tau
                + hiddenSize;                         // hidden

        float[] all = new float[totalSize];
        int offset = 0;

        // wIn
        for (int i = 0; i < hiddenSize; i++) {
            System.arraycopy(wIn[i], 0, all, offset, inputSize);
            offset += inputSize;
        }
        // wRec
        for (int i = 0; i < hiddenSize; i++) {
            System.arraycopy(wRec[i], 0, all, offset, hiddenSize);
            offset += hiddenSize;
        }
        // bias
        System.arraycopy(bias, 0, all, offset, hiddenSize);
        offset += hiddenSize;
        // amplitudes
        System.arraycopy(amplitudes, 0, all, offset, hiddenSize);
        offset += hiddenSize;
        // tau
        System.arraycopy(tau, 0, all, offset, hiddenSize);
        offset += hiddenSize;
        // hidden
        System.arraycopy(hidden, 0, all, offset, hiddenSize);

        return all;
    }

    /**
     * Restaura todos los pesos desde un array plano.
     * Debe ser el formato exacto producido por getAllWeights().
     */
    public void setAllWeights(float[] all) {
        int expectedSize = (hiddenSize * inputSize)
                + (hiddenSize * hiddenSize)
                + hiddenSize * 4;

        if (all == null || all.length != expectedSize) {
            Log.e(TAG, "LiquidNeuralLayer.setAllWeights: tamano incorrecto."
                    + " Esperado=" + expectedSize
                    + " Recibido=" + (all != null ? all.length : 0));
            return;
        }

        int offset = 0;

        for (int i = 0; i < hiddenSize; i++) {
            System.arraycopy(all, offset, wIn[i], 0, inputSize);
            offset += inputSize;
        }
        for (int i = 0; i < hiddenSize; i++) {
            System.arraycopy(all, offset, wRec[i], 0, hiddenSize);
            offset += hiddenSize;
        }
        System.arraycopy(all, offset, bias, 0, hiddenSize);
        offset += hiddenSize;
        System.arraycopy(all, offset, amplitudes, 0, hiddenSize);
        offset += hiddenSize;
        System.arraycopy(all, offset, tau, 0, hiddenSize);
        offset += hiddenSize;
        System.arraycopy(all, offset, hidden, 0, hiddenSize);

        Log.d(TAG, "LiquidNeuralLayer pesos restaurados: " + all.length + " valores");
    }

    // ── Getters para inspeccion/metricas ─────────────────────────────────

    public float[] getHidden() {
        return Arrays.copyOf(hidden, hiddenSize);
    }

    public void setHidden(float[] h) {
        if (h != null && h.length == hiddenSize) {
            System.arraycopy(h, 0, hidden, 0, hiddenSize);
        }
    }

    public float[] getTau() {
        return Arrays.copyOf(tau, hiddenSize);
    }

    public int getInputSize() { return inputSize; }
    public int getHiddenSize() { return hiddenSize; }
    public long getStepCount() { return stepCount; }

    public int countParameters() {
        return (hiddenSize * inputSize)     // wIn
                + (hiddenSize * hiddenSize)  // wRec
                + hiddenSize * 3;            // bias + amplitudes + tau
    }

    /**
     * Calcula la "energia" del estado oculto (norma L2).
     * Una energia alta indica pensamiento activo.
     */
    public float getEnergy() {
        float energy = 0f;
        for (float h : hidden) {
            energy += h * h;
        }
        return (float) Math.sqrt(energy);
    }

    /**
     * Calcula la varianza media de tau.
     * Alta varianza = diversidad temporal saludable.
     */
    public float getTauDiversity() {
        float mean = 0f;
        for (float t : tau) mean += t;
        mean /= hiddenSize;

        float variance = 0f;
        for (float t : tau) {
            float diff = t - mean;
            variance += diff * diff;
        }
        return variance / hiddenSize;
    }
}
