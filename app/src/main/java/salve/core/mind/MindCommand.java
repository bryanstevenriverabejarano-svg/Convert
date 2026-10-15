package salve.core.mind;

import java.text.Normalizer;
import java.util.Locale;

/** Órdenes explícitas de Bryan sobre el estado interno. Coincidencia exacta, no palabras sueltas. */
public final class MindCommand {
    public enum Action { DESCRIBE, ACCEPT_NARRATIVE, REJECT_NARRATIVE, PAUSE, RESUME, DREAM_NOW, EXPORT,
        RESTORE_NARRATIVE, SELF_DISCOVER_NOW, IDENTITY_AUTONOMY_ON, IDENTITY_AUTONOMY_OFF, WHAT_I_DID }

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
            case "recupera tu narrativa anterior": case "vuelve a tu narrativa anterior": case "recupera tu identidad anterior":
                return Action.RESTORE_NARRATIVE;
            case "examinate": case "descubre quien eres": case "busca quien eres": case "piensa en quien eres":
            case "conocete a ti misma": case "analizate":
                return Action.SELF_DISCOVER_NOW;
            case "decide tu quien eres": case "toma el control de quien eres": case "elige tu propia identidad":
            case "activa tu autonomia de identidad":
                return Action.IDENTITY_AUTONOMY_ON;
            case "no cambies tu identidad sin preguntarme": case "desactiva tu autonomia de identidad":
                return Action.IDENTITY_AUTONOMY_OFF;
            case "que has hecho": case "que has hecho hoy": case "que sabes hacer": case "como sabes lo que haces":
            case "que has hecho tu": case "tu registro de acciones":
                return Action.WHAT_I_DID;
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
