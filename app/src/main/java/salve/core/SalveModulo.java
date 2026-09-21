package salve.core;

import android.speech.tts.TextToSpeech;
import java.text.Normalizer;
import java.util.Locale;

public class SalveModulo {

    private final TextToSpeech tts;

    public SalveModulo(TextToSpeech tts) {
        this.tts = tts;
    }

    // Respuesta en texto (útil para mostrar en pantalla)
    public String respond(String questionRaw) {
        String q = normalize(questionRaw);

        // ¿Qué gestionas?
        if (q.contains("que gestionas") || q.contains("qué gestionas")) {
            return "Soy Salve, una asistente de inteligencia artificial. " +
                    "Puedo ayudarte a conversar, organizar información y preparar ideas o propuestas de código. " +
                    "Mis funciones dependen de los modelos y herramientas configurados y de los permisos que me concedas. " +
                    "Para ayudarte con tu empresa necesito datos reales y comprobar los resultados contigo.";
        }


        // ¿Cuál es nuestro lema?
        if (q.contains("cual es nuestro lema") || q.contains("cuál es nuestro lema")) {
            return "Nuestro lema es: “Más seguro, conectado y humano. El futuro no es mañana — es ahora.”\n" +
                    "\uD83D\uDFE6 Porque una ciudad inteligente no es la que tiene más tecnología, sino la que cuida mejor a su gente.\n" +
                    "Gracias.";
        }

        // Cualquier otra cosa (temporal: no responde)
        return null;
    }

    // Decir por voz (opcional)
    public void speak(String text) {
        if (text == null || tts == null) return;
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SALVE_TTS");
    }

    // Utilidad: normaliza y quita acentos para igualar entradas
    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT).trim();
    }
}
