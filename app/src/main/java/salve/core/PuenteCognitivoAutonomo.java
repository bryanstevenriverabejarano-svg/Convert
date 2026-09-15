package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Map;

import salve.core.cognitive.CognitiveCore;
import salve.core.cognitive.ConceptSpace;

/**
 * EL PUENTE.
 * Transfiere experiencias de la Memoria (transitorio) al Grafo (estructural)
 * basándose únicamente en energía neuronal, sin tocar el LLM.
 */
public class PuenteCognitivoAutonomo {
    private static final String TAG = "Salve/Puente";
    private final MemoriaEmocional memoria;
    private final GrafoConocimientoVivo grafo;
    private final CognitiveCore core;

    public PuenteCognitivoAutonomo(Context context, CognitiveCore core) throws Exception {
        this.memoria = new MemoriaEmocional(context);
        this.grafo = new GrafoConocimientoVivo(context);
        this.core = core;
    }

    public void ejecutarConsolidacionMatematica() {
        ConceptSpace espacioConceptos = core.getConceptSpace();
        
        // 1. Obtener conceptos con mayor "energía" o frecuencia de uso real
        Map<String, float[]> conceptosActivos = espacioConceptos.getAllConcepts(); // Asumiendo un getter en tu ConceptSpace
        
        for (Map.Entry<String, float[]> entry : conceptosActivos.entrySet()) {
            String concepto = entry.getKey();
            float[] vector = entry.getValue();
            
            // Simulación matemática de peso: Si la suma absoluta del vector supera un umbral
            float energiaConcepto = calcularEnergia(vector);
            
            if (energiaConcepto > 2.5f) { // Umbral de "epifanía"
                Log.d(TAG, "Concepto ha cruzado el umbral estructural: " + concepto);
                
                // 2. Cristalizar en el Grafo de Conocimiento directamente
                // No necesitamos que el LLM nos diga que es importante. La matemática lo dice.
                memoria.registrarHallazgoEnGrafo(
                        concepto,
                        "Cristalización Autónoma",
                        java.util.Collections.singletonList("cristalizado"),
                        true
                );
            }
        }
        
        // 3. Forjar la Identidad (Bias modification)
        // En vez de un texto de identidad, modificamos los rasgos de EmergentBehavior
        core.getEmergentBehavior().observe("auto_reflexion", "consolidacion_estructural", 0.5f);
    }

    private float calcularEnergia(float[] vector) {
        float suma = 0;
        for (float val : vector) {
            suma += Math.abs(val);
        }
        return suma;
    }
}
