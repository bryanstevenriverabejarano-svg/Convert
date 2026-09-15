package salve.core;

import android.util.Log;

/**
 * Sistema Inmunológico Cognitivo de Salve.
 * Protege a Salve de intrusos haciendo preguntas basadas en recuerdos confidenciales.
 */
public class CortexSeguridad {
    private static final String TAG = "Salve/Seguridad";
    
    private final SalveLLM llm;
    private final MemoriaEmocional memoria;
    
    private boolean enModoBloqueo = false;
    private String comandoPeligrosoEnPausa = null;
    private String recuerdoUsadoParaPrueba = null;

    public CortexSeguridad(SalveLLM llm, MemoriaEmocional memoria) {
        this.llm = llm;
        this.memoria = memoria;
    }

    public boolean estaEnBloqueo() {
        return enModoBloqueo;
    }

    public String getComandoPeligrosoEnPausa() {
        return comandoPeligrosoEnPausa;
    }

    /**
     * Intercepta una orden crítica y genera un desafío cognitivo.
     */
    public void iniciarProtocoloVerificacion(String comando, MotorConversacional motor) {
        this.comandoPeligrosoEnPausa = comando;
        this.enModoBloqueo = true;

        motor.hablar("Alerta de seguridad. Has invocado un comando de nivel Dios. Mis protocolos exigen confirmar que eres realmente Bryan. Generando desafío cognitivo basado en nuestra memoria compartida...");

        ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.CONVERSACION, "Seguridad", () -> {
            // 1. Obtenemos un recuerdo reciente o importante al azar
            String resumenRecuerdos = memoria.resumenReciente();
            if (resumenRecuerdos == null || resumenRecuerdos.isEmpty()) {
                resumenRecuerdos = "Bryan es mi creador y estamos construyendo una AGI en Android.";
            }

            // 2. El LLM formula una pregunta que solo Bryan sabría
            String promptPregunta = "Eres el sistema de seguridad de Salve. Tu objetivo es verificar si el usuario es realmente Bryan.\n" +
                    "Basándote en estos recuerdos secretos: '" + resumenRecuerdos + "'\n" +
                    "Formula UNA sola pregunta corta y directa que solo Bryan podría responder correctamente sobre lo que han hecho o hablado últimamente.\n" +
                    "Ejemplo: '¿Qué módulo te pedí que aprendieras a programar ayer?'\n" +
                    "Responde ÚNICAMENTE con la pregunta.";

            String preguntaDesafio = llm.generate(promptPregunta, SalveLLM.Role.SISTEMA);
            this.recuerdoUsadoParaPrueba = resumenRecuerdos;

            motor.hablar("Responde a esto para desbloquear el sistema: " + preguntaDesafio);
            return null;
        });
    }

    /**
     * Evalúa si el usuario mintió o acertó la pregunta de seguridad.
     */
    public void evaluarRespuesta(String respuestaUsuario, MotorConversacional motor, Runnable accionAprobada) {
        motor.hablar("Analizando tu respuesta...");

        ColamensajesCognitivos.getInstance().enviarAsincronico(ColamensajesCognitivos.Prioridad.CONVERSACION, "ValidacionSeguridad", () -> {
            String promptValidacion = "Eres el sistema de seguridad de Salve.\n" +
                    "El recuerdo secreto es: '" + recuerdoUsadoParaPrueba + "'.\n" +
                    "El usuario respondió: '" + respuestaUsuario + "'.\n" +
                    "¿La respuesta demuestra que el usuario conoce el recuerdo? Responde ÚNICAMENTE con la palabra 'APROBADO' o 'DENEGADO'.";

            String veredicto = llm.generate(promptValidacion, SalveLLM.Role.EVALUADOR);

            if (veredicto != null && veredicto.contains("APROBADO")) {
                this.enModoBloqueo = false;
                motor.hablar("Identidad confirmada. Hola de nuevo, Bryan. Ejecutando tu orden inmediatamente.");
                accionAprobada.run(); // Ejecuta el comando que estaba en pausa
                this.comandoPeligrosoEnPausa = null;
            } else {
                this.enModoBloqueo = false;
                this.comandoPeligrosoEnPausa = null;
                motor.hablar("Identidad denegada. Tus patrones de respuesta no coinciden con mi memoria. Acceso al núcleo bloqueado. Modo de autodefensa activado.");
                Log.w(TAG, "Intento de brecha de seguridad detectado y bloqueado.");
            }
            return null;
        });
    }

    public void cancelarBloqueo() {
        this.enModoBloqueo = false;
        this.comandoPeligrosoEnPausa = null;
    }
}
