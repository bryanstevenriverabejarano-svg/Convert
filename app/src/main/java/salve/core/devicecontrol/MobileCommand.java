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
        // Una sola intención del usuario: abrir el chat de un contacto y dejar un texto preparado.
        // El borrador se abre con la API pública de WhatsApp: no se pulsa «Enviar».
        final String wa = AppAliases.WHATSAPP;
        final String say = "(?:escr[ií]be(?:le)?|escribir(?:le)?|redacta(?:le)?|d[ií]le|dile|p[oó]n(?:le)?)"; // nunca «envía/manda»: solo se prepara el borrador
        Matcher chat = Pattern.compile(
                "(?isu)^(?:abre|abrir|[aá]breme|entra en|ve a)\\s+"
                + "(?:(?:el\\s+)?" + wa + "\\s+y\\s+(?:(?:abre|abrir|busca|entra en|ve a|en)\\s+)?)?"
                + "(?:el\\s+)?(?:chat|conversaci[oó]n)\\s+(?:de|con)\\s+(.+?)\\s+(?:y\\s+)?" + say + "\\s+(.+)$").matcher(text);
        // «en WhatsApp dile a mami que la quiero», «dile a mami por WhatsApp que…», «escríbele a mami por WhatsApp: …»
        Matcher tell = Pattern.compile(
                "(?isu)^(?:(?:abre\\s+)?(?:el\\s+)?(?:en|por)\\s+(?:el\\s+)?" + wa + "[,]?\\s+)?" + say + "\\s+(?:un\\s+(?:mensaje|" + wa + ")\\s+)?a\\s+"
                + "([^:\\n+0-9][^:\\n]{0,40}?)\\s+(?:(?:por|en)\\s+(?:el\\s+)?" + wa + "\\s+)?(?:diciendo(?:le)?\\s+)?(que\\s+.+|:\\s*.+)$").matcher(text);
        boolean viaWhatsApp = Pattern.compile("(?iu)\\b" + wa + "\\b").matcher(text).find();
        if (!chat.matches() && tell.matches() && viaWhatsApp) chat = tell;
        if (chat.matches()) {
            String name = chat.group(1).trim().replaceFirst("(?iu)\\s+(?:por|en)\\s+(?:el\\s+)?" + wa + "$", "");
            String body = chat.group(2).trim().replaceFirst("^:\\s*", "");
            if (name.isEmpty() || name.length() > 60 || body.isEmpty() || body.length() > 4_000) return null;
            if (name.matches("(?iu)(?:y|e|o|a|de|con|el|la)")) return null; // «abre el chat de y escribe hola»: sin destinatario
            // «Escríbele que la quiero mucho» se expresa en segunda persona en el borrador.
            // No se cambia el resto de mensajes: nunca se modifica una orden de envío.
            body = body.replaceFirst("(?iu)^que\\s+(?:la|lo)\\s+quiero\\b", "Te quiero");
            // «dile que llego tarde» → «Llego tarde»: el «que» es de la orden, no del mensaje.
            body = body.replaceFirst("(?iu)^que\\s+", "");
            if (!body.isEmpty()) body = Character.toUpperCase(body.charAt(0)) + body.substring(1);
            return new MobileCommand(Type.WHATSAPP, "", null, name, body);
        }
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
