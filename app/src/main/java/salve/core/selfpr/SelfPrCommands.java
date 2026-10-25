package salve.core.selfpr;

import java.text.Normalizer;
import java.util.Locale;

/** Cómo le pide Bryan que proponga código («abre un PR») o que cuente cómo van sus propuestas. */
public final class SelfPrCommands {
    private SelfPrCommands() { }

    public static boolean isCommand(String input) {
        String t = clean(input);
        return t.matches("(?:abre|haz|hazte|crea|escribe|manda|envia|propon)(?: tu| un| una| tu propio| tus)? ?"
                + "(?:pr|pull request|parche|propuesta de codigo|propuesta en github)(?: en github| para mejorarte| propio)?")
                || t.matches("(?:programate|mejorate|automejorate) (?:y )?(?:abre|haz|crea|manda) (?:un )?(?:pr|pull request|parche)");
    }

    public static boolean isStatusCommand(String input) {
        String t = clean(input);
        return t.matches("(?:como van|que tal van|estado de|que paso con|y) (?:tus|los) (?:prs?|pull requests|parches|propuestas(?: en github)?)");
    }

    static String clean(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim();
        return t.replaceFirst("^salve ", "").replaceFirst("^(?:por favor|venga|oye) ", "").replaceFirst(" por favor$", "");
    }
}
