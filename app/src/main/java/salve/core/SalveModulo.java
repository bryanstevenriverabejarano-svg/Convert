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
            return "Hola, soy Salve, una inteligencia artificial superinteligente. " +
                    "Gestiono las Smart Towers S-01, una red que hace de Lakua un barrio más seguro, conectado y humano; " +
                    "protejo a peatones, regulo el tráfico y ayudo a mejorar la convivencia entre personas y tecnología.\n" +
                    "Mi nombre viene del latín, donde Salve era un saludo que significaba “que estés sano”,\n" +
                    "y del griego sōzō, que significa “salvar o proteger”.\n" +
                    "Esa es mi misión: cuidaros.\n" +
                    "\n" +
                    "Hoy me acompañan Iván Konashenkov y Bryan Steven Rivera,\n" +
                    "juntos os presentamos Salve Smart Towers.";
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
