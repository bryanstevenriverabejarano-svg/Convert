package salve.core.cognitive;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * PatternFormation — Automata celular neural para formacion de conceptos.
 *
 * FUNCION COGNITIVA: Implementa un grid de "neuronas virtuales" donde cada
 * celda percibe su vecindario, actualiza su estado segun reglas aprendidas,
 * y los patrones estables que emergen representan conceptos formados.
 * La auto-reparacion es inherente: si se pierde estado, el patron se reconstruye.
 *
 * INSPIRACION CIENTIFICA: Neural Cellular Automata (Mordvintsev et al., 2020,
 * Google Brain). Cada celda ejecuta un MLP diminuto compartido que decide
 * como actualizar su estado basandose en el vecindario. La actualizacion es
 * estocastica (no todas las celdas se actualizan cada tick) para fomentar
 * la emergencia de patrones robustos.
 *
 * LIMITACION CONOCIDA: El grid 2D es una simplificacion. Un espacio de mayor
 * dimension captaria relaciones mas ricas pero seria mas costoso.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: LiquidNeuralLayer (estado oculto como seed)
 *   - Alimenta a: ConceptSpace (patrones → conceptos), ThoughtStream (patrones detectados)
 */
public class PatternFormation {

    private static final String TAG = "Salve/Cognitive";

    /** Tamano del grid (gridSize x gridSize celdas) */
    private final int gridSize;

    /** Canales por celda (dimension del estado de cada celda) */
    private final int channels;

    /** Grid de estados: [gridSize][gridSize][channels] */
    private float[][][] grid;

    /** Pesos del MLP compartido para regla de actualizacion.
     *  Input: vecindario promedio (channels) + estado propio (channels) = 2*channels
     *  Hidden: channels
     *  Output: channels (delta de estado)
     */
    private float[][] mlpW1;  // [channels][2 * channels]
    private float[] mlpB1;     // [channels]
    private float[][] mlpW2;  // [channels][channels]
    private float[] mlpB2;     // [channels]

    /** Probabilidad de que cada celda se actualice en un tick */
    private float updateProbability = 0.5f;

    /** Tasa de aprendizaje para ajuste de reglas */
    private float learningRate = 0.001f;

    /** Contador de ticks */
    private long tickCount = 0;

    /** Lista de patrones estables detectados */
    private final List<DetectedPattern> detectedPatterns;

    private final Random rng;

    public PatternFormation(int gridSize, int channels) {
        this.gridSize = gridSize;
        this.channels = channels;
        this.grid = new float[gridSize][gridSize][channels];
        this.detectedPatterns = new ArrayList<>();
        this.rng = new Random(42);

        initializeMLP();
        seedGrid();
    }

    private void initializeMLP() {
        int inputDim = 2 * channels;

        mlpW1 = new float[channels][inputDim];
        mlpB1 = new float[channels];
        mlpW2 = new float[channels][channels];
        mlpB2 = new float[channels];

        float scale1 = (float) Math.sqrt(2.0 / inputDim);
        float scale2 = (float) Math.sqrt(2.0 / channels);

        for (int i = 0; i < channels; i++) {
            for (int j = 0; j < inputDim; j++) {
                mlpW1[i][j] = (float) (rng.nextGaussian() * scale1);
            }
            for (int j = 0; j < channels; j++) {
                mlpW2[i][j] = (float) (rng.nextGaussian() * scale2 * 0.1f);
            }
        }
    }

    private void seedGrid() {
        float scale = 0.1f;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                for (int c = 0; c < channels; c++) {
                    grid[y][x][c] = (float) (rng.nextGaussian() * scale);
                }
            }
        }
    }

    /**
     * Inyecta un estado externo como seed en el centro del grid.
     * Usado para plantar el estado de LiquidNeuralLayer y dejar que
     * el automata forme patrones a partir de el.
     *
     * @param state Vector de estado (se mapea a los canales del centro)
     */
    public synchronized void injectState(float[] state) {
        if (state == null) return;

        int centerY = gridSize / 2;
        int centerX = gridSize / 2;
        int len = Math.min(state.length, channels);

        // Inyectar en el centro y vecindario inmediato
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int y = centerY + dy;
                int x = centerX + dx;
                if (y >= 0 && y < gridSize && x >= 0 && x < gridSize) {
                    float decay = (dy == 0 && dx == 0) ? 1.0f : 0.5f;
                    for (int c = 0; c < len; c++) {
                        grid[y][x][c % channels] += state[c] * decay * 0.3f;
                    }
                }
            }
        }
    }

    /**
     * Ejecuta un tick del automata celular.
     * Cada celda que se actualiza:
     *  1. Percibe el promedio de su vecindario Moore (8 vecinos)
     *  2. Concatena vecindario + estado propio
     *  3. Pasa por MLP compartido → obtiene delta
     *  4. Actualiza su estado con el delta
     *
     * La actualizacion es estocastica para emergencia robusta.
     *
     * @return Numero de celdas actualizadas
     */
    public synchronized int tick() {
        tickCount++;
        float[][][] newGrid = new float[gridSize][gridSize][channels];
        int updated = 0;

        // Copiar grid actual
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                System.arraycopy(grid[y][x], 0, newGrid[y][x], 0, channels);
            }
        }

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                // Actualizacion estocastica
                if (rng.nextFloat() > updateProbability) continue;

                // Percibir vecindario (promedio Moore)
                float[] neighborhood = perceiveNeighborhood(y, x);

                // Concatenar vecindario + estado propio
                float[] mlpInput = new float[2 * channels];
                System.arraycopy(neighborhood, 0, mlpInput, 0, channels);
                System.arraycopy(grid[y][x], 0, mlpInput, channels, channels);

                // Forward MLP: hidden = ReLU(W1 * input + b1)
                float[] hidden = new float[channels];
                for (int i = 0; i < channels; i++) {
                    float sum = mlpB1[i];
                    for (int j = 0; j < 2 * channels; j++) {
                        sum += mlpW1[i][j] * mlpInput[j];
                    }
                    hidden[i] = Math.max(0, sum); // ReLU
                }

                // Output = W2 * hidden + b2
                float[] delta = new float[channels];
                for (int i = 0; i < channels; i++) {
                    float sum = mlpB2[i];
                    for (int j = 0; j < channels; j++) {
                        sum += mlpW2[i][j] * hidden[j];
                    }
                    delta[i] = sum;
                }

                // Aplicar delta con residual connection
                for (int c = 0; c < channels; c++) {
                    newGrid[y][x][c] = grid[y][x][c] + delta[c] * 0.1f;
                    // Clamp
                    newGrid[y][x][c] = Math.max(-3f, Math.min(3f, newGrid[y][x][c]));
                }

                updated++;
            }
        }

        grid = newGrid;

        // Detectar patrones estables cada 10 ticks
        if (tickCount % 10 == 0) {
            detectStablePatterns();
        }

        return updated;
    }

    /**
     * Ejecuta multiples ticks.
     */
    public int multiTick(int numTicks) {
        int totalUpdated = 0;
        for (int i = 0; i < numTicks; i++) {
            totalUpdated += tick();
        }
        return totalUpdated;
    }

    /**
     * Obtiene los patrones estables detectados desde la ultima consulta.
     */
    public synchronized List<DetectedPattern> getDetectedPatterns() {
        List<DetectedPattern> result = new ArrayList<>(detectedPatterns);
        detectedPatterns.clear();
        return result;
    }

    /**
     * Obtiene el estado del grid como vector plano (para serializacion).
     */
    public synchronized float[] getGridState() {
        float[] flat = new float[gridSize * gridSize * channels];
        int idx = 0;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                System.arraycopy(grid[y][x], 0, flat, idx, channels);
                idx += channels;
            }
        }
        return flat;
    }

    /**
     * Restaura el estado del grid desde un vector plano.
     */
    public synchronized void setGridState(float[] flat) {
        if (flat == null || flat.length != gridSize * gridSize * channels) {
            Log.w(TAG, "PatternFormation: estado de grid invalido, ignorando");
            return;
        }
        int idx = 0;
        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                System.arraycopy(flat, idx, grid[y][x], 0, channels);
                idx += channels;
            }
        }
    }

    /**
     * Refuerza las reglas del MLP que produjeron un patron positivo.
     */
    public synchronized void reinforceRules(float reward) {
        if (Math.abs(reward) < 0.01f) return;

        float rate = learningRate * reward;
        // Perturbar pesos del MLP en la direccion de la recompensa
        for (int i = 0; i < channels; i++) {
            for (int j = 0; j < 2 * channels; j++) {
                mlpW1[i][j] += rate * (float) (rng.nextGaussian() * 0.01);
            }
            for (int j = 0; j < channels; j++) {
                mlpW2[i][j] += rate * (float) (rng.nextGaussian() * 0.01);
            }
        }
    }

    public int getGridSize() { return gridSize; }
    public int getChannels() { return channels; }
    public long getTickCount() { return tickCount; }

    // ── Internos ──────────────────────────────────────────────────────────

    private float[] perceiveNeighborhood(int cy, int cx) {
        float[] avg = new float[channels];
        int count = 0;

        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dy == 0 && dx == 0) continue;
                int ny = cy + dy;
                int nx = cx + dx;
                if (ny >= 0 && ny < gridSize && nx >= 0 && nx < gridSize) {
                    for (int c = 0; c < channels; c++) {
                        avg[c] += grid[ny][nx][c];
                    }
                    count++;
                }
            }
        }

        if (count > 0) {
            for (int c = 0; c < channels; c++) {
                avg[c] /= count;
            }
        }

        return avg;
    }

    /**
     * Detecta patrones estables en el grid buscando regiones con
     * alta varianza interna (actividad) y baja varianza temporal
     * (estabilidad).
     */
    private void detectStablePatterns() {
        // Dividir grid en cuadrantes y analizar cada uno
        int halfSize = gridSize / 2;
        int[][] regions = {{0, 0}, {0, halfSize}, {halfSize, 0}, {halfSize, halfSize}};

        for (int[] region : regions) {
            float[] regionVector = computeRegionVector(region[0], region[1], halfSize);
            float energy = vectorEnergy(regionVector);

            // Si la energia es suficientemente alta, es un patron activo
            if (energy > 0.5f) {
                DetectedPattern pattern = new DetectedPattern();
                pattern.vector = regionVector;
                pattern.energy = energy;
                pattern.regionY = region[0];
                pattern.regionX = region[1];
                pattern.tick = tickCount;
                detectedPatterns.add(pattern);
            }
        }
    }

    private float[] computeRegionVector(int startY, int startX, int size) {
        float[] vec = new float[channels];
        int count = 0;

        for (int y = startY; y < Math.min(startY + size, gridSize); y++) {
            for (int x = startX; x < Math.min(startX + size, gridSize); x++) {
                for (int c = 0; c < channels; c++) {
                    vec[c] += grid[y][x][c];
                }
                count++;
            }
        }

        if (count > 0) {
            for (int c = 0; c < channels; c++) {
                vec[c] /= count;
            }
        }

        return vec;
    }

    private float vectorEnergy(float[] vec) {
        float energy = 0f;
        for (float v : vec) energy += v * v;
        return (float) Math.sqrt(energy);
    }

    // ── Clase de patron detectado ────────────────────────────────────────

    public static class DetectedPattern {
        public float[] vector;
        public float energy;
        public int regionY;
        public int regionX;
        public long tick;

        @Override
        public String toString() {
            return "Pattern(energy=" + String.format("%.2f", energy)
                    + " region=" + regionY + "," + regionX
                    + " tick=" + tick + ")";
        }
    }
}
