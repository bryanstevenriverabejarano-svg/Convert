package salve.core.mind;

import java.text.Normalizer;
import java.util.Locale;

/** Órdenes explícitas de Bryan sobre el estado interno. Coincidencia exacta, no palabras sueltas. */
public final class MindCommand {
    public enum Action { DESCRIBE, ACCEPT_NARRATIVE, REJECT_NARRATIVE, PAUSE, RESUME, DREAM_NOW, EXPORT }

    private MindCommand() {}

    public static Action parse(String input) {
        String t = normalize(input);
        switch (t) {
            case "tu mente": case "estado de tu mente": case "que hay en tu mente": case "en que has pensado":
            case "que has pensado": case "que has estado pensando": case "que crees de ti": case "que crees sobre ti":
            case "muestrame tu mente": case "tu modelo interno":
                return Action.DESCRIBE;
            case "acepta tu nueva identidad": case "acepta tu nueva narrativa":
                return Action.ACCEPT_NARRATIVE;
            case "rechaza tu nueva identidad": case "rechaza tu nueva narrativa":
                return Action.REJECT_NARRATIVE;
            case "pausa tu mente": case "deja de pensar":
                return Action.PAUSE;
            case "reanuda tu mente": case "vuelve a pensar":
                return Action.RESUME;
            case "suena ahora": case "consolida tus recuerdos": case "duerme y consolida":
                return Action.DREAM_NOW;
            case "exporta tu mente": case "exporta tu mente para entrenar":
                return Action.EXPORT;
            default:
                return null;
        }
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,;:]+", " ")
                .replaceAll("^\\s*salve\\s+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
