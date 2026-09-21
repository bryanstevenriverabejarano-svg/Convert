package salve.core.tools;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only explicit commands change the device or avatar; discussion about them remains conversation. */
public final class AssistantControlCommand {
    public enum Type { DEVICES, ROOM, WALK, BED, SLEEP, WAKE, PAJAMAS, DAY,
        LIST_RECIPES, CANCEL_RECIPE, LEARN_RECIPE, RUN_RECIPE, DELETE_RECIPE }
    public final Type type;
    public final String argument;
    private AssistantControlCommand(Type type, String argument) { this.type = type; this.argument = argument; }

    public static AssistantControlCommand parse(String input) {
        if (input == null) return null;
        String original = input.trim().replaceFirst("(?iu)^salve[, ]+", "");
        String normalized = Normalizer.normalize(original, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[.!¡¿?]+$", "").trim();
        Type type;
        switch (normalized) {
            case "mi movil": case "control del movil": case "abre el panel de dispositivos":
            case "mis dispositivos": case "abre whatsapp": case "abrir whatsapp": type = Type.DEVICES; break;
            case "abre tu habitacion": case "abrir habitacion": case "cambia tu ropa":
            case "evoluciona visualmente": type = Type.ROOM; break;
            case "camina": case "camina por la pantalla": type = Type.WALK; break;
            case "crea una cama": case "crea tu cama": type = Type.BED; break;
            case "acuestate": case "vete a dormir": type = Type.SLEEP; break;
            case "despierta": case "levantate": type = Type.WAKE; break;
            case "ponte el pijama": case "ponte pijama": type = Type.PAJAMAS; break;
            case "ponte ropa de dia": type = Type.DAY; break;
            case "mis herramientas": case "lista tus rutinas": type = Type.LIST_RECIPES; break;
            case "cancelar rutina": case "cancela la rutina": type = Type.CANCEL_RECIPE; break;
            default: return recipeCommand(original);
        }
        return new AssistantControlCommand(type, "");
    }

    private static AssistantControlCommand recipeCommand(String original) {
        String[] patterns = {"(?:aprende la rutina|crea una herramienta virtual|forja una herramienta para)",
                "(?:ejecuta tu rutina|usa tu habilidad|ejecuta la herramienta)",
                "(?:elimina la rutina|borra la herramienta)"};
        Type[] types = {Type.LEARN_RECIPE, Type.RUN_RECIPE, Type.DELETE_RECIPE};
        for (int i = 0; i < patterns.length; i++) {
            Matcher match = Pattern.compile("^" + patterns[i] + "\\s+(.+)$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                    .matcher(original);
            if (match.matches()) return new AssistantControlCommand(types[i], match.group(1).trim());
        }
        return null;
    }
}
