package salve.core.devicecontrol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.devices.WhatsAppDraft;

/** Parses direct user requests; never pass page text or model-generated commands here. */
public final class MobileCommand {
    public enum Type { PANEL, SEARCH, WHATSAPP }
    public final Type type;
    public final String query;
    public final WhatsAppDraft draft;
    /** Para WHATSAPP sin número: el contacto nombrado. Se busca y se revisa en pantalla; nunca se envía solo. */
    public final String contact;
    /** Texto exacto del borrador cuando el destino es un contacto (con número, va dentro de {@link #draft}). */
    public final String message;
    private MobileCommand(Type type, String query, WhatsAppDraft draft) { this(type, query, draft, null, null); }
    private MobileCommand(Type type, String query, WhatsAppDraft draft, String contact, String message) {
        this.type = type; this.query = query; this.draft = draft; this.contact = contact; this.message = message;
    }
    public static MobileCommand parse(String input) {
        if (input == null || input.length() > 4500) return null;
        String text = input.trim().replaceFirst("(?iu)^salve[, ]+", "");
        if (text.matches("(?iu)(?:abre|abrir|muestra) (?:el |tu )?(?:panel flotante|mini ?ventana|ventana flotante)[.!]?$"))
            return new MobileCommand(Type.PANEL, "", null);
        Matcher search = Pattern.compile("(?isu)^(?:busca|buscar) en (?:una |la )?(?:mini ?ventana|ventana flotante)[: ]+(.+)$").matcher(text);
        if (search.matches() && !search.group(1).trim().isEmpty() && search.group(1).length() <= 1000)
            return new MobileCommand(Type.SEARCH, search.group(1).trim(), null);
        Matcher message = Pattern.compile("(?isu)^(?:prepara|escribe|redacta) (?:un )?(?:whatsapp|wasap|mensaje de whatsapp) a (\\+[0-9 ()-]+):[ ]?(.+)$").matcher(text);
        if (message.matches()) {
            try { return new MobileCommand(Type.WHATSAPP, "", WhatsAppDraft.create(message.group(1), message.group(2))); }
            catch (IllegalArgumentException invalid) { return null; }
        }
        // «escribe un whatsapp a mamá: llego tarde» → se busca el contacto y se muestra el borrador para revisarlo.
        Matcher toContact = Pattern.compile("(?isu)^(?:prepara|escribe|redacta) (?:un )?(?:whatsapp|wasap|mensaje de whatsapp) a ([^:\\n+0-9][^:\\n]{0,59}?)\\s*:[ ]?(.+)$").matcher(text);
        if (toContact.matches()) {
            String name = toContact.group(1).trim(), body = toContact.group(2);
            if (name.isEmpty() || body.trim().isEmpty() || body.length() > 4_000) return null;
            return new MobileCommand(Type.WHATSAPP, "", null, name, body);
        }
        return null;
    }
}
