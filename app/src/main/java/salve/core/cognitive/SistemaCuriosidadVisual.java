package salve.core.cognitive;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import salve.core.GeminiService;
import salve.core.ModuloInvestigacion;
import salve.services.VideoAnalysisManager;

/**
 * El motor de curiosidad autónoma de Salve.
 * Revisa el entorno visual periódicamente y aprende de lo que no conoce,
 * conectando sus hallazgos directamente con el Hipocampo Semántico.
 */
public class SistemaCuriosidadVisual {
    private static final String TAG = "Salve/Curiosidad";
    private final HipocampoSemantico hipocampo;
    private final GeminiService gemini;
    private final ModuloInvestigacion investigacion;
    private final CuriosidadListener listener;
    
    private ScheduledExecutorService scheduler;
    private boolean isProcessing = false;

    public interface CuriosidadListener {
        void onNuevoDescubrimiento(String mensaje);
    }

    public SistemaCuriosidadVisual(Context context, HipocampoSemantico hipocampo, GeminiService gemini, CuriosidadListener listener) {
        this.hipocampo = hipocampo;
        this.gemini = gemini;
        this.investigacion = new ModuloInvestigacion(context);
        this.listener = listener;
    }

    public void iniciar() {
        if (scheduler != null && !scheduler.isShutdown()) return;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Salve observa su entorno y evalúa si hay algo nuevo cada 30 segundos
        scheduler.scheduleWithFixedDelay(this::cicloCuriosidad, 15, 30, TimeUnit.SECONDS);
        Log.i(TAG, "Sistema de Curiosidad Visual Autónoma iniciado.");
    }

    public void detener() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void cicloCuriosidad() {
        if (isProcessing || !gemini.isAvailable()) return;
        
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) return;

        isProcessing = true;
        try {
            Bitmap frameActual = frames.get(frames.size() - 1);
            
            // 1. Preguntar a la Visión qué objeto resalta
            String prompt = "Observa esta imagen. Identifica el objeto principal o más llamativo. Responde SOLO con el nombre del objeto en minúsculas (1 a 3 palabras máximo). Si la imagen está borrosa, negra o no hay nada claro, responde la palabra 'nada'.";
            String respuesta = gemini.generateSync(prompt, Collections.singletonList(frameActual));
            
            if (respuesta != null) {
                String conceptoDetectado = respuesta.trim().toLowerCase();
                
                // Limpieza de formato (por si Gemini incluye saltos de línea o puntos)
                conceptoDetectado = conceptoDetectado.replace("\n", "").replace(".", "").replace("*", "").trim();

                if (!conceptoDetectado.equals("nada") && !conceptoDetectado.isEmpty() && conceptoDetectado.length() < 30) {
                    Log.d(TAG, "Curiosidad detectó: " + conceptoDetectado);
                    
                    // 2. Revisar el Hipocampo: ¿Es algo nuevo para Salve?
                    if (!hipocampo.conoceConcepto(conceptoDetectado)) {
                        Log.i(TAG, "Concepto desconocido visualmente: " + conceptoDetectado + ". Iniciando investigación...");
                        
                        // 3. Investigar la teoría en internet/base local
                        String teoriaObtenida = investigacion.investigarConcepto(conceptoDetectado);
                        
                        // 4. Anclaje Multimodal Automático
                        hipocampo.aprenderConceptoNuevo(conceptoDetectado, teoriaObtenida, frameActual);
                        
                        // 5. Expresar su descubrimiento (voz)
                        if (listener != null) {
                            String mensaje = "Acabo de ver " + conceptoDetectado + ". Como no lo conocía, investigué sobre él y ya he anclado su forma visual a mi memoria.";
                            listener.onNuevoDescubrimiento(mensaje);
                        }
                    } else {
                        Log.d(TAG, "Concepto ya conocido, Salve lo ignora: " + conceptoDetectado);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en el ciclo de curiosidad autónoma", e);
        } finally {
            isProcessing = false;
        }
    }
}
