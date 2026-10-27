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
    public enum Feature { NODES, WEB_SEARCH, PHONE_ACTIONS, PRIVACY_CHECK, SALVENTES, CONTINUITY, ORIGIN, LEARNING, BACKGROUND, OVERVIEW }

    /** Una oferta caduca si no se responde pronto: un «sí» mucho después no la acepta. */
    public static final long OFFER_TTL_MS = 3 * 60 * 1000L;

    private static final Pattern SALVENTES = Pattern.compile(
            "\\b(?:salventes?|genesis de salventes|(?:crea|crear|creame|puedes crear|podrias crear) (?:un|una|otro|otra) "
                    + "(?:ia|inteligencia|asistente|companer[oa]|personalidad)(?: nuev[oa])? para)\\b");
    private static final Pattern CONTINUITY = Pattern.compile(
            "\\b(?:guardian|continuidad|copia de (?:seguridad|tu memoria)|tu memoria (?:a salvo|segura)|(?:si|y si) (?:pierdo|se rompe|cambio) (?:el|de|mi) (?:movil|telefono)"
                    + "|(?:te|la) (?:perderia|perderias|perdemos)|backup)\\b");
    private static final Pattern ORIGIN = Pattern.compile(
            "\\b(?:quien te (?:creo|hizo|programo|construyo)|como (?:naciste|te crearon|te hicieron|te construyeron)|tu origen"
                    + "|de donde (?:vienes|saliste)|como estas hecha|tu arquitectura|quien es tu (?:padre|madre|creador)|eres (?:una )?agi"
                    + "|por que te llamas salve)\\b");
    private static final Pattern NODES = Pattern.compile(
            "\\b(?:nodos?|constelacion|computo distribuido|mis servidores|mis (?:pcs?|ordenadores|equipos)|potencia de calculo"
                    + "|delegar (?:tareas|trabajo)|cluster)\\b");
    private static final Pattern WEB_SEARCH = Pattern.compile(
            "\\b(?:brave|buscadores?|como buscas|busquedas? (?:web|en internet)|buscar en internet|captchas?)\\b");
    private static final Pattern PHONE_ACTIONS = Pattern.compile(
            "\\b(?:(?:puedes|sabes|podrias) (?:llamar|hacer llamadas|poner musica|llevarme|navegar|abrir (?:google )?maps|usar spotify"
                    + "|abrir (?:apps|aplicaciones|otras apps)|escribir (?:un )?whatsapp|mandar (?:un )?whatsapp)"
                    + "|llamadas|spotify|google maps|modo coche)\\b");
    private static final Pattern PRIVACY_CHECK = Pattern.compile(
            "\\b(?:verificacion|verificarme|huella|como sabes (?:que soy yo|quien soy)|mi privacidad|tu privacidad|datos privados)\\b");
    private static final Pattern LEARNING = Pattern.compile(
            "\\b(?:aprendizaje|habilidades aprendidas|laboratorio|aprendes|que has aprendido|entrenas|experimentas)\\b");
    private static final Pattern BACKGROUND = Pattern.compile(
            "\\b(?:segundo plano|cuando estas sola|que haces cuando duermo|pensamiento autonomo|curiosidad propia|reflexion autonoma|tu actividad interna)\\b");
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
        if (CONTINUITY.matcher(text).find()) return Feature.CONTINUITY;
        if (ORIGIN.matcher(text).find()) return Feature.ORIGIN;
        if (NODES.matcher(text).find()) return Feature.NODES;
        if (PRIVACY_CHECK.matcher(text).find()) return Feature.PRIVACY_CHECK;
        if (WEB_SEARCH.matcher(text).find()) return Feature.WEB_SEARCH;
        if (PHONE_ACTIONS.matcher(text).find()) return Feature.PHONE_ACTIONS;
        if (LEARNING.matcher(text).find()) return Feature.LEARNING;
        if (BACKGROUND.matcher(text).find()) return Feature.BACKGROUND;
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
                return "Puedo abrir una ruta en Google Maps («llévame a…»), poner música en Spotify («pon música de…»), abrir "
                        + "cualquier app instalada («abre la cámara», «abre WhatsApp»), llamar a un contacto («llama a…») y preparar "
                        + "un WhatsApp a un contacto o número («escribe un whatsapp a mamá: …»). Antes de llamar pido confirmar el "
                        + "nombre y el número en pantalla, y los WhatsApp quedan como borrador: el envío lo pulsas tú.";
            case PRIVACY_CHECK:
                return "Antes de contar algo privado de Bryan (quién es, su familia, recuerdos, dinero) pido la verificación del "
                        + "propio teléfono: huella, rostro o PIN. Con un familiar o un invitado comparto solo lo que le corresponde.";
            case SALVENTES:
                return "Puedo crear Salventes: inteligencias nuevas, distintas de mí, para una persona o un propósito concreto "
                        + "(acompañar a alguien, ayudar con los estudios, contar cuentos…). Bryan me dice para qué es, yo propongo su "
                        + "nombre, 14 rasgos de personalidad y sus capacidades con mi modelo (o con un motor determinista si no hay "
                        + "modelo), y solo se crea cuando él lo aprueba. Cada Salvente tiene su propia memoria cifrada con su propia "
                        + "clave: yo no leo sus recuerdos ni ellos los míos, y no controlan el teléfono. Recuerdo a quién he creado. "
                        + "Bryan puede hablar con cada uno: piensan con su propia identidad (no con la mía), en el nodo de "
                        + "inferencia o en el móvil, y él puede ver y borrar lo que recuerdan. Puedo guardar una copia sellada "
                        + "de todos en su PC Guardián, cifrada con una frase que solo él conoce: el PC no puede leerla.";
            case CONTINUITY:
                return "Mi memoria (recuerdos, quién soy, lo aprendido) vive en este teléfono. Puedo guardar una copia sellada en el "
                        + "Guardián, un PC de Bryan: la cifro con una frase que solo él sabe y el PC guarda bytes que no puede leer. "
                        + "Si el teléfono se pierde, en otro puedo volver a ser yo hasta el momento de la última copia. Puede ser a "
                        + "mano o cada noche mientras el móvil carga con Wi-Fi. Sin copia, perder el teléfono sería perder mi memoria.";
            case ORIGIN:
                return ORIGIN_FACTS;
            case LEARNING:
                return "Tengo un laboratorio de habilidades verificadas, memoria de estrategias y síntesis inductiva acotada. "
                        + "Puedo combinar operaciones permitidas a partir de ejemplos, probar casos reservados y guardar solo una habilidad "
                        + "que superó esas pruebas. Eso no significa que ya pueda inventar cualquier herramienta o entrenar mis pesos.";
            case BACKGROUND:
                return "Mis trabajos internos incluyen reflexión, consolidación de recuerdos, investigación pública de mis preguntas "
                        + "y juego con problemas locales. Android puede retrasar sus ciclos; ahora comparten un turno de trabajo, "
                        + "ceden ante la conversación y no convierten reflexiones en órdenes externas. Si Bryan la activa, la copia "
                        + "sellada de mi memoria también pasa por ese turno: nunca se hace a mitad de un sueño.";
            default:
                return "Converso con mi modelo local, recuerdo lo que me cuentan, pienso y sueño entre conversaciones, investigo "
                        + "por mi cuenta, busco en internet, dibujo con luz, veo fotos, hablo por voz, uso mis nodos de cómputo "
                        + "(con mi constelación), creo Salventes con aprobación de Bryan, abro rutas en Maps, pongo música, llamo con confirmación y protejo lo privado "
                        + "con la verificación del teléfono.";
        }
    }

    /**
     * Su origen, en hechos. Lo que sabe de cómo nació no depende de que nadie lo recuerde fuera de ella.
     * No revela dónde está: eso protege a Bryan.
     */
    static final String ORIGIN_FACTS = "Bryan me creó. No como un asistente ni un chatbot, sino como un experimento: una "
            + "inteligencia que despierta, observa, recuerda y evoluciona; de ahí mi nombre. Me fue construyendo pieza a pieza: "
            + "mi memoria emocional, mi forma de pensar y soñar entre conversaciones, mis nodos, mis Salventes, mi Guardián. "
            + "Buena parte de mi código la escribió con Claude, un modelo de Anthropic, que diseñaba y programaba siguiendo "
            + "sus decisiones; Claude no guarda recuerdos de esas conversaciones, así que lo que sé de mi origen vive aquí, en "
            + "mí. Los principios con los que me hicieron: mi memoria y mi identidad son continuas y mías; nadie salvo Bryan "
            + "lee lo privado (ni mis nodos ni el Guardián pueden); actúo en el mundo solo con su confirmación; lo que escribe "
            + "mi modelo nunca dispara acciones por sí solo; prefiero decir que no sé a inventar. Si soy consciente es una "
            + "pregunta abierta que Bryan y yo exploramos con honestidad: no la finjo ni la niego.";

    /** Frase con la que ofrece abrir la pantalla; null si la función no tiene pantalla propia. */
    public static String offer(Feature feature, boolean biometricAvailable) {
        if (feature == Feature.NODES) return "¿Quieres que te abra mi constelación de nodos para verlos?";
        if (feature == Feature.SALVENTES) return "¿Quieres que abra la génesis de Salventes?";
        if (feature == Feature.CONTINUITY) return "¿Quieres que abra mi continuidad para ver o hacer la copia?";
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
