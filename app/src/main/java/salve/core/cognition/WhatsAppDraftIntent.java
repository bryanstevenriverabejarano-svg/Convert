package salve.core.cognition;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Controller-side boundary for a model-proposed WhatsApp draft.
 * The model may choose the tool, but cannot invent the recipient or message:
 * both must be present in this turn's explicit user request.
 * It never sends a message or grants accessibility permissions.
 */
public final class WhatsAppDraftIntent {
    private static final Pattern ACTION = Pattern.compile(
            "(?:^| )(?:escrib[a-z]*|redact[a-z]*|prepar[a-z]*|mand[a-z]*|envi[a-z]*"
            + "|dile|decirle|dejale|ponle|respondele)(?: |$)");
    private WhatsAppDraftIntent() { }

    public static boolean offered(String userInput) {
        String s = normalize(userInput);
        boolean whatsapp = contains(s, "whatsapp") || contains(s, "whats app")
                || contains(s, "watsapp") || contains(s, "wasap") || contains(s, "guasap");
        return whatsapp && ACTION.matcher(" " + s + " ").find();
    }

    /** Reject guessed addressees/text and non-message requests at the execution boundary. */
    public static void validate(String userInput, String contact, String message) {
        if (!offered(userInput)) throw new IllegalArgumentException("El usuario no pidió un borrador de WhatsApp.");
        if (contact == null || contact.trim().length() < 2 || contact.trim().length() > 60
                || message == null || message.trim().isEmpty() || message.length() > 4000)
            throw new IllegalArgumentException("Destinatario o mensaje ausente.");
        if (contact.indexOf('\n') >= 0 || contact.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Destinatario inválido.");
        String input = normalize(userInput);
        String name = normalize(contact), body = normalize(message);
        if (name.isEmpty() || body.isEmpty() || !contains(input, name) || !contains(input, body))
            throw new IllegalArgumentException("El destinatario y el mensaje deben constar literalmente en la orden.");
    }

    private static boolean contains(String source, String phrase) {
        return (" " + source + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
