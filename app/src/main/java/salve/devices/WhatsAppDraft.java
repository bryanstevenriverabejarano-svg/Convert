package salve.devices;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** An explicit draft destination. It never resolves a contact or sends a message. */
public final class WhatsAppDraft {
    private final String phone;
    private final String message;

    private WhatsAppDraft(String phone, String message) {
        this.phone = phone;
        this.message = message;
    }

    public static WhatsAppDraft create(String phoneInput, String message) {
        String phone = phoneInput == null ? "" : phoneInput.trim().replaceAll("[ ()-]", "");
        if (!phone.matches("\\+[1-9][0-9]{6,14}")) {
            throw new IllegalArgumentException("Escribe el número internacional con + y código de país, sin extensiones.");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Escribe el texto del borrador.");
        }
        if (message.length() > 4_000) {
            throw new IllegalArgumentException("El borrador admite hasta 4000 caracteres.");
        }
        return new WhatsAppDraft(phone, message);
    }

    /**
     * Número de la agenda en formato internacional si ya lo trae (+34…, 0034…); null si es local.
     * Los números locales se convierten con el país del móvil en la pantalla, no aquí.
     */
    public static String internationalOrNull(String number) {
        if (number == null) return null;
        String clean = number.trim().replaceAll("[ ()./-]", "");
        if (clean.startsWith("00")) clean = "+" + clean.substring(2);
        return clean.matches("\\+[1-9][0-9]{6,14}") ? clean : null;
    }

    public String getPhone() { return phone; }
    public String getMessage() { return message; }

    public String getUrl() {
        try {
            return "https://wa.me/" + phone.substring(1) + "?text="
                    + URLEncoder.encode(message, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
