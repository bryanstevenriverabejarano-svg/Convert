package salve.core.mind;

import java.text.Normalizer;
import java.util.Locale;

/** «Enséñame el mapa de tu mente», «abre tu mapa mental»… (frase exacta, no palabras sueltas). */
public final class MindMapCommand {
    private MindMapCommand() { }
    public static boolean matches(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
        return t.matches("(?:(?:ensename|muestrame|abre(?:me)?|quiero ver|pon(?:me)?) )?(?:el |tu )?mapa (?:de tu mente|mental|de la mente)(?: de salve)?");
    }
}
