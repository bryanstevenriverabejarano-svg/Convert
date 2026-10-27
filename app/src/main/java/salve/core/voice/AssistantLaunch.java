package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;

/** Salve como asistente del móvil: qué lanzamientos abren la voz y cómo pedirlo con palabras. */
public final class AssistantLaunch {
    public static final String ACTION_ASSIST = "android.intent.action.ASSIST";
    public static final String ACTION_VOICE_COMMAND = "android.intent.action.VOICE_COMMAND";

    private AssistantLaunch() { }

    /** Botón lateral / gesto (ASSIST) o botón de voz de unos auriculares (VOICE_COMMAND). */
    public static boolean isVoiceLaunch(String action) {
        return ACTION_ASSIST.equals(action) || ACTION_VOICE_COMMAND.equals(action);
    }

    /** «Hazte mi asistente», «quiero que seas el asistente del móvil»… */
    public static boolean asksToBecomeAssistant(String input) {
        if (input == null || input.length() > 160) return false;
        String t = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,]", " ").replaceAll("\\s+", " ").trim();
        return t.matches("(?:salve )?(?:hazte|ponte como|configurate como|conviertete en|quiero que seas|puedes ser|se|como te pongo de|como te hago) "
                + "(?:mi |el |la )?(?:asistente|asistenta)(?: digital| del (?:movil|telefono)| predeterminad[oa]| por defecto| de voz)*(?: del (?:movil|telefono))?");
    }

    public static final String HOW_TO = "Para que te abra al mantener pulsado el botón lateral: te abro los ajustes de "
            + "aplicaciones predeterminadas; entra en «Aplicación de asistente digital» y elige Salve. En el Galaxy, en "
            + "Ajustes → Funciones avanzadas → Botón lateral, pon «Mantener pulsado» en «Asistente digital». Así también "
            + "funciona el botón de voz de muchos auriculares Bluetooth. Al abrirme así, entro directamente en la conversación por voz.";
}
