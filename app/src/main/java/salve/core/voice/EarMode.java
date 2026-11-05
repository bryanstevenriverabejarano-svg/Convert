package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * «Modo oídos»: en la conversación por voz, Salve se oye a Bryan con el oído de su propio modelo
 * (Gemma 4 sin censura + mmproj) en vez del dictado de Android: entiende lo que el dictado escribe mal
 * y nota el tono. Aquí está la orden para activarlo y cómo leer lo que devuelve el modelo (puro).
 */
public final class EarMode {
    public enum Command { ON, OFF }

    /** Lo que el modelo oyó: las palabras (para las órdenes y la conversación) y el tono. */
    public static final class Heard {
        public final String text, tone;
        Heard(String text, String tone) { this.text = text; this.tone = tone; }
    }

    public static final String PROMPT = "Eres el oído de Salve. Escucha el audio: es Bryan hablándote en español. "
            + "Escribe EXACTAMENTE lo que dice, palabra por palabra, sin responderle ni añadir nada. "
            + "Contesta solo con dos líneas:\nTEXTO: <lo que dice>\nTONO: <cómo lo dice, en dos a cuatro palabras>\n"
            + "Si no se entiende ninguna palabra, escribe «TEXTO:» vacío.";

    private static final Pattern ON = Pattern.compile(
            "^(?:quiero que )?(?:escuchame|oyeme|activa|pon|usa|enciende)(?: el| tu| tus| los)? ?"
                    + "(?:de verdad|con tus oidos|tus oidos|oidos|modo oidos|modo oido)(?: de verdad| en la conversacion| al hablar)?$");
    private static final Pattern OFF = Pattern.compile(
            "^(?:desactiva|apaga|quita)(?: el| tu| tus| los)? ?(?:modo oidos|modo oido|oidos)$"
                    + "|^(?:usa|vuelve a usar|vuelve a)(?: el)? (?:dictado|reconocimiento de android|reconocimiento normal)$");

    private EarMode() { }

    public static Command parse(String input) {
        String text = normalize(input);
        if (ON.matcher(text).matches()) return Command.ON;
        if (OFF.matcher(text).matches()) return Command.OFF;
        return null;
    }

    /** Lee «TEXTO: …\nTONO: …»; si el modelo no respeta el formato, todo lo que dijo es el texto. */
    public static Heard parseHeard(String output) {
        if (output == null) return new Heard("", "");
        String text = null, tone = "";
        for (String line : output.split("\\r?\\n")) {
            String l = line.trim().replaceFirst("^[*_\\-• ]+", "");
            String upper = l.toUpperCase(Locale.ROOT);
            if (upper.startsWith("TEXTO:")) text = l.substring(6).replaceFirst("^[*_ ]+", "").trim();
            else if (upper.startsWith("TONO:")) tone = l.substring(5).replaceFirst("^[*_ ]+", "").trim();
        }
        if (text == null) text = output.trim();
        text = text.replaceAll("^[«\"“]+|[»\"”]+$", "").trim();
        if (text.length() > 600) text = text.substring(0, 600);
        if (tone.length() > 60) tone = tone.substring(0, 60);
        return new Heard(text, tone.replaceAll("[.«»\"]", "").trim());
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:salve |oye salve )", "").replaceFirst(" (?:por favor|porfa)$", "").trim();
    }
}
