package salve.core.memory;

import java.text.Normalizer;
import java.util.Locale;

/** Explicit cloud control is not a public web query or an invented model action. */
public final class CloudMemoryCommand {
    public enum Action { RESTORE, STATUS }
    private CloudMemoryCommand() { }
    public static Action parse(String input) {
        String text = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "").replaceFirst(" por favor$", "");
        if (text.matches("(?:recupera|restaura|sincroniza) (?:tus |los |mis )?recuerdos(?: (?:de|desde|con) (?:la nube|pcloud))?")) return Action.RESTORE;
        if (text.matches("(?:estado de (?:tus |los |mis )?recuerdos|estado de (?:la nube|pcloud)|como va (?:la sincronizacion|la recuperacion))")) return Action.STATUS;
        return null;
    }
}
