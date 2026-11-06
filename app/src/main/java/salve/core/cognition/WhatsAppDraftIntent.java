package salve.core.cognition;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A narrow capability gate for model-authored drafts. This class does NOT write
 * the message: the LLM chooses content and wording from the user's actual goal.
 * It only checks that the user requested contact and named the addressee.
 * Final recipient matching and sending remain outside the model's authority.
 */
public final class WhatsAppDraftIntent {
    private static final Pattern MESSAGE_ACTION = Pattern.compile(
            "\\b(?:dile|diles|digale|digas|cuentale|comunicale|avisa[a-z]*|"
            + "escrib[a-z]*|redact[a-z]*|prepar[a-z]*|mand[a-z]*|envi[a-z]*|"
            + "responde[a-z]*|ponle|comenta[a-z]*|explica[a-z]*|contacta[a-z]*)\\b");
    private static final Pattern TO_SOMEONE = Pattern.compile(
            "\\b(?:a|al|para|con)\\s+(?:(?:mi|mis|la|el|un|una)\\s+)?[a-z0-9+]{2,}");
    private WhatsAppDraftIntent() { }

    /** Offers a draft tool; the LLM still decides if it actually needs to use it. */
    public static boolean offered(String userInput) {
        String s = normalize(userInput);
        boolean channel = contains(s, "whatsapp") || contains(s, "whats app")
                || contains(s, "watsapp") || contains(s, "wasap") || contains(s, "guasap");
        boolean communication = MESSAGE_ACTION.matcher(s).find();
        // Natural requests like "Dile a mi madre que no podré ir" don't have to
        // repeat the application name. WhatsApp is merely the available draft route.
        return communication && (channel || TO_SOMEONE.matcher(s).find());
    }

    /**
     * Do not demand a word-for-word message: doing so would defeat language
     * understanding and prevent Salve from drafting an appropriate explanation.
     * Require only an addressee grounded in the current user's request.
     */
    public static void validate(String userInput, String contact, String message) {
        if (!offered(userInput))
            throw new IllegalArgumentException("No hay solicitud explícita de comunicación.");
        if (contact == null || contact.trim().length() < 2 || contact.trim().length() > 60
                || message == null || message.trim().isEmpty() || message.length() > 4000)
            throw new IllegalArgumentException("Destinatario o borrador ausente.");
        if (contact.indexOf('\n') >= 0 || contact.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Destinatario inválido.");
        String input = normalize(userInput), name = normalize(contact);
        if (name.isEmpty() || !contains(input, name))
            throw new IllegalArgumentException("El destinatario debe aparecer en la petición del usuario.");
        // The LLM may compose/rephrase the message. WhatsApp only receives a
        // draft, never an instruction to send; human review owns the final text.
    }

    private static boolean contains(String source, String phrase) {
        return (" " + source + " ").contains(" " + phrase + " ");
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9+]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
