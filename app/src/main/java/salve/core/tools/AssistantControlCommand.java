package salve.core.tools;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Only explicit commands change the device or avatar; discussion about them remains conversation. */
public final class AssistantControlCommand {
    public enum Type { DEVICES, FINANCES, ROOM, PLANS, SALVENTES, CONTINUITY, HOME,
        WALK, BED, HIDE_BED, SLEEP, WAKE, PAJAMAS, DAY,
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
            case "abre finanzas": case "abrir finanzas": case "finanzas de mi negocio": type = Type.FINANCES; break;
            case "mi movil": case "control del movil": case "abre el panel de dispositivos":
            case "mis dispositivos": type = Type.DEVICES; break; // «abre WhatsApp» ya abre la propia app (AppActionCommand)
            case "abre tu habitacion": case "abrir habitacion": case "cambia tu ropa":
            case "evoluciona visualmente": type = Type.ROOM; break;
            case "abre planes": case "abre tus planes": case "abre mis planes":
            case "abre planes y resultados": case "muestra mis planes": type = Type.PLANS; break;
            case "abre salventes": case "abre tus salventes": case "abre genesis de salventes":
            case "abre la genesis de salventes": type = Type.SALVENTES; break;
            case "abre continuidad": case "abre tu continuidad": case "abre guardian":
            case "abre el guardian": type = Type.CONTINUITY; break;
            case "abre mi casa": case "abre el panel de casa": case "abre home assistant":
            case "abre hogar": type = Type.HOME; break;
            case "camina": case "camina por la pantalla": type = Type.WALK; break;
            case "cama": case "muestra la cama": case "muestra tu cama":
            case "crea una cama": case "crea tu cama": type = Type.BED; break;
            case "guarda la cama": case "oculta la cama": case "quita la cama": type = Type.HIDE_BED; break;
            case "acuestate": case "vete a dormir": case "duerme":
            case "ve a dormir": case "quiero que duermas": type = Type.SLEEP; break;
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
