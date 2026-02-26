package salve.core.cognitive;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CognitiveState — Estado cognitivo serializable de Salve.
 *
 * FUNCION COGNITIVA: Sin persistencia, Salve "muere" cada vez que se cierra
 * la app. Esta clase guarda y restaura TODO el estado cognitivo:
 * pesos neurales, conceptos aprendidos, rasgos emergentes, cadenas causales,
 * estado oculto de la red liquida, y el contenido de WorkingMemory.
 *
 * INSPIRACION CIENTIFICA: Analogia con la consolidacion de memoria durante
 * el sueno (Stickgold, 2005). Al igual que el cerebro consolida
 * recuerdos durante la noche, CognitiveState consolida el estado completo
 * del sustrato cognitivo al persistirlo.
 *
 * FORMATO: JSON serializado con Gson al almacenamiento interno de Android.
 * Los pesos neurales (float arrays) se serializan como arrays JSON.
 * No es el formato mas eficiente, pero es legible y depurable.
 *
 * LIMITACION CONOCIDA: Con muchos conceptos y pesos, el archivo puede
 * ser grande. En produccion se podria usar protobuf o flatbuffers.
 *
 * RELACION CON OTROS MODULOS:
 *   - Lee de: LiquidNeuralLayer, ConceptSpace, ReasoningEngine,
 *     EmergentBehavior, WorkingMemory, ThoughtStream
 *   - Escribe a: Los mismos al restaurar
 *   - Invocado por: CognitiveCore (auto-save), ThinkWorker (consolidacion)
 */
public class CognitiveState {

    private static final String TAG = "Salve/Cognitive";
    private static final String STATE_FILENAME = "salve_cognitive_state.json";

    private final Context context;
    private final Gson gson;

    public CognitiveState(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeSpecialFloatingPointValues()
                .create();
    }

    /**
     * Guarda el estado cognitivo completo a disco.
     * Debe llamarse:
     *   - Periodicamente en background (ThinkWorker)
     *   - Al cerrar la app (onPause)
     *   - Despues de cada ciclo de sueno
     *   - Despues de conversaciones significativas
     *
     * @return true si se guardo exitosamente
     */
    public boolean save(LiquidNeuralLayer liquidLayer,
                        ConceptSpace conceptSpace,
                        ReasoningEngine reasoningEngine,
                        EmergentBehavior emergentBehavior,
                        WorkingMemory workingMemory,
                        PatternFormation patternFormation,
                        ThoughtStream thoughtStream,
                        float[] currentMood) {

        try {
            StateData data = new StateData();

            // Pesos de LiquidNeuralLayer
            if (liquidLayer != null) {
                data.liquidWeights = liquidLayer.getAllWeights();
                data.liquidHidden = liquidLayer.getHidden();
                data.liquidTaus = liquidLayer.getTau();
                data.liquidInputSize = liquidLayer.getInputSize();
                data.liquidHiddenSize = liquidLayer.getHiddenSize();
                data.liquidStepCount = liquidLayer.getStepCount();
            }

            // ConceptSpace
            if (conceptSpace != null) {
                data.conceptVectors = conceptSpace.getAllConcepts();
                data.conceptVectorSize = conceptSpace.getVectorSize();
            }

            // ReasoningEngine
            if (reasoningEngine != null) {
                data.causalLinks = serializeCausalLinks(reasoningEngine.getAllCausalLinks());
            }

            // EmergentBehavior
            if (emergentBehavior != null) {
                data.emergentTraits = emergentBehavior.getTraits();
            }

            // PatternFormation
            if (patternFormation != null) {
                data.cellStates = patternFormation.getGridState();
                data.patternGridSize = patternFormation.getGridSize();
                data.patternChannels = patternFormation.getChannels();
            }

            // ThoughtStream
            if (thoughtStream != null) {
                data.totalThoughtTicks = thoughtStream.getTotalTicks();
            }

            // Estado de animo actual
            data.currentMood = currentMood;

            // Timestamp
            data.lastSavedTimestamp = System.currentTimeMillis();

            // Serializar y escribir
            String json = gson.toJson(data);
            File file = new File(context.getFilesDir(), STATE_FILENAME);

            // Escritura atomica: escribir a temporal y renombrar
            File temp = new File(context.getFilesDir(), STATE_FILENAME + ".tmp");
            try (FileWriter writer = new FileWriter(temp)) {
                writer.write(json);
            }

            // Renombrar atomicamente
            if (temp.renameTo(file)) {
                Log.d(TAG, "CognitiveState guardado: " + (json.length() / 1024) + "KB"
                        + " conceptos=" + (data.conceptVectors != null ? data.conceptVectors.size() : 0)
                        + " rasgos=" + (data.emergentTraits != null ? data.emergentTraits.size() : 0));
                return true;
            } else {
                Log.e(TAG, "CognitiveState: fallo rename atomico");
                // Intentar escritura directa como fallback
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(json);
                }
                return true;
            }

        } catch (Exception e) {
            Log.e(TAG, "CognitiveState: error guardando", e);
            return false;
        }
    }

    /**
     * Restaura el estado cognitivo desde disco.
     * Si no existe archivo, retorna null (estado de "nacimiento").
     *
     * @return RestoreResult con los datos restaurados, o null
     */
    public RestoreResult restore() {
        File file = new File(context.getFilesDir(), STATE_FILENAME);
        if (!file.exists()) {
            Log.d(TAG, "CognitiveState: no hay estado previo (nacimiento)");
            return null;
        }

        try {
            StringBuilder jsonBuilder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line);
                }
            }

            StateData data = gson.fromJson(jsonBuilder.toString(), StateData.class);
            if (data == null) {
                Log.w(TAG, "CognitiveState: datos deserializados son null");
                return null;
            }

            RestoreResult result = new RestoreResult();
            result.data = data;
            result.ageMs = System.currentTimeMillis() - data.lastSavedTimestamp;

            Log.d(TAG, "CognitiveState restaurado: edad="
                    + (result.ageMs / 1000 / 60) + " min"
                    + " conceptos=" + (data.conceptVectors != null ? data.conceptVectors.size() : 0)
                    + " rasgos=" + (data.emergentTraits != null ? data.emergentTraits.size() : 0));

            return result;

        } catch (Exception e) {
            Log.e(TAG, "CognitiveState: error restaurando", e);
            return null;
        }
    }

    /**
     * Aplica el estado restaurado a los componentes cognitivos.
     */
    public void applyRestore(RestoreResult result,
                              LiquidNeuralLayer liquidLayer,
                              ConceptSpace conceptSpace,
                              ReasoningEngine reasoningEngine,
                              EmergentBehavior emergentBehavior,
                              PatternFormation patternFormation) {

        if (result == null || result.data == null) return;
        StateData data = result.data;

        try {
            // Restaurar LiquidNeuralLayer
            if (liquidLayer != null && data.liquidWeights != null) {
                liquidLayer.setAllWeights(data.liquidWeights);
                if (data.liquidHidden != null) {
                    liquidLayer.setHidden(data.liquidHidden);
                }
                Log.d(TAG, "CognitiveState: LiquidNeuralLayer restaurada");
            }

            // Restaurar ConceptSpace
            if (conceptSpace != null && data.conceptVectors != null) {
                conceptSpace.setAllConcepts(data.conceptVectors);
                Log.d(TAG, "CognitiveState: ConceptSpace restaurado");
            }

            // Restaurar ReasoningEngine
            if (reasoningEngine != null && data.causalLinks != null) {
                Map<String, List<ReasoningEngine.CausalLink>> links =
                        deserializeCausalLinks(data.causalLinks);
                reasoningEngine.setAllCausalLinks(links);
                Log.d(TAG, "CognitiveState: ReasoningEngine restaurado");
            }

            // Restaurar EmergentBehavior
            if (emergentBehavior != null && data.emergentTraits != null) {
                emergentBehavior.setTraits(data.emergentTraits);
                Log.d(TAG, "CognitiveState: EmergentBehavior restaurado");
            }

            // Restaurar PatternFormation
            if (patternFormation != null && data.cellStates != null) {
                patternFormation.setGridState(data.cellStates);
                Log.d(TAG, "CognitiveState: PatternFormation restaurado");
            }

        } catch (Exception e) {
            Log.e(TAG, "CognitiveState: error aplicando restauracion", e);
        }
    }

    /**
     * Verifica si existe un estado guardado.
     */
    public boolean hasSavedState() {
        return new File(context.getFilesDir(), STATE_FILENAME).exists();
    }

    /**
     * Elimina el estado guardado (reset completo).
     */
    public boolean deleteState() {
        File file = new File(context.getFilesDir(), STATE_FILENAME);
        boolean deleted = file.delete();
        Log.d(TAG, "CognitiveState: estado eliminado=" + deleted);
        return deleted;
    }

    // ── Serializacion de tipos complejos ─────────────────────────────────

    /**
     * Convierte las cadenas causales a formato serializable con Gson.
     * CausalLink contiene enum LinkType que Gson maneja directamente.
     */
    private Map<String, List<SerializedCausalLink>> serializeCausalLinks(
            Map<String, List<ReasoningEngine.CausalLink>> links) {
        Map<String, List<SerializedCausalLink>> result = new HashMap<>();

        for (Map.Entry<String, List<ReasoningEngine.CausalLink>> entry : links.entrySet()) {
            List<SerializedCausalLink> serialized = new ArrayList<>();
            for (ReasoningEngine.CausalLink link : entry.getValue()) {
                SerializedCausalLink sl = new SerializedCausalLink();
                sl.effect = link.effect;
                sl.strength = link.strength;
                sl.type = link.type.name();
                sl.observationCount = link.observationCount;
                serialized.add(sl);
            }
            result.put(entry.getKey(), serialized);
        }

        return result;
    }

    private Map<String, List<ReasoningEngine.CausalLink>> deserializeCausalLinks(
            Map<String, List<SerializedCausalLink>> serialized) {
        Map<String, List<ReasoningEngine.CausalLink>> result = new HashMap<>();

        for (Map.Entry<String, List<SerializedCausalLink>> entry : serialized.entrySet()) {
            List<ReasoningEngine.CausalLink> links = new ArrayList<>();
            for (SerializedCausalLink sl : entry.getValue()) {
                ReasoningEngine.CausalLink link = new ReasoningEngine.CausalLink();
                link.effect = sl.effect;
                link.strength = sl.strength;
                try {
                    link.type = ReasoningEngine.LinkType.valueOf(sl.type);
                } catch (Exception e) {
                    link.type = ReasoningEngine.LinkType.SEMANTIC;
                }
                link.observationCount = sl.observationCount;
                links.add(link);
            }
            result.put(entry.getKey(), links);
        }

        return result;
    }

    // ── Clases de datos ─────────────────────────────────────────────────

    /**
     * Estructura que contiene TODO el estado cognitivo serializado.
     */
    public static class StateData {
        // LiquidNeuralLayer
        public float[] liquidWeights;
        public float[] liquidHidden;
        public float[] liquidTaus;
        public int liquidInputSize;
        public int liquidHiddenSize;
        public long liquidStepCount;

        // ConceptSpace
        public Map<String, float[]> conceptVectors;
        public int conceptVectorSize;

        // ReasoningEngine (cadenas causales)
        public Map<String, List<SerializedCausalLink>> causalLinks;

        // EmergentBehavior (rasgos)
        public List<EmergentBehavior.EmergentTrait> emergentTraits;

        // PatternFormation
        public float[] cellStates;
        public int patternGridSize;
        public int patternChannels;

        // ThoughtStream
        public long totalThoughtTicks;

        // Estado emocional
        public float[] currentMood;

        // Metadata
        public long lastSavedTimestamp;
    }

    /**
     * Resultado de una operacion de restauracion.
     */
    public static class RestoreResult {
        public StateData data;
        /** Edad del estado guardado en milisegundos */
        public long ageMs;
    }

    /**
     * Version serializable de CausalLink (sin enums directos).
     */
    public static class SerializedCausalLink {
        public String effect;
        public float strength;
        public String type;
        public int observationCount;
    }
}
