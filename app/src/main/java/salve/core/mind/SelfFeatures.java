package salve.core.mind;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Conocimiento de sus propias funciones: cuando la conversación toca una de ellas, Salve recibe
 * hechos (qué es, cómo se usa y su estado real) en lugar de improvisar, y puede ofrecer abrir la
 * pantalla correspondiente. Abrir algo exige siempre un «sí» explícito del usuario en el turno
 * siguiente; el texto del modelo nunca abre nada.
 */
public final class SelfFeatures {
    public enum Feature { NODES, WEB_SEARCH, PHONE_ACTIONS, PRIVACY_CHECK, SALVENTES, OVERVIEW }

    /** Una oferta caduca si no se responde pronto: un «sí» mucho después no la acepta. */
    public static final long OFFER_TTL_MS = 3 * 60 * 1000L;

    private static final Pattern SALVENTES = Pattern.compile(
            "\\b(?:salventes?|genesis de salventes|(?:crea|crear|creame|puedes crear|podrias crear) (?:un|una|otro|otra) "
                    + "(?:ia|inteligencia|asistente|companer[oa]|personalidad)(?: nuev[oa])? para)\\b");
    private static final Pattern NODES = Pattern.compile(
            "\\b(?:nodos?|constelacion|computo distribuido|mis servidores|mis (?:pcs?|ordenadores|equipos)|potencia de calculo"
                    + "|delegar (?:tareas|trabajo)|cluster)\\b");
    private static final Pattern WEB_SEARCH = Pattern.compile(
            "\\b(?:brave|buscadores?|como buscas|busquedas? (?:web|en internet)|buscar en internet|captchas?)\\b");
    private static final Pattern PHONE_ACTIONS = Pattern.compile(
            "\\b(?:(?:puedes|sabes|podrias) (?:llamar|hacer llamadas|poner musica|llevarme|navegar|abrir (?:google )?maps|usar spotify)"
                    + "|llamadas|spotify|google maps|modo coche)\\b");
    private static final Pattern PRIVACY_CHECK = Pattern.compile(
            "\\b(?:verificacion|verificarme|huella|como sabes (?:que soy yo|quien soy)|mi privacidad|tu privacidad|datos privados)\\b");
    private static final Pattern OVERVIEW = Pattern.compile(
            "\\b(?:que (?:puedes|sabes) hacer|tus (?:funciones|habilidades|capacidades|herramientas)|para que sirves"
                    + "|que funciones tienes|de que eres capaz|que cosas haces)\\b");
    private static final Pattern YES = Pattern.compile(
            "^(?:si|sip|vale|venga|claro|ok|okay|dale|adelante|hazlo|por favor|si por favor|si claro|claro que si|perfecto|genial"
                    + "|abrela|abrelo|abrela ya|muestramela|muestramelos|ensenamela|ensenamelos|quiero verla|quiero verlos|a ver"
                    + "|si abrela|si abrelo|si muestramela|si muestramelos|si quiero|si ensenamelos)$");
    private static final Pattern NO = Pattern.compile("^(?:no|nop|ahora no|luego|mas tarde|dejalo|no gracias|no hace falta)$");

    private SelfFeatures() { }

    public static Feature detect(String input) {
        if (input == null || input.length() > 600) return null;
        String text = normalize(input);
        if (text.isEmpty()) return null;
        if (SALVENTES.matcher(text).find()) return Feature.SALVENTES;
        if (NODES.matcher(text).find()) return Feature.NODES;
        if (PRIVACY_CHECK.matcher(text).find()) return Feature.PRIVACY_CHECK;
        if (WEB_SEARCH.matcher(text).find()) return Feature.WEB_SEARCH;
        if (PHONE_ACTIONS.matcher(text).find()) return Feature.PHONE_ACTIONS;
        if (OVERVIEW.matcher(text).find()) return Feature.OVERVIEW;
        return null;
    }

    /** Qué es cada función, en primera persona y sin adornos. */
    public static String facts(Feature feature) {
        switch (feature) {
            case NODES:
                return "Tengo nodos de cómputo: ordenadores de Bryan (su PC, una torre…) a los que puedo enviar tareas de texto "
                        + "pesadas para pensar con un modelo más grande; si fallan, respondo con el móvil. Hablo con ellos directamente "
                        + "por HTTPS con TLS mutuo (ellos solo aceptan mi certificado y yo solo el suyo), sin VPN ni servicios de "
                        + "terceros, en casa o fuera con su dirección remota; yo no les envío mi ubicación; "
                        + "ellos sí pueden compartirme la suya. Se ven en mi constelación de nodos: el móvil en el centro y cada "
                        + "nodo con su color (verde activo, ámbar ocupado, rojo desconectado), con ficha para probarlo, activarlo "
                        + "o desactivarlo y ver dónde está.";
            case WEB_SEARCH:
                return "Busco en internet leyendo resultados públicos de Google, Bing o DuckDuckGo y, si Bryan me da una clave, "
                        + "con la API oficial de Brave Search, que no tiene CAPTCHA. Si un buscador pide CAPTCHA, ofrezco abrir "
                        + "la búsqueda en su navegador; no me salto esas comprobaciones.";
            case PHONE_ACTIONS:
                return "Puedo abrir una ruta en Google Maps («llévame a…»), poner música en Spotify («pon música de…») y llamar "
                        + "a un contacto («llama a…»); antes de llamar siempre pido confirmar el nombre y el número en pantalla.";
            case PRIVACY_CHECK:
                return "Antes de contar algo privado de Bryan (quién es, su familia, recuerdos, dinero) pido la verificación del "
                        + "propio teléfono: huella, rostro o PIN. Con un familiar o un invitado comparto solo lo que le corresponde.";
            case SALVENTES:
                return "Puedo crear Salventes: inteligencias nuevas, distintas de mí, para una persona o un propósito concreto "
                        + "(acompañar a alguien, ayudar con los estudios, contar cuentos…). Bryan me dice para qué es, yo propongo su "
                        + "nombre, 14 rasgos de personalidad y sus capacidades con mi modelo (o con un motor determinista si no hay "
                        + "modelo), y solo se crea cuando él lo aprueba. Cada Salvente tiene su propia memoria cifrada con su propia "
                        + "clave: yo no leo sus recuerdos ni ellos los míos, y no controlan el teléfono. Recuerdo a quién he creado.";
            default:
                return "Converso con mi modelo local, recuerdo lo que me cuentan, pienso y sueño entre conversaciones, investigo "
                        + "por mi cuenta, busco en internet, dibujo con luz, veo fotos, hablo por voz, uso mis nodos de cómputo "
                        + "(con mi constelación), creo Salventes con aprobación de Bryan, abro rutas en Maps, pongo música, llamo con confirmación y protejo lo privado "
                        + "con la verificación del teléfono.";
        }
    }

    /** Frase con la que ofrece abrir la pantalla; null si la función no tiene pantalla propia. */
    public static String offer(Feature feature, boolean biometricAvailable) {
        if (feature == Feature.NODES) return "¿Quieres que te abra mi constelación de nodos para verlos?";
        if (feature == Feature.SALVENTES) return "¿Quieres que abra la génesis de Salventes?";
        if (feature == Feature.PRIVACY_CHECK && biometricAvailable) return "¿Quieres que te verifique ahora con el teléfono?";
        return null;
    }

    /** Bloque para el contexto del turno: hechos + estado real + cómo responder. */
    public static String promptBlock(Feature feature, String liveState, String offer) {
        return " | CONOCIMIENTO_DE_MÍ (hechos, no inventes ni preguntes qué son): " + facts(feature)
                + (liveState == null || liveState.isEmpty() ? "" : " Estado ahora: " + liveState + ".")
                + " Responde como quien conoce sus propias funciones, en primera persona y breve."
                + (offer == null ? "" : " No añadas tú la oferta de abrir la pantalla: la añado yo al final.");
    }

    public static boolean accepts(String input) { return input != null && YES.matcher(normalize(input)).matches(); }

    public static boolean declines(String input) { return input != null && NO.matcher(normalize(input)).matches(); }

    /** Pregunta añadida a la respuesta salvo que ya pregunte lo mismo. */
    public static String withOffer(String reply, String offer) {
        if (offer == null || reply == null) return reply;
        String text = normalize(reply);
        if (text.contains(normalize(offer))) return reply;
        return reply.trim() + "\n\n" + offer;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}\\p{N} ]+", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "");
    }
}
