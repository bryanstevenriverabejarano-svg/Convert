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
    private MobileCommand(Type type, String query, WhatsAppDraft draft) {
        this.type = type; this.query = query; this.draft = draft;
    }
    public static MobileCommand parse(String input) {
        if (input == null || input.length() > 4500) return null;
        String text = input.trim().replaceFirst("(?iu)^salve[, ]+", "");
        if (text.matches("(?iu)(?:abre|abrir|muestra) (?:el |tu )?(?:panel flotante|mini ?ventana|ventana flotante)[.!]?$"))
            return new MobileCommand(Type.PANEL, "", null);
        Matcher search = Pattern.compile("(?isu)^(?:busca|buscar) en (?:una |la )?(?:mini ?ventana|ventana flotante)[: ]+(.+)$").matcher(text);
        if (search.matches() && !search.group(1).trim().isEmpty() && search.group(1).length() <= 1000)
            return new MobileCommand(Type.SEARCH, search.group(1).trim(), null);
        Matcher message = Pattern.compile("(?isu)^(?:prepara|escribe) (?:un )?(?:whatsapp|wasap) a (\\+[0-9 ()-]+):[ ]?(.+)$").matcher(text);
        if (message.matches()) {
            try { return new MobileCommand(Type.WHATSAPP, "", WhatsAppDraft.create(message.group(1), message.group(2))); }
            catch (IllegalArgumentException invalid) { return null; }
        }
        return null;
    }
}
