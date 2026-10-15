package salve.core.memory;

import java.text.Normalizer;
import java.util.Locale;

/** Explicit cloud control is not a public web query or an invented model action. */
public final class CloudMemoryCommand {
    public enum Action { RESTORE, STATUS, ASK }
    private CloudMemoryCommand() { }
    public static Action parse(String input) {
        // Con erratas de teclado corregidas («rexupera tus recuerdos»).
        String text = TypoTolerantText.forRouting(input).replace('ñ', 'n')
                .replaceFirst("^salve ", "").replaceFirst(" por favor$", "");
        if (text.matches("(?:recupera|restaura|sincroniza) (?:tus |los |mis )?recuerdos(?: (?:de|desde|con) (?:la nube|pcloud))?")) return Action.RESTORE;
        if (text.matches("(?:estado de (?:tus |los |mis )?recuerdos|estado de (?:la nube|pcloud)|como va (?:la sincronizacion|la recuperacion))")) return Action.STATUS;
        // Preguntas sobre recuerdos de la nube («¿y el primer recuerdo que tienes en p cloud?»):
        // si pCloud no está conectada en esta instalación, hay que decirlo en vez de responder
        // con los recuerdos locales como si fueran toda la historia.
        if (text.matches(".*\\b(p ?cloud|la nube)\\b.*") && text.matches(".*\\b(recuerdo|recuerdos|memoria|memorias|recuerdas)\\b.*")) return Action.ASK;
        return null;
    }
}
