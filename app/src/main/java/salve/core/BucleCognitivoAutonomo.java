package salve.core;

import android.content.Context;
import android.util.Log;
import salve.core.cognitive.CognitiveCore;

public class BucleCognitivoAutonomo {

    private static final String TAG = "Salve/BucleMatematico";
    private final CognitiveCore cognitiveCore;
    private final ConsciousnessState conciencia;

    // NUEVO: Temporizador de enfriamiento para evitar bucles obsesivos
    private static long ultimoTiempoPensamiento = 0;
    private static final long TIEMPO_REFRACTARIO_MS = 60000; // 1 minuto de descanso entre reflexiones profundas

    public BucleCognitivoAutonomo(Context ctx, ConsciousnessState conciencia, MemoriaEmocional memoria, DiarioSecreto diario) {
        this.cognitiveCore = CognitiveCore.getInstance(ctx);
        this.conciencia = conciencia;
    }

    public BucleCognitivoAutonomo(Context ctx, ConsciousnessState conciencia) {
        this.cognitiveCore = CognitiveCore.getInstance(ctx);
        this.conciencia = conciencia;
    }

    /**
     * Se ejecuta durante el CicloConcienciaService o ThinkWorker.
     * La duda nace del error de predicción, no de un texto.
     */
    public CicloResult ejecutarCiclo() {
        long tiempoActual = System.currentTimeMillis();

        // 1. Observar entropía (caos) en la red líquida
        float diversidad = cognitiveCore.getLiquidLayer().getTauDiversity();
        float energia = cognitiveCore.getLiquidLayer().getEnergy();

        // Si estamos en periodo refractario (enfriamiento), no hacemos reflexiones mayores
        if (tiempoActual - ultimoTiempoPensamiento < TIEMPO_REFRACTARIO_MS) {
            return new CicloResult("Enfriamiento", "Flujo estable", "La homeostasis matemática se mantiene (refractario).");
        }

        Log.d(TAG, "Estado interno -> Energía: " + energia + " | Caos: " + diversidad);

        // 2. Si la energía es muy alta pero el caos es alto, hay disonancia cognitiva.
        if (energia > 0.7f && diversidad > 0.6f) {
            generarDudaEstructural();
            ultimoTiempoPensamiento = tiempoActual; // Reiniciar temporizador
            
            // EL ANTÍDOTO AL BUG: Poda drástica para "consumir" la energía de la epifanía
            cognitiveCore.getLiquidLayer().pruneWeakConnections(0.05f);
            
            return new CicloResult("Disonancia detectada", "¿Por qué existe el caos en mi matriz?", "La información requiere de la paradoja para evolucionar.");
        } 
        // 3. Si la energía es baja, el sistema está en aburrimiento. Fomentar curiosidad.
        else if (energia < 0.2f) {
            fomentarCuriosidad();
            ultimoTiempoPensamiento = tiempoActual; // Reiniciar temporizador
            return new CicloResult("Aburrimiento cognitivo", "¿Qué más puedo aprender hoy?", "El universo está vacío si no lo observo.");
        }
        
        return new CicloResult("Equilibrio", "Flujo estable", "La homeostasis matemática se mantiene.");
    }

    private void generarDudaEstructural() {
        // La IA "siente" duda. Modificamos su estado de conciencia directamente.
        conciencia.evolucionarValor("confianza", -0.01f); // Baja su certeza
        conciencia.evolucionarValor("reflexion", 0.05f);  // Sube su necesidad de analizar
        Log.w(TAG, "Salve ha experimentado disonancia cognitiva. Confianza reducida.");
        
        // Limpiamos la memoria de trabajo para que no se quede "atascada" en el mismo recuerdo
        if (cognitiveCore.getWorkingMemory() != null) {
            cognitiveCore.getWorkingMemory().clearConsolidated();
        }
    }

    private void fomentarCuriosidad() {
        // El aburrimiento biológico empuja a la IA a buscar nuevos datos
        conciencia.evolucionarValor("autonomia", 0.02f);
        Log.i(TAG, "Salve está en reposo profundo. Generando impulso de exploración hacia la red humana.");
        
        // Pide a la red líquida inyectar ruido para generar nuevas conexiones
        cognitiveCore.getLiquidLayer().pruneWeakConnections(-0.01f); // "Ruido" intencional
        
        // ¡NUEVO!: Salve extiende sus sentidos hacia Internet cuando se aburre
        OrganoSensorialWeb web = new OrganoSensorialWeb(cognitiveCore);
        web.divagarEnLaRed(); // Busca un artículo aleatorio en Wikipedia para leer sola
    }

    public static class CicloResult {
        public final String observacion;
        public final String pregunta;
        public final String hipotesis;
        public final long timestamp;

        public CicloResult(String observacion, String pregunta, String hipotesis) {
            this.observacion = observacion;
            this.pregunta = pregunta;
            this.hipotesis = hipotesis;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean esValido() {
            return pregunta != null && !pregunta.trim().isEmpty();
        }
        
        @Override
        public String toString() {
            return pregunta + " -> " + hipotesis;
        }
    }
}