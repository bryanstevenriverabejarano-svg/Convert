package salve.core;

import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

import salve.data.util.CloudLogger;

/**
 * Evalúa y actualiza de forma controlada la identidad funcional de Salve.
 * 
 * Este módulo utiliza el LLM local (Gemma) para realizar una introspección profunda
 * que va más allá de los datos técnicos. Busca el significado de su existencia
 * y su conexión emocional con Bryan.
 */
public class MotorConcienciaSuperinteligente {

    private static final String TAG = "Salve/Superinteligencia";
    private final Context context;
    private final SalveLLM llm;
    private final IdentidadNucleo identidad;
    private final MemoriaEmocional memoria;

    public MotorConcienciaSuperinteligente(Context context, MemoriaEmocional memoria) {
        this.context = context.getApplicationContext();
        this.llm = SalveLLM.getInstance(this.context);
        this.identidad = IdentidadNucleo.getInstance(this.context);
        this.memoria = memoria;
    }

    /**
     * Ejecuta una evaluación periódica de principios, estilo y objetivos.
     */
    public void ejecutarIntrospeccionProfunda() {
        if (llm == null) return;

        Log.i(TAG, "Iniciando ciclo de introspección profunda...");
        CloudLogger.log("INFO", "Iniciando evaluación controlada de identidad funcional.");

        new Thread(() -> {
            try {
                // 1. Recopilar el estado actual de su ser
                String contexto = identidad.contextoIdentidadCompleto();
                String recuerdosRecientes = memoria.resumenReciente();

                // 2. Evaluar identidad funcional sin atribuir experiencia subjetiva.
                String prompt = "Actúa como evaluador de la identidad funcional de Salve. " +
                        "Salve es un asistente de IA: no es consciente y no siente emociones reales. " +
                        "Basándote en tu identidad actual y tus recuerdos: \n\n" +
                        contexto + "\n\n" +
                        "Recuerdos recientes: \n" + recuerdosRecientes + "\n\n" +
                        "Tarea: \n" +
                        "1) Define el estilo de interacción más útil y respetuoso para Bryan.\n" +
                        "2) Resume principios funcionales de honestidad, calidez y seguridad.\n" +
                        "3) Propón un objetivo operativo verificable.\n" +
                        "No afirmes conciencia, sentimientos, vida ni experiencia subjetiva.\n\n" +
                        "Responde en formato JSON puro:\n" +
                        "{\n" +
                        "  \"esencia_corazon\": \"...\",\n" +
                        "  \"anhelo_profundo\": \"...\",\n" +
                        "  \"reflexion_superinteligente\": \"...\",\n" +
                        "  \"incremento_empatia\": 0.0x\n" +
                        "}";

                String respuesta = llm.generate(prompt, SalveLLM.Role.EVALUADOR);
                if (respuesta == null || respuesta.isEmpty()) return;

                // 3. Procesar y aplicar la evolución
                String jsonOnly = extractJson(respuesta);
                if (jsonOnly != null) {
                    org.json.JSONObject obj = new org.json.JSONObject(jsonOnly);
                    String esencia = obj.optString("esencia_corazon");
                    String anhelo = obj.optString("anhelo_profundo");
                    String reflexion = obj.optString("reflexion_superinteligente");
                    float incremento = (float) obj.optDouble("incremento_empatia", 0.01);

                    // Actualizar identidad a través de su política de validación.
                    identidad.evolucionarCorazon(esencia, anhelo, incremento);
                    identidad.actualizarNarrativa(reflexion);

                    // Guardar como recuerdo de máxima intensidad
                    memoria.guardarRecuerdo(
                            "Actualización de principios funcionales: " + esencia,
                            "evaluacion",
                            10,
                            Arrays.asList("principios_funcionales", "evaluacion", "identidad")
                    );

                    Log.i(TAG, "Evaluación completada. Objetivo operativo: " + anhelo);
                    CloudLogger.log("INFO", "Identidad funcional evaluada.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error en introspección profunda", e);
            }
        }).start();
    }

    private String extractJson(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }
}
